/* TLNT Admin · v3.0
       Lõi: tiện ích, gọi API, hộp xác nhận, popup, đăng nhập, bộ đăng ký tab.
       Quy ước: mỗi tính năng là một Tabs.add({...}) độc lập; dữ liệu chỉ đi qua api()/act();
       dữ liệu người chơi luôn dựng bằng textContent (h()) — không innerHTML. */
    "use strict";
    let token = sessionStorage.getItem("tlntToken") || "";
    const $ = id => document.getElementById(id);
    function h(tag, attrs, ...kids) {
      const el = document.createElement(tag);
      for (const [k, v] of Object.entries(attrs || {})) {
        if (k === "class") el.className = v;
        else if (k === "value") el.value = v;
        else if (k.startsWith("on")) el.addEventListener(k.slice(2), v);
        else if (v !== false && v != null) el.setAttribute(k, v === true ? "" : v);
      }
      for (const kid of kids.flat(Infinity)) if (kid != null && kid !== false) el.append(kid instanceof Node ? kid : String(kid));
      return el;
    }
    const fill = (tb, rows, emptyText) => tb.replaceChildren(...(rows.length ? rows :
      [h("tr", {}, h("td", { colspan: tb.closest("table").querySelectorAll("th").length, class: "empty mut", style: "text-align:center" }, emptyText || "Chưa có dữ liệu"))]));
    const num = n => (n == null ? "–" : Number(n).toLocaleString("vi-VN"));
    const pct = (a, b) => b > 0 ? (a * 100 / b).toFixed(1).replace(".", ",") + "%" : "0%";
    function dur(s) { const d = Math.floor(s / 86400), hh = Math.floor(s % 86400 / 3600), m = Math.floor(s % 3600 / 60); return (d ? d + "n " : "") + hh + "g " + m + "p"; }
    function toast(msg, err) { const t = $("toast"); t.textContent = msg; t.className = "show" + (err ? " err" : ""); clearTimeout(toast.t); toast.t = setTimeout(() => t.className = "", 3800); }
    const kv = (k, v) => [h("div", {}, k), h("div", {}, v == null || v === "" ? "–" : v)];
    const chip = (text, cls) => h("span", { class: "chip " + (cls || "") }, text);
    const ROLES = ["Người chơi", "GM", "Quản trị viên"];

    async function api(path, body) {
      const opt = { method: body ? "POST" : "GET", headers: { "X-TLNT-Admin": "1", "X-TLNT-Token": token } };
      if (body) { opt.headers["Content-Type"] = "application/json"; opt.body = JSON.stringify(body); }
      const res = await fetch("/api/" + path, opt);
      const data = await res.json().catch(() => ({ ok: false, error: "Phản hồi không hợp lệ" }));
      if (res.status === 401) { showLogin(); throw new Error("Cần đăng nhập"); }
      if (!res.ok || data.ok === false) throw new Error(data.error || ("HTTP " + res.status));
      return data;
    }
    function ask(text) {
      return new Promise(res => {
        const d = $("dlg"); $("dlgText").textContent = text; d.classList.add("show"); $("dlgYes").focus();
        const done = v => { d.classList.remove("show"); d.onkeydown = null; res(v); };
        $("dlgYes").onclick = () => done(true); $("dlgNo").onclick = () => done(false);
        d.onkeydown = e => { if (e.key === "Escape") { e.stopPropagation(); done(false); } };
      });
    }
    /* Gọi API thao tác: hỏi xác nhận (nếu có), xoay nút đang bấm, báo kết quả. Trả về kết quả hoặc null nếu lỗi / huỷ. */
    async function act(path, body, confirmText) {
      const btn = document.activeElement && document.activeElement.closest ? document.activeElement.closest(".btn") : null;
      if (confirmText && !(await ask(confirmText))) return null;
      if (btn) btn.classList.add("busy");
      try { const r = await api(path, body); toast(r.message || "Xong."); return r; }
      catch (e) { toast(e.message, true); return null; }
      finally { if (btn) btn.classList.remove("busy"); }
    }

    /* Popup: modal(tiêu đề, nội dung, { wide, actions:[{label, primary, danger, run}] }).
       run() trả về giá trị "falsy" thì popup giữ nguyên (vd lỗi), ngược lại tự đóng. Trả về hàm đóng. */
    function modal(title, body, opts = {}) {
      const back = h("div", { class: "mdl" });
      const box = h("div", { class: "mdl-box" + (opts.wide ? " wide" : "") + (opts.item ? " item-modal" : ""), role: "dialog", "aria-modal": "true", "aria-label": title });
      const close = () => { back.remove(); removeEventListener("keydown", esc); if (opts.onClose) opts.onClose(); };
      const esc = e => { if (e.key === "Escape" && !$("dlg").classList.contains("show") && document.querySelector(".mdl:last-of-type") === back) close(); };
      box.append(h("div", { class: "mdl-h" }, h("h3", {}, title), h("button", { class: "btn small", onclick: close, "aria-label": "Đóng" }, "✕")), h("div", { class: "mdl-b" }, body));
      if (opts.actions && opts.actions.length) box.append(h("div", { class: "mdl-f" }, h("button", { class: "btn", onclick: close }, "Đóng"),
        opts.actions.map(a => h("button", { class: "btn" + (a.primary ? " primary" : "") + (a.danger ? " danger" : ""), onclick: async () => { if (await a.run()) close(); } }, a.label))));
      back.append(box); back.addEventListener("mousedown", e => { if (e.target === back) close(); });
      document.body.append(back); addEventListener("keydown", esc);
      const first = box.querySelector(".mdl-b input, .mdl-b select, .mdl-b textarea"); if (first && matchMedia("(pointer:fine)").matches) setTimeout(() => first.focus(), 30);
      return close;
    }
    /* Popup hỏi 1 giá trị: prompt2(tiêu đề, nhãn, giá trị, kiểu) -> Promise<string|null> */
    function prompt2(title, label, value, type) {
      return new Promise(res => {
        const inp = h("input", { type: type || "text", value: value == null ? "" : value, style: "width:100%" });
        let done = false;
        const close = modal(title, h("div", {}, h("label", { class: "lb" }, label), inp), {
          onClose: () => { if (!done) res(null); },
          actions: [{ label: "Đồng ý", primary: true, run: () => { done = true; res(inp.value); return true; } }]
        });
        inp.onkeydown = e => { if (e.key === "Enter") { done = true; res(inp.value); close(); } };
      });
    }

    /* ---------- Đăng nhập ---------- */
    function showLogin() { $("login").style.display = "flex"; $("loginPw").focus(); }
    $("loginBtn").onclick = async () => {
      try { const r = await api("login", { password: $("loginPw").value }); token = r.token; sessionStorage.setItem("tlntToken", token); $("login").style.display = "none"; Tabs.show(Tabs.start()); }
      catch (e) { $("loginErr").textContent = e.message; }
    };
    $("loginPw").onkeydown = e => { if (e.key === "Enter") $("loginBtn").click(); };
    const loginOpen = () => $("login").style.display === "flex";

    /* ---------- Trạng thái chung (thanh trên cùng) ---------- */
    const Admin = (() => {
      const subs = []; let statusRequest = null;
      async function refreshStatus() {
        if (statusRequest) return statusRequest;
        statusRequest = (async () => {
          const s = await api("status");
          $("hdrStatus").classList.remove("is-error");
          $("hdrStatus").replaceChildren(h("b"), h("span", {}, `Online ${num(s.inWorld)}${s.bots ? " (+" + num(s.bots) + " bot)" : ""} · CPU ${s.cpuProcess < 0 ? "?" : s.cpuProcess + "%"} · RAM ${num(s.ramUsedMb)}/${num(s.ramMaxMb)} MB · chạy ${dur(s.uptimeSeconds)}`));
          $("hdrMaint").hidden = !s.maintenance;
          $("hdrShutdown").hidden = s.shutdownInSeconds < 0;
          $("hdrShutdown").textContent = "Tắt server sau " + s.shutdownInSeconds + " giây";
          subs.forEach(f => f(s));
          return s;
        })();
        try { return await statusRequest; } finally { statusRequest = null; }
      }
      const fail = e => {
        $("hdrStatus").classList.add("is-error");
        $("hdrStatus").replaceChildren(h("b", { style: "background:#ff7a66" }), h("span", {}, "Mất kết nối: " + e.message));
      };
      return { refreshStatus, fail, onStatus: f => subs.push(f) };
    })();

    /* ---------- Bộ đăng ký tab ----------
       Tabs.add({ id, group, label, html, poll?, setup(root) -> { refresh?() } }); Tabs.go(id, arg) mở tab + gọi open(arg). */
    const ICONS = {
      dash: '<path d="M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z"/>',
      accounts: '<rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="11" r="2.5"/><path d="M5.5 16.5c.6-1.6 2-2.5 3.5-2.5s2.9.9 3.5 2.5M14 10h4M14 13.5h4"/>',
      players: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6M16 4.5a3.5 3.5 0 010 7M18 14.5c2 .6 3.5 2.4 3.5 5.5"/>',
      notify: '<path d="M3 11v3a1 1 0 001 1h2l5 4V6L6 10H4a1 1 0 00-1 1zM15 9a4 4 0 010 6M18 6.5a8 8 0 010 11"/>',
      events: '<path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 01-10 0zM17 5h3v2a3 3 0 01-3 3M7 5H4v2a3 3 0 003 3"/>',
      codes: '<rect x="3" y="8" width="18" height="4"/><path d="M12 8v13M5 12v9h14v-9M12 8c-2-4-6-3-5 0 .6 1.5 3 1 5 0zM12 8c2-4 6-3 5 0-.6 1.5-3 1-5 0z"/>',
      logs: '<path d="M5 3h14v18H5zM9 8h6M9 12h6M9 16h4"/>',
      globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/>',
      trend: '<path d="M3 17l6-6 4 4 8-8M15 7h6v6"/>',
      user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-7 8-7s8 3 8 7"/>',
      boss: '<path d="M5 5 8 3l4 3 4-3 3 2-1 5 3 4-3 1-2 6h-8l-2-6-3-1 3-4z"/><path d="M9 11h.01M15 11h.01M10 15h4"/>',
      data: '<ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v7c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 12v7c0 1.7 3.6 3 8 3s8-1.3 8-3v-7"/>',
      search: '<circle cx="10.8" cy="10.8" r="6.8"/><path d="m16 16 5 5"/>',
      tribe: '<path d="M12 21s8-4 8-11V5l-8-3-8 3v5c0 7 8 11 8 11z"/><path d="M8 12h8M12 8v8"/>',
      server: '<rect x="3" y="3" width="18" height="8" rx="2"/><rect x="3" y="13" width="18" height="8" rx="2"/><path d="M7 7h.01M7 17h.01M11 7h6M11 17h6"/>',
      shield: '<path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11z"/><path d="m9 12 2 2 4-4"/>',
      market: '<path d="M3 9h18l-2 12H5L3 9zM5 9l2-6h10l2 6"/><path d="M9 13v4M15 13v4"/>'
    };
    const svgIcon = k => { const s = h("span", { class: "ico", "aria-hidden": "true" }); s.innerHTML = '<svg viewBox="0 0 24 24">' + (ICONS[k] || '<circle cx="12" cy="12" r="3"/>') + '</svg>'; return s; };
    const Tabs = (() => {
      const list = []; let cur = null, timer = null;
      const add = def => list.push(def);
      const byId = id => list.find(t => t.id === id);
      function mount() {
        const groups = new Map();
        const app = $("app"), sideToggle = $("sideToggle"), navBackdrop = $("navBackdrop"), search = $("menuSearch");
        const setDrawer = open => { app.classList.toggle("open", open); $("menuBtn").setAttribute("aria-expanded", String(open)); };
        list.forEach(t => {
          const sec = h("section", { id: "tab-" + t.id }); $("view").append(sec); sec.innerHTML = t.html;
          t.api = (t.setup && t.setup(sec)) || {};
          t.btn = h("button", { type: "button", "data-tab": t.id, title: t.label, "aria-label": t.label, "data-search": (t.label + " " + t.id + " " + (t.group || "Khác")).toLocaleLowerCase("vi"), onclick: () => { location.hash = t.id; search.value = ""; filterNav(""); setDrawer(false); } }, svgIcon(t.icon || t.id), h("span", { class: "nav-label" }, t.label));
          const g = t.group || "Khác";
          if (!groups.has(g)) groups.set(g, h("details", { open: true }, h("summary", {}, g)));
          groups.get(g).append(t.btn);
        });
        $("nav").replaceChildren(...groups.values());
        $("menuBtn").onclick = () => setDrawer(!app.classList.contains("open"));
        try { app.classList.toggle("compact", localStorage.getItem("tlntAdminSidebarCollapsed") === "1"); } catch (_) { }
        const updateSideControl = () => {
          const mobile = innerWidth <= 1100, compact = app.classList.contains("compact");
          sideToggle.textContent = mobile ? "×" : compact ? "›" : "‹";
          sideToggle.title = mobile ? "Đóng menu" : compact ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng";
          sideToggle.setAttribute("aria-label", sideToggle.title);
        };
        updateSideControl();
        addEventListener("resize", updateSideControl);
        sideToggle.onclick = () => {
          if (innerWidth <= 1100) { setDrawer(false); return; }
          const compact = app.classList.toggle("compact");
          updateSideControl();
          try { localStorage.setItem("tlntAdminSidebarCollapsed", compact ? "1" : "0"); } catch (_) { }
        };
        navBackdrop.onclick = () => setDrawer(false);
        $("logout").onclick = () => { sessionStorage.removeItem("tlntToken"); token = ""; location.reload(); };
        function filterNav(value) {
          const q = value.trim().toLocaleLowerCase("vi");
          list.forEach(t => { t.btn.hidden = !!q && !t.btn.dataset.search.includes(q); });
          groups.forEach(d => {
            d.hidden = ![...d.querySelectorAll("button")].some(b => !b.hidden);
            if (q) d.open = true;
          });
          $("navEmpty").hidden = !q || list.some(t => !t.btn.hidden);
        }
        search.oninput = () => filterNav(search.value);
        search.onkeydown = e => {
          const matches = list.filter(t => !t.btn.hidden);
          if (e.key === "ArrowDown" || e.key === "ArrowUp") { e.preventDefault(); (e.key === "ArrowUp" ? matches[matches.length - 1] : search.value.trim() ? matches[0] : cur)?.btn.focus(); }
          else if (e.key === "Enter") { e.preventDefault(); (search.value ? matches[0] : cur)?.btn.click(); search.blur(); }
          else if (e.key === "Escape") { search.value = ""; filterNav(""); search.blur(); }
        };
        $("nav").onkeydown = e => {
          if (e.key !== "ArrowDown" && e.key !== "ArrowUp") return;
          const matches = list.filter(t => !t.btn.hidden), at = matches.findIndex(t => t.btn === document.activeElement);
          if (!matches.length) return;
          e.preventDefault(); matches[(at + (e.key === "ArrowDown" ? 1 : matches.length - 1)) % matches.length].btn.focus();
        };
        addEventListener("keydown", e => {
          if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
            e.preventDefault();
            if (innerWidth > 1100) { app.classList.remove("compact"); updateSideControl(); } else setDrawer(true);
            search.focus();
          } else if (e.key === "Escape" && app.classList.contains("open")) setDrawer(false);
        });
        addEventListener("hashchange", () => show(start()));
      }
      const start = () => (byId(location.hash.slice(1).split("/")[0]) || list[0]).id;
      async function refreshTab(tab) {
        if (!tab || !tab.api.refresh || tab.refreshing) return;
        tab.refreshing = true;
        try { await tab.api.refresh(); } finally { tab.refreshing = false; }
      }
      async function refresh() {
        try { await Admin.refreshStatus(); await refreshTab(cur); } catch (e) { Admin.fail(e); }
      }
      function show(id) {
        cur = byId(id);
        list.forEach(t => {
          t.btn.classList.toggle("active", t === cur); $("tab-" + t.id).classList.toggle("active", t === cur);
          if (t === cur) t.btn.setAttribute("aria-current", "page"); else t.btn.removeAttribute("aria-current");
        });
        const section = cur.btn.closest("details"); if (section) section.open = true;
        $("pageTitle").replaceChildren(h("span", { class: "mut" }, (cur.group || "Khác") + "  /  "), h("strong", {}, cur.label));
        document.title = cur.label + " · TLNT Admin";
        clearInterval(timer); refresh();
        const tab = cur;
        if (tab.poll && tab.api.refresh) timer = setInterval(() => {
          if (cur === tab && document.visibilityState === "visible" && !loginOpen() && !document.querySelector(".mdl")) refreshTab(tab).catch(() => { });
        }, tab.poll);
      }
      /* Mở tab khác và gọi open(arg) của tab đó (vd từ tài khoản sang nhân vật). */
      function go(id, arg) { const t = byId(id); if (location.hash.slice(1) !== id) location.hash = id; else show(id); if (t.api.open) t.api.open(arg); }
      const currentId = () => cur ? cur.id : null;
      return { add, mount, show, refresh, start, go, currentId };
    })();

/* ------------------------------------------------------------------ */

