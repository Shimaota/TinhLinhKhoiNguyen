package com.tlnt.server.account;

import com.tlnt.server.util.Log;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chống dò mật khẩu + chống làm nghẽn đăng nhập. Mọi kiểm tra chạy TRƯỚC khi băm PBKDF2 (120.000 vòng, việc tốn CPU nhất
 * của pool blocking) nên kẻ tấn công không đốt được CPU server:
 * <ul>
 *   <li>Mỗi IP tối đa {@code maxConnectionsPerIp} kết nối cùng lúc (quá -> đóng ngay khi vừa kết nối).</li>
 *   <li>Mỗi IP tối đa {@code loginsPerIpPerMinute} lần thử đăng nhập / phút, {@code captchasPerIpPerMinute} lần xin
 *       captcha / phút (vẽ PNG cũng tốn CPU). Đếm theo IP nên kết nối lại KHÔNG xoá được bộ đếm.</li>
 *   <li>Mỗi tài khoản sai mật khẩu {@code failsPerAccount} lần trong {@value #FAIL_WINDOW_MS} ms -> khoá đăng nhập tài
 *       khoản đó {@code accountLockMinutes} phút (kể cả đúng mật khẩu), đăng nhập đúng thì xoá bộ đếm.</li>
 * </ul>
 * IP loopback (127.0.0.1 — bot tải / chạy thử trên máy) được miễn nếu {@code exemptLoopback}. Bộ đếm cũ dọn mỗi phút
 * ({@link #cleanup}).
 */
public final class LoginGuard {

   private static final long MINUTE_MS = 60_000;
   /** Cửa sổ đếm số lần sai của 1 tài khoản. */
   static final long FAIL_WINDOW_MS = 15 * MINUTE_MS;

   /** Bộ đếm cửa sổ cố định: {@code count} lần kể từ {@code start}. */
   private static final class Window {
      long start;
      int count;
      long lockedUntil;
   }

   private final int maxConnectionsPerIp;
   private final int loginsPerIpPerMinute;
   private final int captchasPerIpPerMinute;
   private final int failsPerAccount;
   private final long accountLockMs;
   private final boolean exemptLoopback;

   private final Map<String, AtomicInteger> connections = new ConcurrentHashMap<>();
   private final Map<String, Window> loginsByIp = new ConcurrentHashMap<>();
   private final Map<String, Window> captchasByIp = new ConcurrentHashMap<>();
   private final Map<String, Window> failsByAccount = new ConcurrentHashMap<>();

   public LoginGuard(int maxConnectionsPerIp, int loginsPerIpPerMinute, int captchasPerIpPerMinute, int failsPerAccount,
         int accountLockMinutes, boolean exemptLoopback) {
      this.maxConnectionsPerIp = maxConnectionsPerIp;
      this.loginsPerIpPerMinute = loginsPerIpPerMinute;
      this.captchasPerIpPerMinute = captchasPerIpPerMinute;
      this.failsPerAccount = failsPerAccount;
      this.accountLockMs = accountLockMinutes * MINUTE_MS;
      this.exemptLoopback = exemptLoopback;
   }

   // ------------------------------------------------------------------ kết nối

   /** Kết nối mới (luồng mạng). @return false = IP đã đủ số kết nối, đóng kết nối này. */
   public boolean onConnect(String ip) {
      if (exempt(ip) || maxConnectionsPerIp <= 0) {
         return true;
      }
      int now = connections.computeIfAbsent(ip, k -> new AtomicInteger()).incrementAndGet();
      if (now > maxConnectionsPerIp) {
         onDisconnect(ip);
         Log.warn("IP %s vuot %d ket noi cung luc -> tu choi", ip, maxConnectionsPerIp);
         return false;
      }
      return true;
   }

   /** Gọi đúng 1 lần cho mỗi {@link #onConnect} trả về true. */
   public void onDisconnect(String ip) {
      if (exempt(ip) || maxConnectionsPerIp <= 0) {
         return;
      }
      AtomicInteger count = connections.get(ip);
      if (count != null && count.decrementAndGet() <= 0) {
         connections.remove(ip, count);
      }
   }

   // ------------------------------------------------------------------ đăng nhập / captcha

   /** Trước khi kiểm tra mật khẩu. @return thông báo từ chối (có dấu) hoặc null = được thử. */
   public String checkLogin(String ip, String username, long now) {
      if (!exempt(ip) && !tick(loginsByIp, ip, loginsPerIpPerMinute, MINUTE_MS, now)) {
         Log.warn("IP %s dang nhap qua %d lan / phut", ip, loginsPerIpPerMinute);
         return "Bạn thử đăng nhập quá nhiều lần, vui lòng đợi 1 phút.";
      }
      Window w = failsByAccount.get(key(username));
      if (w != null) {
         synchronized (w) {
            if (w.lockedUntil > now) {
               long minutes = (w.lockedUntil - now + MINUTE_MS - 1) / MINUTE_MS;
               return "Tài khoản tạm khoá do nhập sai mật khẩu nhiều lần. Thử lại sau " + minutes + " phút.";
            }
         }
      }
      return null;
   }

   /** Sai mật khẩu (kể cả tài khoản không tồn tại — không lộ tài khoản nào có thật). */
   public void onLoginFailed(String ip, String username, long now) {
      if (failsPerAccount <= 0) {
         return;
      }
      Window w = failsByAccount.computeIfAbsent(key(username), k -> new Window());
      synchronized (w) {
         if (now - w.start > FAIL_WINDOW_MS) {
            w.start = now;
            w.count = 0;
         }
         if (++w.count >= failsPerAccount) {
            w.lockedUntil = now + accountLockMs;
            w.count = 0;
            w.start = now;
            Log.warn("Tai khoan '%s' sai mat khau %d lan (lan cuoi tu %s) -> khoa dang nhap %d phut",
                  Log.ascii(username), failsPerAccount, ip, accountLockMs / MINUTE_MS);
         }
      }
   }

   public void onLoginSucceeded(String username) {
      failsByAccount.remove(key(username));
   }

   /** Xin captcha (đăng ký). @return false = quá nhiều, bỏ qua yêu cầu. */
   public boolean allowCaptcha(String ip, long now) {
      return exempt(ip) || tick(captchasByIp, ip, captchasPerIpPerMinute, MINUTE_MS, now);
   }

   // ------------------------------------------------------------------ admin panel

   /** Tài khoản đang bị tạm khoá đăng nhập vì sai mật khẩu: {tên (chữ thường), còn bao nhiêu giây}. */
   public java.util.List<Object[]> lockedAccounts(long now) {
      java.util.List<Object[]> out = new java.util.ArrayList<>();
      failsByAccount.forEach((name, w) -> {
         synchronized (w) {
            if (w.lockedUntil > now) {
               out.add(new Object[] {name, (w.lockedUntil - now) / 1000});
            }
         }
      });
      return out;
   }

   /** Gỡ khoá đăng nhập do sai mật khẩu. @return true nếu đang bị khoá */
   public boolean unlockAccount(String username) {
      Window w = failsByAccount.remove(key(username));
      return w != null && w.lockedUntil > System.currentTimeMillis();
   }

   /** Số kết nối đang mở theo IP (chỉ IP có ≥ {@code min}). */
   public java.util.Map<String, Integer> connectionsByIp(int min) {
      java.util.Map<String, Integer> out = new java.util.TreeMap<>();
      connections.forEach((ip, n) -> {
         if (n.get() >= min) {
            out.put(ip, n.get());
         }
      });
      return out;
   }

   public int maxConnectionsPerIp() {
      return maxConnectionsPerIp;
   }

   // ------------------------------------------------------------------ dọn dẹp

   /** Mỗi phút (luồng scheduler): bỏ bộ đếm đã hết hạn -> map không phình theo thời gian. */
   public void cleanup(long now) {
      loginsByIp.values().removeIf(w -> now - w.start > MINUTE_MS);
      captchasByIp.values().removeIf(w -> now - w.start > MINUTE_MS);
      failsByAccount.values().removeIf(w -> now - w.start > FAIL_WINDOW_MS && w.lockedUntil <= now);
   }

   // ------------------------------------------------------------------ tiện ích

   /** +1 lần trong cửa sổ {@code windowMs}. @return false nếu vượt {@code limit} ({@code limit <= 0} = không giới hạn). */
   private static boolean tick(Map<String, Window> map, String key, int limit, long windowMs, long now) {
      if (limit <= 0) {
         return true;
      }
      Window w = map.computeIfAbsent(key, k -> new Window());
      synchronized (w) {
         if (now - w.start > windowMs) {
            w.start = now;
            w.count = 0;
         }
         return ++w.count <= limit;
      }
   }

   private boolean exempt(String ip) {
      return exemptLoopback && ip != null && (ip.startsWith("127.") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1"));
   }

   private static String key(String username) {
      return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
   }
}
