package com.tlnt.server.player;

import com.tlnt.server.db.SqlWork;
import com.tlnt.server.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Luồng ghi DB nhân vật DUY NHẤT ("db-writer") — thay executor 1 luồng cũ, giữ nguyên thứ tự ghi nhưng nhanh hơn nhiều
 * khi đông người thoát cùng lúc (tắt server, sập mạng):
 * <ul>
 *   <li>Việc xếp hàng theo đúng thứ tự gọi. Các lượt LƯU THƯỜNG liền nhau được gom tối đa {@link #BATCH_SIZE} người
 *       vào 1 lô JDBC trong 1 transaction ({@code rewriteBatchedStatements}) -> 5.000 người ~ 25 lô thay vì 5.000
 *       lượt đi về SQL. Việc có transaction riêng (thư, kí gửi: {@link #transaction}) chạy đơn lẻ, làm "vách ngăn"
 *       nên không bao giờ bị đảo thứ tự với lượt lưu trước / sau.</li>
 *   <li>Lô lỗi -> lưu từng người; mỗi người thử lại {@link #RETRIES} lần (chờ 1 / 2 / 4 s — MySQL mất kết nối chốc
 *       lát vẫn qua). Vẫn lỗi -> ghi câu UPDATE ra {@code failed_saves/*.sql} để admin chạy tay khôi phục, KHÔNG mất
 *       tiến trình người chơi.</li>
 * </ul>
 */
final class SaveWriter {

   /** Số người tối đa 1 lô (1 transaction). */
   static final int BATCH_SIZE = 200;
   private static final int RETRIES = 3;
   private static final Path FAILED_DIR = Paths.get("failed_saves");
   private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

   private sealed interface Job permits Save, Tx {
   }

   private record Save(PlayerRepository.Snapshot snapshot, boolean logout, CompletableFuture<Void> done) implements Job {
   }

   private record Tx(PlayerRepository.Snapshot snapshot, SqlWork work, CompletableFuture<Boolean> done) implements Job {
   }

   private final PlayerRepository repository;
   private final LinkedBlockingQueue<Job> queue = new LinkedBlockingQueue<>();
   /** Việc đã nhận mà chưa ghi xong (cả việc đang chạy) — để đếm tiến trình lúc tắt server. */
   private final AtomicInteger pending = new AtomicInteger();
   private final Thread thread;
   private volatile boolean stopping;

   SaveWriter(PlayerRepository repository) {
      this.repository = repository;
      thread = new Thread(this::run, "db-writer");
      thread.setDaemon(false); // JVM không được thoát khi còn dữ liệu chưa ghi
      thread.start();
   }

   /** Lưu nhân vật (gom lô). Future luôn hoàn thành bình thường (lỗi đã xử lý bên trong). */
   CompletableFuture<Void> save(PlayerRepository.Snapshot snapshot, boolean logout) {
      CompletableFuture<Void> done = new CompletableFuture<>();
      submit(new Save(snapshot, logout, done));
      return done;
   }

   /** Nhân vật ({@code snapshot}, null = không) + {@code work} trong 1 transaction. Future = true nếu đã commit. */
   CompletableFuture<Boolean> transaction(PlayerRepository.Snapshot snapshot, SqlWork work) {
      CompletableFuture<Boolean> done = new CompletableFuture<>();
      submit(new Tx(snapshot, work, done));
      return done;
   }

   int pending() {
      return pending.get();
   }

   /** Chờ ghi hết mọi việc đã xếp (không giới hạn thời gian), in tiến trình mỗi vài giây, rồi dừng luồng. */
   void drainAndStop() {
      long lastLog = 0;
      int start = pending.get();
      while (pending.get() > 0) {
         long now = System.currentTimeMillis();
         if (now - lastLog >= 2_000) {
            Log.info("Dang luu du lieu nhan vat: con %d / %d luot", pending.get(), Math.max(start, pending.get()));
            lastLog = now;
         }
         try {
            Thread.sleep(50);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
         }
      }
      stopping = true;
      thread.interrupt();
      try {
         thread.join(10_000);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   private void submit(Job job) {
      pending.incrementAndGet();
      queue.add(job);
   }

   private void run() {
      List<Save> batch = new ArrayList<>(BATCH_SIZE);
      while (!stopping || !queue.isEmpty()) {
         Job job;
         try {
            job = queue.poll(1, TimeUnit.SECONDS);
         } catch (InterruptedException e) {
            continue; // bị đánh thức lúc dừng: vòng while tự kết thúc khi hàng đợi rỗng
         }
         if (job == null) {
            continue;
         }
         if (job instanceof Tx tx) {
            runTx(tx);
            continue;
         }
         batch.add((Save) job);
         // Gom các lượt lưu thường LIỀN NHAU; gặp Tx thì dừng (Tx chạy ở vòng sau -> đúng thứ tự).
         while (batch.size() < BATCH_SIZE && queue.peek() instanceof Save) {
            batch.add((Save) queue.poll());
         }
         runBatch(batch);
         batch.clear();
      }
   }

   private void runTx(Tx tx) {
      boolean ok;
      try {
         repository.saveInTransaction(tx.snapshot(), tx.work());
         ok = true;
      } catch (SQLException | RuntimeException e) {
         Log.error("GHI DB THAT BAI (da rollback)" + (tx.snapshot() == null ? ""
               : " nhan vat #" + tx.snapshot().playerId() + " '" + tx.snapshot().name() + "'"), e);
         ok = false;
      }
      pending.decrementAndGet();
      tx.done().complete(ok);
   }

   private void runBatch(List<Save> batch) {
      try {
         List<PlayerRepository.Snapshot> snapshots = new ArrayList<>(batch.size());
         boolean[] logout = new boolean[batch.size()];
         for (int i = 0; i < batch.size(); i++) {
            snapshots.add(batch.get(i).snapshot());
            logout[i] = batch.get(i).logout();
         }
         repository.saveBatch(snapshots, logout);
      } catch (SQLException | RuntimeException e) {
         Log.warn("Luu lo %d nhan vat loi (%s) -> luu tung nguoi", batch.size(), e.getMessage());
         for (Save save : batch) {
            saveOneWithRetry(save);
         }
      }
      for (Save save : batch) {
         pending.decrementAndGet();
         save.done().complete(null);
      }
   }

   private void saveOneWithRetry(Save save) {
      PlayerRepository.Snapshot s = save.snapshot();
      for (int attempt = 1; attempt <= RETRIES; attempt++) {
         try {
            repository.save(s, save.logout());
            return;
         } catch (SQLException | RuntimeException e) {
            Log.warn("Luu nhan vat #%d '%s' loi lan %d/%d: %s", s.playerId(), Log.ascii(s.name()), attempt, RETRIES,
                  e.getMessage());
            if (attempt < RETRIES) {
               try {
                  Thread.sleep(1_000L << (attempt - 1)); // 1 s, 2 s
               } catch (InterruptedException ie) {
                  Thread.currentThread().interrupt();
                  break;
               }
            }
         }
      }
      dumpToFile(s, save.logout());
   }

   /** Ghi câu UPDATE ra file để admin chạy tay khi DB hoạt động lại. */
   private static void dumpToFile(PlayerRepository.Snapshot s, boolean logout) {
      try {
         Files.createDirectories(FAILED_DIR);
         Path file = FAILED_DIR.resolve("player_" + s.playerId() + "_" + LocalDateTime.now().format(FILE_TIME) + ".sql");
         String sql = "-- Luu that bai nhan vat #" + s.playerId() + " '" + Log.ascii(s.name()) + "' luc "
               + LocalDateTime.now() + System.lineSeparator() + PlayerRepository.updateSql(s, logout) + ";"
               + System.lineSeparator();
         Files.writeString(file, sql, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
               StandardOpenOption.TRUNCATE_EXISTING);
         Log.error("LUU NHAN VAT THAT BAI #%d '%s' -> da ghi %s (chay file nay vao tlnt_server de khoi phuc)",
               s.playerId(), Log.ascii(s.name()), file.toAbsolutePath());
      } catch (IOException e) {
         Log.error("LUU NHAN VAT THAT BAI #" + s.playerId() + " va KHONG ghi duoc file du phong", e);
      }
   }
}
