<template>
  <div class="batch-page">
    <div class="card config-card">
      <div class="page-title">批量生成</div>

      <div v-if="batchTaskSnapshot" class="task-status-panel" :class="{ 'task-status-panel--active': isExecutionLocked }">
        <div class="task-status-header">
          <div>
            <div class="task-status-title">{{ taskStatusTitle }}</div>
            <div class="task-status-desc">{{ taskStatusDescription }}</div>
          </div>
          <div class="task-status-actions">
            <el-tag :type="taskStatusTagType" effect="light">{{ batchTaskSnapshot.statusText }}</el-tag>
            <el-button
                v-if="batchTaskSnapshot.canTerminate"
                type="danger"
                plain
                @click="terminateBatchExperiment"
            >
              终止批量生成
            </el-button>
          </div>
        </div>

        <el-progress
            :percentage="taskProgressPercent"
            :status="taskProgressStatus"
            :stroke-width="16"
        />

        <div class="task-metrics">
          <span>总任务数 {{ taskSummaryMetrics.total }}</span>
          <span>已完成 {{ taskSummaryMetrics.completed }}</span>
          <span>当前执行 {{ taskSummaryMetrics.running }}</span>
          <span>剩余 {{ taskSummaryMetrics.remaining }}</span>
          <span>成功 {{ taskSummaryMetrics.success }}</span>
          <span>失败 {{ taskSummaryMetrics.failed }}</span>
        </div>

        <div v-if="taskCurrentItem" class="task-current-card">
          <div>当前正在生成第 {{ taskCurrentItem.currentIndex }} / {{ taskCurrentItem.totalTaskCount }} 条</div>
          <div>当前函数：{{ taskCurrentItem.functionName || "—" }}</div>
          <div>当前策略：{{ getStrategyLabel(taskCurrentItem.strategy) }}</div>
          <div>当前轮次：{{ formatIterationRound(taskCurrentItem.runIndex) }}</div>
        </div>
      </div>

      <el-form
          :model="data.form"
          :disabled="isExecutionLocked"
          label-width="100px"
          class="batch-form"
          :class="{ 'batch-form--locked': isExecutionLocked }"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="选择模型">
              <el-select
                  v-model="data.form.modelConfigIds"
                  multiple
                  filterable
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="请选择至少一个模型"
                  style="width: 100%"
              >
                <el-option
                    v-for="item in data.enabledModelList"
                    :key="item.id"
                    :label="item.modelName"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="评估模型">
              <el-select
                  v-model="data.form.evaluationModelConfigId"
                  filterable
                  clearable
                  placeholder="可为空，默认使用当前评估模型"
                  style="width: 100%"
              >
                <el-option
                    v-for="item in data.enabledModelList"
                    :key="item.id"
                    :label="item.modelName"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="生成策略">
              <el-select
                  v-model="data.form.strategies"
                  multiple
                  placeholder="请选择策略"
                  style="width: 100%"
              >
                <el-option label="普通生成" value="normal" />
                <el-option label="链式分析生成" value="cot" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="6">
            <el-form-item label="输出语言">
              <el-select v-model="data.form.outputLanguage" style="width: 100%">
                <el-option label="中文" value="zh" />
                <el-option label="English" value="en" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="6">
            <el-form-item label="迭代轮次">
              <el-input-number v-model="data.form.runCount" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="选择函数">
          <el-select
              v-model="data.form.functionIds"
              multiple
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择至少一个函数"
              style="width: 100%"
          >
            <el-option
                v-for="item in data.functionList"
                :key="item.id"
                :label="buildFunctionLabel(item)"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <div class="selected-summary">
          <div>已选函数：{{ data.form.functionIds.length }} 个</div>
          <div>已选模型：{{ data.form.modelConfigIds.length }} 个</div>
          <div>策略：{{ selectedStrategyLabelsText }}</div>
          <div>预计生成记录：{{ estimatedTaskCount }} 条</div>
        </div>

        <div class="selected-functions" v-if="selectedFunctionPreview.length > 0">
          <span class="preview-label">函数预览：</span>
          <span v-for="item in selectedFunctionPreview" :key="item.id" class="preview-tag">
            {{ item.functionName }}
          </span>
          <span v-if="data.form.functionIds.length > 8" class="preview-more">共 {{ data.form.functionIds.length }} 个</span>
        </div>

        <div v-if="data.prefilledFromFunctionPage && selectedFunctionDetails.length > 0" class="prefill-tip">
          已从函数管理页面自动带入 {{ selectedFunctionDetails.length }} 个函数，你可以先确认列表，再删除单个函数或清空后重新选择。
        </div>

        <div class="selected-function-panel">
          <div class="selected-function-header">
            <div>
              <div class="selected-function-title">批量生成函数列表</div>
              <div class="selected-function-desc">正式发起批量生成前，可在这里确认、删除或清空后重新选择。</div>
            </div>
            <el-button
                type="danger"
                link
                :disabled="isExecutionLocked || selectedFunctionDetails.length === 0"
                @click="clearSelectedFunctions"
            >
              清空后重新选择
            </el-button>
          </div>

          <el-table
              v-if="selectedFunctionDetails.length > 0"
              :data="selectedFunctionDetails"
              stripe
              max-height="260"
              style="width: 100%"
          >
            <el-table-column prop="id" label="编号" width="90" align="center" />
            <el-table-column prop="functionName" label="函数名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="className" label="所属类名" min-width="140" show-overflow-tooltip />
            <el-table-column prop="language" label="语言" width="100" align="center" />
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="100" align="center">
              <template #default="scope">
                <el-button type="danger" link :disabled="isExecutionLocked" @click="removeSelectedFunction(scope.row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else class="selected-function-empty">
            暂无已选函数。你可以在上方手动选择，或先回到函数管理页面勾选后再进入这里。
          </div>
        </div>

        <div class="form-actions">
          <el-button type="primary" :loading="data.running" :disabled="isExecutionLocked" @click="startBatchExperiment">
            {{ startButtonText }}
          </el-button>
          <el-button type="primary" plain :disabled="data.running || isExecutionLocked" @click="resetForm">重置</el-button>
        </div>
        <div v-if="isExecutionLocked" class="config-lock-mask">
          <div class="config-lock-mask__title">批量生成执行中</div>
          <div class="config-lock-mask__desc">当前任务未完成，配置区域已锁定，请等待完成或先终止当前任务。</div>
        </div>
      </el-form>
    </div>

    <div class="card result-card" v-if="data.result">
      <div class="result-header">
        <div>
          <div class="page-title">批量生成结果</div>
          <div class="result-meta">
            <el-tag type="warning" effect="light">批次号：{{ data.result.batchNo }}</el-tag>
            <span class="result-meta-text">输出语言：{{ getOutputLanguageLabel(data.result.outputLanguage) }}</span>
            <span class="result-meta-text">迭代轮次：{{ data.result.runCount || 0 }}</span>
          </div>
        </div>
        <el-button
            type="success"
            :disabled="sortedFilteredResultItems.length === 0"
            @click="exportBatchResult"
        >
          导出当前结果
        </el-button>
      </div>

      <div v-if="false" class="summary-grid">
        <div class="summary-item">
          <div class="summary-label">函数总数</div>
          <div class="summary-value">{{ overallFunctionCount }}</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">实验任务数</div>
          <div class="summary-value">{{ data.result.totalTaskCount ?? resultItems.length }}</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">生成成功数</div>
          <div class="summary-value success">{{ data.result.successCount ?? 0 }}</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">生成失败数</div>
          <div class="summary-value danger">{{ data.result.failedCount ?? 0 }}</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">平均评估分数</div>
          <div class="summary-value">{{ formatScore(data.result.averageEvaluationScore) }}</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">平均测试人员分数</div>
          <div class="summary-value">{{ formatScore(data.result.averageTesterScore) }}</div>
        </div>
        <div class="summary-item">
          <div class="summary-label">当前筛选记录</div>
          <div class="summary-value">{{ sortedFilteredResultItems.length }}</div>
        </div>
      </div>

      <div class="comparison-section" v-if="false">
        <div class="section-header">
          <div class="section-title">模型对比统计</div>
          <div class="section-tip">基于当前筛选结果</div>
        </div>

        <el-table :data="resultModelStats" stripe style="width: 100%">
          <el-table-column prop="modelName" label="模型名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="totalCount" label="记录数" width="100" align="center" />
          <el-table-column prop="successCount" label="成功数" width="100" align="center" />
          <el-table-column prop="failedCount" label="失败数" width="100" align="center" />
          <el-table-column label="平均评估分数" width="130" align="center">
            <template #default="scope">
              {{ formatScore(scope.row.averageEvaluationScore) }}
            </template>
          </el-table-column>
          <el-table-column label="平均测试人员分数" width="150" align="center">
            <template #default="scope">
              {{ formatScore(scope.row.averageTesterScore) }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="result-filter-bar">
        <el-input
            v-model="data.resultFilter.functionName"
            placeholder="按函数名称筛选"
            clearable
            style="width: 220px"
        />
        <el-input
            v-model="data.resultFilter.modelName"
            placeholder="按模型名称筛选"
            clearable
            style="width: 220px"
        />
        <el-select
            v-model="data.resultFilter.status"
            clearable
            placeholder="按生成状态筛选"
            style="width: 180px"
        >
          <el-option label="成功" value="成功" />
          <el-option label="失败" value="失败" />
        </el-select>
        <el-button plain @click="resetResultFilters">重置筛选</el-button>
      </div>

      <div class="result-table-wrapper">
        <el-table
            :data="pagedResultItems"
            stripe
            style="width: 100%"
            @sort-change="handleResultSortChange"
        >
          <el-table-column prop="functionName" label="函数名称" min-width="150" show-overflow-tooltip sortable="custom" />
          <el-table-column prop="className" label="所属类名" min-width="130" show-overflow-tooltip />
          <el-table-column prop="language" label="编程语言" width="110" align="center" sortable="custom" />
          <el-table-column prop="modelName" label="所用模型" min-width="150" show-overflow-tooltip sortable="custom" />
          <el-table-column label="生成策略" width="130" align="center">
            <template #default="scope">
              {{ getStrategyLabel(scope.row.strategy) }}
            </template>
          </el-table-column>
          <el-table-column prop="runIndex" label="轮次" width="96" align="center" sortable="custom">
            <template #default="scope">
              {{ formatIterationRound(scope.row.runIndex) }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="生成时间" min-width="170" align="center" sortable="custom" />
          <el-table-column label="生成状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === '成功' ? 'success' : 'danger'" effect="light">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="evaluationScore" label="评估分数" width="110" align="center" sortable="custom">
            <template #default="scope">
              {{ formatScore(scope.row.evaluationScore) }}
            </template>
          </el-table-column>
          <el-table-column prop="testerEvaluationScore" label="测试人员评分" width="130" align="center" sortable="custom">
            <template #default="scope">
              {{ formatScore(scope.row.testerEvaluationScore) }}
            </template>
          </el-table-column>
          <el-table-column prop="message" label="结果说明" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="110" fixed="right" align="center">
            <template #default="scope">
              <el-button type="primary" link @click="openResultDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper" v-if="sortedFilteredResultItems.length > data.resultPageSize">
        <el-pagination
            background
            layout="prev, pager, next, total"
            :total="sortedFilteredResultItems.length"
            :page-size="data.resultPageSize"
            :current-page="data.resultCurrentPage"
            @current-change="handleResultCurrentChange"
        />
      </div>
    </div>

    <el-dialog
        v-model="data.detailVisible"
        title="实验记录详情"
        width="960px"
        destroy-on-close
    >
      <div v-if="data.detailItem" class="detail-dialog">
        <div v-if="data.detailLoading" class="detail-loading-text">
          正在根据记录ID加载完整生成内容与评估明细...
        </div>

        <div class="detail-overview">
          <div class="detail-overview-item">
            <div class="detail-overview-label">生成状态</div>
            <div class="detail-overview-value">
              <el-tag :type="data.detailItem.status === '成功' ? 'success' : 'danger'" effect="light">
                {{ data.detailItem.status || "未生成" }}
              </el-tag>
            </div>
          </div>
          <div class="detail-overview-item">
            <div class="detail-overview-label">评估状态</div>
            <div class="detail-overview-value">
              <el-tag :type="detailEvaluationStatus.type" effect="light">
                {{ detailEvaluationStatus.text }}
              </el-tag>
            </div>
          </div>
          <div class="detail-overview-item">
            <div class="detail-overview-label">评估分数</div>
            <div class="detail-overview-value">{{ formatDetailScore(data.detailItem) }}</div>
          </div>
          <div class="detail-overview-item">
            <div class="detail-overview-label">测试人员评分</div>
            <div class="detail-overview-value">{{ formatScore(data.detailItem.testerEvaluationScore) }}</div>
          </div>
          <div class="detail-overview-item">
            <div class="detail-overview-label">生成用例数</div>
            <div class="detail-overview-value">{{ data.detailItem.generatedCaseCount ?? 0 }}</div>
          </div>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">基础信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="记录ID">{{ data.detailItem.recordId || "—" }}</el-descriptions-item>
            <el-descriptions-item label="函数名称">{{ data.detailItem.functionName || "—" }}</el-descriptions-item>
            <el-descriptions-item label="所属类名">{{ data.detailItem.className || "—" }}</el-descriptions-item>
            <el-descriptions-item label="编程语言">{{ data.detailItem.language || "—" }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ data.detailItem.remark || "—" }}</el-descriptions-item>
            <el-descriptions-item label="所用模型">{{ data.detailItem.modelName || "—" }}</el-descriptions-item>
            <el-descriptions-item label="生成策略">{{ getStrategyLabel(data.detailItem.strategy) }}</el-descriptions-item>
            <el-descriptions-item label="迭代轮次">{{ formatIterationRound(data.detailItem.runIndex) }}</el-descriptions-item>
            <el-descriptions-item label="生成时间">{{ data.detailItem.createTime || "—" }}</el-descriptions-item>
            <el-descriptions-item label="上一轮记录ID">{{ data.detailItem.previousRecordId ?? "—" }}</el-descriptions-item>
            <el-descriptions-item label="结果说明">{{ data.detailItem.message || "—" }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">生成测试用例文本</div>
          <pre class="detail-pre">{{ data.detailItem.resultText || "暂无生成内容" }}</pre>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">评估结果</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="正常路径覆盖">{{ getEvaluationFieldDisplay(data.detailItem, "normalPathCoverage", "正常路径覆盖") }}</el-descriptions-item>
            <el-descriptions-item label="边界条件覆盖">{{ getEvaluationFieldDisplay(data.detailItem, "boundaryCoverage", "边界条件覆盖") }}</el-descriptions-item>
            <el-descriptions-item label="异常分支覆盖">{{ getEvaluationFieldDisplay(data.detailItem, "exceptionCoverage", "异常分支覆盖") }}</el-descriptions-item>
            <el-descriptions-item label="语法规范性">{{ getEvaluationFieldDisplay(data.detailItem, "syntaxNorm", "语法规范性") }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">改进建议</div>
          <div v-if="detailSuggestionList.length > 0" class="detail-suggestion-list">
            <div
                v-for="(item, index) in detailSuggestionList"
                :key="`${data.detailItem.recordId || 'detail'}-${index}`"
                class="detail-suggestion-item"
            >
              {{ index + 1 }}. {{ item }}
            </div>
          </div>
          <div v-else class="detail-empty">暂无改进建议</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import * as XLSX from "xlsx";
import request from "@/utils/request.js";
import {
  clearStoredBatchExperimentTask,
  fetchBatchExperimentTaskById,
  getBatchExperimentLockMessage,
  isBatchExperimentTaskActive,
  normalizeBatchExperimentTask,
  readStoredBatchExperimentTask,
  resolveLatestBatchExperimentTask,
  writeStoredBatchExperimentTask
} from "@/utils/batchExperimentTask.js";
import {
  RUNTIME_PAGE_CACHE_KEYS,
  consumeRuntimePageCache,
  readRuntimePageCache,
  writeRuntimePageCache
} from "@/utils/runtimePageCache.js";

const BATCH_EXPERIMENT_FUNCTIONS_KEY = "batchExperimentSelectedFunctions";
const currentUser = JSON.parse(localStorage.getItem("xm-user") || "{}");
const currentRole = currentUser.loginType === "USER" ? "USER" : currentUser.role;
let taskPollingTimer = null;

const STRATEGY_LABEL_MAP = {
  normal: "普通生成",
  cot: "链式分析生成"
};

const OUTPUT_LANGUAGE_LABEL_MAP = {
  zh: "中文",
  en: "English"
};

const getEvaluationModelStorageKey = () => {
  return `evaluation-default-model-${currentUser.loginType || ""}-${currentRole}-${currentUser.id || ""}`;
};

const readStoredEvaluationModel = () => {
  try {
    return JSON.parse(localStorage.getItem(getEvaluationModelStorageKey()) || "null");
  } catch (e) {
    return null;
  }
};

const buildDefaultBatchForm = () => ({
  modelConfigIds: [],
  evaluationModelConfigId: null,
  strategies: ["normal", "cot"],
  outputLanguage: "zh",
  runCount: 3,
  functionIds: []
});

const data = reactive({
  running: false,
  batchTask: readStoredBatchExperimentTask(),
  enabledModelList: [],
  functionList: [],
  prefilledFunctions: [],
  prefilledFromFunctionPage: false,
  result: null,
  resultCurrentPage: 1,
  resultPageSize: 8,
  resultFilter: {
    functionName: "",
    modelName: "",
    status: ""
  },
  resultSort: {
    prop: "createTime",
    order: "descending"
  },
  detailVisible: false,
  detailItem: null,
  detailLoading: false,
  detailRequestKey: 0,
  form: buildDefaultBatchForm()
});

const saveBatchExperimentPageState = () => {
  writeRuntimePageCache(RUNTIME_PAGE_CACHE_KEYS.batchExperimentPageState, {
    form: data.form,
    taskSnapshot: data.batchTask,
    prefilledFunctions: data.prefilledFunctions,
    prefilledFromFunctionPage: data.prefilledFromFunctionPage,
    result: data.result,
    resultCurrentPage: data.resultCurrentPage,
    resultPageSize: data.resultPageSize,
    resultFilter: data.resultFilter,
    resultSort: data.resultSort
  });
};

const restoreBatchExperimentPageState = () => {
  const cache = readRuntimePageCache(RUNTIME_PAGE_CACHE_KEYS.batchExperimentPageState);
  if (!cache) {
    return false;
  }

  const cachedForm = cache.form || {};
  data.form = {
    ...buildDefaultBatchForm(),
    ...cachedForm,
    modelConfigIds: Array.isArray(cachedForm.modelConfigIds) ? [...cachedForm.modelConfigIds] : [],
    strategies: Array.isArray(cachedForm.strategies) ? [...cachedForm.strategies] : ["normal", "cot"],
    functionIds: Array.isArray(cachedForm.functionIds) ? [...cachedForm.functionIds] : []
  };
  data.batchTask = normalizeBatchExperimentTask(cache.taskSnapshot || readStoredBatchExperimentTask());
  data.prefilledFunctions = Array.isArray(cache.prefilledFunctions) ? [...cache.prefilledFunctions] : [];
  data.prefilledFromFunctionPage = !!cache.prefilledFromFunctionPage;
  data.result = normalizeBatchResultPayload(cache.result || null);
  data.resultCurrentPage = cache.resultCurrentPage || 1;
  data.resultPageSize = cache.resultPageSize || 8;
  data.resultFilter = {
    functionName: "",
    modelName: "",
    status: "",
    ...(cache.resultFilter || {})
  };
  data.resultSort = {
    prop: "createTime",
    order: "descending",
    ...(cache.resultSort || {})
  };
  data.detailVisible = false;
  data.detailItem = null;
  data.detailLoading = false;
  data.detailRequestKey = 0;
  data.running = false;
  return true;
};

const syncBatchModelSelections = () => {
  const availableModels = data.enabledModelList || [];
  const availableModelIds = new Set(availableModels.map(item => String(item.id)));

  data.form.modelConfigIds = data.form.modelConfigIds.filter(id => availableModelIds.has(String(id)));
  if (data.form.modelConfigIds.length === 0 && availableModels.length > 0) {
    data.form.modelConfigIds = [availableModels[0].id];
  }

  if (availableModelIds.has(String(data.form.evaluationModelConfigId))) {
    return;
  }

  const cached = readStoredEvaluationModel();
  if (cached?.id) {
    const matched = availableModels.find(item => String(item.id) === String(cached.id));
    data.form.evaluationModelConfigId = matched ? matched.id : null;
  } else {
    data.form.evaluationModelConfigId = null;
  }
  saveBatchExperimentPageState();
};

const estimatedTaskCount = computed(() => {
  return data.form.functionIds.length * data.form.modelConfigIds.length * data.form.strategies.length * (data.form.runCount || 0);
});

const getStrategyLabel = (strategy) => {
  return STRATEGY_LABEL_MAP[strategy] || strategy || "未选择";
};

const getOutputLanguageLabel = (language) => {
  return OUTPUT_LANGUAGE_LABEL_MAP[language] || language || "—";
};

const formatScore = (score) => {
  return score === null || score === undefined || score === "" ? "—" : score;
};

const formatIterationRound = (runIndex) => {
  return runIndex === null || runIndex === undefined || runIndex === "" ? "—" : `第${runIndex}轮`;
};

const buildSuggestionList = (suggestions, suggestionText = "") => {
  if (Array.isArray(suggestions) && suggestions.length > 0) {
    return suggestions
        .map(item => String(item || "").trim())
        .filter(Boolean);
  }

  const text = String(suggestionText || "").trim();
  if (!text) {
    return [];
  }

  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) {
      return parsed
          .map(item => String(item || "").trim())
          .filter(Boolean);
    }
  } catch (e) {
    // Fallback to plain text splitting when suggestions are not stored as JSON.
  }

  return text
      .split(/\r?\n+/)
      .map(item => item.trim())
      .filter(Boolean);
};

const normalizeBatchResultItem = (item = {}) => {
  const suggestionText = String(item.suggestionText || "").trim();
  return {
    ...item,
    resultText: item.resultText || "",
    createTime: item.createTime || "",
    normalPathCoverage: item.normalPathCoverage || "",
    boundaryCoverage: item.boundaryCoverage || "",
    exceptionCoverage: item.exceptionCoverage || "",
    syntaxNorm: item.syntaxNorm || "",
    suggestionText,
    suggestions: buildSuggestionList(item.suggestions, suggestionText)
  };
};

const normalizeBatchResultPayload = (result) => {
  if (!result) {
    return null;
  }

  return {
    ...result,
    items: Array.isArray(result.items) ? result.items.map(item => normalizeBatchResultItem(item)) : []
  };
};

const hasEvaluationContent = (item = {}) => {
  return item.evaluationScore !== null && item.evaluationScore !== undefined && item.evaluationScore !== ""
      || !!String(item.normalPathCoverage || "").trim()
      || !!String(item.boundaryCoverage || "").trim()
      || !!String(item.exceptionCoverage || "").trim()
      || !!String(item.syntaxNorm || "").trim()
      || buildSuggestionList(item.suggestions, item.suggestionText).length > 0;
};

const getEvaluationStatusMeta = (item = {}) => {
  const hasScore = item.evaluationScore !== null && item.evaluationScore !== undefined && item.evaluationScore !== "";
  const hasContent = hasEvaluationContent(item);

  if (hasScore && hasContent) {
    return { code: "completed", text: "评估已完成", type: "success" };
  }
  if (hasContent) {
    return { code: "partial", text: "评估已完成，分数未返回", type: "warning" };
  }
  if (item.status === "成功") {
    return { code: "pending", text: "评估结果待补齐", type: "warning" };
  }
  if (item.status === "失败") {
    return { code: "skipped", text: "未执行评估", type: "info" };
  }
  return { code: "pending", text: "评估未完成", type: "info" };
};

const formatDetailScore = (item = {}) => {
  const meta = getEvaluationStatusMeta(item);
  if (item.evaluationScore !== null && item.evaluationScore !== undefined && item.evaluationScore !== "") {
    return item.evaluationScore;
  }
  if (meta.code === "partial") {
    return "模型未返回分数";
  }
  if (meta.code === "completed") {
    return "已完成";
  }
  return "待评估";
};

const getEvaluationFieldDisplay = (item = {}, fieldName = "", fieldLabel = "") => {
  const value = String(item?.[fieldName] || "").trim();
  if (value) {
    return value;
  }

  const meta = getEvaluationStatusMeta(item);
  if (meta.code === "partial") {
    return `评估已完成，但${fieldLabel}未返回内容`;
  }
  if (meta.code === "pending") {
    return "评估结果待补齐，请稍后重新查看详情";
  }
  if (meta.code === "skipped") {
    return "本条记录未执行评估，请先检查生成失败原因";
  }
  return "当前暂无可展示的评估内容";
};

const buildDetailItem = (row = {}, record = null) => {
  const baseItem = normalizeBatchResultItem(JSON.parse(JSON.stringify(row || {})));
  const suggestionText = record?.suggestionText || baseItem.suggestionText || "";
  const suggestions = suggestionText
      ? buildSuggestionList([], suggestionText)
      : buildSuggestionList(baseItem.suggestions, "");

  return {
    ...baseItem,
    recordId: record?.id ?? baseItem.recordId ?? null,
    resultText: record?.resultText || baseItem.resultText || "",
    createTime: record?.createTime || baseItem.createTime || "",
    evaluationScore: record?.evaluationScore ?? baseItem.evaluationScore ?? null,
    normalPathCoverage: record?.normalPathCoverage || baseItem.normalPathCoverage || "",
    boundaryCoverage: record?.boundaryCoverage || baseItem.boundaryCoverage || "",
    exceptionCoverage: record?.exceptionCoverage || baseItem.exceptionCoverage || "",
    syntaxNorm: record?.syntaxNorm || baseItem.syntaxNorm || "",
    suggestionText,
    suggestions
  };
};

const detailEvaluationStatus = computed(() => {
  return getEvaluationStatusMeta(data.detailItem || {});
});

const detailSuggestionList = computed(() => {
  const detailItem = data.detailItem || {};
  const suggestions = buildSuggestionList(detailItem.suggestions, detailItem.suggestionText);
  if (suggestions.length > 0) {
    return suggestions;
  }

  const meta = getEvaluationStatusMeta(detailItem);
  if (meta.code === "completed" || meta.code === "partial") {
    return ["评估已执行，但当前记录未返回改进建议。"];
  }
  if (meta.code === "skipped") {
    return ["本条记录未执行评估，请优先处理生成失败原因。"];
  }
  return ["评估结果待补齐，请稍后重新查看详情。"];
});

const syncResultDetailCache = (detailItem) => {
  if (!detailItem?.recordId || !Array.isArray(data.result?.items)) {
    return;
  }

  const target = data.result.items.find(item => String(item.recordId) === String(detailItem.recordId));
  if (!target) {
    return;
  }

  Object.assign(target, {
    resultText: detailItem.resultText,
    createTime: detailItem.createTime,
    evaluationScore: detailItem.evaluationScore,
    normalPathCoverage: detailItem.normalPathCoverage,
    boundaryCoverage: detailItem.boundaryCoverage,
    exceptionCoverage: detailItem.exceptionCoverage,
    syntaxNorm: detailItem.syntaxNorm,
    suggestionText: detailItem.suggestionText,
    suggestions: detailItem.suggestions
  });
};

const normalizeFunctionId = (value) => {
  if (value === null || value === undefined || value === "") {
    return "";
  }
  return String(value);
};

const uniqueFunctionIds = (ids = []) => {
  const result = [];
  const idSet = new Set();

  ids.forEach(id => {
    const key = normalizeFunctionId(id);
    if (!key || idSet.has(key)) {
      return;
    }
    idSet.add(key);
    result.push(id);
  });

  return result;
};

const uniqueFunctionList = (list = []) => {
  const result = [];
  const idSet = new Set();

  list.forEach(item => {
    const key = normalizeFunctionId(item?.id);
    if (!key || idSet.has(key)) {
      return;
    }
    idSet.add(key);
    result.push(item);
  });

  return result;
};

const selectedStrategyLabelsText = computed(() => {
  return data.form.strategies.map(getStrategyLabel).join("、") || "未选择";
});

const selectedFunctionDetails = computed(() => {
  const functionMap = new Map(data.functionList.map(item => [normalizeFunctionId(item.id), item]));
  const prefilledMap = new Map(data.prefilledFunctions.map(item => [normalizeFunctionId(item.id), item]));

  return data.form.functionIds
      .map(id => functionMap.get(normalizeFunctionId(id)) || prefilledMap.get(normalizeFunctionId(id)))
      .filter(Boolean);
});

const selectedFunctionPreview = computed(() => {
  return selectedFunctionDetails.value.slice(0, 8);
});

const batchTaskSnapshot = computed(() => data.batchTask || null);

const isExecutionLocked = computed(() => {
  return isBatchExperimentTaskActive(batchTaskSnapshot.value);
});

const taskProgressPercent = computed(() => {
  const percent = Number(batchTaskSnapshot.value?.progressPercent || 0);
  if (Number.isNaN(percent)) {
    return 0;
  }
  return Math.min(Math.max(percent, 0), 100);
});

const taskCurrentItem = computed(() => batchTaskSnapshot.value?.currentItem || null);

const taskStatusTitle = computed(() => {
  return batchTaskSnapshot.value?.statusText || "批量生成状态";
});

const taskStatusDescription = computed(() => {
  const task = batchTaskSnapshot.value;
  if (!task) {
    return "";
  }
  if (isExecutionLocked.value) {
    return "当前任务未完成，页面已锁定，请勿重复提交或修改配置。";
  }
  if (task.status === "COMPLETED") {
    return "当前任务已完成，页面已恢复可编辑状态。";
  }
  if (task.status === "TERMINATED") {
    return "当前任务已终止，未执行部分已停止，已完成记录已保留。";
  }
  if (task.status === "FAILED") {
    return task.message || "批量生成执行失败，请检查后端日志或模型配置。";
  }
  return task.message || "当前任务状态已更新。";
});

const taskStatusTagType = computed(() => {
  const status = batchTaskSnapshot.value?.status;
  if (status === "COMPLETED") {
    return "success";
  }
  if (status === "TERMINATED") {
    return "warning";
  }
  if (status === "FAILED") {
    return "danger";
  }
  if (status === "TERMINATING") {
    return "warning";
  }
  return "primary";
});

const taskProgressStatus = computed(() => {
  const status = batchTaskSnapshot.value?.status;
  if (status === "COMPLETED") {
    return "success";
  }
  if (status === "FAILED") {
    return "exception";
  }
  if (status === "TERMINATED" || status === "TERMINATING") {
    return "warning";
  }
  return "";
});

const taskSummaryMetrics = computed(() => {
  const task = batchTaskSnapshot.value;
  return {
    total: Number(task?.totalTaskCount || 0),
    completed: Number(task?.completedCount || 0),
    running: Number(task?.runningCount || 0),
    remaining: Number(task?.remainingCount || 0),
    success: Number(task?.successCount || 0),
    failed: Number(task?.failedCount || 0)
  };
});

const startButtonText = computed(() => {
  if (isExecutionLocked.value) {
    return "批量生成执行中";
  }
  return data.running ? "正在创建批量任务..." : "开始批量生成";
});

const resultItems = computed(() => data.result?.items || []);

const overallFunctionCount = computed(() => {
  if (data.result?.functionCount != null) {
    return data.result.functionCount;
  }

  return new Set(
      resultItems.value.map(item => normalizeFunctionId(item.functionId || `${item.functionName}-${item.className}`))
  ).size;
});

const filteredResultItems = computed(() => {
  return resultItems.value.filter(item => {
    const functionNameMatched = !data.resultFilter.functionName
        || (item.functionName || "").toLowerCase().includes(data.resultFilter.functionName.toLowerCase());
    const modelNameMatched = !data.resultFilter.modelName
        || (item.modelName || "").toLowerCase().includes(data.resultFilter.modelName.toLowerCase());
    const statusMatched = !data.resultFilter.status || item.status === data.resultFilter.status;

    return functionNameMatched && modelNameMatched && statusMatched;
  });
});

const getSortValue = (item, prop) => {
  if (prop === "createTime") {
    return item.createTime ? new Date(item.createTime.replace(/-/g, "/")).getTime() : 0;
  }
  if (["evaluationScore", "testerEvaluationScore", "runIndex"].includes(prop)) {
    return item[prop] ?? -1;
  }
  return String(item[prop] ?? "").toLowerCase();
};

const sortedFilteredResultItems = computed(() => {
  const list = [...filteredResultItems.value];
  const { prop, order } = data.resultSort;

  if (!prop || !order) {
    return list;
  }

  const direction = order === "ascending" ? 1 : -1;
  list.sort((a, b) => {
    const valueA = getSortValue(a, prop);
    const valueB = getSortValue(b, prop);

    if (valueA > valueB) return direction;
    if (valueA < valueB) return -direction;
    return 0;
  });

  return list;
});

const pagedResultItems = computed(() => {
  const start = (data.resultCurrentPage - 1) * data.resultPageSize;
  const end = start + data.resultPageSize;
  return sortedFilteredResultItems.value.slice(start, end);
});

const calculateAverageScore = (items, field) => {
  const scores = items
      .map(item => item[field])
      .filter(value => value !== null && value !== undefined && value !== "");

  if (scores.length === 0) {
    return null;
  }

  const total = scores.reduce((sum, value) => sum + Number(value), 0);
  return Math.round(total / scores.length);
};

const resultModelStats = computed(() => {
  const map = new Map();

  sortedFilteredResultItems.value.forEach(item => {
    const key = item.modelName || "未命名模型";
    if (!map.has(key)) {
      map.set(key, {
        modelName: key,
        totalCount: 0,
        successCount: 0,
        failedCount: 0,
        evaluationScores: [],
        testerScores: []
      });
    }

    const current = map.get(key);
    current.totalCount++;

    if (item.status === "成功") {
      current.successCount++;
    } else {
      current.failedCount++;
    }

    if (item.evaluationScore !== null && item.evaluationScore !== undefined) {
      current.evaluationScores.push(Number(item.evaluationScore));
    }
    if (item.testerEvaluationScore !== null && item.testerEvaluationScore !== undefined) {
      current.testerScores.push(Number(item.testerEvaluationScore));
    }
  });

  return Array.from(map.values()).map(item => ({
    modelName: item.modelName,
    totalCount: item.totalCount,
    successCount: item.successCount,
    failedCount: item.failedCount,
    averageEvaluationScore: item.evaluationScores.length
        ? Math.round(item.evaluationScores.reduce((sum, value) => sum + value, 0) / item.evaluationScores.length)
        : null,
    averageTesterScore: item.testerScores.length
        ? Math.round(item.testerScores.reduce((sum, value) => sum + value, 0) / item.testerScores.length)
        : null
  }));
});

const buildFunctionLabel = (item) => {
  const className = item.className ? `（${item.className}）` : "";
  const language = item.language ? ` - ${item.language}` : "";
  return `${item.functionName}${className}${language}`;
};

const stopTaskPolling = () => {
  if (!taskPollingTimer) {
    return;
  }
  clearInterval(taskPollingTimer);
  taskPollingTimer = null;
};

const applyTaskRequestSnapshot = (task) => {
  const requestSnapshot = task?.requestSnapshot;
  if (!requestSnapshot) {
    return;
  }

  data.form.modelConfigIds = Array.isArray(requestSnapshot.modelConfigIds)
      ? [...requestSnapshot.modelConfigIds]
      : [];
  data.form.evaluationModelConfigId = requestSnapshot.evaluationModelConfigId ?? null;
  data.form.strategies = Array.isArray(requestSnapshot.strategies)
      ? [...requestSnapshot.strategies]
      : ["normal", "cot"];
  data.form.outputLanguage = requestSnapshot.outputLanguage || "zh";
  data.form.runCount = requestSnapshot.runCount || 1;
  data.form.functionIds = Array.isArray(requestSnapshot.functionIds)
      ? [...requestSnapshot.functionIds]
      : [];
  data.prefilledFunctions = [];
  data.prefilledFromFunctionPage = false;
};

const clearBatchTaskState = () => {
  data.batchTask = null;
  stopTaskPolling();
  clearStoredBatchExperimentTask();
};

const applyBatchTaskSnapshot = (task, options = {}) => {
  const normalizedTask = normalizeBatchExperimentTask(task);
  const previousTaskId = data.batchTask?.taskId || "";
  const previousStatus = data.batchTask?.status || "";

  data.batchTask = normalizedTask;

  if (normalizedTask) {
    writeStoredBatchExperimentTask(normalizedTask);
    applyTaskRequestSnapshot(normalizedTask);

    if (normalizedTask.result) {
      data.result = normalizeBatchResultPayload(normalizedTask.result);
    } else if (isBatchExperimentTaskActive(normalizedTask)) {
      data.result = null;
      resetResultViewState();
    }

    if (isBatchExperimentTaskActive(normalizedTask) && normalizedTask.taskId) {
      if (previousTaskId && previousTaskId !== normalizedTask.taskId) {
        stopTaskPolling();
      }
      if (!taskPollingTimer) {
        taskPollingTimer = window.setInterval(() => {
          refreshBatchTaskStatus(normalizedTask.taskId, { silent: true });
        }, 3000);
      }
    } else {
      stopTaskPolling();
    }
  } else {
    clearBatchTaskState();
  }

  const nextStatus = normalizedTask?.status || "";
  if (options.silent || previousTaskId !== normalizedTask?.taskId || previousStatus === nextStatus) {
    return;
  }

  if (nextStatus === "COMPLETED") {
    ElMessage.success("批量生成已完成");
  } else if (nextStatus === "TERMINATED") {
    ElMessage.warning("批量生成已终止");
  } else if (nextStatus === "FAILED") {
    ElMessage.error(normalizedTask?.message || "批量生成失败");
  }
};

const refreshBatchTaskStatus = async (taskId = data.batchTask?.taskId, options = {}) => {
  if (!taskId) {
    return null;
  }

  try {
    const task = await fetchBatchExperimentTaskById(taskId);
    applyBatchTaskSnapshot(task, options);
    return task;
  } catch (e) {
    if (!options.silent) {
      ElMessage.error(e.message || "批量生成任务状态获取失败");
    }
    return null;
  }
};

const syncBatchTaskStatusOnEnter = async () => {
  try {
    const latestTask = await resolveLatestBatchExperimentTask();
    if (latestTask) {
      applyBatchTaskSnapshot(latestTask, { silent: true });
      return isBatchExperimentTaskActive(latestTask);
    }
    return false;
  } catch (e) {
    return false;
  }
};

const loadPrefilledFunctions = () => {
  const runtimeFunctions = consumeRuntimePageCache(RUNTIME_PAGE_CACHE_KEYS.batchExperimentIncomingFunctions);
  if (Array.isArray(runtimeFunctions) && runtimeFunctions.length > 0) {
    clearBatchTaskState();
    const functionList = uniqueFunctionList(runtimeFunctions);
    data.prefilledFunctions = functionList;
    data.prefilledFromFunctionPage = true;
    data.form.functionIds = uniqueFunctionIds(functionList.map(item => item.id));
    data.result = null;
    resetResultViewState();
    ElMessage.success(`已自动带入 ${functionList.length} 个函数`);
    saveBatchExperimentPageState();
    return true;
  }

  const cache = sessionStorage.getItem(BATCH_EXPERIMENT_FUNCTIONS_KEY);
  if (!cache) {
    return false;
  }

  try {
    clearBatchTaskState();
    const parsed = JSON.parse(cache);
    const functionList = uniqueFunctionList(Array.isArray(parsed) ? parsed : []);

    data.prefilledFunctions = functionList;
    data.prefilledFromFunctionPage = functionList.length > 0;
    data.form.functionIds = uniqueFunctionIds(functionList.map(item => item.id));
    data.result = null;
    resetResultViewState();

    if (functionList.length > 0) {
      ElMessage.success(`已自动带入 ${functionList.length} 个函数`);
    }
  } catch (e) {
    data.prefilledFunctions = [];
    data.prefilledFromFunctionPage = false;
    data.form.functionIds = [];
    saveBatchExperimentPageState();
  } finally {
    sessionStorage.removeItem(BATCH_EXPERIMENT_FUNCTIONS_KEY);
  }
};

const syncSelectedFunctions = () => {
  const functionMap = new Map(data.functionList.map(item => [normalizeFunctionId(item.id), item]));
  const nextIds = uniqueFunctionIds(
      data.form.functionIds.filter(id => functionMap.has(normalizeFunctionId(id)))
  );

  if (nextIds.length !== data.form.functionIds.length) {
    ElMessage.warning("部分函数已不可用，系统已自动移除");
  }

  data.form.functionIds = nextIds;
  data.prefilledFunctions = uniqueFunctionList(
      data.prefilledFunctions
          .map(item => functionMap.get(normalizeFunctionId(item.id)) || item)
          .filter(item => functionMap.has(normalizeFunctionId(item.id)))
  );
};

const removeSelectedFunction = (functionId) => {
  if (isExecutionLocked.value) {
    return;
  }
  const targetId = normalizeFunctionId(functionId);
  data.form.functionIds = data.form.functionIds.filter(id => normalizeFunctionId(id) !== targetId);
  data.prefilledFunctions = data.prefilledFunctions.filter(item => normalizeFunctionId(item.id) !== targetId);

  if (data.form.functionIds.length === 0) {
    data.prefilledFromFunctionPage = false;
  }
};

const clearSelectedFunctions = () => {
  if (isExecutionLocked.value) {
    return;
  }
  data.form.functionIds = [];
  data.prefilledFunctions = [];
  data.prefilledFromFunctionPage = false;
};

const loadEnabledModels = () => {
  request.get("/modelConfig/selectEnabledList", {
    params: {
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    }
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      data.enabledModelList = res.data || [];
      syncBatchModelSelections();
      saveBatchExperimentPageState();
    } else {
      ElMessage.error(res.msg || "加载模型失败");
    }
  }).catch(() => ElMessage.error("加载模型失败"));
};

