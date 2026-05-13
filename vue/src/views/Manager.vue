<template>
  <div class="manager-container">
    <div class="manager-header">
      <div class="manager-header-left">
        <img src="@/assets/imgs/logo.png" alt="">
        <div class="title">基于大语言模型的函数级代码测试用例生成系统</div>
      </div>

      <div class="manager-header-center">
        <el-breadcrumb separator="/" class="header-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/manager/home' }">
            首页
          </el-breadcrumb-item>
          <el-breadcrumb-item>
            {{ route.meta.name }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="manager-header-right">
        <div style="display: flex; align-items: center; gap: 10px;">
          <el-dropdown @command="handleManualCommand">
            <el-button class="header-theme-btn" plain>
              用户手册
              <el-icon style="margin-left: 6px;"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="view">在线查看</el-dropdown-item>
                <el-dropdown-item command="download">下载手册</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown @command="handleThemeCommand">
            <el-button class="header-theme-btn" plain>
              主题切换
              <el-icon style="margin-left: 6px;"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                    v-for="item in themeOptions"
                    :key="item.key"
                    :command="item.key"
                >
                  {{ item.label }}{{ currentTheme === item.key ? "（当前）" : "" }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <el-dropdown style="cursor: pointer" @command="handleUserCommand">
          <div class="header-user">
            <img
                style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover"
                :src="avatar"
                alt=""
            >
            <span class="header-user-name">
              {{ user.name || user.username || "未登录" }}
            </span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div style="display: flex">
      <div class="manager-main-left">
        <el-menu
            :default-active="route.path"
            :default-openeds="['1', '2', '3']"
            router
        >
          <el-menu-item index="/manager/home">
            <el-icon><HomeFilled /></el-icon>
            <span>系统首页</span>
          </el-menu-item>

          <el-sub-menu index="1">
            <template #title>
              <el-icon><Menu /></el-icon>
              <span>用例生成管理</span>
            </template>
            <el-menu-item index="/manager/generate">用例生成</el-menu-item>
            <el-menu-item index="/manager/batchExperiment">批量生成</el-menu-item>
            <el-menu-item index="/manager/history">生成记录</el-menu-item>
            <el-menu-item index="/manager/caseAnalysis">用例分析</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="2">
            <template #title>
              <el-icon><Menu /></el-icon>
              <span>基础信息管理</span>
            </template>
            <el-menu-item index="/manager/function">函数管理</el-menu-item>
            <el-menu-item index="/manager/requirement" class="center-menu-item">需求规格说明书管理</el-menu-item>
            <el-menu-item index="/manager/model">模型配置</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="3" v-if="user.loginType === 'ADMIN'">
            <template #title>
              <el-icon><Menu /></el-icon>
              <span>账号管理</span>
            </template>
            <el-menu-item v-if="user.role === 'SUPER_ADMIN'" index="/manager/admin">管理员信息</el-menu-item>
            <el-menu-item index="/manager/user">用户信息</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>

      <div class="manager-main-right">
        <RouterView />
      </div>
    </div>

    <el-dialog v-model="profileVisible" title="个人信息" width="520px" destroy-on-close>
      <el-form :model="profileForm" label-width="80px" style="padding: 10px 20px 0 10px">
        <el-form-item label="用户名">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="profileForm.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="profileForm.phone" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" maxlength="50" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="修改密码" width="520px" destroy-on-close>
      <el-form :model="passwordForm" label-width="90px" style="padding: 10px 20px 0 10px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="savePassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import "@/assets/css/manager.css";
import avatar from "@/assets/imgs/avatar.png";
import {reactive, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {ElMessage, ElMessageBox} from "element-plus";
import request from "@/utils/request.js";
import {THEME_OPTIONS, applyTheme, getCurrentThemeKey} from "@/utils/theme.js";

const router = useRouter();
const route = useRoute();

const user = reactive(JSON.parse(localStorage.getItem("xm-user") || "{}"));

const profileVisible = ref(false);
const passwordVisible = ref(false);

const currentTheme = ref(getCurrentThemeKey());
const themeOptions = THEME_OPTIONS;

const profileForm = reactive({
  id: null,
  username: "",
  name: "",
  phone: "",
  email: "",
  role: ""
});

const passwordForm = reactive({
  password: "",
  newPassword: "",
  confirmPassword: ""
});

const openProfile = () => {
  profileForm.id = user.id;
  profileForm.username = user.username || "";
  profileForm.name = user.name || "";
  profileForm.phone = user.phone || "";
  profileForm.email = user.email || "";
  profileForm.role = user.role || "";
  profileVisible.value = true;
};

const openPassword = () => {
  passwordForm.password = "";
  passwordForm.newPassword = "";
  passwordForm.confirmPassword = "";
  passwordVisible.value = true;
};

const saveProfile = () => {
  const url = user.loginType === "ADMIN" ? "/admin/update" : "/user/update";

  request.put(`${url}?currentUserId=${user.id}&currentUserRole=${user.role || user.loginType}`, profileForm).then(res => {
    if (res.code === "200" || res.code === 200) {
      Object.assign(user, profileForm);
      localStorage.setItem("xm-user", JSON.stringify(user));
      ElMessage.success("保存成功");
      profileVisible.value = false;
    } else {
      ElMessage.error(res.msg || "保存失败");
    }
  }).catch(() => {
    ElMessage.error("请求失败");
  });
};

const savePassword = () => {
  if (!passwordForm.password) {
    ElMessage.warning("请输入原密码");
    return;
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning("请输入新密码");
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning("两次输入的新密码不一致");
    return;
  }

  const url = user.loginType === "ADMIN" ? "/admin/updatePassword" : "/user/updatePassword";

  request.put(url, {
    username: user.username,
    password: passwordForm.password,
    newPassword: passwordForm.newPassword
  }).then(res => {
    if (res.code === "200" || res.code === 200) {
      ElMessage.success("密码修改成功，请重新登录");
      passwordVisible.value = false;
      localStorage.removeItem("xm-user");
      router.push("/login");
    } else {
      ElMessage.error(res.msg || "密码修改失败");
    }
  }).catch(() => {
    ElMessage.error("请求失败");
  });
};

const logout = () => {
  ElMessageBox.confirm("确定退出当前登录吗？", "退出确认", {
    type: "warning"
  }).then(() => {
    localStorage.removeItem("xm-user");
    router.push("/login");
  }).catch(() => {
  });
};

const handleManualCommand = (command) => {
  const manualUrl = `${import.meta.env.BASE_URL}user-manual.html`;

  if (command === "view") {
    window.open(manualUrl, "_blank");
    return;
  }

  if (command === "download") {
    const link = document.createElement("a");
    link.href = manualUrl;
    link.download = "用户手册.html";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
};

const handleThemeCommand = (themeKey) => {
  currentTheme.value = applyTheme(themeKey);
  ElMessage.success("主题切换成功");
};

const handleUserCommand = (command) => {
  if (command === "profile") openProfile();
  if (command === "password") openPassword();
  if (command === "logout") logout();
};
</script>
