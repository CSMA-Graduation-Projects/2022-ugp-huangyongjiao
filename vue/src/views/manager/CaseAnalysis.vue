<template>
  <div class="analysis-page" v-loading="data.loading">
    <div class="analysis-layout">
      <div class="card overview-card">
        <div class="overview-badge">数据分析 / 图表展示</div>

        <div class="page-title-row">
          <div class="title-with-tip">
            <div class="page-title">用例分析</div>
            <el-tooltip placement="top" effect="light">
              <template #content>
                <div class="tooltip-content">{{ pageIntroTip }}</div>
              </template>
              <el-icon class="hint-icon"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
          <el-button plain :loading="data.loading" @click="loadData">刷新统计</el-button>
        </div>

        <div class="overview-stats">
          <div class="overview-stat-card">
            <div class="overview-stat-label">已评估记录</div>
            <div class="overview-stat-value">{{ summary.scoredRecords }}</div>
          </div>
          <div class="overview-stat-card">
            <div class="overview-stat-label">策略记录分布</div>
            <div class="overview-stat-value overview-stat-value--inline">
              {{ summary.normalRecords }}（普通） / {{ summary.cotRecords }}（链式）
            </div>
          </div>
          <div class="overview-stat-card">
            <div class="overview-stat-label">整体平均分</div>
            <div class="overview-stat-value">{{ summary.averageScoreText }}</div>
          </div>
          <div class="overview-stat-card">
            <div class="overview-stat-label">函数覆盖数</div>
            <div class="overview-stat-value">{{ chartFunctionOptions.length }}</div>
          </div>
          <div class="overview-stat-card">
            <div class="overview-stat-label">coverage 成功记录</div>
            <div class="overview-stat-value">{{ summary.coverageSuccessCount }}</div>
          </div>
          <div class="overview-stat-card">
            <div class="overview-stat-label">平均语句 / 分支覆盖率</div>
            <div class="overview-stat-value overview-stat-value--inline">
              {{ summary.averageLineCoverageText }}（分支 {{ summary.averageBranchCoverageText }}）
            </div>
          </div>
        </div>

        <div class="public-benchmark-summary">
          <div class="public-benchmark-title">公开测试基准对比统计</div>
          <div class="public-benchmark-grid">
            <div>
              <span>参与对比记录数</span>
              <b>{{ publicBenchmarkStats.compareRecordCount }}</b>
            </div>
            <div>
              <span>平均基准覆盖率</span>
              <b>{{ publicBenchmarkStats.averageBenchmarkCoverageRateText }}</b>
            </div>
            <div>
              <span>平均新增有效测试点数</span>
              <b>{{ publicBenchmarkStats.averageExtraCaseCountText }}</b>
            </div>
            <div>
              <span>平均增量补充率</span>
              <b>{{ publicBenchmarkStats.averageExtraRateText }}</b>
            </div>
            <div>
              <span>平均覆盖扩展率</span>
              <b>{{ publicBenchmarkStats.averageExpandRateText }}</b>
            </div>
          </div>
        </div>
      </div>

      <div class="card chart-card chart-card--strategy">
        <div class="chart-header">
          <div>
            <div class="title-with-tip chart-title-row">
              <div class="chart-title">普通生成和链式生成策略对比</div>
              <el-tooltip placement="top" effect="light">
                <template #content>
                  <div class="tooltip-content">{{ strategyChartTip }}</div>
                </template>
                <el-icon class="hint-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </div>

          <div class="legend-group">
            <div v-for="item in strategyMetricLegend" :key="item.label" class="legend-item">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </div>

        <div class="strategy-chart-grid">
          <div class="strategy-mini-chart">
            <div class="strategy-mini-title">评估分数对比</div>
            <svg viewBox="0 0 360 300" class="chart-svg strategy-mini-svg">
              <defs>
                <linearGradient id="strategyScoreMiniGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" style="stop-color: var(--el-color-primary-light-7)" />
                  <stop offset="100%" style="stop-color: var(--theme-chart-bar)" />
                </linearGradient>
              </defs>

              <template v-if="hasStrategyScoreChartData">
                <g v-for="tick in strategyScoreTicks" :key="`strategy-score-${tick.value}`">
                  <line :x1="strategyMiniChartLeft" :y1="tick.y" :x2="strategyMiniChartRight" :y2="tick.y" class="grid-line" />
                  <text :x="strategyMiniChartLeft - 9" :y="tick.y + 4" text-anchor="end" class="axis-text">
                    {{ tick.label }}
                  </text>
                </g>

                <line :x1="strategyMiniChartLeft" :y1="strategyMiniChartTop" :x2="strategyMiniChartLeft" :y2="strategyMiniChartBottom" class="axis-line" />
                <line :x1="strategyMiniChartLeft" :y1="strategyMiniChartBottom" :x2="strategyMiniChartRight" :y2="strategyMiniChartBottom" class="axis-line" />

                <g v-for="item in strategyScoreBars" :key="item.key">
                  <rect
                    v-if="item.value !== null"
                    :x="item.x"
                    :y="item.y"
                    :width="item.barWidth"
                    :height="item.height"
                    rx="10"
                    :fill="item.fill"
                  >
                    <title>{{ `${item.label} 评估分数：${item.valueText}` }}</title>
                  </rect>

                  <text
                    :x="item.centerX"
                    :y="item.valueY"
                    text-anchor="middle"
                    class="value-text"
                    :style="{ fill: item.color }"
                  >
                    {{ item.valueText }}
                  </text>

                  <text :x="item.centerX" :y="strategyMiniChartLabelY" text-anchor="middle" class="axis-strong-text">
                    {{ item.label }}
                  </text>
                  <text :x="item.centerX" :y="strategyMiniChartCountY" text-anchor="middle" class="axis-text">
                    {{ item.countText }}
                  </text>
                </g>
              </template>

              <g v-else>
                <text x="180" y="142" text-anchor="middle" class="empty-text strategy-empty-text">暂无评分数据</text>
                <text x="180" y="168" text-anchor="middle" class="empty-subtext">当前记录中还没有可用于平均的评估分数</text>
              </g>
            </svg>
          </div>

          <div class="strategy-mini-chart">
            <div class="strategy-mini-title">覆盖扩展率对比</div>
            <svg viewBox="0 0 360 300" class="chart-svg strategy-mini-svg">
              <defs>
                <linearGradient id="strategyExpandMiniGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" style="stop-color: var(--el-color-primary-light-3)" />
                  <stop offset="100%" style="stop-color: var(--theme-chart-line)" />
                </linearGradient>
              </defs>

              <template v-if="hasStrategyExpandChartData">
                <g v-for="tick in strategyExpandTicks" :key="`strategy-expand-${tick.value}`">
                  <line :x1="strategyMiniChartLeft" :y1="tick.y" :x2="strategyMiniChartRight" :y2="tick.y" class="grid-line" />
                  <text :x="strategyMiniChartLeft - 9" :y="tick.y + 4" text-anchor="end" class="axis-text">
                    {{ tick.label }}
                  </text>
                </g>

                <line :x1="strategyMiniChartLeft" :y1="strategyMiniChartTop" :x2="strategyMiniChartLeft" :y2="strategyMiniChartBottom" class="axis-line" />
                <line :x1="strategyMiniChartLeft" :y1="strategyMiniChartBottom" :x2="strategyMiniChartRight" :y2="strategyMiniChartBottom" class="axis-line" />

                <g v-for="item in strategyExpandBars" :key="item.key">
                  <rect
                    v-if="item.value !== null"
                    :x="item.x"
                    :y="item.y"
                    :width="item.barWidth"
                    :height="item.height"
                    rx="10"
                    :fill="item.fill"
                  >
                    <title>{{ `${item.label} 覆盖扩展率：${item.valueText}` }}</title>
                  </rect>

                  <text
                    :x="item.centerX"
                    :y="item.valueY"
                    text-anchor="middle"
                    class="value-text"
                    :style="{ fill: item.color }"
                  >
                    {{ item.valueText }}
                  </text>

                  <text :x="item.centerX" :y="strategyMiniChartLabelY" text-anchor="middle" class="axis-strong-text">
                    {{ item.label }}
                  </text>
                  <text :x="item.centerX" :y="strategyMiniChartCountY" text-anchor="middle" class="axis-text">
                    {{ item.countText }}
                  </text>
                </g>
              </template>

              <g v-else>
                <text x="180" y="142" text-anchor="middle" class="empty-text strategy-empty-text">暂无覆盖扩展率数据</text>
                <text x="180" y="168" text-anchor="middle" class="empty-subtext">当前记录中还没有有效公开断言基准对比结果</text>
              </g>
            </svg>
          </div>
        </div>
      </div>

      <div class="card chart-card chart-card--trend">
        <div class="chart-header chart-header-stack">
          <div class="chart-header-main">
            <div class="title-with-tip chart-title-row">
              <div class="chart-title">同一函数生成次数与评估分数趋势</div>
              <el-tooltip placement="top" effect="light">
                <template #content>
                  <div class="tooltip-content">{{ trendChartTip }}</div>
                </template>
                <el-icon class="hint-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </div>

          <div class="chart-toolbar">
            <el-select
              v-model="data.selectedTrendFunction"
              class="chart-selector"
              size="small"
              placeholder="请选择函数"
              :disabled="chartFunctionOptions.length === 0"
            >
              <el-option
                v-for="item in chartFunctionOptions"
                :key="item.value"
                :label="item.optionLabel"
                :value="item.value"
              />
            </el-select>

            <div class="trend-tags">
              <div class="trend-tag">总记录 {{ trendAllRecords.length }}</div>
              <div class="trend-tag">已评估轮次 {{ trendRecords.length }}</div>
              <div class="trend-tag">平均分 {{ trendAverageScoreText }}</div>
            </div>
          </div>
        </div>

        <svg viewBox="0 0 760 360" class="chart-svg">
          <defs>
            <linearGradient id="trendAreaGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" style="stop-color: var(--theme-chart-line); stop-opacity: 0.30" />
              <stop offset="100%" style="stop-color: var(--theme-chart-line); stop-opacity: 0.02" />
            </linearGradient>
          </defs>

          <template v-if="lineDots.length > 0">
            <g v-for="tick in trendScoreTicks" :key="`trend-${tick.value}`">
              <line :x1="chartLeft" :y1="tick.y" :x2="chartRight" :y2="tick.y" class="grid-line" />
              <text :x="chartLeft - 12" :y="tick.y + 4" text-anchor="end" class="axis-text">
                {{ tick.label }}
              </text>
            </g>

            <line :x1="chartLeft" :y1="chartTop" :x2="chartLeft" :y2="chartBottom" class="axis-line" />
            <line :x1="chartLeft" :y1="chartBottom" :x2="chartRight" :y2="chartBottom" class="axis-line" />

            <polygon :points="lineAreaPoints" fill="url(#trendAreaGradient)" />
            <polyline
              :points="linePoints"
              fill="none"
              stroke="var(--theme-chart-line)"
              stroke-width="4"
              stroke-linejoin="round"
              stroke-linecap="round"
            />

            <g v-for="dot in lineDots" :key="dot.runIndex">
              <circle
                :cx="dot.x"
                :cy="dot.y"
                :r="lineDots.length > 20 ? 3.5 : 4.5"
                class="line-dot"
              />
              <text
                v-if="shouldShowLineValue(dot.runIndex)"
                :x="dot.x"
                :y="dot.y - 12"
                text-anchor="middle"
                class="value-text"
              >
                {{ dot.score }}
              </text>
            </g>

            <g v-for="mark in trendAxisMarks" :key="`trend-mark-${mark.runIndex}`">
              <line :x1="mark.x" :y1="chartBottom" :x2="mark.x" :y2="chartBottom + 6" class="axis-line" />
              <text :x="mark.x" y="312" text-anchor="middle" class="axis-text">{{ formatTrendRunLabel(mark.runIndex) }}</text>
            </g>
          </template>

          <g v-else>
            <text x="380" y="170" text-anchor="middle" class="empty-text">暂无可展示的趋势数据</text>
            <text x="380" y="198" text-anchor="middle" class="empty-subtext">
              请切换到已有评估分数的函数，查看该函数多次生成后的分数变化
            </text>
          </g>
        </svg>
      </div>

      <div class="card chart-card chart-card--compare">
        <div class="chart-header chart-header-stack">
          <div class="chart-header-main">
            <div class="title-with-tip chart-title-row">
              <div class="chart-title">同一函数下不同模型分数差异</div>
              <el-tooltip placement="top" effect="light">
                <template #content>
                  <div class="tooltip-content">{{ modelCompareChartTip }}</div>
                </template>
                <el-icon class="hint-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </div>

          <div class="chart-toolbar">
            <el-select
              v-model="data.selectedCompareFunction"
              class="chart-selector"
              size="small"
              placeholder="请选择函数"
              :disabled="chartFunctionOptions.length === 0"
            >
              <el-option
                v-for="item in chartFunctionOptions"
                :key="item.value"
                :label="item.optionLabel"
                :value="item.value"
              />
            </el-select>

            <div class="legend-group">
              <div v-for="item in compareLegendItems" :key="item.label" class="legend-item">
                <span class="legend-dot" :style="{ background: item.color }"></span>
                <span>{{ item.label }}</span>
              </div>
            </div>
          </div>
        </div>

        <svg
          :viewBox="`0 0 ${compareChartWidth} 360`"
          class="chart-svg compare-chart-svg"
          :style="{ minWidth: `${compareChartWidth}px` }"
        >
          <template v-if="hasModelCompareData">
            <g v-for="tick in compareScoreTicks" :key="`compare-${tick.value}`">
              <line :x1="compareChartLeft" :y1="tick.y" :x2="compareChartRight" :y2="tick.y" class="grid-line" />
              <text :x="compareChartLeft - 12" :y="tick.y + 4" text-anchor="end" class="axis-text">
                {{ tick.label }}
              </text>
            </g>

            <line :x1="compareChartLeft" :y1="chartTop" :x2="compareChartLeft" :y2="chartBottom" class="axis-line" />
            <line :x1="compareChartLeft" :y1="chartBottom" :x2="compareChartRight" :y2="chartBottom" class="axis-line" />

            <g v-for="item in modelCompareBars" :key="item.modelName">
              <title>
                {{ `${item.modelName}：系统评估 ${item.systemScoreText}，测试人员 ${item.testerScoreText}` }}
              </title>

              <rect
                v-if="item.systemScore !== null"
                :x="item.systemX"
                :y="getScoreY(item.systemScore, compareScoreAxisMax)"
                :width="item.barWidth"
                :height="chartBottom - getScoreY(item.systemScore, compareScoreAxisMax)"
                rx="12"
                fill="var(--theme-chart-bar)"
                fill-opacity="0.92"
              />
              <rect
                v-if="item.testerScore !== null"
                :x="item.testerX"
                :y="getScoreY(item.testerScore, compareScoreAxisMax)"
                :width="item.barWidth"
                :height="chartBottom - getScoreY(item.testerScore, compareScoreAxisMax)"
                rx="12"
                fill="var(--theme-chart-line)"
                fill-opacity="0.92"
              />

              <text
                v-if="item.systemScore !== null"
                :x="item.systemCenterX"
                :y="getScoreY(item.systemScore, compareScoreAxisMax) - 12"
                text-anchor="middle"
                class="value-text"
              >
                {{ item.systemScore }}
              </text>
              <text
                v-else
                :x="item.systemCenterX"
                :y="chartBottom - 10"
                text-anchor="middle"
                class="axis-text"
              >
                —
              </text>

              <text
                v-if="item.testerScore !== null"
                :x="item.testerCenterX"
                :y="getScoreY(item.testerScore, compareScoreAxisMax) - 12"
                text-anchor="middle"
                class="value-text"
              >
                {{ item.testerScore }}
              </text>
              <text
                v-else
                :x="item.testerCenterX"
                :y="chartBottom - 10"
                text-anchor="middle"
                class="axis-text"
              >
                —
              </text>

              <text :x="item.centerX" y="312" text-anchor="middle" class="axis-strong-text">
                {{ item.shortLabel }}
              </text>
              <text :x="item.centerX" y="334" text-anchor="middle" class="axis-text">
                记录 {{ item.recordCount }}
              </text>
            </g>
          </template>

          <g v-else>
            <text :x="compareChartWidth / 2" y="170" text-anchor="middle" class="empty-text">
              暂无可展示的模型对比数据
            </text>
            <text :x="compareChartWidth / 2" y="198" text-anchor="middle" class="empty-subtext">
              请选择含有系统评分或测试人员评分的函数，查看模型差异
            </text>
          </g>
        </svg>
      </div>

      <div class="card performance-card">
        <div class="chart-header performance-header">
          <div>
            <div class="chart-title">不同模型生成性能对比</div>
          </div>
          <div class="trend-tag">模型 {{ modelPerformanceSummary.length }}</div>
        </div>

        <el-table
          :data="modelPerformanceSummary"
          class="performance-table"
          height="100%"
          empty-text="暂无性能统计数据"
        >
          <el-table-column prop="modelName" label="模型名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="recordCount" label="记录数量" width="100" align="center" />
          <el-table-column label="平均输入Token" width="130" align="center">
            <template #default="{ row }">
              {{ formatAverageText(row.averagePromptTokens) }}
            </template>
          </el-table-column>
          <el-table-column label="平均输出Token" width="130" align="center">
            <template #default="{ row }">
              {{ formatAverageText(row.averageCompletionTokens) }}
            </template>
          </el-table-column>
          <el-table-column label="平均总Token" width="120" align="center">
            <template #default="{ row }">
              {{ formatAverageText(row.averageTotalTokens) }}
            </template>
          </el-table-column>
          <el-table-column label="平均延时" width="120" align="center">
            <template #default="{ row }">
              {{ formatAverageText(row.averageLatencyMs, " ms") }}
            </template>
          </el-table-column>
          <el-table-column label="平均评估分数" width="130" align="center">
            <template #default="{ row }">
              {{ formatScoreText(row.averageEvaluationScore) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from "vue";
import { QuestionFilled } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request.js";

const user = JSON.parse(localStorage.getItem("xm-user") || "{}");
const currentRole = user.loginType === "USER" ? "USER" : user.role;

const chartLeft = 70;
const chartRight = 710;
const chartTop = 36;
const chartBottom = 280;
const defaultChartWidth = 760;
const strategyMiniChartLeft = 48;
const strategyMiniChartRight = 336;
const strategyMiniChartTop = 44;
const strategyMiniChartBottom = 224;
const strategyMiniChartLabelY = 258;
const strategyMiniChartCountY = 280;
const strategyMiniBarWidth = 48;

const data = reactive({
  loading: false,
  records: [],
  selectedTrendFunction: "",
  selectedCompareFunction: ""
});

const pageIntroTip = "基于当前账号可见的全部生成记录，展示策略差异、同一函数的多次生成趋势，以及同一函数在不同模型下的系统评估分数和测试人员分数对比。";
const strategyChartTip = "按策略聚合普通生成与链式生成的平均评估分数，并使用公开断言基准对比中的覆盖扩展率字段计算平均覆盖扩展率。大模型评分为 0 或测试人员评分为 0 的记录不参与平均分统计。";
const trendChartTip = "折线图按所选函数的实际迭代轮次展开。横轴表示该函数第几轮生成，纵轴表示该轮对应的评估分数；同一轮存在多条记录时按该轮平均分展示，未完成评估的记录不会绘制点位。";
const modelCompareChartTip = "图表以所选函数为单位，按模型聚合展示系统评估平均分与测试人员平均分，用于比较不同模型在同一函数上的表现差异。";

const strategyMetricLegend = [
  { label: "评估分数", color: "var(--theme-chart-bar)" },
  { label: "覆盖扩展率", color: "var(--theme-chart-line)" }
];

const compareLegendItems = [
  { label: "系统评估分数", color: "var(--theme-chart-bar)" },
  { label: "测试人员分数", color: "var(--theme-chart-line)" }
];

const normalizeText = (value, fallback = "") => {
  const text = String(value ?? "").trim();
  return text || fallback;
};

const toNumber = (value) => {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = Number(value);
  return Number.isFinite(number) ? number : null;
};

const toTimestamp = (value, fallback = 0) => {
  if (!value) {
    return fallback;
  }

  const normalized = String(value).replace(/-/g, "/");
  const timestamp = new Date(normalized).getTime();
  return Number.isFinite(timestamp) ? timestamp : fallback;
};

const formatScoreText = (value) => {
  return value === null ? "暂无" : `${value} 分`;
};

const formatStrategyScoreText = (value) => {
  return value === null ? "暂无评分" : `${Number(value).toFixed(1)} 分`;
};

const formatCoverageText = (value) => {
  return value === null ? "未统计" : `${Number(value).toFixed(1)}%`;
};

const formatPercentText = (value) => {
  return value === null ? "暂无" : `${Number(value).toFixed(2)}%`;
};

const formatNumberText = (value) => {
  return value === null ? "暂无" : Number(value).toFixed(2);
};

const formatAverageText = (value, suffix = "") => {
  if (value === null) {
    return "未统计";
  }

  const text = Number.isInteger(value) ? String(value) : Number(value).toFixed(1);
  return `${text}${suffix}`;
};

const truncateLabel = (value, maxLength = 8) => {
  const text = normalizeText(value, "未命名");
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
};

const buildScoreTicks = (maxValue, top = chartTop, bottom = chartBottom) => {
  const safeMax = maxValue > 0 ? maxValue : 100;
  const step = safeMax / 5;
  const ticks = [];

  for (let i = 0; i <= 5; i++) {
    const value = Number((i * step).toFixed(1));
    ticks.unshift({
      value,
      label: Number.isInteger(value) ? String(value) : value.toFixed(1),
      y: bottom - (value / safeMax) * (bottom - top)
    });
  }

  return ticks;
};

const getAverageScore = (scores) => {
  if (!scores || scores.length === 0) {
    return null;
  }

  const total = scores.reduce((sum, item) => sum + item, 0);
  return Number((total / scores.length).toFixed(1));
};

const getAverageNumber = (values) => {
  const validValues = (Array.isArray(values) ? values : [])
    .filter(value => value !== null && value !== undefined && Number.isFinite(Number(value)))
    .map(value => Number(value));

  if (validValues.length === 0) {
    return null;
  }

  const total = validValues.reduce((sum, item) => sum + item, 0);
  return Number((total / validValues.length).toFixed(1));
};

const getStrategyKey = (strategy) => {
  return strategy === "cot" ? "cot" : "normal";
};

const normalizeRunIndex = (value) => {
  const number = Number(value);
  return Number.isInteger(number) && number > 0 ? number : null;
};

const compareRecordOrder = (a, b) => {
  const timeDiff = a.createTimestamp - b.createTimestamp;
  if (timeDiff !== 0) {
    return timeDiff;
  }

  return (a.id || 0) - (b.id || 0);
};

const normalizedRecords = computed(() => {
  return (Array.isArray(data.records) ? data.records : []).map(item => ({
    ...item,
    functionName: normalizeText(item.functionName, "未命名函数"),
    modelName: normalizeText(item.modelName, "未命名模型"),
    strategyKey: getStrategyKey(item.strategy),
    sourceType: normalizeText(item.sourceType).toLowerCase(),
    functionId: toNumber(item.functionId),
    runIndex: normalizeRunIndex(item.runIndex),
    functionKey: normalizeText(item.sourceType).toLowerCase() === "function"
      ? (toNumber(item.functionId) !== null
          ? `function-${toNumber(item.functionId)}`
          : `function-name-${normalizeText(item.functionName, "未命名函数")}`)
      : "",
    functionName: normalizeText(item.functionName, "未命名函数"),
    modelName: normalizeText(item.modelName, "未命名模型"),
    score: toNumber(item.evaluationScore),
    testerScore: toNumber(item.testerEvaluationScore),
    promptTokens: toNumber(item.promptTokens),
    completionTokens: toNumber(item.completionTokens),
    totalTokens: toNumber(item.totalTokens),
    latencyMs: toNumber(item.latencyMs),
    lineCoverage: toNumber(item.lineCoverage),
    branchCoverage: toNumber(item.branchCoverage),
    coverageStatus: normalizeText(item.coverageStatus).toUpperCase(),
    publicCompareResult: normalizeText(item.publicCompareResult),
    publicCoveredCount: toNumber(item.publicCoveredCount),
    publicPartialCount: toNumber(item.publicPartialCount),
    publicMissingCount: toNumber(item.publicMissingCount),
    publicExtraCaseCount: toNumber(item.publicExtraCaseCount),
    publicExtraRate: toNumber(item.publicExtraRate),
    publicExpandRate: toNumber(item.publicExpandRate),
    createTimestamp: toTimestamp(item.createTime, 0)
  }));
});

const isValidStrategyScoreRecord = (item) => {
  return item.score !== null && item.score !== 0 && item.testerScore !== 0;
};

const hasPublicBenchmarkCounts = (item) => {
  return item.publicCoveredCount !== null
    || item.publicPartialCount !== null
    || item.publicMissingCount !== null;
};

const hasPublicBenchmarkData = (item) => {
  return !!item.publicCompareResult && hasPublicBenchmarkCounts(item);
};

const hasValidPublicExpandRate = (item) => {
  return hasPublicBenchmarkData(item) && item.publicExpandRate !== null;
};

const strategySummary = computed(() => {
  const summaryMap = {
    normal: {
      key: "normal",
      label: "普通生成",
      recordCount: 0,
      scoredCount: 0,
      totalScore: 0,
      expandRateCount: 0,
      totalExpandRate: 0
    },
    cot: {
      key: "cot",
      label: "链式生成",
      recordCount: 0,
      scoredCount: 0,
      totalScore: 0,
      expandRateCount: 0,
      totalExpandRate: 0
    }
  };

  normalizedRecords.value.forEach(item => {
    const target = summaryMap[item.strategyKey];
    target.recordCount += 1;

    if (isValidStrategyScoreRecord(item)) {
      target.scoredCount += 1;
      target.totalScore += item.score;
    }

    if (hasValidPublicExpandRate(item)) {
      target.expandRateCount += 1;
      target.totalExpandRate += item.publicExpandRate;
    }
  });

  return Object.values(summaryMap).map(item => ({
    ...item,
    avgScore: item.scoredCount > 0 ? Number((item.totalScore / item.scoredCount).toFixed(1)) : null,
    avgExpandRate: item.expandRateCount > 0 ? Number((item.totalExpandRate / item.expandRateCount).toFixed(2)) : null
  }));
});

const scoredRecords = computed(() => {
  return normalizedRecords.value
    .filter(item => item.score !== null)
    .slice()
    .sort(compareRecordOrder);
});

const coverageSuccessRecords = computed(() => {
  return normalizedRecords.value
    .filter(item => item.coverageStatus === "SUCCESS")
    .slice()
    .sort(compareRecordOrder);
});

const modelPerformanceSummary = computed(() => {
  const modelMap = new Map();

  normalizedRecords.value.forEach(item => {
    if (!modelMap.has(item.modelName)) {
      modelMap.set(item.modelName, {
        modelName: item.modelName,
        recordCount: 0,
        promptTokens: [],
        completionTokens: [],
        totalTokens: [],
        latencyMs: [],
        evaluationScores: []
      });
    }

    const target = modelMap.get(item.modelName);
    target.recordCount += 1;
    if (item.promptTokens !== null) {
      target.promptTokens.push(item.promptTokens);
    }
    if (item.completionTokens !== null) {
      target.completionTokens.push(item.completionTokens);
    }
    if (item.totalTokens !== null) {
      target.totalTokens.push(item.totalTokens);
    }
    if (item.latencyMs !== null) {
      target.latencyMs.push(item.latencyMs);
    }
    if (item.score !== null) {
      target.evaluationScores.push(item.score);
    }
  });

  return Array.from(modelMap.values())
    .map(item => ({
      modelName: item.modelName,
      recordCount: item.recordCount,
      averagePromptTokens: getAverageNumber(item.promptTokens),
      averageCompletionTokens: getAverageNumber(item.completionTokens),
      averageTotalTokens: getAverageNumber(item.totalTokens),
      averageLatencyMs: getAverageNumber(item.latencyMs),
      averageEvaluationScore: getAverageNumber(item.evaluationScores)
    }))
    .sort((a, b) => {
      if (b.recordCount !== a.recordCount) {
        return b.recordCount - a.recordCount;
      }
      return a.modelName.localeCompare(b.modelName, "zh-Hans-CN");
    });
});

const functionOptions = computed(() => {
  const functionMap = new Map();

  normalizedRecords.value.forEach(item => {
    if (!functionMap.has(item.functionName)) {
      functionMap.set(item.functionName, {
        value: item.functionName,
        label: item.functionName,
        totalCount: 0,
        scoredCount: 0,
        modelSet: new Set()
      });
    }

    const target = functionMap.get(item.functionName);
    target.totalCount += 1;
    if (item.score !== null) {
      target.scoredCount += 1;
    }
    target.modelSet.add(item.modelName);
  });

  return Array.from(functionMap.values())
    .sort((a, b) => {
      if (b.scoredCount !== a.scoredCount) {
        return b.scoredCount - a.scoredCount;
      }
      if (b.totalCount !== a.totalCount) {
        return b.totalCount - a.totalCount;
      }
      return a.label.localeCompare(b.label, "zh-Hans-CN");
    })
    .map(item => ({
      ...item,
      modelCount: item.modelSet.size,
      optionLabel: `${item.label}（${item.totalCount} 次生成）`
    }));
});

const iterativeFunctionRecords = computed(() => {
  return normalizedRecords.value
    .filter(item => item.sourceType === "function" && item.functionKey && item.runIndex !== null)
    .slice()
    .sort((a, b) => {
      if ((a.runIndex || 0) !== (b.runIndex || 0)) {
        return (a.runIndex || 0) - (b.runIndex || 0);
      }
      return compareRecordOrder(a, b);
    });
});

const chartFunctionOptions = computed(() => {
  const functionMap = new Map();

  iterativeFunctionRecords.value.forEach(item => {
    if (!functionMap.has(item.functionKey)) {
      functionMap.set(item.functionKey, {
        value: item.functionKey,
        label: item.functionName,
        maxRunIndex: 0,
        scoredCount: 0,
        recordCount: 0,
        modelSet: new Set()
      });
    }

    const target = functionMap.get(item.functionKey);
    target.recordCount += 1;
    target.maxRunIndex = Math.max(target.maxRunIndex, item.runIndex || 0);
    if (item.score !== null) {
      target.scoredCount += 1;
    }
    target.modelSet.add(item.modelName);
  });

  return Array.from(functionMap.values())
    .sort((a, b) => {
      if (b.maxRunIndex !== a.maxRunIndex) {
        return b.maxRunIndex - a.maxRunIndex;
      }
      if (b.scoredCount !== a.scoredCount) {
        return b.scoredCount - a.scoredCount;
      }
      return a.label.localeCompare(b.label, "zh-Hans-CN");
    })
    .map(item => ({
      ...item,
      optionLabel: `${item.label}（迭代至第${item.maxRunIndex}轮）`
    }));
});

const findChartFunctionOption = (value) => {
  return chartFunctionOptions.value.find(item => item.value === value) || null;
};

const summary = computed(() => {
  const normalItem = strategySummary.value.find(item => item.key === "normal");
  const cotItem = strategySummary.value.find(item => item.key === "cot");
  const averageScore = getAverageScore(scoredRecords.value.map(item => item.score));
  const averageLineCoverage = getAverageScore(coverageSuccessRecords.value
    .map(item => item.lineCoverage)
    .filter(value => value !== null));
  const averageBranchCoverage = getAverageScore(coverageSuccessRecords.value
    .map(item => item.branchCoverage)
    .filter(value => value !== null));

  return {
    totalRecords: normalizedRecords.value.length,
    scoredRecords: scoredRecords.value.length,
    normalRecords: normalItem?.recordCount || 0,
    cotRecords: cotItem?.recordCount || 0,
    averageScore,
    averageScoreText: formatScoreText(averageScore),
    coverageSuccessCount: coverageSuccessRecords.value.length,
    averageLineCoverageText: formatCoverageText(averageLineCoverage),
    averageBranchCoverageText: formatCoverageText(averageBranchCoverage)
  };
});

const getAverageByScale = (values) => {
  const validValues = (Array.isArray(values) ? values : [])
    .filter(value => value !== null && value !== undefined && Number.isFinite(Number(value)))
    .map(value => Number(value));

  if (validValues.length === 0) {
    return null;
  }

  const total = validValues.reduce((sum, value) => sum + value, 0);
  return Number((total / validValues.length).toFixed(2));
};

const publicBenchmarkStats = computed(() => {
  const compareRecords = normalizedRecords.value.filter(hasPublicBenchmarkData);
  const validExpandRateRecords = compareRecords.filter(hasValidPublicExpandRate);
  const benchmarkCoverageRates = compareRecords
    .map(item => {
      const total = Number(item.publicCoveredCount || 0)
        + Number(item.publicPartialCount || 0)
        + Number(item.publicMissingCount || 0);
      return total > 0 ? (Number(item.publicCoveredCount || 0) * 100) / total : null;
    })
    .filter(value => value !== null);

  const averageBenchmarkCoverageRate = getAverageByScale(benchmarkCoverageRates);
  const averageExtraCaseCount = getAverageByScale(compareRecords.map(item => item.publicExtraCaseCount ?? 0));
  const averageExtraRate = getAverageByScale(compareRecords.map(item => item.publicExtraRate ?? 0));
  const averageExpandRate = getAverageByScale(validExpandRateRecords.map(item => item.publicExpandRate));

  return {
    compareRecordCount: compareRecords.length,
    averageBenchmarkCoverageRateText: formatPercentText(averageBenchmarkCoverageRate),
    averageExtraCaseCountText: formatNumberText(averageExtraCaseCount),
    averageExtraRateText: formatPercentText(averageExtraRate),
    averageExpandRateText: formatPercentText(averageExpandRate)
  };
});

const strategyMiniGroups = [
  { key: "normal", centerX: 132 },
  { key: "cot", centerX: 256 }
];

const getStrategySummaryItem = (key) => {
  return strategySummary.value.find(item => item.key === key) || null;
};

const getStrategyScoreAxisMax = (values) => {
  const validValues = values.filter(value => value !== null);
  if (validValues.length === 0) {
    return 100;
  }

  const maxValue = Math.max(...validValues);
  if (maxValue <= 100) {
    return 100;
  }

  return Math.ceil((maxValue * 1.15) / 20) * 20;
};

const getStrategyPercentAxisMax = (values) => {
  const validValues = values.filter(value => value !== null);
  if (validValues.length === 0) {
    return 100;
  }

  const maxValue = Math.max(...validValues);
  const step = maxValue > 100 ? 20 : 10;
  return Math.max(step, Math.ceil((maxValue * 1.2) / step) * step);
};

const strategyScoreAxisMax = computed(() => {
  return getStrategyScoreAxisMax(strategySummary.value.map(item => item.avgScore));
});

const strategyExpandAxisMax = computed(() => {
  return getStrategyPercentAxisMax(strategySummary.value.map(item => item.avgExpandRate));
});

const strategyScoreTicks = computed(() => {
  return buildScoreTicks(strategyScoreAxisMax.value, strategyMiniChartTop, strategyMiniChartBottom);
});

const strategyExpandTicks = computed(() => {
  return buildScoreTicks(strategyExpandAxisMax.value, strategyMiniChartTop, strategyMiniChartBottom)
    .map(item => ({
      ...item,
      label: `${item.label}%`
    }));
});

const hasStrategyScoreChartData = computed(() => {
  return strategySummary.value.some(item => item.avgScore !== null);
});

const hasStrategyExpandChartData = computed(() => {
  return strategySummary.value.some(item => item.avgExpandRate !== null);
});

const getStrategyMiniBarY = (value, axisMax) => {
  const safeValue = Math.max(Number(value) || 0, 0);
  const safeAxisMax = axisMax > 0 ? axisMax : 100;
  return strategyMiniChartBottom - (safeValue / safeAxisMax) * (strategyMiniChartBottom - strategyMiniChartTop);
};

const buildStrategyMiniBars = ({
  valueKey,
  countKey,
  axisMax,
  valueFormatter,
  emptyText,
  countLabel,
  fill,
  color
}) => {
  return strategyMiniGroups.map(item => {
    const source = getStrategySummaryItem(item.key);
    const value = source?.[valueKey] ?? null;
    const y = value === null ? strategyMiniChartBottom : getStrategyMiniBarY(value, axisMax);

    return {
      ...item,
      label: source?.label || "",
      value,
      valueText: value === null ? emptyText : valueFormatter(value),
      countText: `${countLabel} ${source?.[countKey] || 0} 条`,
      x: item.centerX - strategyMiniBarWidth / 2,
      y,
      valueY: value === null ? strategyMiniChartBottom - 8 : Math.max(y - 14, 18),
      height: value === null ? 0 : strategyMiniChartBottom - y,
      barWidth: strategyMiniBarWidth,
      fill,
      color
    };
  });
};

const strategyScoreBars = computed(() => {
  return buildStrategyMiniBars({
    valueKey: "avgScore",
    countKey: "scoredCount",
    axisMax: strategyScoreAxisMax.value,
    valueFormatter: formatStrategyScoreText,
    emptyText: "暂无评分",
    countLabel: "评分",
    fill: "url(#strategyScoreMiniGradient)",
    color: "var(--theme-chart-bar)"
  });
});

const strategyExpandBars = computed(() => {
  return buildStrategyMiniBars({
    valueKey: "avgExpandRate",
    countKey: "expandRateCount",
    axisMax: strategyExpandAxisMax.value,
    valueFormatter: formatPercentText,
    emptyText: "暂无数据",
    countLabel: "有效",
    fill: "url(#strategyExpandMiniGradient)",
    color: "var(--theme-chart-line)"
  });
});

const trendAllRecords = computed(() => {
  return iterativeFunctionRecords.value
    .filter(item => item.functionKey === data.selectedTrendFunction)
    .slice()
    .sort((a, b) => {
      if ((a.runIndex || 0) !== (b.runIndex || 0)) {
        return (a.runIndex || 0) - (b.runIndex || 0);
      }
      return compareRecordOrder(a, b);
    });
});

const trendRecords = computed(() => {
  const roundMap = new Map();

  trendAllRecords.value.forEach(item => {
    if (item.runIndex === null) {
      return;
    }

    if (!roundMap.has(item.runIndex)) {
      roundMap.set(item.runIndex, {
        runIndex: item.runIndex,
        recordCount: 0,
        scoreList: []
      });
    }

    const target = roundMap.get(item.runIndex);
    target.recordCount += 1;
    if (item.score !== null) {
      target.scoreList.push(item.score);
    }
  });

  return Array.from(roundMap.values())
    .sort((a, b) => a.runIndex - b.runIndex)
    .map(item => ({
      runIndex: item.runIndex,
      recordCount: item.recordCount,
      score: getAverageScore(item.scoreList)
    }))
    .filter(item => item.score !== null);
});

const trendAverageScore = computed(() => {
  return getAverageScore(trendRecords.value.map(item => item.score));
});

const trendAverageScoreText = computed(() => {
  return formatScoreText(trendAverageScore.value);
});

const trendMaxRunIndex = computed(() => {
  return trendAllRecords.value.reduce((maxValue, item) => {
    return Math.max(maxValue, item.runIndex || 0);
  }, 0);
});

const trendScoreAxisMax = computed(() => {
  const scores = trendRecords.value.map(item => item.score);
  if (scores.length === 0) {
    return 100;
  }

  const maxValue = Math.max(...scores);
  return maxValue <= 100 ? 100 : Math.ceil(maxValue / 20) * 20;
});

const trendScoreTicks = computed(() => buildScoreTicks(trendScoreAxisMax.value));

const formatTrendRunLabel = (runIndex) => {
  return `第${runIndex}次`;
};

const lineDots = computed(() => {
  const totalRuns = trendMaxRunIndex.value;
  if (totalRuns === 0 || trendRecords.value.length === 0) {
    return [];
  }

  const width = chartRight - chartLeft;
  const centerX = chartLeft + width / 2;

  return trendRecords.value.map(item => {
    const x = totalRuns === 1
      ? centerX
      : chartLeft + (width / (totalRuns - 1)) * (item.runIndex - 1);

    return {
      ...item,
      x,
      y: getScoreY(item.score, trendScoreAxisMax.value)
    };
  });
});

const linePoints = computed(() => {
  return lineDots.value.map(item => `${item.x},${item.y}`).join(" ");
});

const lineAreaPoints = computed(() => {
  if (lineDots.value.length === 0) {
    return "";
  }

  const first = lineDots.value[0];
  const last = lineDots.value[lineDots.value.length - 1];

  return [
    `${first.x},${chartBottom}`,
    ...lineDots.value.map(item => `${item.x},${item.y}`),
    `${last.x},${chartBottom}`
  ].join(" ");
});

const trendAxisMarks = computed(() => {
  const totalRuns = trendMaxRunIndex.value;
  if (totalRuns === 0) {
    return [];
  }

  const width = chartRight - chartLeft;
  const centerX = chartLeft + width / 2;
  const buildMark = (runIndex) => ({
    runIndex,
    x: totalRuns === 1
      ? centerX
      : chartLeft + (width / (totalRuns - 1)) * (runIndex - 1)
  });

  if (totalRuns <= 8) {
    return Array.from({ length: totalRuns }, (_, index) => buildMark(index + 1));
  }

  const runIndexSet = new Set([1, totalRuns]);
  const step = Math.ceil((totalRuns - 1) / 6);

  for (let i = 1 + step; i < totalRuns; i += step) {
    runIndexSet.add(i);
  }

  return Array.from(runIndexSet)
    .sort((a, b) => a - b)
    .map(buildMark);
});

const lineValueRunSet = computed(() => {
  const set = new Set();
  if (lineDots.value.length === 0) {
    return set;
  }

  if (lineDots.value.length <= 8) {
    lineDots.value.forEach(item => set.add(item.runIndex));
    return set;
  }

  let maxDot = lineDots.value[0];
  let minDot = lineDots.value[0];

  lineDots.value.forEach(item => {
    if (item.score > maxDot.score) {
      maxDot = item;
    }
    if (item.score < minDot.score) {
      minDot = item;
    }
  });

  set.add(lineDots.value[0].runIndex);
  set.add(lineDots.value[lineDots.value.length - 1].runIndex);
  set.add(lineDots.value[Math.floor((lineDots.value.length - 1) / 2)].runIndex);
  set.add(maxDot.runIndex);
  set.add(minDot.runIndex);
  return set;
});

const shouldShowLineValue = (runIndex) => {
  return lineValueRunSet.value.has(runIndex);
};

const compareFunctionRecords = computed(() => {
  return iterativeFunctionRecords.value
    .filter(item => item.functionKey === data.selectedCompareFunction)
    .slice()
    .sort((a, b) => {
      if ((a.runIndex || 0) !== (b.runIndex || 0)) {
        return (a.runIndex || 0) - (b.runIndex || 0);
      }
      return compareRecordOrder(a, b);
    });
});

const modelCompareSummary = computed(() => {
  const modelMap = new Map();

  compareFunctionRecords.value.forEach(item => {
    if (!modelMap.has(item.modelName)) {
      modelMap.set(item.modelName, {
        modelName: item.modelName,
        recordCount: 0,
        systemScores: [],
        testerScores: []
      });
    }

    const target = modelMap.get(item.modelName);
    target.recordCount += 1;
    if (item.score !== null) {
      target.systemScores.push(item.score);
    }
    if (item.testerScore !== null) {
      target.testerScores.push(item.testerScore);
    }
  });

  return Array.from(modelMap.values())
    .map(item => {
      const systemScore = getAverageScore(item.systemScores);
      const testerScore = getAverageScore(item.testerScores);
      return {
        modelName: item.modelName,
        recordCount: item.recordCount,
        systemScore,
        testerScore,
        systemScoreText: formatScoreText(systemScore),
        testerScoreText: formatScoreText(testerScore)
      };
    })
    .sort((a, b) => {
      const aBase = a.systemScore ?? a.testerScore ?? -1;
      const bBase = b.systemScore ?? b.testerScore ?? -1;
      if (bBase !== aBase) {
        return bBase - aBase;
      }
      return a.modelName.localeCompare(b.modelName, "zh-Hans-CN");
    });
});

const hasModelCompareData = computed(() => {
  return modelCompareSummary.value.some(item => item.systemScore !== null || item.testerScore !== null);
});

const compareScoreAxisMax = computed(() => {
  const scoreList = [];

  modelCompareSummary.value.forEach(item => {
    if (item.systemScore !== null) {
      scoreList.push(item.systemScore);
    }
    if (item.testerScore !== null) {
      scoreList.push(item.testerScore);
    }
  });

  if (scoreList.length === 0) {
    return 100;
  }

  const maxValue = Math.max(...scoreList);
  return maxValue <= 100 ? 100 : Math.ceil(maxValue / 20) * 20;
});

const compareScoreTicks = computed(() => buildScoreTicks(compareScoreAxisMax.value));

const compareLayout = computed(() => {
  const groupCount = Math.max(modelCompareSummary.value.length, 1);
  const barWidth = 30;
  const barGap = 12;
  const groupGap = 44;
  const groupWidth = barWidth * 2 + barGap;
  const contentWidth = groupCount * groupWidth + Math.max(groupCount - 1, 0) * groupGap;
  const viewWidth = Math.max(defaultChartWidth, chartLeft + contentWidth + 60);
  const availableWidth = viewWidth - chartLeft - 60;
  const startX = chartLeft + Math.max((availableWidth - contentWidth) / 2, 0);

  return {
    barWidth,
    groupGap,
    groupWidth,
    startX,
    viewWidth,
    chartRight: viewWidth - 60
  };
});

const compareChartWidth = computed(() => compareLayout.value.viewWidth);
const compareChartLeft = computed(() => chartLeft);
const compareChartRight = computed(() => compareLayout.value.chartRight);

const modelCompareBars = computed(() => {
  const layout = compareLayout.value;

  return modelCompareSummary.value.map((item, index) => {
    const groupX = layout.startX + index * (layout.groupWidth + layout.groupGap);
    const systemX = groupX;
    const testerX = groupX + layout.barWidth + 12;
    const centerX = groupX + layout.groupWidth / 2;

    return {
      ...item,
      barWidth: layout.barWidth,
      groupX,
      systemX,
      testerX,
      systemCenterX: systemX + layout.barWidth / 2,
      testerCenterX: testerX + layout.barWidth / 2,
      centerX,
      shortLabel: truncateLabel(item.modelName, 10)
    };
  });
});

const getScoreY = (score, axisMax, top = chartTop) => {
  const safeScore = Math.max(Number(score) || 0, 0);
  const safeAxisMax = axisMax > 0 ? axisMax : 100;
  return chartBottom - (safeScore / safeAxisMax) * (chartBottom - top);
};

const syncSelectedFunctions = () => {
  const options = chartFunctionOptions.value;
  const valueSet = new Set(options.map(item => item.value));

  if (options.length === 0) {
    data.selectedTrendFunction = "";
    data.selectedCompareFunction = "";
    return;
  }

  if (!valueSet.has(data.selectedTrendFunction)) {
    data.selectedTrendFunction = options[0].value;
  }

  if (!valueSet.has(data.selectedCompareFunction)) {
    data.selectedCompareFunction = data.selectedTrendFunction;
  }
};

const loadData = async () => {
  data.loading = true;
  try {
    const res = await request.get("/generateRecord/selectAll", {
      params: {
        currentUserId: user.id,
        currentUserRole: currentRole
      }
    });

    if (res.code === "200" || res.code === 200) {
      data.records = Array.isArray(res.data) ? res.data : [];
      syncSelectedFunctions();
    } else {
      data.records = [];
      syncSelectedFunctions();
      ElMessage.error(res.msg || "加载用例分析数据失败");
    }
  } catch (e) {
    data.records = [];
    syncSelectedFunctions();
    ElMessage.error("请求用例分析数据失败");
  } finally {
    data.loading = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.analysis-page {
  min-height: calc(100vh - 84px);
  padding: 0;
  box-sizing: border-box;
  overflow: auto;
}

.analysis-layout {
  display: grid;
  grid-template-columns: minmax(340px, 0.92fr) minmax(0, 1.08fr);
  grid-template-rows: 368px 420px 300px;
  grid-template-areas:
    "overview strategy"
    "trend compare"
    "performance performance";
  gap: 12px;
  align-items: stretch;
  min-height: calc(100vh - 84px);
}

.overview-card {
  grid-area: overview;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 12px 14px 10px;
  box-sizing: border-box;
  background:
    linear-gradient(
      135deg,
      var(--theme-hero-start) 0%,
      var(--theme-hero-middle) 52%,
      var(--theme-hero-end) 100%
    ),
    var(--theme-card-bg);
  border: 1px solid var(--theme-accent-border);
}

.chart-card--strategy {
  grid-area: strategy;
  overflow-x: hidden;
}

.strategy-chart-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-items: stretch;
}

.strategy-mini-chart {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.strategy-mini-title {
  margin-bottom: 6px;
  min-height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 13px;
  line-height: 1.3;
  font-weight: 700;
  color: var(--theme-title-color);
}

.strategy-mini-svg {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
}

.strategy-mini-chart .strategy-mini-svg {
  min-width: 0;
  height: 100%;
}

.chart-card--trend {
  grid-area: trend;
}

.chart-card--compare {
  grid-area: compare;
}

.performance-card {
  grid-area: performance;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 14px 16px;
}

.performance-header {
  align-items: center;
}

.performance-table {
  flex: 1;
  min-height: 0;
}

.overview-badge {
  display: inline-flex;
  align-items: center;
  margin-bottom: 6px;
  padding: 5px 11px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: var(--theme-accent);
  background: var(--theme-accent-soft);
  border: 1px solid var(--theme-accent-border);
  letter-spacing: 0.5px;
  transition: background-color 0.25s ease, border-color 0.25s ease, color 0.25s ease;
}

.page-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--theme-title-color);
}

.title-with-tip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.page-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.overview-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 6px 8px;
  flex: 1;
  min-height: 0;
  padding-bottom: 2px;
  box-sizing: border-box;
}

.overview-stat-card {
  min-height: 0;
  height: 100%;
  padding: 7px 9px 6px;
  box-sizing: border-box;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.2));
  border: 1px solid rgba(255, 255, 255, 0.42);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 2px;
}