const loadFunctions = () => {
  request.get("/functionInfo/selectAll", {
    params: {
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    }
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      data.functionList = res.data || [];
      syncSelectedFunctions();
    } else {
      ElMessage.error(res.msg || "加载函数失败");
    }
  }).catch(() => ElMessage.error("加载函数失败"));
};

const resetForm = () => {
  if (isExecutionLocked.value) {
    return;
  }
  data.form.modelConfigIds = data.enabledModelList.length > 0 ? [data.enabledModelList[0].id] : [];
  data.form.strategies = ["normal", "cot"];
  data.form.outputLanguage = "zh";
  data.form.runCount = 3;
  data.form.functionIds = [];
  data.prefilledFunctions = [];
  data.prefilledFromFunctionPage = false;

  const cached = readStoredEvaluationModel();
  if (cached?.id) {
    const matched = data.enabledModelList.find(item => String(item.id) === String(cached.id));
    data.form.evaluationModelConfigId = matched ? matched.id : null;
  } else {
    data.form.evaluationModelConfigId = null;
  }
  clearBatchTaskState();
  data.result = null;
  resetResultViewState();
};

const resetResultFilters = () => {
  data.resultFilter.functionName = "";
  data.resultFilter.modelName = "";
  data.resultFilter.status = "";
  data.resultSort.prop = "createTime";
  data.resultSort.order = "descending";
  data.resultCurrentPage = 1;
};

