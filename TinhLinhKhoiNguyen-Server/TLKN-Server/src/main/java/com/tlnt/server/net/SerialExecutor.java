package com.tlnt.server.net;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chạy task của MỘT session lần lượt từng cái một, mượn thread của pool chung. Nhờ vậy 2 gói của cùng 1 người
 * không bao giờ chạy song song (handler không cần khoá), còn gói của những người khác nhau chạy song song.
 *
 * Mỗi task tự chọn pool ({@code game} cho việc nhẹ, {@code blocking} cho SQL / băm mật khẩu): thứ tự trong 1
 * session vẫn giữ nguyên, nhưng 1 lượt đăng nhập chậm chỉ chiếm luồng blocking, không làm nghẽn luồng game của
 * người khác.
 */
public final class SerialExecutor {

   /** Số task tối đa chạy liền trong 1 lượt, rồi nhả thread cho session khác. */
   private static final int BATCH = 32;

   private record Task(Runnable runnable, Executor pool) {
   }

   private final Queue<Task> tasks = new ConcurrentLinkedQueue<>();
   private final AtomicInteger pending = new AtomicInteger();
   private final AtomicBoolean running = new AtomicBoolean();

   public void execute(Runnable task, Executor pool) {
      tasks.add(new Task(task, pool));
      pending.incrementAndGet();
      schedule();
   }

   /** Số task đang chờ — dùng để phát hiện client spam gói. */
   public int pending() {
      return pending.get();
   }

   private void schedule() {
      while (running.compareAndSet(false, true)) {
         Task head = tasks.peek();
         if (head == null) {
            running.set(false);
            if (tasks.isEmpty()) {
               return;
            }
            continue; // vừa có task mới chen vào sau khi peek
         }
         Executor pool = head.pool();
         try {
            pool.execute(() -> drain(pool));
            return;
         } catch (RejectedExecutionException shuttingDown) {
            tasks.clear();
            pending.set(0);
            running.set(false);
            return;
         }
      }
   }

   private void drain(Executor pool) {
      try {
         for (int i = 0; i < BATCH; i++) {
            Task task = tasks.peek();
            if (task == null || task.pool() != pool) {
               break; // hết việc, hoặc task kế tiếp thuộc pool khác -> chuyển sang pool đó
            }
            tasks.poll();
            pending.decrementAndGet();
            task.runnable().run();
         }
      } finally {
         running.set(false);
         if (!tasks.isEmpty()) {
            schedule();
         }
      }
   }
}