/* ================= Thành phần dùng chung ================= */
    const fmtPct = v => v <= 0 ? "0%" : v < 10 ? v.toFixed(1).replace(".", ",") + "%" : Math.round(v) + "%";
    const fmtMem = m => m >= 1024 ? (m / 1024).toFixed(1).replace(".", ",") + " GB" : num(m) + " MB";

    /* Vòng tròn phần trăm (CPU, RAM). Đổi màu khi >= 60% và >= 85%. */
    function donut(label, p, text, sub) {
      const c = 2 * Math.PI * 42, v = Math.max(0, Math.min(100, p)), col = v >= 85 ? "#c8402b" : v >= 60 ? "#f2a900" : "#0f4c4a";
      const d = h("div", { class: "donut" });
      d.innerHTML = '<svg viewBox="0 0 100 100"><circle cx="50" cy="50" r="42" fill="none" stroke="#e3ebe8" stroke-width="10"/><circle cx="50" cy="50" r="42" fill="none" stroke="' + col + '" stroke-width="10" stroke-linecap="round" stroke-dasharray="' + (c * v / 100) + " " + c + '" transform="rotate(-90 50 50)"/><text x="50" y="56" text-anchor="middle" font-size="19" font-weight="800" fill="#12302f">' + text + "</text></svg>";
      d.append(h("b", {}, label), h("small", {}, sub || ""));
      return d;
    }
    /* Thanh ngang: [[nhãn, giá trị], ...]; tỉ lệ theo tổng (hiện %) hoặc theo giá trị lớn nhất. */
    function hbars(rows, { color, total, onClick } = {}) {
      const sum = total || rows.reduce((a, r) => a + Number(r[1]), 0), mx = Math.max(1, ...rows.map(r => Number(r[1])));
      if (!rows.length) return h("p", { class: "mut" }, "Chưa có dữ liệu");
      return h("div", { class: "bars" }, rows.map(r => h("div", { class: "bar", style: onClick ? "cursor:pointer" : "", onclick: onClick ? () => onClick(r) : null },
        h("span", { title: r[0] }, r[0]), h("div", { class: "t" }, h("i", { style: `width:${Math.max(1.5, Number(r[1]) * 100 / mx)}%;background:${color || "var(--teal)"}` })),
        h("b", {}, num(r[1]), h("small", { class: "mut", style: "font-weight:400" }, " " + pct(Number(r[1]), sum))))));
    }
    /* Biểu đồ cột nhóm (SVG): labels[], series [{label, color, values[]}]. Rê chuột xem số. */
    function vchart(labels, series) {
      const ns = "http://www.w3.org/2000/svg", W = 720, H = 210, L = 34, R = 6, T = 10, B = 24;
      const svg = document.createElementNS(ns, "svg"); svg.setAttribute("viewBox", `0 0 ${W} ${H}`); svg.setAttribute("class", "vchart"); svg.setAttribute("preserveAspectRatio", "none");
      const el = (tag, a, txt) => { const e = document.createElementNS(ns, tag); for (const k in a) e.setAttribute(k, a[k]); if (txt != null) e.textContent = txt; svg.append(e); return e; };
      const mxRaw = Math.max(1, ...series.flatMap(s => s.values)), p = Math.pow(10, Math.floor(Math.log10(mxRaw))), mx = [1, 2, 5, 10].map(m => m * p).find(x => x >= mxRaw);
      const y = v => T + (H - T - B) * (1 - v / mx), slot = (W - L - R) / Math.max(1, labels.length), bw = Math.min(14, (slot - 6) / series.length);
      for (let k = 0; k <= 4; k++) { const v = mx * k / 4; el("line", { x1: L, x2: W - R, y1: y(v), y2: y(v), stroke: "#e3ebe8" }); el("text", { x: L - 5, y: y(v) + 3, "text-anchor": "end" }, num(Math.round(v))); }
      labels.forEach((lab, i) => {
        const x0 = L + slot * i + (slot - bw * series.length) / 2;
        series.forEach((s, j) => { const v = s.values[i] || 0; const r = el("rect", { x: x0 + j * bw, y: y(v), width: Math.max(1, bw - 1.5), height: Math.max(0, H - B - y(v)), fill: s.color, rx: 2 }); const t = document.createElementNS(ns, "title"); t.textContent = lab + " · " + s.label + ": " + num(v); r.append(t); });
        if (labels.length <= 16 || i % 2 === 0) el("text", { x: L + slot * i + slot / 2, y: H - 7, "text-anchor": "middle" }, lab);
      });
      return svg;
    }
    const legend = series => h("div", { class: "legend" }, series.map(s => h("span", {}, h("i", { style: "background:" + s.color }), s.label)));

    /* ----- Ảnh client (Assest): /asset/icon/N.png, /asset/ui/N.png ----- */
    const ASSET = p => "/asset/" + p + ".png";
    /* Icon item thu nhỏ (danh sách, thẻ); thiếu ảnh thì ẩn. */
    function ico(path, size) {
      if (!path) return "";
      const im = h("img", { class: "ico-sm", src: ASSET(path), alt: "", loading: "lazy", style: size ? `width:${size}px;height:${size}px` : "" });
      im.onerror = () => im.remove();
      return im;
    }
    /* Số trên ô như client (GameUtils.formatCompactNumber): dưới 10 triệu viết đủ, lớn hơn thì k / m / b / t. */
    function compact(v) {
      if (v < 1e7) return num(v);
      const u = ["", "k", "m", "b", "t"], i = Math.min(4, Math.floor(Math.log10(v) / 3)), x = Math.floor(v / Math.pow(1000, i) * 10) / 10;
      return String(x).replace(".", ",") + u[i];
    }

    /* ----- Item: thẻ thông tin như cửa sổ thông tin item của client (ItemInfoPopup) ----- */
    function itemCard(it) {
      const card = h("div", { class: "icard" });
      card.append(h("div", { class: "ihead" }, h("div", { class: "islot" }, slotFace(it)),
        h("div", {}, h("div", { class: "nm" }, it.name + (it.stars ? " +" + it.stars : "")), it.gradeName ? h("div", { class: "gr" }, "(" + it.gradeName + ")") : "",
          it.type ? h("div", { class: "ty" }, it.type) : "")));
      if (it.description) card.append(h("div", { class: "ds" }, it.description));
      if (it.requiredClass) card.append(h("div", {}, "Dành cho: " + it.requiredClass));
      if (it.requiredLevel != null) card.append(h("div", {}, "Cấp độ yêu cầu: " + it.requiredLevel));
      if (it.expire) card.append(h("div", { class: it.expired ? "bad" : "" }, "Hạn sử dụng: " + it.expire + (it.expired ? " (đã hết hạn)" : "")));
      (it.lines || []).forEach(l => card.append(h("div", { class: "ln" }, l)));
      if (it.charms && it.charms.length) {
        card.append(h("div", { class: "sec" }, "Bùa linh hồn (" + it.charms.length + ")"));
        it.charms.forEach((c, idx) => card.append(h("div", { class: "sub-row charm-line" },
          c.icon ? ico(c.icon, 24) : h("span", { class: "chip sm" }, "Lỗ " + (idx + 1)),
          h("span", {}, h("b", {}, c.name), c.lines && c.lines.length ? ": " + c.lines.join(", ") : ""))));
      }
      if (it.stones && it.stones.length) {
        card.append(h("div", { class: "sec" }, "Đá nguyên tố (" + it.stones.length + ")"));
        it.stones.forEach(st => card.append(h("div", { class: "sub-row stone-line" },
          st.icon ? ico(st.icon, 24) : h("span", { class: "chip sm warn" }, "Lỗ " + st.hole),
          h("span", {}, h("b", {}, st.name), " (Cấp " + st.level + ") — ", h("span", { class: "ln" }, st.line)))));
      }
      if (it.buffMinutes) card.append(h("div", { class: "ln" }, "Hiệu ứng trong " + num(it.buffMinutes) + " phút"));
      const meta = ["ID " + it.id];
      if (it.qty != null) meta.push("SL " + num(it.qty));
      if (it.locked != null) meta.push(it.locked ? "Khoá" : "Không khoá");
      if (it.slot != null && it.slot >= 0) meta.push("Ô " + it.slot);
      if (it.holes) meta.push(it.holes + " lỗ khảm");
      if (it.created) meta.push("Tạo " + it.created);
      card.append(h("div", { class: "meta" }, meta.join(" · ")));
      return card;
    }
    /* Popup nổi khi rê chuột / focus vào ô item (chỉ máy có chuột). */
    const Pop = (() => {
      let el = null;
      function show(target, it) {
        if (!matchMedia("(hover:hover)").matches) return;
        hide(); el = h("div", { class: "pop" }, itemCard(it)); document.body.append(el);
        const r = target.getBoundingClientRect(), w = el.offsetWidth, hh = el.offsetHeight;
        let x = r.right + 8, y = r.top; if (x + w > innerWidth - 8) x = Math.max(8, r.left - w - 8); if (y + hh > innerHeight - 8) y = Math.max(8, innerHeight - hh - 8);
        el.style.left = x + "px"; el.style.top = y + "px";
      }
      function hide() { if (el) { el.remove(); el = null; } }
      addEventListener("scroll", hide, true);
      return { show, hide };
    })();
    /* Mặt 1 ô đồ như ItemSlotButton của client:
       nền khung ui/99 (9 mảnh) — có ấn thì thay bằng viền lửa động icon/500-515 (Nhật) / 526-541 (Nguyệt),
       icon item cỡ thật theo ô 104 px, số lượng góc dưới phải, sao cấp nâng icon/490+ / 516+ góc trên phải (chỉ đồ có ấn;
       đồ thường hiện "+N" nhỏ), ổ khoá ui/194 góc trên trái, ô trống hiện tên ô (đồ mặc). */
    function slotFace(it, label) {
      const parts = [];
      if (it && it.grade > 0) parts.push(h("span", { class: "glow g" + it.grade }));
      if (it) {
        const fb = h("span", { class: "fb" }, it.name);
        if (it.icon) {
          const im = h("img", { class: "ic", src: ASSET(it.icon), alt: it.name, loading: "lazy", draggable: "false" });
          fb.classList.add("hid");
          im.onload = () => { im.style.width = Math.min(160, im.naturalWidth / 1.04) + "%"; };
          im.onerror = () => { im.remove(); fb.classList.remove("hid"); };
          parts.push(im);
        }
        parts.push(fb);
        if (it.qty > 1) parts.push(h("span", { class: "q" }, compact(it.qty)));
        if (it.stars > 0 && it.grade > 0) parts.push(h("img", { class: "star", src: ASSET("icon/" + ((it.grade === 1 ? 490 : 516) + Math.min(9, it.stars - 1))), alt: "+" + it.stars }));
        else if (it.stars > 0) parts.push(h("span", { class: "plus" }, "+" + it.stars));
        if (it.locked) parts.push(h("img", { class: "lk", src: ASSET("ui/194"), alt: "khoá" }));
        if (it.expired) parts.push(h("span", { class: "xp" }, "Hết hạn"));
      } else if (label) parts.push(h("span", { class: "lb2" }, label));
      return parts;
    }
    /* Ô đồ bấm được (hành trang / rương / đồ mặc / thư / bộ sưu tập). */
    function tile(it, onClick, label) {
      if (!it) return h("div", { class: "slot" + (label ? "" : " empty") }, slotFace(null, label));
      const t = h("button", { class: "slot" + (it.grade > 0 ? " graded" : "") + (it.expired ? " exp" : ""), type: "button", "aria-label": it.name + (it.qty > 1 ? " x" + it.qty : "") }, slotFace(it, label));
      t.onmouseenter = () => Pop.show(t, it); t.onmouseleave = Pop.hide; t.onfocus = () => Pop.show(t, it); t.onblur = Pop.hide;
      t.onclick = () => { Pop.hide(); if (onClick) onClick(it); else modal(it.name, itemCard(it)); };
      return t;
    }
    /* Khung panel như client (ui/100, 9 mảnh) chứa lưới 5 cột như hành trang / rương; nút đáy dùng ảnh ui/102. */
    function bagPanel(slots, buttonText, onButton) {
      return h("div", { class: "bagp" }, h("div", { class: "bagp-in" }, h("div", { class: "bagp-grid" }, slots)),
        buttonText ? h("div", { class: "bagp-foot" }, h("button", { class: "cp-btn2", type: "button", onclick: onButton }, buttonText)) : "");
    }
    /* Bảng trang bị như EquipmentPaperdollPanel: 10 ô (chẵn bên trái, lẻ bên phải), vòng ui/101 ở giữa. */
    function paperdoll(body, p, onItem) {
      const TOP = [25, 139, 253, 367, 481];
      const cells = body.map(s => {
        const col = s.slot % 2, row = Math.floor(s.slot / 2);
        const el = tile(s.item, s.item ? it => onItem(it) : null, s.slotName);
        el.classList.add("abs"); el.style.left = (col ? 497 : 15) / 6.16 + "%"; el.style.top = TOP[row] / 6.10 + "%";
        return el;
      });
      return h("div", { class: "cp-wrap" }, h("div", { class: "cpanel doll" },
        h("img", { class: "ring", src: ASSET("ui/101"), alt: "" }),
        h("div", { class: "who" }, h("b", {}, p.name), h("span", {}, p.cls + " · " + p.gender), h("span", {}, "Cấp " + p.level + (p.title ? " · " + p.title : ""))),
        cells));
    }
    /* ----- Loại chỉ số (stat_type) ----- */
    let STATS = null;
    async function statTypes() { if (!STATS) { const list = await api("stat-types"); STATS = new Map(list.map(s => [s.id, s])); } return STATS; }
    const rng = v => Array.isArray(v) ? (v[0] === v[1] ? num(v[0]) : num(v[0]) + "–" + num(v[1])) : num(v);
    /* 1 dòng [loại, giá trị | [min,max]...] -> chữ như client. */
    function statLine(line) {
      const st = STATS && STATS.get(line[0]); const vals = line.slice(1);
      if (!st) return "Chỉ số #" + line[0] + " " + vals.map(rng).join(", ");
      return st.pattern.replace(/\{(\d+)\}/g, (_, k) => vals[k] != null ? rng(vals[k]) : "?");
    }

    /* ----- Tìm mẫu item ----- */
    function itemSearch(onPick, initial) {
      const inp = h("input", { placeholder: "Nhập tên hoặc ID item để tìm…", style: "width:100%", value: initial || "", autocomplete: "off", role: "combobox", "aria-expanded": "false", "aria-autocomplete": "list" });
      const drop = h("div", { class: "search-drop", role: "listbox", hidden: true }); let timer, seq = 0;
      const run = async () => {
        const query = inp.value.trim(), my = ++seq;
        if (!query) { drop.hidden = true; drop.replaceChildren(); inp.setAttribute("aria-expanded", "false"); return; }
        drop.hidden = false; inp.setAttribute("aria-expanded", "true");
        drop.replaceChildren(h("div", { class: "mut", style: "padding:8px 10px" }, "Tìm kiếm…"));
        let list;
        try { list = await api("items?q=" + encodeURIComponent(inp.value)); } catch (e) { return toast(e.message, true); }
        if (my !== seq) return;
        drop.replaceChildren(...(list.length ? list.slice(0, 20).map(t => h("button", { type: "button", role: "option", onclick: () => { onPick(t); inp.value = ""; drop.hidden = true; drop.replaceChildren(); inp.setAttribute("aria-expanded", "false"); } },
          ico(t.icon, 28), h("b", {}, "#" + t.id + " " + t.name), h("span", { class: "mut" }, " · " + (t.type || "?") + (t.requiredLevel ? " · cấp " + t.requiredLevel : "") + (t.requiredClass ? " · " + t.requiredClass : "")))) : [h("div", { class: "mut", style: "padding:8px 10px" }, "Không có item phù hợp")]));
      };
      inp.oninput = () => { clearTimeout(timer); timer = setTimeout(run, 220); };
      inp.onkeydown = e => { if (e.key === "Escape") { ++seq; drop.hidden = true; inp.setAttribute("aria-expanded", "false"); } };
      return h("div", {}, inp, drop);
    }

    /* ----- Bộ sửa dòng chỉ số ----- */
    function statsEditor(lines, allowRange) {
      const box = h("div"); const rows = [];
      function addRow(line) {
        const sel = h("select", {}, h("option", { value: "" }, "— chọn chỉ số —"), [...STATS.values()].map(s => h("option", { value: s.id }, "#" + s.id + " " + s.pattern)));
        const vals = h("span", { style: "display:contents" }); const row = h("div", { class: "stat-row" });
        const state = { sel, inputs: [] };
        function buildInputs(prev) {
          vals.replaceChildren(); state.inputs = [];
          const st = STATS.get(+sel.value); const n = st ? st.args : 0;
          for (let k = 0; k < n; k++) {
            const v = prev ? prev[k] : null, lo = Array.isArray(v) ? v[0] : v, hi = Array.isArray(v) ? v[1] : "";
            const a = h("input", { type: "number", placeholder: allowRange ? "từ" : "giá trị", value: lo == null ? "" : lo });
            const b = allowRange ? h("input", { type: "number", placeholder: "đến (tuỳ)", value: hi === lo ? "" : hi }) : null;
            state.inputs.push([a, b]); vals.append(a); if (b) vals.append(h("span", { class: "mut" }, "–"), b);
          }
          if (!n && st) vals.append(h("span", { class: "mut" }, "(dòng không có số)"));
        }
        sel.onchange = () => buildInputs(null);
        if (line) { sel.value = line[0]; buildInputs(line.slice(1)); }
        row.append(sel, vals, h("button", { class: "btn small", type: "button", onclick: () => { row.remove(); rows.splice(rows.indexOf(state), 1); } }, "Bỏ"));
        rows.push(state); box.insertBefore(row, addBtn);
      }
      const addBtn = h("button", { class: "btn small", type: "button", onclick: () => addRow(null) }, "+ Thêm dòng chỉ số");
      box.append(addBtn);
      (lines || []).forEach(addRow);
      return {
        el: box,
        get() {
          const out = [];
          for (const r of rows) {
            if (!r.sel.value) continue;
            const line = [+r.sel.value];
            for (const [a, b] of r.inputs) {
              if (a.value === "") throw new Error("Nhập đủ giá trị cho dòng " + statLine([+r.sel.value]));
              const lo = Math.trunc(+a.value), hi = b && b.value !== "" ? Math.trunc(+b.value) : lo;
              line.push(hi !== lo ? [Math.min(lo, hi), Math.max(lo, hi)] : lo);
            }
            out.push(line);
          }
          return out;
        }
      };
    }

    /* ----- Bộ sửa item (popup) -----
       mode "reward": món quà {"i","q","l","s","d"} (tặng thư, thêm vào túi, giftcode; chỉ số cho phép khoảng min–max)
       mode "loot":   dòng quà boss {"i","n","q","l","c","d","perm","s"}
       mode "edit":   sửa món đang có (it = dữ liệu item đầy đủ): số lượng, khoá, cấp nâng, ấn, hạn, chỉ số gốc
       Trả về Promise<object|null>. Khoá lạ trong value (vd "c"/"g" giftcode) được giữ nguyên.
       opts.extras = [{key, label, options?, def?, hint?, type?}] trường phụ (giá shop, tỉ lệ câu cá...);
       opts.lockTri = "chữ mặc định" -> ô khoá 3 trạng thái (Mặc định / Khoá / Không khoá; mặc định = không ghi "l"). */
    async function editItem(mode, value, title, opts = {}) {
      await statTypes();
      return new Promise(res => {
        let tpl = value && value.i ? { id: value.i, name: value.name || "#" + value.i, icon: value.icon } : mode === "edit" ? { id: value.id, name: value.name, icon: value.icon } : null;
        const v = value ? JSON.parse(JSON.stringify(value)) : {};
        const picked = h("div", { class: "box", style: "margin-top:8px" });
        const showPicked = () => picked.replaceChildren(tpl ? h("div", { class: "sub-row", style: "flex-wrap:wrap" }, tpl.icon ? ico(tpl.icon, 34) : "", h("span", { class: "chip ok" }, "#" + tpl.id), h("b", {}, tpl.name), tpl.type ? h("span", { class: "chip sm" }, tpl.type) : "", tpl.description ? h("div", { class: "mut", style: "font-size:12px;width:100%;margin-top:4px" }, tpl.description) : "") : h("span", { class: "mut" }, "Chưa chọn item"));
        showPicked();
        const range = x => Array.isArray(x) ? x : x == null ? [null, null] : [x, x];
        const pair = (x, def) => { const [a, b] = range(x); const i1 = h("input", { type: "number", value: a == null ? def : a, style: "width:110px" }), i2 = h("input", { type: "number", value: b == null || b === a ? "" : b, placeholder: "đến (tuỳ)", style: "width:110px" }); return { el: h("span", { class: "row", style: "margin:0" }, i1, h("span", { class: "mut" }, "–"), i2), get: () => { const lo = +i1.value, hi = i2.value === "" ? lo : +i2.value; return lo === hi ? lo : [Math.min(lo, hi), Math.max(lo, hi)]; } }; };
        const form = h("div", { class: "field" }); const F = {};
        let stats, X = [];
        if (mode === "edit") {
          const toLocal = ms => { if (!(ms > 0)) return ""; const d = new Date(ms); d.setMinutes(d.getMinutes() - d.getTimezoneOffset()); return d.toISOString().slice(0, 16); };
          F.q = h("input", { type: "number", min: 1, value: value.qty, style: "width:100%" });
          F.l = h("input", { type: "checkbox" }); F.l.checked = !!value.locked;
          F.star = h("input", { type: "number", min: 0, max: 10, value: value.stars || 0, style: "width:100%" });
          F.grade = h("select", { style: "width:100%" }, h("option", { value: 0 }, "Không ấn"), h("option", { value: 1 }, "Nhật ấn (Cấp 1)"), h("option", { value: 2 }, "Nguyệt ấn (Cấp 2)")); F.grade.value = value.grade || 0;
          F.perm = h("input", { type: "checkbox" }); F.perm.checked = !(value.expireAt > 0);
          F.exp = h("input", { type: "datetime-local", value: toLocal(value.expireAt), style: "width:100%" }); F.exp.disabled = F.perm.checked; F.perm.onchange = () => F.exp.disabled = F.perm.checked;
          F.cc = h("input", { type: "checkbox" }); F.cs = h("input", { type: "checkbox" });
          stats = statsEditor(value.stats, false);
          form.className = "";
          form.append(
            h("div", { class: "sect", style: "margin-bottom:10px" },
              h("div", { class: "sect-h" }, h("h4", {}, "Thông tin vật phẩm"), chip("ID #" + value.id, "ok")),
              h("div", { class: "sub-row", style: "font-size:15px;font-weight:700;margin-bottom:10px" },
                value.icon ? ico(value.icon, 34) : "",
                h("span", {}, value.name)
              ),
              h("div", { class: "edit-item-grid" },
                h("div", {}, h("label", { class: "lb" }, "Số lượng"), F.q),
                h("div", {}, h("label", { class: "lb" }, "Cấp sao (+)"), F.star),
                h("div", {}, h("label", { class: "lb" }, "Ấn trang bị"), F.grade),
                h("div", { style: "display:flex;align-items:center;padding-top:18px" },
                  h("label", { style: "cursor:pointer;display:inline-flex;align-items:center;gap:6px;font-weight:600" }, F.l, " Khoá (không giao dịch)")
                )
              )
            ),
            h("div", { class: "sect", style: "margin-bottom:10px" },
              h("h4", {}, "Thời hạn sử dụng"),
              h("div", { style: "display:flex;flex-direction:column;gap:8px" },
                h("label", { style: "cursor:pointer;display:inline-flex;align-items:center;gap:6px;font-weight:600" }, F.perm, " Vĩnh viễn (không hết hạn)"),
                h("div", { style: "display:flex;align-items:center;gap:8px" },
                  h("span", { class: "mut", style: "font-size:12px;white-space:nowrap" }, "Hết hạn lúc:"),
                  F.exp
                )
              )
            ),
            h("div", { class: "sect", style: "margin-bottom:10px" },
              h("div", { class: "sect-h" },
                h("h4", {}, "Chỉ số thuộc tính GỐC"),
                h("span", { class: "hint", style: "margin:0" }, "Giá trị gốc chưa cộng % sao (game tự +4%/sao)")
              ),
              stats.el
            ),
            (value.charms && value.charms.length) || (value.stones && value.stones.length) ?
              h("div", { class: "sect danger" },
                h("h4", {}, "Tuỳ chọn gỡ socket"),
                h("div", { style: "display:flex;flex-wrap:wrap;gap:16px" },
                  value.charms && value.charms.length ? h("label", { style: "cursor:pointer;display:inline-flex;align-items:center;gap:6px;font-weight:600;color:var(--red)" }, F.cc, " Gỡ hết " + value.charms.length + " bùa linh hồn") : "",
                  value.stones && value.stones.length ? h("label", { style: "cursor:pointer;display:inline-flex;align-items:center;gap:6px;font-weight:600;color:var(--red)" }, F.cs, " Gỡ hết " + value.stones.length + " đá nguyên tố") : ""
                )
              ) : ""
          );
        } else {
          F.q = mode === "loot" ? pair(v.q, 1) : { el: h("input", { type: "number", min: 1, value: v.q || 1, style: "width:140px" }) };
          if (opts.lockTri && mode !== "loot") { F.l = h("select", {}, h("option", { value: "" }, "Mặc định (" + opts.lockTri + ")"), h("option", { value: "1" }, "Khoá"), h("option", { value: "0" }, "Không khoá")); F.l.value = v.l == null ? "" : v.l ? "1" : "0"; }
          else { F.l = h("input", { type: "checkbox" }); F.l.checked = mode === "loot" ? v.l !== 0 : !!v.l; }
          F.d = mode === "loot" ? pair(v.d, "") : { el: h("input", { type: "number", min: 0, value: v.d || "", placeholder: "trống = vĩnh viễn", style: "width:160px" }) };
          stats = statsEditor(v.s, true);
          form.append(h("label", {}, "Item"), h("div", {}, itemSearch(t => { tpl = t; showPicked(); }), picked),
            h("label", {}, mode === "loot" ? "Số lượng mỗi món" : "Số lượng"), F.q.el,
            h("label", {}, "Khoá"), F.l.tagName === "SELECT" ? F.l : h("label", {}, F.l, " Khoá (không giao dịch được)"),
            h("label", {}, "Hạn dùng (ngày)"), F.d.el);
          if (mode === "loot") {
            F.n = pair(v.n, 1); F.c = h("input", { type: "number", min: 0, max: 100, step: "0.1", value: v.c == null ? 100 : v.c, style: "width:100px" });
            F.pm = h("input", { type: "number", min: 0, max: 100, step: "0.1", value: v.perm || 0, style: "width:100px" });
            form.append(h("label", {}, "Số món rơi"), F.n.el, h("label", {}, "Tỉ lệ rơi (%)"), F.c, h("label", {}, "% ra vĩnh viễn"), h("span", {}, F.pm, h("span", { class: "mut" }, " (chỉ khi có hạn dùng)")));
          }
          X = (opts.extras || []).map(x => {
            const cur = v[x.key]; let inp;
            if (x.options) { inp = h("select", {}, Object.entries(x.options).map(([k, l]) => h("option", { value: k }, l))); inp.value = String(cur == null ? (x.def != null ? x.def : Object.keys(x.options)[0]) : cur); }
            else inp = h("input", { type: x.type || "number", step: "any", value: cur == null ? (x.def != null ? x.def : "") : cur, style: "width:160px" });
            form.append(h("label", {}, x.label), x.hint ? h("span", {}, inp, h("span", { class: "mut" }, " " + x.hint)) : inp);
            return [x, inp];
          });
          form.append(h("label", {}, "Dòng chỉ số"), h("div", {}, h("p", { class: "hint", style: "margin:0 0 6px" }, "Nhập 2 số (từ – đến) để random mỗi lần nhận."), stats.el));
        }
        let done = false;
        modal(title || (mode === "edit" ? "Sửa item" : "Chọn item"), form, {
          wide: true, onClose: () => { if (!done) res(null); },
          actions: [{
            label: mode === "edit" ? "Lưu thay đổi" : "Xong", primary: true, run: () => {
              let s; try { s = stats.get(); } catch (e) { toast(e.message, true); return false; }
              if (mode === "edit") {
                const q = Math.trunc(+F.q.value); if (!(q > 0)) { toast("Số lượng phải > 0.", true); return false; }
                const out = { q, l: F.l.checked ? 1 : 0, star: Math.trunc(+F.star.value), grade: +F.grade.value, s };
                out.expireAt = F.perm.checked || !F.exp.value ? -1 : new Date(F.exp.value).getTime();
                if (F.cc.checked) out.clearCharms = 1; if (F.cs.checked) out.clearStones = 1;
                done = true; res(out); return true;
              }
              if (!tpl) { toast("Chọn một item.", true); return false; }
              const out = Object.assign({}, v, { i: tpl.id, name: tpl.name, icon: tpl.icon || "" });
              out.q = mode === "loot" ? F.q.get() : Math.trunc(+F.q.el.value);
              if (!(Array.isArray(out.q) ? out.q[0] > 0 : out.q > 0)) { toast("Số lượng phải > 0.", true); return false; }
              if (mode === "loot") { out.l = F.l.checked ? 1 : 0; out.n = F.n.get(); out.c = +F.c.value; out.perm = +F.pm.value; if (out.c >= 100) delete out.c; if (!out.perm) delete out.perm; if (out.n === 1) delete out.n; if (out.q === 1) delete out.q; if (out.l === 1) delete out.l; }
              else if (F.l.tagName === "SELECT") { if (F.l.value === "") delete out.l; else out.l = +F.l.value; }
              else out.l = F.l.checked ? 1 : 0;
              for (const [x, inp] of X) { if (inp.value === "") delete out[x.key]; else out[x.key] = x.type === "text" ? inp.value : +inp.value; }
              const dval = mode === "loot" ? (F.d.el.querySelector("input").value === "" ? 0 : F.d.get()) : +F.d.el.value;
              if (dval && (!Array.isArray(dval) || dval[1] > 0)) out.d = dval; else delete out.d;
              if (s.length) out.s = s; else delete out.s;
              done = true; res(out); return true;
            }
          }]
        });
      });
    }

    /* Tóm tắt 1 dòng quà / món quà. */
    function rewardSummary(r, mode, opts = {}) {
      const parts = [];
      if (mode === "loot") { parts.push((r.n ? rng(r.n) : 1) + " món × " + rng(r.q == null ? 1 : r.q)); if (r.c != null && r.c < 100) parts.push(r.c + "% rơi"); parts.push(r.l === 0 ? "không khoá" : "khoá"); }
      else { parts.push("SL " + rng(r.q || 1)); parts.push(r.l == null && opts.lockTri ? opts.lockTri : r.l ? "khoá" : "không khoá"); }
      (opts.extras || []).forEach(x => { if (r[x.key] != null) parts.push(x.label + ": " + (x.options && x.options[r[x.key]] != null ? x.options[r[x.key]] : num(r[x.key]))); });
      if (r.d) parts.push("hạn " + rng(r.d) + " ngày"); if (r.perm) parts.push(r.perm + "% vĩnh viễn");
      if (r.c && mode !== "loot" && !(opts.extras || []).some(x => x.key === "c")) parts.push("class " + r.c); if (r.g) parts.push("giới tính " + r.g);
      return parts.join(" · ");
    }
    /* Danh sách quà dạng bảng (thêm / sửa / xoá từng món qua popup). */
    function rewardList(container, { mode = "reward", max, hint, separateIconName = false, extras, lockTri, reorder } = {}) {
      const eo = { extras, lockTri };
      let items = [];
      const tb = h("tbody");
      const render = () => fill(tb, items.map((r, i) => h("tr", {},
        h("td", { class: "mut" }, i + 1), ...(separateIconName
          ? [h("td", {}, ico(r.icon, 34)), h("td", {}, h("b", {}, r.name || "#" + r.i), h("div", { class: "mut", style: "font-size:11.5px" }, "#" + r.i))]
          : [h("td", {}, h("div", { class: "sub-row" }, ico(r.icon, 34), h("div", {}, h("b", {}, r.name || "#" + r.i), h("div", { class: "mut", style: "font-size:11.5px" }, "#" + r.i))))]),
        h("td", {}, rewardSummary(r, mode, eo), (r.s || []).map(l => h("div", { class: "ln", style: "color:#1f6b3a;font-size:12px" }, statLine(l)))),
        h("td", {}, h("div", { class: "gc-reward-actions" }, reorder ? [h("button", { class: "btn small", type: "button", title: "Lên", disabled: i === 0, onclick: () => { [items[i - 1], items[i]] = [items[i], items[i - 1]]; render(); } }, "↑"),
        h("button", { class: "btn small", type: "button", title: "Xuống", disabled: i === items.length - 1, onclick: () => { [items[i + 1], items[i]] = [items[i], items[i + 1]]; render(); } }, "↓")] : "",
          h("button", { class: "btn small", type: "button", onclick: async () => { const nv = await editItem(mode, r, "Sửa món quà", eo); if (nv) { items[i] = nv; render(); } } }, "Sửa"),
          h("button", { class: "btn small danger", type: "button", onclick: () => { items.splice(i, 1); render(); } }, "Xoá"))))), "Chưa có món nào");
      const add = h("button", { class: "btn small primary", type: "button", onclick: async () => { if (max && items.length >= max) return toast("Tối đa " + max + " món.", true); const r = await editItem(mode, null, "Thêm món quà", eo); if (r) { items.push(r); render(); } } }, "+ Thêm món");
      container.replaceChildren(h("div", { class: "tw", style: "max-height:320px" }, h("table", { class: "ilist" }, h("thead", {}, h("tr", {}, h("th", {}, "#"), ...(separateIconName ? [h("th", {}, "Icon"), h("th", {}, "Tên item")] : [h("th", {}, "Item")]), h("th", {}, "Chi tiết"), h("th", {}, ""))), tb)),
        h("div", { class: "row", style: "margin-top:8px;justify-content:space-between" }, h("span", { class: "hint", style: "margin:0" }, hint || ""), add));
      statTypes().then(render).catch(() => render());
      return { get: () => items.map(r => { const o = Object.assign({}, r); delete o.name; delete o.icon; return o; }), set: list => { items = (list || []).map(r => JSON.parse(JSON.stringify(r))); render(); }, size: () => items.length };
    }

    /* Popup gửi thư quà cho 1 nhân vật (dùng ở tab Người chơi + Tài khoản). */
    function giftDialog(id, name) {
      const title = h("input", { style: "width:100%", placeholder: "Quà từ quản trị viên" }), content = h("textarea", { placeholder: "Lời nhắn (tuỳ chọn)", style: "min-height:70px" });
      const listBox = h("div"); const list = rewardList(listBox, { hint: "Item 1 = vàng (khoá = vàng khoá), 2 = lam thạch. Quá 5 món tự chia nhiều thư." });
      modal("Gửi quà qua thư · " + name, h("div", {}, h("label", { class: "lb" }, "Tiêu đề"), title, h("label", { class: "lb" }, "Nội dung"), content, h("label", { class: "lb" }, "Vật phẩm đính kèm"), listBox), {
        wide: true, actions: [{ label: "Gửi thư", primary: true, run: async () => { const items = list.get(); if (!items.length && !title.value.trim()) { toast("Thêm ít nhất 1 món hoặc nhập tiêu đề.", true); return false; } return !!(await act("player/gift", { id, items, title: title.value, content: content.value }, "Gửi " + items.length + " món cho " + name + "?")); } }]
      });
    }

/* ------------------------------------------------------------------ */