const resetResultViewState = () => {
  resetResultFilters();
  data.detailVisible = false;
  data.detailItem = null;
  data.detailLoading = false;
  data.detailRequestKey = 0;
};

const validateForm = () => {
  if (data.form.functionIds.length === 0) {
    ElMessage.warning("请至少选择一个函数");
    return false;
  }
  if (data.form.modelConfigIds.length === 0) {
    ElMessage.warning("请至少选择一个模型");
    return false;
  }
  if (data.form.strategies.length === 0) {
    ElMessage.warning("请至少选择一个生成策略");
    return false;
  }
  if (!data.form.runCount || data.form.runCount <= 0) {
    ElMessage.warning("迭代轮次必须大于 0");
    return false;
  }
  return true;
};

const startBatchExperiment = () => {
  if (isExecutionLocked.value) {
    ElMessage.warning(getBatchExperimentLockMessage(batchTaskSnapshot.value));
    return;
  }

  if (!validateForm()) {
    return;
  }

  data.running = true;
  clearBatchTaskState();
  data.result = null;
  resetResultViewState();
  saveBatchExperimentPageState();

  request.post("/generate/batchExperiment", {
    functionIds: data.form.functionIds,
    modelConfigIds: data.form.modelConfigIds,
    strategies: data.form.strategies,
    runCount: data.form.runCount,
    outputLanguage: data.form.outputLanguage,
    evaluationModelConfigId: data.form.evaluationModelConfigId,
    currentUserId: currentUser.id,
    currentUserRole: currentRole
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      const task = normalizeBatchExperimentTask(res.data || null);
      applyBatchTaskSnapshot(task, { silent: true });
      saveBatchExperimentPageState();
      ElMessage.success("批量生成任务已启动");
    } else {
      syncBatchTaskStatusOnEnter();
      ElMessage.error(res.msg || "批量生成失败");
    }
  }).catch(() => {
    syncBatchTaskStatusOnEnter();
    ElMessage.error("批量生成失败");
  }).finally(() => {
    data.running = false;
    saveBatchExperimentPageState();
  });
};