.overview-stat-label {
  font-size: 10px;
  line-height: 1.2;
  color: var(--theme-text-secondary);
}

.overview-stat-value {
  min-width: 0;
  font-size: 21px;
  font-weight: 800;
  line-height: 1.08;
  color: var(--theme-title-color);
}

.overview-stat-value--inline {
  max-width: 100%;
  font-size: 15px;
  line-height: 1.18;
  letter-spacing: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.overview-stat-extra {
  font-size: 10px;
  line-height: 1.2;
  color: var(--theme-text-secondary);
}

.public-benchmark-summary {
  margin-top: 8px;
  padding: 9px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.32);
  border: 1px solid rgba(255, 255, 255, 0.42);
  flex-shrink: 0;
}

.public-benchmark-title {
  font-size: 12px;
  font-weight: 800;
  color: var(--theme-title-color);
  margin-bottom: 7px;
}

.public-benchmark-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.public-benchmark-grid > div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 6px 7px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.28);
  color: var(--theme-text-secondary);
}

.public-benchmark-grid span {
  font-size: 10px;
  line-height: 1.2;
}

.public-benchmark-grid b {
  font-size: 15px;
  line-height: 1.15;
  color: var(--theme-title-color);
}

.chart-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 14px 16px 12px;
  background: linear-gradient(180deg, rgba(255, 252, 245, 0.92), rgba(255, 255, 255, 0.58));
  overflow-x: auto;
  overflow-y: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.chart-header-stack {
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
}

