<template>
  <div class="generate-page">
    <div class="left-panel card">
      <div class="panel-title">测试用例生成</div>

      <div v-if="hasLockedGenerationTask" class="batch-lock-notice">
        <div class="batch-lock-notice__title">当前存在未完成的批量生成任务</div>
        <div class="batch-lock-notice__desc">{{ generationLockNotice }}</div>
      </div>

      <el-form :model="data.form" label-width="90px" class="generate-form">
        <el-form-item label="选择模型">
          <el-select
              v-model="data.form.modelConfigId"
              placeholder="请选择可用模型"
              style="width: 100%"
              clearable
          >
            <el-option
                v-for="item in data.enabledModelList"
                :key="item.id"
                :label="item.modelName"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="生成策略">
          <el-select v-model="data.form.strategy" placeholder="请选择生成策略" style="width: 100%">
            <el-option label="普通生成" value="normal" />
            <el-option label="链式分析生成" value="cot" />
          </el-select>
        </el-form-item>

        <el-form-item label="输出语言">
          <el-select v-model="data.form.outputLanguage" placeholder="请选择输出语言" style="width: 100%">
            <el-option label="中文" value="zh" />
            <el-option label="English" value="en" />
          </el-select>
        </el-form-item>

        <template v-if="data.form.sourceType === 'function'">
          <el-form-item label="函数名称">
            <el-input
                v-model="data.form.functionName"
                placeholder="请输入函数名称"
                maxlength="255"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="所属类名">
            <el-input
                v-model="data.form.className"
                placeholder="请输入所属类名"
                maxlength="255"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="函数代码">
            <el-input
                v-model="data.form.codeText"
                type="textarea"
                :rows="8"
                class="code-textarea"
                placeholder="请输入函数代码"
                maxlength="20000"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="输入说明">
            <el-input
                v-model="data.form.inputDesc"
                type="textarea"
                :rows="3"
                placeholder="请输入输入说明"
                maxlength="2000"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="输出说明">
            <el-input
                v-model="data.form.outputDesc"
                type="textarea"
                :rows="3"
                placeholder="请输入输出说明"
                maxlength="2000"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="备注">
            <el-input
                v-model="data.form.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入备注"
                maxlength="255"
                show-word-limit
            />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="需求名称">
            <el-input
                v-model="data.form.requirementName"
                placeholder="请输入需求名称"
                maxlength="255"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="所属模块">
            <el-input
                v-model="data.form.moduleName"
                placeholder="请输入所属模块"
                maxlength="255"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="需求内容">
            <el-input
                v-model="data.form.requirementContent"
                type="textarea"
                :rows="10"
                placeholder="请输入需求内容"
                maxlength="20000"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="前置条件">
            <el-input
                v-model="data.form.preconditionDesc"
                type="textarea"
                :rows="3"
                placeholder="请输入前置条件"
                maxlength="2000"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="预期说明">
            <el-input
                v-model="data.form.expectedDesc"
                type="textarea"
                :rows="3"
                placeholder="请输入预期说明"
                maxlength="2000"
                show-word-limit
            />
          </el-form-item>

          <el-form-item label="备注">
            <el-input
                v-model="data.form.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入备注"
                maxlength="255"
                show-word-limit
            />
          </el-form-item>
        </template>

        <el-form-item class="form-actions">
          <el-button type="primary" :loading="data.generating" :disabled="hasLockedGenerationTask || data.evaluating" @click="generateCase">
            {{ data.generating ? "正在生成..." : "生成测试用例" }}
          </el-button>
          <el-button type="primary" plain @click="resetForm" :disabled="data.generating || data.evaluating">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="right-panel card">
      <div class="right-top">
        <div class="sub-title">生成结果</div>

        <div v-if="data.generating" class="loading-line">
          <span class="loading-dot"></span>
          <span class="loading-dot"></span>
          <span class="loading-dot"></span>
        </div>

        <div class="result-wrapper">
          <el-input
              v-model="data.resultText"
              type="textarea"
              readonly
              resize="none"
              class="result-textarea"
              placeholder="生成结果将在这里展示"
          />
        </div>
      </div>

      <div class="right-bottom">
        <div class="evaluation-title-row">
          <div class="sub-title">评估结果</div>
          <el-button
              v-if="showRegenerateBtn"
              type="primary"
              :disabled="hasLockedGenerationTask"
              :loading="data.generating || data.evaluating"
              @click="regenerateCase"
          >
            再次生成
          </el-button>
        </div>

        <div v-if="data.evaluating && !data.evaluationResult" class="evaluation-empty">
          <div class="loading-box">
            <div class="loading-line">
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
              <span class="loading-dot"></span>
            </div>
            <div class="loading-tip">正在评估生成结果，请稍候...</div>
          </div>
        </div>

        <div v-else-if="data.evaluationResult" class="evaluation-panel">
          <div class="evaluation-scroll-content">
          <div class="evaluation-top-row">
            <div class="score-card">
              <div class="score-label">综合评分</div>
              <div class="score-value">{{ data.evaluationResult.score ?? "—" }}</div>
            </div>

            <div class="summary-card">
              <div class="summary-title">覆盖与规范分析</div>
              <div class="summary-content">
                <div class="summary-item">
                  <span class="summary-label">正常路径覆盖：</span>
                  <span>{{ data.evaluationResult.normalPathCoverage || "—" }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">边界条件覆盖：</span>
                  <span>{{ data.evaluationResult.boundaryCoverage || "—" }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">异常分支覆盖：</span>
                  <span>{{ data.evaluationResult.exceptionCoverage || "—" }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">语法规范性：</span>
                  <span>{{ data.evaluationResult.syntaxNorm || "—" }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="coverage-card">
            <div class="coverage-title">coverage.py 覆盖率</div>
            <div class="coverage-grid">
              <div class="coverage-item">
                <span class="coverage-label">语句覆盖率：</span>
                <span>{{ formatCoverage(data.evaluationResult.lineCoverage) }}</span>
              </div>
              <div class="coverage-item">
                <span class="coverage-label">分支覆盖率：</span>
                <span>{{ formatCoverage(data.evaluationResult.branchCoverage) }}</span>
              </div>
              <div class="coverage-item">
                <span class="coverage-label">覆盖率状态：</span>
                <el-tag size="small" :type="getCoverageStatusTag(data.evaluationResult.coverageStatus)">
                  {{ data.evaluationResult.coverageStatus || "未统计" }}
                </el-tag>
              </div>
              <div class="coverage-item coverage-item--full">
                <span class="coverage-label">覆盖率说明：</span>
                <span>{{ data.evaluationResult.coverageMessage || "未统计" }}</span>
              </div>
            </div>
          </div>

          <div class="suggestion-block">
            <div class="suggestion-header">
              <div class="suggestion-title">改进建议</div>
            </div>

            <div class="suggestion-content">
              <ul>
                <li v-for="(item, index) in data.evaluationResult.suggestions || []" :key="index">
                  {{ item }}
                </li>
              </ul>
            </div>
          </div>
          </div>
        </div>

        <div v-else class="evaluation-empty">
          <span>评估结果将在这里展示</span>
        </div>

        <div v-if="showPublicCompareBlock" class="public-compare-panel">
          <div class="public-compare-title">公开测试基准对比分析</div>

          <div class="public-compare-content">
          <div v-if="!hasPublicTestContent" class="public-compare-empty">
            暂无公开测试基准对比分析结果
          </div>

          <template v-else>
            <div class="public-analysis-card">
              <div class="public-compare-subtitle">公开测试基准覆盖分析</div>
              <div class="public-compare-stats public-compare-stats--benchmark">
                <div>公开断言总数：{{ publicCompareTotal }}</div>
                <div>已覆盖：{{ data.publicCoveredCount ?? 0 }}</div>
                <div>部分覆盖：{{ data.publicPartialCount ?? 0 }}</div>
                <div>缺失：{{ data.publicMissingCount ?? 0 }}</div>
                <div>基准覆盖率：{{ benchmarkCoverageRateText }}</div>
                <div>综合基准覆盖率：{{ benchmarkComprehensiveRateText }}</div>
              </div>

              <el-alert
                  v-if="publicCompareFailureMessage"
                  class="public-compare-alert"
                  type="warning"
                  :title="publicCompareFailureMessage"
                  show-icon
                  :closable="false"
              />

              <div v-if="!data.publicCompareResult" class="public-compare-empty">
                生成测试用例后将自动对比公开测试基准
              </div>

              <template v-else>
                <div v-if="publicMissingItems.length" class="public-compare-list">
                  <div class="public-compare-section-title">缺失测试点</div>
                  <div v-for="(item, index) in publicMissingItems" :key="`missing-${index}`" class="public-compare-item">
                    <div class="public-compare-item-head">
                      <el-tag size="small" :type="getPublicCompareStatusTag(item.status)">
                        {{ getPublicCompareStatusText(item.status) }}
                      </el-tag>
                      <span>{{ item.assertion || "未返回断言原文" }}</span>
                    </div>
                    <div class="public-compare-reason">原因：{{ item.reason || "暂无说明" }}</div>
                    <div class="public-compare-suggest">建议补充：{{ item.suggestedCase || "暂无建议" }}</div>
                  </div>
                </div>

                <div v-if="publicPartialItems.length" class="public-compare-list">
                  <div class="public-compare-section-title">部分覆盖测试点</div>
                  <div v-for="(item, index) in publicPartialItems" :key="`partial-${index}`" class="public-compare-item">
                    <div class="public-compare-item-head">
                      <el-tag size="small" :type="getPublicCompareStatusTag(item.status)">
                        {{ getPublicCompareStatusText(item.status) }}
                      </el-tag>
                      <span>{{ item.assertion || "未返回断言原文" }}</span>
                    </div>
                    <div class="public-compare-reason">原因：{{ item.reason || "暂无说明" }}</div>
                    <div class="public-compare-suggest">建议补充：{{ item.suggestedCase || "暂无建议" }}</div>
                  </div>
                </div>

                <div v-if="!publicMissingItems.length && !publicPartialItems.length" class="public-compare-empty">
                  {{ publicCompareFailureMessage ? "公开测试基准覆盖分析暂未完成，生成记录已保存。" : (publicCompareSummary || "公开测试基准已覆盖或暂无缺失项") }}
                </div>
              </template>
            </div>

            <div class="public-analysis-card">
              <div class="public-compare-subtitle">生成用例增量价值分析</div>
              <div v-if="!hasIncrementalAnalysis" class="public-compare-empty">
                暂无生成用例增量价值分析结果
              </div>

              <template v-else>
                <div class="public-compare-stats public-compare-stats--incremental">
                  <div>生成用例总数：{{ incrementalGeneratedCaseCount }}</div>
                  <div>匹配基准用例数：{{ incrementalMatchedCaseCount }}</div>
                  <div>新增有效测试点数：{{ incrementalExtraCaseCount }}</div>
                  <div>无效或重复测试点数：{{ incrementalInvalidCaseCount }}</div>
                  <div>增量补充率：{{ incrementalExtraRateText }}</div>
                  <div>覆盖扩展率：{{ incrementalExpandRateText }}</div>
                </div>

                <div v-if="incrementalExtraItems.length" class="public-compare-list">
                  <div class="public-compare-section-title">新增有效测试点列表</div>
                  <div v-for="(item, index) in incrementalExtraItems" :key="`extra-${index}`" class="public-compare-item">
                    <div class="public-compare-item-head">
                      <el-tag size="small" type="success">新增有效</el-tag>
                      <span>{{ item.generatedCaseId || `TC-${String(index + 1).padStart(3, "0")}` }}：{{ item.scenario || "未返回场景说明" }}</span>
                    </div>
                    <div class="public-compare-reason">类型：{{ getIncrementalTypeText(item.type) }}</div>
                    <div class="public-compare-reason">原因：{{ item.reason || "暂无说明" }}</div>
                    <div class="public-compare-suggest">价值：{{ item.value || "暂无价值说明" }}</div>
                  </div>
                </div>

                <div v-if="incrementalInvalidItems.length" class="public-compare-list">
                  <div class="public-compare-section-title">无效或重复测试点</div>
                  <div v-for="(item, index) in incrementalInvalidItems" :key="`invalid-${index}`" class="public-compare-item">
                    <div class="public-compare-item-head">
                      <el-tag size="small" type="info">无效/重复</el-tag>
                      <span>{{ item.generatedCaseId || `TC-${String(index + 1).padStart(3, "0")}` }}</span>
                    </div>
                    <div class="public-compare-reason">原因：{{ item.reason || "暂无说明" }}</div>
                  </div>
                </div>

                <div v-if="!incrementalExtraItems.length && !incrementalInvalidItems.length" class="public-compare-empty">
                  {{ incrementalSummary || "暂无新增有效测试点详情" }}
                </div>
              </template>
            </div>
          </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onBeforeUnmount, onMounted, computed, watch } from "vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request.js";
import {
  fetchBatchExperimentTaskById,
  getBatchExperimentLockMessage,
  isBatchExperimentTaskActive,
  normalizeBatchExperimentTask,
  readStoredBatchExperimentTask,
  resolveLatestBatchExperimentTask
} from "@/utils/batchExperimentTask.js";
import {
  RUNTIME_PAGE_CACHE_KEYS,
  consumeRuntimePageCache,
  readRuntimePageCache,
  writeRuntimePageCache
} from "@/utils/runtimePageCache.js";

const currentUser = JSON.parse(localStorage.getItem("xm-user") || "{}");
const currentRole = currentUser.loginType === "USER" ? "USER" : currentUser.role;
let batchTaskPollingTimer = null;
const LEGACY_GENERATE_FUNCTION_KEY = "generateFunctionInfo";
const LEGACY_GENERATE_REQUIREMENT_KEY = "generateRequirementInfo";

const getEvaluationModelStorageKey = () => {
  return `evaluation-default-model-${currentUser.loginType || ""}-${currentRole}-${currentUser.id || ""}`;
};

const readDefaultEvaluationModelId = () => {
  try {
    const cache = JSON.parse(localStorage.getItem(getEvaluationModelStorageKey()) || "null");
    return cache?.id ?? null;
  } catch (e) {
    return null;
  }
};

const buildDefaultForm = (defaultModelId = null, defaultEvaluationModelId = null) => ({
  modelConfigId: defaultModelId,
  evaluationModelConfigId: defaultEvaluationModelId,
  strategy: "normal",
  sourceType: "function",
  outputLanguage: "zh",

  functionId: null,
  testerCaseCount: null,
  functionName: "",
  className: "",
  language: "",
  codeText: "",
  inputDesc: "",
  outputDesc: "",
  remark: "",
  publicTestContent: "",
  publicTestSource: "",
  publicAssertCount: 0,

  requirementName: "",
  moduleName: "",
  requirementContent: "",
  preconditionDesc: "",
  expectedDesc: "",

  resultText: "",
  currentUserId: currentUser.id,
  currentUserRole: currentRole,
  regenerate: false,
  previousResultText: "",
  previousEvaluationSummary: "",
  recordId: null
});

const data = reactive({
  form: buildDefaultForm(),
  resultText: "",
  evaluationResult: null,
  publicCompareResult: "",
  publicCoveredCount: 0,
  publicPartialCount: 0,
  publicMissingCount: 0,
  publicExtraResult: "",
  generatedCaseCount: 0,
  publicMatchedCaseCount: 0,
  publicExtraCaseCount: 0,
  publicInvalidCaseCount: 0,
  publicExtraRate: 0,
  publicExpandRate: 0,
  enabledModelList: [],
  generating: false,
  evaluating: false,
  lastRecordId: null,
  batchTask: readStoredBatchExperimentTask()
});

const showRegenerateBtn = computed(() => {
  return !!data.evaluationResult;
});

const showPublicCompareBlock = computed(() => data.form.sourceType === "function");

const hasPublicTestContent = computed(() => {
  return String(data.form.publicTestContent || "").trim().length > 0;
});

const formatCoverage = (value) => {
  const number = Number(value);
  return Number.isFinite(number) ? `${number.toFixed(2)}%` : "未统计";
};

const getCoverageStatusTag = (status) => {
  if (status === "SUCCESS") return "success";
  if (status === "TEST_FAILED") return "warning";
  if (status === "FAILED") return "danger";
  if (status === "SKIPPED") return "info";
  return "info";
};

const parsePublicCompareResult = (text) => {
  const raw = String(text || "").trim();
  if (!raw) {
    return null;
  }

  try {
    const start = raw.indexOf("{");
    const end = raw.lastIndexOf("}");
    const jsonText = start >= 0 && end > start ? raw.slice(start, end + 1) : raw;
    return JSON.parse(jsonText);
  } catch (e) {
    return {
      rawText: raw,
      items: [],
      summary: raw
    };
  }
};

const publicCompareDetail = computed(() => parsePublicCompareResult(data.publicCompareResult));

const publicExtraDetail = computed(() => parsePublicCompareResult(data.publicExtraResult));

const benchmarkCoverageDetail = computed(() => {
  const detail = publicCompareDetail.value;
  return detail?.benchmarkCoverageAnalysis || detail;
});

const incrementalValueDetail = computed(() => {
  return publicExtraDetail.value || publicCompareDetail.value?.incrementalValueAnalysis || null;
});

const toNumber = (value) => {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
};

const formatRateText = (value) => {
  const number = toNumber(value);
  return number === null ? "暂无数据" : `${number.toFixed(2)}%`;
};

const publicCompareItems = computed(() => {
  return Array.isArray(benchmarkCoverageDetail.value?.items) ? benchmarkCoverageDetail.value.items : [];
});

const publicMissingItems = computed(() => {
  return publicCompareItems.value.filter(item => String(item?.status || "").toLowerCase() === "missing");
});

const publicPartialItems = computed(() => {
  return publicCompareItems.value.filter(item => String(item?.status || "").toLowerCase() === "partial");
});

const publicCompareTotal = computed(() => {
  const detailTotal = Number(benchmarkCoverageDetail.value?.publicTotal ?? benchmarkCoverageDetail.value?.total);
  if (Number.isFinite(detailTotal) && detailTotal >= 0) {
    return detailTotal;
  }
  const formTotal = Number(data.form.publicAssertCount);
  if (Number.isFinite(formTotal) && formTotal >= 0) {
    return formTotal;
  }
  return (data.publicCoveredCount || 0) + (data.publicPartialCount || 0) + (data.publicMissingCount || 0);
});

const publicCompareSummary = computed(() => {
  return benchmarkCoverageDetail.value?.summary || publicCompareDetail.value?.rawText || "";
});

const publicCompareFailureMessage = computed(() => {
  const message = publicCompareSummary.value;
  return String(message || "").includes("公开测试基准对比分析失败") ? message : "";
});

const benchmarkCoverageRate = computed(() => {
  const detailRate = toNumber(benchmarkCoverageDetail.value?.benchmarkCoverageRate);
  if (detailRate !== null) {
    return detailRate;
  }
  const total = publicCompareTotal.value;
  return total > 0 ? (Number(data.publicCoveredCount || 0) * 100) / total : null;
});

const benchmarkComprehensiveRate = computed(() => {
  const detailRate = toNumber(benchmarkCoverageDetail.value?.benchmarkComprehensiveRate);
  if (detailRate !== null) {
    return detailRate;
  }
  const total = publicCompareTotal.value;
  return total > 0
      ? ((Number(data.publicCoveredCount || 0) + Number(data.publicPartialCount || 0) * 0.5) * 100) / total
      : null;
});

const benchmarkCoverageRateText = computed(() => formatRateText(benchmarkCoverageRate.value));

const benchmarkComprehensiveRateText = computed(() => formatRateText(benchmarkComprehensiveRate.value));

const incrementalExtraItems = computed(() => {
  return Array.isArray(incrementalValueDetail.value?.extraItems) ? incrementalValueDetail.value.extraItems : [];
});

const incrementalInvalidItems = computed(() => {
  return Array.isArray(incrementalValueDetail.value?.invalidItems) ? incrementalValueDetail.value.invalidItems : [];
});

const incrementalGeneratedCaseCount = computed(() => {
  return toNumber(incrementalValueDetail.value?.generatedCaseCount) ?? Number(data.generatedCaseCount || 0);
});

const incrementalMatchedCaseCount = computed(() => {
  return toNumber(incrementalValueDetail.value?.publicMatchedCaseCount) ?? Number(data.publicMatchedCaseCount || 0);
});

const incrementalExtraCaseCount = computed(() => {
  return toNumber(incrementalValueDetail.value?.extraValidCount) ?? Number(data.publicExtraCaseCount || 0);
});

const incrementalInvalidCaseCount = computed(() => {
  return toNumber(incrementalValueDetail.value?.invalidOrDuplicateCount) ?? Number(data.publicInvalidCaseCount || 0);
});

const incrementalExtraRate = computed(() => {
  return toNumber(incrementalValueDetail.value?.extraRate) ?? toNumber(data.publicExtraRate);
});

const incrementalExpandRate = computed(() => {
  return toNumber(incrementalValueDetail.value?.expandRate) ?? toNumber(data.publicExpandRate);
});

const incrementalExtraRateText = computed(() => formatRateText(incrementalExtraRate.value));

const incrementalExpandRateText = computed(() => formatRateText(incrementalExpandRate.value));

const incrementalSummary = computed(() => {
  return incrementalValueDetail.value?.summary || incrementalValueDetail.value?.rawText || "";
});

const hasIncrementalAnalysis = computed(() => {
  return !!data.publicExtraResult
      || !!publicCompareDetail.value?.incrementalValueAnalysis
      || incrementalExtraItems.value.length > 0
      || incrementalInvalidItems.value.length > 0
      || incrementalGeneratedCaseCount.value > 0
      || incrementalExtraCaseCount.value > 0;
});

const getPublicCompareStatusTag = (status) => {
  const value = String(status || "").toLowerCase();
  if (value === "covered") return "success";
  if (value === "partial") return "warning";
  if (value === "missing") return "danger";
  return "info";
};

const getPublicCompareStatusText = (status) => {
  const value = String(status || "").toLowerCase();
  if (value === "covered") return "已覆盖";
  if (value === "partial") return "部分覆盖";
  if (value === "missing") return "缺失";
  return "未识别";
};

const getIncrementalTypeText = (type) => {
  const value = String(type || "").toLowerCase();
  const map = {
    boundary: "边界值",
    empty_input: "空数据",
    exception: "异常输入",
    different_length: "不同长度",
    negative: "负数",
    float: "浮点数",
    special_structure: "特殊数据结构",
    normal_extension: "普通场景扩展",
    other: "其他"
  };
  return map[value] || type || "其他";
};

const batchTaskSnapshot = computed(() => data.batchTask || null);

const hasLockedGenerationTask = computed(() => {
  return isBatchExperimentTaskActive(batchTaskSnapshot.value);
});

const generationLockNotice = computed(() => {
  return getBatchExperimentLockMessage(batchTaskSnapshot.value);
});

const buildGeneratePreferenceSnapshot = () => ({
  modelConfigId: data.form.modelConfigId,
  evaluationModelConfigId: data.form.evaluationModelConfigId,
  strategy: data.form.strategy,
  outputLanguage: data.form.outputLanguage
});

const resetGenerateResultState = () => {
  data.resultText = "";
  data.evaluationResult = null;
  data.publicCompareResult = "";
  data.publicCoveredCount = 0;
  data.publicPartialCount = 0;
  data.publicMissingCount = 0;
  data.publicExtraResult = "";
  data.generatedCaseCount = 0;
  data.publicMatchedCaseCount = 0;
  data.publicExtraCaseCount = 0;
  data.publicInvalidCaseCount = 0;
  data.publicExtraRate = 0;
  data.publicExpandRate = 0;
  data.lastRecordId = null;
  data.generating = false;
  data.evaluating = false;
};

const saveGeneratePageState = () => {
  writeRuntimePageCache(RUNTIME_PAGE_CACHE_KEYS.generatePageState, {
    form: {
      ...data.form,
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    },
    resultText: data.resultText,
    evaluationResult: data.evaluationResult,
    publicCompareResult: data.publicCompareResult,
    publicCoveredCount: data.publicCoveredCount,
    publicPartialCount: data.publicPartialCount,
    publicMissingCount: data.publicMissingCount,
    publicExtraResult: data.publicExtraResult,
    generatedCaseCount: data.generatedCaseCount,
    publicMatchedCaseCount: data.publicMatchedCaseCount,
    publicExtraCaseCount: data.publicExtraCaseCount,
    publicInvalidCaseCount: data.publicInvalidCaseCount,
    publicExtraRate: data.publicExtraRate,
    publicExpandRate: data.publicExpandRate,
    lastRecordId: data.lastRecordId
  });
};

const restoreGeneratePageState = () => {
  const cache = readRuntimePageCache(RUNTIME_PAGE_CACHE_KEYS.generatePageState);
  if (!cache) {
    return false;
  }

  const cachedForm = cache.form || {};
  data.form = {
    ...buildDefaultForm(cachedForm.modelConfigId ?? null, cachedForm.evaluationModelConfigId ?? null),
    ...cachedForm,
    currentUserId: currentUser.id,
    currentUserRole: currentRole
  };
  data.resultText = cache.resultText ?? "";
  data.evaluationResult = cache.evaluationResult || null;
  data.publicCompareResult = cache.publicCompareResult || "";
  data.publicCoveredCount = cache.publicCoveredCount ?? 0;
  data.publicPartialCount = cache.publicPartialCount ?? 0;
  data.publicMissingCount = cache.publicMissingCount ?? 0;
  data.publicExtraResult = cache.publicExtraResult || "";
  data.generatedCaseCount = cache.generatedCaseCount ?? 0;
  data.publicMatchedCaseCount = cache.publicMatchedCaseCount ?? 0;
  data.publicExtraCaseCount = cache.publicExtraCaseCount ?? 0;
  data.publicInvalidCaseCount = cache.publicInvalidCaseCount ?? 0;
  data.publicExtraRate = cache.publicExtraRate ?? 0;
  data.publicExpandRate = cache.publicExpandRate ?? 0;
  data.lastRecordId = cache.lastRecordId ?? null;
  data.generating = false;
  data.evaluating = false;
  return true;
};

const stopBatchTaskPolling = () => {
  if (!batchTaskPollingTimer) {
    return;
  }
  clearInterval(batchTaskPollingTimer);
  batchTaskPollingTimer = null;
};

const applyBatchTaskSnapshot = (task) => {
  data.batchTask = normalizeBatchExperimentTask(task);

  if (hasLockedGenerationTask.value && data.batchTask?.taskId) {
    if (!batchTaskPollingTimer) {
      batchTaskPollingTimer = window.setInterval(() => {
        refreshBatchTaskStatus(data.batchTask?.taskId, { silent: true });
      }, 3000);
    }
  } else {
    stopBatchTaskPolling();
  }
};

const refreshBatchTaskStatus = async (taskId = data.batchTask?.taskId, options = {}) => {
  if (!taskId) {
    return null;
  }

  try {
    const task = await fetchBatchExperimentTaskById(taskId);
    applyBatchTaskSnapshot(task);
    return task;
  } catch (e) {
    if (!options.silent) {
      ElMessage.error(e.message || "批量生成任务状态获取失败");
    }
    return null;
  }
};

const initializeBatchTaskState = async () => {
  try {
    const task = await resolveLatestBatchExperimentTask();
    applyBatchTaskSnapshot(task);
  } catch (e) {
    applyBatchTaskSnapshot(data.batchTask);
  }
};

const applyGenerateSource = (sourceType, payload = {}) => {
  const preferenceSnapshot = buildGeneratePreferenceSnapshot();
  data.form = {
    ...buildDefaultForm(preferenceSnapshot.modelConfigId ?? null, preferenceSnapshot.evaluationModelConfigId ?? null),
    ...preferenceSnapshot,
    sourceType,
    currentUserId: currentUser.id,
    currentUserRole: currentRole
  };

  if (sourceType === "requirement") {
    data.form.requirementName = payload.requirementName || "";
    data.form.moduleName = payload.moduleName || "";
    data.form.requirementContent = payload.requirementContent || "";
    data.form.preconditionDesc = payload.preconditionDesc || "";
    data.form.expectedDesc = payload.expectedDesc || "";
    data.form.remark = payload.remark || "";
    data.form.functionId = null;
    data.form.testerCaseCount = null;
  } else {
    data.form.functionId = payload.id || null;
    data.form.testerCaseCount = payload.testerCaseCount ?? null;
    data.form.functionName = payload.functionName || "";
    data.form.className = payload.className || "";
    data.form.language = payload.language || "";
    data.form.codeText = payload.codeText || "";
    data.form.inputDesc = payload.inputDesc || "";
    data.form.outputDesc = payload.outputDesc || "";
    data.form.remark = payload.remark || "";
    data.form.publicTestContent = payload.publicTestContent || "";
    data.form.publicTestSource = payload.publicTestSource || "";
    data.form.publicAssertCount = payload.publicAssertCount ?? 0;
  }

  resetGenerateResultState();
  saveGeneratePageState();
};

const consumeLegacyGenerateSource = () => {
  const requirementCache = sessionStorage.getItem(LEGACY_GENERATE_REQUIREMENT_KEY);
  if (requirementCache) {
    try {
      return {
        sourceType: "requirement",
        payload: JSON.parse(requirementCache)
      };
    } catch (e) {
      return null;
    } finally {
      sessionStorage.removeItem(LEGACY_GENERATE_REQUIREMENT_KEY);
      sessionStorage.removeItem(LEGACY_GENERATE_FUNCTION_KEY);
    }
  }

  const functionCache = sessionStorage.getItem(LEGACY_GENERATE_FUNCTION_KEY);
  if (!functionCache) {
    return null;
  }

  try {
    return {
      sourceType: "function",
      payload: JSON.parse(functionCache)
    };
  } catch (e) {
    return null;
  } finally {
    sessionStorage.removeItem(LEGACY_GENERATE_FUNCTION_KEY);
  }
};

const loadGenerateSourceFromTransfer = () => {
  const runtimeSource = consumeRuntimePageCache(RUNTIME_PAGE_CACHE_KEYS.generateIncomingSource);
  if (runtimeSource?.sourceType) {
    applyGenerateSource(runtimeSource.sourceType, runtimeSource.payload || {});
    return true;
  }

  const legacySource = consumeLegacyGenerateSource();
  if (legacySource?.sourceType) {
    applyGenerateSource(legacySource.sourceType, legacySource.payload || {});
    return true;
  }

  return false;
};

const syncGenerateModelSelections = () => {
  const availableModels = data.enabledModelList || [];
  const availableModelIds = new Set(availableModels.map(item => String(item.id)));
  const defaultModelId = availableModels.length > 0 ? availableModels[0].id : null;

  if (!availableModelIds.has(String(data.form.modelConfigId))) {
    data.form.modelConfigId = defaultModelId;
  }

  if (availableModelIds.has(String(data.form.evaluationModelConfigId))) {
    return;
  }

  const cachedEvaluationModelId = readDefaultEvaluationModelId();
  const matchedEvaluationModel = availableModels.find(item => item.id === cachedEvaluationModelId);
  data.form.evaluationModelConfigId = matchedEvaluationModel ? matchedEvaluationModel.id : defaultModelId;
};

const loadEnabledModelList = () => {
  request.get("/modelConfig/selectEnabledList", {
    params: {
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    }
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      data.enabledModelList = res.data || [];
      syncGenerateModelSelections();
      saveGeneratePageState();
    }
  }).catch(() => {});
};

const buildEvaluationSummary = (evaluation) => {
  if (!evaluation) return "";
  const suggestions = (evaluation.suggestions || []).map((item, index) => `${index + 1}. ${item}`).join("\n");

  if (data.form.outputLanguage === "en") {
    return [
      `Normal Path Coverage: ${evaluation.normalPathCoverage || "—"}`,
      `Boundary Coverage: ${evaluation.boundaryCoverage || "—"}`,
      `Exception Coverage: ${evaluation.exceptionCoverage || "—"}`,
      `Syntax Compliance: ${evaluation.syntaxNorm || "—"}`,
      `Overall Score: ${evaluation.score ?? "—"}`,
      `Suggestions:`,
      suggestions
    ].join("\n");
  }

  return [
    `正常路径覆盖：${evaluation.normalPathCoverage || "—"}`,
    `边界条件覆盖：${evaluation.boundaryCoverage || "—"}`,
    `异常分支覆盖：${evaluation.exceptionCoverage || "—"}`,
    `语法规范性：${evaluation.syntaxNorm || "—"}`,
    `综合评分：${evaluation.score ?? "—"}`,
    `改进建议：`,
    suggestions
  ].join("\n");
};

const applyPublicCompareResponse = (responseData = {}) => {
  data.publicCompareResult = responseData.publicCompareResult || "";
  data.publicCoveredCount = responseData.publicCoveredCount ?? 0;
  data.publicPartialCount = responseData.publicPartialCount ?? 0;
  data.publicMissingCount = responseData.publicMissingCount ?? 0;
  data.publicExtraResult = responseData.publicExtraResult || "";
  data.generatedCaseCount = responseData.generatedCaseCount ?? 0;
  data.publicMatchedCaseCount = responseData.publicMatchedCaseCount ?? 0;
  data.publicExtraCaseCount = responseData.publicExtraCaseCount ?? 0;
  data.publicInvalidCaseCount = responseData.publicInvalidCaseCount ?? 0;
  data.publicExtraRate = responseData.publicExtraRate ?? 0;
  data.publicExpandRate = responseData.publicExpandRate ?? 0;
};

const evaluateGeneratedResult = (payload, recordId, successMessage) => {
  data.evaluating = true;
  saveGeneratePageState();
  request.post("/generate/evaluate", {
    ...payload,
    resultText: data.resultText,
    recordId
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      data.evaluationResult = res.data || null;
      ElMessage.success(successMessage);
    } else {
      ElMessage.error(res.msg || "评估失败");
    }
    saveGeneratePageState();
  }).catch(() => {
    ElMessage.error("评估请求失败");
    saveGeneratePageState();
  }).finally(() => {
    data.evaluating = false;
    saveGeneratePageState();
  });
};

const validateForm = () => {
  if (!data.form.modelConfigId) {
    ElMessage.warning("请选择模型");
    return false;
  }

  if (!data.form.outputLanguage) {
    ElMessage.warning("请选择输出语言");
    return false;
  }

  if (data.form.sourceType === "requirement") {
    if (!data.form.requirementName) {
      ElMessage.warning("请输入需求名称");
      return false;
    }
    if (!data.form.requirementContent) {
      ElMessage.warning("请输入需求内容");
      return false;
    }
  } else {
    if (!data.form.functionName) {
      ElMessage.warning("请输入函数名称");
      return false;
    }
    if (!data.form.codeText) {
      ElMessage.warning("请输入函数代码");
      return false;
    }
  }

  return true;
};

const doGenerate = (isRegenerate) => {
  if (data.generating || data.evaluating) return;
  if (hasLockedGenerationTask.value) {
    ElMessage.warning(generationLockNotice.value);
    return;
  }
  if (!validateForm()) return;

  const payload = {
    ...data.form,
    regenerate: isRegenerate,
    previousResultText: isRegenerate ? data.resultText : "",
    previousEvaluationSummary: isRegenerate ? buildEvaluationSummary(data.evaluationResult) : "",
    recordId: null
  };

  data.generating = true;
  data.resultText = isRegenerate ? "正在再次生成测试用例，请稍候..." : "正在生成测试用例，请稍候...";
  data.evaluationResult = null;
  data.publicCompareResult = "";
  data.publicCoveredCount = 0;
  data.publicPartialCount = 0;
  data.publicMissingCount = 0;
  data.publicExtraResult = "";
  data.generatedCaseCount = 0;
  data.publicMatchedCaseCount = 0;
  data.publicExtraCaseCount = 0;
  data.publicInvalidCaseCount = 0;
  data.publicExtraRate = 0;
  data.publicExpandRate = 0;
  data.lastRecordId = null;
  saveGeneratePageState();

  request.post("/generate/testcase", payload).then(res => {
    if (res.code === "200" || res.code === 200) {
      const responseData = res.data || {};
      data.resultText = responseData.resultText || "";
      data.lastRecordId = responseData.recordId || null;
      applyPublicCompareResponse(responseData);

      const successMessage = isRegenerate ? "再次生成完成，正在自动评估..." : "生成完成，正在自动评估...";
      ElMessage.success(successMessage);
      saveGeneratePageState();

      setTimeout(() => {
        evaluateGeneratedResult(payload, data.lastRecordId, isRegenerate ? "再次生成评估完成" : "评估完成");
      }, 500);
    } else {
      ElMessage.error(res.msg || "生成失败");
      data.resultText = "生成失败：" + (res.msg || "未知错误");
      saveGeneratePageState();
    }
  }).catch(() => {
    ElMessage.error("请求失败");
    data.resultText = "请求失败，请检查后端服务或模型接口配置。";
    saveGeneratePageState();
  }).finally(() => {
    data.generating = false;
    saveGeneratePageState();
  });
};

const generateCase = () => {
  doGenerate(false);
};

const regenerateCase = () => {
  doGenerate(true);
};

const resetForm = () => {
  const defaultModelId = data.enabledModelList.length > 0 ? data.enabledModelList[0].id : null;
  const defaultEvaluationModelId = (() => {
    const cachedId = readDefaultEvaluationModelId();
    const matched = data.enabledModelList.find(item => item.id === cachedId);
    return matched ? matched.id : defaultModelId;
  })();
  data.form = buildDefaultForm(defaultModelId, defaultEvaluationModelId);
  data.resultText = "";
  data.evaluationResult = null;
  data.publicCompareResult = "";
  data.publicCoveredCount = 0;
  data.publicPartialCount = 0;
  data.publicMissingCount = 0;
  data.publicExtraResult = "";
  data.generatedCaseCount = 0;
  data.publicMatchedCaseCount = 0;
  data.publicExtraCaseCount = 0;
  data.publicInvalidCaseCount = 0;
  data.publicExtraRate = 0;
  data.publicExpandRate = 0;
  data.lastRecordId = null;
  data.generating = false;
  data.evaluating = false;
  saveGeneratePageState();
};

watch(
    () => ({
      form: data.form,
      resultText: data.resultText,
      evaluationResult: data.evaluationResult,
      publicCompareResult: data.publicCompareResult,
      publicCoveredCount: data.publicCoveredCount,
      publicPartialCount: data.publicPartialCount,
      publicMissingCount: data.publicMissingCount,
      publicExtraResult: data.publicExtraResult,
      generatedCaseCount: data.generatedCaseCount,
      publicMatchedCaseCount: data.publicMatchedCaseCount,
      publicExtraCaseCount: data.publicExtraCaseCount,
      publicInvalidCaseCount: data.publicInvalidCaseCount,
      publicExtraRate: data.publicExtraRate,
      publicExpandRate: data.publicExpandRate,
      lastRecordId: data.lastRecordId
    }),
    () => {
      saveGeneratePageState();
    },
    { deep: true }
);

onMounted(() => {
  const hasIncomingSource = loadGenerateSourceFromTransfer();
  if (!hasIncomingSource) {
    restoreGeneratePageState();
  }
  loadEnabledModelList();
  initializeBatchTaskState();
});

onBeforeUnmount(() => {
  stopBatchTaskPolling();
});
</script>

<style scoped>
.generate-page {
  display: flex;
  gap: 16px;
  align-items: stretch;
  height: calc(100vh - 84px);
  min-height: 0;
  overflow: hidden;
}

.left-panel,
.right-panel {
  flex: 1;
  min-width: 0;
  height: 100%;
  min-height: 0;
  padding: 18px 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.left-panel {
  overflow-y: auto;
}

.right-panel {
  overflow: hidden;
}

.panel-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 16px;
}

.batch-lock-notice {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff7e8;
  border: 1px solid #f3d19e;
  color: #8c5a00;
  line-height: 1.7;
}

.batch-lock-notice__title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
}

