<template>
  <div class="history-page">
    <div class="card history-card">
      <div class="page-title">生成记录</div>

      <div class="table-wrapper">
        <el-table
            :data="pagedTableData"
            stripe
            height="100%"
            style="width: 100%"
            :row-style="getRowStyle"
            :header-cell-style="{ textAlign: 'center' }"
        >
          <el-table-column prop="id" label="编号" width="70" align="center" />

          <el-table-column prop="sourceType" label="来源类型" width="110" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.sourceType === 'requirement'" type="warning" effect="light">
                需求规格
              </el-tag>
              <el-tag v-else type="success" effect="light">
                函数
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="functionName" label="名称" min-width="160" show-overflow-tooltip align="center">
            <template #default="scope">
              <span class="function-name">{{ scope.row.functionName || "—" }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="modelName" label="模型名称" min-width="130" align="center">
            <template #default="scope">
              <span class="model-name">{{ scope.row.modelName || "—" }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="strategy" label="生成策略" min-width="120" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.strategy === 'cot'" type="warning" effect="light">
                链式分析生成
              </el-tag>
              <el-tag v-else type="success" effect="light">
                普通生成
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="生成结果" min-width="360">
            <template #default="scope">
              <div class="result-cell">
                <div class="result-card">
                  <div class="result-preview">{{ scope.row.resultText || "—" }}</div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="evaluationScore" label="大模型评估" width="110" align="center">
            <template #default="scope">
              <span class="score-text">{{ scope.row.evaluationScore ?? "—" }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="testerEvaluationScore" label="测试人员评估分数" width="150" align="center">
            <template #default="scope">
              <span class="score-text">{{ scope.row.testerEvaluationScore ?? "—" }}</span>
            </template>
          </el-table-column>

          <el-table-column label="公开测试基准对比分析" min-width="240" align="center">
            <template #default="scope">
              <div class="public-compare-cell">
                <div v-if="scope.row.publicCompareResult">
                  <div>基准覆盖率：{{ getRecordBenchmarkCoverageRateText(scope.row) }}</div>
                  <div>新增有效测试点：{{ scope.row.publicExtraCaseCount ?? 0 }}</div>
                  <div>增量补充率：{{ formatRate(scope.row.publicExtraRate) }}</div>
                  <div>覆盖扩展率：{{ formatRate(scope.row.publicExpandRate) }}</div>
                </div>
                <div v-else class="muted-text">暂无公开测试基准对比分析结果</div>
                <el-button
                    v-if="scope.row.publicCompareResult"
                    type="primary"
                    plain
                    size="small"
                    @click="openPublicCompareDialog(scope.row)"
                >
                  查看基准对比分析
                </el-button>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="coverage.py 覆盖率" min-width="230" align="center">
            <template #default="scope">
              <div class="coverage-cell">
                <div>语句覆盖率：{{ formatCoverage(scope.row.lineCoverage) }}</div>
                <div>分支覆盖率：{{ formatCoverage(scope.row.branchCoverage) }}</div>
                <div>
                  覆盖率状态：
                  <el-tag size="small" :type="getCoverageStatusTag(scope.row.coverageStatus)">
                    {{ scope.row.coverageStatus || "未统计" }}
                  </el-tag>
                </div>
                <el-tooltip
                    :disabled="!formatCoverageDescription(scope.row)"
                    placement="top"
                    effect="light"
                    popper-class="coverage-description-popper"
                >
                  <template #content>
                    <div class="coverage-tooltip-content">{{ formatCoverageDescription(scope.row) }}</div>
                  </template>
                  <span
                      class="coverage-message-entry"
                      :class="{ 'is-empty': !formatCoverageDescription(scope.row) }"
                  >
                    覆盖率说明：{{ formatCoverageDescription(scope.row) || "暂无说明" }}
                  </span>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="createTime" label="生成时间" min-width="170" align="center">
            <template #default="scope">
              <span class="time-text">{{ scope.row.createTime || "—" }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="scope">
              <div class="operation-buttons">
                <el-button type="primary" size="small" @click="exportRecord(scope.row)">
                  导出
                </el-button>
                <el-button type="danger" size="small" @click="handleDelete(scope.row)">
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
            background
            layout="prev, pager, next, total"
            :total="data.tableData.length"
            :page-size="data.pageSize"
            :current-page="data.currentPage"
            @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog v-model="data.publicCompareVisible" title="公开测试基准对比分析" width="920px" destroy-on-close>
      <div class="public-compare-dialog">
        <div class="public-dialog-stat-groups">
          <div class="public-dialog-stat-group">
            <div class="public-dialog-section-title">公开测试基准覆盖分析</div>
            <div class="public-compare-stats">
              <div>公开断言总数：{{ publicCompareDialogTotal }}</div>
              <div>已覆盖：{{ data.publicCompareRecord?.publicCoveredCount ?? 0 }}</div>
              <div>部分覆盖：{{ data.publicCompareRecord?.publicPartialCount ?? 0 }}</div>
              <div>缺失：{{ data.publicCompareRecord?.publicMissingCount ?? 0 }}</div>
              <div>基准覆盖率：{{ publicCompareDialogCoverageRateText }}</div>
            </div>
          </div>

          <div class="public-dialog-stat-group">
            <div class="public-dialog-section-title">生成用例增量价值分析</div>
            <div v-if="hasDialogIncrementalAnalysis" class="public-compare-stats public-compare-stats--six">
              <div>生成用例总数：{{ dialogGeneratedCaseCount }}</div>
              <div>匹配基准用例数：{{ dialogMatchedCaseCount }}</div>
              <div>新增有效测试点数：{{ dialogExtraCaseCount }}</div>
              <div>无效或重复测试点数：{{ dialogInvalidCaseCount }}</div>
              <div>增量补充率：{{ dialogExtraRateText }}</div>
              <div>覆盖扩展率：{{ dialogExpandRateText }}</div>
            </div>
            <div v-else class="public-compare-empty">暂无生成用例增量价值分析结果</div>
          </div>
        </div>

        <div class="public-dialog-detail-block">
          <div class="public-dialog-section-title">公开测试基准覆盖详情</div>
          <div v-if="publicCompareDialogSummary && !publicCompareDialogFailureMessage" class="public-compare-summary">
            {{ publicCompareDialogSummary }}
          </div>

          <el-alert
              v-if="publicCompareDialogFailureMessage"
              class="public-compare-alert"
              type="warning"
              :title="publicCompareDialogFailureMessage"
              show-icon
              :closable="false"
          />

          <div v-if="publicCompareDialogItems.length" class="public-compare-detail-list">
            <div v-for="(item, index) in publicCompareDialogItems" :key="index" class="public-compare-detail-item">
              <div class="public-compare-detail-head">
                <el-tag size="small" :type="getPublicCompareStatusTag(item.status)">
                  {{ getPublicCompareStatusText(item.status) }}
                </el-tag>
                <span>{{ item.assertion || "未返回断言原文" }}</span>
              </div>
              <div>原因：{{ item.reason || "暂无说明" }}</div>
              <div v-if="item.matchedGeneratedCase">匹配内容：{{ item.matchedGeneratedCase }}</div>
              <div v-if="!isPublicCompareCovered(item.status)">建议补充：{{ item.suggestedCase || "暂无建议" }}</div>
            </div>
          </div>

          <pre v-else class="public-compare-raw">{{ data.publicCompareRecord?.publicCompareResult || "暂无公开测试基准覆盖分析结果" }}</pre>
        </div>

        <div class="public-dialog-detail-block">
          <div class="public-dialog-section-title">生成用例增量价值详情</div>
          <div v-if="!hasDialogIncrementalAnalysis" class="public-compare-empty">
            暂无生成用例增量价值分析结果
          </div>

          <template v-else>
            <div v-if="dialogIncrementalSummary" class="public-compare-summary">
              {{ dialogIncrementalSummary }}
            </div>

            <div v-if="dialogExtraItems.length" class="public-compare-detail-list">
              <div v-for="(item, index) in dialogExtraItems" :key="`extra-${index}`" class="public-compare-detail-item">
                <div class="public-compare-detail-head">
                  <el-tag size="small" type="success">新增有效</el-tag>
                  <span>{{ item.generatedCaseId || `TC-${String(index + 1).padStart(3, "0")}` }}：{{ item.scenario || "未返回场景说明" }}</span>
                </div>
                <div>类型：{{ getIncrementalTypeText(item.type) }}</div>
                <div>原因：{{ item.reason || "暂无说明" }}</div>
                <div>价值：{{ item.value || "暂无价值说明" }}</div>
              </div>
            </div>

            <div v-if="dialogInvalidItems.length" class="public-compare-detail-list public-compare-detail-list--spaced">
              <div v-for="(item, index) in dialogInvalidItems" :key="`invalid-${index}`" class="public-compare-detail-item">
                <div class="public-compare-detail-head">
                  <el-tag size="small" type="info">无效/重复</el-tag>
                  <span>{{ item.generatedCaseId || `TC-${String(index + 1).padStart(3, "0")}` }}</span>
                </div>
                <div>原因：{{ item.reason || "暂无说明" }}</div>
              </div>
            </div>

            <div v-if="!dialogExtraItems.length && !dialogInvalidItems.length" class="public-compare-empty">
              暂无新增有效测试点详情
            </div>
          </template>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, onMounted, computed } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import * as XLSX from "xlsx";

const currentUser = JSON.parse(localStorage.getItem("xm-user") || "{}");
const currentRole = currentUser.loginType === "USER" ? "USER" : currentUser.role;

const data = reactive({
  tableData: [],
  currentPage: 1,
  pageSize: 6,
  publicCompareVisible: false,
  publicCompareRecord: null
});

const load = ({ preservePage = false } = {}) => {
  request.get("/generateRecord/selectAll", {
    params: {
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    }
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      data.tableData = res.data || [];
      if (!preservePage) {
        data.currentPage = 1;
      }

      const maxPage = Math.max(1, Math.ceil(data.tableData.length / data.pageSize));
      if (data.currentPage > maxPage) {
        data.currentPage = maxPage;
      }
    } else {
      ElMessage.error(res.msg || "加载失败");
    }
  }).catch(() => {
    ElMessage.error("请求失败");
  });
};

const pagedTableData = computed(() => {
  const start = (data.currentPage - 1) * data.pageSize;
  const end = start + data.pageSize;
  return data.tableData.slice(start, end);
});

const handleCurrentChange = (page) => {
  data.currentPage = page;
};

const getRowStyle = () => {
  return {
    height: "132px"
  };
};

const normalizeLine = (line) => {
  return (line || "")
      .replace(/\r/g, "")
      .replace(/^[-•]\s*/, "")
      .trim();
};

const getPureText = (text) => {
  return (text || "").replace(/\r/g, "").trim();
};

const formatCoverage = (value) => {
  const number = Number(value);
  return Number.isFinite(number) ? `${number.toFixed(2)}%` : "未统计";
};

const COVERAGE_FAILURE_STATUSES = new Set(["TEST_FAILED", "ERROR", "FAILED"]);
const COVERAGE_EXCEPTION_NAME_PATTERN =
    "(?:[A-Za-z_][\\w.]*?(?:Error|Exception|Warning)|AssertionError|TypeError|AttributeError|SyntaxError|NameError|ValueError|KeyError|IndexError|ImportError|ModuleNotFoundError|ZeroDivisionError|RuntimeError|NotImplementedError|TimeoutError|StopIteration|StopAsyncIteration|KeyboardInterrupt|SystemExit|GeneratorExit|BaseException|Exception)";
const COVERAGE_EXCEPTION_LINE_REGEXP = new RegExp(
    `^(?:E\\s+|ERROR:\\s*)?(${COVERAGE_EXCEPTION_NAME_PATTERN})\\s*:\\s*(.+)?$`
);
const COVERAGE_EMBEDDED_EXCEPTION_REGEXP = new RegExp(
    `(?:^|\\s)(${COVERAGE_EXCEPTION_NAME_PATTERN})\\s*:\\s*(.+)$`
);
const COVERAGE_EXCEPTION_ONLY_LINE_REGEXP = new RegExp(
    `^(?:E\\s+)?(${COVERAGE_EXCEPTION_NAME_PATTERN})$`
);
const COVERAGE_FAILURE_KEYWORD_REGEXP = new RegExp(
    `Traceback|\\bERROR\\b:?|\\bFAILED\\b|${COVERAGE_EXCEPTION_NAME_PATTERN}\\s*:`,
    "i"
);

const getRawCoverageDescription = (row) => {
  const fields = [
    row?.coverageDescription,
    row?.coverageRemark,
    row?.coverageMessage
  ];
  const text = fields.find(item => String(item || "").trim());
  return String(text || "").trim();
};

const stripCoverageAnsi = (text) => {
  return String(text || "")
      .replace(/\x1B\[[0-?]*[ -/]*[@-~]/g, "")
      .replace(/\r/g, "\n");
};

const sanitizeCoveragePathText = (text) => {
  return String(text || "")
      .replace(/[A-Za-z]:\\[^\s"'<>，。；;]+/g, "[路径]")
      .replace(/\/[^\s"'<>，。；;]*(?:tmp|temp|pytest|coverage)[^\s"'<>，。；;]*/gi, "[路径]");
};

const cleanCoverageLogLine = (line) => {
  return sanitizeCoveragePathText(stripCoverageAnsi(line))
      .replace(/^[|>\s]+/, "")
      .replace(/^\[?(?:ERROR|FAILED|FAIL|WARN|WARNING)\]?\s*[:：-]?\s*/i, "")
      .replace(/\s+/g, " ")
      .trim();
};

const isCoverageNoiseLine = (line) => {
  return !line ||
      /^[-=_*]{3,}$/.test(line) ||
      /^Traceback \(most recent call last\):$/i.test(line) ||
      /^File ["']?.+["']?, line \d+/i.test(line) ||
      /^FAILED\s*\(.*\)$/i.test(line) ||
      /^ERROR:?\s*$/i.test(line);
};

const compactCoverageText = (text) => {
  return stripCoverageAnsi(text)
      .split("\n")
      .map(item => cleanCoverageLogLine(item))
      .filter(item => !isCoverageNoiseLine(item))
      .join(" ")
      .replace(/\s+/g, " ")
      .trim();
};

const extractCoverageExceptionReason = (text) => {
  const reasons = stripCoverageAnsi(text)
      .split("\n")
      .map(item => cleanCoverageLogLine(item))
      .filter(item => !isCoverageNoiseLine(item))
      .map((line) => {
        const directMatch = line.match(COVERAGE_EXCEPTION_LINE_REGEXP);
        const embeddedMatch = directMatch ? null : line.match(COVERAGE_EMBEDDED_EXCEPTION_REGEXP);
        const onlyLineMatch = directMatch || embeddedMatch ? null : line.match(COVERAGE_EXCEPTION_ONLY_LINE_REGEXP);
        const match = directMatch || embeddedMatch || onlyLineMatch;

        if (!match) {
          return "";
        }

        const exceptionName = match[1];
        const exceptionMessage = String(match[2] || "").trim();
        return exceptionMessage ? `${exceptionName}: ${exceptionMessage}` : exceptionName;
      })
      .filter(Boolean);

  return reasons.length ? reasons[reasons.length - 1] : "";
};

const shouldSummarizeCoverageDescription = (row, rawText) => {
  const status = String(row?.coverageStatus || "").trim().toUpperCase();
  if (COVERAGE_FAILURE_STATUSES.has(status)) {
    return true;
  }
  return COVERAGE_FAILURE_KEYWORD_REGEXP.test(rawText);
};

const formatCoverageDescription = (row) => {
  const rawText = getRawCoverageDescription(row);
  const status = String(row?.coverageStatus || "").trim().toUpperCase();

  if (status === "SUCCESS" && !shouldSummarizeCoverageDescription(row, rawText)) {
    return compactCoverageText(rawText) || "覆盖率统计成功";
  }

  if (!rawText && !COVERAGE_FAILURE_STATUSES.has(status)) {
    return "";
  }

  if (shouldSummarizeCoverageDescription(row, rawText)) {
    const reason = extractCoverageExceptionReason(rawText);
    return reason
        ? `测试代码执行失败：${reason}`
        : "测试代码执行失败，覆盖率统计未完成";
  }

  return compactCoverageText(rawText);
};

const toNumber = (value) => {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
};

const formatRate = (value) => {
  const number = toNumber(value);
  return number === null ? "暂无数据" : `${number.toFixed(2)}%`;
};

const getCoverageStatusTag = (status) => {
  const normalizedStatus = String(status || "").trim().toUpperCase();
  if (normalizedStatus === "SUCCESS") return "success";
  if (normalizedStatus === "TEST_FAILED") return "warning";
  if (normalizedStatus === "FAILED" || normalizedStatus === "ERROR") return "danger";
  if (normalizedStatus === "SKIPPED") return "info";
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

const publicCompareDialogDetail = computed(() => parsePublicCompareResult(data.publicCompareRecord?.publicCompareResult));

const publicExtraDialogDetail = computed(() => parsePublicCompareResult(data.publicCompareRecord?.publicExtraResult));

const publicCompareDialogBenchmark = computed(() => {
  const detail = publicCompareDialogDetail.value;
  return detail?.benchmarkCoverageAnalysis || detail;
});

const publicCompareDialogIncremental = computed(() => {
  return publicExtraDialogDetail.value || publicCompareDialogDetail.value?.incrementalValueAnalysis || null;
});

const publicCompareDialogItems = computed(() => {
  return Array.isArray(publicCompareDialogBenchmark.value?.items) ? publicCompareDialogBenchmark.value.items : [];
});

const publicCompareDialogTotal = computed(() => {
  const detailTotal = Number(publicCompareDialogBenchmark.value?.publicTotal ?? publicCompareDialogBenchmark.value?.total);
  if (Number.isFinite(detailTotal) && detailTotal >= 0) {
    return detailTotal;
  }
  return (data.publicCompareRecord?.publicCoveredCount || 0)
      + (data.publicCompareRecord?.publicPartialCount || 0)
      + (data.publicCompareRecord?.publicMissingCount || 0);
});

const publicCompareDialogSummary = computed(() => {
  return publicCompareDialogBenchmark.value?.summary || "";
});

const publicCompareDialogFailureMessage = computed(() => {
  const message = publicCompareDialogSummary.value || publicCompareDialogDetail.value?.rawText || "";
  return String(message || "").includes("公开测试基准对比分析失败") ? message : "";
});

const getRecordBenchmarkCoverageRate = (row) => {
  const detail = parsePublicCompareResult(row?.publicCompareResult);
  const benchmark = detail?.benchmarkCoverageAnalysis || detail || {};
  const detailRate = toNumber(benchmark.benchmarkCoverageRate);
  if (detailRate !== null) {
    return detailRate;
  }
  const covered = Number(row?.publicCoveredCount || 0);
  const partial = Number(row?.publicPartialCount || 0);
  const missing = Number(row?.publicMissingCount || 0);
  const total = toNumber(benchmark.publicTotal ?? benchmark.total) ?? (covered + partial + missing);
  return total > 0 ? (covered * 100) / total : null;
};

const getRecordBenchmarkCoverageRateText = (row) => {
  return formatRate(getRecordBenchmarkCoverageRate(row));
};

const publicCompareDialogCoverageRate = computed(() => getRecordBenchmarkCoverageRate(data.publicCompareRecord));

const publicCompareDialogCoverageRateText = computed(() => formatRate(publicCompareDialogCoverageRate.value));

const dialogGeneratedCaseCount = computed(() => {
  return toNumber(publicCompareDialogIncremental.value?.generatedCaseCount) ?? Number(data.publicCompareRecord?.generatedCaseCount || 0);
});

const dialogMatchedCaseCount = computed(() => {
  return toNumber(publicCompareDialogIncremental.value?.publicMatchedCaseCount) ?? Number(data.publicCompareRecord?.publicMatchedCaseCount || 0);
});

const dialogExtraCaseCount = computed(() => {
  return toNumber(publicCompareDialogIncremental.value?.extraValidCount) ?? Number(data.publicCompareRecord?.publicExtraCaseCount || 0);
});

const dialogInvalidCaseCount = computed(() => {
  return toNumber(publicCompareDialogIncremental.value?.invalidOrDuplicateCount) ?? Number(data.publicCompareRecord?.publicInvalidCaseCount || 0);
});

const dialogExtraRate = computed(() => {
  return toNumber(publicCompareDialogIncremental.value?.extraRate) ?? toNumber(data.publicCompareRecord?.publicExtraRate);
});

const dialogExpandRate = computed(() => {
  return toNumber(publicCompareDialogIncremental.value?.expandRate) ?? toNumber(data.publicCompareRecord?.publicExpandRate);
});

const dialogExtraRateText = computed(() => formatRate(dialogExtraRate.value));

const dialogExpandRateText = computed(() => formatRate(dialogExpandRate.value));

const dialogExtraItems = computed(() => {
  return Array.isArray(publicCompareDialogIncremental.value?.extraItems) ? publicCompareDialogIncremental.value.extraItems : [];
});

const dialogInvalidItems = computed(() => {
  return Array.isArray(publicCompareDialogIncremental.value?.invalidItems) ? publicCompareDialogIncremental.value.invalidItems : [];
});

const dialogIncrementalSummary = computed(() => {
  return publicCompareDialogIncremental.value?.summary || publicCompareDialogIncremental.value?.rawText || "";
});

const hasDialogIncrementalAnalysis = computed(() => {
  return !!data.publicCompareRecord?.publicExtraResult
      || !!publicCompareDialogDetail.value?.incrementalValueAnalysis
      || dialogExtraItems.value.length > 0
      || dialogInvalidItems.value.length > 0
      || dialogGeneratedCaseCount.value > 0
      || dialogExtraCaseCount.value > 0;
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

const isPublicCompareCovered = (status) => {
  return String(status || "").toLowerCase() === "covered";
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

const openPublicCompareDialog = (row) => {
  data.publicCompareRecord = row || null;
  data.publicCompareVisible = true;
};

const isCaseStartLine = (line) => {
  const text = normalizeLine(line);
  return /^用例编号[:：]/i.test(text) || /^Test\s*Case\s*ID\s*:/i.test(text);
};

const extractValue = (line, patterns) => {
  const text = normalizeLine(line);
  for (const pattern of patterns) {
    if (pattern.test(text)) {
      return text.replace(pattern, "").trim();
    }
  }
  return null;
};

const exportCodeBoundaryLinePatterns = [
  /^```/i,
  /^import\s+unittest\b/i,
  /^class\s+Test\b/i,
  /^public\s+class\b/i,
  /^@Test\b/i,
  /^以下是测试代码[:：]?/i,
  /^测试代码如下[:：]?/i
];

const exportCodeBoundaryInlinePatterns = [
  /```/i,
  /import\s+unittest\b/i,
  /\bclass\s+Test\w*\b/i,
  /\bpublic\s+class\b/i,
  /@Test\b/i,
  /以下是测试代码[:：]?/i,
  /测试代码如下[:：]?/i
];

const isExportCodeBoundaryLine = (line) => {
  const text = normalizeLine(line);
  return exportCodeBoundaryLinePatterns.some(pattern => pattern.test(text));
};

const splitBeforeExportCodeBoundary = (text) => {
  const raw = String(text || "");
  const boundaryIndex = exportCodeBoundaryInlinePatterns.reduce((minIndex, pattern) => {
    const index = raw.search(pattern);
    if (index === -1) {
      return minIndex;
    }
    return minIndex === -1 ? index : Math.min(minIndex, index);
  }, -1);

  return {
    text: boundaryIndex === -1 ? raw.trim() : raw.slice(0, boundaryIndex).trim(),
    hasBoundary: boundaryIndex !== -1
  };
};

const removeExportCodeSection = (text) => {
  const lines = String(text || "").replace(/\r/g, "").split("\n");
  const keptLines = [];

  for (const line of lines) {
    const splitResult = splitBeforeExportCodeBoundary(line);

    if (isExportCodeBoundaryLine(line) || splitResult.hasBoundary) {
      if (splitResult.text) {
        keptLines.push(splitResult.text);
      }
      break;
    }

    keptLines.push(line);
  }

  return keptLines.join("\n").trim();
};

const extractTestCaseSection = (text) => {
  const pureText = getPureText(text);
  if (!pureText) return "";

  const lines = pureText.split("\n");
  let startIndex = -1;

  for (let i = 0; i < lines.length; i++) {
    const line = normalizeLine(lines[i]);

    if (
        /^测试用例[:：]?$/i.test(line) ||
        /^第二部分[:：]?\s*测试用例$/i.test(line) ||
        /^Part\s*2\s*:\s*Test\s*Cases$/i.test(line) ||
        /^Test\s*Cases[:：]?$/i.test(line) ||
        isCaseStartLine(line)
    ) {
      startIndex = i;
      break;
    }
  }

  if (startIndex === -1) {
    return removeExportCodeSection(pureText);
  }

  let sliced = lines.slice(startIndex).join("\n").trim();

  sliced = sliced
      .replace(/^测试用例[:：]?\s*/i, "")
      .replace(/^第二部分[:：]?\s*测试用例\s*/i, "")
      .replace(/^Part\s*2\s*:\s*Test\s*Cases\s*/i, "")
      .replace(/^Test\s*Cases[:：]?\s*/i, "")
      .trim();

  return removeExportCodeSection(sliced);
};

const parseResultToRows = (resultText) => {
  const caseOnlyText = extractTestCaseSection(resultText);
  const text = getPureText(caseOnlyText);
  if (!text) return [];

  const lines = text
      .split("\n")
      .map(item => normalizeLine(item))
      .filter(Boolean);

  const rows = [];
  let current = null;
  let currentField = "";
  let shouldStopParsing = false;

  const createEmptyCase = () => ({
    用例编号: "",
    测试目的: "",
    输入数据: "",
    执行步骤: "",
    预期结果: ""
  });

  const cleanExportCellValue = (value) => splitBeforeExportCodeBoundary(value).text.trim();

  const cleanParsedValue = (value) => {
    const splitResult = splitBeforeExportCodeBoundary(value);
    if (splitResult.hasBoundary) {
      shouldStopParsing = true;
    }
    return splitResult.text;
  };

  const pushCurrentIfNeeded = () => {
    if (!current) return;
    const hasValue = Object.values(current).some(v => String(v || "").trim());
    if (hasValue) {
      rows.push({
        用例编号: cleanExportCellValue(current.用例编号),
        测试目的: cleanExportCellValue(current.测试目的),
        输入数据: cleanExportCellValue(current.输入数据),
        执行步骤: cleanExportCellValue(current.执行步骤),
        预期结果: cleanExportCellValue(current.预期结果)
      });
    }
  };

  const appendToField = (field, value) => {
    if (!current || !field || !value) return;
    const cleanValue = cleanParsedValue(value);
    if (!cleanValue) return;
    current[field] = current[field] ? `${current[field]}\n${cleanValue}` : cleanValue;
  };

  for (const line of lines) {
    if (isExportCodeBoundaryLine(line)) {
      break;
    }

    const caseNo = extractValue(line, [
      /^用例编号[:：]\s*/i,
      /^Test\s*Case\s*ID\s*:\s*/i
    ]);
    if (caseNo !== null) {
      pushCurrentIfNeeded();
      current = createEmptyCase();
      current.用例编号 = cleanParsedValue(caseNo);
      currentField = "用例编号";
      if (shouldStopParsing) break;
      continue;
    }

    if (!current) {
      current = createEmptyCase();
    }

    const purpose = extractValue(line, [
      /^测试目的[:：]\s*/i,
      /^Test\s*Objective\s*:\s*/i
    ]);
    if (purpose !== null) {
      current.测试目的 = cleanParsedValue(purpose);
      currentField = "测试目的";
      if (shouldStopParsing) break;
      continue;
    }

    const input = extractValue(line, [
      /^输入数据[:：]\s*/i,
      /^Input\s*Data\s*:\s*/i
    ]);
    if (input !== null) {
      current.输入数据 = cleanParsedValue(input);
      currentField = "输入数据";
      if (shouldStopParsing) break;
      continue;
    }

    const steps = extractValue(line, [
      /^执行步骤[:：]\s*/i,
      /^Execution\s*Steps\s*:\s*/i
    ]);
    if (steps !== null) {
      current.执行步骤 = cleanParsedValue(steps);
      currentField = "执行步骤";
      if (shouldStopParsing) break;
      continue;
    }

    const expected = extractValue(line, [
      /^预期结果[:：]\s*/i,
      /^Expected\s*Result\s*:\s*/i
    ]);
    if (expected !== null) {
      current.预期结果 = cleanParsedValue(expected);
      currentField = "预期结果";
      if (shouldStopParsing) break;
      continue;
    }

    if (
        /^测试用例[:：]?$/i.test(line) ||
        /^第二部分[:：]?\s*测试用例$/i.test(line) ||
        /^Part\s*2\s*:\s*Test\s*Cases$/i.test(line) ||
        /^Test\s*Cases[:：]?$/i.test(line)
    ) {
      continue;
    }

    if (currentField) {
      appendToField(currentField, line);
      if (shouldStopParsing) break;
    } else {
      appendToField("执行步骤", line);
      currentField = "执行步骤";
      if (shouldStopParsing) break;
    }
  }

  pushCurrentIfNeeded();

  if (rows.length > 0) {
    return rows;
  }

  return removeExportCodeSection(text).split("\n").filter(Boolean).map((line, index) => ({
    用例编号: `原文-${index + 1}`,
    测试目的: "",
    输入数据: "",
    执行步骤: cleanExportCellValue(line),
    预期结果: ""
  }));
};

const exportRecord = (row) => {
  const rows = parseResultToRows(row.resultText);

  const ws = XLSX.utils.json_to_sheet(rows, {
    header: ["用例编号", "测试目的", "输入数据", "执行步骤", "预期结果"]
  });

  ws["!cols"] = [
    { wch: 18 },
    { wch: 32 },
    { wch: 32 },
    { wch: 48 },
    { wch: 42 }
  ];

  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "测试用例");

  const typeText = row.sourceType === "requirement" ? "需求规格" : "函数";
  const nameText = (row.functionName || "未命名").replace(/[\\/:*?"<>|]/g, "_");
  const timeText = row.createTime ? String(row.createTime).replace(/[: ]/g, "-") : "time";
  const fileName = `${typeText}_${nameText}_${timeText}.xlsx`;

  XLSX.writeFile(wb, fileName);
};

const handleDelete = async (row) => {
  if (!row?.id) {
    ElMessage.error("记录编号不存在");
    return;
  }

  try {
    await ElMessageBox.confirm(
        "删除后无法恢复，确认删除这条生成记录吗？",
        "删除确认",
        {
          type: "warning",
          confirmButtonText: "确认删除",
          cancelButtonText: "取消"
        }
    );
  } catch (e) {
    return;
  }

  try {
    const res = await request.delete(`/generateRecord/delete/${row.id}`, {
      params: {
        currentUserId: currentUser.id,
        currentUserRole: currentRole
      }
    });

    if (res.code === "200" || res.code === 200) {
      ElMessage.success("删除成功");
      load({ preservePage: true });
    } else {
      ElMessage.error(res.msg || "删除失败");
    }
  } catch (e) {
    ElMessage.error("删除失败");
  }
};

onMounted(() => {
  load();
});
</script>

<style scoped>
.history-page {
  height: calc(100vh - 84px);
  padding: 12px 0;
  box-sizing: border-box;
}

.history-card {
  height: 100%;
  padding: 16px 20px 12px 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.table-wrapper {
  flex: 1;
  min-height: 0;
}

.pagination-wrapper {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

.operation-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.function-name {
  font-weight: 600;
  color: #333;
}

.model-name,
.time-text,
.score-text {
  color: #555;
}

.coverage-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #555;
  font-size: 13px;
  line-height: 1.4;
  text-align: left;
}

.coverage-message-entry {
  display: block;
  max-width: 100%;
  color: #555;
  cursor: help;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coverage-message-entry.is-empty {
  color: #909399;
  cursor: default;
}

.coverage-tooltip-content {
  max-width: 420px;
  max-height: 96px;
  overflow: hidden;
  line-height: 1.6;
  white-space: normal;
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

:global(.coverage-description-popper) {
  max-width: 440px;
}

.result-cell {
  width: 100%;
  text-align: left;
}

.result-card {
  width: 100%;
  max-height: 84px;
  overflow: auto;
  background: #faf8f2;
  border: 1px solid #ebe5d6;
  border-radius: 8px;
  padding: 8px 10px;
  box-sizing: border-box;
  text-align: left;
}

.result-preview {
  white-space: pre-wrap;
  line-height: 1.5;
  color: #555;
  text-align: left;
}

.muted-text {
  color: #909399;
}

.public-compare-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  justify-content: center;
  color: #555;
  font-size: 13px;
  line-height: 1.5;
}

.public-compare-dialog {
  color: #555;
}

.public-dialog-stat-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 14px;
}

.public-dialog-stat-group,
.public-dialog-detail-block {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.public-dialog-detail-block {
  margin-top: 12px;
}

.public-dialog-section-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 10px;
}

.public-compare-stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.public-compare-stats > div {
  border: 1px solid #ebeef5;
  background: #fafafa;
  border-radius: 6px;
  padding: 8px 10px;
}

.public-compare-stats--six {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.public-compare-empty {
  color: #909399;
  line-height: 1.7;
}

.public-compare-alert {
  margin-bottom: 12px;
}

.public-compare-summary {
  margin-bottom: 12px;
  line-height: 1.7;
  color: #303133;
}

.public-compare-detail-list {
  max-height: 480px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.public-compare-detail-list--spaced {
  margin-top: 10px;
}

.public-compare-detail-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  line-height: 1.7;
  background: #fff;
}

.public-compare-detail-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
  color: #303133;
  font-family: Consolas, monospace;
  word-break: break-all;
}

.public-compare-raw {
  margin: 0;
  max-height: 420px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
}

@media (max-width: 900px) {
  .public-compare-stats,
  .public-compare-stats--six {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