const terminateBatchExperiment = async () => {
  if (!batchTaskSnapshot.value?.taskId || !batchTaskSnapshot.value?.canTerminate) {
    return;
  }

  try {
    await ElMessageBox.confirm(
        "确认终止当前批量生成任务吗？已完成的生成记录会保留，未执行部分将不再继续。",
        "终止确认",
        {
          type: "warning",
          confirmButtonText: "确认终止",
          cancelButtonText: "继续执行"
        }
    );

    const res = await request.post(`/generate/batchExperiment/${batchTaskSnapshot.value.taskId}/terminate`, {});
    if (res.code === "200" || res.code === 200) {
      applyBatchTaskSnapshot(res.data || null, { silent: true });
      ElMessage.success(res.data?.message || "已收到终止请求，正在停止后续任务");
    } else {
      ElMessage.error(res.msg || "终止失败，请稍后重试");
    }
  } catch (e) {
    if (e === "cancel" || e === "close") {
      return;
    }
    if (e?.message) {
      ElMessage.error(e.message);
      return;
    }
    ElMessage.error("终止失败，请稍后重试");
  }
};

const handleResultCurrentChange = (page) => {
  data.resultCurrentPage = page;
};

const handleResultSortChange = ({ prop, order }) => {
  data.resultSort.prop = prop || "";
  data.resultSort.order = order || "";
  data.resultCurrentPage = 1;
};

