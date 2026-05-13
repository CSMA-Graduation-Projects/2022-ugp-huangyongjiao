<template>
  <div class="model-page">
    <div class="card" style="margin-bottom: 10px;">
      <div class="toolbar">
        <div class="search-group">
          <el-input
              v-model="data.searchForm.modelName"
              placeholder="请输入模型名称"
              clearable
              style="width: 220px"
              @keyup.enter="load"
          />
          <el-select
              v-model="data.searchForm.status"
              placeholder="请选择状态"
              clearable
              style="width: 160px"
          >
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>

          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="primary" plain @click="resetSearch">重置</el-button>
        </div>

        <div>
          <el-button type="primary" @click="handleAdd">新增模型</el-button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="page-title">模型配置</div>

      <div class="default-eval-bar">
        <div class="default-eval-info">
          <div class="default-eval-title">默认评估模型</div>
          <div class="default-eval-desc">生成完成后的自动评估会默认调用这里选择的已启用模型。</div>
        </div>

        <div class="default-eval-actions">
          <el-select
              v-model="data.defaultEvaluationModelId"
              placeholder="请选择已启用模型"
              style="width: 260px"
              clearable
          >
            <el-option
                v-for="item in enabledModelOptions"
                :key="item.id"
                :label="item.modelName"
                :value="item.id"
            />
          </el-select>
          <el-button type="primary" @click="saveDefaultEvaluationModel">保存默认评估模型</el-button>
        </div>
      </div>

      <el-table :data="pagedTableData" stripe style="width: 100%">
        <el-table-column label="编号" width="70" align="center">
          <template #default="scope">
            {{ (data.currentPage - 1) * data.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="modelName" label="模型名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="apiUrl" label="API地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="temperature" label="温度系数" width="100" align="center" />
        <el-table-column prop="maxTokens" label="最大Token" width="110" align="center" />

        <el-table-column label="评估默认" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.id === data.defaultEvaluationModelId" type="warning" effect="light">默认</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '启用' ? 'success' : 'info'" effect="light">
              {{ scope.row.status || "—" }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" fixed="right" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="primary" plain size="small" @click="handleView(scope.row)">查看</el-button>
            <el-button type="primary" size="small" @click="handleToggleStatus(scope.row)">
              {{ scope.row.status === "启用" ? "停用" : "启用" }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 12px; display: flex; justify-content: flex-end">
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

    <el-dialog v-model="data.formVisible" :title="data.form.id ? '编辑模型' : '新增模型'" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="rules" label-width="100px" style="padding: 10px 20px 0 10px">
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="data.form.modelName" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="data.form.apiUrl" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="data.form.apiKey" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="温度系数" prop="temperature">
              <el-input-number v-model="data.form.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="最大Token" prop="maxTokens">
              <el-input-number v-model="data.form.maxTokens" :min="1" :max="100000" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="状态" prop="status">
          <el-switch
              v-model="data.form.status"
              active-text="启用"
              inactive-text="停用"
              active-value="启用"
              inactive-value="停用"
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

    <el-dialog v-model="data.viewVisible" title="模型详情" width="700px" destroy-on-close>
      <div class="detail-item"><span class="detail-label">模型名称：</span>{{ data.viewData.modelName || "—" }}</div>
      <div class="detail-item"><span class="detail-label">API地址：</span>{{ data.viewData.apiUrl || "—" }}</div>
      <div class="detail-item"><span class="detail-label">温度系数：</span>{{ data.viewData.temperature ?? "—" }}</div>
      <div class="detail-item"><span class="detail-label">最大Token：</span>{{ data.viewData.maxTokens ?? "—" }}</div>
      <div class="detail-item"><span class="detail-label">状态：</span>{{ data.viewData.status || "—" }}</div>
      <div class="detail-item"><span class="detail-label">是否默认评估模型：</span>{{ data.viewData.id === data.defaultEvaluationModelId ? "是" : "否" }}</div>
      <div class="detail-item detail-key">
        <div class="detail-label">API Key：</div>
        <pre>{{ data.viewData.apiKey || "—" }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/utils/request.js";

const formRef = ref();
const currentUser = JSON.parse(localStorage.getItem("xm-user") || "{}");
const currentRole = currentUser.loginType === "USER" ? "USER" : currentUser.role;

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

const writeStoredEvaluationModel = (row) => {
  if (!row) {
    localStorage.removeItem(getEvaluationModelStorageKey());
    return;
  }
  localStorage.setItem(getEvaluationModelStorageKey(), JSON.stringify({
    id: row.id,
    modelName: row.modelName
  }));
};

const buildDefaultForm = () => ({
  id: null,
  modelName: "",
  apiUrl: "",
  apiKey: "",
  temperature: 0.7,
  maxTokens: 4096,
  status: "启用"
});

const data = reactive({
  searchForm: {
    modelName: "",
    status: ""
  },
  tableData: [],
  currentPage: 1,
  pageSize: 5,

  formVisible: false,
  viewVisible: false,

  form: buildDefaultForm(),
  viewData: {},
  defaultEvaluationModelId: null
});

const rules = {
  modelName: [{ required: true, message: "请输入模型名称", trigger: "blur" }],
  apiUrl: [{ required: true, message: "请输入API地址", trigger: "blur" }],
  apiKey: [{ required: true, message: "请输入API Key", trigger: "blur" }]
};

const pagedTableData = computed(() => {
  const start = (data.currentPage - 1) * data.pageSize;
  const end = start + data.pageSize;
  return data.tableData.slice(start, end);
});

const enabledModelOptions = computed(() => {
  return data.tableData.filter(item => item.status === "启用");
});

const syncDefaultEvaluationModel = () => {
  const options = enabledModelOptions.value || [];

  if (data.tableData.length === 0) {
    return;
  }

  const cached = readStoredEvaluationModel();
  const matched = options.find(item => String(item.id) === String(cached?.id));

  if (matched) {
    data.defaultEvaluationModelId = matched.id;
    return;
  }

  if (options.length > 0) {
    data.defaultEvaluationModelId = options[0].id;
    writeStoredEvaluationModel(options[0]);
    return;
  }

  data.defaultEvaluationModelId = null;
  writeStoredEvaluationModel(null);
};

const load = () => {
  request.get("/modelConfig/selectAll", {
    params: {
      modelName: data.searchForm.modelName,
      status: data.searchForm.status,
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    }
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      data.tableData = res.data || [];
      data.currentPage = 1;
      syncDefaultEvaluationModel();
    } else {
      ElMessage.error(res.msg || "加载失败");
    }
  }).catch(() => ElMessage.error("请求失败"));
};

const handleCurrentChange = (page) => {
  data.currentPage = page;
};

const resetSearch = () => {
  data.searchForm.modelName = "";
  data.searchForm.status = "";
  load();
};

const resetForm = () => {
  data.form = buildDefaultForm();
};

const handleAdd = () => {
  resetForm();
  data.formVisible = true;
};

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify({
    ...row,
    status: row.status || "停用"
  }));
  data.formVisible = true;
};

const handleView = (row) => {
  data.viewData = JSON.parse(JSON.stringify(row));
  data.viewVisible = true;
};

const saveDefaultEvaluationModel = () => {
  const selected = enabledModelOptions.value.find(item => item.id === data.defaultEvaluationModelId);
  if (!selected) {
    ElMessage.warning("请选择一个已启用模型作为默认评估模型");
    return;
  }
  writeStoredEvaluationModel(selected);
  ElMessage.success("默认评估模型已保存");
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除模型“${row.modelName}”吗？`, "删除确认", {
    type: "warning"
  }).then(() => {
    request.delete(`/modelConfig/delete/${row.id}`, {
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

const handleToggleStatus = (row) => {
  const nextStatus = row.status === "启用" ? "停用" : "启用";
  ElMessageBox.confirm(`确认${nextStatus}模型“${row.modelName}”吗？`, `${nextStatus}确认`, {
    type: "warning"
  }).then(() => {
    request.put("/modelConfig/update", {
      ...row,
      status: nextStatus,
      currentUserId: currentUser.id,
      currentUserRole: currentRole
    }).then(res => {
      if (res.code === "200" || res.code === 200) {
        ElMessage.success(`${nextStatus}成功`);
        load();
      } else {
        ElMessage.error(res.msg || `${nextStatus}失败`);
      }
    }).catch(() => ElMessage.error("请求失败"));
  }).catch(() => {});
};

const save = async () => {
  if (!formRef.value) return;

  await formRef.value.validate((valid) => {
    if (!valid) return;

    const url = data.form.id ? "/modelConfig/update" : "/modelConfig/add";
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

onMounted(() => {
  load();
});
</script>

<style scoped>
.model-page {
  min-height: calc(100vh - 84px);
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
}

.search-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.default-eval-bar {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #fafaf7;
  border: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.default-eval-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.default-eval-desc {
  color: #666;
  line-height: 1.6;
}

.default-eval-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-item {
  margin-bottom: 10px;
  word-break: break-all;
  line-height: 1.8;
}

.detail-label {
  font-weight: bold;
  color: #5a2d0c;
  margin-right: 6px;
}

.detail-key pre {
  margin: 8px 0 0;
  padding: 12px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.04);
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
}
</style>