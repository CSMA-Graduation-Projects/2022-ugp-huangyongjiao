<template>
  <div class="requirement-page">
    <div class="card requirement-card">
      <div class="page-title">需求规格说明书管理</div>

      <div class="toolbar">
        <div class="search-group">
          <el-input
              v-model="data.searchForm.requirementName"
              placeholder="请输入需求名称"
              clearable
              style="width: 220px"
              @keyup.enter="load"
          />
          <el-input
              v-model="data.searchForm.moduleName"
              placeholder="请输入所属模块"
              clearable
              style="width: 220px"
              @keyup.enter="load"
          />

          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="primary" plain @click="resetSearch">重置</el-button>
        </div>

        <div class="action-group">
          <el-tag v-if="hasLockedGenerationTask" type="warning" effect="light">
            当前存在未完成的批量生成任务，相关生成功能已锁定
          </el-tag>
          <el-button type="primary" @click="handleAdd">新增需求规格说明书</el-button>
        </div>
      </div>

      <div class="table-wrapper">
        <el-table :data="pagedTableData" stripe height="100%" style="width: 100%">
          <el-table-column label="编号" width="70" align="center">
            <template #default="scope">
              {{ (data.currentPage - 1) * data.pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>

          <el-table-column prop="requirementName" label="需求名称" min-width="150" show-overflow-tooltip>
            <template #default="scope">
              <span class="main-text">{{ scope.row.requirementName || "—" }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="moduleName" label="所属模块" min-width="120" align="center" show-overflow-tooltip>
            <template #default="scope">
              <span class="sub-text">{{ scope.row.moduleName || "—" }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
            <template #default="scope">
              <div class="ellipsis-text" :title="scope.row.remark || ''">
                {{ scope.row.remark || "—" }}
              </div>
            </template>
          </el-table-column>

          <el-table-column label="需求内容" min-width="320">
            <template #default="scope">
              <div class="content-preview">{{ scope.row.requirementContent || "—" }}</div>
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

    <el-dialog v-model="data.formVisible" :title="data.form.id ? '编辑需求规格说明书' : '新增需求规格说明书'" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="rules" label-width="110px" style="padding: 10px 20px 0 10px">
        <el-form-item label="需求名称" prop="requirementName">
          <el-input v-model="data.form.requirementName" placeholder="请输入需求名称" maxlength="255" show-word-limit />
        </el-form-item>

        <el-form-item label="所属模块" prop="moduleName">
          <el-input v-model="data.form.moduleName" placeholder="请输入所属模块" maxlength="255" show-word-limit />
        </el-form-item>

        <el-form-item label="前置条件说明" prop="preconditionDesc">
          <el-input v-model="data.form.preconditionDesc" type="textarea" :rows="3" placeholder="请输入前置条件说明" maxlength="2000" show-word-limit />
        </el-form-item>

        <el-form-item label="预期说明" prop="expectedDesc">
          <el-input v-model="data.form.expectedDesc" type="textarea" :rows="3" placeholder="请输入预期说明" maxlength="2000" show-word-limit />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="data.form.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="255" show-word-limit />
        </el-form-item>

        <el-form-item label="需求内容" prop="requirementContent">
          <el-input v-model="data.form.requirementContent" type="textarea" :rows="10" placeholder="请输入需求内容" maxlength="20000" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" plain @click="data.formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="data.viewVisible" title="需求规格说明书详情" width="760px" destroy-on-close>
      <div class="detail-wrapper">
        <div class="detail-item"><span class="detail-label">需求名称：</span><span>{{ data.viewData.requirementName || "—" }}</span></div>
        <div class="detail-item"><span class="detail-label">所属模块：</span><span>{{ data.viewData.moduleName || "—" }}</span></div>
        <div class="detail-item"><span class="detail-label">前置条件说明：</span><span>{{ data.viewData.preconditionDesc || "—" }}</span></div>
        <div class="detail-item"><span class="detail-label">预期说明：</span><span>{{ data.viewData.expectedDesc || "—" }}</span></div>
        <div class="detail-item"><span class="detail-label">备注：</span><span>{{ data.viewData.remark || "—" }}</span></div>
        <div class="detail-item detail-content">
          <div class="detail-label">需求内容：</div>
          <pre>{{ data.viewData.requirementContent || "—" }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
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

const formRef = ref();
const router = useRouter();
const currentUser = JSON.parse(localStorage.getItem("xm-user") || "{}");
let batchTaskPollingTimer = null;

const currentRole = currentUser.loginType === "USER" ? "USER" : currentUser.role;

const data = reactive({
  filteredTableData: [],
  currentPage: 1,
  pageSize: 5,
  batchTask: readStoredBatchExperimentTask(),

  searchForm: {
    requirementName: "",
    moduleName: ""
  },

  formVisible: false,
  viewVisible: false,

  form: {
    id: null,
    requirementName: "",
    moduleName: "",
    requirementContent: "",
    preconditionDesc: "",
    expectedDesc: "",
    remark: ""
  },

  viewData: {}
});

const hasLockedGenerationTask = computed(() => {
  return isBatchExperimentTaskActive(data.batchTask);
});

const generationLockNotice = computed(() => {
  return getBatchExperimentLockMessage(data.batchTask);
});

const rules = {
  requirementName: [{ required: true, message: "请输入需求名称", trigger: "blur" }],
  requirementContent: [{ required: true, message: "请输入需求内容", trigger: "blur" }]
};

const pagedTableData = computed(() => {
  const start = (data.currentPage - 1) * data.pageSize;
  const end = start + data.pageSize;
  return data.filteredTableData.slice(start, end);
});

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
  request.get("/requirementSpec/selectAll", {
    params: {
      requirementName: data.searchForm.requirementName,
      moduleName: data.searchForm.moduleName,
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    }
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      data.filteredTableData = res.data || [];
      data.currentPage = 1;
    } else {
      ElMessage.error(res.msg || "加载失败");
    }
  }).catch(() => ElMessage.error("请求失败"));
};

const handleCurrentChange = (page) => {
  data.currentPage = page;
};

const resetSearch = () => {
  data.searchForm.requirementName = "";
  data.searchForm.moduleName = "";
  load();
};

const resetForm = () => {
  data.form = {
    id: null,
    requirementName: "",
    moduleName: "",
    requirementContent: "",
    preconditionDesc: "",
    expectedDesc: "",
    remark: ""
  };
};

const handleAdd = () => {
  resetForm();
  data.formVisible = true;
};

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row));
  data.formVisible = true;
};

