<template>
  <div class="function-page">
    <div class="card function-card">
      <div class="page-title">函数管理</div>

      <div class="toolbar">
        <div class="search-group">
          <el-input
              v-model="data.searchForm.functionName"
              placeholder="请输入函数名称"
              clearable
              style="width: 220px"
              @keyup.enter="load"
          />
          <el-input
              v-model="data.searchForm.className"
              placeholder="请输入所属类名"
              clearable
              style="width: 220px"
              @keyup.enter="load"
          />
          <el-select
              v-model="data.searchForm.language"
              placeholder="请选择语言"
              clearable
              style="width: 160px"
          >
            <el-option label="Java" value="Java" />
            <el-option label="Python" value="Python" />
            <el-option label="C" value="C" />
            <el-option label="C++" value="C++" />
            <el-option label="JavaScript" value="JavaScript" />
          </el-select>

          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="primary" plain @click="resetSearch">重置</el-button>
        </div>

        <div class="action-group">
          <el-tag v-if="hasLockedGenerationTask" type="warning" effect="light">
            当前存在未完成的批量生成任务，相关生成功能已锁定
          </el-tag>
          <el-tag v-if="selectedFunctionCount > 0" type="warning" effect="light">
            已选 {{ selectedFunctionCount }} 个函数
          </el-tag>
          <el-button
              v-if="selectedFunctionCount > 0"
              type="primary"
              :disabled="hasLockedGenerationTask"
              @click="handleBatchGenerate"
          >
            批量生成
          </el-button>
          <el-button type="success" plain @click="downloadImportTemplate">下载导入模板</el-button>
          <el-button type="warning" plain @click="openImportDialog">批量导入</el-button>
          <el-button type="primary" @click="handleAdd">新增函数</el-button>
        </div>
      </div>

      <div class="table-wrapper">
        <el-table
            ref="functionTableRef"
            :data="pagedTableData"
            row-key="id"
            stripe
            height="100%"
            style="width: 100%"
            @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" :reserve-selection="true" />
          <el-table-column label="编号" width="70" align="center">
            <template #default="scope">
              {{ (data.currentPage - 1) * data.pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>

          <el-table-column prop="functionName" label="函数名称" min-width="120" show-overflow-tooltip>
            <template #default="scope">
              <span class="main-text">{{ scope.row.functionName || "—" }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="className" label="所属类名" min-width="100" align="center" show-overflow-tooltip>
            <template #default="scope">
              <span class="sub-text">{{ scope.row.className || "—" }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="language" label="语言" width="110" align="center">
            <template #default="scope">
              <el-tag type="success" effect="light">{{ scope.row.language || "—" }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="publicAssertCount" label="公开断言数" width="110" align="center">
            <template #default="scope">
              <span>{{ scope.row.publicAssertCount ?? 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
            <template #default="scope">
              <div class="ellipsis-text" :title="scope.row.remark || ''">
                {{ scope.row.remark || "—" }}
              </div>
            </template>
          </el-table-column>

          <el-table-column label="函数代码" min-width="300">
            <template #default="scope">
              <div class="code-preview">{{ scope.row.codeText || "—" }}</div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="250" fixed="right" align="center">
            <template #default="scope">
              <div class="op-wrap">
                <div class="op-row top-row">
                  <el-button type="primary" size="small" :disabled="hasLockedGenerationTask" @click="handleGenerate(scope.row)">生成测试用例</el-button>
                </div>
                <div class="op-row bottom-row">
                  <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
                  <el-button type="primary" plain size="small" @click="handleView(scope.row)">查看</el-button>
                  <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
            background
            layout="prev, pager, next, total"
            :total="data.filteredTableData.length"
            :page-size="data.pageSize"
            :current-page="data.currentPage"
            @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog
        v-model="data.formVisible"
        :title="data.form.id ? '编辑函数' : '新增函数'"
        width="760px"
        destroy-on-close
    >
      <el-form
          ref="formRef"
          :model="data.form"
          :rules="rules"
          label-width="90px"
          style="padding: 10px 20px 0 10px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="函数名称" prop="functionName">
              <el-input
                  v-model="data.form.functionName"
                  placeholder="请输入函数名称"
                  maxlength="255"
                  show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属类名" prop="className">
              <el-input
                  v-model="data.form.className"
                  placeholder="请输入所属类名"
                  maxlength="255"
                  show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="语言" prop="language">
              <el-select v-model="data.form.language" placeholder="请选择语言" style="width: 100%">
                <el-option label="Java" value="Java" />
                <el-option label="Python" value="Python" />
                <el-option label="C" value="C" />
                <el-option label="C++" value="C++" />
                <el-option label="JavaScript" value="JavaScript" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
                label="人工用例数"
                prop="testerCaseCount"
                label-width="110px"
                class="tester-case-item"
            >
              <el-input-number
                  v-model="data.form.testerCaseCount"
                  :min="1"
                  :max="999"
                  style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="输入说明" prop="inputDesc">
          <el-input
              v-model="data.form.inputDesc"
              type="textarea"
              :rows="3"
              placeholder="请输入输入说明"
              maxlength="2000"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="输出说明" prop="outputDesc">
          <el-input
              v-model="data.form.outputDesc"
              type="textarea"
              :rows="3"
              placeholder="请输入输出说明"
              maxlength="2000"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
              v-model="data.form.remark"
              type="textarea"
              :rows="2"
              placeholder="请输入备注"
              maxlength="255"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="函数代码" prop="codeText">
          <el-input
              v-model="data.form.codeText"
              type="textarea"
              :rows="10"
              placeholder="请输入函数代码"
              maxlength="20000"
              show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" plain @click="data.formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="data.importVisible" title="批量导入函数" width="860px" destroy-on-close>
      <div class="import-wrapper">
        <div class="import-tip">
          <div class="tip-title">导入说明</div>
          <div class="tip-text">支持 .xlsx、.xls、.csv、.json、.jsonl。表格导入兼容 publicTestContent、publicTestSource、publicAssertCount 字段；HumanEval 会从 test 字段提取 assert，MBPP 会读取 test_list。</div>
        </div>

        <el-row :gutter="16" style="margin-bottom: 12px;">
          <el-col :span="8">
            <el-form-item label="默认语言" label-width="90px">
              <el-select v-model="data.importConfig.defaultLanguage" style="width: 100%">
                <el-option label="Java" value="Java" />
                <el-option label="Python" value="Python" />
                <el-option label="C" value="C" />
                <el-option label="C++" value="C++" />
                <el-option label="JavaScript" value="JavaScript" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认类名" label-width="90px">
              <el-input v-model="data.importConfig.defaultClassName" maxlength="255" placeholder="缺失时自动补全" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认用例数" label-width="90px">
              <el-input-number v-model="data.importConfig.defaultTesterCaseCount" :min="1" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="import-actions">
          <el-button type="success" plain @click="downloadImportTemplate">下载模板</el-button>
          <el-button type="primary" @click="triggerFileSelect">选择文件</el-button>
          <input ref="fileInputRef" type="file" accept=".xlsx,.xls,.csv,.json,.jsonl" class="hidden-file-input" @change="handleImportFileChange" />
          <span class="file-name">{{ data.importFileName || '未选择文件' }}</span>
        </div>

        <div v-if="data.importFileName" class="import-summary">
          <el-tag type="primary" effect="light">解析总行数：{{ data.importTotalCount }}</el-tag>
          <el-tag type="success" effect="light">可导入：{{ data.importList.length }}</el-tag>
          <el-tag type="danger" effect="light">需处理：{{ data.importErrors.length }}</el-tag>
        </div>

        <div v-if="data.importErrors.length" class="import-error-box">
          <div class="import-error-title">以下行存在问题，已在导入前拦截：</div>
          <div v-for="(item, index) in data.importErrors.slice(0, 20)" :key="index" class="import-error-item">
            {{ item }}
          </div>
          <div v-if="data.importErrors.length > 20" class="import-error-item">仅展示前20条问题，请先修正文件后再导入。</div>
        </div>

        <div class="preview-title">导入预览（最多显示 6 条有效数据）</div>
        <el-table :data="data.importPreview" stripe max-height="260" style="width: 100%">
          <el-table-column label="序号" width="60" align="center">
            <template #default="scope">{{ scope.$index + 1 }}</template>
          </el-table-column>
          <el-table-column prop="functionName" label="函数名称" min-width="120" show-overflow-tooltip />
          <el-table-column prop="className" label="所属类名" min-width="120" show-overflow-tooltip />
          <el-table-column prop="language" label="语言" width="100" align="center" />
          <el-table-column prop="testerCaseCount" label="人工用例数" width="110" align="center" />
          <el-table-column prop="publicAssertCount" label="公开断言数" width="110" align="center" />
          <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        </el-table>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" plain @click="data.importVisible = false">取消</el-button>
          <el-button type="primary" :disabled="data.importList.length === 0" @click="submitBatchImport">确认导入</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="data.viewVisible" title="函数详情" width="760px" destroy-on-close>
      <div class="detail-wrapper">
        <div class="detail-item">
          <span class="detail-label">函数名称：</span>
          <span>{{ data.viewData.functionName || "—" }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">所属类名：</span>
          <span>{{ data.viewData.className || "—" }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">语言：</span>
          <span>{{ data.viewData.language || "—" }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">输入说明：</span>
          <span>{{ data.viewData.inputDesc || "—" }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">输出说明：</span>
          <span>{{ data.viewData.outputDesc || "—" }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">备注：</span>
          <span>{{ data.viewData.remark || "—" }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">人工用例数：</span>
          <span>{{ data.viewData.testerCaseCount ?? "—" }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">公开测试来源：</span>
          <span>{{ data.viewData.publicTestSource || "暂无公开断言内容" }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">公开断言数：</span>
          <span>{{ data.viewData.publicAssertCount ?? 0 }}</span>
        </div>
        <div class="detail-item detail-code">
          <div class="detail-label">公开断言内容：</div>
          <pre>{{ data.viewData.publicTestContent || "暂无公开断言内容" }}</pre>
        </div>
        <div class="detail-item detail-code">
          <div class="detail-label">函数代码：</div>
          <pre>{{ data.viewData.codeText || "—" }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, computed, onBeforeUnmount, onMounted, ref, nextTick, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import * as XLSX from "xlsx";
import request from "@/utils/request.js";
import {
  fetchBatchExperimentTaskById,
  getBatchExperimentLockMessage,
  isBatchExperimentTaskActive,
  normalizeBatchExperimentTask,
  readStoredBatchExperimentTask,
  resolveLatestBatchExperimentTask
} from "@/utils/batchExperimentTask.js";
import { RUNTIME_PAGE_CACHE_KEYS, writeRuntimePageCache } from "@/utils/runtimePageCache.js";

const BATCH_EXPERIMENT_FUNCTIONS_KEY = "batchExperimentSelectedFunctions";
const formRef = ref();
const fileInputRef = ref();
const functionTableRef = ref();
const restoringSelection = ref(false);
let batchTaskPollingTimer = null;
const router = useRouter();
const currentUser = JSON.parse(localStorage.getItem("xm-user") || "{}");
const currentRole = currentUser.loginType === "USER" ? "USER" : currentUser.role;
const supportedLanguages = ["Java", "Python", "C", "C++", "JavaScript"];

const createEmptyForm = () => ({
  id: null,
  functionName: "",
  className: "",
  language: "Java",
  inputDesc: "",
  outputDesc: "",
  remark: "",
  testerCaseCount: 6,
  publicTestContent: "",
  publicTestSource: "",
  publicAssertCount: 0,
  codeText: ""
});

const createImportConfig = () => ({
  defaultLanguage: "Java",
  defaultClassName: "DefaultClass",
  defaultTesterCaseCount: 6
});

const data = reactive({
  filteredTableData: [],
  currentPage: 1,
  pageSize: 5,
  batchTask: readStoredBatchExperimentTask(),

  searchForm: {
    functionName: "",
    className: "",
    language: ""
  },

  formVisible: false,
  viewVisible: false,
  importVisible: false,

  form: createEmptyForm(),
  viewData: {},

  importConfig: createImportConfig(),
  importFileName: "",
  importTotalCount: 0,
  importList: [],
  importPreview: [],
  importErrors: [],
  selectedFunctionMap: {}
});

const hasLockedGenerationTask = computed(() => {
  return isBatchExperimentTaskActive(data.batchTask);
});

const generationLockNotice = computed(() => {
  return getBatchExperimentLockMessage(data.batchTask);
});

const rules = {
  functionName: [{ required: true, message: "请输入函数名称", trigger: "blur" }],
  className: [{ required: true, message: "请输入所属类名", trigger: "blur" }],
  language: [{ required: true, message: "请选择语言", trigger: "change" }],
  codeText: [{ required: true, message: "请输入函数代码", trigger: "blur" }],
  testerCaseCount: [{ required: true, message: "请输入测试人员预估用例数", trigger: "change" }]
};

const pagedTableData = computed(() => {
  const start = (data.currentPage - 1) * data.pageSize;
  const end = start + data.pageSize;
  return data.filteredTableData.slice(start, end);
});

const selectedFunctionList = computed(() => {
  return data.filteredTableData
      .filter(item => !!data.selectedFunctionMap[item.id])
      .map(item => data.selectedFunctionMap[item.id] || item);
});

const selectedFunctionCount = computed(() => selectedFunctionList.value.length);

const syncSelectedFunctionsWithCurrentList = (list = data.filteredTableData) => {
  const rowMap = new Map((list || []).map(item => [String(item.id), item]));

  Object.keys(data.selectedFunctionMap).forEach(key => {
    if (!rowMap.has(String(key))) {
      delete data.selectedFunctionMap[key];
      return;
    }

    data.selectedFunctionMap[key] = rowMap.get(String(key));
  });
};

const restoreTableSelection = async () => {
  if (!functionTableRef.value) {
    return;
  }

  restoringSelection.value = true;
  functionTableRef.value.clearSelection();

  pagedTableData.value.forEach(row => {
    if (data.selectedFunctionMap[row.id]) {
      functionTableRef.value.toggleRowSelection(row, true);
    }
  });

  await nextTick();
  restoringSelection.value = false;
};

const handleSelectionChange = (selection) => {
  if (restoringSelection.value) {
    return;
  }

  const currentPageIdSet = new Set(pagedTableData.value.map(item => String(item.id)));
  currentPageIdSet.forEach(id => {
    delete data.selectedFunctionMap[id];
  });

  selection.forEach(item => {
    data.selectedFunctionMap[item.id] = item;
  });
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

const load = () => {
  request.get("/functionInfo/selectAll", {
    params: {
      functionName: data.searchForm.functionName,
      className: data.searchForm.className,
      language: data.searchForm.language,
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    }
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      data.filteredTableData = res.data || [];
      syncSelectedFunctionsWithCurrentList(data.filteredTableData);
      data.currentPage = 1;
    } else {
      ElMessage.error(res.msg || "加载失败");
    }
  }).catch(() => {
    ElMessage.error("请求失败");
  });
};

const handleCurrentChange = (page) => {
  data.currentPage = page;
};

const resetSearch = () => {
  data.searchForm.functionName = "";
  data.searchForm.className = "";
  data.searchForm.language = "";
  load();
};

const resetForm = () => {
  data.form = createEmptyForm();
};

const resetImportState = () => {
  data.importConfig = createImportConfig();
  data.importFileName = "";
  data.importTotalCount = 0;
  data.importList = [];
  data.importPreview = [];
  data.importErrors = [];
  if (fileInputRef.value) {
    fileInputRef.value.value = "";
  }
};

const handleAdd = () => {
  resetForm();
  data.formVisible = true;
};

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row));
  if (data.form.testerCaseCount == null || data.form.testerCaseCount === "") {
    data.form.testerCaseCount = 6;
  }
  data.formVisible = true;
};

const handleView = (row) => {
  data.viewData = JSON.parse(JSON.stringify(row));
  data.viewVisible = true;
};

const handleGenerate = (row) => {
  if (hasLockedGenerationTask.value) {
    ElMessage.warning(generationLockNotice.value);
    return;
  }
  writeRuntimePageCache(RUNTIME_PAGE_CACHE_KEYS.generateIncomingSource, {
    sourceType: "function",
    payload: row
  });
  router.push("/manager/generate");
};

const handleBatchGenerate = () => {
  if (hasLockedGenerationTask.value) {
    ElMessage.warning(generationLockNotice.value);
    return;
  }
  if (selectedFunctionList.value.length === 0) {
    ElMessage.warning("请至少勾选一条函数记录");
    return;
  }

  writeRuntimePageCache(
      RUNTIME_PAGE_CACHE_KEYS.batchExperimentIncomingFunctions,
      selectedFunctionList.value
  );
  router.push("/manager/batchExperiment");
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除函数“${row.functionName}”吗？`, "删除确认", {
    type: "warning"
  }).then(() => {
    request.delete(`/functionInfo/delete/${row.id}`, {
      params: {
        currentUserId: currentUser.id,
        currentUserRole: currentRole
      }
    }).then(res => {
      if (res.code === "200" || res.code === 200) {
        ElMessage.success("删除成功");
        load();
      } else {
        ElMessage.error(res.msg || "删除失败");
      }
    }).catch(() => {
      ElMessage.error("请求失败");
    });
  }).catch(() => {});
};

const save = async () => {
  if (!formRef.value) return;

  await formRef.value.validate((valid) => {
    if (!valid) return;

    const url = data.form.id ? "/functionInfo/update" : "/functionInfo/add";
    const method = data.form.id ? request.put : request.post;

    const payload = {
      ...data.form,
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    };

    method(url, payload).then(res => {
      if (res.code === "200" || res.code === 200) {
        ElMessage.success(data.form.id ? "更新成功" : "新增成功");
        data.formVisible = false;
        load();
      } else {
        ElMessage.error(res.msg || "保存失败");
      }
    }).catch(() => {
      ElMessage.error("请求失败");
    });
  });
};

const downloadImportTemplate = () => {
  const templateRows = [{
    "函数名称": "calculateDiscount",
    "所属类名": "OrderService",
    "语言": "Java",
    "输入说明": "price: 原价；memberLevel: 会员等级；isHoliday: 是否节假日",
    "输出说明": "返回折后价格",
    "备注": "示例数据，可删除后批量填写",
    "人工用例数": 6,
    "公开测试来源": "",
    "公开断言数": 0,
    "公开断言内容": "",
    "函数代码": "public double calculateDiscount(double price, int memberLevel, boolean isHoliday) {\n    if (price <= 0) {\n        throw new IllegalArgumentException(\"price must be positive\");\n    }\n    double discount = memberLevel >= 3 ? 0.8 : 0.95;\n    if (isHoliday) {\n        discount -= 0.05;\n    }\n    return price * Math.max(discount, 0.7);\n}"
  }];
  const worksheet = XLSX.utils.json_to_sheet(templateRows);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, "函数导入模板");
  XLSX.writeFile(workbook, "函数批量导入模板.xlsx");
};

const openImportDialog = () => {
  resetImportState();
  data.importVisible = true;
};

const triggerFileSelect = () => {
  fileInputRef.value?.click();
};

const normalizeText = (value) => {
  if (value === null || value === undefined) {
    return "";
  }
  return String(value).trim();
};

const findCellValue = (row, keys) => {
  for (const key of keys) {
    const matchedKey = Object.keys(row).find(item => normalizeText(item).toLowerCase() === key.toLowerCase());
    if (matchedKey !== undefined) {
      return row[matchedKey];
    }
  }
  return "";
};

const normalizePublicAssertions = (value) => {
  if (Array.isArray(value)) {
    return value.map(item => String(item ?? "").trim()).filter(Boolean).join("\n");
  }

  const text = String(value ?? "").trim();
  if (!text) {
    return "";
  }

  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) {
      return parsed.map(item => String(item ?? "").trim()).filter(Boolean).join("\n");
    }
  } catch (e) {
  }

  return text
      .replace(/\r/g, "")
      .split("\n")
      .map(item => item.trim())
      .filter(Boolean)
      .join("\n");
};

const countAssertionLines = (content) => {
  return String(content || "")
      .replace(/\r/g, "")
      .split("\n")
      .map(item => item.trim())
      .filter(Boolean)
      .length;
};

const normalizeImportRow = (row, index) => {
  const functionName = normalizeText(findCellValue(row, ["函数名称", "函数名", "functionName"]));
  const className = normalizeText(findCellValue(row, ["所属类名", "类名", "className"])) || normalizeText(data.importConfig.defaultClassName);
  const language = normalizeText(findCellValue(row, ["语言", "language"])) || data.importConfig.defaultLanguage;
  const inputDesc = normalizeText(findCellValue(row, ["输入说明", "inputDesc"]));
  const outputDesc = normalizeText(findCellValue(row, ["输出说明", "outputDesc"]));
  const remark = normalizeText(findCellValue(row, ["备注", "remark"]));
  const codeText = String(findCellValue(row, ["函数代码", "代码", "codeText"]) ?? "").trim();
  const testerValue = findCellValue(row, ["人工用例数", "testerCaseCount"]);
  const testerCaseCount = Number.parseInt(testerValue, 10) > 0
      ? Number.parseInt(testerValue, 10)
      : data.importConfig.defaultTesterCaseCount;
  const publicTestContent = normalizePublicAssertions(findCellValue(row, ["公开断言内容", "公开测试内容", "publicTestContent", "test_list", "test"]));
  const publicTestSource = normalizeText(findCellValue(row, ["公开测试来源", "publicTestSource"]));
  const publicAssertValue = findCellValue(row, ["公开断言数", "publicAssertCount"]);
  const publicAssertCount = Number.parseInt(publicAssertValue, 10) >= 0
      ? Number.parseInt(publicAssertValue, 10)
      : countAssertionLines(publicTestContent);

  const errors = [];
  if (!functionName) errors.push("函数名称不能为空");
  if (!className) errors.push("所属类名不能为空");
  if (!language) errors.push("语言不能为空");
  if (!codeText) errors.push("函数代码不能为空");
  if (functionName.length > 255) errors.push("函数名称长度不能超过255");
  if (className.length > 255) errors.push("所属类名长度不能超过255");
  if (inputDesc.length > 2000) errors.push("输入说明长度不能超过2000");
  if (outputDesc.length > 2000) errors.push("输出说明长度不能超过2000");
  if (remark.length > 255) errors.push("备注长度不能超过255");
  if (codeText.length > 20000) errors.push("函数代码长度不能超过20000");

  return {
    rowIndex: index + 1,
    item: {
      functionName,
      className,
      language: supportedLanguages.includes(language) ? language : language,
      inputDesc,
      outputDesc,
      remark,
      testerCaseCount,
      publicTestContent,
      publicTestSource,
      publicAssertCount,
      codeText
    },
    errors
  };
};

const parseJsonDatasetRows = (text, fileName) => {
  const content = String(text || "").trim();
  if (!content) {
    return [];
  }

  if (String(fileName || "").toLowerCase().endsWith(".jsonl")) {
    return content
        .split(/\r?\n/)
        .map(item => item.trim())
        .filter(Boolean)
        .map((item, index) => {
          try {
            return JSON.parse(item);
          } catch (e) {
            return {
              __parseError: `第${index + 1}行 JSON 解析失败：${e.message || "格式错误"}`
            };
          }
        });
  }

  const root = JSON.parse(content);
  if (Array.isArray(root)) {
    return root;
  }

  for (const key of ["data", "items", "examples", "tasks", "problems"]) {
    if (Array.isArray(root?.[key])) {
      return root[key];
    }
  }
  return root ? [root] : [];
};

const extractPythonFunctionName = (codeText) => {
  const match = String(codeText || "").match(/\bdef\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/);
  return match?.[1] || "";
};

const extractHumanEvalAssertions = (testText) => {
  return String(testText || "")
      .replace(/\r/g, "")
      .split("\n")
      .map(item => item.trim())
      .filter(item => /^assert\b/.test(item) && /\bcandidate\s*\(/.test(item))
      .join("\n");
};

const extractHumanEvalInputDesc = (prompt) => {
  const text = String(prompt || "");
  const match = text.match(/(?:[rRuUbBfF]*)("""|''')([\s\S]*?)\1/);
  if (!match) {
    return "输入参数见函数代码中的文档说明";
  }

  let lines = match[2].replace(/\r/g, "").split("\n");
  while (lines.length && !lines[0].trim()) {
    lines.shift();
  }
  while (lines.length && !lines[lines.length - 1].trim()) {
    lines.pop();
  }

  const nonEmptyIndents = lines
      .filter(line => line.trim())
      .map(line => (line.match(/^\s*/) || [""])[0].length);
  const minIndent = nonEmptyIndents.length ? Math.min(...nonEmptyIndents) : 0;

  const desc = lines
      .map(line => line.slice(Math.min(minIndent, line.length)))
      .join("\n")
      .trim();
  return desc || "输入参数见函数代码中的文档说明";
};

const buildHumanEvalCode = (row) => {
  const prompt = String(row?.prompt ?? "");
  const solution = String(row?.canonical_solution ?? "");
  const code = String(row?.code ?? "");
  if (code.trim()) {
    return code.trim();
  }
  return `${prompt}${solution}`.trim();
};

const normalizeDatasetRow = (row, index) => {
  if (row?.__parseError) {
    return {
      rowIndex: index + 1,
      item: null,
      errors: [row.__parseError]
    };
  }

  if (Array.isArray(row?.test_list)) {
    const publicTestContent = normalizePublicAssertions(row.test_list);
    const publicAssertCount = row.test_list.length;
    const codeText = String(row.code ?? row.canonical_solution ?? row.prompt ?? "").trim();
    const functionName = normalizeText(row.entry_point) || extractPythonFunctionName(codeText) || `mbpp_${row.task_id ?? index + 1}`;
    return normalizeImportRow({
      "函数名称": functionName,
      "所属类名": "MBPPTask",
      "语言": "Python",
      "输入说明": row.text ?? row.prompt ?? "",
      "输出说明": "",
      "备注": `来源：MBPP；任务ID：${row.task_id ?? "未知"}；参考测试用例数：${publicAssertCount}`,
      "人工用例数": publicAssertCount || data.importConfig.defaultTesterCaseCount,
      "公开测试来源": "MBPP",
      "公开断言数": publicAssertCount,
      "公开断言内容": publicTestContent,
      "函数代码": codeText
    }, index);
  }

  if (typeof row?.test === "string" && (row.entry_point || row.prompt || String(row.task_id || "").includes("HumanEval"))) {
    const codeText = buildHumanEvalCode(row);
    const publicTestContent = extractHumanEvalAssertions(row.test);
    const publicAssertCount = countAssertionLines(publicTestContent);
    const functionName = normalizeText(row.entry_point) || extractPythonFunctionName(codeText) || `humaneval_${index + 1}`;
    return normalizeImportRow({
      "函数名称": functionName,
      "所属类名": "HumanEvalTask",
      "语言": "Python",
      "输入说明": extractHumanEvalInputDesc(row.prompt),
      "输出说明": "",
      "备注": `来源：HumanEval；任务ID：${row.task_id ?? "未知"}；参考测试用例数：${publicAssertCount}`,
      "人工用例数": publicAssertCount || data.importConfig.defaultTesterCaseCount,
      "公开测试来源": "HumanEval",
      "公开断言数": publicAssertCount,
      "公开断言内容": publicTestContent,
      "函数代码": codeText
    }, index);
  }

  return normalizeImportRow(row, index);
};

const applyParsedImportRows = (rows, normalizer = normalizeImportRow) => {
  const validList = [];
  const errors = [];

  rows.forEach((row, index) => {
    const result = normalizer(row, index);
    if (result.errors.length) {
      errors.push(`第${result.rowIndex}行：${result.errors.join("；")}`);
    } else {
      validList.push(result.item);
    }
  });

  data.importList = validList;
  data.importPreview = validList.slice(0, 6);
  data.importErrors = errors;
};

const handleImportFileChange = async (event) => {
  const file = event.target.files?.[0];
  if (!file) {
    return;
  }

  data.importFileName = file.name;

  try {
    const lowerFileName = file.name.toLowerCase();
    let rows = [];
    let normalizer = normalizeImportRow;

    if (lowerFileName.endsWith(".json") || lowerFileName.endsWith(".jsonl")) {
      rows = parseJsonDatasetRows(await file.text(), file.name);
      normalizer = normalizeDatasetRow;
    } else {
      const buffer = await file.arrayBuffer();
      const workbook = XLSX.read(buffer, { type: "array" });
      const sheetName = workbook.SheetNames?.[0];
      if (!sheetName) {
        ElMessage.warning("未读取到工作表");
        return;
      }

      const worksheet = workbook.Sheets[sheetName];
      rows = XLSX.utils.sheet_to_json(worksheet, { defval: "" });
    }

    data.importTotalCount = rows.length;
    applyParsedImportRows(rows, normalizer);

    if (!rows.length) {
      ElMessage.warning("文件中没有可读取的数据");
      return;
    }

    if (data.importList.length > 0) {
      ElMessage.success(`解析完成，可导入 ${data.importList.length} 条数据`);
    } else {
      ElMessage.warning("文件解析完成，但没有可导入的数据");
    }
  } catch (e) {
    console.error(e);
    ElMessage.error("文件解析失败，请检查模板格式");
  } finally {
    if (fileInputRef.value) {
      fileInputRef.value.value = "";
    }
  }
};

const escapeHtml = (text) => {
  return String(text ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
};

const buildImportResultHtml = (resultData) => {
  const totalCount = resultData?.totalCount ?? data.importList.length;
  const insertedCount = resultData?.insertedCount ?? 0;
  const duplicateCount = resultData?.duplicateCount ?? 0;
  const publicTestUpdatedCount = resultData?.publicTestUpdatedCount ?? 0;
  const failedCount = resultData?.failedCount ?? 0;
  const duplicateMessages = resultData?.duplicateMessages || [];
  const failedMessages = resultData?.failedMessages || [];

  let html = `
    <div style="line-height: 1.8;">
      <div>本次共导入 <b>${totalCount}</b> 条数据。</div>
      <div>新增函数：<b>${insertedCount}</b> 条。</div>
      <div>更新公开断言：<b>${publicTestUpdatedCount}</b> 条。</div>
      <div>重复跳过：<b>${duplicateCount}</b> 条。</div>
      <div>导入失败：<b>${failedCount}</b> 条。</div>
    </div>
  `;

  if (duplicateMessages.length) {
    html += `<div style="margin-top: 12px;"><b>处理明细（最多展示20条）：</b></div>`;
    html += `<div style="max-height: 180px; overflow-y: auto; margin-top: 6px; padding: 10px; background: #f8f8f8; border-radius: 6px; line-height: 1.7;">${duplicateMessages.map(item => escapeHtml(item)).join("<br/>")}</div>`;
  }

  if (failedMessages.length) {
    html += `<div style="margin-top: 12px;"><b>失败明细（最多展示20条）：</b></div>`;
    html += `<div style="max-height: 180px; overflow-y: auto; margin-top: 6px; padding: 10px; background: #f8f8f8; border-radius: 6px; line-height: 1.7;">${failedMessages.map(item => escapeHtml(item)).join("<br/>")}</div>`;
  }

  return html;
};

const submitBatchImport = () => {
  if (!data.importList.length) {
    ElMessage.warning("请先选择有效的导入文件");
    return;
  }

  request.post("/functionInfo/batchAdd", data.importList, {
    params: {
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    }
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      const insertedCount = res.data?.insertedCount ?? 0;
      const duplicateCount = res.data?.duplicateCount ?? 0;
      const publicTestUpdatedCount = res.data?.publicTestUpdatedCount ?? 0;
      const failedCount = res.data?.failedCount ?? 0;

      ElMessageBox.alert(buildImportResultHtml(res.data), "导入结果", {
        dangerouslyUseHTMLString: true,
        confirmButtonText: "确定",
        type: (insertedCount > 0 || publicTestUpdatedCount > 0) ? "success" : "warning"
      }).then(() => {
        data.importVisible = false;
        resetImportState();
        load();
      });

      if (insertedCount > 0 || publicTestUpdatedCount > 0 || duplicateCount > 0 || failedCount > 0) {
        ElMessage.success(`导入完成：新增 ${insertedCount} 条，更新公开断言 ${publicTestUpdatedCount} 条，重复跳过 ${duplicateCount} 条，失败 ${failedCount} 条`);
      }
    } else {
      ElMessage.error(res.msg || "批量导入失败");
    }
  }).catch(() => {
    ElMessage.error("请求失败");
  });
};

onMounted(() => {
  load();
  initializeBatchTaskState();
});

watch(pagedTableData, () => {
  nextTick(() => {
    restoreTableSelection();
  });
});

onBeforeUnmount(() => {
  stopBatchTaskPolling();
});
</script>

<style scoped>
.function-page {
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
}

.function-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #3d2a1d;
  margin-bottom: 14px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.search-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.action-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.table-wrapper {
  flex: 1;
  min-height: 0;
}

.main-text {
  font-weight: 600;
}

.sub-text {
  color: #666;
}

.ellipsis-text {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.code-preview {
  max-height: 98px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 6px;
  padding: 8px 10px;
  font-family: Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}

.pagination-wrapper {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.op-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.op-row {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.top-row {
  width: 100%;
}

.bottom-row {
  width: 100%;
}

.detail-wrapper {
  line-height: 1.9;
  color: #333;
}

.detail-item {
  margin-bottom: 10px;
  word-break: break-all;
}

.detail-label {
  font-weight: bold;
  color: #5a2d0c;
  margin-right: 6px;
}

.detail-code pre {
  margin: 8px 0 0;
  padding: 12px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
}

.tester-case-item :deep(.el-form-item__label) {
  white-space: nowrap;
}

.tester-case-item :deep(.el-input-number) {
  width: 100%;
}

.import-wrapper {
  padding-top: 4px;
}

.import-tip {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #faf6ef;
  border: 1px solid #ebe0d2;
}

.tip-title {
  font-size: 14px;
  font-weight: 700;
  color: #5a2d0c;
  margin-bottom: 4px;
}

.tip-text {
  color: #666;
  line-height: 1.7;
}

.import-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.hidden-file-input {
  display: none;
}

.file-name {
  color: #666;
  font-size: 13px;
}

.import-summary {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.import-error-box {
  max-height: 180px;
  overflow: auto;
  border-radius: 8px;
  background: rgba(245, 108, 108, 0.08);
  border: 1px solid rgba(245, 108, 108, 0.24);
  padding: 12px;
  margin-bottom: 12px;
}

.import-error-title {
  font-weight: 700;
  color: #c45656;
  margin-bottom: 8px;
}

.import-error-item {
  color: #a94442;
  line-height: 1.7;
}

.preview-title {
  font-size: 14px;
  font-weight: 700;
  color: #333;
  margin-bottom: 10px;
}
</style>