const openResultDetail = async (row) => {
  data.detailRequestKey += 1;
  const currentRequestKey = data.detailRequestKey;

  data.detailItem = buildDetailItem(row);
  data.detailVisible = true;

  if (!row?.recordId) {
    data.detailLoading = false;
    return;
  }

  data.detailLoading = true;
  try {
    const res = await request.get(`/generateRecord/selectById/${row.recordId}`);
    if (currentRequestKey !== data.detailRequestKey || !data.detailVisible) {
      return;
    }

    if (res.code === "200" || res.code === 200) {
      const detailItem = buildDetailItem(row, res.data || {});
      data.detailItem = detailItem;
      syncResultDetailCache(detailItem);
    } else {
      ElMessage.warning(res.msg || "加载完整实验记录失败，当前先展示列表摘要信息");
    }
  } catch (e) {
    if (currentRequestKey !== data.detailRequestKey) {
      return;
    }
    ElMessage.warning("加载完整实验记录失败，当前先展示列表摘要信息");
  } finally {
    if (currentRequestKey === data.detailRequestKey) {
      data.detailLoading = false;
    }
  }
};

const buildFilterSummaryText = () => {
  const list = [];
  if (data.resultFilter.functionName) list.push(`函数名称包含：${data.resultFilter.functionName}`);
  if (data.resultFilter.modelName) list.push(`模型名称包含：${data.resultFilter.modelName}`);
  if (data.resultFilter.status) list.push(`生成状态：${data.resultFilter.status}`);
  return list.length > 0 ? list.join("；") : "未筛选";
};

