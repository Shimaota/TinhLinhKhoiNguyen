package com.tlnt.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.tlnt.util.FileStorage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class CrashHandler implements UncaughtExceptionHandler {
   /** Crash log file name, inside the game data folder ({@link FileStorage#getStorageRoot()}). */
   public static final String CRASH_FILE = "crash.txt";
   /** Above this size the file is overwritten instead of appended, so it does not grow forever. */
   private static final long MAX_CRASH_FILE_BYTES = 256 * 1024;

   public com.tlnt.core.GameMain game;

   public CrashHandler(GameMain game) {
      super();
      this.game = game;
   }

   @Override
   public final void uncaughtException(Thread thread, Throwable throwable) {
      System.out.println("Có lỗi lỏ xảy ra!");
      throwable.printStackTrace();
      writeCrashFile(thread, throwable);
      if (Gdx.app != null) {
         Gdx.app.exit();
      }
   }

   /**
    * Writes the error to {@code crash.txt} so players can send it in (Android exits silently via {@code System.exit},
    * with no "has stopped" dialog). Errors while writing the file are only logged, never rethrown, so the original error is not lost.
    */
   private static void writeCrashFile(Thread thread, Throwable throwable) {
      try {
         StringWriter text = new StringWriter();
         PrintWriter out = new PrintWriter(text);
         out.println("===== " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " =====");
         out.println("Game: " + ClientConfig.GAME_NAME + " " + ClientConfig.VERSION);
         if (Gdx.app != null) {
            out.println("Nen tang: " + Gdx.app.getType() + " (API/version " + Gdx.app.getVersion() + ")");
         }
         out.println("He dieu hanh: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
               + " | Java/VM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
         out.println("Thread: " + thread.getName());
         throwable.printStackTrace(out);
         out.println();
         out.flush();

         FileHandle file = FileStorage.getExternalFile(CRASH_FILE);
         boolean append = file.exists() && file.length() < MAX_CRASH_FILE_BYTES;
         file.writeString(text.toString(), append, "UTF-8");
         System.out.println("Da ghi loi ra: " + file.path());
      } catch (Throwable e) {
         System.out.println("Khong ghi duoc " + CRASH_FILE + ": " + e);
      }
   }

}
