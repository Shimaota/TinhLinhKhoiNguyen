package com.tlnt.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.tlnt.core.Settings;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FileStorage {

   /**
    * Preferences {@code name} (prefix {@link com.tlnt.core.ClientConfig#PREFS_PREFIX}). PC: a file in
    * {@link com.tlnt.core.ClientConfig#desktopPrefsDir()} (set by DesktopLauncher via {@code setPreferencesConfig} — no longer
    * {@code ~/.prefs/}); Android: SharedPreferences with the same name.
    */
   public static final Preferences getPreferences(String name) {
      return Gdx.app.getPreferences(com.tlnt.core.ClientConfig.PREFS_PREFIX + name);
   }

   public static String getStorageRoot() {
      if (Gdx.app == null) {
         return "AppData/LocalLow/" + com.tlnt.core.ClientConfig.STORAGE_FOLDER;
      }
      switch (Gdx.app.getType()) {
         case Desktop:
            return "AppData/LocalLow/" + com.tlnt.core.ClientConfig.STORAGE_FOLDER;
         case WebGL:
            return com.tlnt.core.ClientConfig.STORAGE_FOLDER;
         case iOS:
            // Gdx.files.external already prepends the app's Documents folder. Adding getExternalStoragePath (an absolute path containing the
            // container id) put the data at Documents/private/var/.../<id>/Documents/..., and iOS changes the container id on every
            // install / update -> all downloaded data was lost (empty inventory icons, getRegion retrying every frame -> lag).
            return com.tlnt.core.ClientConfig.STORAGE_FOLDER;
         default:
            String extPath = Gdx.files != null ? Gdx.files.getExternalStoragePath() : "";
            return (extPath != null ? extPath : "") + com.tlnt.core.ClientConfig.STORAGE_FOLDER;
      }
   }

   public static FileHandle getExternalFile(String relativePath) {
      String fullPath = getStorageRoot() + "/" + relativePath;
      if (Gdx.app != null && (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.WebGL
            || (Gdx.files != null && !Gdx.files.isExternalStorageAvailable()))) {
         return Gdx.files.local(fullPath);
      }
      return Gdx.files.external(fullPath);
   }

   public static void writeFile(String relativePath, byte[] data) {
      if (data != null) {
         FileHandle file = getExternalFile(relativePath);
         file.writeBytes(data, false);
      }
   }

   public static byte[] readFile(String relativePath) {
      FileHandle file;
      return (file = getExternalFile(relativePath)).exists() ? file.readBytes() : new byte[0];
   }

   /** Extracts into the storage folder. false = corrupt file / out of space (the caller must download again, not treat it as done). */
   public static boolean extractZip(FileHandle zipFile, String targetDir) {
      try {
         FileHandle dir = getExternalFile(targetDir);
         if (!dir.exists()) {
            dir.mkdirs();
         }

         try (
            InputStream in = zipFile.read();
            ZipInputStream zipIn = new ZipInputStream(in);
         ) {
            ZipEntry entry = zipIn.getNextEntry();

            while (entry != null) {
               String entryRelativePath = (targetDir != null && !targetDir.isEmpty() ? targetDir + "/" : "") + entry.getName();
               if (!entry.isDirectory()) {
                  if ("__deleted__.txt".equals(entry.getName())) {
                     ByteArrayOutputStream deletedListOut = new ByteArrayOutputStream();
                     byte[] chunk = new byte[4096];

                     int readCount;
                     while ((readCount = zipIn.read(chunk)) != -1) {
                        deletedListOut.write(chunk, 0, readCount);
                     }

                     String deletedList = new String(deletedListOut.toByteArray(), StandardCharsets.UTF_8);
                     String[] deletedPaths;
                     int pathCount = (deletedPaths = deletedList.split("\\r?\\n")).length;

                     for (int i = 0; i < pathCount; i++) {
                        String deletedPath;
                        if ((deletedPath = deletedPaths[i]) != null && !(deletedPath = deletedPath.trim()).isEmpty()) {
                           FileHandle deletedFile = getExternalFile((targetDir != null && !targetDir.isEmpty() ? targetDir + "/" : "") + deletedPath);

                           try {
                              if (deletedFile.exists()) {
                                 if (deletedFile.isDirectory()) {
                                    deletedFile.deleteDirectory();
                                 } else {
                                    deletedFile.delete();
                                 }
                              }
                           } catch (Exception ignored) {
                           }
                        }
                     }

                     zipIn.closeEntry();
                     entry = zipIn.getNextEntry();
                     continue;
                  }

                  FileHandle outFile = getExternalFile(entryRelativePath);
                  outFile.parent().mkdirs();

                  try (java.io.OutputStream out = outFile.write(false)) {
                     byte[] buf = new byte[4096];
                     int count;

                     while ((count = zipIn.read(buf)) != -1) {
                        out.write(buf, 0, count);
                     }
                  }
               } else {
                  getExternalFile(entryRelativePath).mkdirs();
               }

               zipIn.closeEntry();
               entry = zipIn.getNextEntry();
            }
         }

         System.out.println("done unzip");
         return true;
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }

   public static Runnable webWipeAction;

   public static void setWebWipeAction(Runnable action) {
      webWipeAction = action;
   }

   public static void wipeDataAndExit() {
      try {
         Settings.clearPreferences();
      } catch (Throwable ignored) {
      }

      if (Gdx.app != null && (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.WebGL
            || (Gdx.files != null && !Gdx.files.isExternalStorageAvailable()))) {
         try {
            Gdx.files.local(getStorageRoot()).deleteDirectory();
         } catch (Throwable ignored) {
         }
         if (webWipeAction != null) {
            webWipeAction.run();
            return;
         }
      } else if (Gdx.files != null) {
         try {
            Gdx.files.external(getStorageRoot()).deleteDirectory();
         } catch (Throwable ignored) {
         }
      }

      if (Gdx.app != null) {
         Gdx.app.exit();
      }
   }

}