const buildExportSummaryRows = () => {
  return [
    { 项目: "批次号", 值: data.result?.batchNo || "—" },
    { 项目: "函数总数", 值: overallFunctionCount.value },
    { 项目: "实验任务总数", 值: data.result?.totalTaskCount ?? resultItems.value.length },
    { 项目: "生成成功数", 值: data.result?.successCount ?? 0 },
    { 项目: "生成失败数", 值: data.result?.failedCount ?? 0 },
    { 项目: "平均评估分数", 值: formatScore(data.result?.averageEvaluationScore) },
    { 项目: "平均测试人员分数", 值: formatScore(data.result?.averageTesterScore) },
    { 项目: "当前筛选结果数", 值: sortedFilteredResultItems.value.length },
    { 项目: "当前筛选平均评估分数", 值: formatScore(calculateAverageScore(sortedFilteredResultItems.value, "evaluationScore")) },
    { 项目: "当前筛选平均测试人员分数", 值: formatScore(calculateAverageScore(sortedFilteredResultItems.value, "testerEvaluationScore")) },
    { 项目: "当前筛选条件", 值: buildFilterSummaryText() },
    { 项目: "当前排序", 值: data.resultSort.prop ? `${data.resultSort.prop} ${data.resultSort.order === "ascending" ? "升序" : "降序"}` : "默认顺序" },
    { 项目: "导出时间", 值: new Date().toLocaleString() }
  ];
};

