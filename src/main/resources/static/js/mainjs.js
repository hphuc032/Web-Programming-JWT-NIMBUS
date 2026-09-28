const api = {
    token() { return localStorage.getItem("token"); },
    async request(url, options = {}) {
        const headers = {"Content-Type": "application/json", ...(options.headers || {})};
        if (options.auth && this.token()) headers.Authorization = `Bearer ${this.token()}`;
        const response = await fetch(url, {...options, headers});
        let body = null;
        try { body = await response.json(); } catch (_) { /* response may be empty */ }
        if (!response.ok) throw new Error(body?.message || `Yêu cầu thất bại (${response.status})`);
        return body;
    }
};

function setMessage(text, isError = false) {
    const element = document.getElementById("message");
    if (!element) return;
    element.textContent = text;
    element.className = `message mt-3 ${isError ? "text-danger" : "text-success"}`;
}

async function login(event) {
    event?.preventDefault();
    setMessage("Đang thắp nến và xác thực...");
    try {
        const response = await api.request("/auth/login", {method: "POST", body: JSON.stringify({
            email: document.getElementById("email").value,
            password: document.getElementById("password").value
        })});
        localStorage.setItem("token", response.token);
        window.location.href = "/user/profile";
    } catch (error) { setMessage(error.message, true); }
}

async function register(event) {
    event?.preventDefault();
    setMessage("Đang tạo tài khoản...");
    try {
        await api.request("/auth/signup", {method: "POST", body: JSON.stringify({
            fullName: document.getElementById("fullName").value,
            email: document.getElementById("email").value,
            password: document.getElementById("password").value
        })});
        setMessage("Tạo tài khoản thành công. Đang chuyển đến đăng nhập...");
        setTimeout(() => window.location.href = "/login", 900);
    } catch (error) { setMessage(error.message, true); }
}

async function loadProfile() {
    if (!api.token()) { window.location.replace("/login"); return; }
    try {
        const user = await api.request("/users/me", {method: "GET", auth: true});
        document.getElementById("profileName").textContent = user.fullName;
        document.getElementById("profileEmail").textContent = user.email;
        document.getElementById("profileRole").textContent = `ROLE_${user.role}`;
        document.getElementById("profileCreated").textContent = new Date(user.createdAt).toLocaleString("vi-VN");
        document.getElementById("avatar").textContent = user.fullName.split(/\s+/).map(word => word[0]).slice(-2).join("").toUpperCase();
    } catch (_) {
        localStorage.removeItem("token");
        window.location.replace("/login");
    }
}

function logout() { localStorage.removeItem("token"); window.location.href = "/login"; }
function updateNavigation() {
    document.querySelectorAll("[data-auth-only]").forEach(el => el.classList.toggle("d-none", !api.token()));
    document.querySelectorAll("[data-guest-only]").forEach(el => el.classList.toggle("d-none", !!api.token()));
}
document.addEventListener("DOMContentLoaded", updateNavigation);
