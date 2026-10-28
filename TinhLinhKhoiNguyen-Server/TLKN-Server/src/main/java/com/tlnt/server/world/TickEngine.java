package com.tlnt.server.world;

import com.tlnt.server.util.Log;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Chạy tick cho mọi khu trên {@code world.tickThreads} luồng cố định (mặc định = số nhân CPU).
 *
 * Mỗi khu thuộc đúng 1 luồng suốt đời (gán vào luồng đang giữ ít khu nhất) -> mọi trạng thái của khu chỉ 1 luồng
 * đụng tới, không cần khoá. Mỗi vòng: tick lần lượt các khu của luồng, rồi ngủ tới mốc tick kế tiếp. Khu trống
 * (không người, không lệnh) tốn gần như 0.
 *
 * Thay cho {@code scheduleAtFixedRate} mỗi khu 1 task (hàng trăm task trong hàng đợi hẹn giờ, mỗi tick là 1 lần
 * đánh thức luồng).
 */
public final class TickEngine {

   private final long tickNanos;
   private final Worker[] workers;
   private volatile boolean running = true;
   /** Thống kê: tổng thời gian tick (ns) và số vòng, để in định kỳ. */
   private final AtomicLong busyNanos = new AtomicLong();
   private final AtomicLong rounds = new AtomicLong();
   private final AtomicLong overruns = new AtomicLong();

   public TickEngine(int threads, long tickMillis) {
      this.tickNanos = TimeUnit.MILLISECONDS.toNanos(tickMillis);
      enableFineTimerOnWindows();
      this.workers = new Worker[Math.max(1, threads)];
      for (int i = 0; i < workers.length; i++) {
         workers[i] = new Worker(i + 1);
         workers[i].start();
      }
   }

   /**
    * Windows mặc định chỉ đánh thức luồng ngủ theo nấc ~15,6 ms: hẹn tick 18 ms thực tế thành 15-16-16-16-16-31 ms...
    * — cứ ~100 ms hụt 1 nhịp 31 ms, client 2 khung hình không có vị trí mới -> người chơi khác / thú cưng đi khựng
    * đều đặn (đã đo). JVM bật bộ hẹn giờ 1 ms của Windows (timeBeginPeriod) trong lúc có luồng
    * {@code Thread.sleep} với số ms không chia hết cho 10 -> 1 luồng nền ngủ mãi {@code Long.MAX_VALUE} ms là tick
    * đều 17-19 ms. Linux không cần (bộ hẹn giờ vốn chính xác).
    */
   private static void enableFineTimerOnWindows() {
      if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
         return;
      }
      Thread keeper = new Thread(() -> {
         while (true) {
            try {
               Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
               return;
            }
         }
      }, "win-timer-1ms");
      keeper.setDaemon(true);
      keeper.start();
   }

   /** Gắn khu vào luồng đang giữ ít khu nhất. */
   synchronized void add(Zone zone) {
      Worker best = workers[0];
      for (Worker worker : workers) {
         if (worker.zones.size() < best.zones.size()) {
            best = worker;
         }
      }
      best.zones.add(zone);
      zone.worker = best;
   }

   synchronized void remove(Zone zone) {
      Worker worker = zone.worker;
      if (worker != null) {
         worker.zones.remove(zone);
         zone.worker = null;
      }
   }

   public void shutdown() {
      running = false;
      for (Worker worker : workers) {
         LockSupport.unpark(worker);
         try {
            worker.join(2_000);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }
   }

   /** Thời gian tick trung bình (ms) / luồng / vòng kể từ lần gọi trước, số vòng bị trễ; rồi reset. */
   public String takeStats() {
      long r = rounds.getAndSet(0);
      long busy = busyNanos.getAndSet(0);
      long late = overruns.getAndSet(0);
      double avgMs = r == 0 ? 0 : busy / 1e6 / r;
      return String.format("tick tb %.2f ms (%d luong, tick %d ms), tre %d vong", avgMs, workers.length,
            TimeUnit.NANOSECONDS.toMillis(tickNanos), late);
   }

   final class Worker extends Thread {

      final CopyOnWriteArrayList<Zone> zones = new CopyOnWriteArrayList<>();

      Worker(int number) {
         super("zone-tick-" + number);
         setDaemon(true);
      }

      @Override
      public void run() {
         long next = System.nanoTime();
         while (running) {
            long start = System.nanoTime();
            long now = System.currentTimeMillis();
            for (Zone zone : zones) {
               try {
                  zone.tick(now);
               } catch (Throwable t) {
                  Log.error("Loi tick map " + zone.map().template().id() + " khu " + zone.id(), t);
               }
            }
            long end = System.nanoTime();
            busyNanos.addAndGet(end - start);
            rounds.incrementAndGet();

            next += tickNanos;
            long sleep = next - end;
            if (sleep > 0) {
               LockSupport.parkNanos(sleep);
            } else {
               overruns.incrementAndGet();
               if (-sleep > tickNanos * 5) {
                  next = end; // trễ quá nhiều: bỏ qua các tick đã lỡ, không chạy dồn
               }
            }
         }
      }
   }
}
