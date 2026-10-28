package com.tlnt.server.admin;

import com.tlnt.server.account.AccountService;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.session.SessionManager;
import com.tlnt.server.util.Log;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tắt server AN TOÀN, không giới hạn thời gian (gõ {@code stop} ở console — {@link ConsoleCommands} — hoặc Ctrl+C):
 * <ol>
 *   <li>Bật bảo trì: chặn đăng nhập mới.</li>
 *   <li>Đá người chơi DẦN DẦN, {@code kickPerSecond} người / giây: mỗi người được báo, đăng xuất trên hàng đợi của
 *       chính họ (huỷ giao dịch... rồi chụp dữ liệu) và xếp lượt lưu -> luồng ghi gom lô, DB không bị dồn 1 lúc.</li>
 *   <li>Chờ mọi người đăng xuất xong, rồi chờ luồng ghi lưu HẾT (in tiến trình, không bỏ ngang).</li>
 *   <li>Việc còn lại ({@code finish}: dừng mạng, pool, DB) do GameServer truyền vào.</li>
 * </ol>
 * Gọi nhiều lần (vd {@code stop} rồi Ctrl+C) chỉ chạy 1 lần; lần sau chờ lần đầu xong.
 */
public final class ShutdownService {

   private final AccountService accounts;
   private final PlayerService players;
   private final SessionManager sessions;
   private final int kickPerSecond;
   private final Runnable finish;
   private final AtomicBoolean started = new AtomicBoolean();
   private final CountDownLatch done = new CountDownLatch(1);

   public ShutdownService(AccountService accounts, PlayerService players, SessionManager sessions, int kickPerSecond,
         Runnable finish) {
      this.accounts = accounts;
      this.players = players;
      this.sessions = sessions;
      this.kickPerSecond = Math.max(1, kickPerSecond);
      this.finish = finish;
   }

   /** Chạy toàn bộ quy trình trên luồng gọi (chặn tới khi xong). */
   public void shutdown() {
      if (!started.compareAndSet(false, true)) {
         awaitDone();
         return;
      }
      try {
         run();
      } catch (Exception e) {
         Log.error("Loi khi tat server", e);
      } finally {
         done.countDown();
      }
   }

   public boolean isShuttingDown() {
      return started.get();
   }

   private void run() {
      long startedAt = System.currentTimeMillis();
      accounts.setMaintenance(true);
      List<PlayerSession> all = sessions.snapshot();
      int online = accounts.onlineCount();
      Log.info("TAT SERVER: chan dang nhap moi, dang xuat %d nguoi (%d / giay)...", online, kickPerSecond);

      int kicked = 0;
      for (int i = 0; i < all.size(); i += kickPerSecond) {
         List<PlayerSession> wave = all.subList(i, Math.min(all.size(), i + kickPerSecond));
         for (PlayerSession session : wave) {
            accounts.disconnectForMaintenance(session);
         }
         kicked += wave.size();
         Log.info("Tat server: da dang xuat %d / %d ket noi, cho luu %d luot", kicked, all.size(),
               players.pendingWrites());
         if (kicked < all.size()) {
            sleep(1_000);
         }
      }

      // Đăng xuất chạy trên pool game (hàng đợi từng người) -> chờ mọi người rời hẳn danh sách online.
      // Có giới hạn (phòng 1 lượt đăng xuất bị kẹt): ai còn sót thì players.shutdown() trong finish vẫn lưu.
      long lastLog = 0;
      long giveUpAt = System.currentTimeMillis() + 120_000;
      while (accounts.onlineCount() > 0 && System.currentTimeMillis() < giveUpAt) {
         long now = System.currentTimeMillis();
         if (now - lastLog >= 2_000) {
            Log.info("Tat server: con %d nguoi dang dang xuat...", accounts.onlineCount());
            lastLog = now;
         }
         sleep(100);
      }
      finish.run(); // dừng mạng / pool + players.shutdown() chờ ghi HẾT + đóng DB
      Log.info("DA TAT SERVER sau %d giay, toan bo du lieu nhan vat da luu.",
            (System.currentTimeMillis() - startedAt) / 1000);
      Log.flush(3_000);
   }

   private void awaitDone() {
      try {
         done.await();
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   private static void sleep(long ms) {
      try {
         Thread.sleep(ms);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }
}
