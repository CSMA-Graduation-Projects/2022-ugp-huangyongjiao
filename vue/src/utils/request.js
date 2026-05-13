import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL || 'http://localhost:7070',
    timeout: 120000
});

function appendUserParams(config) {
    const method = (config.method || "").toLowerCase();

    if (!["post", "put", "patch"].includes(method)) {
        return config;
    }

    let body = config.data;
    if (!body) {
        return config;
    }

    if (typeof body === "string") {
        try {
            body = JSON.parse(body);
        } catch (e) {
            return config;
        }
    }

    if (Object.prototype.toString.call(body) !== "[object Object]") {
        return config;
    }

    const { currentUserId, currentUserRole } = body;

    if (currentUserId == null && !currentUserRole) {
        return config;
    }

    config.params = {
        ...(config.params || {})
    };

    if (currentUserId != null && config.params.currentUserId == null) {
        config.params.currentUserId = currentUserId;
    }

    if (currentUserRole != null && config.params.currentUserRole == null) {
        config.params.currentUserRole = currentUserRole;
    }

    return config;
}

request.interceptors.request.use(
    config => {
        config.headers["Content-Type"] = "application/json;charset=utf-8";
        appendUserParams(config);
        return config;
    },
    error => Promise.reject(error)
);

request.interceptors.response.use(
    response => {
        let res = response.data;

        if (response.config.responseType === "blob") {
            return res;
        }

        if (typeof res === "string") {
            res = res ? JSON.parse(res) : res;
        }

        return res;
    },
    error => {
        const status = error?.response?.status;

        if (status === 404) {
            ElMessage.error("未找到请求接口");
        } else if (status === 500) {
            ElMessage.error("系统异常，请查看后端控制台报错");
        } else if (status) {
            ElMessage.error(`请求失败（${status}）`);
        } else {
            ElMessage.error("网络异常，请稍后重试");
        }

        return Promise.reject(error);
    }
);

export default request;