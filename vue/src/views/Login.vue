<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-title">函数级代码测试用例生成系统</div>
      <div class="login-subtitle">欢迎登录</div>

      <el-form :model="data.form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="role">
          <el-radio-group v-model="data.form.role" style="width: 100%; display: flex; justify-content: center;">
            <el-radio label="ADMIN">管理员登录</el-radio>
            <el-radio label="USER">用户登录</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item prop="username">
          <el-input
              v-model="data.form.username"
              prefix-icon="User"
              placeholder="请输入用户名"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="data.form.password"
              prefix-icon="Lock"
              placeholder="请输入密码"
              show-password
              size="large"
              @keyup.enter="login"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" @click="login">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import request from "@/utils/request.js";

const router = useRouter();
const formRef = ref();

const data = reactive({
  form: {
    role: "ADMIN",
    username: "",
    password: ""
  }
});

const rules = {
  role: [{ required: true, message: "请选择登录身份", trigger: "change" }],
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }]
};

const login = async () => {
  if (!formRef.value) return;

  await formRef.value.validate((valid) => {
    if (!valid) return;

    const url = data.form.role === "ADMIN" ? "/admin/login" : "/user/login";

    request.post(url, data.form).then(res => {
      if (res.code === "200" || res.code === 200) {
        const loginUser = res.data || {};
        loginUser.loginType = data.form.role;
        localStorage.setItem("xm-user", JSON.stringify(loginUser));
        ElMessage.success("登录成功");
        router.push("/manager/home");
      } else {
        ElMessage.error(res.msg || "登录失败");
      }
    }).catch(() => {
      ElMessage.error("请求失败");
    });
  });
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, var(--theme-login-start) 0%, var(--theme-login-end) 100%);
  transition: background 0.25s ease;
}

.login-box {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 36px 32px 26px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, .15);
}

.login-title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  color: var(--theme-title-color);
  margin-bottom: 10px;
}

.login-subtitle {
  text-align: center;
  font-size: 15px;
  color: var(--theme-text-muted);
  margin-bottom: 28px;
}
</style>