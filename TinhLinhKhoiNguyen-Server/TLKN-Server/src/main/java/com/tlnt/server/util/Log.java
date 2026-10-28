package com.tlnt.server.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Log BẤT ĐỒNG BỘ, không phụ thuộc thư viện ngoài.
 *
 * <ul>
 *   <li>Luồng gọi chỉ format chuỗi rồi đẩy vào hàng đợi -> không bao giờ chờ console (console Windows rất chậm,
 *       ghi thẳng từ luồng game sẽ làm tụt tick khi đông người).</li>
 *   <li>1 luồng nền ghi ra stdout (UTF-8 tường minh để cmd/PowerShell hiện đúng tiếng Việt), gom nhiều dòng
 *       rồi flush 1 lần.</li>
 *   <li>Hàng đợi đầy (log quá dồn dập) -> bỏ dòng mới, đếm số dòng bị bỏ và báo lại sau.</li>
 *   <li>Mức log chỉnh bằng {@code log.level} trong server.properties (DEBUG/INFO/WARN/ERROR). Log theo từng
 *       người chơi (kết nối, vào map...) để ở DEBUG để 10.000 người online không làm ngập console.</li>
 *   <li>Mỗi dòng: {@code giờ | MỨC | Nguồn | nội dung}; nguồn = tên class gọi log (tự lấy bằng StackWalker, chỉ khi
 *       mức log đang bật). Màu ANSI theo {@code log.color} (auto / on / off).</li>
 * </ul>
 */
public final class Log {

   public enum Level { DEBUG, INFO, WARN, ERROR }

   private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
   private static final int SOURCE_WIDTH = 14;
   private static final String RESET = "[0m";
   private static final String DIM = "[90m";
   private static final String CYAN = "[36m";
   private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
   private static final int QUEUE_CAPACITY = 65_536;
   private static final int DRAIN_BATCH = 512;

   private static final BlockingQueue<String> QUEUE = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
   private static final AtomicLong DROPPED = new AtomicLong();
   private static volatile Level level = Level.INFO;
   private static volatile boolean color = detectColor();
   private static final Thread WRITER;

   static {
      useUtf8Console();
      WRITER = new Thread(Log::writeLoop, "log-writer");
      WRITER.setDaemon(true);
      WRITER.start();
   }

   private Log() {
   }

   public static void setLevel(Level newLevel) {
      level = newLevel;
   }

   /** {@code log.color}: "on" / "off" / "auto" (bật khi terminal hiểu mã màu ANSI). */
   public static void setColor(String mode) {
      String m = mode == null ? "auto" : mode.trim().toLowerCase(java.util.Locale.ROOT);
      color = switch (m) {
         case "on", "true", "1" -> true;
         case "off", "false", "0" -> false;
         default -> detectColor();
      };
   }

