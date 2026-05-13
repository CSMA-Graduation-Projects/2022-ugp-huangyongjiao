export const THEME_STORAGE_KEY = "xm-system-theme";

export const THEME_OPTIONS = [
    { key: "amber", label: "棕黄配色" },
    { key: "blue", label: "蓝白配色" },
    { key: "green", label: "绿白配色" },
    { key: "purple", label: "紫白配色" }
];

export const DEFAULT_THEME = "amber";

const THEMES = {
    amber: {
        "--theme-page-bg": "#f5e3a1",
        "--theme-header-bg": "#5a2d0c",
        "--theme-sidebar-bg": "#ffc900",
        "--theme-menu-bg": "#513616",
        "--theme-menu-sub-bg": "#724615",
        "--theme-menu-hover-bg": "#d9a05f",
        "--theme-menu-active-bg": "#ffb600",
        "--theme-card-bg": "#eece53",
        "--theme-card-shadow": "rgba(90, 45, 12, 0.25)",

        "--theme-title-color": "#3e2614",
        "--theme-text-primary": "#3d2a1d",
        "--theme-text-secondary": "#6c584a",
        "--theme-text-muted": "#888888",
        "--theme-link": "#8e512b",

        "--theme-hero-start": "#fff7ec",
        "--theme-hero-middle": "#f4dfbf",
        "--theme-hero-end": "#e7c18b",

        "--theme-accent": "#8e512b",
        "--theme-accent-soft": "rgba(142, 81, 43, 0.12)",
        "--theme-accent-border": "rgba(142, 81, 43, 0.25)",
        "--theme-accent-shadow": "rgba(142, 81, 43, 0.28)",

        "--theme-chart-line": "#8e512b",
        "--theme-chart-bar": "#c78b53",
        "--theme-chart-axis": "#777777",

        "--theme-login-start": "#f3d9a5",
        "--theme-login-end": "#8e512b",

        "--el-color-primary": "#8e512b",
        "--el-color-primary-light-3": "#ab7351",
        "--el-color-primary-light-5": "#c5987f",
        "--el-color-primary-light-7": "#ddc1b2",
        "--el-color-primary-light-8": "#e8d4ca",
        "--el-color-primary-light-9": "#f2e7e1",
        "--el-color-primary-dark-2": "#724124"
    },

    blue: {
        "--theme-page-bg": "#eaf3ff",
        "--theme-header-bg": "#1f4f8a",
        "--theme-sidebar-bg": "#d9ebff",
        "--theme-menu-bg": "#285d96",
        "--theme-menu-sub-bg": "#3d74b0",
        "--theme-menu-hover-bg": "#6ba3dd",
        "--theme-menu-active-bg": "#4a8fd8",
        "--theme-card-bg": "#ffffff",
        "--theme-card-shadow": "rgba(31, 79, 138, 0.18)",

        "--theme-title-color": "#15365e",
        "--theme-text-primary": "#1b3d66",
        "--theme-text-secondary": "#5d7490",
        "--theme-text-muted": "#7b8da3",
        "--theme-link": "#2f73c9",

        "--theme-hero-start": "#f7fbff",
        "--theme-hero-middle": "#e4f0ff",
        "--theme-hero-end": "#cfe3ff",

        "--theme-accent": "#2f73c9",
        "--theme-accent-soft": "rgba(47, 115, 201, 0.12)",
        "--theme-accent-border": "rgba(47, 115, 201, 0.25)",
        "--theme-accent-shadow": "rgba(47, 115, 201, 0.24)",

        "--theme-chart-line": "#2f73c9",
        "--theme-chart-bar": "#7fb2f0",
        "--theme-chart-axis": "#738399",

        "--theme-login-start": "#dfeeff",
        "--theme-login-end": "#2f73c9",

        "--el-color-primary": "#2f73c9",
        "--el-color-primary-light-3": "#5a92d7",
        "--el-color-primary-light-5": "#82b0e5",
        "--el-color-primary-light-7": "#b2d0f1",
        "--el-color-primary-light-8": "#c9def6",
        "--el-color-primary-light-9": "#e0ecfb",
        "--el-color-primary-dark-2": "#265ca1"
    },

    green: {
        "--theme-page-bg": "#e8f6ec",
        "--theme-header-bg": "#1f6b4b",
        "--theme-sidebar-bg": "#d3f0db",
        "--theme-menu-bg": "#2f7b58",
        "--theme-menu-sub-bg": "#43916c",
        "--theme-menu-hover-bg": "#7ac9a1",
        "--theme-menu-active-bg": "#55ad7d",
        "--theme-card-bg": "#ffffff",
        "--theme-card-shadow": "rgba(31, 107, 75, 0.18)",

        "--theme-title-color": "#184b35",
        "--theme-text-primary": "#1f5a41",
        "--theme-text-secondary": "#607d6f",
        "--theme-text-muted": "#809486",
        "--theme-link": "#2f8f5b",

        "--theme-hero-start": "#f6fcf8",
        "--theme-hero-middle": "#e4f7ea",
        "--theme-hero-end": "#cdeed8",

        "--theme-accent": "#2f8f5b",
        "--theme-accent-soft": "rgba(47, 143, 91, 0.12)",
        "--theme-accent-border": "rgba(47, 143, 91, 0.25)",
        "--theme-accent-shadow": "rgba(47, 143, 91, 0.24)",

        "--theme-chart-line": "#2f8f5b",
        "--theme-chart-bar": "#79c79a",
        "--theme-chart-axis": "#73877c",

        "--theme-login-start": "#dff4e6",
        "--theme-login-end": "#2f8f5b",

        "--el-color-primary": "#2f8f5b",
        "--el-color-primary-light-3": "#59a77b",
        "--el-color-primary-light-5": "#84bf9d",
        "--el-color-primary-light-7": "#b4dbc2",
        "--el-color-primary-light-8": "#cae7d3",
        "--el-color-primary-light-9": "#e0f3e5",
        "--el-color-primary-dark-2": "#267249"
    },

    purple: {
        "--theme-page-bg": "#f6f3ff",
        "--theme-header-bg": "#6f52c8",
        "--theme-sidebar-bg": "#ece5ff",
        "--theme-menu-bg": "#7b61d9",
        "--theme-menu-sub-bg": "#9078ea",
        "--theme-menu-hover-bg": "#b09cf5",
        "--theme-menu-active-bg": "#8c71f0",
        "--theme-card-bg": "#ffffff",
        "--theme-card-shadow": "rgba(111, 82, 200, 0.18)",

        "--theme-title-color": "#5738b6",
        "--theme-text-primary": "#5e43a8",
        "--theme-text-secondary": "#786b9e",
        "--theme-text-muted": "#968db0",
        "--theme-link": "#7a5af8",

        "--theme-hero-start": "#fbf9ff",
        "--theme-hero-middle": "#f1ecff",
        "--theme-hero-end": "#e4d9ff",

        "--theme-accent": "#7a5af8",
        "--theme-accent-soft": "rgba(122, 90, 248, 0.12)",
        "--theme-accent-border": "rgba(122, 90, 248, 0.25)",
        "--theme-accent-shadow": "rgba(122, 90, 248, 0.24)",

        "--theme-chart-line": "#7a5af8",
        "--theme-chart-bar": "#b197fc",
        "--theme-chart-axis": "#8a82a3",

        "--theme-login-start": "#efe8ff",
        "--theme-login-end": "#7a5af8",

        "--el-color-primary": "#7a5af8",
        "--el-color-primary-light-3": "#967bfa",
        "--el-color-primary-light-5": "#b39dfb",
        "--el-color-primary-light-7": "#d1c2fd",
        "--el-color-primary-light-8": "#e0d7fe",
        "--el-color-primary-light-9": "#f0ebff",
        "--el-color-primary-dark-2": "#5f45d4"
    }
};

export function getCurrentThemeKey() {
    const saved = localStorage.getItem(THEME_STORAGE_KEY);
    return THEMES[saved] ? saved : DEFAULT_THEME;
}

export function applyTheme(themeKey, persist = true) {
    const finalKey = THEMES[themeKey] ? themeKey : DEFAULT_THEME;
    const themeVars = THEMES[finalKey];
    const root = document.documentElement;

    Object.entries(themeVars).forEach(([key, value]) => {
        root.style.setProperty(key, value);
    });

    root.setAttribute("data-theme", finalKey);

    if (persist) {
        localStorage.setItem(THEME_STORAGE_KEY, finalKey);
    }

    return finalKey;
}

export function initTheme() {
    const themeKey = getCurrentThemeKey();
    applyTheme(themeKey, false);
}