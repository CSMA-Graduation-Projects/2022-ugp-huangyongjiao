package com.example.service;

import com.example.entity.BatchExperimentRequest;
import com.example.entity.BatchExperimentResponse;
import com.example.entity.BatchExperimentTaskCurrentItem;
import com.example.entity.BatchExperimentTaskSnapshot;
import com.example.exception.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class BatchExperimentTaskService {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_TERMINATING = "TERMINATING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_TERMINATED = "TERMINATED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String GENERATION_LOCK_MESSAGE = "当前已有批量生成任务正在执行，请等待完成或先终止当前任务";

    private static final int MAX_TASK_HISTORY = 20;

    private final Object monitor = new Object();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("batch-experiment-task-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, BatchExperimentTaskState> taskStore = new LinkedHashMap<>();
    private String activeTaskId;

    public BatchExperimentTaskSnapshot createTask(BatchExperimentRequest requestSnapshot, String batchNo, int totalTaskCount) {
        synchronized (monitor) {
            if (hasActiveTaskLocked()) {
                throw new CustomException("409", GENERATION_LOCK_MESSAGE);
            }

            BatchExperimentTaskState taskState = new BatchExperimentTaskState();
            taskState.taskId = UUID.randomUUID().toString().replace("-", "");
            taskState.batchNo = batchNo;
            taskState.status = STATUS_RUNNING;
            taskState.message = "批量生成执行中，请勿重复提交";
            taskState.requestSnapshot = copyRequest(requestSnapshot);
            taskState.totalTaskCount = Math.max(totalTaskCount, 0);
            taskState.completedCount = 0;
            taskState.successCount = 0;
            taskState.failedCount = 0;
            taskState.startedAt = currentDateTimeText();
            taskState.finishedAt = null;
            taskState.cancelRequested = false;
            taskState.currentItem = null;
            taskState.result = null;

            taskStore.put(taskState.taskId, taskState);
            activeTaskId = taskState.taskId;
            trimTaskHistoryLocked();
            return taskState.toSnapshot(objectMapper);
        }
    }

    public void submitTask(Runnable runnable) {
        executorService.submit(runnable);
    }

    public void assertGenerationUnlocked() {
        synchronized (monitor) {
            if (hasActiveTaskLocked()) {
                throw new CustomException("409", GENERATION_LOCK_MESSAGE);
            }
        }
    }

    public boolean hasActiveTask() {
        synchronized (monitor) {
            return hasActiveTaskLocked();
        }
    }

    public BatchExperimentTaskSnapshot getCurrentTaskSnapshot() {
        synchronized (monitor) {
            if (!hasActiveTaskLocked()) {
                return null;
            }
            return taskStore.get(activeTaskId).toSnapshot(objectMapper);
        }
    }

    public BatchExperimentTaskSnapshot getTaskSnapshot(String taskId) {
        synchronized (monitor) {
            BatchExperimentTaskState taskState = taskStore.get(taskId);
            return taskState == null ? null : taskState.toSnapshot(objectMapper);
        }
    }

    public BatchExperimentTaskSnapshot requestTerminate(String taskId) {
        synchronized (monitor) {
            BatchExperimentTaskState taskState = taskStore.get(taskId);
            if (taskState == null) {
                throw new CustomException("404", "未找到对应的批量生成任务");
            }

            if (!isActiveStatus(taskState.status)) {
                throw new CustomException("409", resolveTerminateDeniedMessage(taskState.status));
            }

            if (!taskState.cancelRequested) {
                taskState.cancelRequested = true;
                taskState.status = STATUS_TERMINATING;
                taskState.message = "已收到终止请求，正在停止后续任务";
            }
            return taskState.toSnapshot(objectMapper);
        }
    }

    public boolean isTerminationRequested(String taskId) {
        synchronized (monitor) {
            BatchExperimentTaskState taskState = taskStore.get(taskId);
            return taskState != null && taskState.cancelRequested;
        }
    }

    public void updateCurrentItem(String taskId, BatchExperimentTaskCurrentItem currentItem) {
        synchronized (monitor) {
            BatchExperimentTaskState taskState = getExistingTaskLocked(taskId);
            taskState.currentItem = copyCurrentItem(currentItem);
            taskState.status = taskState.cancelRequested ? STATUS_TERMINATING : STATUS_RUNNING;
            taskState.message = taskState.cancelRequested
                    ? "已收到终止请求，正在停止后续任务"
                    : "批量生成执行中，请勿重复提交";
        }
    }

    public void updateProgress(String taskId, int completedCount, int successCount, int failedCount) {
        synchronized (monitor) {
            BatchExperimentTaskState taskState = getExistingTaskLocked(taskId);
            taskState.completedCount = Math.max(completedCount, 0);
            taskState.successCount = Math.max(successCount, 0);
            taskState.failedCount = Math.max(failedCount, 0);
        }
    }

    public void markCompleted(String taskId, BatchExperimentResponse result) {
        synchronized (monitor) {
            BatchExperimentTaskState taskState = getExistingTaskLocked(taskId);
            taskState.status = STATUS_COMPLETED;
            taskState.message = "批量生成已完成";
            taskState.finishedAt = currentDateTimeText();
            taskState.currentItem = null;
            taskState.cancelRequested = false;
            taskState.result = copyResult(result);
            if (result != null) {
                taskState.completedCount = safeInt(result.getTotalTaskCount(), taskState.totalTaskCount);
                taskState.successCount = safeInt(result.getSuccessCount(), taskState.successCount);
                taskState.failedCount = safeInt(result.getFailedCount(), taskState.failedCount);
            } else {
                taskState.completedCount = taskState.totalTaskCount;
            }
            clearActiveTaskLocked(taskId);
        }
    }

    public void markTerminated(String taskId, BatchExperimentResponse result) {
        synchronized (monitor) {
            BatchExperimentTaskState taskState = getExistingTaskLocked(taskId);
            taskState.status = STATUS_TERMINATED;
            taskState.message = "批量生成已终止";
            taskState.finishedAt = currentDateTimeText();
            taskState.currentItem = null;
            taskState.cancelRequested = false;
            taskState.result = copyResult(result);
            if (result != null) {
                int completedCount = result.getItems() == null ? taskState.completedCount : result.getItems().size();
                taskState.completedCount = Math.max(completedCount, 0);
                taskState.successCount = safeInt(result.getSuccessCount(), taskState.successCount);
                taskState.failedCount = safeInt(result.getFailedCount(), taskState.failedCount);
            }
            clearActiveTaskLocked(taskId);
        }
    }

    public void markFailed(String taskId, BatchExperimentResponse result, String failureMessage) {
        synchronized (monitor) {
            BatchExperimentTaskState taskState = getExistingTaskLocked(taskId);
            taskState.status = STATUS_FAILED;
            taskState.message = (failureMessage == null || failureMessage.trim().isEmpty())
                    ? "批量生成执行失败"
                    : failureMessage.trim();
            taskState.finishedAt = currentDateTimeText();
            taskState.currentItem = null;
            taskState.cancelRequested = false;
            taskState.result = copyResult(result);
            if (result != null) {
                int completedCount = result.getItems() == null ? taskState.completedCount : result.getItems().size();
                taskState.completedCount = Math.max(completedCount, 0);
                taskState.successCount = safeInt(result.getSuccessCount(), taskState.successCount);
                taskState.failedCount = safeInt(result.getFailedCount(), taskState.failedCount);
            }
            clearActiveTaskLocked(taskId);
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    private BatchExperimentTaskState getExistingTaskLocked(String taskId) {
        BatchExperimentTaskState taskState = taskStore.get(taskId);
        if (taskState == null) {
            throw new CustomException("404", "未找到对应的批量生成任务");
        }
        return taskState;
    }

    private boolean hasActiveTaskLocked() {
        if (activeTaskId == null || activeTaskId.trim().isEmpty()) {
            return false;
        }
        BatchExperimentTaskState taskState = taskStore.get(activeTaskId);
        return taskState != null && isActiveStatus(taskState.status);
    }

    private boolean isActiveStatus(String status) {
        return STATUS_RUNNING.equals(status) || STATUS_TERMINATING.equals(status);
    }

    private void clearActiveTaskLocked(String taskId) {
        if (taskId != null && taskId.equals(activeTaskId)) {
            activeTaskId = null;
        }
    }

    private void trimTaskHistoryLocked() {
        while (taskStore.size() > MAX_TASK_HISTORY) {
            String oldestTaskId = taskStore.keySet().iterator().next();
            if (oldestTaskId.equals(activeTaskId)) {
                break;
            }
            taskStore.remove(oldestTaskId);
        }
    }

    private String resolveTerminateDeniedMessage(String status) {
        if (STATUS_COMPLETED.equals(status)) {
            return "当前任务已完成，无需终止";
        }
        if (STATUS_TERMINATED.equals(status)) {
            return "当前任务已终止";
        }
        if (STATUS_FAILED.equals(status)) {
            return "当前任务已失败";
        }
        return "当前任务已接近完成，终止未生效";
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private BatchExperimentRequest copyRequest(BatchExperimentRequest request) {
        return deepCopy(request, BatchExperimentRequest.class);
    }

    private BatchExperimentResponse copyResult(BatchExperimentResponse result) {
        return deepCopy(result, BatchExperimentResponse.class);
    }

    private BatchExperimentTaskCurrentItem copyCurrentItem(BatchExperimentTaskCurrentItem currentItem) {
        return deepCopy(currentItem, BatchExperimentTaskCurrentItem.class);
    }

    private <T> T deepCopy(T source, Class<T> type) {
        if (source == null) {
            return null;
        }
        return objectMapper.convertValue(source, type);
    }

    private String currentDateTimeText() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static class BatchExperimentTaskState {
        private String taskId;
        private String batchNo;
        private String status;
        private String message;
        private boolean cancelRequested;
        private int totalTaskCount;
        private int completedCount;
        private int successCount;
        private int failedCount;
        private String startedAt;
        private String finishedAt;
        private BatchExperimentTaskCurrentItem currentItem;
        private BatchExperimentRequest requestSnapshot;
        private BatchExperimentResponse result;

        private BatchExperimentTaskSnapshot toSnapshot(ObjectMapper objectMapper) {
            BatchExperimentTaskSnapshot snapshot = new BatchExperimentTaskSnapshot();
            snapshot.setTaskId(taskId);
            snapshot.setBatchNo(batchNo);
            snapshot.setStatus(status);
            snapshot.setStatusText(resolveStatusText(status));
            snapshot.setMessage(message);
            snapshot.setCanTerminate(isActiveStatus(status) && !cancelRequested);
            snapshot.setTerminateRequested(cancelRequested);
            snapshot.setGenerationLocked(isActiveStatus(status));
            snapshot.setTotalTaskCount(totalTaskCount);
            snapshot.setCompletedCount(completedCount);
            int runningCount = isActiveStatus(status) && currentItem != null ? 1 : 0;
            snapshot.setRunningCount(runningCount);
            snapshot.setRemainingCount(Math.max(totalTaskCount - completedCount - runningCount, 0));
            snapshot.setProgressPercent(resolveProgressPercent(totalTaskCount, completedCount, status));
            snapshot.setSuccessCount(successCount);
            snapshot.setFailedCount(failedCount);
            snapshot.setStartedAt(startedAt);
            snapshot.setFinishedAt(finishedAt);
            snapshot.setCurrentItem(currentItem == null ? null : objectMapper.convertValue(currentItem, BatchExperimentTaskCurrentItem.class));
            snapshot.setRequestSnapshot(requestSnapshot == null ? null : objectMapper.convertValue(requestSnapshot, BatchExperimentRequest.class));
            snapshot.setResult(result == null ? null : objectMapper.convertValue(result, BatchExperimentResponse.class));
            return snapshot;
        }

        private boolean isActiveStatus(String currentStatus) {
            return STATUS_RUNNING.equals(currentStatus) || STATUS_TERMINATING.equals(currentStatus);
        }

        private int resolveProgressPercent(int total, int completed, String currentStatus) {
            if (total <= 0) {
                return 0;
            }
            if (STATUS_COMPLETED.equals(currentStatus)) {
                return 100;
            }
            return (int) Math.round((completed * 100.0d) / total);
        }

        private String resolveStatusText(String currentStatus) {
            if (STATUS_RUNNING.equals(currentStatus)) {
                return "批量生成中";
            }
            if (STATUS_TERMINATING.equals(currentStatus)) {
                return "批量生成终止中";
            }
            if (STATUS_COMPLETED.equals(currentStatus)) {
                return "批量生成已完成";
            }
            if (STATUS_TERMINATED.equals(currentStatus)) {
                return "批量生成已终止";
            }
            if (STATUS_FAILED.equals(currentStatus)) {
                return "批量生成失败";
            }
            return "未知状态";
        }
    }
}
