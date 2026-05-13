<template>
  <div class="home-page">
    <div class="hero card">
      <div class="hero-left">
        <div class="hero-badge">智能测试 / LLM 推理 / 自动生成</div>
        <div class="hero-title">基于大语言模型的函数级代码测试用例生成系统</div>
        <div class="hero-desc">
          面向函数级代码测试场景，支持函数管理、模型配置、测试用例生成、生成记录管理与角色分级控制。
        </div>

        <div class="hero-tags">
          <el-tag effect="plain" class="hero-tag">当前身份：{{ roleText }}</el-tag>
          <el-tag effect="plain" class="hero-tag">当前用户：{{ user.name || user.username }}</el-tag>
        </div>
      </div>

      <div class="hero-right">
        <div class="pulse-wrap">
          <div class="pulse-circle pulse1"></div>
          <div class="pulse-circle pulse2"></div>
          <div class="core-circle">LLM</div>
        </div>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card card">
        <div class="stat-label">函数总数</div>
        <div class="stat-value">{{ data.stats.functionCount }}</div>
      </div>

      <div class="stat-card card">
        <div class="stat-label">生成记录数</div>
        <div class="stat-value">{{ data.stats.recordCount }}</div>
      </div>

      <div class="stat-card card">
        <div class="stat-label">启用模型数</div>
        <div class="stat-value">{{ data.stats.modelCount }}</div>
      </div>

      <div class="stat-card card" v-if="user.loginType === 'ADMIN'">
        <div class="stat-label">管理员总数</div>
        <div class="stat-value">{{ data.stats.adminCount }}</div>
      </div>

      <div class="stat-card card">
        <div class="stat-label">{{ user.role === 'ADMIN' ? '所属用户数' : '用户总数' }}</div>
        <div class="stat-value">{{ data.stats.userCount }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="card chart-card">
        <div class="chart-title">近7天生成记录趋势</div>
        <svg viewBox="0 0 640 260" class="chart-svg">
          <line x1="50" y1="220" x2="610" y2="220" stroke="#ddd" />
          <line x1="50" y1="20" x2="50" y2="220" stroke="#ddd" />

          <polyline
              :points="linePoints"
              fill="none"
              stroke="var(--theme-chart-line)"
              stroke-width="3"
              stroke-linejoin="round"
              stroke-linecap="round"
          />

          <g v-for="(item, index) in data.lineChart" :key="index">
            <circle
                :cx="50 + index * 93"
                :cy="getLineY(item.count)"
                r="4"
                fill="var(--theme-chart-line)"
            />
            <text :x="50 + index * 93" y="240" text-anchor="middle" class="axis-text">
              {{ item.date }}
            </text>
            <text :x="50 + index * 93" :y="getLineY(item.count) - 10" text-anchor="middle" class="value-text">
              {{ item.count }}
            </text>
          </g>
        </svg>
      </div>

      <div class="card chart-card">
        <div class="chart-title">各模型使用次数统计</div>
        <svg viewBox="0 0 760 320" class="chart-svg">
          <line x1="60" y1="230" x2="720" y2="230" stroke="#ddd" />
          <line x1="60" y1="20" x2="60" y2="230" stroke="#ddd" />

          <g v-for="(item, index) in data.barChart" :key="index">
            <rect
                :x="getBarX(index)"
                :y="getBarY(item.count)"
                width="60"
                :height="230 - getBarY(item.count)"
                rx="8"
                fill="var(--theme-chart-bar)"
            />
            <text
                :x="getBarCenterX(index)"
                :y="getBarY(item.count) - 10"
                text-anchor="middle"
                class="value-text"
            >
              {{ item.count }}
            </text>
            <text
                :x="getBarCenterX(index)"
                y="248"
                text-anchor="middle"
                class="axis-text model-name-text"
            >
              <tspan
                  v-for="(line, lineIndex) in getModelNameLines(item.name)"
                  :key="`${item.name}-${lineIndex}`"
                  :x="getBarCenterX(index)"
                  :dy="lineIndex === 0 ? 0 : 14"
              >
                {{ line }}
              </tspan>
            </text>
          </g>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, onMounted } from "vue";
