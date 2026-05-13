import request from "@/utils/request.js";

const STORAGE_KEY = "__XM_BATCH_EXPERIMENT_TASK__";
const ACTIVE_STATUSES = ["RUNNING", "TERMINATING"];
const FINISHED_STATUSES = ["COMPLETED", "TERMINATED", "FAILED"];

const cloneValue = (value) => {
  if (value === null || value === undefined) {
    return value;
  }

  try {
    return JSON.parse(JSON.stringify(value));
  } catch (e) {
    return value;
  }
};

const normalizeRequestSnapshot = (requestSnapshot) => {
  if (!requestSnapshot) {
    return null;
  }

  return {
    ...requestSnapshot,
    functionIds: Array.isArray(requestSnapshot.functionIds) ? [...requestSnapshot.functionIds] : [],
    modelConfigIds: Array.isArray(requestSnapshot.modelConfigIds) ? [...requestSnapshot.modelConfigIds] : [],
    strategies: Array.isArray(requestSnapshot.strategies) ? [...requestSnapshot.strategies] : []
  };
};

export const normalizeBatchExperimentTask = (task) => {
  if (!task) {
    return null;
  }

  return {
    ...task,
    canTerminate: !!task.canTerminate,
    terminateRequested: !!task.terminateRequested,
    generationLocked: !!task.generationLocked,
    totalTaskCount: Number(task.totalTaskCount || 0),
    completedCount: Number(task.completedCount || 0),
    runningCount: Number(task.runningCount || 0),
    remainingCount: Number(task.remainingCount || 0),
    progressPercent: Number(task.progressPercent || 0),
    successCount: Number(task.successCount || 0),
    failedCount: Number(task.failedCount || 0),
    currentItem: task.currentItem ? { ...task.currentItem } : null,
    requestSnapshot: normalizeRequestSnapshot(task.requestSnapshot),
    result: cloneValue(task.result || null)
  };
};

export const readStoredBatchExperimentTask = () => {
  try {
    return normalizeBatchExperimentTask(JSON.parse(localStorage.getItem(STORAGE_KEY) || "null"));
  } catch (e) {
    return null;
  }
};

export const writeStoredBatchExperimentTask = (task) => {
  const normalizedTask = normalizeBatchExperimentTask(task);
  if (!normalizedTask) {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(normalizedTask));
  return normalizedTask;
};

export const clearStoredBatchExperimentTask = () => {
  localStorage.removeItem(STORAGE_KEY);
};

export const isBatchExperimentTaskActive = (task) => {
  return ACTIVE_STATUSES.includes(task?.status);
};

export const isBatchExperimentTaskFinished = (task) => {
  return FINISHED_STATUSES.includes(task?.status);
};

export const getBatchExperimentLockMessage = (task) => {
  return task?.message || "当前已有批量生成任务正在执行，请等待完成或先终止当前任务";
};

const normalizeApiTaskResponse = (res) => {
  if (!(res.code === "200" || res.code === 200)) {
    throw new Error(res.msg || "批量生成任务状态获取失败");
  }

  const normalizedTask = normalizeBatchExperimentTask(res.data || null);
  if (normalizedTask) {
    writeStoredBatchExperimentTask(normalizedTask);
  } else {
    clearStoredBatchExperimentTask();
  }
  return normalizedTask;
};

export const fetchCurrentBatchExperimentTask = async () => {
  const res = await request.get("/generate/batchExperiment/current");
  return normalizeApiTaskResponse(res);
};

export const fetchBatchExperimentTaskById = async (taskId) => {
  if (!taskId) {
    clearStoredBatchExperimentTask();
    return null;
  }

  const res = await request.get(`/generate/batchExperiment/${taskId}`);
  return normalizeApiTaskResponse(res);
};

export const resolveLatestBatchExperimentTask = async () => {
  const currentTask = await fetchCurrentBatchExperimentTask();
  if (currentTask) {
    return currentTask;
  }

  const storedTask = readStoredBatchExperimentTask();
  if (!storedTask?.taskId) {
    return null;
  }

  try {
    return await fetchBatchExperimentTaskById(storedTask.taskId);
  } catch (e) {
    clearStoredBatchExperimentTask();
    return null;
  }
};
