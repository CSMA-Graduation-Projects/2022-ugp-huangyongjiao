import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", redirect: "/login" },
    { path: "/login", component: () => import("@/views/Login.vue") },

    {
      path: "/manager",
      component: () => import("@/views/Manager.vue"),
      children: [
        {
          path: "home",
          meta: { name: "系统首页" },
          component: () => import("@/views/manager/Home.vue"),
        },
        {
          path: "generate",
          meta: { name: "用例生成" },
          component: () => import("@/views/manager/Generate.vue"),
        },
        {
          path: "history",
          meta: { name: "生成记录" },
          component: () => import("@/views/manager/History.vue"),
        },
        {
          path: "caseAnalysis",
          meta: { name: "用例分析" },
          component: () => import("@/views/manager/CaseAnalysis.vue"),
        },
        {
          path: "batchExperiment",
          meta: { name: "批量生成" },
          component: () => import("@/views/manager/BatchExperiment.vue"),
        },
        {
          path: "function",
          meta: { name: "函数管理" },
          component: () => import("@/views/manager/FunctionInfo.vue"),
        },
        {
          path: "requirement",
          meta: { name: "需求规格说明书管理" },
          component: () => import("@/views/manager/RequirementSpec.vue"),
        },
        {
          path: "model",
          meta: { name: "模型配置" },
          component: () => import("@/views/manager/ModelConfig.vue"),
        },
        {
          path: "admin",
          meta: { name: "管理员信息" },
          component: () => import("@/views/manager/Admin.vue"),
        },
        {
          path: "user",
          meta: { name: "用户信息" },
          component: () => import("@/views/manager/User.vue"),
        },
      ],
    },

    { path: "/404", component: () => import("@/views/404.vue") },
    { path: "/:pathMatch(.*)", redirect: "/404" },
  ],
});

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem("xm-user") || "null");

  if (to.path === "/login") {
    next();
    return;
  }

  if (!user) {
    next("/login");
    return;
  }

  if (to.path === "/manager/admin" && user.loginType !== "ADMIN") {
    next("/manager/home");
    return;
  }

  if (to.path === "/manager/user" && user.loginType !== "ADMIN") {
    next("/manager/home");
    return;
  }

  next();
});

export default router;