const buildExportResultRows = () => {
  return sortedFilteredResultItems.value.map(item => ({
    函数名称: item.functionName || "",
    所属类名: item.className || "",
    编程语言: item.language || "",
    备注: item.remark || "",
    所用模型: item.modelName || "",
    生成策略: getStrategyLabel(item.strategy),
    迭代轮次: formatIterationRound(item.runIndex),
    生成时间: item.createTime || "",
    生成状态: item.status || "",
    评估分数: item.evaluationScore ?? "",
    测试人员评分: item.testerEvaluationScore ?? "",
    生成用例数: item.generatedCaseCount ?? "",
    结果说明: item.message || "",
    正常路径覆盖: item.normalPathCoverage || "",
    边界条件覆盖: item.boundaryCoverage || "",
    异常分支覆盖: item.exceptionCoverage || "",
    语法规范性: item.syntaxNorm || "",
    改进建议: buildSuggestionList(item.suggestions, item.suggestionText).join("\n"),
    生成测试用例文本: item.resultText || ""
  }));
};

const exportBatchResult = () => {
  if (!data.result || sortedFilteredResultItems.value.length === 0) {
    ElMessage.warning("当前没有可导出的结果");
    return;
  }

  const workbook = XLSX.utils.book_new();

  const summarySheet = XLSX.utils.json_to_sheet(buildExportSummaryRows(), {
    header: ["项目", "值"]
  });
  summarySheet["!cols"] = [{ wch: 22 }, { wch: 80 }];
  XLSX.utils.book_append_sheet(workbook, summarySheet, "实验概览");

  if (resultModelStats.value.length > 0) {
    const modelSheet = XLSX.utils.json_to_sheet(resultModelStats.value.map(item => ({
      模型名称: item.modelName,
      记录数: item.totalCount,
      成功数: item.successCount,
      失败数: item.failedCount,
      平均评估分数: item.averageEvaluationScore ?? "",
      平均测试人员分数: item.averageTesterScore ?? ""
    })));
    modelSheet["!cols"] = [
      { wch: 24 },
      { wch: 10 },
      { wch: 10 },
      { wch: 10 },
      { wch: 14 },
      { wch: 18 }
    ];
    XLSX.utils.book_append_sheet(workbook, modelSheet, "模型统计");
  }

  const resultSheet = XLSX.utils.json_to_sheet(buildExportResultRows());
  resultSheet["!cols"] = [
    { wch: 20 },
    { wch: 18 },
    { wch: 12 },
    { wch: 20 },
    { wch: 22 },
    { wch: 16 },
    { wch: 10 },
    { wch: 20 },
    { wch: 12 },
    { wch: 12 },
    { wch: 14 },
    { wch: 12 },
    { wch: 20 },
    { wch: 28 },
    { wch: 24 },
    { wch: 24 },
    { wch: 24 },
    { wch: 22 },
    { wch: 40 },
    { wch: 80 }
  ];
  XLSX.utils.book_append_sheet(workbook, resultSheet, "实验结果");

  const safeBatchNo = String(data.result.batchNo || "batch").replace(/[\\/:*?"<>|]/g, "_");
  XLSX.writeFile(workbook, `批量生成结果_${safeBatchNo}.xlsx`);
};

watch(
    () => ({
      form: data.form,
      taskSnapshot: data.batchTask,
      prefilledFunctions: data.prefilledFunctions,
      prefilledFromFunctionPage: data.prefilledFromFunctionPage,
      result: data.result,
      resultCurrentPage: data.resultCurrentPage,
      resultPageSize: data.resultPageSize,
      resultFilter: data.resultFilter,
      resultSort: data.resultSort
    }),
    () => {
      saveBatchExperimentPageState();
    },
    { deep: true }
);

watch(
    () => [data.resultFilter.functionName, data.resultFilter.modelName, data.resultFilter.status],
    () => {
      data.resultCurrentPage = 1;
    }
);

const initializeBatchExperimentPage = async () => {
  restoreBatchExperimentPageState();
  loadEnabledModels();
  loadFunctions();

  const hasActiveTask = await syncBatchTaskStatusOnEnter();
  if (hasActiveTask) {
    return;
  }

  const hasPrefilledFunctions = loadPrefilledFunctions();
  if (hasPrefilledFunctions) {
    return;
  }

  if (data.batchTask?.taskId) {
    const restoredTask = await refreshBatchTaskStatus(data.batchTask.taskId, { silent: true });
    if (restoredTask) {
      return;
    }
  }

  clearBatchTaskState();
};

onMounted(() => {
  initializeBatchExperimentPage();
});

onBeforeUnmount(() => {
  stopTaskPolling();
});
</script>

<style scoped>
.batch-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.batch-form {
  position: relative;
}

.task-status-panel {
  margin-bottom: 14px;
  padding: 16px 18px;
  border-radius: 14px;
  border: 1px solid #d7e6ff;
  background: linear-gradient(135deg, #f8fbff 0%, #eef5ff 100%);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.task-status-panel--active {
  border-color: #bfd7ff;
  box-shadow: inset 0 0 0 1px rgba(74, 118, 216, 0.06);
}

.task-status-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.task-status-title {
  font-size: 16px;
  font-weight: 700;
  color: #274878;
  margin-bottom: 4px;
}

.task-status-desc {
  color: #4c6a94;
  line-height: 1.7;
}

.task-status-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.task-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.task-metrics span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #dce7f8;
  color: #536b8d;
  font-size: 12px;
}

.task-current-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #dce7f8;
  color: #4b607d;
  line-height: 1.7;
}

.config-lock-mask {
  position: absolute;
  inset: 0;
  z-index: 3;
  border-radius: 14px;
  background: rgba(248, 251, 255, 0.7);
  backdrop-filter: blur(2px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  pointer-events: none;
}

.config-lock-mask__title {
  font-size: 18px;
  font-weight: 700;
  color: #274878;
}

.config-lock-mask__desc {
  max-width: 420px;
  text-align: center;
  color: #4c6a94;
  line-height: 1.7;
}

.selected-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 10px 12px;
  margin-bottom: 12px;
  border-radius: 10px;
  background: #fafaf7;
  color: #606266;
}

.selected-functions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.preview-label {
  color: #606266;
}

.preview-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef5ff;
  color: #3b6fd8;
  font-size: 12px;
}

