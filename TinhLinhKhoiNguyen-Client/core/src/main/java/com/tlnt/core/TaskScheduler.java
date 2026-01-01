package com.tlnt.core;

import java.util.concurrent.TimeUnit;

public final class TaskScheduler {
   public static void schedule(Runnable task, long delay, TimeUnit unit) {
      if (task == null) return;
      new Thread(new Runnable() {
         @Override
         public void run() {
            if (delay > 0) {
               try {
                  Thread.sleep(unit.toMillis(delay));
               } catch (Throwable ignored) {
               }
            }
            try {
               task.run();
            } catch (Throwable t) {
               t.printStackTrace();
            }
         }
      }, "TaskScheduler").start();
   }

   public static void shutdownNow() {
   }
}