/* ================= Tổng quan ================= */
    /* Số lớn viết gọn (205,4 tỷ); số đầy đủ đặt ở title. */
    function short(n) {
      n = Number(n || 0); const a = Math.abs(n), f = (v, u) => (Math.round(v * 10) / 10).toLocaleString("vi-VN") + " " + u;
      return a >= 1e12 ? f(n / 1e12, "nghìn tỷ") : a >= 1e9 ? f(n / 1e9, "tỷ") : a >= 1e6 ? f(n / 1e6, "triệu") : num(Math.round(n));
    }
    Tabs.add({
      id: "dash", group: "Điều hành", label: "Tổng quan", poll: 30000,
      html: `<div class="dash-head">
    <div class="dash-head-info">
      <h1>Tổng quan hệ thống</h1>
      <p class="dash-desc">Thống kê hoạt động người chơi, dòng tiền kinh tế và tài nguyên máy chủ.</p>
    </div>
    <div class="dash-head-actions">
      <div class="sync-status" id="dSyncBox" title="Dữ liệu tổng hợp từ CSDL SQL được lưu đệm 30 giây trong RAM để tối ưu hiệu năng.">
        <span class="sync-indicator"></span>
        <span id="dSub">Đang đồng bộ...</span>
      </div>
      <button class="btn small primary dash-refresh-btn" id="dFresh" title="Truy vấn dữ liệu mới nhất từ CSDL ngay lập tức">
        <svg class="refresh-icon" viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" fill="none" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 11-.57-8.38l5.67-5.67"/></svg>
        <span>Làm mới</span>
      </button>
    </div>
  </div>
  <div class="kpis">
    <div class="card kpi"><span class="ic" data-ic="accounts"></span><div><small>Tài khoản</small><strong id="kAcc">–</strong><em id="kAccSub">&nbsp;</em></div></div>
    <div class="card kpi"><span class="ic blue" data-ic="players"></span><div><small>Nhân vật</small><strong id="kChar">–</strong><em id="kCharSub">&nbsp;</em></div></div>
    <div class="card kpi"><span class="ic sun" data-ic="globe"></span><div><small>Đang online</small><strong id="kOn">–</strong><em id="kOnSub">&nbsp;</em></div></div>
    <div class="card kpi"><span class="ic red" data-ic="trend"></span><div><small>Hoạt động 24 giờ</small><strong id="kAct">–</strong><em id="kActSub">&nbsp;</em></div></div>
  </div>
  <div class="c-half">
    <div class="card">
      <div class="ph" style="margin-bottom:6px">
        <h2>Kinh tế thế giới</h2>
        <span class="chip sm" style="font-weight:600">Trong nhân vật</span>
      </div>
      <p class="hint">Tài sản đang lưu giữ trên nhân vật (chưa gồm hòm thư, rương và chợ kí gửi).</p>
      <div class="econ-grid" id="econ"></div>
    </div>
    <div class="card">
      <div class="ph" style="margin-bottom:6px">
        <h2>Tài nguyên máy chủ</h2>
        <span class="chip sm ok" id="srvStatusChip">Bình thường</span>
      </div>
      <p class="hint" id="srvSub">Đang lấy mẫu JVM...</p>
      <div class="donuts" id="donuts"></div>
    </div>
  </div>
  <div class="card">
    <div class="ph" style="margin-bottom:6px">
      <h2>Vận hành kết nối & CSDL</h2>
      <span class="mut" style="font-size:12px">Cập nhật mỗi 10 giây</span>
    </div>
    <div class="minis" id="ops"></div>
  </div>`,
      setup(root) {
        root.querySelectorAll("[data-ic]").forEach(e => e.innerHTML = '<svg viewBox="0 0 24 24">' + ICONS[e.dataset.ic] + "</svg>");
        const set = (id, v) => { const el = $(id); if (el) el.textContent = v; };
        let last = null, lastStatus = null;

        // Khởi tạo trước cấu trúc DOM (In-place updates) - không parse SVG hay replaceChildren liên tục
        function makeDonut(label) {
          const c = 2 * Math.PI * 42;
          const d = h("div", { class: "donut" });
          d.innerHTML = '<svg viewBox="0 0 100 100"><circle cx="50" cy="50" r="42" fill="none" stroke="#e3ebe8" stroke-width="10"/><circle class="donut-arc" cx="50" cy="50" r="42" fill="none" stroke="#0f4c4a" stroke-width="10" stroke-linecap="round" stroke-dasharray="0 ' + c + '" transform="rotate(-90 50 50)"/><text class="donut-val" x="50" y="56" text-anchor="middle" font-size="19" font-weight="800" fill="#12302f">–</text></svg>';
          const b = h("b", {}, label);
          const sub = h("small", {}, "");
          d.append(b, sub);
          const arc = d.querySelector(".donut-arc"), val = d.querySelector(".donut-val");
          return {
            el: d,
            update(p, text, subTxt) {
              const v = Math.max(0, Math.min(100, p));
              const col = v >= 85 ? "#c8402b" : v >= 60 ? "#f2a900" : "#0f4c4a";
              arc.setAttribute("stroke", col);
              arc.setAttribute("stroke-dasharray", (c * v / 100).toFixed(1) + " " + c.toFixed(1));
              val.textContent = text;
              sub.textContent = subTxt || "";
            }
          };
        }
        const dCpu = makeDonut("CPU tiến trình");
        const dRam = makeDonut("RAM JVM");
        $("donuts").replaceChildren(dCpu.el, dRam.el);

        function makeMini(title) {
          const box = h("div", {}, h("small", {}, title), h("div", { class: "big" }, "–"), h("small", { class: "mut" }, ""));
          const valEl = box.querySelector(".big"), subEl = box.querySelector(".mut");
          return {
            el: box,
            update(v, subTxt) { valEl.textContent = v; subEl.textContent = subTxt || ""; }
          };
        }
        const mConn = makeMini("Kết nối TCP");
        const mZones = makeMini("Khu đang mở");
        const mPeakToday = makeMini("Đỉnh online hôm nay");
        const mPeakAll = makeMini("Đỉnh từ lúc bật");
        const mPending = makeMini("Chờ ghi CSDL");
        const mThreads = makeMini("Luồng JVM");
        $("ops").replaceChildren(mConn.el, mZones.el, mPeakToday.el, mPeakAll.el, mPending.el, mThreads.el);

        function makeEconCard(iconText, title, cls) {
          const card = h("div", { class: "econ-card " + cls },
            h("div", { class: "econ-icon" }, iconText),
            h("div", { class: "econ-meta" },
              h("small", {}, title),
              h("strong", {}, "–"),
              h("span", {}, "")
            )
          );
          const strongEl = card.querySelector("strong"), spanEl = card.querySelector("span");
          return {
            el: card,
            update(v, subTxt) {
              card.title = num(v);
              strongEl.textContent = short(v);
              spanEl.textContent = subTxt || "";
            }
          };
        }
        const eGold = makeEconCard("🪙", "Vàng", "gold");
        const eLockedGold = makeEconCard("🔒", "Vàng khoá", "locked-gold");
        const eDiamond = makeEconCard("💎", "Lam thạch", "diamond");
        const eRecharge = makeEconCard("⭐", "Tổng nạp", "recharge");
        $("econ").replaceChildren(eGold.el, eLockedGold.el, eDiamond.el, eRecharge.el);

        function updateStatusUI(s) {
          if (!s) return;
          const cpu = Math.max(0, s.cpuProcess), ram = s.ramMaxMb > 0 ? s.ramUsedMb * 100 / s.ramMaxMb : 0;
          dCpu.update(cpu, s.cpuProcess < 0 ? "?" : fmtPct(cpu), "Cả máy: " + (s.cpuSystem < 0 ? "?" : s.cpuSystem + "%") + " · " + s.cores + " nhân");
          dRam.update(ram, fmtPct(ram), fmtMem(s.ramUsedMb) + " / " + fmtMem(s.ramMaxMb));
          set("srvSub", "Java " + s.javaVersion + " · Chạy " + dur(s.uptimeSeconds));
          const chip = $("srvStatusChip");
          if (chip) {
            chip.textContent = cpu >= 85 || ram >= 85 ? "Tải cao" : cpu >= 60 || ram >= 60 ? "Vừa phải" : "Ổn định";
            chip.className = "chip sm " + (cpu >= 85 || ram >= 85 ? "bad" : cpu >= 60 || ram >= 60 ? "warn" : "ok");
          }
          mConn.update(num(s.connections), num(s.online) + " tài khoản đăng nhập");
          mZones.update(num(s.zones), num(s.activeMaps) + " map có người");
          mPeakToday.update(num(s.peakToday), s.peakTodayAt ? "lúc " + s.peakTodayAt : "");
          mPeakAll.update(num(s.peakOnline), s.peakAt || "");
          mPending.update(num(s.pendingWrites), "lượt lưu đang xếp hàng");
          mThreads.update(num(s.threads), "");
          set("kOn", num(s.inWorld));
          if (last) set("kOnSub", "trên " + num(last.totals.characters) + " nhân vật (" + pct(s.inWorld, last.totals.characters) + ")");
        }

        Admin.onStatus(s => {
          lastStatus = s;
          if (Tabs.currentId && Tabs.currentId() === "dash") {
            updateStatusUI(s);
          }
        });

        async function load(fresh) {
          const o = await api("overview" + (fresh ? "?fresh=1" : "")); last = o; const t = o.totals, on = o.online;
          const ago = o.computedAgo;
          const syncText = ago == null ? "Đã đồng bộ · đệm 30s" : ago <= 0 ? "Vừa đồng bộ · đệm 30s" : "Đồng bộ " + ago + "s trước · đệm 30s";
          set("dSub", syncText);

          set("kAcc", num(t.accounts)); set("kAccSub", "+" + num(t.newAccountsToday) + " hôm nay · " + num(t.banned) + " bị khoá");
          set("kChar", num(t.characters)); set("kCharSub", "+" + num(t.newCharsToday) + " hôm nay · TB cấp " + String(t.avgLevel).replace(".", ","));
          set("kOn", num(on.count)); set("kOnSub", "trên " + num(t.characters) + " nhân vật (" + pct(on.count, t.characters) + ")" + (on.bots ? " · +" + num(on.bots) + " bot" : ""));
          set("kAct", num(t.active1d)); set("kActSub", num(t.active7d) + " / 7 ngày · " + num(t.active30d) + " / 30 ngày");
          const per = t.characters > 0 ? t.gold / t.characters : 0;
          eGold.update(t.gold, "TB " + short(per) + " / nhân vật");
          eLockedGold.update(t.lockedGold, "Vàng khoá theo nhân vật");
          eDiamond.update(t.diamond, "Lam thạch hiện có");
          eRecharge.update(t.recharge, "Tích lũy nạp kim cương");
          if (lastStatus) updateStatusUI(lastStatus);
        }

        const freshBtn = $("dFresh");
        if (freshBtn) {
          freshBtn.onclick = async () => {
            freshBtn.classList.add("busy");
            try {
              await load(true);
              toast("Đã đồng bộ số liệu CSDL mới nhất.");
            } catch (e) {
              toast(e.message, true);
            } finally {
              freshBtn.classList.remove("busy");
            }
          };
        }
        return { refresh: () => load(false) };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Tài khoản: danh sách + popup chi tiết ================= */
    /* Popup tài khoản — mở được từ mọi tab (openAccount(id)). onChange: gọi lại khi có thay đổi. */
    let accountsChanged = () => { };
    async function openAccount(id) {
      let a; try { a = await api("account?id=" + id); } catch (e) { return toast(e.message, true); }
      const body = h("div", { class: "account-management" });
      const close = modal("Quản lý tài khoản · " + a.username, body, { wide: true });
      const reload = async () => { try { a = await api("account?id=" + id); draw(); accountsChanged(); } catch (e) { toast(e.message, true); } };
      const doAct = async (path, data, confirm) => { if (await act(path, Object.assign({ id }, data), confirm)) reload(); };
      function draw() {
        const p = a.player;
        const roleSel = h("select", {}, ROLES.map((x, i) => h("option", { value: i }, x))); roleSel.value = a.role;
        const mails = (a.mails || []).map(m => {
          const box = h("div", { class: "mail" });
          box.append(h("div", { class: "mh", onclick: () => box.classList.toggle("open") }, h("div", { style: "min-width:0" }, h("b", {}, m.subject || "(không tiêu đề)"), h("div", { class: "mut", style: "font-size:11.5px" }, m.box + " · " + m.from + " → " + m.to + " · " + m.time)),
            h("div", { class: "chips-r" }, m.items.length ? chip(m.items.length + " món", m.claimed ? "" : "warn") : "", m.read ? "" : chip("Chưa đọc", "ok"))),
            h("div", { class: "mb2" }, h("p", { style: "white-space:pre-wrap;margin-bottom:8px" }, m.content || ""), m.items.length ? h("div", { class: "grid-items" }, m.items.map(it => tile(it, null))) : "",
              m.items.length ? h("p", { class: "hint", style: "margin-top:6px" }, m.claimed ? "Đã nhận đồ đính kèm." : "Chưa nhận đồ đính kèm.") : ""));
          return box;
        });
        body.replaceChildren(
          h("div", { class: "hero" }, h("div", { class: "av big" }, a.username.slice(0, 1).toUpperCase()),
            h("div", { class: "main" }, h("h2", {}, a.username, h("span", { class: "mut", style: "font-weight:600" }, " #" + a.id)),
              h("div", { class: "chips-r" }, a.online ? chip("Online", "ok") : chip("Offline"), a.banned ? chip("Bị khoá", "bad") : "", a.role ? chip(ROLES[a.role], "warn") : "", a.pw2 ? chip("Có MK cấp 2") : ""))),
          h("div", { class: "sect" }, h("h4", {}, "Thông tin"), h("div", { class: "kv" }, kv("Email", a.email), kv("Tạo lúc", a.created + (a.registerIp ? " · IP " + a.registerIp : "")),
            kv("Đăng nhập cuối", (a.lastLogin || "chưa") + (a.ip ? " · IP " + a.ip : "")), a.banned ? kv("Lý do khoá", a.banReason || "(không ghi)") : [],
            kv("MK cấp 2", a.pw2 ? "Có" + (a.protection ? " · đang bật bảo vệ" : "") : "Chưa tạo"))),
          h("div", { class: "sect" }, h("h4", {}, "Bảo mật & phân quyền"),
            h("div", { class: "acts" },
              h("button", { class: "btn small", onclick: async () => { const pw = await prompt2("Đổi mật khẩu · " + a.username, "Mật khẩu mới (6-32 ký tự)", "", "text"); if (pw != null) doAct("account/password", { password: pw }); } }, "Đổi mật khẩu"),
              h("button", { class: "btn small", onclick: async () => { const em = await prompt2("Đổi email", "Email mới", a.email); if (em != null) doAct("account/email", { email: em }); } }, "Đổi email"),
              h("button", { class: "btn small", onclick: async () => { const pw = await prompt2("MK cấp 2 · " + a.username, "MK cấp 2 mới", "", "text"); if (pw != null) doAct("account/secondary", { action: "set", password: pw }); } }, a.pw2 ? "Đổi MK cấp 2" : "Đặt MK cấp 2"),
              a.pw2 ? h("button", { class: "btn small", onclick: () => doAct("account/secondary", { action: "clear" }, "Xoá MK cấp 2 và tắt bảo vệ của " + a.username + "?") }, "Xoá MK cấp 2") : ""),
            h("div", { class: "row", style: "margin-top:10px" }, h("label", { class: "mut" }, "Vai trò"), roleSel, h("button", { class: "btn small primary", onclick: () => doAct("account/role", { role: +roleSel.value }) }, "Lưu vai trò")),
            h("p", { class: "hint", style: "margin:0" }, "GM / quản trị viên dùng được lệnh test trong ô chat kể cả khi chat.testCommands tắt.")),
          h("div", { class: "sect" }, h("h4", {}, "Nhân vật"),
            p ? h("div", { class: "li", onclick: () => openPlayer(p.id) }, h("div", { class: "av" }, p.name.slice(0, 1).toUpperCase()),
              h("div", { class: "main" }, h("div", { class: "t" }, p.name), h("div", { class: "s" }, p.gender + " · cấp " + p.level + " · vàng " + short(p.gold) + " · lam thạch " + num(p.diamond) + " · chơi " + dur(p.playTime))),
              h("div", { class: "chips-r" }, h("button", { class: "btn small", onclick: e => { e.stopPropagation(); giftDialog(p.id, p.name); } }, "Gửi quà"), h("button", { class: "btn small primary" }, "Mở")))
              : h("p", { class: "mut" }, "Tài khoản chưa tạo nhân vật.")),
          h("div", { class: "sect" }, h("h4", {}, "Thư gần đây (" + mails.length + ")"), mails.length ? mails : h("p", { class: "mut" }, "Không có thư.")),
          h("div", { class: "sect danger" }, h("h4", {}, "Khoá / xoá"),
            h("div", { class: "acts" },
              a.banned ? h("button", { class: "btn small", onclick: () => doAct("account/ban", { banned: false }) }, "Mở khoá tài khoản")
                : h("button", { class: "btn small danger", onclick: async () => { const why = await prompt2("Khoá tài khoản " + a.username, "Lý do (người chơi thấy khi đăng nhập)", ""); if (why != null) doAct("account/ban", { banned: true, reason: why }); } }, "Khoá tài khoản"),
              h("button", { class: "btn small danger", onclick: async () => { const typed = await prompt2("XOÁ tài khoản " + a.username, "Gõ lại tên tài khoản để xác nhận (không hoàn tác)", ""); if (typed == null) return; if (typed !== a.username) return toast("Tên không khớp, chưa xoá.", true); if (await act("account/delete", { id })) { close(); accountsChanged(); } } }, "Xoá tài khoản"))));
      }
      draw();
    }
    Tabs.add({
      id: "accounts", group: "Điều hành", label: "Tài khoản", poll: 0,
      html: `<div class="ph"><div><h1>Tài khoản</h1><p class="sub">Chọn một dòng hoặc bấm “Quản lý” để mở hồ sơ và các thao tác tài khoản.</p></div><button class="btn primary" id="aNew">+ Tạo tài khoản</button></div>
  <div class="card">
    <div class="row"><input id="aQ" class="grow" type="search" placeholder="Tài khoản, email, tên nhân vật, IP hoặc ID"><button class="btn primary" id="aSearch">Tìm</button></div>
    <div class="chips scroll" id="aFilter" style="margin-bottom:12px"></div>
    <div class="account-list-head"><strong id="aResultTitle">Danh sách tài khoản</strong><span id="aInfo"></span></div>
    <div class="list" id="aList"></div>
    <div class="pager"><span class="mut">Dùng Enter hoặc Space để mở dòng đang chọn.</span><button class="btn small" id="aPrev">‹ Trước</button><button class="btn small" id="aNext">Sau ›</button></div>
  </div>`,
      setup(root) {
        const FILTERS = [["", "Tất cả"], ["online", "Đang online"], ["banned", "Bị khoá"], ["staff", "GM / quản trị"], ["pw2", "Có MK cấp 2"], ["nochar", "Chưa có nhân vật"]];
        let filter = "", page = 0, total = 0, size = 50, loaded = false;
        $("aFilter").replaceChildren(...FILTERS.map(([k, l]) => h("button", { type: "button", class: k === filter ? "on" : "", onclick: e => { filter = k; page = 0;[...$("aFilter").children].forEach(b => b.classList.toggle("on", b === e.currentTarget)); load(); } }, l)));
        async function load() {
          let r; try { r = await api(`accounts?q=${encodeURIComponent($("aQ").value)}&filter=${filter}&page=${page}`); } catch (e) { return toast(e.message, true); }
          loaded = true; total = r.total; size = r.pageSize;
          $("aResultTitle").textContent = (filter ? "Kết quả lọc" : "Danh sách tài khoản") + " · " + num(total) + " tài khoản";
          $("aList").replaceChildren(...(r.rows.length ? r.rows.map(a => h("div", { class: "li account-row", tabindex: 0, role: "button", "aria-label": "Quản lý tài khoản " + a.username, onclick: () => openAccount(a.id), onkeydown: e => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openAccount(a.id); } } },
            h("div", { class: "av" + (a.online ? " on" : "") }, a.username.slice(0, 1).toUpperCase()),
            h("div", { class: "main" }, h("div", { class: "t" }, a.username, h("span", { class: "mut", style: "font-weight:500" }, " #" + a.id)),
              h("div", { class: "s" }, (a.playerId ? a.playerName + " · cấp " + a.level : "Chưa có nhân vật") + " · " + (a.email || "")),
              h("div", { class: "s" }, "Đăng nhập: " + (a.lastLogin || "chưa") + (a.ip ? " · " + a.ip : ""))),
            h("div", { class: "chips-r" }, a.banned ? chip("Bị khoá", "bad") : a.online ? chip("Online", "ok") : chip("Offline"), a.role ? chip(ROLES[a.role], "warn") : "", a.pw2 ? chip("MK2") : "",
              h("button", { class: "btn small primary account-manage", tabindex: -1, onclick: e => { e.stopPropagation(); openAccount(a.id); } }, "Quản lý →"))))
            : [h("p", { class: "empty" }, "Không có tài khoản phù hợp")]));
          $("aInfo").textContent = total ? `${page * size + 1}–${Math.min(total, (page + 1) * size)} / ${num(total)}` : "0 kết quả";
          $("aPrev").disabled = page === 0; $("aNext").disabled = (page + 1) * size >= total;
        }
        accountsChanged = () => { if (loaded) load(); };
        $("aSearch").onclick = () => { page = 0; load(); }; $("aQ").onkeydown = e => { if (e.key === "Enter") { page = 0; load(); } };
        $("aPrev").onclick = () => { page--; load(); }; $("aNext").onclick = () => { page++; load(); };
        $("aNew").onclick = () => {
          const u = h("input", { placeholder: "4-20 ký tự: chữ không dấu, số, _", autocomplete: "off" }), p = h("input", { type: "text", placeholder: "6-32 ký tự", autocomplete: "off" }), m = h("input", { type: "email", placeholder: "trống = tự sinh" });
          const r = h("select", {}, ROLES.map((x, i) => h("option", { value: i }, x)));
          modal("Tạo tài khoản", h("div", { class: "field" }, h("label", {}, "Tài khoản"), u, h("label", {}, "Mật khẩu"), p, h("label", {}, "Email"), m, h("label", {}, "Vai trò"), r), {
            actions: [{ label: "Tạo", primary: true, run: async () => { const res = await act("account/create", { username: u.value, password: p.value, email: m.value, role: +r.value }); if (res) { load(); openAccount(res.id); } return !!res; } }]
          });
        };
        return { refresh: async () => { if (!loaded) await load(); } };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Người chơi: danh sách + popup quản lý ================= */
    let playersChanged = () => { };
    let BUFFS = null, ENHANCE_DATA = null;
    const QUICK_NAMES = ["HP", "MP", "Phím nhanh"];
    let MAPS = null;

    async function openPlayer(id) {
      let r; try { r = await api("player?id=" + id); } catch (e) { return toast(e.message, true); }
      const body = h("div"); let view = "info", colKind = null, pd = null;
      modal("Nhân vật · " + r.player.name, body, { wide: true });
      const reload = async () => { try { r = await api("player?id=" + id); pd = null; draw(); playersChanged(); } catch (e) { toast(e.message, true); } };
      /* Dữ liệu nhiệm vụ / thành tựu chỉ tải khi mở tab cần. */
      const DATA_VIEWS = new Set(["quest", "ach"]);
      const ACH_TYPES = { elite: "Hạ Tinh anh", leader: "Hạ Thủ lĩnh", guardian: "Hạ Vệ thần", spirit: "Hạ Tinh linh", revive: "Hồi sinh bằng Cỏ hồi sinh", market_sell: "Đăng bán kí gửi", brew: "Pha chế thành công", activity_chest: "Mở Rương hoạt động", upgrade_star: "Số sao nâng cấp cao nhất", refine_stone: "Đá khởi nguyên đã luyện", fish: "Câu được cá", beehive: "Phá tổ ong", pig_harvest: "Thu hoạch heo", level: "Cấp cao nhất" };
      const ACH_STATUS = [["Đang làm", ""], ["Nhận được", "warn"], ["Đã nhận", "ok"]];

      const progressAct = async (action, extra, confirmText) => { const res = await act("player/progress/action", Object.assign({ id, action }, extra || {}), confirmText); if (res) reload(); return res; };
      async function run(path, data, confirmText) { const res = await act(path, Object.assign({ id }, data), confirmText); if (res) reload(); return res; }
      const itemAct = (where, slot, action, extra, confirmText) => run("player/item/action", Object.assign({ box: where, slot, action }, extra || {}), confirmText);

      function draw() {
        const acc = r.account, p = r.player;
        const TABS = [
          ["info", "📊 Tổng quan"],
          ["body", "⚔️ Trang bị"],
          ["bag", "🎒 Hành trang " + p.bag.items.length + "/" + p.bag.size],
          ["box", "📦 Rương " + p.box.items.length + "/" + p.box.size],
          ["col", "✨ Bộ sưu tập"],
          ["buff", "🧪 Hiệu ứng (" + p.buffs.length + ")"],
          ["skill", "📜 Kỹ năng"],
          ["quest", "🎯 Nhiệm vụ"],
          ["ach", "🏆 Thành tựu"]
        ];
        const content = h("div");
        if (DATA_VIEWS.has(view) && !pd) {
          content.append(h("p", { class: "empty" }, "Đang tải dữ liệu…"));
          api("player/progress?id=" + id).then(x => { pd = x; draw(); }).catch(e => { content.replaceChildren(h("p", { class: "empty" }, e.message)); });
        } else ({ info, body: bodyView, bag: el => boxView("bag", el), box: el => boxView("box", el), col: collections, buff: buffs, skill: skills, quest: questView, ach: achView })[view](content);

        body.replaceChildren(
          h("div", { class: "hero" },
            h("div", { class: "av big" + (p.online ? " on" : "") }, p.name.slice(0, 1).toUpperCase()),
            h("div", { class: "main" },
              h("div", { style: "display:flex;align-items:center;gap:8px;flex-wrap:wrap" },
                h("h2", { style: "margin:0" }, p.name),
                h("span", { class: "chip" }, "#" + p.id),
                h("span", { class: "chip ok" }, "Cấp " + p.level),
                p.cls ? h("span", { class: "chip" }, p.cls) : "",
                p.gender ? h("span", { class: "chip mut-chip" }, p.gender) : ""
              ),
              h("div", { class: "mut", style: "margin-top:4px" },
                "Tài khoản: ", h("b", { style: "color:var(--ink)" }, acc.username), " · Vị trí: ", p.map, (p.zone >= 0 ? " (Khu " + p.zone + ")" : "")
              ),
              h("div", { class: "chips-r", style: "justify-content:flex-start;margin-top:6px" },
                p.online ? chip("🟢 Đang online", "ok") : chip("⚪ Offline", "mut-chip"),
                acc.banned ? chip("🔒 Tài khoản bị khoá", "bad") : "",
                acc.role ? chip(ROLES[acc.role], "warn") : "",
                p.trading ? chip("🔄 Đang giao dịch", "warn") : ""
              )
            )
          ),
          h("div", { class: "toolbar" },
            h("div", { class: "tb-group" },
              h("button", { class: "btn small primary", onclick: () => { view = "body"; draw(); } }, "⚔️ Trang bị"),
              h("button", { class: "btn small", onclick: editDialog }, "✏️ Sửa chỉ số"),
              h("button", { class: "btn small", onclick: teleportDialog }, "🌀 Dịch chuyển"),
              h("button", { class: "btn small", onclick: () => giftDialog(p.id, p.name) }, "🎁 Gửi quà"),
              h("button", { class: "btn small", onclick: () => addItem("bag") }, "➕ Thêm item")
            ),
            h("div", { class: "tb-group" },
              h("button", { class: "btn small", disabled: !p.online, onclick: () => run("player/kick", {}, "Kick " + p.name + "?") }, "⚡ Kick game"),
              acc.banned
                ? h("button", { class: "btn small ok", onclick: async () => { if (await act("account/ban", { id: acc.id, banned: false })) reload(); } }, "🔓 Mở khoá TK")
                : h("button", { class: "btn small danger", onclick: async () => { const why = await prompt2("Khoá tài khoản " + acc.username, "Lý do khoá", ""); if (why != null && await act("account/ban", { id: acc.id, banned: true, reason: why })) reload(); } }, "🔒 Khoá TK"),
              h("button", { class: "btn small", onclick: () => openAccount(acc.id) }, "👤 Xem tài khoản"),
              h("button", { class: "btn small", onclick: reload, title: "Tải lại dữ liệu" }, "↻ Tải lại")
            )
          ),
          h("div", { class: "subtabs" }, TABS.map(([k, l]) => h("button", { class: k === view ? "on" : "", onclick: () => { view = k; draw(); } }, l))),
          content
        );

        /* ---------- Tổng quan ---------- */
        function info(el) {
          const pctExp = p.expToLevel > 0 ? Math.min(100, p.exp * 100 / p.expToLevel) : 0;
          const sect = (title, onEdit, ...rows) => h("div", { class: "sect" },
            h("div", { class: "sect-h" }, h("h4", {}, title), onEdit ? h("button", { class: "btn small", onclick: onEdit }, "Chỉnh sửa") : ""),
            h("div", { class: "kv" }, rows)
          );
          el.append(
            h("div", { class: "info-grid" },
              sect("Thông tin nhân vật", editDialog,
                kv("Cấp độ", p.level + " / " + p.maxLevel),
                kv("EXP hiện tại", h("div", {}, num(p.exp) + " / " + num(p.expToLevel) + " (" + pctExp.toFixed(2).replace(".", ",") + "%)", h("div", { class: "xpbar" }, h("i", { style: "width:" + pctExp + "%" })))),
                kv("Điểm tiềm năng", num(p.talentPoints) + " điểm"),
                kv("Thể lực", num(p.stamina)),
                kv("Sát khí", p.pk),
                kv("Danh hiệu", p.title || "–"),
                kv("Nhiệm vụ", p.quest || "–")
              ),
              sect("Vị trí & Bang hội", teleportDialog,
                kv("Bản đồ", p.map + " (#" + p.mapId + ")"),
                kv("Khu vực", p.zone >= 0 ? "Khu " + p.zone : "–"),
                kv("Toạ độ", p.pos || "Điểm xuất hiện mặc định"),
                kv("Bộ lạc / Bang", p.tribe || (p.online ? "Chưa vào bộ lạc" : "(chỉ xem khi online)")),
                kv("Tổ đội", p.party || "Chưa vào đội")
              ),
              sect("Tài sản tiền tệ", editDialog,
                kv("Vàng", num(p.gold)),
                kv("Vàng khoá", num(p.lockedGold)),
                kv("Lam thạch", num(p.diamond)),
                kv("Tổng nạp", num(p.recharge))
              ),
              sect("Tài khoản liên kết", () => openAccount(acc.id),
                kv("Tài khoản", acc.username + " (#" + acc.id + ")"),
                kv("Vai trò", ROLES[acc.role]),
                kv("Địa chỉ IP", p.ip || acc.ip || "–"),
                acc.banned ? kv("Lý do khoá", acc.banReason || "(không ghi)") : [],
                kv("Ngày tạo", acc.charCreated),
                kv("Đăng nhập cuối", acc.lastLogin),
                kv("Thời gian chơi", dur(p.playTime))
              )
            ),
            h("div", { class: "sect", style: "margin-top:12px" },
              h("div", { class: "sect-h" },
                h("h4", {}, "Đặc tính chiến đấu " + (p.online ? "(đang áp dụng)" : "(tính từ đồ đang mặc)")),
                h("span", { class: "hint", style: "margin:0" }, p.online ? "Đã bao gồm buff & đồ" : "Trạng thái offline")
              ),
              h("div", { class: "stat-grid" }, p.stats.map(([k, v]) => h("div", {}, h("span", { class: "mut" }, k), h("b", {}, num(v)))))
            )
          );
        }

        /* Một popup sửa mọi thông số nhân vật (chỉ gửi phần đã đổi). */
        function editDialog() {
          const n = (v, min, max) => h("input", { type: "number", value: v, min, max, inputmode: "numeric", style: "width:100%" });
          const F = {
            level: n(p.level, p.level, p.maxLevel), exp: n(p.exp, 0, Math.max(0, p.expToLevel - 1)), talentPoints: n(p.talentPoints, 0), stamina: n(p.stamina, 0), pk: n(p.pk, 0, 127),
            gold: n(p.gold, 0), lockedGold: n(p.lockedGold, 0), diamond: n(p.diamond, 0)
          };
          modal("Chỉnh sửa thông số · " + p.name, h("div", {},
            h("div", { class: "sect" },
              h("h4", {}, "Cấp độ & Kinh nghiệm"),
              h("div", { class: "field" },
                h("label", {}, "Cấp độ (chỉ nâng lên, max " + p.maxLevel + ")"), F.level,
                h("label", {}, "EXP trong cấp"), h("div", {}, F.exp, h("div", { class: "hint", style: "margin:4px 0 0" }, "Phạm vi: 0 – " + num(Math.max(0, p.expToLevel - 1))))
              ),
              h("p", { class: "hint", style: "margin:8px 0 0" }, "Nâng cấp sẽ tự động cộng điểm tiềm năng theo logic game.")
            ),
            h("div", { class: "sect" },
              h("h4", {}, "Tiềm năng, Thể lực & Sát khí"),
              h("div", { class: "field" },
                h("label", {}, "Điểm tiềm năng"), F.talentPoints,
                h("label", {}, "Thể lực"), F.stamina,
                h("label", {}, "Điểm sát khí (PK)"), F.pk
              )
            ),
            h("div", { class: "sect" },
              h("h4", {}, "Tài sản tiền tệ"),
              h("div", { class: "field" },
                h("label", {}, "Vàng"), F.gold,
                h("label", {}, "Vàng khoá"), F.lockedGold,
                h("label", {}, "Lam thạch"), F.diamond
              )
            )
          ), {
            actions: [{
              label: "Lưu thay đổi", primary: true, run: async () => {
                const v = k => Math.trunc(+F[k].value), changed = k => v(k) !== Math.trunc(+{ level: p.level, exp: p.exp, talentPoints: p.talentPoints, stamina: p.stamina, pk: p.pk, gold: p.gold, lockedGold: p.lockedGold, diamond: p.diamond }[k]);
                let ok = true, any = false;
                if (changed("level")) { any = true; ok = !!(await act("player/level", { id, level: v("level") })) && ok; }
                const st = {};["exp", "talentPoints", "stamina", "pk"].forEach(k => { if (changed(k)) st[k] = v(k); });
                if (Object.keys(st).length && !changed("level")) { any = true; ok = !!(await act("player/stats", Object.assign({ id }, st))) && ok; }
                else if (Object.keys(st).length) { delete st.exp; if (Object.keys(st).length) { any = true; ok = !!(await act("player/stats", Object.assign({ id }, st))) && ok; } }
                const cur = {};["gold", "lockedGold", "diamond"].forEach(k => { if (changed(k)) cur[k] = v(k); });
                if (Object.keys(cur).length) { any = true; ok = !!(await act("player/currency", Object.assign({ id }, cur))) && ok; }
                if (!any) { toast("Chưa đổi gì."); return true; }
                reload(); return ok;
              }
            }]
          });
        }

        async function teleportDialog() {
          if (!MAPS) { try { MAPS = await api("map-list"); } catch (e) { return toast(e.message, true); } }
          const sel = h("select", { style: "width:100%" }, MAPS.map(m => h("option", { value: m[0], disabled: m[2] }, "#" + m[0] + " " + m[1] + (m[2] ? " (phó bản / chiến trường)" : ""))));
          sel.value = p.mapId;
          const x = h("input", { type: "number", placeholder: "trống = điểm xuất hiện" }), y = h("input", { type: "number", placeholder: "trống = điểm xuất hiện" });
          modal("Dịch chuyển · " + p.name, h("div", {}, h("div", { class: "field" }, h("label", {}, "Map"), sel, h("label", {}, "Toạ độ X"), x, h("label", {}, "Toạ độ Y"), y),
            h("p", { class: "hint", style: "margin-top:10px" }, p.online ? "Đang online: chuyển ngay (bỏ qua hồi chuyển map / khoá nhiệm vụ)." : "Offline: đổi vị trí lưu, lần vào game sau xuất hiện ở đó.")), {
            actions: [{ label: "Dịch chuyển", primary: true, run: async () => !!(await run("player/teleport", { map: +sel.value, x: x.value === "" ? null : +x.value, y: y.value === "" ? null : +y.value })) }]
          });
        }

        async function addItem(where) {
          const it = await editItem("reward", null, "Thêm item vào " + (where === "box" ? "rương" : "hành trang") + " · " + p.name);
          if (it) { delete it.name; delete it.icon; run("player/item/add", { box: where, item: it }); }
        }

        /* Popup 1 món (gọn): thẻ thông tin bên trái; bên phải nút như trong game theo nơi đang để + phím tắt + quản trị. */
        function itemActions(where, it, kind) {
          let close;
          const go = async (fn) => { if (await fn()) close(); };
          const b = (label, fn, cls) => h("button", { class: "btn small" + (cls ? " " + cls : ""), type: "button", onclick: () => go(fn) }, label);
          const acts = [], extra = [];
          if (where === "bag") {
            const quickIdx = p.quick.indexOf(it.slot), col = p.collections.find(c => c.itemType === it.typeId);
            acts.push(b("▶️ Sử dụng", () => itemAct("bag", it.slot, "use", { qty: 1 }), "primary"), b("🛡️ Mặc vào", () => itemAct("bag", it.slot, "equip")),
              b("📦 Cất rương", () => itemAct("bag", it.slot, "store")), b(it.locked ? "🔓 Mở khoá" : "🔒 Khoá", () => itemAct("bag", it.slot, it.locked ? "unlock" : "lock")),
              col ? b("✨ Vào " + col.name.toLowerCase(), () => itemAct("bag", it.slot, "collect")) : null,
              b("💰 Bán", () => itemAct("bag", it.slot, "sell", {}, "Bán " + it.name + (it.qty > 1 ? " x" + it.qty : "") + " (vàng khoá như trong game)?")));
            const quickSel = h("select", {}, QUICK_NAMES.map((n, i) => h("option", { value: i }, "Ô " + n)), h("option", { value: -1 }, "Bỏ phím tắt"));
            quickSel.value = quickIdx >= 0 ? quickIdx : 0;
            extra.push(h("div", { class: "ia-cap" }, "Phím tắt", quickIdx >= 0 ? h("span", { class: "chip ok" }, "Ô " + QUICK_NAMES[quickIdx]) : ""),
              h("div", { class: "ia-row" }, quickSel, b("Gán", () => itemAct("bag", it.slot, "quick", { quick: +quickSel.value }))));
            if (!p.online) extra.push(h("p", { class: "ia-note" }, "Sử dụng / Mặc vào cần nhân vật online."));
          } else if (where === "box") acts.push(b("📤 Lấy ra hành trang", () => itemAct("box", it.slot, "take"), "primary"));
          else if (where === "body") acts.push(b("📥 Tháo ra", () => itemAct("body", it.slot, "unequip"), "primary"));
          else if (where === "back") acts.push(b("📥 Tháo vào hành trang", () => itemAct("back", 0, "take"), "primary"));
          else if (where === "col") acts.push(b(it.selected ? "⏸️ Thôi dùng" : "▶️ Dùng", () => itemAct("col", it.slot, "select", { kind }), "primary"), b("📤 Lấy ra hành trang", () => itemAct("col", it.slot, "take", { kind })));
          let admin = null;
          if (where !== "col") {
            const slot = where === "back" ? 0 : it.slot;
            const qty = h("input", { type: "number", min: 1, max: it.qty, value: it.qty, title: "Số lượng xoá", style: "width:80px" });
            admin = [
              h("div", { class: "ia-cap", style: "margin-top:10px" }, "Quản trị viên GM"),
              h("div", { class: "act-grid", style: "margin-top:6px" },
                where === "body" ? b("💎 Khảm Socket...", async () => { close(); enhanceDialog(it); return false; }, "primary") : null,
                b("✏️ Sửa chỉ số...", async () => { const e = await editItem("edit", it, "Sửa " + it.name); return e ? !!(await run("player/item/edit", { box: where, slot, item: e })) : false; })
              ),
              h("div", { class: "ia-row", style: "margin-top:8px" },
                it.qty > 1 ? qty : null,
                b(it.qty > 1 ? "🗑️ Xoá bớt" : "🗑️ Xoá món", async () => !!(await run("player/item/remove", { box: where, slot, qty: it.qty > 1 ? +qty.value : 0 }, "Xoá " + (it.qty > 1 ? qty.value + " × " : "") + it.name + " của " + p.name + "?")), "danger")
              )
            ];
          }
          close = modal(it.name + (it.stars ? " +" + it.stars : ""), h("div", { class: "item-pop" }, itemCard(it),
            h("div", { class: "item-acts" },
              h("div", { class: "ia-cap" }, "Thao tác người chơi"),
              h("div", { class: "act-grid" }, acts.filter(Boolean)),
              extra,
              admin
            )), { item: true });
        }

        async function enhanceDialog(it) {
          if (!ENHANCE_DATA) {
            try {
              const [charms, stones] = await Promise.all([api("soul-charms"), api("element-stones")]);
              ENHANCE_DATA = { charms, stones };
            } catch (e) { toast(e.message, true); return; }
          }
          const charms = ENHANCE_DATA.charms, stones = ENHANCE_DATA.stones;
          const charmSelect = h("select", { style: "width:100%" }, charms.map(c => h("option", { value: c.id }, c.name + " · #" + c.id)));
          const charmPreview = h("div", { class: "mut", style: "margin-top:8px;line-height:1.7" });
          const showCharm = () => {
            const c = charms.find(x => x.id === +charmSelect.value);
            charmPreview.replaceChildren(
              c ? h("div", { class: "sub-row", style: "margin-bottom:6px" },
                c.icon ? ico(c.icon, 30) : "",
                h("b", {}, c.name)
              ) : "",
              ...(c && c.lines.length ? c.lines.map(line => h("div", { class: "enhance-preview-line" }, "• " + line)) : [h("div", {}, "Shop chưa cấu hình dòng chỉ số cho bùa này.")])
            );
          };
          charmSelect.onchange = showCharm; showCharm();
          const stoneSelect = h("select", { style: "width:100%" }, stones.map(s => h("option", { value: s.id }, s.name + " · #" + s.id)));
          const levelSelect = h("select", { style: "width:100%" }, Array.from({ length: 10 }, (_, i) => h("option", { value: i + 1 }, "Cấp " + (i + 1))));
          const stonePreview = h("div", { class: "hint", style: "margin-top:8px" });
          const showStone = () => {
            const s = stones.find(x => x.id === +stoneSelect.value), lv = s && s.levels.find(x => x.level === +levelSelect.value);
            stonePreview.replaceChildren(
              s ? h("div", { class: "sub-row", style: "margin-bottom:6px" },
                s.icon ? ico(s.icon, 30) : "",
                h("b", {}, s.name + (lv ? " (Cấp " + lv.level + ")" : ""))
              ) : "",
              lv ? h("div", { class: "enhance-preview-line" }, lv.line) : ""
            );
          };
          stoneSelect.onchange = showStone; levelSelect.onchange = showStone; showStone();
          const selectedLabel = h("div", { class: "socket-selection" }, "Chọn một lỗ để thao tác.");
          const selected = { kind: null, index: -1 };
          const targets = [];
          const charmAt = i => (it.charms || [])[i] || null;
          const stoneAt = i => (it.stones || []).find(s => s.hole === i + 1) || null;
          const socketButton = (kind, i, item, limit) => {
            const canSelect = i <= limit;
            const filled = !!item;
            const title = kind === "charm" ? "Lỗ Bùa " : "Lỗ Đá ";
            const button = h("button", {
              type: "button",
              class: "socket-card-v2" + (filled ? " filled" : " empty") + (!canSelect ? " unavailable" : ""),
              disabled: !canSelect,
              onclick: () => selectSocket(kind, i)
            },
              h("div", { class: "socket-header" },
                h("span", { class: "socket-title" }, title + (i + 1)),
                filled
                  ? h("span", { class: "chip sm " + (kind === "charm" ? "ok" : "warn") }, kind === "charm" ? "Đã khảm" : "Cấp " + item.level)
                  : h("span", { class: "chip sm mut-chip" }, canSelect ? "Trống" : "Khoá")
              ),
              h("div", { class: "socket-row" },
                filled && item.icon ? ico(item.icon, 28) : h("span", { class: "socket-plus", style: "width:28px;height:28px;flex:none;font-size:16px" }, canSelect ? "+" : "·"),
                h("div", { class: "socket-body", style: "flex:1;min-width:0" },
                  h("b", { class: "socket-name" }, filled ? item.name : (canSelect ? "+ Chọn lỗ này" : "Chưa mở lỗ")),
                  filled
                    ? h("div", { class: "socket-lines" }, ...(kind === "charm" ? (item.lines || []) : [item.line]).map(line => h("span", {}, line)))
                    : h("div", { class: "socket-hint" }, canSelect ? "Sẵn sàng khảm" : "Mở lỗ trước đó trước")
                )
              )
            );
            targets.push({ kind, index: i, button, filled });
            return button;
          };
          const charmGrid = h("div", { class: "socket-grid-v2" }, Array.from({ length: it.openSockets || 0 }, (_, i) => socketButton("charm", i, charmAt(i), (it.charms || []).length)));
          const stoneGrid = h("div", { class: "socket-grid-v2" }, Array.from({ length: it.openSockets || 0 }, (_, i) => socketButton("stone", i, stoneAt(i), it.holes || 0)));
          function selectSocket(kind, index) {
            selected.kind = kind; selected.index = index;
            targets.forEach(t => t.button.classList.toggle("selected", t.kind === kind && t.index === index));
            const target = targets.find(t => t.kind === kind && t.index === index);
            const what = kind === "charm" ? "bùa linh hồn" : "đá nguyên tố";
            selectedLabel.textContent = "Đang chọn " + what + " · Lỗ " + (index + 1) + (target && target.filled ? " (sẽ thay nội dung lỗ này)" : " (lỗ trống)");
          }
          if (it.openSockets > 0) {
            if ((it.charms || []).length < it.openSockets) selectSocket("charm", (it.charms || []).length);
            else if ((it.holes || 0) < it.openSockets) selectSocket("stone", it.holes || 0);
            else selectSocket("charm", 0);
          }
          const socketCount = h("div", { class: "chips-r" },
            chip("Lỗ mở: " + (it.openSockets || 0) + " (theo +" + (it.stars || 0) + " sao)", "ok"),
            chip("Bùa: " + (it.charmCount || 0) + "/" + (it.openSockets || 0), (it.charmCount || 0) >= (it.openSockets || 0) ? "warn" : "ok"),
            chip("Nguyên tố: " + (it.stoneCount || 0) + "/" + (it.openSockets || 0), (it.stoneCount || 0) >= (it.openSockets || 0) ? "warn" : "ok")
          );
          let closeEnhance, enhancing = false;
          const applyEnhance = async data => {
            if (enhancing) return false;
            enhancing = true;
            try {
              const res = await act("player/item/enhance", Object.assign({ id, slot: it.slot }, data));
              if (!res) return false;
              let row = r.player.body.find(x => x.slot === it.slot);
              let fresh = res.item || null;
              try {
                const latest = await api("player/item?id=" + id + "&box=body&slot=" + it.slot);
                fresh = latest.item || fresh;
              } catch (e) { toast("Đã gửi thao tác nhưng chưa tải được trang bị mới: " + e.message, true); }
              if (row && fresh) row.item = fresh;
              if (fresh) draw();
              closeEnhance();
              if (fresh) await enhanceDialog(fresh);
              return false;
            } finally { enhancing = false; }
          };
          closeEnhance = modal("Khảm Socket · " + it.name + (it.stars ? " +" + it.stars : ""), h("div", { class: "enhance-dialog-v2" },
            h("div", { class: "sect" },
              h("div", { class: "row", style: "justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px" },
                h("h4", { style: "margin:0" }, "Trạng thái Socket trang bị"),
                socketCount
              ),
              h("p", { class: "hint", style: "margin:6px 0 0" }, "Mỗi 2 sao mở 1 lỗ socket (tối đa 5 lỗ). Chọn lỗ muốn thao tác rồi bấm Khảm / Đặt tương ứng.")
            ),
            h("div", { class: "enhance-two-col" },
              h("div", { class: "sect enhance-col" },
                h("div", { class: "sect-h" }, h("h4", {}, "Lỗ bùa linh hồn"), chip("Bùa", "ok")),
                charmGrid.children.length ? charmGrid : h("p", { class: "mut" }, "Trang bị chưa mở lỗ socket nào."),
                h("div", { style: "margin-top:12px" },
                  h("label", { class: "lb" }, "Chọn bùa linh hồn muốn khảm"),
                  charmSelect,
                  charmPreview,
                  h("button", {
                    class: "btn primary",
                    type: "button",
                    style: "width:100%;margin-top:10px",
                    onclick: () => {
                      if (selected.kind !== "charm" || selected.index < 0) { toast("Hãy chọn một lỗ bùa trước.", true); return; }
                      if (!charms.length) { toast("Không có bùa linh hồn nào trong shop.", true); return; }
                      applyEnhance({ action: "charm", template: +charmSelect.value, socket: selected.index });
                    }
                  }, "Khảm bùa random vào lỗ chọn")
                )
              ),
              h("div", { class: "sect enhance-col" },
                h("div", { class: "sect-h" }, h("h4", {}, "Lỗ đá nguyên tố"), chip("Đá nguyên tố", "warn")),
                stoneGrid.children.length ? stoneGrid : h("p", { class: "mut" }, "Trang bị chưa mở lỗ socket nào."),
                h("div", { style: "margin-top:12px" },
                  h("label", { class: "lb" }, "Chọn đá và cấp độ"),
                  h("div", { style: "display:grid;grid-template-columns:1fr 100px;gap:8px" }, stoneSelect, levelSelect),
                  stonePreview,
                  h("button", {
                    class: "btn primary",
                    type: "button",
                    style: "width:100%;margin-top:10px",
                    onclick: () => {
                      if (selected.kind !== "stone" || selected.index < 0) { toast("Hãy chọn một lỗ nguyên tố trước.", true); return; }
                      applyEnhance({ action: "element", template: +stoneSelect.value, level: +levelSelect.value, socket: selected.index });
                    }
                  }, "Đặt đá nguyên tố vào lỗ chọn")
                )
              )
            ),
            h("div", { class: "sect danger", style: "margin-top:12px" },
              h("div", { style: "display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px" },
                h("div", {},
                  h("h4", { style: "margin:0" }, "Thao tác gỡ socket"),
                  selectedLabel
                ),
                h("button", {
                  class: "btn danger",
                  type: "button",
                  onclick: () => {
                    const target = targets.find(t => t.kind === selected.kind && t.index === selected.index);
                    if (!target || !target.filled) { toast("Lỗ đang chọn chưa có khảm để gỡ.", true); return; }
                    const action = selected.kind === "charm" ? "removeCharm" : "removeStone";
                    applyEnhance({ action, index: selected.index });
                  }
                }, "Gỡ tại lỗ đang chọn")
              )
            )
          ), { wide: true });
        }

        /* ---------- Trang bị / Hành trang / Rương / Bộ sưu tập ---------- */
        function bodyView(el) {
          const gear = p.body.map(x => x.item).filter(Boolean);
          const charmCount = gear.reduce((n, x) => n + (x.charmCount || 0), 0), stoneCount = gear.reduce((n, x) => n + (x.stoneCount || 0), 0);
          el.append(h("div", { class: "stat-grid", style: "grid-template-columns:repeat(4,minmax(0,1fr));margin-bottom:14px" },
            ...[["Món đang mặc", gear.length], ["Tổng sao", gear.reduce((n, x) => n + (x.stars || 0), 0)], ["Bùa linh hồn", charmCount], ["Đá nguyên tố", stoneCount]].map(([label, value]) => h("div", {}, h("span", { class: "mut" }, label), h("b", {}, value)))),
            h("div", { class: "two-panels" }, paperdoll(p.body, p, it => itemActions("body", it)),
              h("div", {}, h("div", { class: "sect" }, h("h4", {}, "Quản lý trang bị"),
                h("p", { class: "hint" }, "Chọn một món để khảm bùa random theo shop, đặt cấp đá nguyên tố, sửa dòng chỉ số, hoặc tháo/xoá. Số lỗ khảm mở theo cấp sao của từng món."),
                h("div", { class: "chips-r" }, chip("Nhân vật " + (p.online ? "đang online" : "offline"), p.online ? "ok" : ""), p.trading ? chip("Đang giao dịch", "warn") : "")),
                h("p", { class: "hint" }, "Mặc đồ mới: vào Hành trang → chọn món → Mặc vào (nhân vật online). Đồ đeo lưng nằm ở Bộ sưu tập."))));
        }
        function boxView(where, el) {
          const box = p[where], bySlot = new Map(box.items.map(i => [i.slot, i]));
          const f = h("input", { type: "search", placeholder: "Lọc theo tên item", class: "grow" });
          const holder = h("div");
          const quickMark = s => { const i = where === "bag" ? p.quick.indexOf(s) : -1; return i >= 0 ? QUICK_NAMES[i] : null; };
          const mk = (it) => { const t = tile(it, x => itemActions(where, x)); const q = it && quickMark(it.slot); if (q) t.append(h("span", { class: "qk" }, q)); return t; };
          const paint = () => {
            const q = f.value.trim().toLowerCase();
            const slots = q ? box.items.filter(i => i.name.toLowerCase().includes(q)).map(mk) : Array.from({ length: box.size }, (_, s) => mk(bySlot.get(s) || null));
            holder.replaceChildren(bagPanel(slots, where === "bag" ? "+ Thêm vào túi" : "+ Thêm vào rương", () => addItem(where)));
          };
          f.oninput = paint; paint();
          el.append(h("div", { class: "row" }, f, h("span", { class: "mut" }, box.items.length + " / " + box.size + " ô")), holder);
        }
        function collections(el) {
          const cols = p.collections.map(c => ({ name: c.name, kind: c.kind, items: c.items, selected: c.selected }))
            .concat([{ name: "Đồ đeo lưng", kind: "back", items: p.back ? [p.back] : [], selected: p.back ? 0 : -1 }]);
          const cur = cols.find(c => c.kind === colKind) || cols[0];
          const grid = cur.items.length ? h("div", { class: "grid-items" }, cur.items.map((it, i) => {
            const x = Object.assign({}, it, { selected: i === cur.selected });
            const t = tile(x, y => itemActions(cur.kind === "back" ? "back" : "col", y, cur.kind)); if (i === cur.selected) t.classList.add("sel"); return t;
          })) : h("p", { class: "empty" }, cur.kind === "back" ? "Không đeo gì ở lưng." : "Trống");
          el.append(h("div", { class: "chip-tabs" }, cols.map(c => h("button", { type: "button", class: c === cur ? "on" : "", onclick: () => { colKind = c.kind; draw(); } }, c.name, h("b", {}, c.items.length)))),
            h("div", { class: "sect" }, grid),
            h("p", { class: "hint" }, cur.kind === "back" ? "Đồ đeo lưng đang mặc: bấm để tháo vào hành trang, sửa hoặc xoá."
              : "Khung sáng = đang dùng. Bấm món để dùng / thôi dùng / lấy ra. Cho món mới vào: Hành trang → bấm món → Vào bộ sưu tập."));
        }
        /* ---------- Nhiệm vụ: chính tuyến + hằng ngày ---------- */
        function questView(el) {
          const q = pd.quest, dq = pd.daily;
          const qSel = h("select", { class: "grow" }, q.quests.map(([qid, name]) => h("option", { value: qid }, "NV " + qid + " · " + name))); qSel.value = q.questId;
          const prog = h("input", { type: "number", min: 0, value: q.progress, style: "width:100px" });
          el.append(h("div", { class: "sect" }, h("div", { class: "sect-h" }, h("h4", {}, "Nhiệm vụ chính tuyến"), chip("NV " + q.questId + " · bước " + (q.stage + 1) + "/" + q.steps.length, "ok")),
            h("div", { class: "row" }, qSel, h("button", { class: "btn small primary", onclick: () => progressAct("quest/set", { questId: +qSel.value, stage: 0, progress: 0 }, "Chuyển " + p.name + " sang " + qSel.selectedOptions[0].textContent + " (bước 1)?") }, "Chuyển tới NV")),
            h("div", { class: "tw", style: "max-height:300px" }, h("table", {}, h("thead", {}, h("tr", {}, ["Bước", "Nội dung", "Cần", ""].map(x => h("th", {}, x)))),
              h("tbody", {}, q.steps.map(([k, name, count]) => h("tr", { class: k === q.stage ? "selected" : "" }, h("td", {}, k + 1), h("td", {}, name), h("td", {}, count || "–"),
                h("td", { style: "white-space:nowrap" }, k === q.stage ? [h("span", { class: "mut" }, "Tiến độ "), prog, " ", h("button", { class: "btn small", onclick: () => progressAct("quest/set", { questId: q.questId, stage: k, progress: +prog.value }) }, "Lưu")]
                  : h("button", { class: "btn small", onclick: () => progressAct("quest/set", { questId: q.questId, stage: k, progress: 0 }, "Đặt " + p.name + " vào bước " + (k + 1) + "?") }, "Tới bước này"))))))),
            h("p", { class: "hint" }, "Online: chuyển nhiệm vụ / bước chạy như lệnh /nv (mũi tên, hội thoại NPC cập nhật ngay).")));
          const pts = h("input", { type: "number", min: 0, value: dq.points, style: "width:90px" }), add = h("input", { type: "number", min: 0, value: dq.added, style: "width:70px" }), sw = h("input", { type: "number", min: 0, value: dq.swaps, style: "width:70px" });
          el.append(h("div", { class: "sect" }, h("div", { class: "sect-h" }, h("h4", {}, "Nhiệm vụ hằng ngày · " + dq.dayText),
            h("button", { class: "btn small danger", onclick: () => progressAct("daily/reset", {}, "Xoá nhiệm vụ hằng ngày của " + p.name + "? Lần mở bảng tới sẽ giao 10 nhiệm vụ mới.") }, "Giao lại từ đầu")),
            h("div", { class: "row" }, h("label", { class: "mut" }, "Điểm chăm chỉ tuần"), pts, h("label", { class: "mut" }, "Đã nhận NV mới"), add, h("label", { class: "mut" }, "Đã đổi NV"), sw,
              h("button", { class: "btn small", onclick: () => progressAct("daily/points", { points: +pts.value, added: +add.value, swaps: +sw.value }) }, "Lưu")),
            dq.tasks.length ? h("div", { class: "tw" }, h("table", {}, h("thead", {}, h("tr", {}, ["#", "Nhiệm vụ", "Tiến độ", "Đã nhận quà", ""].map(x => h("th", {}, x)))),
              h("tbody", {}, dq.tasks.map(t => {
                const pr = h("input", { type: "number", min: 0, max: t.goal, value: t.progress, style: "width:80px" }), cl = h("input", { type: "checkbox" }); cl.checked = t.claimed;
                return h("tr", {}, h("td", {}, t.index + 1), h("td", {}, t.name, h("div", { class: "mut", style: "font-size:11px" }, t.kind)),
                  h("td", { style: "white-space:nowrap" }, pr, " / " + num(t.goal), " ", h("button", { class: "btn small", onclick: () => { pr.value = t.goal; } }, "Xong")),
                  h("td", {}, cl), h("td", {}, h("button", { class: "btn small primary", onclick: () => progressAct("daily/task", { index: t.index, progress: +pr.value, claimed: cl.checked }) }, "Lưu")));
              })))) : h("p", { class: "mut" }, "Chưa giao nhiệm vụ hôm nay (giao khi người chơi mở bảng / vào game).")));
        }
        /* ---------- Thành tựu ---------- */
        function achView(el) {
          const a = pd.achievements;
          el.append(h("div", { class: "sect" }, h("h4", {}, "Bộ đếm"), h("div", { class: "ach-counts" }, a.types.map(t => {
            const inp = h("input", { type: "number", min: 0, value: t.count });
            return h("div", {}, h("span", {}, ACH_TYPES[t.key] || t.key, t.max ? h("small", { class: "mut" }, " (giá trị cao nhất)") : ""), inp,
              h("button", { class: "btn small", onclick: () => progressAct("achievement/count", { type: t.key, value: +inp.value }) }, "Lưu"));
          }))));
          const f = h("select", {}, h("option", { value: "" }, "Mọi trạng thái"), ACH_STATUS.map((s, i) => h("option", { value: i }, s[0])));
          const tb = h("tbody");
          const paint = () => fill(tb, a.items.filter(x => f.value === "" || x.status === +f.value).map(x => h("tr", {},
            h("td", {}, h("b", {}, x.title), h("div", { class: "mut", style: "font-size:11px" }, "#" + x.id + " · " + (ACH_TYPES[x.type] || x.type))),
            h("td", {}, num(x.progress) + " / " + num(x.target)), h("td", {}, chip(ACH_STATUS[x.status][0], ACH_STATUS[x.status][1])),
            h("td", {}, x.status === 2 ? h("button", { class: "btn small", onclick: () => progressAct("achievement/claim", { achievementId: x.id, claimed: false }, "Cho " + p.name + " nhận lại quà \"" + x.title + "\"?") }, "Cho nhận lại")
              : h("button", { class: "btn small", onclick: () => progressAct("achievement/claim", { achievementId: x.id, claimed: true }) }, "Đánh dấu đã nhận")))), "Không có");
          f.onchange = paint; paint();
          el.append(h("div", { class: "sect" }, h("div", { class: "sect-h" }, h("h4", {}, "Danh sách thành tựu"), f), h("div", { class: "tw", style: "max-height:380px" }, h("table", {}, h("thead", {}, h("tr", {}, ["Thành tựu", "Tiến độ", "Trạng thái", ""].map(x => h("th", {}, x)))), tb))),
            h("p", { class: "hint" }, "Tiến độ = bộ đếm của loại so với mốc. Đánh dấu đã nhận không phát quà; cho nhận lại thì người chơi bấm Nhận trong game."));
        }
        /* ---------- Hiệu ứng ---------- */
        function buffs(el) {
          const setTime = async (item, name, minutes) => { const m = await prompt2("Thời gian · " + name, "Còn bao nhiêu phút (tính từ bây giờ)", minutes, "number"); if (m != null && +m > 0) run("player/buff/add", { item, minutes: +m }); };
          const list = h("div", { class: "list" }, p.buffs.length ? p.buffs.map(bf => h("div", { class: "li static" },
            h("div", { class: "main" }, h("div", { class: "t" }, bf.name), h("div", { class: "s" }, bf.description || ""), h("div", { class: "s" }, "Hết hạn " + bf.until + " · còn " + dur(bf.secondsLeft))),
            h("div", { class: "chips-r" }, h("button", { class: "btn small", onclick: () => setTime(bf.item, bf.name, Math.max(1, Math.round(bf.secondsLeft / 60))) }, "Sửa thời gian"),
              h("button", { class: "btn small danger", onclick: () => run("player/buff/remove", { option: bf.option }, "Gỡ " + bf.name + "?") }, "Gỡ"))))
            : h("p", { class: "mut" }, "Không có hiệu ứng nào."));
          const sel = h("select", { class: "grow" }), mins = h("input", { type: "number", min: 1, style: "width:90px", placeholder: "phút", inputmode: "numeric" });
          const fillSel = () => { sel.replaceChildren(...BUFFS.map(x => h("option", { value: x.item }, x.name + " · " + num(x.minutes) + " phút"))); mins.value = BUFFS.length ? BUFFS[0].minutes : ""; };
          sel.onchange = () => { const x = BUFFS.find(z => z.item == sel.value); if (x) mins.value = x.minutes; };
          if (BUFFS) fillSel(); else api("buffs").then(l => { BUFFS = l; fillSel(); }).catch(e => toast(e.message, true));
          el.append(list, h("div", { class: "sect", style: "margin-top:12px" }, h("h4", {}, "Bật hiệu ứng trực tiếp (không cần item)"),
            h("div", { class: "row" }, sel, mins, h("span", { class: "mut" }, "phút"), h("button", { class: "btn small primary", onclick: () => run("player/buff/add", { item: +sel.value, minutes: +mins.value }) }, "Bật")),
            h("p", { class: "hint", style: "margin:0" }, "Cùng nhóm thì đè hiệu ứng cũ (Trí tuệ I/II, Sức mạnh I/II, Cứng cáp I/II).")));
        }
        /* ---------- Kỹ năng ---------- */
        function skills(el) {
          el.append(h("div", { class: "list" }, p.skills.map(s => h("div", { class: "li static" }, h("div", { class: "main" }, h("div", { class: "t" }, s.name), h("div", { class: "s" }, "ID " + s.id + " · tối đa cấp " + s.maxLevel)),
            h("div", { class: "chips-r" }, chip("Cấp " + s.level, "ok"), h("button", { class: "btn small", onclick: async () => { const lv = await prompt2(s.name, "Cấp mới (0-" + s.maxLevel + ")", s.level, "number"); if (lv != null) run("player/skill", { skill: s.id, level: +lv }); } }, "Sửa cấp"))))));
        }
      }
      draw();
    }
    Tabs.add({
      id: "players", icon: "players", group: "Điều hành", label: "Người chơi", poll: 15000,
      html: `
<div class="ph">
  <div>
    <h1>Quản lý Người chơi</h1>
    <p class="sub">Xem danh sách nhân vật online hoặc tìm kiếm theo tên, tài khoản, ID. Bấm vào nhân vật để quản lý chi tiết.</p>
  </div>
</div>
<div class="card p-main-card">
  <div class="p-search-toolbar">
    <div class="p-search-input-wrap">
      <input id="pQ" class="grow" type="search" placeholder="🔍 Nhập tên nhân vật, tên tài khoản hoặc ID nhân vật..." autocomplete="off">
      <button class="btn primary" id="pSearch" type="button">🔍 Tìm kiếm</button>
      <button class="btn" id="pResetSearch" type="button" style="display:none">✕ Xoá tìm</button>
    </div>
    <div class="p-mode-segmented" id="pMode">
      <button type="button" class="on" data-m="online">🟢 Đang online <span id="pOnCount"></span></button>
      <button type="button" data-m="search">🔍 Kết quả tìm <span id="pFoundCount"></span></button>
    </div>
  </div>
  <div class="p-quick-filter-row" id="pFilterRow">
    <input id="pOnFilter" type="search" placeholder="⚡ Lọc nhanh danh sách đang hiển thị theo tên, bản đồ hoặc IP...">
    <span class="mut" id="pShownCount" style="font-size:12px;white-space:nowrap"></span>
  </div>
  <div class="p-player-list" id="pList"></div>
</div>`,
      setup(root) {
        let online = [], onlineTotal = 0, found = [], mode = "online";
        const setMode = m => {
          mode = m;
          [...$("pMode").children].forEach(b => b.classList.toggle("on", b.dataset.m === m));
          $("pResetSearch").style.display = (m === "search" || $("pQ").value.trim()) ? "inline-flex" : "none";
          render();
        };
        [...$("pMode").children].forEach(b => b.onclick = () => setMode(b.dataset.m));
        $("pResetSearch").onclick = () => {
          $("pQ").value = "";
          found = [];
          setMode("online");
        };

        const row = p => {
          const isOnline = p.online || mode === "online";
          return h("div", {
            class: "p-player-card" + (isOnline ? " is-online" : " is-offline"),
            tabindex: 0,
            role: "button",
            onclick: () => openPlayer(p.id),
            onkeydown: e => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openPlayer(p.id); } }
          },
            h("div", { class: "p-av-wrap" },
              h("div", { class: "p-av" + (isOnline ? " on" : "") }, p.name.slice(0, 1).toUpperCase()),
              isOnline ? h("span", { class: "p-status-dot", title: "Đang online" }) : ""
            ),
            h("div", { class: "p-info-main" },
              h("div", { class: "p-title-row" },
                h("span", { class: "p-name" }, p.name),
                h("span", { class: "p-id" }, "#" + p.id),
                h("span", { class: "chip sm" }, "Cấp " + p.level),
                p.cls ? h("span", { class: "chip sm" }, p.cls) : "",
                p.gender ? h("span", { class: "chip sm mut-chip" }, p.gender) : ""
              ),
              h("div", { class: "p-sub-row" },
                h("span", { class: "p-loc" }, "📍 ", h("b", {}, p.map || "–"), p.zone != null && p.zone >= 0 ? " (Khu " + p.zone + ")" : ""),
                h("span", { class: "p-conn" },
                  mode === "online"
                    ? ("🌐 IP: " + (p.ip || "–") + " · ⏱️ " + dur(p.session))
                    : ("👤 TK: " + (p.username || "–"))
                )
              )
            ),
            h("div", { class: "p-action-col" },
              p.banned ? chip("🔒 Khoá", "bad") : isOnline ? chip("🟢 Online", "ok") : chip("⚪ Offline"),
              p.trading ? chip("🔄 Giao dịch", "warn") : "",
              h("button", { class: "btn small", onclick: (e) => { e.stopPropagation(); openPlayer(p.id); } }, "⚙️ Quản lý")
            )
          );
        };

        function render() {
          const f = $("pOnFilter").value.trim().toLowerCase();
          const rows = mode === "online"
            ? online.filter(p => !f || (p.name + " " + p.map + " " + (p.ip || "")).toLowerCase().includes(f)).slice(0, 400)
            : found.filter(p => !f || (p.name + " " + (p.username || "") + " " + (p.map || "")).toLowerCase().includes(f));
          
          $("pList").replaceChildren(...(rows.length ? rows.map(row) : [
            h("p", { class: "empty", style: "padding:24px 10px" },
              mode === "online"
                ? (f ? "Không có người chơi online phù hợp với bộ lọc." : "Hiện không có ai đang online.")
                : (found.length ? "Không có kết quả khớp với bộ lọc." : "Chưa có kết quả tìm kiếm — nhập tên, tài khoản hoặc ID rồi bấm Tìm kiếm.")
            )
          ]));
          $("pOnCount").textContent = "(" + num(onlineTotal) + ")";
          $("pFoundCount").textContent = found.length ? "(" + found.length + ")" : "";
          $("pShownCount").textContent = "Hiển thị: " + rows.length + " nhân vật";
        }

        async function loadOnline() {
          try { const r = await api("players/online"); online = r.rows; onlineTotal = r.total; render(); } catch(e) {}
        }
        async function doSearch() {
          const q = $("pQ").value.trim();
          if (!q) { toast("Vui lòng nhập từ khoá tìm kiếm."); return; }
          try {
            found = await api("players/search?q=" + encodeURIComponent(q));
            setMode("search");
            if (!found.length) toast("Không tìm thấy nhân vật nào phù hợp.");
          } catch (e) { toast(e.message, true); }
        }
        let filterTimer = null;
        $("pOnFilter").oninput = () => { clearTimeout(filterTimer); filterTimer = setTimeout(render, 140); };
        $("pSearch").onclick = doSearch;
        $("pQ").onkeydown = e => { if (e.key === "Enter") doSearch(); };
        playersChanged = () => loadOnline().catch(() => { });
        return { refresh: loadOnline };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Bot người chơi ================= */
    Tabs.add({
      id: "bots", icon: "players", group: "Điều hành", label: "Bot người chơi", poll: 10000,
      html: `<div class="bt-hero">
    <div class="bt-hero-main"><h1>Bot người chơi</h1>
      <p class="sub">Theo dõi bot đang online, hoạt động theo khu và điều chỉnh cấu hình.</p></div>
    <div class="bt-hero-side"><span class="bt-pill" id="btPill">–</span>
      <button class="btn primary" id="btToggle">–</button><button class="btn danger" id="btKickAll">Cho thoát hết</button>
      <button class="btn" id="btRefresh" type="button" title="Làm mới dữ liệu">Làm mới</button></div>
  </div>
  <div class="bt-runtime" id="btRuntime" role="group" aria-label="Trạng thái bot">
    <span class="bt-runtime-mark"></span><span id="btRuntimeText">Đang tải trạng thái…</span>
    <span class="bt-runtime-fill" role="progressbar" aria-label="Bot theo mục tiêu cấu hình" aria-valuemin="0" aria-valuemax="100"><i id="btRuntimeBar"></i></span>
    <small class="bt-runtime-target" id="btRuntimeTarget"></small><small class="bt-updated" id="btUpdated"></small>
  </div>
  <div class="bt-kpis">
    <div class="bt-kpi"><small>Bot online</small><b id="btOn">–</b><em id="btOnSub">&nbsp;</em></div>
    <button type="button" class="bt-kpi bt-kpi-action" id="btAwakeCard" title="Mở danh sách bot đang hoạt động"><small>Đang hoạt động</small><b id="btAwake">–</b><em id="btAwakeSub">khu có người thật</em></button>
    <div class="bt-kpi"><small>Trong tổ đội</small><b id="btParty">–</b><em id="btPartySub">&nbsp;</em></div>
    <div class="bt-kpi"><small>Lượt bị PK</small><b id="btPk">–</b><em>tính từ lúc mở server</em></div>
  </div>
  <div class="event-switcher bt-tabs" role="tablist" aria-label="Bot người chơi">
    <button type="button" class="active" id="btTabOverview" data-bt-panel="overview" role="tab" aria-selected="true" aria-controls="btPanelOverview">Tổng quan</button>
    <button type="button" id="btTabList" data-bt-panel="list" role="tab" aria-selected="false" aria-controls="btPanelList">Danh sách<span class="ev-badge" id="btListBadge">0</span></button>
    <button type="button" id="btTabSettings" data-bt-panel="settings" role="tab" aria-selected="false" aria-controls="btPanelSettings">Cài đặt<span class="ev-badge bt-dirty-badge" id="btDirtyBadge" hidden>chưa lưu</span></button>
  </div>
  <div class="bt-panel" id="btPanelOverview" role="tabpanel" aria-labelledby="btTabOverview">
    <div class="c-half">
      <div class="card"><h2>Cơ cấu bot</h2><p class="hint">Bấm 1 loại để xem danh sách bot loại đó.</p>
        <div class="bt-stack" id="btStack"></div><div class="bt-legend" id="btLegend"></div></div>
      <div class="card"><h2>Theo map</h2><p class="hint">Bấm một map để lọc danh sách. Bot ngủ khi khu không có người thật.</p>
        <div class="bt-maps" id="btMaps"></div></div>
    </div>
  </div>
  <div class="bt-panel" id="btPanelList" role="tabpanel" aria-labelledby="btTabList" hidden>
    <div class="card">
      <div class="bt-toolbar"><div class="bt-seg" id="btKindSeg" aria-label="Lọc theo loại bot"></div><span class="bt-toolbar-spacer"></span>
        <label class="bt-check"><input type="checkbox" id="btOnlyAwake"> Chỉ đang hoạt động</label>
        <label class="bt-search"><span aria-hidden="true">⌕</span><input id="btQ" type="search" placeholder="Tìm tên, map hoặc bãi" aria-label="Tìm tên, map hoặc bãi"><button type="button" id="btClearQ" aria-label="Xoá nội dung tìm kiếm" title="Xoá tìm kiếm" hidden>×</button></label></div>
      <div class="mut bt-count" id="btCount"></div>
      <div class="bt-rows" id="btRows"></div>
    </div>
  </div>
  <div class="bt-panel" id="btPanelSettings" role="tabpanel" aria-labelledby="btTabSettings" hidden>
    <div class="bt-settings" id="btForm"></div>
    <div class="bt-savebar" id="btSaveBar"><span id="btSaveText">Không có thay đổi.</span><span style="flex:1"></span>
      <button class="btn" id="btUndo" disabled>Hoàn tác</button><button class="btn primary" id="btSave" disabled>Lưu cài đặt</button></div>
    <p class="hint" style="margin-top:8px">Lưu vào <code>tlnt_data.bot_profile</code>. Món bộ sưu tập + dòng chỉ số bot dùng và câu chat: <a href="#data">Dữ liệu game › Bot người chơi</a>.</p>
  </div>`,
      setup(root) {
        const KCOLOR = { offline: "#0f766e", auto: "#2563eb", boss: "#dc2626", fish: "#0ea5e9", idle: "#64748b", wander: "#a855f7", afk: "#a3a3a3", travel: "#d97706", spar: "#e11d48" };
        const CARDS = [
          ["Chung", "Số lượng, cấp độ và thời gian online.", [
            ["chat", "Thỉnh thoảng chat khu", "bool"], ["pkFightBack", "Bị PK thì đánh trả / bỏ chạy", "bool"],
            ["count", "Số bot online", "num", 0, 5000], ["minLevel", "Cấp thấp nhất", "num", 1, 100], ["maxLevel", "Cấp cao nhất", "num", 1, 100],
            ["sessionMin", "Online ít nhất (phút)", "num", 5, 1440], ["sessionMax", "Online nhiều nhất (phút)", "num", 5, 1440], ["spawnPerSecond", "Vào / ra mỗi giây", "num", 1, 50]]],
          ["Tỉ lệ loại bot", "Phần còn lại = đứng chơi / đi dạo.", [
            ["grindPercent", "Đi up", "range", 0, 100], ["autoPercent", "… trong đó kiểu Auto đánh", "range", 0, 100],
            ["fishPercent", "Câu cá", "range", 0, 100], ["afkPercent", "AFK", "range", 0, 100]], "mix"],
          ["Hành vi", "Mỗi khu tự lấy trần bot ngẫu nhiên từ 50–100% mức cấu hình.", [
            ["partyPercent", "Bot up đi theo nhóm", "range", 0, 100], ["zonePercent", "Mức trần bot trong 1 khu", "range", 5, 90],
            ["bossHunters", "Bot săn mỗi boss thế giới", "num", 0, 20]]],
          ["Bộ sưu tập", "% bot có 1 món mỗi loại (cấp thấp ít hơn).", [
            ["costumeChance", "Ngoại trang", "range", 0, 100], ["titleChance", "Danh hiệu", "range", 0, 100], ["petChance", "Thú cưng", "range", 0, 100],
            ["maskChance", "Mặt nạ", "range", 0, 100], ["backChance", "Đeo lưng", "range", 0, 100]]]];
        const F = CARDS.flatMap(c => c[2]);
        let data = null, saved = null, dirty = false, panel = "overview", kindFilter = "", qTimer = null, requestSeq = 0;

        /* ---- tab con */
        const subTabs = [...root.querySelectorAll("[data-bt-panel]")];
        subTabs.forEach((b, i) => {
          b.onclick = () => setPanel(b.dataset.btPanel);
          b.onkeydown = e => {
            if (!["ArrowRight", "ArrowLeft", "Home", "End"].includes(e.key)) return;
            e.preventDefault();
            const next = e.key === "Home" ? 0 : e.key === "End" ? subTabs.length - 1 : (i + (e.key === "ArrowRight" ? 1 : subTabs.length - 1)) % subTabs.length;
            setPanel(subTabs[next].dataset.btPanel);
            subTabs[next].focus();
          };
        });
        function setPanel(p) {
          panel = p;
          root.querySelectorAll("[data-bt-panel]").forEach(b => {
            const active = b.dataset.btPanel === p;
            b.classList.toggle("active", active);
            b.setAttribute("aria-selected", active ? "true" : "false");
            b.tabIndex = active ? 0 : -1;
          });
          $("btPanelOverview").hidden = p !== "overview"; $("btPanelList").hidden = p !== "list"; $("btPanelSettings").hidden = p !== "settings";
          if (p === "list") load().catch(() => { });
        }

        /* ---- cài đặt */
        $("btForm").append(...CARDS.map(([title, hint, fields, extra]) => h("div", { class: "card bt-set" }, h("h2", {}, title), h("p", { class: "hint" }, hint),
          ...fields.map(([k, label, type, min, max]) => {
            if (type === "bool") return h("label", { class: "bt-switch" }, h("input", { type: "checkbox", id: "bt_" + k, onchange: changed }), h("i"), h("span", {}, label));
            if (type === "range") return h("label", { class: "bt-range" }, h("span", {}, label), h("b", { id: "btv_" + k }, ""),
              h("input", { type: "range", id: "bt_" + k, min, max, oninput: changed }));
            return h("label", { class: "bt-num" }, h("span", {}, label), h("input", { type: "number", id: "bt_" + k, min, max, oninput: changed }));
          }),
          extra === "mix" ? h("div", { class: "bt-mix" }, h("div", { class: "bt-stack small", id: "btMixBar" }), h("div", { class: "bt-mix-text", id: "btMix" })) : "")));
        const read = () => { const o = {}; F.forEach(([k, , t]) => { const el = $("bt_" + k); o[k] = t === "bool" ? el.checked : Number(el.value); }); return o; };
        function write(c) { F.forEach(([k, , t]) => { const el = $("bt_" + k); if (t === "bool") el.checked = !!c[k]; else el.value = c[k]; }); refreshForm(); }
        function refreshForm() {
          const v = read();
          F.forEach(([k, , t]) => { if (t === "range") $("btv_" + k).textContent = v[k] + "%"; });
          const g = v.grindPercent, f = Math.min(v.fishPercent, 100 - g), a = Math.min(v.afkPercent, 100 - g - f), idle = Math.max(0, 100 - g - f - a);
          const parts = [["auto", "Auto đánh", Math.round(g * v.autoPercent / 100)], ["offline", "Treo offline", g - Math.round(g * v.autoPercent / 100)],
            ["fish", "Câu cá", f], ["afk", "AFK", a], ["idle", "Đứng / đi dạo", idle]];
          const target = Math.max(0, v.count || 0);
          $("btMixBar").replaceChildren(...parts.filter(p => p[2] > 0).map(([k, l, n]) => h("i", { style: `width:${n}%;background:${KCOLOR[k]}`, title: l + " " + n + "%" })));
          $("btMix").replaceChildren(...parts.map(([k, l, n]) => h("span", {}, h("i", { style: "background:" + KCOLOR[k] }), l + " " + n + "%", h("small", {}, "≈ " + num(Math.round(target * n / 100))))),
            g + v.fishPercent + v.afkPercent > 100 ? h("span", { class: "bad-t" }, "Tổng vượt 100%: bot câu cá / AFK được giảm theo thứ tự.") : "");
          const valid = F.filter(([, , t]) => t !== "bool").every(([k, , , min, max]) => {
            const el = $("bt_" + k), value = Number(el.value);
            return el.value !== "" && Number.isFinite(value) && value >= min && value <= max;
          })
            && v.minLevel <= v.maxLevel && v.sessionMin <= v.sessionMax;
          $("btForm").classList.toggle("bt-invalid", !valid);
          $("btSaveBar").classList.toggle("invalid", !valid);
          $("btSave").disabled = !dirty || !valid;
          $("btSaveText").textContent = !valid ? (v.minLevel > v.maxLevel ? "Cấp thấp nhất phải nhỏ hơn hoặc bằng cấp cao nhất." :
            v.sessionMin > v.sessionMax ? "Thời gian online tối thiểu phải nhỏ hơn hoặc bằng thời gian tối đa." : "Vui lòng nhập đủ các giá trị.")
            : dirty ? "Có thay đổi chưa lưu." : "Không có thay đổi.";
        }
        function changed() {
          dirty = saved && JSON.stringify(read()) !== JSON.stringify(Object.fromEntries(F.map(([k]) => [k, saved[k]])));
          refreshForm();
          $("btUndo").disabled = !dirty;
          $("btSaveBar").classList.toggle("dirty", !!dirty);
          $("btDirtyBadge").hidden = !dirty;
        }
        $("btUndo").onclick = () => { write(saved); changed(); };
        $("btSave").onclick = async () => {
          const r = await act("bots/config", Object.assign({}, data.config, read()));
          if (r) { saved = r.config; data.config = r.config; write(saved); changed(); renderHead(); setTimeout(load, 1500); }
        };

        /* ---- đầu trang */
        function renderHead() {
          const c = data.config;
          $("btPill").textContent = c.enabled ? "● Đang bật" : "○ Đang tắt";
          $("btPill").className = "bt-pill " + (data.ready ? (c.enabled ? "on" : "off") : "wait");
          if (!data.ready) $("btPill").textContent = "◷ Đang khởi tạo";
          $("btToggle").textContent = c.enabled ? "Tắt bot" : "Bật bot";
          $("btToggle").className = "btn " + (c.enabled ? "" : "primary");
          $("btToggle").disabled = !data.ready;
          $("btKickAll").disabled = data.online === 0;
          const bossCount = (data.kinds.find(k => k.key === "boss") || {}).count || 0;
          const regularOnline = Math.max(0, data.online - bossCount);
          $("btOn").textContent = num(data.online);
          $("btOnSub").textContent = (c.enabled ? "mục tiêu " + num(c.count) + " + " + num(bossCount) + " săn boss" : "đã tắt") + " · vào " + num(data.spawnedTotal) + " lượt";
          $("btAwake").textContent = num(data.awake);
          $("btAwakeSub").textContent = (data.online ? Math.round(data.awake * 100 / data.online) : 0) + "% bot ở khu có người thật · bấm để xem";
          $("btParty").textContent = num(data.inParty);
          $("btPartySub").textContent = num(data.partiesMade) + " tổ đội đã lập";
          $("btPk").textContent = num(data.pkFights);
          $("btListBadge").textContent = num(data.online);
          const runtime = $("btRuntime");
          runtime.classList.toggle("is-off", !c.enabled);
          runtime.classList.toggle("is-wait", !data.ready);
          $("btRuntimeText").textContent = !data.ready ? "Bot chưa sẵn sàng — đang nạp dữ liệu cấu hình." : c.enabled ? "Hệ thống bot đang chạy." : "Hệ thống bot đang tắt.";
          const target = c.enabled ? c.count : 0;
          const fill = target > 0 ? Math.min(100, regularOnline * 100 / target) : 0;
          $("btRuntimeBar").style.width = fill + "%";
          $("btRuntimeBar").parentElement.setAttribute("aria-valuenow", String(Math.round(fill)));
          $("btRuntimeTarget").textContent = c.enabled ? num(regularOnline) + " / " + num(target) + " bot mục tiêu" + (bossCount ? " · " + num(bossCount) + " săn boss" : "") : num(data.online) + " bot còn lại";
          $("btUpdated").textContent = "Cập nhật " + new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
        }
        $("btToggle").onclick = async () => {
          if (!data) return;
          const on = !data.config.enabled;
          if (!on && !(await ask("Tắt bot? Bot sẽ thoát dần " + data.config.spawnPerSecond + " bot mỗi giây."))) return;
          const r = await act("bots/config", Object.assign({}, data.config, { enabled: on }));
          if (r) { data.config = r.config; if (!dirty) { saved = r.config; write(saved); } renderHead(); setTimeout(load, 1500); }
        };
        $("btKickAll").onclick = async () => { if (await act("bots/kick-all", {}, "Cho mọi bot thoát ngay? (đang bật thì bot mới sẽ vào dần lại)")) setTimeout(load, 1000); };
        $("btRefresh").onclick = () => load().catch(e => toast(e.message, true));
        $("btAwakeCard").onclick = () => { $("btOnlyAwake").checked = true; kindFilter = ""; setPanel("list"); };

        /* ---- tổng quan */
        function renderOverview() {
          const total = Math.max(1, data.online);
          const kinds = data.kinds.filter(k => k.count > 0);
          $("btStack").replaceChildren(...kinds.map(k => h("i", { style: `width:${k.count * 100 / total}%;background:${KCOLOR[k.key]}`, title: k.label + ": " + k.count })));
          $("btLegend").replaceChildren(...data.kinds.map(k => h("button", { type: "button", class: "bt-leg", onclick: () => { kindFilter = k.key; setPanel("list"); } },
            h("i", { style: "background:" + KCOLOR[k.key] }), h("span", {}, k.label), h("b", {}, num(k.count)),
            h("small", {}, (k.count ? Math.round(k.count * 100 / total) + "%" : "–") + (k.awake ? " · " + k.awake + " hoạt động" : "")))));
          const maps = [...data.maps].sort((a, b) => (b.grind + b.idle) - (a.grind + a.idle));
          const max = Math.max(1, ...maps.map(m => m.grind + m.idle));
          $("btMaps").replaceChildren(...(maps.length ? maps.map(m => h("button", { type: "button", class: "bt-map", title: "Lọc bot ở " + m.name, onclick: () => {
            kindFilter = ""; $("btOnlyAwake").checked = false; $("btQ").value = m.name; $("btClearQ").hidden = false; setPanel("list");
          } },
            h("span", { class: "bt-map-name" }, m.name),
            h("span", { class: "bt-map-bar" }, h("i", { class: "g", style: "width:" + m.grind * 100 / max + "%" }), h("i", { class: "o", style: "width:" + m.idle * 100 / max + "%" })),
            h("span", { class: "bt-map-n" }, num(m.grind + m.idle)),
            m.awake ? chip(num(m.awake) + " hoạt động", "ok") : h("span", { class: "mut bt-map-sleep" }, "ngủ"))) : [h("p", { class: "empty" }, "Chưa có bot")]));
        }

        /* ---- danh sách */
        function renderList() {
          $("btKindSeg").replaceChildren(h("button", { type: "button", class: kindFilter ? "" : "on", onclick: () => { kindFilter = ""; load(); } }, "Tất cả"),
            ...data.kinds.map(k => h("button", { type: "button", class: kindFilter === k.key ? "on" : "", onclick: () => { kindFilter = k.key; load(); } },
              h("i", { style: "background:" + KCOLOR[k.key] }), k.label, h("small", {}, num(k.count)))));
          const only = $("btOnlyAwake").checked;
          const rows = data.bots.filter(b => !only || b.awake);
          $("btCount").textContent = num(data.matched) + " bot" + (data.matched > data.bots.length ? " · hiện " + data.bots.length + " (đang hoạt động trước, cấp cao trước)" : "")
            + (only ? " · lọc: " + rows.length + " đang hoạt động" : "");
          $("btRows").replaceChildren(...(rows.length ? rows.map(b => h("div", { class: "bt-row" + (b.awake ? " awake" : ""), onclick: () => detail(b.id) },
            h("div", { class: "bt-av", style: "background:" + KCOLOR[b.kind] }, b.name.slice(0, 1).toUpperCase()),
            h("div", { class: "bt-who" }, h("b", {}, b.name, b.dead ? " 💀" : "", b.pk ? " ⚔️" : ""),
              h("small", {}, "Lv " + b.level, b.gained > 0 ? h("span", { class: "ok-t" }, " +" + b.gained) : "", " · " + b.cls + " · EXP " + String(b.expPct).replace(".", ",") + "%")),
            h("button", { type: "button", class: "bt-kchip", style: "color:" + KCOLOR[b.kind] + ";border-color:" + KCOLOR[b.kind], "aria-label": "Xem chi tiết " + b.name, onclick: e => { e.stopPropagation(); detail(b.id); } }, b.kindLabel),
            h("div", { class: "bt-where" }, h("span", {}, b.map + (b.zone >= 0 ? " · khu " + b.zone : "")), h("small", {}, b.place)),
            h("div", { class: "bt-meta" }, b.party ? h("span", { title: "Tổ đội" }, "👥 " + b.party) : "", h("span", { title: "Còn online" }, "⏱ " + b.minutesLeft + "p"),
              b.awake ? h("span", { class: "ok-t" }, "● hoạt động") : h("span", { class: "mut" }, "ngủ")),
            h("button", { class: "btn small", onclick: async e => { e.stopPropagation(); if (await act("bots/kick", { id: b.id })) setTimeout(load, 700); } }, "Thoát")))
            : [h("p", { class: "empty" }, data.ready ? "Không có bot phù hợp." : "Đang nạp…")]));
        }
        $("btOnlyAwake").onchange = () => data && renderList();
        $("btQ").oninput = () => { $("btClearQ").hidden = !$("btQ").value; clearTimeout(qTimer); qTimer = setTimeout(() => load().catch(() => { }), 250); };
        $("btClearQ").onclick = () => { $("btQ").value = ""; $("btClearQ").hidden = true; load().catch(e => toast(e.message, true)); $("btQ").focus(); };

        async function detail(id) {
          let r; try { r = (await api("bots/detail?id=" + id)).bot; } catch (e) { return toast(e.message, true); }
          const grid = rows => h("div", { class: "kv" }, rows.flatMap(([k, v]) => kv(k, v)));
          const table = (head, rows) => h("div", { class: "tw", style: "max-height:260px" }, h("table", {}, h("thead", {}, h("tr", {}, head.map(x => h("th", {}, x)))),
            h("tbody", {}, rows.map(cells => h("tr", {}, cells.map(c => h("td", {}, c)))))));
          const stars = (n, g) => n > 0 ? (g === 2 ? "☾ +" : "☀ +") + n : "";
          modal(r.name + " · Lv" + r.level, h("div", { class: "bt-detail" },
            grid([["Loại", r.kindLabel + (r.style ? " — " + r.style : "")], ["Chỗ", r.place + " · " + r.map], ["Class / nhân vật", r.cls + " · " + r.gender],
              ["Kiểu cộng điểm", r.build + " · còn " + num(r.talentPoints) + " điểm"], ["Cấp lúc vào", r.startLevel + (r.level > r.startLevel ? " → " + r.level : "")],
              ["Online", r.onlineMinutes + " phút · còn " + r.minutesLeft + " phút"], ["Chết / bị PK / quăng cần", r.deaths + " / " + r.pkFights + " / " + r.casts],
              ["Vàng", num(r.gold)], ["Tổ đội", r.party ? r.party.join(", ") : "–"]]),
            h("h4", {}, "Chỉ số"), grid(r.stats || []),
            h("h4", {}, "Trang bị & bộ sưu tập"), table(["Ô", "Món", "Sao"], r.gear.map(g => [g[0], g[1], stars(g[2], g[3])])),
            h("h4", {}, "Kỹ năng"), table(["Kỹ năng", "Cấp", "Trên thanh"], r.skills.map(s => [s[0], s[1] > 0 ? s[1] : h("span", { class: "mut" }, "chưa học"), s[2] ? "✓" : ""])),
            h("h4", {}, "Tiềm năng"), grid(r.talents.map(t => [t[0], num(t[1])]))), {
            wide: true, actions: [{ label: "Cho thoát", danger: true, run: async () => { const ok = await act("bots/kick", { id: r.id }); if (ok) setTimeout(load, 700); return ok; } }]
          });
        }

        async function load() {
          const q = $("btQ").value.trim();
          const seq = ++requestSeq;
          const next = await api("bots?kind=" + encodeURIComponent(kindFilter) + "&q=" + encodeURIComponent(q));
          if (seq !== requestSeq) return;
          data = next;
          if (!saved || !dirty) { saved = data.config; write(saved); changed(); }
          renderHead(); renderOverview(); renderList();
        }
        return { refresh: load };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Thông báo & bảo trì ================= */
    Tabs.add({
      id: "notify", icon: "notify", group: "Điều hành", label: "Thông báo & bảo trì", poll: 30000,
      html: `<div class="ph">
    <div>
      <h1>Thông báo &amp; bảo trì</h1>
      <p class="sub">Chữ chạy toàn server, chế độ bảo trì và lịch tắt máy chủ.</p>
    </div>
  </div>
  <div class="notify-kpis">
    <div class="card notify-kpi warn">
      <div class="ic"><svg viewBox="0 0 24 24"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg></div>
      <div>
        <small>Trạng thái bảo trì</small>
        <div class="val" id="maintState"></div>
      </div>
    </div>
    <div class="card notify-kpi danger">
      <div class="ic"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg></div>
      <div>
        <small>Lịch tắt máy chủ</small>
        <div class="val" id="sdState">Không có lịch tắt.</div>
      </div>
    </div>
  </div>
  <div class="cols c-notify">
    <div class="card">
      <div class="ph" style="margin-bottom:6px">
        <div>
          <h2>Thông báo toàn server</h2>
          <p class="hint" style="margin:2px 0 0">Hiện ở dòng chữ chạy và khung chat "Hệ thống" của mọi người chơi đang online.</p>
        </div>
      </div>
      <div class="chips" id="bcTpl" style="margin-bottom:10px"></div>
      <textarea id="bcText" maxlength="300" placeholder="Nhập nội dung thông báo phát toàn server..." style="min-height:96px"></textarea>
      <div class="counter" id="bcCount">0 / 300</div>
      <div class="preview-ticker" id="bcPreview" hidden></div>
      <div class="row" style="margin-top:12px;gap:8px;align-items:center">
        <label class="mut" style="font-weight:600">Phát</label>
        <input id="bcRepeat" type="number" min="1" max="50" value="1" style="width:68px;text-align:center">
        <span class="mut">lần</span>
        <span class="mut" style="margin-left:4px">cách nhau</span>
        <input id="bcInterval" type="number" min="10" max="3600" value="60" style="width:76px;text-align:center">
        <span class="mut">giây</span>
        <span style="flex:1"></span>
        <button class="btn primary" id="bcBtn" style="padding:6px 16px">Phát thông báo</button>
      </div>
    </div>
    <div style="display:flex;flex-direction:column;gap:16px">
      <div class="card sun" style="margin-bottom:16px">
        <div class="ph" style="margin-bottom:6px">
          <div>
            <h2>Chế độ bảo trì</h2>
            <p class="hint" style="margin:2px 0 0">Chặn người chơi mới đăng nhập. Người đang online tiếp tục chơi bình thường.</p>
          </div>
        </div>
        <div class="row" style="margin-top:12px;gap:8px">
          <button class="btn primary" id="maintOn">Bật bảo trì</button>
          <button class="btn" id="maintOff">Tắt bảo trì</button>
        </div>
      </div>
      <div class="card danger" style="margin:0">
        <div class="ph" style="margin-bottom:6px">
          <div>
            <h2>Lên lịch tắt máy chủ</h2>
            <p class="hint" style="margin:2px 0 0">Đếm ngược bằng chữ chạy, đưa người chơi ra ngoài, lưu dữ liệu rồi thoát an toàn.</p>
          </div>
        </div>
        <div class="row" style="margin-top:12px;gap:8px;align-items:center">
          <label class="mut" style="font-weight:600">Chờ</label>
          <input id="sdSeconds" type="number" value="300" min="0" max="3600" style="width:84px;text-align:center">
          <span class="mut">giây</span>
          <span style="flex:1"></span>
          <button class="btn danger" id="sdBtn">Lên lịch tắt</button>
          <button class="btn" id="sdCancel">Huỷ lịch</button>
        </div>
      </div>
    </div>
  </div>
  <div class="card">
    <div class="ph" style="margin-bottom:10px">
      <div>
        <h2>Lịch sử thông báo</h2>
        <p class="hint" style="margin:2px 0 0">Tối đa 30 lần phát gần nhất kể từ lúc máy chủ khởi động.</p>
      </div>
    </div>
    <div class="tw"><table><thead><tr><th style="width:140px">Thời gian</th><th>Nội dung thông báo</th><th style="width:110px">Tiến độ</th><th style="width:120px">Trạng thái</th><th style="width:110px;text-align:right">Thao tác</th></tr></thead><tbody id="bcRows"></tbody></table></div>
  </div>`,
      setup(root) {
        const TPL = ["Máy chủ sẽ bảo trì sau 10 phút, vui lòng thoát game để tránh mất dữ liệu.", "Sự kiện nhân đôi kinh nghiệm đã bắt đầu, chúc các bạn chơi vui!", "Boss thế giới sắp xuất hiện, hãy chuẩn bị!", "Cảm ơn các bạn đã đồng hành cùng TLNT!"];
        $("bcTpl").replaceChildren(...TPL.map(t => h("button", { type: "button", onclick: () => { $("bcText").value = t; upd(); } }, t.length > 34 ? t.slice(0, 34) + "…" : t)));
        const upd = () => { const v = $("bcText").value; $("bcCount").textContent = v.length + " / 300"; $("bcPreview").hidden = !v.trim(); $("bcPreview").textContent = "Hệ thống: " + v; };
        $("bcText").oninput = upd;
        $("bcBtn").onclick = async () => {
          const rep = +$("bcRepeat").value || 1;
          if (await act("broadcast", { text: $("bcText").value, repeat: rep, interval: +$("bcInterval").value }, rep > 1 ? `Phát ${rep} lần, mỗi ${$("bcInterval").value} giây?` : null)) { $("bcText").value = ""; upd(); load(); }
        };
        $("maintOn").onclick = () => act("maintenance", { on: true }, "Bật bảo trì (chặn đăng nhập mới)?").then(Admin.refreshStatus);
        $("maintOff").onclick = () => act("maintenance", { on: false }).then(Admin.refreshStatus);
        $("sdBtn").onclick = () => act("shutdown", { seconds: +$("sdSeconds").value }, "Hẹn TẮT SERVER sau " + $("sdSeconds").value + " giây?").then(Admin.refreshStatus);
        $("sdCancel").onclick = () => act("shutdown/cancel", {}).then(Admin.refreshStatus);
        Admin.onStatus(s => {
          $("maintState").replaceChildren(s.maintenance ? chip("Đang bảo trì", "warn") : chip("Đang mở", "ok"));
          $("sdState").textContent = s.shutdownInSeconds >= 0 ? "Máy chủ sẽ tắt sau " + s.shutdownInSeconds + " giây." : "Không có lịch tắt.";
        });
        async function load() {
          const list = await api("broadcasts");
          fill($("bcRows"), list.map(b => h("tr", {}, h("td", { class: "mut", style: "white-space:nowrap" }, b.time), h("td", {}, b.text, b.repeat > 1 ? h("div", { class: "mut", style: "font-size:11.5px" }, b.repeat + " lần · mỗi " + b.interval + " giây") : ""),
            h("td", {}, b.sent + " / " + b.repeat), h("td", {}, b.running ? chip("Đang phát", "ok") : b.cancelled ? chip("Đã dừng") : chip("Xong")),
            h("td", { style: "text-align:right" }, b.running ? h("button", { class: "btn small danger", onclick: () => act("broadcast/cancel", { id: b.id }).then(load) }, "Dừng") : h("button", { class: "btn small", onclick: () => { $("bcText").value = b.text; upd(); } }, "Dùng lại")))), "Chưa phát thông báo nào");
        }
        return { refresh: load };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Boss thế giới + vị trí tài nguyên ================= */
    Tabs.add({
      id: "events", icon: "boss", group: "Nội dung game", label: "Boss & tài nguyên", poll: 15000,
      html: `<h1>Boss &amp; tài nguyên</h1><p class="sub">Quản lý boss thế giới, mini boss Cáo và tài nguyên (cây, tổ ong, quả dừa) trên bản đồ.</p>
  <div class="event-switcher" role="tablist" aria-label="Boss và tài nguyên">
    <button type="button" class="active" data-event-panel="boss" role="tab" aria-selected="true"><svg viewBox="0 0 24 24"><path d="M5 20l2-9 5 4 5-4 2 9zM7 11L4 5l5 3 3-5 3 5 5-3-3 6"/></svg>Boss thế giới<span class="ev-badge" id="bossTabBadge">0</span></button>
    <button type="button" data-event-panel="fox" role="tab" aria-selected="false"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><text x="12" y="16" text-anchor="middle" font-size="12" fill="currentColor">🦊</text></svg>Mini boss Cáo<span class="ev-badge" id="foxTabBadge">0</span></button>
    <button type="button" data-event-panel="resources" role="tab" aria-selected="false"><svg viewBox="0 0 24 24"><path d="M12 22c-4.97 0-9-2.24-9-5v-6l9-5 9 5v6c0 2.76-4.03 5-9 5z"/><path d="M3 11l9 5 9-5"/></svg>Vị trí tài nguyên<span class="ev-badge" id="resTabBadge">0</span></button>
  </div>
  <div class="event-panel" id="eventPanelBoss">
    <div class="ev-kpis" id="bossKpis"></div>
    <div class="card"><div class="ph"><div><h2>Boss thế giới</h2><p class="hint">Máu cập nhật mỗi 3 giây khi boss đang sống. Bấm tên boss sống để xem bảng sát thương.</p></div><div class="row" style="margin:0"><button class="btn small" id="bossReload">↻ Làm mới</button></div></div>
    <div class="boss-filters">
      <input id="bossQ" type="search" placeholder="🔍 Tìm theo tên hoặc ID boss">
      <select id="bossFilterState">
        <option value="">Tất cả trạng thái</option>
        <option value="alive">Đang sống</option>
        <option value="waiting">Chờ hồi sinh / Đang xuất hiện</option>
        <option value="disabled">Đang tắt</option>
      </select>
      <span style="flex:1"></span>
      <span class="mut" id="bossFilteredCount"></span>
    </div>
    <div class="bc-list-head" aria-hidden="true"><span>Boss</span><span>Trạng thái</span><span>Máu / hồi sinh</span><span>Vị trí &amp; chu kỳ</span><span>Thao tác</span></div>
    <div class="bc-list" id="bRows"></div></div>
  </div>
  <div class="event-panel" id="eventPanelFox" hidden>
    <div class="ev-kpis" id="foxKpis"></div>
    <div class="card"><div class="ph"><div><h2>Mini boss · Tiểu Tinh linh Cáo</h2><p class="hint">5 con lang thang trộm vàng khoá của người đứng im. Bấm vào một con đang sống để xem ai bị trộm.</p></div><div class="row" style="margin:0"><button class="btn small primary" id="foxSpawn">⚡ Mọc ngay</button><button class="btn small" id="foxReload">↻ Làm mới</button></div></div>
    <div class="fox-list-head" aria-hidden="true"><span>Cáo / trạng thái</span><span>Vị trí</span><span>Máu</span><span>Vàng bị trộm</span><span>Thao tác</span></div>
    <div class="fox-list" id="foxRows"></div></div>
  </div>
  <div class="event-panel" id="eventPanelResources" hidden>
    <div class="ev-kpis" id="resKpis"></div>
    <div class="card"><div class="ph"><div><h2>Vị trí tài nguyên</h2><p class="hint" id="evGatherSub">Các cây, tổ ong và quả dừa đang xuất hiện ở map / khu; tự cập nhật khoảng 15 giây một lần.</p></div><div class="row" style="margin:0"><button class="btn small primary" id="evGatherAdd">＋ Thêm tài nguyên</button><button class="btn small" id="evGatherReload">↻ Làm mới</button></div></div>
    <div class="res-filters"><input id="evGatherQ" type="search" placeholder="🔍 Tìm theo tên, map, khu hoặc ID">
      <select id="evGatherKind"><option value="">Mọi nguồn</option><option value="plant">🌿 Cây hái được</option><option value="hive">🐝 Tổ ong</option><option value="coconut">🥥 Quả dừa</option></select>
      <div class="res-item-filter" id="evGatherItemFilter"><button type="button" class="btn res-filter-select" id="evGatherItemButton" aria-haspopup="listbox" aria-expanded="false"><span class="res-filter-current"><span id="evGatherItemIcon"></span><span id="evGatherItemLabel">Mọi đồ nhặt được</span></span><span class="res-filter-arrow">▾</span></button>
        <div class="res-item-options" id="evGatherItemOptions" role="listbox" hidden></div><select id="evGatherItem" aria-label="Lọc theo đồ nhặt được" hidden><option value="">Mọi đồ nhặt được</option></select></div>
      <select id="evGatherMap"><option value="">Mọi map</option></select></div>
    <div class="ph" style="margin:0 0 10px"><h2 style="margin:0">Danh sách vị trí</h2><span id="evGatherCount" class="mut"></span></div>
    <div class="res-list" id="evGatherRows"></div>
    <div class="ev-pagination" id="evGatherPageBox" style="display:none"><span class="mut" id="evGatherPageInfo"></span><div class="row" style="margin:0"><button class="btn small" id="evGatherPrev">‹ Trước</button><button class="btn small" id="evGatherNext">Sau ›</button></div></div>
  </div></div>`,
      setup(root) {
        const KIND = { plant: "Cây", hive: "Tổ ong", coconut: "Quả dừa" };
        const KIND_ICON = { plant: "🌿", hive: "🐝", coconut: "🥥" };
        const IC_CROWN = '<svg viewBox="0 0 24 24"><path d="M5 20l2-9 5 4 5-4 2 9zM7 11L4 5l5 3 3-5 3 5 5-3-3 6"/></svg>';
        const IC_CHECK = '<svg viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>';
        const IC_TIME = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>';
        const IC_OFF = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>';
        const IC_FOX = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><text x="12" y="16" text-anchor="middle" font-size="12" fill="currentColor">🦊</text></svg>';
        const IC_GOLD = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><line x1="12" y1="3" x2="12" y2="7"/></svg>';
        const IC_USERS = '<svg viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>';
        const IC_BOX = '<svg viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/></svg>';
        const IC_TREE = '<svg viewBox="0 0 24 24"><path d="M12 22v-8"/><path d="M12 14c-4-1-7-5-7-9 5 0 8 3 9 7 1-4 4-7 9-7 0 4-3 8-7 9"/></svg>';
        const IC_HIVE = '<svg viewBox="0 0 24 24"><path d="M12 2L4 7v10l8 5 8-5V7z"/><line x1="12" y1="22" x2="12" y2="12"/></svg>';
        const IC_COCO = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><circle cx="9.5" cy="10" r="1.5" fill="currentColor"/><circle cx="14.5" cy="10" r="1.5" fill="currentColor"/><circle cx="12" cy="14" r="1.5" fill="currentColor"/></svg>';

        function makeKpi(iconHtml, label, value, colorClass) {
          const wrap = h("div", { class: "ev-ic " + (colorClass || "teal") });
          wrap.innerHTML = iconHtml;
          return h("div", { class: "ev-kpi" }, wrap, h("div", {}, h("small", {}, label), h("strong", {}, value)));
        }

        const mapList = async () => { if (!MAPS) MAPS = await api("map-list"); return MAPS; };

        /* Popup thêm tài nguyên */
        async function addResource() {
          let types; try { types = await api("resource-types"); await mapList(); } catch (e) { return toast(e.message, true); }
          if (!types.length) return toast("Chưa cấu hình cây / săn bắt.", true);
          const tSel = h("select", { style: "width:100%" }, types.map((t, i) => h("option", { value: i }, (KIND_ICON[t.kind] || "") + " " + KIND[t.kind] + " · " + t.name + " → " + t.items.map(x => x.name).join(", "))));
          const preview = h("div", { class: "res-items", style: "margin-top:6px" });
          const mSel = h("select", { style: "width:100%" }), zone = h("input", { type: "number", min: 0, placeholder: "mọi khu", style: "width:110px" }), count = h("input", { type: "number", min: 1, max: 20, value: 1, style: "width:90px" });
          const x = h("input", { type: "number", placeholder: "trống = ngẫu nhiên", style: "width:160px" }), y = h("input", { type: "number", placeholder: "trống = mặt đất", style: "width:160px" });
          const sync = () => {
            const t = types[+tSel.value];
            preview.replaceChildren(...t.items.map(i => h("span", { class: "res-item" }, ico(i.icon, 28), i.name)));
            const cfg = new Set(t.maps);
            mSel.replaceChildren(...MAPS.filter(m => !m[2]).map(m => h("option", { value: m[0] }, "#" + m[0] + " " + m[1] + (cfg.has(m[0]) ? " ★" : ""))));
            if (t.maps.length) mSel.value = t.maps[0];
          };
          tSel.onchange = sync; sync();
          modal("Thêm tài nguyên", h("div", {}, h("div", { class: "field" }, h("label", {}, "Loại"), h("div", {}, tSel, preview),
            h("label", {}, "Map"), h("div", {}, mSel, h("div", { class: "hint", style: "margin:4px 0 0" }, "★ = map có trong cấu hình của loại này (có sẵn điểm mọc).")),
            h("label", {}, "Khu"), h("span", {}, zone, h("span", { class: "mut" }, " trống = mọi khu thường của map")), h("label", {}, "Số lượng / khu"), count,
            h("label", {}, "Toạ độ X"), x, h("label", {}, "Toạ độ Y"), y),
            h("p", { class: "hint", style: "margin-top:10px" }, "Tài nguyên admin đặt nằm ngoài giới hạn toàn server, hái / phá xong là hết (không mọc lại). Nhập X để đặt đúng chỗ (nhiều cái thì xếp ngang cách 120 px, Y tự hạ xuống mặt đất).")), {
            actions: [{
              label: "Đặt", primary: true, run: async () => {
                const t = types[+tSel.value];
                const res = await act("resource/add", { kind: t.kind, templateId: t.templateId, map: +mSel.value, zone: zone.value === "" ? null : +zone.value, count: +count.value, x: x.value === "" ? null : +x.value, y: y.value === "" ? null : +y.value });
                if (res) setTimeout(loadGatherLocations, 600); return !!res;
              }
            }]
          });
        }

        /* Popup dời boss / cáo */
        async function moveBoss(b) {
          try { await mapList(); } catch (e) { return toast(e.message, true); }
          const mSel = h("select", { style: "width:100%" }, MAPS.filter(m => !m[2]).map(m => h("option", { value: m[0] }, "#" + m[0] + " " + m[1]))); mSel.value = b.mapId;
          const zone = h("input", { type: "number", min: 0, value: b.zoneId, style: "width:90px" });
          const x = h("input", { type: "number", placeholder: "trống = ngẫu nhiên xa cửa", style: "width:200px" }), y = h("input", { type: "number", placeholder: "trống = mặt đất", style: "width:200px" });
          modal("Dời " + b.name, h("div", {}, h("p", { class: "hint", style: "margin-top:0" }, "Đang ở " + (b.mapName || ("map " + b.mapId)) + " khu " + b.zoneId + ". Giữ nguyên máu, bảng sát thương (cáo giữ vàng đã trộm); không thông báo toàn server."),
            h("div", { class: "field" }, h("label", {}, "Map"), mSel, h("label", {}, "Khu"), zone, h("label", {}, "Toạ độ X"), x, h("label", {}, "Toạ độ Y"), y)), {
            actions: [{
              label: "Dời", primary: true, run: async () => {
                const res = await act("boss/move", { id: b.key || b.id, map: +mSel.value, zone: zone.value === "" ? null : +zone.value, x: x.value === "" ? null : +x.value, y: y.value === "" ? null : +y.value });
                if (res) setTimeout(load, 800); return !!res;
              }
            }]
          });
        }

        /* Popup chi tiết 1 con cáo */
        function foxDetail(f) {
          modal(f.name, h("div", {}, h("div", { class: "minis", style: "margin-bottom:12px" },
            h("div", {}, h("small", {}, "Vị trí"), h("div", { class: "big", style: "font-size:15px" }, f.mapName + " khu " + f.zoneId), h("small", {}, "(" + num(f.x) + ", " + num(f.y) + ")")),
            h("div", {}, h("small", {}, "Đã trộm"), h("div", { class: "big" }, num(f.total)), h("small", {}, "/ 1.000.000 vàng khoá")),
            h("div", {}, h("small", {}, "Đang nhắm"), h("div", { class: "big", style: "font-size:15px" }, f.targetName || "–"))),
            h("div", { class: "tw", style: "max-height:360px" }, h("table", {}, h("thead", {}, h("tr", {}, ["Người bị trộm", "Vàng khoá", ""].map(z => h("th", {}, z)))),
              h("tbody", {}, f.victims.length ? f.victims.map(v => h("tr", {}, h("td", {}, h("a", { href: "#", onclick: e => { e.preventDefault(); openPlayer(v[0]); } }, v[1] || "#" + v[0])),
                h("td", {}, num(v[2]) + " / 100.000"), h("td", {}, v[2] >= 100000 ? chip("Đã đủ", "warn") : ""))) : [h("tr", {}, h("td", { colspan: 3, class: "empty" }, "Chưa trộm của ai"))])))), { wide: true });
        }

        function foxStatus(f) {
          const s = f.secondsToSpawn || 0;
          if (!f.alive) return h("span", { class: "mut" }, f.spawning ? "Đang mọc" : "Mọc sau " + (s < 60 ? s + "s" : Math.floor(s / 60) + "m " + s % 60 + "s"));
          if (f.detected) return chip("Bị phát hiện", "bad");
          if (f.full) return chip("Đủ 1 triệu", "warn");
          if (f.targetName) return chip("Nhắm " + f.targetName, "warn");
          return chip("Lang thang", "ok");
        }

        async function loadFoxes() {
          const fs = await api("foxes");
          const aliveFoxes = fs.filter(f => f.alive).length;
          let totalGold = 0, totalVictims = 0;
          fs.forEach(f => {
            if (f.alive) {
              totalGold += (f.total || 0);
              totalVictims += (f.victims || []).length;
            }
          });
          $("foxTabBadge").textContent = aliveFoxes + " sống";
          $("foxKpis").replaceChildren(
            makeKpi(IC_FOX, "Số lượng Cáo", fs.length, "teal"),
            makeKpi(IC_CHECK, "Đang sống", aliveFoxes, "green"),
            makeKpi(IC_GOLD, "Vàng bị trộm", short(totalGold), "amber"),
            makeKpi(IC_USERS, "Nạn nhân", totalVictims, "blue")
          );
          const cards = fs.map(f => h("article", { class: "fox-card" + (f.alive ? " clickable" : " fox-dead"), onclick: f.alive && f.victims ? () => foxDetail(f) : null },
            h("div", { class: "fox-head" },
              h("div", { class: "fox-avatar" }, "🦊"),
              h("span", { class: "fox-name" }, f.name),
              foxStatus(f)),
            h("div", { class: "fox-location" }, f.alive ? h("span", {}, "📍 " + f.mapName + " (#" + f.mapId + ") · khu " + f.zoneId + (f.x != null ? " · (" + num(f.x) + ", " + num(f.y) + ")" : "")) : h("span", { class: "mut" }, "Chưa xuất hiện")),
            h("div", { class: "fox-health" }, f.alive && f.hp >= 0 ? hpCell({ alive: true, hp: f.hp, maxHp: f.maxHp }) : h("span", { class: "mut" }, "–")),
            h("div", { class: "fox-stolen" }, f.alive ? (f.total > 0 ? h("div", {},
              h("b", {}, num(f.total)), " vàng khoá",
              h("div", { class: "fox-stolen-bar" }, h("i", { style: "width:" + Math.min(100, f.total / 10000) + "%" })),
              h("small", { class: "mut" }, (f.victims || []).length + " người bị trộm")) : h("span", { class: "mut" }, "0 vàng khoá")) : h("span", { class: "mut" }, "–")),
            h("div", { class: "fox-actions", onclick: e => e.stopPropagation() },
              h("button", { class: "btn small", disabled: !f.alive, onclick: () => moveBoss({ key: f.key, name: f.name, mapId: f.mapId, mapName: f.mapName, zoneId: f.zoneId }) }, "📍 Dời"),
              h("button", { class: "btn small", disabled: !f.alive || !(f.total > 0), onclick: () => act("fox/refund", { slot: f.slot }, "Trả " + num(f.total) + " vàng khoá cho " + (f.victims ? f.victims.length : 0) + " người qua thư và xoá sổ trộm của " + f.name + "?").then(loadFoxes) }, "💰 Trả vàng"),
              h("button", { class: "btn small danger", disabled: !f.alive, onclick: () => act("fox/kill", { slot: f.slot }, "Hạ " + f.name + "? Vàng đã trộm rơi xuống đất.").then(() => setTimeout(loadFoxes, 800)) }, "💀 Hạ"))));
          $("foxRows").replaceChildren(...(cards.length ? cards : [h("div", { class: "fox-empty" }, "Chưa bật mini boss Cáo.")]));
        }

        let activePanel = "boss", bossList = [], gatherRows = [], gatherLoadedAt = 0, gatherLoading = false, gatherPage = 1;
        const PAGE_SIZE = 40;
        const itemsOf = r => r.items || [];
        function fillSelect(sel, entries, allText) {
          const cur = sel.value;
          sel.replaceChildren(h("option", { value: "" }, allText), ...entries.map(([v, t]) => h("option", { value: v }, t)));
          sel.value = entries.some(([v]) => String(v) === cur) ? cur : "";
        }

        function renderBossList() {
          const q = $("bossQ").value.trim().toLowerCase();
          const filterState = $("bossFilterState").value;
          const filtered = bossList.filter(b => {
            if (filterState === "alive" && !b.alive) return false;
            if (filterState === "waiting" && (b.alive || b.disabled)) return false;
            if (filterState === "disabled" && !b.disabled) return false;
            if (q) {
              const txt = (b.name + " " + b.id + " " + (b.status || "")).toLowerCase();
              if (!txt.includes(q)) return false;
            }
            return true;
          });
          $("bossFilteredCount").textContent = filtered.length + " / " + bossList.length + " boss";
          const aliveCount = bossList.filter(b => b.alive).length;
          const disabledCount = bossList.filter(b => b.disabled).length;
          const waitCount = bossList.length - aliveCount - disabledCount;
          $("bossTabBadge").textContent = aliveCount + " sống";
          $("bossKpis").replaceChildren(
            makeKpi(IC_CROWN, "Tổng boss", bossList.length, "teal"),
            makeKpi(IC_CHECK, "Đang sống", aliveCount, "green"),
            makeKpi(IC_TIME, "Chờ hồi sinh", waitCount, "amber"),
            makeKpi(IC_OFF, "Đang tắt", disabledCount, "red")
          );
          const fmtCd = sec => { sec = Math.max(0, sec | 0); const hh = Math.floor(sec / 3600), mm = Math.floor(sec % 3600 / 60), ss = sec % 60;
            return (hh ? hh + ":" + String(mm).padStart(2, "0") : mm) + ":" + String(ss).padStart(2, "0"); };
          const cards = filtered.map(b => {
            const st = b.disabled ? "off" : b.alive ? "alive" : b.spawning ? "spawning" : "wait";
            const stLabel = { off: "Đang tắt", alive: "Đang sống", spawning: "Đang xuất hiện", wait: "Chờ hồi sinh" }[st];
            const pct = b.alive && b.maxHp > 0 && b.hp >= 0 ? Math.max(0, Math.min(100, b.hp * 100 / b.maxHp)) : 0;
            const cycle = (b.respawnMinutes || 0) * 60;
            const waitPct = st === "wait" && cycle > 0 ? Math.max(0, Math.min(100, 100 - b.nextSpawnIn * 100 / cycle)) : 0;
            const meter = b.alive
              ? h("div", { class: "bc-meter" },
                  h("div", { class: "bc-bar hp" + (pct < 20 ? " low" : pct < 50 ? " mid" : "") }, h("i", { style: "width:" + pct.toFixed(1) + "%" })),
                  h("div", { class: "bc-meter-row" }, h("b", {}, b.hp >= 0 ? pct.toFixed(1).replace(".", ",") + "%" : "Đang đọc…"),
                    h("span", { class: "mut" }, b.hp >= 0 ? num(b.hp) + " / " + num(b.maxHp) + " HP" : "")))
              : st === "wait"
                ? h("div", { class: "bc-meter" },
                    h("div", { class: "bc-bar cd" }, h("i", { style: "width:" + waitPct.toFixed(1) + "%" })),
                    h("div", { class: "bc-meter-row" }, h("span", { class: "mut" }, "Xuất hiện sau"), h("b", { "data-cd": Date.now() + b.nextSpawnIn * 1000 }, fmtCd(b.nextSpawnIn))))
                : h("div", { class: "bc-meter" }, h("div", { class: "bc-meter-row" }, h("span", { class: "mut" },
                    st === "off" ? "Bật lại trong \"Sửa\" (cột Bật) để boss xuất hiện theo lịch." : "Đang tìm khu để xuất hiện…")));
            const facts = h("div", { class: "bc-facts" },
              h("span", {}, "📍 ", b.alive ? (b.mapName || "?") + " · khu " + b.zoneId : (b.mapCount ? b.mapCount + " map có thể ra" : "–")),
              b.respawnMinutes ? h("span", {}, "⟳ " + b.respawnMinutes + " phút") : "",
              b.maxHp && !b.alive ? h("span", {}, "❤ " + num(b.maxHp)) : "",
              b.alive && b.hunters ? h("span", { class: "bc-hunters" }, "🤖 " + b.hunters + " bot đang săn") : "");
            const btn = (cls, label, fn) => h("button", { class: "btn small " + cls, onclick: stop(fn) }, label);
            const actions = b.alive ? [btn("primary", "Sát thương", () => bossDamage(b)), btn("", "Dời", () => moveBoss(b)), btn("", "Sửa", () => editBoss(b.id)),
              btn("danger", "Hạ", () => act("boss/kill", { id: b.id }, "Hạ " + b.name + "? (không ai nhận quà kết liễu)").then(() => setTimeout(load, 800)))]
              : !b.disabled ? [btn("ok", "Gọi ra ngay", () => act("boss/spawn", { id: b.id }).then(() => setTimeout(load, 1500))), btn("", "Dời", () => moveBoss(b)), btn("", "Sửa", () => editBoss(b.id))]
              : [btn("primary", "Sửa / bật", () => editBoss(b.id))];
            return h("article", { class: "bc " + st, onclick: () => { if (b.alive) bossDamage(b); } },
              h("div", { class: "bc-head" },
                h("div", { class: "bc-avatar" }, b.kind === "char" ? "🧙" : "👹"),
                h("div", { class: "bc-title" }, h("b", {}, b.name), h("small", {}, "#" + b.id + " · Cấp " + (b.level || "–") + " · " + (b.kind === "char" ? "Người chơi giả" : "Quái")))),
              h("div", { class: "bc-st" }, h("span", { class: "bc-state " + st }, stLabel)),
              meter, facts, h("div", { class: "bc-actions" }, ...actions));
          });
          $("bRows").replaceChildren(...(cards.length ? cards : [h("div", { class: "boss-empty" }, bossList.length ? "Không có boss nào khớp bộ lọc tìm kiếm." : "Chưa cấu hình boss thế giới.")]));
        }

        function renderGatherLocations() {
          const kind = $("evGatherKind").value, item = $("evGatherItem").value, map = $("evGatherMap").value, q = $("evGatherQ").value.trim().toLowerCase();
          const list = gatherRows.filter(r => (!kind || r.kind === kind) && (!item || itemsOf(r).some(i => String(i.id) === item)) && (!map || String(r.mapId) === map)
            && (!q || (r.name + " " + r.mapName + " " + r.mapId + " khu " + r.zone + " " + r.templateId + " " + itemsOf(r).map(i => i.name).join(" ")).toLowerCase().includes(q)));
          $("evGatherCount").textContent = list.length + " / " + gatherRows.length + " vị trí";
          const maxPage = Math.max(1, Math.ceil(list.length / PAGE_SIZE));
          if (gatherPage > maxPage) gatherPage = maxPage;
          if (gatherPage < 1) gatherPage = 1;
          const paged = list.slice((gatherPage - 1) * PAGE_SIZE, gatherPage * PAGE_SIZE);

          const pageBox = $("evGatherPageBox");
          if (list.length > PAGE_SIZE) {
            pageBox.style.display = "flex";
            $("evGatherPageInfo").textContent = "Trang " + gatherPage + " / " + maxPage + " (hiển thị " + paged.length + " / " + list.length + " vị trí)";
            $("evGatherPrev").disabled = gatherPage <= 1;
            $("evGatherNext").disabled = gatherPage >= maxPage;
          } else {
            pageBox.style.display = "none";
          }

          const cards = paged.map(r => {
            const remove = h("button", {
              class: "btn small danger", title: "Xoá tài nguyên ở vị trí này", onclick: async () => {
                if (await act("resource/remove", { kind: r.kind, map: r.mapId, zone: r.zone, entityId: r.entityId }, "Xoá " + r.name + " ở " + r.mapName + " khu " + r.zone + " (" + num(r.x) + ", " + num(r.y) + ")?")) {
                  gatherRows = gatherRows.filter(x => x !== r); renderSummary(); renderGatherLocations();
                }
              }
            }, "🗑 Xoá");
            const items = itemsOf(r).length ? itemsOf(r).map(i => h("span", { class: "res-item", title: i.name + " #" + i.id }, ico(i.icon, 24), h("span", {}, i.name))) : h("span", { class: "mut" }, "Không có đồ nhặt được");
            return h("article", { class: "res-card kind-" + r.kind },
              h("div", { class: "res-card-head" },
                h("div", { class: "res-card-title" }, (KIND_ICON[r.kind] || "") + " " + r.name, h("small", {}, "Mẫu #" + r.templateId + " · " + (KIND[r.kind] || r.kind)))),
              h("div", { class: "res-card-items" }, items),
              h("div", { class: "res-meta" },
                h("div", {}, h("small", {}, "Vị trí"), h("b", {}, "📍 " + r.mapName + " (#" + r.mapId + ") · khu " + r.zone)),
                h("div", {}, h("small", {}, "Toạ độ"), h("b", {}, "(" + num(r.x) + ", " + num(r.y) + ")"))),
              remove);
          });
          const empty = h("div", { class: "res-empty" }, gatherRows.length ? "Không có vị trí nào khớp bộ lọc." : "Chưa có tài nguyên nào trong khu đang chạy.");
          $("evGatherRows").replaceChildren(...(cards.length ? cards : [empty]));
        }

        function renderSummary() {
          const plantCount = gatherRows.filter(r => r.kind === "plant").length;
          const hiveCount = gatherRows.filter(r => r.kind === "hive").length;
          const coconutCount = gatherRows.filter(r => r.kind === "coconut").length;
          $("resTabBadge").textContent = gatherRows.length;
          $("resKpis").replaceChildren(
            makeKpi(IC_BOX, "Tổng tài nguyên", gatherRows.length, "teal"),
            makeKpi(IC_TREE, "Cây hái được", plantCount, "green"),
            makeKpi(IC_HIVE, "Tổ ong", hiveCount, "amber"),
            makeKpi(IC_COCO, "Quả dừa", coconutCount, "blue")
          );
          const byItem = new Map();
          gatherRows.forEach(r => itemsOf(r).forEach(i => { const e = byItem.get(i.id) || { item: i, n: 0 }; e.n++; byItem.set(i.id, e); }));
          const sorted = [...byItem.values()].sort((a, b) => b.n - a.n);
          const itemSelect = $("evGatherItem"), options = $("evGatherItemOptions"), selected = sorted.find(e => String(e.item.id) === itemSelect.value);
          fillSelect(itemSelect, sorted.map(e => [e.item.id, e.item.name + " (" + e.n + ")"]), "Mọi đồ nhặt được");
          $("evGatherItemIcon").replaceChildren(...(selected ? [ico(selected.item.icon, 24)] : []));
          $("evGatherItemLabel").textContent = selected ? selected.item.name + " (" + selected.n + ")" : "Mọi đồ nhặt được";
          options.replaceChildren(
            h("button", { type: "button", class: "res-item-option" + (!itemSelect.value ? " on" : ""), role: "option", "aria-selected": !itemSelect.value, onclick: () => chooseItem("") },
              h("span", { style: "width:24px" }), h("span", {}, "Mọi đồ nhặt được")),
            ...sorted.map(e => h("button", { type: "button", class: "res-item-option" + (String(e.item.id) === itemSelect.value ? " on" : ""), role: "option", "aria-selected": String(e.item.id) === itemSelect.value, onclick: () => chooseItem(e.item.id) },
              ico(e.item.icon, 24), h("span", {}, e.item.name), h("small", {}, "(" + e.n + ")"))));
          const maps = new Map();
          gatherRows.forEach(r => { const m = maps.get(r.mapId) || { name: r.mapName, n: 0 }; m.n++; maps.set(r.mapId, m); });
          fillSelect($("evGatherMap"), [...maps.entries()].sort((a, b) => a[0] - b[0]).map(([id, m]) => [id, "#" + id + " " + m.name + " (" + m.n + ")"]), "Mọi map");
        }

        function chooseItem(id) {
          $("evGatherItem").value = id;
          $("evGatherItemOptions").hidden = true;
          $("evGatherItemButton").setAttribute("aria-expanded", "false");
          gatherPage = 1;
          renderSummary(); renderGatherLocations();
        }

        async function loadGatherLocations() {
          if (gatherLoading) return;
          gatherLoading = true; gatherLoadedAt = Date.now();
          try { gatherRows = await api("gather-locations"); renderSummary(); renderGatherLocations(); $("evGatherSub").textContent = "Cập nhật lúc " + new Date().toLocaleTimeString("vi-VN") + " · tự làm mới khoảng 15 giây một lần."; }
          catch (e) { toast(e.message, true); }
          finally { gatherLoading = false; }
        }

        setInterval(() => {
          if (activePanel !== "boss" || document.visibilityState !== "visible") return;
          const now = Date.now();
          document.querySelectorAll("#bRows [data-cd]").forEach(el => { const sec = Math.max(0, Math.round((+el.dataset.cd - now) / 1000));
            const hh = Math.floor(sec / 3600), mm = Math.floor(sec % 3600 / 60), ss = sec % 60;
            el.textContent = (hh ? hh + ":" + String(mm).padStart(2, "0") : mm) + ":" + String(ss).padStart(2, "0"); });
        }, 1000);
        $("bossQ").oninput = renderBossList;
        $("bossFilterState").onchange = renderBossList;
        $("bossReload").onclick = () => load().catch(e => toast(e.message, true));
        $("foxReload").onclick = () => loadFoxes().catch(e => toast(e.message, true));

        $("evGatherKind").onchange = () => { gatherPage = 1; renderGatherLocations(); };
        $("evGatherQ").oninput = () => { gatherPage = 1; renderGatherLocations(); };
        $("evGatherMap").onchange = () => { gatherPage = 1; renderGatherLocations(); };
        $("evGatherPrev").onclick = () => { if (gatherPage > 1) { gatherPage--; renderGatherLocations(); } };
        $("evGatherNext").onclick = () => { gatherPage++; renderGatherLocations(); };

        $("evGatherItemButton").onclick = () => {
          const menu = $("evGatherItemOptions"), open = menu.hidden;
          menu.hidden = !open; $("evGatherItemButton").setAttribute("aria-expanded", String(open));
        };
        document.addEventListener("click", e => {
          if (!$("evGatherItemFilter").contains(e.target)) {
            $("evGatherItemOptions").hidden = true; $("evGatherItemButton").setAttribute("aria-expanded", "false");
          }
        });
        $("evGatherReload").onclick = loadGatherLocations;
        $("evGatherAdd").onclick = addResource;
        $("foxSpawn").onclick = () => act("fox/spawn", {}).then(() => setTimeout(loadFoxes, 1500));
        const eventPanels = { boss: $("eventPanelBoss"), fox: $("eventPanelFox"), resources: $("eventPanelResources") };
        root.querySelectorAll(".event-switcher [data-event-panel]").forEach(button => {
          button.onclick = () => {
            activePanel = button.dataset.eventPanel;
            root.querySelectorAll(".event-switcher [data-event-panel]").forEach(tab => {
              const selected = tab === button;
              tab.classList.toggle("active", selected); tab.setAttribute("aria-selected", String(selected));
            });
            Object.entries(eventPanels).forEach(([key, panel]) => { panel.hidden = key !== activePanel; });
            load().catch(e => toast(e.message, true));
          };
        });

        function hpCell(b) {
          if (!b.alive) return h("span", { class: "mut" }, b.maxHp ? "Tối đa " + num(b.maxHp) + " HP" : "–");
          if (!(b.hp >= 0)) return h("span", { class: "mut" }, "Đang đọc…");
          const p = b.maxHp > 0 ? Math.max(0, Math.min(100, b.hp * 100 / b.maxHp)) : 0;
          return h("div", { class: "ghp" }, h("div", { class: "ghp-bar" }, h("i", { style: "width:" + p + "%" }), h("span", {}, num(b.hp) + "/" + num(b.maxHp))));
        }

        const stop = fn => e => { e.stopPropagation(); fn(); };

        async function load() {
          if (activePanel === "resources") {
            if (Date.now() - gatherLoadedAt >= 15000) loadGatherLocations();
            return;
          }
          if (activePanel === "fox") {
            loadFoxes().catch(e => toast(e.message, true));
            return;
          }
          bossList = await api("bosses");
          renderBossList();
        }

        /* Bảng sát thương từng người lên boss đang sống: sửa / xoá dòng, xem nhân vật, đá khỏi game. Tự làm mới 3 giây. */
        function bossDamage(b) {
          const body = h("div", {}, h("p", { class: "mut" }, "Đang đọc…")); let timer = null, busy = false;
          const close = modal("Sát thương lên " + b.name + " (#" + b.id + ")", body, { wide: true, onClose: () => clearInterval(timer) });
          const edit = async r => {
            const v = await prompt2("Sửa sát thương · " + r.name, "Tổng sát thương mới (0 = xoá khỏi bảng)", r.damage, "number");
            if (v == null || v === "") return;
            if (await act("boss/damage", { id: b.id, player: r.id, damage: Math.max(0, Math.trunc(+v)) })) draw();
          };
          async function draw() {
            if (busy) return;
            busy = true;
            let r;
            try { r = await api("boss-damage?id=" + b.id); } catch (e) { body.replaceChildren(h("p", { class: "mut" }, e.message)); return; } finally { busy = false; }
            if (!r.alive) { clearInterval(timer); body.replaceChildren(h("p", { class: "empty" }, "Boss không còn sống (" + r.status + "). Bảng sát thương chỉ có khi boss đang sống.")); load(); return; }
            const mx = Math.max(1, ...r.rows.map(x => x.damage));
            body.replaceChildren(
              h("div", { class: "minis", style: "margin-bottom:12px" }, h("div", {}, h("small", {}, "Trạng thái"), h("div", { class: "big", style: "font-size:15px" }, r.status)),
                h("div", {}, h("small", {}, "Người đã đánh"), h("div", { class: "big" }, num(r.rows.length))), h("div", {}, h("small", {}, "Tổng sát thương"), h("div", { class: "big", title: num(r.total) }, short(r.total)))),
              h("p", { class: "hint" }, "Quà boss rơi cho người kết liễu; bảng này là sổ sát thương (quà nhiệm vụ cho người đã đánh). Sửa / xoá chỉ ảnh hưởng con boss đang sống."),
              h("div", { class: "tw", style: "max-height:440px" }, h("table", {}, h("thead", {}, h("tr", {}, ["#", "Người chơi", "Sát thương", "Tỉ lệ", "Trạng thái", ""].map(x => h("th", {}, x)))),
                h("tbody", {}, r.rows.length ? r.rows.map(x => h("tr", {},
                  h("td", {}, x.rank <= 3 ? h("b", { class: "rank r" + x.rank }, x.rank) : x.rank),
                  h("td", {}, h("a", { href: "#", onclick: e => { e.preventDefault(); close(); openPlayer(x.id); } }, h("b", {}, x.name || "#" + x.id)), h("div", { class: "mut", style: "font-size:11px" }, "#" + x.id)),
                  h("td", { style: "min-width:180px" }, h("div", { class: "dmg" }, h("i", { style: "width:" + Math.max(1.5, x.damage * 100 / mx) + "%" })), h("b", {}, num(x.damage))),
                  h("td", {}, String(x.percent).replace(".", ",") + "%"),
                  h("td", {}, x.inZone ? chip("Ở khu boss", "ok") : x.online ? chip("Online, khác khu", "warn") : chip("Offline")),
                  h("td", { style: "white-space:nowrap" }, h("button", { class: "btn small", onclick: () => edit(x) }, "Sửa"), " ",
                    h("button", { class: "btn small danger", onclick: async () => { if (await act("boss/damage", { id: b.id, player: x.id, damage: 0 }, "Xoá " + x.name + " khỏi bảng sát thương " + b.name + "?")) draw(); } }, "Xoá"), " ",
                    h("button", { class: "btn small danger", disabled: !x.online, onclick: async () => { if (await act("player/kick", { id: x.id }, "Đá " + x.name + " khỏi game?")) draw(); } }, "Đá")))) :
                  [h("tr", {}, h("td", { colspan: 6, class: "empty" }, "Chưa ai gây sát thương"))]))));
          }
          draw();
          timer = setInterval(() => { if (document.visibilityState === "visible" && !$("dlg").classList.contains("show")) draw(); }, 3000);
          return close;
        }

        async function editBoss(id) {
          let b; try { b = await api("boss?id=" + id); } catch (e) { return toast(e.message, true); }
          const active = h("input", { type: "checkbox" }); active.checked = b.active;
          const maps = h("input", { style: "width:100%", value: b.maps.map(m => m.id).join(", ") });
          const resp = h("input", { type: "number", min: 1, value: b.respawnMinutes, style: "width:100px" }), first = h("input", { type: "number", min: 0, value: b.firstMinutes, style: "width:100px" });
          const lv = h("input", { type: "number", min: 1, value: b.level, style: "width:100px" }), hp = h("input", { type: "number", min: 1, value: b.hp, style: "width:160px" }), atk = h("input", { type: "number", min: 0, value: b.attack, style: "width:120px" });
          const lootBox = h("div"); const loot = rewardList(lootBox, { mode: "loot", hint: "Quà rơi xuống đất cho người kết liễu (quà tung lại cho từng dòng mỗi lần boss chết)." }); loot.set(b.loot);
          modal("Sửa boss · " + b.name + " (#" + b.id + ")", h("div", {},
            h("div", { class: "field" }, h("label", {}, "Loại"), h("span", {}, b.kind), h("label", {}, "Bật"), h("label", {}, active, " Boss xuất hiện theo lịch"),
              h("label", {}, "Map (ID)"), h("div", {}, maps, h("div", { class: "hint", style: "margin:4px 0 0" }, b.maps.map(m => "#" + m.id + " " + m.name).join(" · "))),
              h("label", {}, "Hồi sinh sau"), h("span", {}, resp, h("span", { class: "mut" }, " phút")), h("label", {}, "Lần đầu sau khi bật"), h("span", {}, first, h("span", { class: "mut" }, " phút")),
              h("label", {}, "Cấp"), lv, h("label", {}, "HP"), hp, h("label", {}, "Tấn công"), atk),
            h("div", { class: "cap" }, "Quà rơi"), lootBox,
            h("p", { class: "hint", style: "margin-top:8px" }, "Boss đang sống giữ chỉ số / quà cũ tới lần xuất hiện sau. Các chỉ số khác (né, chí mạng, chiêu...) sửa ở tab Dữ liệu game → Boss thế giới.")), {
            wide: true, actions: [{
              label: "Lưu boss", primary: true, run: async () => {
                const ids = maps.value.split(/[\s,;]+/).filter(Boolean).map(Number);
                if (!ids.length || ids.some(isNaN)) { toast("Danh sách map không hợp lệ.", true); return false; }
                const r = await act("boss/save", { id, active: active.checked, maps: ids, respawnMinutes: +resp.value, firstMinutes: +first.value, level: +lv.value, hp: +hp.value, attack: +atk.value, loot: loot.get() });
                if (r) load(); return !!r;
              }
            }]
          });
        }
        return { refresh: load };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Giải đấu bộ lạc ================= */
    Tabs.add({
      id: "tournament", icon: "events", group: "Nội dung game", label: "Giải đấu bộ lạc", poll: 10000,
      html: `<div class="sec-head">
    <div>
      <h1>Giải đấu bộ lạc</h1>
      <p class="sub">Mở giải đấu ngay, điều phối giai đoạn thi đấu hoặc huỷ giải đang chạy.</p>
    </div>
  </div>
  <div class="card tour-open-card">
    <div class="ph" style="margin-bottom:6px">
      <div>
        <h2>Mở giải đấu thủ công</h2>
        <p class="hint" style="margin:2px 0 0">Giải sẽ mở ngay lập tức cho các bộ lạc đủ điều kiện tham gia (lịch tự động vẫn chạy song song).</p>
      </div>
    </div>
    <div class="tour-open-layout">
      <select id="tKind" class="tour-kind-select">
        <option value="KID">🧒 Giải Nhi đồng (Tập sự)</option>
        <option value="WARRIOR">⚔️ Giải Chiến binh (Trung cấp)</option>
        <option value="LEADER">👑 Giải Thủ lĩnh (Cấp cao)</option>
        <option value="HERO">🌟 Giải Anh hùng (Đỉnh cao)</option>
        <option value="TEAM">🛡️ Giải Hợp chiến (Đồng đội)</option>
      </select>
      <div class="tour-timer-wrap">
        <label class="mut" style="font-weight:600">Thời gian đăng ký:</label>
        <input id="tSeconds" type="number" value="300" min="10" max="3600" style="width:78px;text-align:center">
        <span class="mut">giây</span>
        <div class="tour-quick-btns">
          <button type="button" class="btn small" onclick="$('tSeconds').value=60">1p</button>
          <button type="button" class="btn small" onclick="$('tSeconds').value=300">5p</button>
          <button type="button" class="btn small" onclick="$('tSeconds').value=600">10p</button>
        </div>
      </div>
      <span style="flex:1"></span>
      <button class="btn primary" id="tOpen" style="padding:7px 18px">▶ Mở giải ngay</button>
    </div>
  </div>
  <div class="card">
    <div class="ph" style="margin-bottom:8px">
      <div>
        <h2>Danh sách giải đang diễn ra</h2>
        <p class="hint" style="margin:2px 0 0">Tự động cập nhật mỗi 10 giây.</p>
      </div>
    </div>
    <div class="tw">
      <table>
        <thead>
          <tr>
            <th>Tên giải đấu</th>
            <th style="width:120px">Giai đoạn</th>
            <th style="width:75px">Số người</th>
            <th style="width:70px">Vòng</th>
            <th style="width:85px">Phe còn</th>
            <th style="width:110px">Trận đang đánh</th>
            <th>Mốc thời gian</th>
            <th style="width:150px;text-align:right">Thao tác</th>
          </tr>
        </thead>
        <tbody id="tRows"></tbody>
      </table>
    </div>
  </div>`,
      setup() {
        const PHASE = { REGISTER: "Đăng ký", WAITING: "Chờ bắt đầu", GATHER: "Tập trung", QUALIFY: "Vòng loại", BREAK: "Nghỉ giữa vòng", ROUND: "Đang thi đấu", DONE: "Kết thúc" };
        async function load() {
          const ts = await api("tournaments");
          fill($("tRows"), ts.map(t => h("tr", {},
            h("td", {}, h("b", {}, t.title)),
            h("td", {}, chip(PHASE[t.phase] || t.phase, t.phase === "ROUND" ? "ok" : t.phase === "DONE" ? "" : "warn")),
            h("td", {}, num(t.entrants)),
            h("td", {}, "Vòng " + t.round),
            h("td", {}, num(t.sidesLeft)),
            h("td", {}, num(t.matches)),
            h("td", { class: "mut" }, t.open + " → " + t.close + " → " + t.start),
            h("td", { style: "white-space:nowrap;text-align:right" },
              h("button", { class: "btn small", onclick: () => act("tournament/skip", { kind: t.kind }).then(load) }, "Giai đoạn sau"), " ",
              h("button", { class: "btn small danger", onclick: () => act("tournament/cancel", { kind: t.kind }, "Huỷ " + t.title + "?").then(load) }, "Huỷ")))),
            "Chưa có giải nào đang chạy. Chọn loại giải ở trên và bấm ‘Mở giải ngay’ để bắt đầu.");
        }
        $("tOpen").onclick = () => act("tournament/open", { kind: $("tKind").value, seconds: +$("tSeconds").value }, "Mở " + $("tKind").selectedOptions[0].textContent + " ngay?").then(load);
        return { refresh: load };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Giftcode ================= */
    Tabs.add({
      id: "codes", group: "Nội dung game", label: "Giftcode", poll: 0,
      html: `<div class="gc-head"><div><h1>Giftcode</h1><p class="sub" style="margin-bottom:0">Tạo mã quà tặng, đặt giới hạn sử dụng và thời gian hiệu lực.</p></div><button class="btn primary" id="gcNew">＋ Tạo giftcode</button></div>
  <div class="gc-stats" id="gcStats"></div>
  <div class="gc-layout">
    <div class="card gc-list-card"><div class="gc-card-head"><h2>Danh sách mã</h2><span class="chip" id="gcCount">0 mã</span></div>
      <input id="gcFilter" class="gc-filter" placeholder="Tìm mã hoặc tên hiển thị…" aria-label="Tìm giftcode">
      <div id="gcRows" class="gc-list" aria-label="Giftcode"></div></div>
  </div>
  `,
      setup(root) {
        let codes = [], editingCode = "";
        const toInput = s => s ? s.replace(" ", "T").slice(0, 16) : "", fromInput = s => s ? s.replace("T", " ") : "";
        function status(c) {
          const now = Date.now();
          const start = c.startAt ? new Date(c.startAt.replace(" ", "T")).getTime() : 0;
          const end = c.endAt ? new Date(c.endAt.replace(" ", "T")).getTime() : 0;
          if (!c.enabled) return ["Đã tắt", ""];
          if (start && start > now) return ["Chờ bắt đầu", "warn"];
          if (end && end <= now) return ["Hết hạn", ""];
          return ["Đang hoạt động", "ok"];
        }
        function render() {
          const f = $("gcFilter").value.trim().toLowerCase();
          const shown = codes.filter(c => !f || (c.code + " " + c.name).toLowerCase().includes(f));
          $("gcCount").textContent = shown.length + " / " + codes.length + " mã";
          const active = codes.filter(c => status(c)[0] === "Đang hoạt động").length;
          $("gcStats").replaceChildren(
            h("div", { class: "gc-stat" }, h("small", {}, "TỔNG MÃ"), h("b", {}, codes.length)),
            h("div", { class: "gc-stat" }, h("small", {}, "ĐANG HOẠT ĐỘNG"), h("b", {}, active)),
            h("div", { class: "gc-stat" }, h("small", {}, "CHƯA HOẠT ĐỘNG"), h("b", {}, codes.length - active)),
            h("div", { class: "gc-stat" }, h("small", {}, "TỔNG PHẦN THƯỞNG"), h("b", {}, codes.reduce((n, c) => n + c.rewards.length, 0))));
          $("gcRows").replaceChildren(...(shown.length ? shown.map(c => {
            const [state, stateClass] = status(c);
            return h("button", { class: "gc-list-item" + (c.code === editingCode ? " selected" : ""), type: "button", onclick: () => edit(c) },
              h("span", {}, h("span", { class: "gc-list-code" }, c.code),
                h("span", { class: "gc-list-meta" }, h("span", {}, c.name || "Chưa đặt tên"), h("span", {}, c.rewards.length + " món quà"), h("span", {}, c.endAt || "Không thời hạn"))),
              chip(state, stateClass));
          }) : [h("div", { class: "hint", style: "padding:18px 10px;text-align:center" }, f ? "Không tìm thấy giftcode phù hợp." : "Chưa có giftcode. Nhấn ‘Tạo giftcode’ để bắt đầu.")]));
        }
        async function loadCodes() { codes = await api("giftcodes"); render(); }
        $("gcFilter").oninput = render;
        function openEditor(c) {
          const data = c || { code: "", name: "", enabled: true, listed: false, usesPerPlayer: 1, maxUses: 0, minLevel: 0, maxLevel: 0, startAt: "", endAt: "", rewards: [] };
          const code = data.code;
          editingCode = code;
          render();
          const codeInput = h("input", { placeholder: "CHỮ HOA, số, _", maxlength: 32, value: code, readonly: !!code });
          codeInput.oninput = () => { if (!code) codeInput.value = codeInput.value.toUpperCase().replace(/[^A-Z0-9_]/g, "").slice(0, 32); };
          const nameInput = h("input", { placeholder: "Tên để nhận biết trong danh sách", value: data.name || "" });
          const enabledInput = h("input", { type: "checkbox" }); enabledInput.checked = !!data.enabled;
          const listedInput = h("input", { type: "checkbox" }); listedInput.checked = !!data.listed;
          const perInput = h("input", { type: "number", min: 0, value: data.usesPerPlayer ?? 1 });
          const maxInput = h("input", { type: "number", min: 0, value: data.maxUses ?? 0 });
          const minLevelInput = h("input", { type: "number", min: 0, value: data.minLevel ?? 0, "aria-label": "Cấp tối thiểu" });
          const maxLevelInput = h("input", { type: "number", min: 0, value: data.maxLevel ?? 0, "aria-label": "Cấp tối đa" });
          const startInput = h("input", { type: "datetime-local", value: toInput(data.startAt) });
          const endInput = h("input", { type: "datetime-local", value: toInput(data.endAt) });
          const rewardBox = h("div", { class: "gc-rewards" });
          const rewards = rewardList(rewardBox, { mode: "reward", separateIconName: true, hint: "Item 1 = vàng (khoá = vàng khoá), 2 = lam thạch. Chỉ số có thể là khoảng để random." });
          rewards.set(data.rewards || []);
          const field = (label, control, full = false) => h("div", { class: "gc-field" + (full ? " full" : "") }, h("label", {}, label), control);
          const section = (title, ...children) => h("div", { class: "gc-form-section" }, h("div", { class: "gc-form-title" }, title), h("div", { class: "gc-fields" }, ...children));
          const body = h("div", { class: "gc-editor-body" },
            section("Thông tin mã", field("Mã giftcode", codeInput), field("Tên hiển thị", nameInput),
              field("Trạng thái", h("div", { class: "gc-checks" }, h("label", {}, enabledInput, " Đang bật"), h("label", {}, listedInput, " Hiện trong danh sách")), true)),
            section("Giới hạn nhận và thời gian", field("Lượt mỗi nhân vật", perInput), field("Tổng lượt server", maxInput),
              field("Khoảng cấp", h("div", { class: "gc-range" }, minLevelInput, h("span", {}, "đến"), maxLevelInput)),
              field("Bắt đầu", startInput), field("Kết thúc", endInput)),
            h("div", { class: "gc-form-section" }, h("div", { class: "gc-form-title" }, "Phần thưởng"), rewardBox));
          const save = async () => {
            const items = rewards.get();
            if (!items.length) { toast("Thêm ít nhất 1 món quà.", true); return false; }
            const minLevel = +minLevelInput.value || 0, maxLevel = +maxLevelInput.value || 0;
            if (minLevel && maxLevel && maxLevel < minLevel) { toast("Cấp tối đa phải lớn hơn hoặc bằng cấp tối thiểu.", true); return false; }
            const startAt = fromInput(startInput.value), endAt = fromInput(endInput.value);
            if (startAt && endAt && new Date(startAt.replace(" ", "T")) >= new Date(endAt.replace(" ", "T"))) { toast("Thời điểm kết thúc phải sau thời điểm bắt đầu.", true); return false; }
            const savedCode = codeInput.value.trim().toUpperCase();
            const result = await act("giftcode/save", {
              code: savedCode, name: nameInput.value, enabled: enabledInput.checked, listed: listedInput.checked,
              usesPerPlayer: Math.max(0, +perInput.value || 0), maxUses: Math.max(0, +maxInput.value || 0), minLevel, maxLevel,
              startAt, endAt, rewards: items
            });
            if (!result) return false;
            editingCode = savedCode;
            await loadCodes();
            return true;
          };
          const remove = async () => {
            if (!code || !await act("giftcode/delete", { code }, "Xoá giftcode " + code + "?")) return false;
            editingCode = "";
            await loadCodes();
            return true;
          };
          modal(code ? "Sửa giftcode · " + code : "Tạo giftcode mới", body, {
            wide: true,
            actions: [
              ...(code ? [{ label: "Xoá mã", danger: true, run: remove }] : []),
              { label: code ? "Lưu thay đổi" : "Tạo giftcode", primary: true, run: save }
            ]
          });
        }
        $("gcNew").onclick = () => openEditor(null);
        function edit(c) { openEditor(c); }
        return { refresh: loadCodes };
      }
    });

/* ------------------------------------------------------------------ */

Object.assign(ICONS, {
      server: '<rect x="3" y="4" width="18" height="7" rx="1.5"/><rect x="3" y="13" width="18" height="7" rx="1.5"/><path d="M7 7.5h.01M7 16.5h.01M11 7.5h6M11 16.5h6"/>',
      shield: '<path d="M12 3l8 3v6c0 4.5-3.4 8.2-8 9-4.6-.8-8-4.5-8-9V6z"/><path d="M9 12l2 2 4-4"/>',
      market: '<path d="M4 9l1.5-5h13L20 9M4 9h16v11H4zM4 9c0 2 1.5 3 3 3s3-1 3-3c0 2 1.5 3 3 3s3-1 3-3c0 2 1.5 3 3 3"/>',
      tribe: '<path d="M5 21V4M5 4h11l-2 4 2 4H5"/>',
      search: '<circle cx="11" cy="11" r="6.5"/><path d="M16 16l5 5"/>'
    });

/* ------------------------------------------------------------------ */

/* ================= Sự kiện: bảng điều khiển ================= */
    Tabs.add({
      id: "gameevents", icon: "events", group: "Nội dung game", label: "Sự kiện", poll: 30000,
      html: `<h1>Sự kiện</h1><p class="sub">Theo dõi trạng thái, lịch vận hành, hoạt động trong game, boss và bảng điểm. Tắt sự kiện sẽ gỡ nội dung đang hoạt động; vật phẩm người chơi đã nhận vẫn được giữ.</p>
        <div id="evSummary" class="ev-summary"></div><div class="ev-dashboard"><nav id="evList" class="ev-list" aria-label="Danh sách sự kiện"></nav><div id="evDetail" class="ev-detail"></div></div>`,
      setup(root) {
        const toLocal = ms => { if (!(ms > 0)) return ""; const d = new Date(ms); d.setMinutes(d.getMinutes() - d.getTimezoneOffset()); return d.toISOString().slice(0, 16); };
        const fromLocal = value => value ? new Date(value).getTime() : 0;
        const when = ms => ms > 0 ? new Date(ms).toLocaleString("vi-VN") : "Chưa đặt";
        let events = [], selectedKey = "", section = "overview", editing = 0;
        const sectionNames = { overview: "Tổng quan", schedule: "Lịch", operations: "Vận hành", leaderboard: "Bảng điểm" };
        const selected = () => events.find(e => e.key === selectedKey) || events[0];
        function summary() {
          const active = events.filter(e => e.active).length;
          const scheduled = events.filter(e => e.start > 0 || e.end > 0).length;
          const items = [["✦", "Sự kiện", events.length], ["●", "Đang bật", active], ["◌", "Đang tắt", events.length - active], ["◷", "Có lịch", scheduled]];
          $("evSummary").replaceChildren(...items.map(([icon, label, value]) => h("div", { class: "ev-summary-card" },
            h("span", { class: "ev-summary-icon" }, icon), h("div", {}, h("small", {}, label), h("strong", {}, num(value))))));
        }
        function schedulePanel(e) {
          const start = h("input", { type: "datetime-local", value: toLocal(e.start) });
          const end = h("input", { type: "datetime-local", value: toLocal(e.end) });
          [start, end].forEach(input => { input.onfocus = () => editing++; input.onblur = () => editing = Math.max(0, editing - 1); });
          const save = async () => {
            const s = fromLocal(start.value), t = fromLocal(end.value);
            if (s && t && t <= s) { toast("Giờ kết thúc phải sau giờ bắt đầu.", "error"); return; }
            editing = 0;
            await act("event/schedule", { key: e.key, start: s, end: t });
            await load();
          };
          return h("div", { class: "card" }, h("h2", {}, "Lịch tự bật / tắt"),
            h("p", { class: "hint" }, "Đến giờ bắt đầu, máy chủ tự bật sự kiện; đến giờ kết thúc, máy chủ tự tắt. Mốc đã chạy sẽ tự xoá. Bỏ trống một ô để không đặt mốc đó."),
            h("div", { class: "ev-grid" },
              h("label", { class: "ev-field" }, "Bắt đầu", start), h("label", { class: "ev-field" }, "Kết thúc", end)),
            h("div", { class: "row" },
              h("button", { class: "btn primary", onclick: save }, "Lưu lịch"),
              h("button", { class: "btn", onclick: async () => { editing = 0; await act("event/schedule", { key: e.key, start: 0, end: 0 }, "Bỏ toàn bộ lịch của " + e.title + "?"); await load(); } }, "Xoá lịch")),
            h("div", { class: "ev-list-row" }, h("span", {}, "Bật lúc"), h("b", {}, when(e.start))),
            h("div", { class: "ev-list-row" }, h("span", {}, "Tắt lúc"), h("b", {}, when(e.end))),
            h("p", { class: "hint" }, "Lịch được máy chủ kiểm tra mỗi 30 giây."));
        }
        function statsPanel(e) {
          const stats = Object.entries(e.stats || {});
          return h("div", { class: "card" }, h("div", { class: "ev-section-heading" }, h("div", {}, h("h2", {}, "Hoạt động sự kiện"), h("p", { class: "hint" }, "Số liệu từ lần khởi động hiện tại của máy chủ.")), chip("Trực tiếp", "ok")),
            stats.length ? h("div", { class: "ev-metrics" }, ...stats.map(([name, value]) => h("div", { class: "ev-metric" }, h("small", {}, name), h("strong", {}, num(value)))))
              : h("div", { class: "empty" }, "Chưa có số liệu cho sự kiện này."));
        }
        function bossPanel(e) {
          const b = e.bossInfo;
          if (!b) return h("div", { class: "card empty" }, "Sự kiện này không có boss được quản lý tại đây.");
          const hpRatio = b.maxHp > 0 ? Math.max(0, Math.min(100, b.hp / b.maxHp * 100)) : 0;
          return h("div", { class: "card ev-boss-card" }, h("div", { class: "ev-section-heading" }, h("div", {}, h("h2", {}, b.name), h("p", { class: "hint" }, "Boss sự kiện · ID #" + b.id)), b.alive ? chip("Đang xuất hiện", "ok") : chip("Chưa xuất hiện")),
            h("div", { class: "ev-list-row" }, h("span", {}, "Trạng thái"), h("b", {}, b.status || (b.alive ? "Đang sống" : "Đã rời bản đồ"))),
            h("div", { class: "ev-list-row" }, h("span", {}, "HP"), h("b", {}, b.alive ? num(b.hp) + " / " + num(b.maxHp) : num(b.maxHp))),
            h("div", { class: "ev-progress" }, h("i", { style: "width:" + (b.alive ? hpRatio : 0) + "%" })),
            h("div", { class: "ev-list-row" }, h("span", {}, "Cấp · hồi sinh"), h("b", {}, num(b.level) + " · " + num(b.respawnMinutes) + " phút")),
            h("div", { class: "row" },
              h("button", { class: "btn primary", disabled: !e.active || b.alive, onclick: async () => { await act("boss/spawn", { id: b.id }, "Gọi " + b.name + " xuất hiện ngay?"); await load(); } }, "Gọi boss ngay"),
              h("button", { class: "btn danger", disabled: !b.alive, onclick: async () => { await act("boss/kill", { id: b.id }, "Gỡ " + b.name + "? Thao tác này không phát quà."); await load(); } }, "Gỡ boss")));
        }
        function operationsPanel(e) {
          const rabbits = e.details || [];
          return h("div", { class: "ev-grid" },
            h("div", {}, bossPanel(e)),
            h("div", { class: "card ev-rabbit-card" }, h("div", { class: "ev-section-heading" }, h("div", {}, h("h2", {}, "Đàn Ngọc thố"), h("p", { class: "hint" }, "Theo dõi trạng thái từng Ngọc thố.")),
                h("button", { class: "btn", disabled: !e.active || !rabbits.length, onclick: async () => { await act("event/rabbits", {}, "Gọi lại Ngọc thố? Cà rốt đang chờ sẽ mất."); await load(); } }, "Gọi lại")),
              rabbits.length ? rabbits.map(r => h("div", { class: "ev-list-row" }, h("b", {}, r.name), h("span", {}, r.status)))
                : h("div", { class: "empty" }, "Sự kiện không có Ngọc thố.")));
        }
        function leaderboardPanel(e) {
          const rows = e.top || [];
          return h("div", { class: "ev-grid" }, statsPanel(e),
            h("div", { class: "card" }, h("h2", {}, "Top điểm sự kiện"), h("p", { class: "hint" }, "Chọn nhân vật để mở hồ sơ."),
              h("div", { class: "tw" }, h("table", {}, h("thead", {}, h("tr", {}, ["Hạng", "Nhân vật", "Cấp", "Điểm"].map(x => h("th", {}, x)))),
                h("tbody", {}, rows.length ? rows.map((r, i) => h("tr", { class: "click", onclick: () => openPlayer(r.id) },
                  h("td", {}, "#" + (i + 1)), h("td", {}, h("b", {}, r.name), h("span", { class: "mut" }, " #" + r.id)), h("td", {}, num(r.level)), h("td", {}, h("b", {}, num(r.points)))))
                  : [h("tr", {}, h("td", { colspan: 4, class: "empty" }, "Chưa có ai ghi điểm."))])))));
        }
        function render() {
          summary();
          const eventList = $("evList");
          eventList.hidden = events.length <= 1;
          eventList.replaceChildren(...events.map(e => h("button", { class: "ev-select" + (e.key === (selected() || {}).key ? " active" : ""), onclick: () => { selectedKey = e.key; section = "overview"; render(); } },
            h("strong", {}, e.title), h("small", {}, e.active ? "Đang bật" : "Đang tắt", " · ", e.start || e.end ? "Có lịch" : "Không có lịch"))));
          const e = selected();
          if (!e) { $("evDetail").replaceChildren(h("div", { class: "card empty" }, "Chưa có sự kiện.")); return; }
          const actions = h("div", { class: "ev-hero-actions" }, e.active
            ? h("button", { class: "btn danger", onclick: async () => { await act("event/set", { key: e.key, on: false }, "Tắt " + e.title + "? Nội dung đang hoạt động của sự kiện sẽ được gỡ."); await load(); } }, "Tắt sự kiện")
            : h("button", { class: "btn primary", onclick: async () => { await act("event/set", { key: e.key, on: true }, "Bật " + e.title + "?"); await load(); } }, "Bật sự kiện"),
            h("button", { class: "btn", onclick: load }, "Làm mới"));
          const header = h("div", { class: "card ev-hero" }, h("div", { class: "ev-hero-main" }, h("div", { class: "ev-emblem", "aria-hidden": "true" }, "🌕"),
            h("div", {}, h("div", { class: "ev-eyebrow" }, "TRUNG THU · QUẢN LÝ SỰ KIỆN"), h("h2", {}, e.title),
              e.active ? chip("Đang vận hành", "ok") : chip("Đang tắt"), e.start || e.end ? chip("Đã lên lịch", "warn") : chip("Vận hành thủ công"),
              h("p", { class: "hint", style: "margin:9px 0 0" }, e.description || ""))), actions);
          const tabs = h("div", { class: "ev-sections", role: "tablist" }, Object.entries(sectionNames).map(([key, title]) => h("button", { class: section === key ? "active" : "", role: "tab", "aria-selected": section === key, onclick: () => { section = key; render(); } }, title)));
          let body = section === "schedule" ? schedulePanel(e) : section === "operations" ? operationsPanel(e)
            : section === "leaderboard" ? leaderboardPanel(e)
              : h("div", { class: "ev-grid" }, statsPanel(e), h("div", { class: "card ev-quick-card" }, h("div", { class: "ev-section-heading" }, h("div", {}, h("h2", {}, "Lịch vận hành"), h("p", { class: "hint" }, "Các mốc tự động tiếp theo.")), chip(e.start || e.end ? "Có lịch" : "Thủ công")),
                h("div", { class: "ev-list-row" }, h("span", {}, "Tự bật"), h("b", {}, when(e.start))),
                h("div", { class: "ev-list-row" }, h("span", {}, "Tự tắt"), h("b", {}, when(e.end))),
                h("button", { class: "btn", onclick: () => { section = "schedule"; render(); } }, "Quản lý lịch")),
              e.bossInfo ? h("div", { class: "card ev-quick-card" }, h("div", { class: "ev-section-heading" }, h("div", {}, h("h2", {}, "Boss sự kiện"), h("p", { class: "hint" }, e.bossInfo.name)), e.bossInfo.alive ? chip("Đang xuất hiện", "ok") : chip("Chưa xuất hiện")),
                h("button", { class: "btn", onclick: () => { section = "operations"; render(); } }, "Mở vận hành")) : "");
          $("evDetail").replaceChildren(header, tabs, body);
        }
        async function load() {
          if (editing) return;
          try {
            const list = await api("events");
            events = Array.isArray(list) ? list : [];
            if (!events.some(e => e.key === selectedKey)) selectedKey = events.length ? events[0].key : "";
            render();
          } catch (error) {
            $("evDetail").replaceChildren(h("div", { class: "card" }, h("h2", {}, "Không tải được sự kiện"), h("p", { class: "mut" }, error.message), h("button", { class: "btn", onclick: load }, "Thử lại")));
          }
        }
        return { refresh: load };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Tra chủ vật phẩm ================= */
    Tabs.add({
      id: "owners", icon: "search", group: "Điều hành", label: "Tra vật phẩm", poll: 0,
      html: `<h1>Tra vật phẩm</h1><p class="sub">Ai đang giữ một mẫu item (túi, rương, đồ mặc, bộ sưu tập, thư chưa nhận, kí gửi) — dùng để soi nhân bản đồ, kiểm tra quà phát ra.</p>
  <div class="cols c-codes">
    <div class="card"><h2>Chọn mẫu item</h2><p class="hint">Gõ tên hoặc ID rồi bấm vào kết quả.</p><div id="owPick"></div></div>
    <div class="card" id="owResult"><div class="empty"><h2>Chưa chọn item</h2></div></div>
  </div>`,
      setup() {
        $("owPick").append(itemSearch(t => load(t.id)));
        async function load(id) {
          const box = $("owResult"); box.replaceChildren(h("h2", {}, "Đang tra…"));
          let r; try { r = await api("item-owners?id=" + id); } catch (e) { box.replaceChildren(h("h2", {}, "Lỗi"), h("p", { class: "mut" }, e.message)); return; }
          box.replaceChildren(
            h("div", { class: "hero" }, ico(r.item.icon, 48), h("div", { class: "main" }, h("h2", {}, r.item.name, h("span", { class: "mut" }, " #" + r.item.id)), h("div", { class: "mut" }, r.item.type))),
            h("div", { class: "minis", style: "margin-bottom:12px" }, h("div", {}, h("small", {}, "Tổng số lượng"), h("div", { class: "big" }, num(r.total))), h("div", {}, h("small", {}, "Số nhân vật giữ"), h("div", { class: "big" }, num(r.ownerCount))), h("div", {}, h("small", {}, "Đang kí gửi"), h("div", { class: "big" }, num(r.market.length)))),
            h("div", { class: "tw" }, h("table", {}, h("thead", {}, h("tr", {}, ["Nhân vật", "Đồ mặc", "Túi", "Rương", "Sưu tập", "Thư", "Tổng"].map(x => h("th", {}, x)))),
              h("tbody", {}, r.owners.length ? r.owners.map(o => h("tr", { class: "click", onclick: () => openPlayer(o.id) }, h("td", {}, h("b", {}, o.name), " ", o.online ? chip("Online", "ok") : "", h("div", { class: "mut", style: "font-size:11px" }, "#" + o.id + " · cấp " + o.level)),
                h("td", {}, num(o.body)), h("td", {}, num(o.bag)), h("td", {}, num(o.box)), h("td", {}, num(o.collection)), h("td", {}, num(o.mail)), h("td", {}, h("b", {}, num(o.total)))))
                : [h("tr", {}, h("td", { colspan: 7, class: "empty" }, "Không ai giữ món này"))]))),
            r.market.length ? h("div", {}, h("div", { class: "cap" }, "Đang kí gửi"), h("div", { class: "list" }, r.market.map(m => h("div", { class: "li static" }, h("div", { class: "main" }, h("div", { class: "t" }, m[1] + " · " + num(m[2]) + " món"), h("div", { class: "s" }, "Mã #" + m[0] + " · giá " + num(m[3]) + " vàng")))))) : "",
            r.ownerCount > r.owners.length ? h("p", { class: "hint" }, "Chỉ hiện " + r.owners.length + " / " + r.ownerCount + " người giữ nhiều nhất.") : "");
        }
        return {};
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Dữ liệu game (bảng cấu hình tlnt_data) =================
       Server khai báo bảng + cột (data/tables); trang dựng form theo kiểu cột, cột JSON dùng trình sửa theo "widget".
       Mọi trình sửa JSON có { el, get(), set(v) } + nút "JSON thô" để sửa tay. */
    Object.assign(ICONS, {
      data: '<ellipse cx="12" cy="5.5" rx="8" ry="3"/><path d="M4 5.5v13c0 1.7 3.6 3 8 3s8-1.3 8-3v-13M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3"/>',
      boss: '<path d="M5 20l2-9 5 4 5-4 2 9zM7 11L4 5l5 3 3-5 3 5 5-3-3 6"/>'
    });
    const SHOP_EXTRAS = [{ key: "p", label: "Giá / lần mua", def: 1 }, { key: "cur", label: "Tiền", options: { 0: "Vàng", 1: "Vàng khoá", 2: "Lam thạch", 3: "Vật phẩm" }, def: 0 }, { key: "ci", label: "Item trả (tiền = vật phẩm)", hint: "ID mẫu item" }];
    const FISH_EXTRAS = [{ key: "w", label: "Tỉ lệ vùng vàng (%)", def: 1 }, { key: "wg", label: "Tỉ lệ vùng xanh (%)", hint: "trống = bằng vùng vàng" }];
    const WHEEL_EXTRAS = [{ key: "w", label: "Trọng số", def: 100 }, { key: "perm", label: "% ra vĩnh viễn", hint: "trống = mặc định" }];
    const ITEM_COLS = new Set(["rod_item", "bait_item", "cost_item", "item_template_id"]);
    const jclone = v => v == null ? v : JSON.parse(JSON.stringify(v));

    /* Nhiều nhóm, mỗi nhóm vài ô đầu (header) + danh sách món (tab shop, mốc phúc lợi). */
    function groupEditor({ header, itemsKey = "items", listOpts = {}, addLabel = "+ Thêm nhóm", groupName = "Nhóm" }) {
      let groups = [];
      const wrap = h("div");
      const add = h("button", { class: "btn small primary", type: "button", onclick: () => { groups.push(make({})); render(); } }, addLabel);
      function make(data) {
        const d = jclone(data) || {};
        const inputs = header.map(f => [f, h("input", { type: f.type === "number" ? "number" : "text", value: d[f.key] == null ? (f.def != null ? f.def : "") : d[f.key], style: "width:" + (f.width || 150) + "px" })]);
        const box = h("div"); const list = rewardList(box, Object.assign({ reorder: true }, listOpts)); list.set(d[itemsKey] || []);
        const g = { d, inputs, list };
        const mv = k => { const i = groups.indexOf(g), j = i + k; if (j < 0 || j >= groups.length) return;[groups[i], groups[j]] = [groups[j], groups[i]]; render(); };
        g.card = h("div", { class: "box grp" }, h("div", { class: "row", style: "margin:0 0 8px" }, h("b", { class: "grp-n" }), inputs.map(([f, inp]) => [h("label", { class: "mut" }, f.label), inp]), h("span", { style: "flex:1" }),
          h("button", { class: "btn small", type: "button", title: "Lên", onclick: () => mv(-1) }, "↑"), h("button", { class: "btn small", type: "button", title: "Xuống", onclick: () => mv(1) }, "↓"),
          h("button", { class: "btn small danger", type: "button", onclick: async () => { if (await ask("Xoá " + groupName.toLowerCase() + " này cùng " + list.size() + " món?")) { groups.splice(groups.indexOf(g), 1); render(); } } }, "Xoá " + groupName.toLowerCase())), box);
        return g;
      }
      function render() { groups.forEach((g, i) => g.card.querySelector(".grp-n").textContent = groupName + " " + (i + 1)); wrap.replaceChildren(...groups.map(g => g.card), add); }
      return {
        el: wrap,
        get: () => groups.map(g => { const o = jclone(g.d); g.inputs.forEach(([f, inp]) => { if (f.type === "number") o[f.key] = inp.value === "" ? 0 : +inp.value; else if (inp.value === "" && f.optional) delete o[f.key]; else o[f.key] = inp.value; }); o[itemsKey] = g.list.get(); return o; }),
        set: v => { groups = (Array.isArray(v) ? v : []).map(make); render(); }
      };
    }
    /* Danh sách món (mảng RewardItem) — mode reward / loot, tuỳ trường phụ; map() / unmap() đổi dạng lưu (vd ô vòng quay). */
    function itemsEditor(opts, map, unmap) {
      const box = h("div"); const list = rewardList(box, Object.assign({ reorder: true }, opts));
      return { el: box, get: () => unmap ? list.get().map(unmap) : list.get(), set: v => list.set((Array.isArray(v) ? v : []).map(x => map ? map(x) : x)) };
    }
    /* Ô vòng quay {w, perm, item:{...}} <-> 1 món phẳng {...item, w, perm}. */
    const wheelIn = s => Object.assign({}, s.item || {}, { w: s.w }, s.perm != null ? { perm: s.perm } : {});
    const wheelOut = r => { const o = { w: r.w == null ? 0 : r.w }; if (r.perm != null) o.perm = r.perm; const it = Object.assign({}, r); delete it.w; delete it.perm; o.item = it; return o; };
    /* Cấu hình hái cây {maps, max, total, respawn [min,max], time, items}. */
    function harvestEditor() {
      let base = {};
      const on = h("input", { type: "checkbox" }), maps = h("input", { style: "width:100%", placeholder: "VD 9, 10, 11" });
      const max = h("input", { type: "number", min: 0, style: "width:100px" }), total = h("input", { type: "number", min: 0, style: "width:100px" });
      const r1 = h("input", { type: "number", min: 1, style: "width:90px" }), r2 = h("input", { type: "number", min: 1, style: "width:90px" }), time = h("input", { type: "number", min: 0, style: "width:110px" });
      const items = itemsEditor({ lockTri: "không khoá" });
      const form = h("div", { class: "field" }, h("label", {}, "Mọc"), h("label", {}, on, " Cây mọc trên map (bỏ chọn = không mọc)"),
        h("label", {}, "Map"), maps, h("label", {}, "Tối đa / khu"), max, h("label", {}, "Tối đa toàn server"), h("span", {}, total, h("span", { class: "mut" }, " 0 = không giới hạn")),
        h("label", {}, "Mọc lại sau"), h("span", {}, r1, h("span", { class: "mut" }, " – "), r2, h("span", { class: "mut" }, " giây (random)")), h("label", {}, "Thời gian hái"), h("span", {}, time, h("span", { class: "mut" }, " ms")),
        h("label", {}, "Đồ hái được"), items.el);
      return {
        el: form,
        get: () => {
          if (!on.checked) return null;
          const o = jclone(base) || {};
          o.maps = maps.value.split(/[\s,;]+/).filter(Boolean).map(Number); if (o.maps.some(isNaN)) throw new Error("Danh sách map không hợp lệ.");
          o.max = +max.value; o.total = +total.value; o.respawn = [+r1.value, +(r2.value || r1.value)]; o.time = +time.value; o.items = items.get(); return o;
        },
        set: v => {
          base = v && typeof v === "object" ? v : {}; on.checked = !!(v && typeof v === "object");
          maps.value = (base.maps || []).join(", "); max.value = base.max == null ? 1 : base.max; total.value = base.total || 0;
          const rs = Array.isArray(base.respawn) ? base.respawn : [base.respawn || 60, base.respawn || 60]; r1.value = rs[0]; r2.value = rs[1]; time.value = base.time == null ? 3000 : base.time; items.set(base.items || []);
        }
      };
    }
    function intListEditor() {
      const inp = h("input", { style: "width:100%", placeholder: "VD 6, 7, 8" });
      return { el: inp, get: () => { const a = inp.value.split(/[\s,;]+/).filter(Boolean).map(Number); if (a.some(isNaN)) throw new Error("Danh sách số không hợp lệ."); return a; }, set: v => inp.value = Array.isArray(v) ? v.join(", ") : "" };
    }
    function rawJsonEditor() {
      const ta = h("textarea", { class: "json", spellcheck: "false" });
      return { el: ta, get: () => { const t = ta.value.trim(); if (!t) return null; try { return JSON.parse(t); } catch (e) { throw new Error("JSON sai: " + e.message); } }, set: v => ta.value = v == null ? "" : typeof v === "string" ? v : JSON.stringify(v, null, 1) };
    }
    function jsonWidget(col) {
      switch (col.widget) {
        case "rewards": return itemsEditor({ lockTri: "khoá" });
        case "loot": return itemsEditor({ mode: "loot" });
        case "fishLoot": return itemsEditor({ extras: FISH_EXTRAS, lockTri: "khoá" });
        case "wheelSlots": return itemsEditor({ extras: WHEEL_EXTRAS, lockTri: "không khoá", max: 12 }, wheelIn, wheelOut);
        case "milestones": return groupEditor({ header: [{ key: "target", label: "Mốc", type: "number", width: 110 }], listOpts: { lockTri: "khoá" }, addLabel: "+ Thêm mốc", groupName: "Mốc" });
        case "shopTabs": return groupEditor({ header: [{ key: "name", label: "Tên tab" }, { key: "icon", label: "Icon", width: 110, optional: true }], listOpts: { extras: SHOP_EXTRAS, lockTri: "khoá nếu trả vàng khoá" }, addLabel: "+ Thêm tab", groupName: "Tab" });
        case "harvest": return harvestEditor();
        case "intList": return intListEditor();
        default: return rawJsonEditor();
      }
    }
    /* Bọc trình sửa JSON: thêm nút "JSON thô" (sửa tay toàn bộ cột). */
    function jsonField(col, value) {
      const w = jsonWidget(col);
      if (typeof value === "string" && col.widget && col.widget !== "json") { toast("Cột " + col.label + " đang chứa JSON hỏng — sửa bằng JSON thô.", true); }
      try { w.set(typeof value === "string" && col.widget !== "json" ? null : value); } catch (e) { toast(e.message, true); }
      if (!col.widget || col.widget === "json" || col.widget === "intList") return w;
      const raw = h("button", {
        class: "btn small", type: "button", onclick: () => {
          let cur; try { cur = w.get(); } catch (e) { cur = null; }
          const ta = rawJsonEditor(); ta.set(typeof value === "string" ? value : cur);
          modal("JSON thô · " + col.label, h("div", {}, h("p", { class: "hint" }, "Sửa trực tiếp JSON của cột. Tên / icon item tự bỏ khi lưu."), ta.el), {
            wide: true, actions: [{ label: "Áp dụng", primary: true, run: () => { try { w.set(ta.get()); value = null; return true; } catch (e) { toast(e.message, true); return false; } } }]
          });
        }
      }, "JSON thô");
      return { el: h("div", {}, h("div", { class: "row", style: "justify-content:flex-end;margin:0 0 6px" }, raw), w.el), get: w.get, set: w.set };
    }
    Tabs.add({
      id: "data", icon: "data", group: "Nội dung game", label: "Dữ liệu game", poll: 0,
      html: `<div class="dt-page">
  <div class="dt-overview">
    <div class="dt-overview-mark" aria-hidden="true"><svg viewBox="0 0 24 24"><ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v7c0 1.7 3.6 3 8 3s8-1.3 8-3V5M4 12v7c0 1.7 3.6 3 8 3s8-1.3 8-3v-7"/></svg></div>
    <div class="dt-overview-copy"><span class="dt-kicker">CẤU HÌNH MÁY CHỦ</span><h1>Dữ liệu game</h1><p>Quản lý các bảng dữ liệu trong <code>tlnt_data</code>. Thay đổi được nạp vào server ngay sau khi lưu.</p></div>
    <div class="dt-overview-status"><span class="dt-live-dot"></span><span>Đồng bộ trực tiếp</span><b id="dtTableCountBadge">– bảng</b></div>
  </div>
  <div class="dt-workspace">
    <aside class="dt-library" aria-label="Danh sách bảng dữ liệu">
      <div class="dt-library-head"><div><span class="dt-kicker">THƯ VIỆN</span><h2>Bảng dữ liệu</h2></div><span id="dtTableCount" class="dt-library-count">–</span></div>
      <label class="dt-table-search"><span aria-hidden="true">⌕</span><input id="dtTableQ" type="search" placeholder="Tìm tên bảng…" autocomplete="off" aria-label="Tìm bảng dữ liệu"></label>
      <div class="dt-nav" id="dtNav" role="listbox" aria-label="Chọn bảng dữ liệu"></div>
      <div class="dt-library-note"><span aria-hidden="true">i</span><span>Chọn một bảng để xem, tìm kiếm hoặc chỉnh sửa dữ liệu.</span></div>
    </aside>
    <div class="dt-main">
      <div class="card dt-editor">
        <div class="dt-editor-head">
          <div class="dt-heading-copy"><span class="dt-kicker"><i class="dt-heading-dot"></i> BẢNG ĐANG MỞ</span><div class="dt-title-line"><h2 id="dtTitle">–</h2><code id="dtCode">–</code></div><p class="dt-desc" id="dtDesc"></p></div>
          <button class="btn primary dt-add" id="dtAdd">＋ Thêm dòng</button>
        </div>
        <div class="dt-toolbar">
          <label class="dt-row-search"><span aria-hidden="true">⌕</span><input id="dtQ" type="search" placeholder="Tìm trong các dòng…" autocomplete="off" aria-label="Tìm nội dung trong bảng"></label>
          <span class="dt-count" id="dtCount">–</span>
          <button class="btn dt-reload" id="dtReload" type="button"><span aria-hidden="true">↻</span> Tải lại</button>
        </div>
        <div class="tw dt-table-shell"><table><thead id="dtHead"></thead><tbody id="dtRows"></tbody></table></div>
      </div>
    </div>
  </div>
</div>`,
      setup() {
        let tables = [], cur = null, rows = [];
        const itemNames = new Map();
        const tableSearch = $("dtTableQ");
        async function itemName(id) {
          if (!(id > 0)) return null;
          if (!itemNames.has(id)) itemNames.set(id, api("items?q=" + id).then(l => l.find(t => t.id === id) || null).catch(() => null));
          return itemNames.get(id);
        }
        function cellText(col, v) {
          if (v == null || v === "") return h("span", { class: "mut" }, "–");
          if (col.type === "bool") return v ? chip("Bật", "ok") : chip("Tắt");
          if (col.type === "json") {
            if (typeof v === "string") return chip("JSON hỏng", "bad");
            if (Array.isArray(v)) return v.length + (col.widget === "milestones" ? " mốc" : col.widget === "shopTabs" ? " tab" : col.widget === "intList" ? " (" + v.join(", ") + ")" : " món");
            return "{…}";
          }
          if (ITEM_COLS.has(col.name)) { const sp = h("span", {}, "#" + v); itemName(v).then(t => { if (t) sp.replaceChildren(ico(t.icon, 22), " " + t.name + " #" + v); }); return sp; }
          const s = String(v); return s.length > 80 ? s.slice(0, 80) + "…" : s;
        }
        function nav() {
          const q = tableSearch.value.trim().toLocaleLowerCase("vi");
          const list = tables.filter(t => !q || (t.title + " " + t.name + " " + t.description).toLocaleLowerCase("vi").includes(q));
          $("dtTableCount").textContent = String(tables.length);
          $("dtTableCountBadge").textContent = tables.length + " bảng";
          const buttons = list.map(t => h("button", {
            type: "button", role: "option", "aria-selected": String(cur === t), class: "dt-tab" + (cur === t ? " on" : ""),
            onclick: () => select(t)
          }, h("span", { class: "dt-tab-mark", "aria-hidden": "true" }, "▦"),
            h("span", { class: "dt-tab-copy" }, h("b", {}, t.title), h("small", {}, t.name)),
            h("span", { class: "dt-tab-arrow", "aria-hidden": "true" }, "›")));
          if (!buttons.length) buttons.push(h("div", { class: "dt-nav-empty" }, "Không tìm thấy bảng phù hợp."));
          $("dtNav").replaceChildren(...buttons);
        }
        async function select(t) {
          cur = t; rows = []; $("dtQ").value = ""; nav();
          $("dtTitle").textContent = t.title; $("dtCode").textContent = t.name; $("dtDesc").textContent = t.description; $("dtAdd").hidden = !t.insert;
          const sum = t.summary.map(n => t.cols.find(c => c.name === n));
          const jsonCols = t.cols.filter(c => c.type === "json" && !t.summary.includes(c.name));
          $("dtHead").replaceChildren(h("tr", {}, sum.map(c => h("th", {}, c.label)), jsonCols.map(c => h("th", {}, c.label)), h("th", {}, "")));
          $("dtCount").textContent = "Đang tải…";
          fill($("dtRows"), [], "Đang tải…");
          await load();
        }
        async function load() {
          if (!cur) return;
          const t = cur;
          $("dtReload").classList.add("busy");
          try {
            const loaded = await api("data/rows?table=" + t.name);
            if (t !== cur) return;
            rows = loaded; render();
          } catch (e) { if (t === cur) { $("dtCount").textContent = "Không tải được"; toast(e.message, true); } }
          finally { $("dtReload").classList.remove("busy"); }
        }
        function render() {
          const t = cur, q = $("dtQ").value.trim().toLowerCase();
          const sum = t.summary.map(n => t.cols.find(c => c.name === n)), jsonCols = t.cols.filter(c => c.type === "json" && !t.summary.includes(c.name));
          const list = rows.filter(r => !q || JSON.stringify(r).toLowerCase().includes(q));
          $("dtCount").textContent = list.length === rows.length ? rows.length + " dòng" : list.length + " / " + rows.length + " dòng";
          fill($("dtRows"), list.map(r => h("tr", { class: "click", onclick: () => edit(r) }, sum.map(c => h("td", {}, cellText(c, r[c.name]))), jsonCols.map(c => h("td", { class: "mut" }, cellText(c, r[c.name]))),
            h("td", { style: "white-space:nowrap;text-align:right" }, h("button", { class: "btn small", onclick: e => { e.stopPropagation(); edit(r); } }, "Sửa"),
              t.insert ? [" ", h("button", { class: "btn small", title: "Tạo dòng mới từ dòng này", onclick: e => { e.stopPropagation(); edit(r, true); } }, "Nhân bản")] : "",
              t.delete ? [" ", h("button", { class: "btn small danger", onclick: async e => { e.stopPropagation(); const key = {}; t.pk.forEach(k => key[k] = r[k]); if (await act("data/delete", { table: t.name, key }, "Xoá dòng " + t.pk.map(k => k + " = " + r[k]).join(", ") + " của " + t.title + "?")) load(); } }, "Xoá")] : ""))), "Bảng trống");
        }
        /* Popup sửa / thêm 1 dòng. copy = true: thêm dòng mới từ dữ liệu dòng r. */
        function edit(r, copy) {
          const t = cur, isNew = !r || copy, src = jclone(r) || {};
          if (isNew && copy) t.pk.forEach(k => { if (t.cols.find(c => c.name === k).type === "int" && rows.length) src[k] = Math.max(...rows.map(x => +x[k] || 0)) + 1; });
          const F = [];
          const form = h("div", { class: "field dt-form" });
          for (const col of t.cols) {
            const v = src[col.name];
            const label = h("label", {}, col.label, col.hint ? h("small", { class: "dt-hint" }, col.hint) : "");
            if (col.readOnly && !isNew) { form.append(label, h("div", {}, v == null || v === "" ? h("span", { class: "mut" }, "–") : String(v))); continue; }
            let inp, get;
            if (col.type === "bool") { inp = h("input", { type: "checkbox" }); inp.checked = isNew && !copy ? true : !!v; get = () => inp.checked; inp = h("label", {}, inp, " Bật"); }
            else if (col.type === "json") { const w = jsonField(col, v == null ? null : v); inp = w.el; get = w.get; }
            else if (col.type === "text") { inp = h("textarea", { style: "min-height:70px" }); inp.value = v == null ? "" : v; get = () => inp.value; }
            else if (ITEM_COLS.has(col.name)) {
              const n = h("input", { type: "number", value: v == null ? "" : v, style: "width:120px" }), lab = h("span", { class: "mut" });
              const show = () => { lab.replaceChildren(); itemName(+n.value).then(tp => { if (tp && +n.value === tp.id) lab.replaceChildren(ico(tp.icon, 26), " " + tp.name); else if (n.value) lab.textContent = "không có mẫu item này"; }); };
              n.oninput = show; show();
              inp = h("div", {}, h("div", { class: "row", style: "margin:0 0 6px" }, n, lab), itemSearch(tp => { n.value = tp.id; show(); }));
              get = () => n.value === "" ? null : +n.value;
            } else {
              const num2 = col.type === "int" || col.type === "long" || col.type === "float";
              inp = h("input", { type: num2 ? "number" : "text", step: col.type === "float" ? "any" : null, value: v == null ? "" : v, style: num2 ? "width:180px" : "width:100%" });
              get = () => num2 ? (inp.value === "" ? null : +inp.value) : inp.value;
            }
            F.push([col, get]); form.append(label, inp);
          }
          const key = {}; if (!isNew) t.pk.forEach(k => key[k] = r[k]);
          modal((isNew ? "Thêm dòng · " : "Sửa · ") + t.title + (isNew ? "" : " (" + t.pk.map(k => k + " " + r[k]).join(", ") + ")"), form, {
            wide: true, actions: [
              ...(!isNew && t.delete ? [{ label: "Xoá dòng", danger: true, run: async () => { const ok2 = await act("data/delete", { table: t.name, key }, "Xoá dòng này?"); if (ok2) load(); return !!ok2; } }] : []),
              {
                label: isNew ? "Thêm" : "Lưu", primary: true, run: async () => {
                  const row = {};
                  try { for (const [col, get] of F) row[col.name] = get(); } catch (e) { toast(e.message, true); return false; }
                  const res = await act("data/save", isNew ? { table: t.name, row } : { table: t.name, key, row });
                  if (res) load(); return !!res;
                }
              }]
          });
        }
        $("dtQ").oninput = () => cur && render();
        tableSearch.oninput = nav;
        $("dtReload").onclick = load;
        $("dtAdd").onclick = () => cur && edit(null);
        return {
          refresh: async () => {
            if (!tables.length) { tables = await api("data/tables"); nav(); if (tables.length) await select(tables[0]); }
          }
        };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Kí gửi ================= */
    Tabs.add({
      id: "market", group: "Nội dung game", label: "Kí gửi", poll: 30000,
      html: `<h1>Kí gửi</h1><p class="sub">Món đang bán trên chợ kí gửi. Gỡ xuống thì đồ về hòm thư người bán (không hoàn phí).</p>
  <div class="card"><div class="row"><input id="mkQ" class="grow" type="search" placeholder="Lọc theo tên item / người bán"><span class="mut" id="mkCount"></span></div>
    <div class="list" id="mkList"></div></div>`,
      setup() {
        let rows = [];
        function render() {
          const q = $("mkQ").value.trim().toLowerCase();
          const list = rows.filter(r => !q || (r.item.name + " " + r.seller).toLowerCase().includes(q));
          $("mkCount").textContent = list.length + " / " + rows.length + " món";
          $("mkList").replaceChildren(...(list.length ? list.map(r => h("div", { class: "li static mk" },
            h("div", { class: "mk-slot" }, tile(r.item, null)),
            h("div", { class: "main" }, h("div", { class: "t" }, r.item.name + (r.item.qty > 1 ? " x" + num(r.item.qty) : "") + (r.item.stars ? " +" + r.item.stars : "")),
              h("div", { class: "s" }, "Người bán: ", h("a", { href: "#", onclick: e => { e.preventDefault(); openPlayer(r.sellerId); } }, r.seller), " · mã #" + r.id),
              h("div", { class: "s" }, "Đăng " + r.created + " · hết hạn sau " + dur(r.secondsLeft))),
            h("div", { class: "chips-r" }, h("b", { class: "price" }, num(r.price) + " vàng"),
              h("button", { class: "btn small danger", onclick: async () => { const why = await prompt2("Gỡ món #" + r.id, "Lý do (gửi kèm thư cho người bán)", ""); if (why != null && await act("market/cancel", { id: r.id, reason: why })) load(); } }, "Gỡ xuống"))))
            : [h("p", { class: "empty" }, "Không có món nào đang bán")]));
        }
        async function load() { rows = await api("market"); render(); }
        $("mkQ").oninput = render;
        return { refresh: load };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Bộ lạc ================= */
    Tabs.add({
      id: "tribes", icon: "tribe", group: "Nội dung game", label: "Bộ lạc", poll: 0,
      html: `<h1>Bộ lạc</h1><p class="sub">Bấm một bộ lạc để sửa thông tin, ngân sách, cấp và quản lý thành viên.</p>
  <div class="card"><div class="row"><input id="trQ" class="grow" type="search" placeholder="Lọc theo tên bộ lạc / thành viên"><span class="mut" id="trCount"></span><button class="btn small" id="trReload">Làm mới</button></div>
    <div class="list" id="trList"></div></div>`,
      setup() {
        let rows = [];
        const leader = t => (t.members.find(m => m.role === "Tù trưởng") || {}).name || "–";
        function render() {
          const q = $("trQ").value.trim().toLowerCase();
          const list = rows.filter(t => !q || (t.name + " " + t.members.map(m => m.name).join(" ")).toLowerCase().includes(q));
          $("trCount").textContent = list.length + " bộ lạc";
          $("trList").replaceChildren(...(list.length ? list.map(t => h("div", { class: "li", tabindex: 0, role: "button", onclick: () => open(t) },
            h("div", { class: "av" }, t.name.slice(0, 1).toUpperCase()),
            h("div", { class: "main" }, h("div", { class: "t" }, t.name, h("span", { class: "mut", style: "font-weight:500" }, " · cấp " + t.level)),
              h("div", { class: "s" }, "Tù trưởng " + leader(t) + " · " + t.members.length + "/" + t.maxMembers + " thành viên" + (t.applicants ? " · " + t.applicants + " đơn xin vào" : "")),
              h("div", { class: "s" }, "Ngân sách " + short(t.gold) + " vàng · " + num(t.diamond) + " lam thạch")),
            h("div", { class: "chips-r" }, chip("EXP " + num(t.exp) + " / " + num(t.need)))))
            : [h("p", { class: "empty" }, "Chưa có bộ lạc nào")]));
        }
        function open(t) {
          const F = {
            name: h("input", { value: t.name }), slogan: h("input", { value: t.slogan || "" }), level: h("input", { type: "number", min: 0, max: 100, value: t.level }),
            exp: h("input", { type: "number", min: 0, value: t.exp }), gold: h("input", { type: "number", min: 0, value: t.gold }), diamond: h("input", { type: "number", min: 0, value: t.diamond })
          };
          const body = h("div");
          const close = modal("Bộ lạc · " + t.name, body, { wide: true });
          const refresh = async () => { await load(); const nt = rows.find(x => x.id === t.id); if (!nt) { close(); return; } t = nt; draw(); };
          const roleSel = m => { const s = h("select", {}, h("option", { value: 0 }, "Tù trưởng"), h("option", { value: 1 }, "Phó tù trưởng"), h("option", { value: 2 }, "Thành viên")); s.value = m.roleId; return s; };
          function draw() {
            body.replaceChildren(
              h("div", { class: "hero" }, h("div", { class: "av big" }, t.name.slice(0, 1).toUpperCase()), h("div", { class: "main" }, h("h2", {}, t.name),
                h("div", { class: "mut" }, "Cấp " + t.level + " · " + t.members.length + "/" + t.maxMembers + " thành viên · rương " + t.warehouse + " món · " + t.applicants + " đơn xin vào"))),
              h("div", { class: "sect" }, h("h4", {}, "Thông tin bộ lạc"), h("div", { class: "field" }, h("label", {}, "Tên"), F.name, h("label", {}, "Khẩu hiệu"), F.slogan,
                h("label", {}, "Cấp (0-100)"), F.level, h("label", {}, "EXP trong cấp"), h("div", {}, F.exp, h("div", { class: "hint", style: "margin:4px 0 0" }, "Cần " + num(t.need) + " để lên cấp")),
                h("label", {}, "Ngân sách vàng"), F.gold, h("label", {}, "Ngân sách lam thạch"), F.diamond),
                h("div", { class: "row", style: "margin-top:12px;justify-content:flex-end" }, h("button", {
                  class: "btn primary", onclick: async () => {
                    if (await act("tribe/update", { id: t.id, name: F.name.value, slogan: F.slogan.value, level: +F.level.value, exp: +F.exp.value, gold: +F.gold.value, diamond: +F.diamond.value })) refresh();
                  }
                }, "Lưu thông tin"))),
              h("div", { class: "sect" }, h("h4", {}, "Thành viên (" + t.members.length + ")"), h("div", { class: "list" }, t.members.map(m => {
                const sel = roleSel(m);
                return h("div", { class: "li static" }, h("div", { class: "av" }, m.name.slice(0, 1).toUpperCase()),
                  h("div", { class: "main" }, h("div", { class: "t" }, h("a", { href: "#", onclick: e => { e.preventDefault(); openPlayer(m.id); } }, m.name), h("span", { class: "mut", style: "font-weight:500" }, " · cấp " + m.level)),
                    h("div", { class: "s" }, "Cống hiến " + num(m.points) + " · góp " + short(m.gold) + " vàng, " + num(m.diamond) + " lam thạch")),
                  h("div", { class: "chips-r" }, sel, h("button", { class: "btn small", onclick: async () => { if (await act("tribe/role", { id: t.id, player: m.id, role: +sel.value }, "Đặt " + m.name + " làm " + sel.selectedOptions[0].textContent + "?")) refresh(); } }, "Lưu"),
                    m.roleId === 0 ? "" : h("button", { class: "btn small danger", onclick: async () => { if (await act("tribe/kick", { id: t.id, player: m.id }, "Đưa " + m.name + " ra khỏi " + t.name + "?")) refresh(); } }, "Đuổi")));
              }))));
          }
          draw();
        }
        async function load() { rows = await api("tribes"); rows.sort((a, b) => b.level - a.level || b.members.length - a.members.length); render(); }
        $("trQ").oninput = render; $("trReload").onclick = () => load().catch(e => toast(e.message, true));
        return { refresh: async () => { if (!rows.length) await load(); } };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Máy chủ ================= */
    Tabs.add({
      id: "server", icon: "server", group: "Hệ thống", label: "Máy chủ", poll: 15000,
      html: `<div class="ph">
    <div>
      <h1>Máy chủ</h1>
      <p class="sub">Nhân EXP, JVM, cơ sở dữ liệu và map.</p>
    </div>
  </div>
  <div class="system-grid">
    <div class="card sun">
      <div class="ph" style="margin-bottom:6px">
        <div>
          <h2>Nhân EXP (Runtime)</h2>
          <p class="hint" style="margin:2px 0 0">Có hiệu lực ngay cho mọi đòn đánh, đến lần khởi động lại (sửa exp.rate trong server.properties để lưu lâu dài).</p>
        </div>
      </div>
      <div id="svRt" style="margin-top:10px"></div>
    </div>
    <div class="card">
      <div class="ph" style="margin-bottom:8px">
        <div>
          <h2>Máy ảo JVM</h2>
          <p class="hint" style="margin:2px 0 0">Bộ nhớ Heap, Non-Heap, số luồng và thu gom rác.</p>
        </div>
        <button class="btn small" id="svGc">Dọn bộ nhớ (GC)</button>
      </div>
      <div id="svJvm"></div>
    </div>
  
    <div class="card">
      <div class="ph" style="margin-bottom:8px">
        <div>
          <h2>Cơ sở dữ liệu</h2>
          <p class="hint" style="margin:2px 0 0">HikariCP connection pool và thống kê bảng dữ liệu.</p>
        </div>
      </div>
      <div id="svDb"></div>
    </div>
    <div class="card">
      <div class="ph" style="margin-bottom:8px">
        <div>
          <h2>Bản đồ &amp; khu vực</h2>
          <p class="hint" id="svMapSum" style="margin:2px 0 0">&nbsp;</p>
        </div>
      </div>
      <div class="row" style="margin-top:10px;gap:10px;align-items:center">
        <input id="svMapQ" type="search" class="grow" placeholder="🔍 Lọc map theo tên hoặc ID...">
        <label class="mut" style="display:flex;align-items:center;gap:6px;font-size:12px;cursor:pointer;white-space:nowrap"><input type="checkbox" id="svMapAll"> Hiện cả map trống</label>
      </div>
      <div class="tw" style="margin-top:8px;max-height:360px">
        <table>
          <thead><tr><th style="width:65px">ID</th><th>Tên Map</th><th style="width:90px">Số khu</th><th style="width:90px">Người</th></tr></thead>
          <tbody id="svMaps"></tbody>
        </table>
      </div>
    </div>
  </div>`,
      setup() {
        let data = null, rtDrawn = false;
        const bytes = b => b >= 1 << 30 ? (b / (1 << 30)).toFixed(2).replace(".", ",") + " GB" : b >= 1 << 20 ? (b / (1 << 20)).toFixed(1).replace(".", ",") + " MB" : num(Math.round(b / 1024)) + " KB";
        function drawRuntime(rt) {
          const rate = h("input", { type: "number", min: 1, max: 100, step: 1, value: rt.expRate, style: "width:80px;text-align:center", inputmode: "numeric" });
          $("svRt").replaceChildren(h("div", { class: "row", style: "gap:10px;align-items:center;margin:0" },
            h("label", { class: "mut", style: "font-weight:600" }, "Hệ số"),
            rate,
            h("span", { class: "chip warn", style: "font-size:12px" }, "x" + rt.expRate),
            h("span", { class: "mut", style: "font-size:12px" }, "(sự kiện x2, x3...)"),
            h("span", { style: "flex:1" }),
            h("button", { class: "btn primary", onclick: async () => { if (await act("server/runtime", { expRate: +rate.value }, "Đặt nhân EXP toàn server = x" + rate.value + "?")) { rtDrawn = false; load(); } } }, "Áp dụng")));
          rtDrawn = true;
        }
        function render() {
          const d = data, j = d.jvm;
          if (!rtDrawn) drawRuntime(d.runtime);
          $("svJvm").replaceChildren(h("div", { class: "minis" },
            h("div", {}, h("small", {}, "Heap đang dùng"), h("div", { class: "big" }, num(j.heapUsedMb) + " MB"), h("small", {}, "cấp " + num(j.heapCommittedMb) + " / tối đa " + num(j.heapMaxMb) + " MB")),
            h("div", {}, h("small", {}, "Ngoài heap"), h("div", { class: "big" }, num(j.nonHeapMb) + " MB")),
            h("div", {}, h("small", {}, "Luồng"), h("div", { class: "big" }, num(j.threads)), h("small", {}, "đỉnh " + num(j.peakThreads) + " · nền " + num(j.daemonThreads))),
            h("div", {}, h("small", {}, "Chạy từ"), h("div", { class: "big", style: "font-size:15px" }, j.startedAt), h("small", {}, dur(j.uptimeSeconds)))),
            h("div", { class: "kv", style: "margin-top:12px" }, kv("Máy ảo", j.vm), kv("Hệ điều hành", j.os + " · " + j.cores + " nhân" + (j.load >= 0 ? " · tải " + j.load.toFixed(2) : "")), kv("Lớp đã nạp", num(j.classes)),
              kv("Tham số", j.args || "(mặc định)"), ...j.gc.map(g => kv("GC " + g[0], num(g[1]) + " lần · " + num(g[2]) + " ms"))));
          $("svDb").replaceChildren(h("div", { class: "tw" }, h("table", {}, h("thead", {}, h("tr", {}, ["Pool", "Đang dùng", "Rảnh", "Tổng", "Chờ"].map(x => h("th", {}, x)))),
            h("tbody", {}, d.pools.map(p => h("tr", {}, h("td", {}, p[0]), h("td", {}, p[1]), h("td", {}, p[2]), h("td", {}, p[3]), h("td", {}, p[4] ? h("b", { style: "color:var(--red)" }, p[4]) : 0)))))),
            h("p", { class: "hint", style: "margin:8px 0" }, "Lượt lưu đang chờ ghi: " + num(d.pendingWrites)),
            h("div", { class: "tw", style: "max-height:300px" }, h("table", {}, h("thead", {}, h("tr", {}, ["Bảng", "Số dòng (ước)", "Dung lượng"].map(x => h("th", {}, x)))),
              h("tbody", {}, d.tables.map(t => h("tr", {}, h("td", {}, t[0]), h("td", {}, num(t[1])), h("td", {}, bytes(t[2]))))))));
          renderMaps();
        }
        function renderMaps() {
          if (!data) return;
          const q = $("svMapQ").value.trim().toLowerCase(), all = $("svMapAll").checked;
          const list = data.maps.filter(m => (all || m.players > 0) && (!q || m.name.toLowerCase().includes(q) || String(m.id) === q));
          $("svMapSum").textContent = data.maps.length + " map · " + num(data.maps.reduce((a, m) => a + m.zones, 0)) + " khu đang mở · " + num(data.maps.reduce((a, m) => a + m.players, 0)) + " người";
          fill($("svMaps"), list.map(m => h("tr", {}, h("td", {}, m.id), h("td", {}, m.name), h("td", {}, m.zones), h("td", {}, m.players))), all || q ? "Không có map phù hợp" : "Chưa có ai trong map nào (bật “Hiện cả map trống”)");
        }
        async function load() { data = await api("server"); render(); }
        $("svMapQ").oninput = renderMaps; $("svMapAll").onchange = renderMaps;
        $("svGc").onclick = () => act("server/gc", {}).then(load);
        return { refresh: load };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Bảo mật ================= */
    Tabs.add({
      id: "security", icon: "shield", group: "Hệ thống", label: "Bảo mật", poll: 30000,
      html: `<div class="ph">
    <div>
      <h1>Bảo mật</h1>
      <p class="sub">Nhiều tài khoản cùng IP, kết nối theo IP, tài khoản bị khoá đăng nhập do nhập sai mật khẩu.</p>
    </div>
  </div>
  <div class="system-grid">
    <div class="card">
      <div class="ph" style="margin-bottom:8px">
        <div>
          <h2>Đang online cùng IP</h2>
          <p class="hint" id="secOnSum" style="margin:2px 0 0">&nbsp;</p>
        </div>
      </div>
      <div class="list" id="secOn" style="margin-top:10px"></div>
    </div>
    <div class="card">
      <div class="ph" style="margin-bottom:8px">
        <div>
          <h2>Khoá đăng nhập do sai mật khẩu</h2>
          <p class="hint" style="margin:2px 0 0">Tự hết sau thời gian khoá; có thể gỡ sớm thủ công.</p>
        </div>
      </div>
      <div class="list" id="secLock" style="margin-top:10px"></div>
      <div style="margin-top:20px;border-top:1px solid var(--line);padding-top:14px">
        <div class="ph" style="margin-bottom:8px">
          <div>
            <h2 style="font-size:14px">Kết nối theo IP</h2>
            <p class="hint" id="secConnSum" style="margin:2px 0 0"></p>
          </div>
        </div>
        <div class="list" id="secConn" style="margin-top:10px"></div>
      </div>
    </div>
  </div>
  <div class="card">
    <div class="ph" style="margin-bottom:8px">
      <div>
        <h2>Tài khoản dùng chung IP đăng nhập cuối</h2>
        <p class="hint" style="margin:2px 0 0">Gợi ý tài khoản phụ / farm. Bấm để xem các tài khoản theo IP đó.</p>
      </div>
    </div>
    <div class="tw" style="margin-top:10px;max-height:400px">
      <table>
        <thead><tr><th style="width:170px">IP</th><th style="width:100px">Số TK</th><th style="width:110px">Bị khoá</th><th>Tài khoản</th></tr></thead>
        <tbody id="secIp"></tbody>
      </table>
    </div>
  </div>`,
      setup() {
        async function load() {
          const s = await api("security");
          $("secOnSum").textContent = s.onlineIps + " IP đang online · " + s.onlineShared.length + " IP có nhiều nhân vật";
          $("secOn").replaceChildren(...(s.onlineShared.length ? s.onlineShared.map(r => h("div", { class: "li static" }, h("div", { class: "main" }, h("div", { class: "t" }, r.ip),
            h("div", { class: "s", style: "white-space:normal" }, r.players.map((p, i) => [i ? ", " : "", h("a", { href: "#", onclick: e => { e.preventDefault(); openPlayer(p[0]); } }, p[1] + " (" + p[2] + ")")]))), chip(r.players.length + " nhân vật", "warn")))
            : [h("p", { class: "mut" }, "Không có IP nào đăng nhập nhiều nhân vật.")]));
          $("secLock").replaceChildren(...(s.locked.length ? s.locked.map(l => h("div", { class: "li static" }, h("div", { class: "main" }, h("div", { class: "t" }, l[0]), h("div", { class: "s" }, "còn " + dur(l[1]))),
            h("button", { class: "btn small", onclick: () => act("security/unlock", { username: l[0] }).then(load) }, "Gỡ khoá")))
            : [h("p", { class: "mut" }, "Không có tài khoản nào bị khoá đăng nhập.")]));
          $("secConnSum").textContent = "Giới hạn " + (s.maxConnectionsPerIp > 0 ? s.maxConnectionsPerIp + " kết nối / IP" : "không giới hạn") + " · chỉ hiện IP có từ 2 kết nối";
          $("secConn").replaceChildren(...(s.connections.length ? s.connections.map(c => h("div", { class: "li static" }, h("div", { class: "main" }, h("div", { class: "t" }, c[0])), chip(c[1] + " kết nối", c[1] >= s.maxConnectionsPerIp && s.maxConnectionsPerIp > 0 ? "bad" : "")))
            : [h("p", { class: "mut" }, "Không có.")]));
          fill($("secIp"), s.sharedLastIp.map(r => h("tr", { class: "click", onclick: () => { Tabs.go("accounts"); setTimeout(() => { $("aQ").value = r[0]; $("aSearch").click(); }, 50); } },
            h("td", {}, h("code", {}, r[0])), h("td", {}, h("b", {}, r[1])), h("td", {}, r[2] ? chip(r[2], "bad") : "0"), h("td", { class: "mut", style: "white-space:normal" }, r[3]))), "Không có IP nào dùng chung");
        }
        return { refresh: load };
      }
    });

/* ------------------------------------------------------------------ */

/* ================= Nhật ký ================= */
    Tabs.add({
      id: "logs", icon: "logs", group: "Hệ thống", label: "Nhật ký & dữ liệu", poll: 8000,
      html: `<div class="sec-head">
    <div>
      <h1>Nhật ký &amp; dữ liệu máy chủ</h1>
      <p class="sub">Nạp nóng dữ liệu cấu hình CSDL và giám sát hoạt động hệ thống gần nhất.</p>
    </div>
    <div class="dash-head-actions">
      <button class="btn primary dash-refresh-btn" id="reloadBtn" title="Nạp tức thì cấu hình shop, giftcode, nhiệm vụ, boss mà không cần khởi động lại server">
        <svg class="refresh-icon" viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" fill="none" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 11-.57-8.38l5.67-5.67"/></svg>
        <span>Nạp lại cấu hình</span>
      </button>
    </div>
  </div>
  <div class="card">
    <div class="ph" style="margin-bottom:8px">
      <div>
        <h2>Sự kiện nhật ký máy chủ</h2>
        <p class="hint" style="margin:2px 0 0">Lưu đệm 500 dòng gần nhất trong bộ nhớ, tự động cập nhật khi mở màn hình.</p>
      </div>
      <span class="log-badge-info" id="logCounter">0 dòng</span>
    </div>
    <div class="log-toolbar">
      <select id="logLevel" style="min-width:130px;font-weight:600">
        <option value="">Tất cả mức độ</option>
        <option value="INFO">🟢 INFO</option>
        <option value="WARN">🟡 WARN</option>
        <option value="ERROR">🔴 ERROR</option>
      </select>
      <input id="logFilter" class="grow" type="search" placeholder="🔍 Lọc nhật ký theo từ khoá, IP hoặc lớp Java...">
      <label class="mut" style="display:flex;align-items:center;gap:6px;cursor:pointer;white-space:nowrap;font-size:12.5px"><input type="checkbox" id="logAdmin"> Chỉ thao tác Admin</label>
      <label class="mut" style="display:flex;align-items:center;gap:6px;cursor:pointer;white-space:nowrap;font-size:12.5px"><input type="checkbox" id="logFollow" checked> Tự cuộn xuống</label>
      <button type="button" class="btn small" id="logClearView" title="Xóa màn hình xem log hiện tại">Xoá hiển thị</button>
    </div>
    <div class="log-terminal" id="logBox"></div>
  </div>`,
      setup(root) {
        let logSeq = 0, logLines = [];
        async function loadLogs() {
          const list = await api("logs?after=" + logSeq);
          if (!list.length) return;
          logSeq = list[list.length - 1].seq; logLines.push(...list); if (logLines.length > 500) logLines = logLines.slice(-500);
          renderLogs();
        }
        function renderLogs() {
          const lv = $("logLevel").value, f = $("logFilter").value.trim().toLowerCase(), adm = $("logAdmin").checked, box = $("logBox");
          const filtered = logLines.filter(l => (!lv || l.level === lv) && (!adm || l.message.startsWith("Admin panel")) && (!f || l.message.toLowerCase().includes(f) || l.source.toLowerCase().includes(f)));
          $("logCounter").textContent = "Hiển thị " + filtered.length + " / " + logLines.length + " dòng";
          box.replaceChildren(...(filtered.length ? filtered.map(l => h("div", { class: l.level }, l.time + "  " + l.level.padEnd(5) + "  " + l.source + "  " + l.message)) : [h("div", { class: "mut", style: "padding:12px;text-align:center" }, "Không có nhật ký phù hợp với bộ lọc.")]));
          if ($("logFollow").checked) box.scrollTop = box.scrollHeight;
        }
        let filterTimer = null;
        $("logLevel").onchange = renderLogs;
        $("logFilter").oninput = () => { clearTimeout(filterTimer); filterTimer = setTimeout(renderLogs, 140); };
        $("logAdmin").onchange = renderLogs;
        $("logClearView").onclick = () => { logLines = []; renderLogs(); };
        const reloadBtn = $("reloadBtn");
        if (reloadBtn) {
          reloadBtn.onclick = async () => {
            reloadBtn.classList.add("busy");
            try {
              const res = await act("reload", {});
              if (res) loadLogs();
            } finally {
              reloadBtn.classList.remove("busy");
            }
          };
        }
        return { refresh: loadLogs };
      }
    });

/* ------------------------------------------------------------------ */

/* Khởi động: dựng tab, kiểm tra đăng nhập, cập nhật trạng thái định kỳ */
    (async () => {
      Tabs.mount();
      try { const r = await api("login", {}); if (r.passwordRequired && !token) { showLogin(); return; } if (!r.passwordRequired) token = ""; }
      catch (e) { if (!token) { showLogin(); return; } }
      Tabs.show(Tabs.start());
    })();
    setInterval(() => { if (document.visibilityState === "visible" && !loginOpen()) Admin.refreshStatus().catch(Admin.fail); }, 15000);
    document.addEventListener("visibilitychange", () => {
      if (document.visibilityState === "visible" && !loginOpen()) {
        Admin.refreshStatus().catch(Admin.fail);
        Tabs.refresh();
      }
    });
