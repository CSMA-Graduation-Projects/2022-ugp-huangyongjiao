<template>
  <div class="user-page">
    <div class="card" style="margin-bottom: 10px;">
      <div class="toolbar">
        <div class="search-group">
          <el-input
              v-model="data.searchForm.username"
              placeholder="请输入用户名"
              clearable
              style="width: 220px"
              @keyup.enter="load"
          />
          <el-input
              v-model="data.searchForm.name"
              placeholder="请输入姓名"
              clearable
              style="width: 220px"
              @keyup.enter="load"
          />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="primary" plain @click="resetSearch">重置</el-button>
        </div>

        <div>
          <el-button type="primary" @click="handleAdd">新增用户</el-button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="page-title">用户信息</div>

      <el-table :data="pagedTableData" stripe style="width: 100%">
        <el-table-column label="编号" width="70" align="center">
          <template #default="scope">
            {{ (data.currentPage - 1) * data.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" min-width="140" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="所属管理员" min-width="140" align="center" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.managerName || "—" }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="primary" plain size="small" @click="handleView(scope.row)">查看</el-button>
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

    <el-dialog v-model="data.formVisible" :title="data.form.id ? '编辑用户' : '新增用户'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="rules" label-width="100px" style="padding: 10px 20px 0 10px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="data.form.username" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="密码" v-if="!data.form.id" prop="password">
          <el-input v-model="data.form.password" show-password maxlength="50" show-word-limit placeholder="不填默认 123456" />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="data.form.name" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="所属管理员" prop="managerId" v-if="user.role === 'SUPER_ADMIN'">
          <el-select v-model="data.form.managerId" placeholder="请选择所属管理员" style="width: 100%">
            <el-option
                v-for="item in data.managerOptions"
                :key="item.id"
                :label="`${item.name || item.username}（${item.username}）`"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="电话">
          <el-input v-model="data.form.phone" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="邮箱">
          <el-input v-model="data.form.email" maxlength="50" show-word-limit />
        </el-form-item>

      </el-form>

      <template #footer>
        <el-button type="primary" plain @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="data.viewVisible" title="用户详情" width="520px" destroy-on-close>
      <div class="detail-item"><span class="detail-label">用户名：</span>{{ data.viewData.username || "—" }}</div>
      <div class="detail-item"><span class="detail-label">姓名：</span>{{ data.viewData.name || "—" }}</div>
      <div class="detail-item"><span class="detail-label">电话：</span>{{ data.viewData.phone || "—" }}</div>
      <div class="detail-item"><span class="detail-label">邮箱：</span>{{ data.viewData.email || "—" }}</div>
      <div class="detail-item"><span class="detail-label">所属管理员：</span>{{ data.viewData.managerName || "—" }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/utils/request.js";

const formRef = ref();
const user = JSON.parse(localStorage.getItem("xm-user") || "{}");

const data = reactive({
  searchForm: {
    username: "",
    name: ""
  },
  tableData: [],
  managerOptions: [],
  currentPage: 1,
  pageSize: 5,

  formVisible: false,
  viewVisible: false,

  form: {
    id: null,
    username: "",
    password: "",
    name: "",
    managerId: null,
    phone: "",
    email: "",
  },

  viewData: {}
});

const rules = {
  username: [{required: true, message: "请输入用户名", trigger: "blur"}],
  name: [{required: true, message: "请输入姓名", trigger: "blur"}],
  managerId: [{required: true, message: "请选择所属管理员", trigger: "change"}]
};

const pagedTableData = computed(() => {
  const start = (data.currentPage - 1) * data.pageSize;
  const end = start + data.pageSize;
  return data.tableData.slice(start, end);
});

const load = () => {
  request.get("/user/selectAll", {
    params: {
      username: data.searchForm.username,
      name: data.searchForm.name,
      currentUserId: user.id,
      currentUserRole: user.role || user.loginType
    }
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      data.tableData = res.data || [];
      data.currentPage = 1;
    } else {
      ElMessage.error(res.msg || "加载失败");
    }
  }).catch(() => ElMessage.error("请求失败"));
};

const loadManagerOptions = () => {
  if (user.role !== "SUPER_ADMIN") return;
  request.get("/admin/selectManagerOptions").then(res => {
    if (res.code === "200" || res.code === 200) {
      data.managerOptions = res.data || [];
    }
  });
};

const handleCurrentChange = (page) => {
  data.currentPage = page;
};

const resetSearch = () => {
  data.searchForm.username = "";
  data.searchForm.name = "";
  load();
};

const resetForm = () => {
  data.form = {
    id: null,
    username: "",
    password: "",
    name: "",
    managerId: user.role === "SUPER_ADMIN" ? null : user.id,
    phone: "",
    email: "",
  };
};

const handleAdd = () => {
  resetForm();
  data.formVisible = true;
};

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row));
  data.form.password = "";
  data.formVisible = true;
};

const handleView = (row) => {
  data.viewData = JSON.parse(JSON.stringify(row));
  data.viewVisible = true;
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除用户“${row.username}”吗？`, "删除确认", {
    type: "warning"
  }).then(() => {
    request.delete(`/user/delete/${row.id}`, {
      params: {
        currentUserId: user.id,
        currentUserRole: user.role || user.loginType
      }
    }).then(res => {
      if (res.code === "200" || res.code === 200) {
        ElMessage.success("删除成功");
        load();
      } else {
        ElMessage.error(res.msg || "删除失败");
      }
    }).catch(() => ElMessage.error("请求失败"));
  }).catch(() => {
  });
};

const save = async () => {
  if (!formRef.value) return;

  await formRef.value.validate((valid) => {
    if (!valid) return;

    if (user.role === "ADMIN") {
      data.form.managerId = user.id;
    }

    const url = data.form.id ? "/user/update" : "/user/add";
    const method = data.form.id ? request.put : request.post;

    const payload = {
      ...data.form,
      currentUserId: user.id,
      currentUserRole: user.role || user.loginType
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
  loadManagerOptions();
});
</script>

<style scoped>
.user-page {
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
</style>