import request from "@/utils/request.js";

const user = JSON.parse(localStorage.getItem("xm-user") || "{}");
const currentRole = computed(() => user.loginType === "USER" ? "USER" : user.role);

const data = reactive({
  stats: {
    functionCount: 0,
    recordCount: 0,
    modelCount: 0,
    adminCount: 0,
    userCount: 0
  },
  lineChart: [],
  barChart: []
});

const roleText = computed(() => {
  if (user.loginType === "USER") return "普通用户";
  if (user.role === "SUPER_ADMIN") return "超级管理员";
  return "普通管理员";
});

const barGap = computed(() => {
  const len = data.barChart.length || 1;
  return Math.max(120, Math.floor(520 / len));
});

const loadStats = async () => {
  try {
    const functionReq = request.get("/functionInfo/selectAll", {
      params: {
        currentUserId: user.id,
        currentUserRole: currentRole.value
      }
    });

    const modelReq = request.get("/modelConfig/selectEnabledList", {
      params: {
        currentUserId: user.id,
        currentUserRole: currentRole.value
      }
    });

    const userReq = request.get("/user/selectAll", {
      params: {
        currentUserId: user.id,
        currentUserRole: currentRole.value
      }
    });

    const historyReq = request.get("/generateRecord/selectAll", {
      params: {
        currentUserId: user.id,
        currentUserRole: currentRole.value
      }
    });

    const reqList = [functionReq, modelReq, userReq, historyReq];

    if (user.loginType === "ADMIN") {
      reqList.push(
          request.get("/admin/selectAll", {
            params: {
              currentUserId: user.id,
              currentUserRole: user.role
            }
          })
      );
    }

    const resList = await Promise.all(reqList);

    const functionList = resList[0]?.data || [];
    const modelList = resList[1]?.data || [];
    const userList = resList[2]?.data || [];
    const historyList = resList[3]?.data || [];
    const adminList = user.loginType === "ADMIN" ? (resList[4]?.data || []) : [];

    data.stats.functionCount = functionList.length;
    data.stats.modelCount = modelList.length;
    data.stats.userCount = userList.length;
    data.stats.recordCount = historyList.length;
    data.stats.adminCount = adminList.length;

    buildLineChart(historyList);
    buildBarChart(historyList);
  } catch (e) {
    data.stats.functionCount = 0;
    data.stats.recordCount = 0;
    data.stats.modelCount = 0;
    data.stats.userCount = 0;
    data.stats.adminCount = 0;
    data.lineChart = [];
    data.barChart = [{ name: "暂无数据", count: 0 }];
  }
};

