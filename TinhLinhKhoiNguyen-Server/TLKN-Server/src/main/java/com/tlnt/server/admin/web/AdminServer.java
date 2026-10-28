package com.tlnt.server.admin.web;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tlnt.server.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Admin panel web: HTTP server có sẵn của JDK ({@code com.sun.net.httpserver}, không thêm thư viện), CHỈ nghe
 * 127.0.0.1 (người dùng chốt: mở trình duyệt trên chính máy chủ, http://127.0.0.1:{@code admin.port}). Trang
 * {@code resources/admin/index.html} + API JSON {@code /api/...} ({@link AdminApi}).
 *
 * <p>Bảo vệ: từ chối kết nối không phải loopback; Host phải là localhost / 127.0.0.1 (chống DNS rebinding); mọi
 * {@code /api} phải có header {@code X-TLNT-Admin} (trang web lạ không gửi được header tự đặt sang đây — không trả CORS)
 * -> chống CSRF; có {@code admin.password} thì phải đăng nhập lấy mã phiên (12 giờ), sai 5 lần khoá 1 phút.
 *
 * <p>Luồng: 2 luồng riêng "admin" (được chạy SQL như lane BLOCKING — không bao giờ chạy trên luồng game / tick).
 *
 * <p>Ảnh client (icon item, khung giao diện, font Saira) ở {@code /asset/icon/N.png}, {@code /asset/ui/N.png},
 * {@code /asset/Saira-Bold.ttf}: đọc từ {@code admin.assetsDir}, CHỈ tên file là số (không đi ra ngoài thư mục), không
 * cần header / mã phiên (thẻ img không gửi được) — chỉ là hình trong game. Trình duyệt lưu đệm 1 ngày.
 */
public final class AdminServer {

   static final String HEADER = "X-TLNT-Admin";
   private static final int MAX_BODY_BYTES = 1 << 20;
   private static final long SESSION_MS = 12 * 3_600_000L;
   private static final int MAX_FAILS = 5;
   private static final long FAIL_LOCK_MS = 60_000;
   private static final Set<String> HOSTS = Set.of("127.0.0.1", "localhost", "[::1]");
   private static final java.util.regex.Pattern ASSET = java.util.regex.Pattern.compile("/asset/(icon|ui)/([0-9]{1,5})[.]png");
   private static final String FONT = "/asset/Saira-Bold.ttf";

   private final int port;
   private final String password;
   private final AdminApi api;
   private final java.nio.file.Path assets;
   private final Gson gson = new Gson();
   private final SecureRandom random = new SecureRandom();
   /** Mã phiên -> hết hạn lúc (ms). */
   private final Map<String, Long> sessions = new ConcurrentHashMap<>();
   private int fails;
   private long lockedUntil;
   private HttpServer server;
   private ExecutorService pool;

   public AdminServer(int port, String password, AdminApi api, java.nio.file.Path assetsDir) {
      this.port = port;
      this.password = password == null ? "" : password;
      this.api = api;
      java.nio.file.Path dir = assetsDir;
      if (dir != null && !java.nio.file.Files.isDirectory(dir.resolve("icon"))) {
         java.nio.file.Path alt = java.nio.file.Path.of("TLKN-Server").resolve(dir); // chạy từ thư mục gốc repo
         dir = java.nio.file.Files.isDirectory(alt.resolve("icon")) ? alt : null;
         if (dir == null) {
            Log.warn("Admin panel: khong thay thu muc anh '%s' (admin.assetsDir) -> trang hien chu thay icon", assetsDir);
         }
      }
      this.assets = dir;
   }

   /** Trang hỏi có ảnh không (không có thì hiện chữ). */
   boolean hasAssets() {
      return assets != null;
   }

   public void start() throws IOException {
      server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 32);
      pool = Executors.newFixedThreadPool(2, r -> {
         Thread t = new Thread(r, "admin");
         t.setDaemon(true);
         return t;
      });
      server.setExecutor(pool);
      server.createContext("/", this::handle);
      server.start();
   }

   public void stop() {
      if (server != null) {
         server.stop(0);
         pool.shutdownNow();
      }
   }

   // ================================================================== 1 yêu cầu

   private void handle(HttpExchange ex) throws IOException {
      try {
         if (!ex.getRemoteAddress().getAddress().isLoopbackAddress() || !hostOk(ex)) {
            send(ex, 403, "text/plain", "Forbidden".getBytes(StandardCharsets.UTF_8));
            return;
         }
         String path = ex.getRequestURI().getPath();
         if (path.equals("/") || path.equals("/index.html") || path.equals("/admin.css") || path.equals("/admin.js")) {
            serveResource(ex, path);
            return;
         }
         if (path.startsWith("/asset/")) {
            asset(ex, path);
            return;
         }
         if (!path.startsWith("/api/")) {
            send(ex, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
         }
         if (ex.getRequestHeaders().getFirst(HEADER) == null) { // chống CSRF: trang web khác không đặt được header này
            json(ex, 403, error("Thiếu header " + HEADER));
            return;
         }
         String route = path.substring("/api/".length());
         JsonObject body = "POST".equals(ex.getRequestMethod()) ? body(ex) : new JsonObject();
         Map<String, String> query = query(ex.getRequestURI().getRawQuery());
         if (route.equals("login")) {
            json(ex, 200, login(body));
            return;
         }
         if (!authorized(ex)) {
            json(ex, 401, error("Cần đăng nhập"));
            return;
         }
         Object result = api.handle(ex.getRequestMethod(), route, query, body);
         json(ex, result == null ? 404 : 200, result == null ? error("Không có API " + route) : result);
      } catch (AdminApi.BadRequest e) {
         json(ex, 400, error(e.getMessage()));
      } catch (Exception e) {
         Log.error("Loi admin panel " + ex.getRequestURI(), e);
         json(ex, 500, error("Lỗi máy chủ: " + e.getMessage()));
      } finally {
         ex.close();
      }
   }

   private static boolean hostOk(HttpExchange ex) {
      String host = ex.getRequestHeaders().getFirst("Host");
      if (host == null) {
         return false;
      }
      host = host.toLowerCase(Locale.ROOT);
      int colon = host.lastIndexOf(':');
      if (colon > 0 && !host.endsWith("]")) {
         host = host.substring(0, colon);
      }
      return HOSTS.contains(host);
   }

   /** File giao diện đã nạp: bản gốc + bản gzip + ETag (đọc jar + nén 1 lần / tiến trình). */
   private record StaticFile(String type, byte[] raw, byte[] gzip, String etag) {
   }

   private final Map<String, StaticFile> statics = new ConcurrentHashMap<>();

   private static StaticFile loadStatic(String file) throws IOException {
      try (InputStream in = AdminServer.class.getResourceAsStream("/admin" + file)) {
         if (in == null) {
            return null;
         }
         byte[] raw = in.readAllBytes();
         java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream(raw.length / 4);
         try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(buf)) {
            gz.write(raw);
         }
         String type = file.endsWith(".html") ? "text/html; charset=utf-8"
               : file.endsWith(".css") ? "text/css; charset=utf-8"
               : file.endsWith(".js") ? "application/javascript; charset=utf-8" : "text/plain";
         return new StaticFile(type, raw, buf.toByteArray(), "\"" + Integer.toHexString(java.util.Arrays.hashCode(raw)) + "\"");
      }
   }

   /**
    * index.html / admin.css / admin.js: nén gzip (~370 KB -> ~70 KB), ETag + "no-cache" -> mở lại trang chỉ hỏi 304,
    * không tải lại.
    */
   private void serveResource(HttpExchange ex, String path) throws IOException {
      String file = path.equals("/") ? "/index.html" : path;
      StaticFile f = statics.get(file);
      if (f == null) {
         f = loadStatic(file);
         if (f == null) {
            send(ex, 404, "text/plain", ("Thieu resources/admin" + file).getBytes(StandardCharsets.UTF_8));
            return;
         }
         statics.put(file, f);
      }
      var h = ex.getResponseHeaders();
      if (file.endsWith(".html")) {
         h.set("Content-Security-Policy",
               "default-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data:");
      }
      h.set("Content-Type", f.type());
      h.set("Cache-Control", "no-cache");
      h.set("ETag", f.etag());
      h.set("X-Content-Type-Options", "nosniff");
      if (f.etag().equals(ex.getRequestHeaders().getFirst("If-None-Match"))) {
         ex.sendResponseHeaders(304, -1);
         return;
      }
      String accept = ex.getRequestHeaders().getFirst("Accept-Encoding");
      byte[] body = f.raw();
      if (accept != null && accept.contains("gzip")) {
         h.set("Content-Encoding", "gzip");
         h.set("Vary", "Accept-Encoding");
         body = f.gzip();
      }
      ex.sendResponseHeaders(200, body.length);
      try (OutputStream out = ex.getResponseBody()) {
         out.write(body);
      }
   }

   private void asset(HttpExchange ex, String path) throws IOException {
      java.nio.file.Path file = null;
      String type = "image/png";
      java.util.regex.Matcher m = ASSET.matcher(path);
      if (assets != null && m.matches()) {
         file = assets.resolve(m.group(1)).resolve(m.group(2) + ".png");
      } else if (assets != null && path.equals(FONT)) {
         file = assets.resolve("Saira-Bold.ttf");
         type = "font/ttf";
      }
      if (file == null || !java.nio.file.Files.isRegularFile(file)) {
         send(ex, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
         return;
      }
      byte[] data = java.nio.file.Files.readAllBytes(file);
      ex.getResponseHeaders().set("Content-Type", type);
      ex.getResponseHeaders().set("Cache-Control", "private, max-age=86400");
      ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
      ex.sendResponseHeaders(200, data.length);
      try (OutputStream out = ex.getResponseBody()) {
         out.write(data);
      }
   }

   // ================================================================== đăng nhập (chỉ khi có admin.password)

   private synchronized Object login(JsonObject body) {
      Map<String, Object> out = new HashMap<>();
      if (password.isEmpty()) {
         out.put("ok", true);
         out.put("token", "");
         out.put("passwordRequired", false);
         return out;
      }
      long now = System.currentTimeMillis();
      if (now < lockedUntil) {
         return error("Sai mật khẩu quá nhiều lần, thử lại sau " + (lockedUntil - now + 999) / 1000 + " giây.");
      }
      String given = body.has("password") ? body.get("password").getAsString() : "";
      if (given.isEmpty()) { // trang vừa mở hỏi "có cần mật khẩu không": không tính là sai
         out.put("ok", true);
         out.put("token", "");
         out.put("passwordRequired", true);
         return out;
      }
      if (!MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8))) {
         if (++fails >= MAX_FAILS) {
            fails = 0;
            lockedUntil = now + FAIL_LOCK_MS;
         }
         Log.warn("Admin panel: sai mat khau");
         return error("Sai mật khẩu.");
      }
      fails = 0;
      byte[] raw = new byte[24];
      random.nextBytes(raw);
      String token = HexFormat.of().formatHex(raw);
      sessions.put(token, now + SESSION_MS);
      sessions.values().removeIf(until -> until < now);
      out.put("ok", true);
      out.put("token", token);
      out.put("passwordRequired", true);
      return out;
   }

   private boolean authorized(HttpExchange ex) {
      if (password.isEmpty()) {
         return true;
      }
      String token = ex.getRequestHeaders().getFirst("X-TLNT-Token");
      Long until = token == null ? null : sessions.get(token);
      return until != null && until > System.currentTimeMillis();
   }

   // ================================================================== tiện ích HTTP

   private static JsonObject body(HttpExchange ex) throws IOException {
      byte[] raw = ex.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
      if (raw.length > MAX_BODY_BYTES) {
         throw new AdminApi.BadRequest("Nội dung gửi lên quá lớn.");
      }
      if (raw.length == 0) {
         return new JsonObject();
      }
      JsonElement el = JsonParser.parseString(new String(raw, StandardCharsets.UTF_8));
      if (!el.isJsonObject()) {
         throw new AdminApi.BadRequest("Nội dung phải là JSON object.");
      }
      return el.getAsJsonObject();
   }

   private static Map<String, String> query(String raw) {
      Map<String, String> map = new HashMap<>();
      if (raw == null || raw.isEmpty()) {
         return map;
      }
      for (String part : raw.split("&")) {
         int eq = part.indexOf('=');
         String k = URLDecoder.decode(eq < 0 ? part : part.substring(0, eq), StandardCharsets.UTF_8);
         String v = eq < 0 ? "" : URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
         map.put(k, v);
      }
      return map;
   }

   private static Map<String, Object> error(String message) {
      Map<String, Object> out = new HashMap<>();
      out.put("ok", false);
      out.put("error", message);
      return out;
   }

   private void json(HttpExchange ex, int status, Object value) throws IOException {
      send(ex, status, "application/json; charset=utf-8", gson.toJson(value).getBytes(StandardCharsets.UTF_8));
   }

   private static void send(HttpExchange ex, int status, String type, byte[] data) throws IOException {
      ex.getResponseHeaders().set("Content-Type", type);
      ex.getResponseHeaders().set("Cache-Control", "no-store");
      ex.getResponseHeaders().set("X-Frame-Options", "DENY");
      ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
      ex.sendResponseHeaders(status, data.length);
      try (OutputStream out = ex.getResponseBody()) {
         out.write(data);
      }
   }
}
