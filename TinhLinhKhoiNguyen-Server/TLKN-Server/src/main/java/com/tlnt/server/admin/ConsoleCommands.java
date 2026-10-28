package com.tlnt.server.admin;

import com.tlnt.server.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Lệnh gõ ở console server (cửa sổ cmd, hoặc ô nhập của Output NetBeans):
 * <ul>
 *   <li>{@code stop} / {@code shutdown}: tắt server an toàn ({@link ShutdownService}) rồi thoát tiến trình. NÊN DÙNG
 *       thay cho nút Stop của NetBeans / đóng cửa sổ (2 cách đó giết tiến trình ngay, không kịp lưu).</li>
 *   <li>{@code online}: in thống kê kết nối / người chơi / lượt ghi DB đang chờ.</li>
 *   <li>{@code help}: danh sách lệnh.</li>
 * </ul>
 * Luồng daemon; không có stdin (chạy nền) thì tự kết thúc.
 */
public final class ConsoleCommands {

   private final ShutdownService shutdown;
   private final Supplier<String> stats;

   public ConsoleCommands(ShutdownService shutdown, Supplier<String> stats) {
      this.shutdown = shutdown;
      this.stats = stats;
   }

   public void start() {
      Thread t = new Thread(this::loop, "console");
      t.setDaemon(true);
      t.start();
   }

   private void loop() {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
      while (true) {
         String line;
         try {
            line = in.readLine();
         } catch (IOException e) {
            return;
         }
         if (line == null) {
            return; // không có console
         }
         String cmd = line.trim().toLowerCase(Locale.ROOT);
         switch (cmd) {
            case "" -> {
            }
            case "stop", "shutdown", "exit" -> {
               if (shutdown.isShuttingDown()) {
                  Log.info("Server dang tat, vui long cho...");
                  continue;
               }
               shutdown.shutdown();
               System.exit(0);
            }
            case "online", "stats" -> Log.info("%s", stats.get());
            case "help", "?" -> Log.info("Lenh: stop (tat server an toan, luu het du lieu) | online (thong ke)"
                  + " | event [trungthu on|off] (bat / tat su kien) | help");
            default -> {
               if (cmd.equals("event") || cmd.startsWith("event ")) {
                  event(cmd.split(" +"));
               } else {
                  Log.info("Khong co lenh '%s' - go help", cmd);
               }
            }
         }
      }
   }

   /** {@code event} = trạng thái; {@code event trungthu on|off} = bật / tắt (SQL ngay trên luồng console). */
   private static void event(String[] args) {
      if (args.length < 3) {
         for (com.tlnt.server.event.GameEvents.Event e : com.tlnt.server.event.GameEvents.Event.values()) {
            Log.info("Su kien %s: %s", e.key, com.tlnt.server.event.GameEvents.active(e) ? "BAT" : "tat");
         }
         Log.info("Dung: event <ma> on|off");
         return;
      }
      com.tlnt.server.event.GameEvents.Event e = com.tlnt.server.event.GameEvents.Event.of(args[1]);
      if (e == null) {
         Log.info("Khong co su kien '%s'", args[1]);
         return;
      }
      try {
         boolean on = args[2].equals("on") || args[2].equals("bat") || args[2].equals("1");
         com.tlnt.server.event.GameEvents.set(e, on);
      } catch (java.sql.SQLException ex) {
         Log.warn("Khong luu duoc trang thai su kien: %s", ex.getMessage());
      }
   }
}