const buildLineChart = (historyList) => {
  const last7Days = [];
  const today = new Date();

  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(today.getDate() - i);
    const dateStr = `${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    last7Days.push({ date: dateStr, count: 0 });
  }

  historyList.forEach(item => {
    const time = item.createTime || "";
    const day = time.length >= 10 ? time.substring(5, 10) : "";
    const target = last7Days.find(v => v.date === day);
    if (target) target.count++;
  });

  data.lineChart = last7Days;
};

const buildBarChart = (historyList) => {
  const map = {};
  historyList.forEach(item => {
    const name = item.modelName || "未知模型";
    map[name] = (map[name] || 0) + 1;
  });

  const list = Object.keys(map).map(key => ({
    name: key,
    count: map[key]
  }));

  data.barChart = list.length ? list.slice(0, 5) : [{ name: "暂无数据", count: 0 }];
};

const lineMax = computed(() => Math.max(...data.lineChart.map(v => v.count), 1));
const barMax = computed(() => Math.max(...data.barChart.map(v => v.count), 1));

const getLineY = (count) => 220 - (count / lineMax.value) * 170;
const getBarY = (count) => 230 - (count / barMax.value) * 170;
const getBarX = (index) => 100 + index * barGap.value;
const getBarCenterX = (index) => getBarX(index) + 30;

const getCharWeight = (char) => /[\u4e00-\u9fa5]/.test(char) ? 2 : 1;

const splitTextByUnits = (text, maxUnitsPerLine) => {
  const lines = [];
  let currentLine = "";
  let currentUnits = 0;

  for (const char of text) {
    const weight = getCharWeight(char);

    if (currentLine && currentUnits + weight > maxUnitsPerLine) {
      lines.push(currentLine);
      currentLine = char;
      currentUnits = weight;
      continue;
    }

    currentLine += char;
    currentUnits += weight;
  }

  if (currentLine) {
    lines.push(currentLine);
  }

  return lines;
};

const truncateTextByUnits = (text, maxUnits) => {
  let result = "";
  let currentUnits = 0;

  for (const char of text) {
    const weight = getCharWeight(char);
    if (currentUnits + weight > maxUnits - 1) {
      return `${result}…`;
    }
    result += char;
    currentUnits += weight;
  }

  return result;
};

const getModelNameLines = (name) => {
  const rawLines = splitTextByUnits(name || "", 12);

  if (rawLines.length <= 3) {
    return rawLines;
  }

  return [
    rawLines[0],
    rawLines[1],
    truncateTextByUnits(rawLines.slice(2).join(""), 12)
  ];
};

const linePoints = computed(() => {
  return data.lineChart
      .map((item, index) => `${50 + index * 93},${getLineY(item.count)}`)
      .join(" ");
});

onMounted(() => {
  loadStats();
});
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  min-height: 220px;
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(
      135deg,
      var(--theme-hero-start) 0%,
      var(--theme-hero-middle) 55%,
      var(--theme-hero-end) 100%
  );
  transition: background 0.25s ease;
}

.hero-left {
  max-width: 60%;
}

.hero-badge {
  display: inline-block;
  background: var(--theme-accent-soft);
  color: var(--theme-accent);
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 13px;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 34px;
  font-weight: bold;
  color: var(--theme-title-color);
  margin-bottom: 14px;
}

.hero-desc {
  color: var(--theme-text-secondary);
  font-size: 15px;
  line-height: 1.8;
  margin-bottom: 18px;
}

.hero-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-tag {
  --el-tag-bg-color: var(--theme-accent-soft);
  --el-tag-border-color: var(--theme-accent-border);
  --el-tag-text-color: var(--theme-accent);
}

.hero-right {
  width: 320px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.pulse-wrap {
  position: relative;
  width: 220px;
  height: 220px;
}

.pulse-circle {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid var(--theme-accent-border);
  animation: pulse 2.8s infinite;
}

.pulse2 {
  animation-delay: 1.2s;
}

.core-circle {
  position: absolute;
  inset: 50%;
  width: 120px;
  height: 120px;
  margin-left: -60px;
  margin-top: -60px;
  border-radius: 50%;
  background: var(--theme-accent);
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 34px;
  font-weight: bold;
  box-shadow: 0 10px 24px var(--theme-accent-shadow);
  transition: background-color 0.25s ease, box-shadow 0.25s ease;
}

@keyframes pulse {
  0% {
    transform: scale(0.72);
    opacity: 0.7;
  }
  100% {
    transform: scale(1.18);
    opacity: 0;
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-card {
  padding: 22px;
}

.stat-label {
  color: var(--theme-text-muted);
  font-size: 14px;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: var(--theme-text-primary);
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  padding: 20px;
}

.chart-title {
  font-size: 18px;
  font-weight: bold;
  color: var(--theme-text-primary);
  margin-bottom: 12px;
}

.chart-svg {
  width: 100%;
  height: 280px;
}

.axis-text {
  font-size: 12px;
  fill: var(--theme-chart-axis);
}

.value-text {
  font-size: 12px;
  fill: var(--theme-chart-line);
  font-weight: bold;
}

.model-name-text {
  font-size: 10px;
}

@media (max-width: 1400px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1200px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
    gap: 24px;
    align-items: flex-start;
  }

  .hero-left {
    max-width: 100%;
  }
}
</style>