.chart-header-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.chart-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.chart-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--theme-title-color);
}

.chart-title-row {
  margin-bottom: 0;
}

.chart-selector {
  width: min(300px, 100%);
}

.legend-group,
.trend-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.legend-item,
.trend-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--theme-accent-soft);
  border: 1px solid var(--theme-accent-border);
  color: var(--theme-text-primary);
  white-space: nowrap;
  transition: background-color 0.25s ease, border-color 0.25s ease, color 0.25s ease;
  font-size: 12px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  transition: background-color 0.25s ease;
}

.hint-icon {
  padding: 2px;
  border-radius: 50%;
  font-size: 15px;
  color: var(--theme-text-secondary);
  cursor: pointer;
  transition: color 0.25s ease, background-color 0.25s ease;
}

.hint-icon:hover {
  color: var(--theme-accent);
  background: var(--theme-accent-soft);
}

.tooltip-content {
  max-width: 340px;
  line-height: 1.6;
  white-space: pre-line;
}

.chart-svg {
  width: 100%;
  display: block;
  flex: 1;
  min-height: 0;
  height: 100%;
}

.compare-chart-svg {
  display: block;
}

.axis-line {
  stroke: var(--theme-chart-axis);
  stroke-opacity: 0.65;
  stroke-width: 1.2;
}

