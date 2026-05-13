const RUNTIME_CACHE_ROOT_KEY = "__XM_RUNTIME_PAGE_CACHE__";

export const RUNTIME_PAGE_CACHE_KEYS = {
  generatePageState: "manager.generate.pageState",
  generateIncomingSource: "manager.generate.incomingSource",
  batchExperimentPageState: "manager.batchExperiment.pageState",
  batchExperimentIncomingFunctions: "manager.batchExperiment.incomingFunctions"
};

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

const getRuntimeCacheRoot = () => {
  if (typeof window === "undefined") {
    return {};
  }

  if (!window[RUNTIME_CACHE_ROOT_KEY]) {
    window[RUNTIME_CACHE_ROOT_KEY] = {};
  }

  return window[RUNTIME_CACHE_ROOT_KEY];
};

export const readRuntimePageCache = (key, defaultValue = null) => {
  const cacheRoot = getRuntimeCacheRoot();
  if (!(key in cacheRoot)) {
    return cloneValue(defaultValue);
  }
  return cloneValue(cacheRoot[key]);
};

export const writeRuntimePageCache = (key, value) => {
  const cacheRoot = getRuntimeCacheRoot();
  cacheRoot[key] = cloneValue(value);
};

export const clearRuntimePageCache = (key) => {
  const cacheRoot = getRuntimeCacheRoot();
  delete cacheRoot[key];
};

export const consumeRuntimePageCache = (key, defaultValue = null) => {
  const value = readRuntimePageCache(key, defaultValue);
  clearRuntimePageCache(key);
  return value;
};