   /**
    * Console Windows mặc định dùng code page cũ (437 / 1258) -> byte UTF-8 hiện thành "Ä�áº§m".
    * Chạy {@code chcp 65001} trên CHÍNH console đó (inheritIO) 1 lần lúc khởi động. Không có console (chạy nền, IDE) thì
    * bỏ qua.
    */
   private static void useUtf8Console() {
      boolean windows = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
      if (!windows || System.console() == null) {
         return;
      }
      try {
         new ProcessBuilder("cmd", "/c", "chcp 65001 >nul").inheritIO().start().waitFor(3, TimeUnit.SECONDS);
      } catch (IOException e) {
         // không đổi được: vẫn chạy, chỉ hiện sai ký tự
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   /** Windows Terminal / VS Code / ConEmu / terminal Linux-mac hiểu ANSI; cmd.exe cũ thì không (hiện ký tự rác). */
   private static boolean detectColor() {
      if (System.getenv("NO_COLOR") != null) {
         return false;
      }
      if (System.getenv("WT_SESSION") != null || System.getenv("TERM_PROGRAM") != null
            || "ON".equalsIgnoreCase(System.getenv("ConEmuANSI"))) {
         return true;
      }
      boolean windows = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
      return !windows && System.console() != null && System.getenv("TERM") != null;
   }

   /**
    * In khung nhiều dòng — CHỈ ký tự ASCII ({@code + - |}): Output NetBeans / IDE đọc theo Windows-1252, ký tự khung
    * Unicode sẽ thành rác.
    * Mỗi dòng 1 cặp {nhãn, giá trị} (vd lúc server sẵn sàng).
    */
   public static void banner(String title, String[][] rows) {
      int labelWidth = 0;
      int width = title.length();
      for (String[] row : rows) {
         labelWidth = Math.max(labelWidth, row[0].length());
      }
      for (String[] row : rows) {
         width = Math.max(width, labelWidth + 3 + row[1].length());
      }
      String bar = "-".repeat(width + 2);
      boolean c = color;
      StringBuilder sb = new StringBuilder();
      sb.append(System.lineSeparator()).append("  +").append(bar).append('+').append(System.lineSeparator());
      sb.append("  | ").append(c ? "[1;32m" : "").append(pad(title, width)).append(c ? RESET : "").append(" |")
            .append(System.lineSeparator());
      sb.append("  +").append(bar).append('+').append(System.lineSeparator());
      for (String[] row : rows) {
         String text = (c ? CYAN : "") + pad(row[0], labelWidth) + (c ? RESET : "") + " : "
               + pad(row[1], width - labelWidth - 3);
         sb.append("  | ").append(text).append(" |").append(System.lineSeparator());
      }
      sb.append("  +").append(bar).append('+');
      if (!QUEUE.offer(sb.toString())) {
         DROPPED.incrementAndGet();
      }
   }

   private static String pad(String text, int width) {
      return text.length() >= width ? text : text + " ".repeat(width - text.length());
   }

   public static boolean isDebug() {
      return level == Level.DEBUG;
   }

   public static void debug(String format, Object... args) {
      if (level == Level.DEBUG) {
         enqueue("DEBUG", format, args);
      }
   }

   public static void info(String format, Object... args) {
      if (level.ordinal() <= Level.INFO.ordinal()) {
         enqueue("INFO", format, args);
      }
   }

   /** Bỏ dấu tiếng Việt (log KHÔNG dấu): "Quả mọng đỏ" -> "Qua mong do". */
   public static String ascii(String text) {
      if (text == null) {
         return "";
      }
      String s = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
      return s.replace('đ', 'd').replace('Đ', 'D');
   }

   public static void warn(String format, Object... args) {
      if (level.ordinal() <= Level.WARN.ordinal()) {
         enqueue("WARN", format, args);
      }
   }

   public static void error(String format, Object... args) {
      enqueue("ERROR", format, args);
   }

   public static void error(String message, Throwable error) {
      StringWriter trace = new StringWriter();
      error.printStackTrace(new PrintWriter(trace));
      enqueue("ERROR", "%s%n%s", message, trace.toString().stripTrailing());
   }

   /** Chờ ghi hết log đang xếp hàng (gọi lúc tắt server). */
   public static void flush(long timeoutMs) {
      long deadline = System.currentTimeMillis() + timeoutMs;
      while (!QUEUE.isEmpty() && System.currentTimeMillis() < deadline) {
         try {
            Thread.sleep(10);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
         }
      }
   }

   private static void enqueue(String levelName, String format, Object... args) {
      String message = args.length == 0 ? format : String.format(format, args);
      String source = source();
      remember(levelName, source.trim(), message);
      if (!QUEUE.offer(line(levelName, source, message))) {
         DROPPED.incrementAndGet();
      }
   }

   /** 1 dòng log gần đây (admin panel): số thứ tự tăng dần, giờ, mức, nguồn, nội dung. */
   public record Entry(long seq, String time, String level, String source, String message) {
   }

   private static final int RECENT_CAPACITY = 500;
   private static final java.util.ArrayDeque<Entry> RECENT = new java.util.ArrayDeque<>(RECENT_CAPACITY);
   private static long recentSeq;

   private static void remember(String levelName, String source, String message) {
      Entry e;
      synchronized (RECENT) {
         e = new Entry(++recentSeq, LocalTime.now().format(TIME_FORMAT), levelName, source, message);
         if (RECENT.size() >= RECENT_CAPACITY) {
            RECENT.pollFirst();
         }
         RECENT.addLast(e);
      }
   }

   /** Các dòng có số thứ tự > {@code afterSeq} (tối đa 500 dòng gần nhất). */
   public static List<Entry> recent(long afterSeq) {
      List<Entry> list = new ArrayList<>();
      synchronized (RECENT) {
         for (Entry e : RECENT) {
            if (e.seq() > afterSeq) {
               list.add(e);
            }
         }
      }
      return list;
   }

   /** Tên class gọi log (bỏ qua chính {@link Log}), cắt / đệm cho thẳng cột. */
   private static String source() {
      String name = WALKER.walk(frames -> frames.map(StackWalker.StackFrame::getDeclaringClass)
            .filter(c -> c != Log.class).findFirst().map(Class::getSimpleName).orElse("?"));
      int inner = name.indexOf('$');
      if (inner > 0) {
         name = name.substring(0, inner);
      }
      return name.length() > SOURCE_WIDTH ? name.substring(0, SOURCE_WIDTH) : pad(name, SOURCE_WIDTH);
   }

   private static String line(String levelName, String source, String message) {
      String time = LocalTime.now().format(TIME_FORMAT);
      String lvl = pad(levelName, 5);
      if (!color) {
         return time + " | " + lvl + " | " + source + " | " + message;
      }
      String levelColor = switch (levelName) {
         case "ERROR" -> "[1;31m";
         case "WARN" -> "[33m";
         case "INFO" -> "[32m";
         default -> DIM;
      };
      String body = switch (levelName) {
         case "ERROR" -> "[31m" + message + RESET;
         case "WARN" -> "[33m" + message + RESET;
         case "DEBUG" -> DIM + message + RESET;
         default -> message;
      };
      return DIM + time + " | " + RESET + levelColor + lvl + RESET + DIM + " | " + RESET + CYAN + source + RESET + DIM
            + " | " + RESET + body;
   }

   private static void writeLoop() {
      Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.out),
            StandardCharsets.UTF_8), 1 << 16);
      List<String> batch = new ArrayList<>(DRAIN_BATCH);
      while (true) {
         try {
            String first = QUEUE.poll(1, TimeUnit.SECONDS);
            if (first != null) {
               batch.add(first);
               QUEUE.drainTo(batch, DRAIN_BATCH - 1);
            }
            long dropped = DROPPED.getAndSet(0);
            if (dropped > 0) {
               batch.add(line("WARN", pad("Log", SOURCE_WIDTH), "Log qua dong, da bo " + dropped + " dong"));
            }
            if (batch.isEmpty()) {
               continue;
            }
            for (String line : batch) {
               out.write(line);
               out.write(System.lineSeparator());
            }
            out.flush();
            batch.clear();
         } catch (InterruptedException e) {
            return;
         } catch (IOException e) {
            batch.clear(); // stdout hỏng: bỏ qua, không làm chết luồng
         }
      }
   }
}