.grid-line {
  stroke: var(--theme-chart-line);
  stroke-opacity: 0.14;
  stroke-width: 1;
  stroke-dasharray: 5 5;
}

.axis-text {
  fill: var(--theme-chart-axis);
  font-size: 12px;
}

.axis-strong-text {
  fill: var(--theme-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.value-text {
  fill: var(--theme-chart-line);
  font-size: 13px;
  font-weight: 700;
}

.line-dot {
  fill: var(--theme-hero-start);
  stroke: var(--theme-chart-line);
  stroke-width: 2;
}

.empty-text {
  fill: var(--theme-title-color);
  font-size: 20px;
  font-weight: 700;
}

.empty-subtext {
  fill: var(--theme-text-secondary);
  font-size: 13px;
}

.strategy-empty-text {
  font-size: 16px;
}

@media (max-width: 1280px) {
  .analysis-layout {
    grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
    grid-template-rows: 356px 400px 300px;
  }
}

@media (max-width: 1100px) {
  .analysis-page {
    min-height: calc(100vh - 84px);
  }

  .analysis-layout {
    grid-template-columns: 1fr;
    grid-template-rows: auto;
    grid-template-areas:
      "overview"
      "strategy"
      "trend"
      "compare"
      "performance";
    height: auto;
  }

  .performance-card {
    min-height: 300px;
  }
}

@media (max-width: 900px) {
  .overview-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .public-benchmark-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .strategy-chart-grid {
    grid-template-columns: 1fr;
    min-height: 560px;
  }
}

@media (max-width: 768px) {
  .analysis-page {
    min-height: calc(100vh - 84px);
  }

  .overview-card {
    padding: 12px 14px 10px;
  }

  .page-title {
    font-size: 24px;
  }

  .chart-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .chart-selector {
    width: 100%;
  }

  .chart-title-row {
    align-items: flex-start;
  }

  .chart-card {
    padding-right: 14px;
  }

  .chart-svg {
    height: auto;
    min-width: 680px;
  }
}
</style>