.generate-form {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.generate-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.form-actions {
  margin-top: auto;
  margin-bottom: 0;
}

.right-top {
  flex: 0 0 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.right-bottom {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding-top: 12px;
  overflow-y: auto;
  padding-right: 4px;
}

.sub-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
  color: #303133;
}

.evaluation-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.evaluation-title-row .sub-title {
  margin-bottom: 0;
}

.loading-line {
  min-height: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8c6b18;
  animation: dot-bounce 1.2s infinite ease-in-out;
}

.loading-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: scale(0.7);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.loading-tip {
  color: #8c6b18;
  font-size: 14px;
}

.result-wrapper {
  flex: 0 0 auto;
  height: clamp(260px, 34vh, 340px);
  max-height: 340px;
  min-height: 0;
  display: flex;
}

.result-textarea {
  width: 100%;
  height: 100%;
}

.result-textarea :deep(.el-textarea__inner) {
  height: 100% !important;
  resize: none;
  line-height: 1.6;
}

.evaluation-panel {
  flex: 0 0 auto;
  min-height: 0;
}

.evaluation-scroll-content {
  max-height: 240px;
  overflow-y: auto;
  padding-right: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.evaluation-empty {
  flex: 0 0 auto;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  background: #fafafa;
  border: 1px dashed #e5e5e5;
  border-radius: 8px;
}

.evaluation-top-row {
  display: flex;
  gap: 12px;
  align-items: stretch;
  min-height: 120px;
  flex-shrink: 0;
}

.score-card {
  width: 160px;
  flex-shrink: 0;
  border-radius: 10px;
  background: #faf8f2;
  border: 1px solid #ebe5d6;
  padding: 16px 12px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.score-label {
  font-size: 14px;
  color: #7a6d57;
}

.score-value {
  font-size: 34px;
  line-height: 1;
  font-weight: 700;
  color: #8c6b18;
}

.summary-card {
  flex: 1;
  min-width: 0;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  padding: 14px 16px;
  box-sizing: border-box;
}

.summary-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 10px;
}

.summary-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-item {
  color: #555;
  line-height: 1.5;
}

.summary-label {
  color: #333;
  font-weight: 600;
}

.coverage-card {
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  padding: 12px 16px;
  box-sizing: border-box;
  flex-shrink: 0;
}

.coverage-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.coverage-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 12px;
}

.coverage-item {
  min-width: 0;
  color: #555;
  line-height: 1.5;
}

.coverage-item--full {
  grid-column: 1 / -1;
}

.coverage-label {
  color: #333;
  font-weight: 600;
}

.suggestion-block {
  flex: 0 0 auto;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  padding: 14px 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.suggestion-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.suggestion-content {
  flex: 0 0 auto;
  overflow: visible;
  color: #555;
  line-height: 1.8;
}

.suggestion-content ul {
  margin: 0;
  padding-left: 20px;
}

.suggestion-content li {
  margin-bottom: 8px;
}

.public-compare-panel {
  margin-top: 12px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
  flex-shrink: 0;
}

.public-compare-content {
  max-height: 360px;
  overflow-y: auto;
  padding-right: 8px;
}

.public-compare-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 8px;
}

.public-compare-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
  color: #555;
  font-size: 13px;
}

.public-compare-stats > div {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 7px 8px;
}

.public-compare-empty {
  color: #909399;
  line-height: 1.7;
}

.public-compare-alert {
  margin-bottom: 10px;
}

.public-compare-subtitle {
  font-weight: 700;
  color: #5a2d0c;
  margin-bottom: 8px;
}

.public-analysis-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
  margin-bottom: 10px;
}

.public-analysis-card:last-child {
  margin-bottom: 0;
}

.public-compare-section-title {
  font-weight: 700;
  color: #303133;
  margin: 4px 0 8px;
}

.public-compare-list {
  max-height: none;
  overflow: visible;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.public-compare-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px 10px;
  background: #fff;
  line-height: 1.6;
  color: #555;
}

.public-compare-item-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #303133;
  font-family: Consolas, monospace;
  word-break: break-all;
}

.public-compare-reason,
.public-compare-suggest {
  margin-top: 4px;
  word-break: break-word;
}

@media (max-width: 1180px) {
  .coverage-grid {
    grid-template-columns: 1fr;
  }

  .public-compare-stats {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