const handleView = (row) => {
  data.viewData = JSON.parse(JSON.stringify(row));
  data.viewVisible = true;
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除需求“${row.requirementName}”吗？`, "删除确认", { type: "warning" }).then(() => {
    request.delete(`/requirementSpec/delete/${row.id}`, {
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
    }).catch(() => ElMessage.error("请求失败"));
  }).catch(() => {});
};

const save = async () => {
  if (!formRef.value) return;

  await formRef.value.validate((valid) => {
    if (!valid) return;

    const url = data.form.id ? "/requirementSpec/update" : "/requirementSpec/add";
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
    }).catch(() => ElMessage.error("请求失败"));
  });
};

const handleGenerate = (row) => {
  if (hasLockedGenerationTask.value) {
    ElMessage.warning(generationLockNotice.value);
    return;
  }
  writeRuntimePageCache(RUNTIME_PAGE_CACHE_KEYS.generateIncomingSource, {
    sourceType: "requirement",
    payload: row
  });
  router.push("/manager/generate");
};

onMounted(() => {
  load();
  initializeBatchTaskState();
});

onBeforeUnmount(() => {
  stopBatchTaskPolling();
});
</script>

<style scoped>
.requirement-page {
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
}

.requirement-card {
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

.content-preview {
  max-height: 98px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.6;
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

.detail-content pre {
  margin: 8px 0 0;
  padding: 12px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
}
</style>