.preview-more {
  color: #909399;
  font-size: 12px;
}

.prefill-tip {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f4f8ff;
  border: 1px solid #d7e6ff;
  color: #355f9c;
  line-height: 1.7;
}

.selected-function-panel,
.comparison-section {
  margin-bottom: 14px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 14px;
  background: #fffdf9;
}

.selected-function-header,
.section-header,
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.selected-function-title,
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.selected-function-desc,
.section-tip {
  color: #909399;
  line-height: 1.6;
}

.selected-function-empty,
.detail-empty {
  padding: 28px 16px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px dashed #dcdfe6;
  color: #909399;
  text-align: center;
}

.form-actions {
  display: flex;
  gap: 10px;
}

.result-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.result-meta-text {
  color: #606266;
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.summary-item {
  padding: 14px;
  border-radius: 12px;
  background: #fafaf7;
  border: 1px solid #ebeef5;
}

.summary-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.summary-value.success {
  color: #67c23a;
}

.summary-value.danger {
  color: #f56c6c;
}

.result-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.result-table-wrapper {
  min-height: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}

.detail-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-loading-text {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f4f8ff;
  border: 1px solid #d7e6ff;
  color: #355f9c;
  line-height: 1.7;
}

.detail-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.detail-overview-item {
  padding: 14px;
  border-radius: 12px;
  background: #fafaf7;
  border: 1px solid #ebeef5;
}

.detail-overview-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.detail-overview-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.detail-section {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}

.detail-section-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 12px;
}

.detail-pre {
  margin: 0;
  padding: 14px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.7;
  max-height: 280px;
  overflow: auto;
}

.detail-suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-suggestion-item {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  line-height: 1.7;
  color: #606266;
}

@media (max-width: 1400px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-overview {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1200px) {
  .task-status-header,
  .task-status-actions,
  .selected-function-header,
  .section-header,
  .result-header {
    flex-direction: column;
    align-items: stretch;
  }

  .task-current-card {
    grid-template-columns: 1fr;
  }
}
</style>
