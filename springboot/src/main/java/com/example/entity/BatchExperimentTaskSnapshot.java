package com.example.entity;

public class BatchExperimentTaskSnapshot {
    private String taskId;
    private String batchNo;
    private String status;
    private String statusText;
    private String message;
    private Boolean canTerminate;
    private Boolean terminateRequested;
    private Boolean generationLocked;
    private Integer totalTaskCount;
    private Integer completedCount;
    private Integer runningCount;
    private Integer remainingCount;
    private Integer progressPercent;
    private Integer successCount;
    private Integer failedCount;
    private String startedAt;
    private String finishedAt;
    private BatchExperimentTaskCurrentItem currentItem;
    private BatchExperimentRequest requestSnapshot;
    private BatchExperimentResponse result;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getCanTerminate() {
        return canTerminate;
    }

    public void setCanTerminate(Boolean canTerminate) {
        this.canTerminate = canTerminate;
    }

    public Boolean getTerminateRequested() {
        return terminateRequested;
    }

    public void setTerminateRequested(Boolean terminateRequested) {
        this.terminateRequested = terminateRequested;
    }

    public Boolean getGenerationLocked() {
        return generationLocked;
    }

    public void setGenerationLocked(Boolean generationLocked) {
        this.generationLocked = generationLocked;
    }

    public Integer getTotalTaskCount() {
        return totalTaskCount;
    }

    public void setTotalTaskCount(Integer totalTaskCount) {
        this.totalTaskCount = totalTaskCount;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public Integer getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(Integer runningCount) {
        this.runningCount = runningCount;
    }

    public Integer getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(Integer remainingCount) {
        this.remainingCount = remainingCount;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public BatchExperimentTaskCurrentItem getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(BatchExperimentTaskCurrentItem currentItem) {
        this.currentItem = currentItem;
    }

    public BatchExperimentRequest getRequestSnapshot() {
        return requestSnapshot;
    }

    public void setRequestSnapshot(BatchExperimentRequest requestSnapshot) {
        this.requestSnapshot = requestSnapshot;
    }

    public BatchExperimentResponse getResult() {
        return result;
    }

    public void setResult(BatchExperimentResponse result) {
        this.result = result;
    }
}
