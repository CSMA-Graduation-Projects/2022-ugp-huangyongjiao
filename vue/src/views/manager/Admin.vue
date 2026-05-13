<template>
  <div class="admin-page">
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

        <div v-if="user.role === 'SUPER_ADMIN'">
          <el-button type="primary" @click="handleAdd">新增管理员</el-button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="page-title">管理员信息</div>

      <el-table :data="pagedTableData" stripe style="width: 100%">
        <el-table-column label="编号" width="70" align="center">
          <template #default="scope">
            {{ (data.currentPage - 1) * data.pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="role" label="角色" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'SUPER_ADMIN' ? 'danger' : 'warning'" effect="light">
              {{ scope.row.role === "SUPER_ADMIN" ? "超级管理员" : "普通管理员" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" min-width="140" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="primary" plain size="small" @click="handleView(scope.row)">查看</el-button>
            <el-button
                v-if="user.role === 'SUPER_ADMIN' && scope.row.id !== user.id"
                type="danger"
                size="small"
                @click="handleDelete(scope.row)"
            >删除</el-button>
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

    <el-dialog v-model="data.formVisible" :title="data.form.id ? '编辑管理员' : '新增管理员'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="rules" label-width="90px" style="padding: 10px 20px 0 10px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="data.form.username" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="密码" v-if="!data.form.id" prop="password">
          <el-input v-model="data.form.password" show-password maxlength="50" show-word-limit placeholder="不填默认 123456" />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="data.form.name" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="data.form.role">
            <el-radio label="SUPER_ADMIN">超级管理员</el-radio>
            <el-radio label="ADMIN">普通管理员</el-radio>
          </el-radio-group>
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

    <el-dialog v-model="data.viewVisible" title="管理员详情" width="760px" destroy-on-close>
      <div class="detail-item"><span class="detail-label">用户名：</span>{{ data.viewData.username || "—" }}</div>
      <div class="detail-item"><span class="detail-label">姓名：</span>{{ data.viewData.name || "—" }}</div>
      <div class="detail-item"><span class="detail-label">角色：</span>{{ data.viewData.role === "SUPER_ADMIN" ? "超级管理员" : "普通管理员" }}</div>
      <div class="detail-item"><span class="detail-label">电话：</span>{{ data.viewData.phone || "—" }}</div>
      <div class="detail-item"><span class="detail-label">邮箱：</span>{{ data.viewData.email || "—" }}</div>

      <div style="margin-top: 18px;" v-if="data.managedUsers.length">
        <div class="sub-title">所属用户信息</div>
        <el-table :data="data.managedUsers" stripe style="width: 100%">
          <el-table-column label="编号" width="70" align="center">
            <template #default="scope">
              {{ scope.$index + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="name" label="姓名" min-width="120" />
          <el-table-column prop="phone" label="电话" min-width="140" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
        </el-table>
      </div>
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
  managedUsers: [],
  currentPage: 1,
  pageSize: 5,

  formVisible: false,
  viewVisible: false,

  form: {
    id: null,
    username: "",
    password: "",
    name: "",
    role: "ADMIN",
    phone: "",
    email: "",
  },

  viewData: {}
});

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  role: [{ required: true, message: "请选择角色", trigger: "change" }]
};

const pagedTableData = computed(() => {
  const start = (data.currentPage - 1) * data.pageSize;
  const end = start + data.pageSize;
  return data.tableData.slice(start, end);
});

const load = () => {
  request.get("/admin/selectAll", {
    params: {
      username: data.searchForm.username,
      name: data.searchForm.name,
      currentUserId: user.id,
      currentUserRole: user.role
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
    role: "ADMIN",
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
  request.get(`/user/selectByManagerId/${row.id}`).then(res => {
    data.managedUsers = (res.code === "200" || res.code === 200) ? (res.data || []) : [];
    data.viewVisible = true;
  }).catch(() => {
    data.managedUsers = [];
    data.viewVisible = true;
  });
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除管理员“${row.username}”吗？`, "删除确认", {
    type: "warning"
  }).then(() => {
    request.delete(`/admin/delete/${row.id}`, {
      params: {
        currentUserId: user.id,
        currentUserRole: user.role
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

    const url = data.form.id ? "/admin/update" : "/admin/add";
    const method = data.form.id ? request.put : request.post;

    const payload = {
      ...data.form,
      currentUserId: user.id,
      currentUserRole: user.role
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
.admin-page {
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

.sub-title {
  font-weight: bold;
  color: #5a2d0c;
  margin-bottom: 10px;
}
</style>