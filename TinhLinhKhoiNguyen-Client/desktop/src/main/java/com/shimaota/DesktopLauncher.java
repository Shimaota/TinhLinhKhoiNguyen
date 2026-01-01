package com.shimaota;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.tlnt.core.ClientConfig;
import com.tlnt.core.GameMain;
import java.io.File;
import org.lwjgl.system.Configuration;

public class DesktopLauncher {
   /** Window / taskbar icon: several sizes so Windows picks the sharpest (missing files are skipped). */
   static final String[] WINDOW_ICONS = {
      "images/icon_128.png", "images/icon_64.png", "images/icon_48.png", "images/icon_32.png", "images/icon_16.png", "images/icon.png"
   };
   /** Game Preferences file names (Settings: "setting", "account"). */
   static final String[] PREFS_NAMES = {"setting", "account"};

   public static void main(String[] var0) {
      Configuration.DISABLE_CHECKS.set(Boolean.TRUE);
      Configuration.DISABLE_FUNCTION_CHECKS.set(Boolean.TRUE);
      migrateLegacyPrefs();
      Lwjgl3ApplicationConfiguration var2;
      (var2 = new Lwjgl3ApplicationConfiguration()).setForegroundFPS(120);
      var2.setWindowedMode(1280, 720);
      var2.setTitle(ClientConfig.GAME_NAME + " - " + ClientConfig.VERSION);
      var2.setWindowIcon(Files.FileType.Classpath, existingIcons());
      // Preferences: <user.home>/AppData/LocalLow/<game folder>/<prefs>/ instead of libGDX's default ~/.prefs/.
      var2.setPreferencesConfig(ClientConfig.desktopPrefsDir(), Files.FileType.External);
      var2.setFullscreenMode(null);
      var2.setResizable(false);
      var2.setBackBufferConfig(8, 8, 8, 8, 0, 8, 16);
      var2.useVsync(true);
      GameMain var1 = new GameMain();
      var2.setWindowListener(new GameWindowListener(var1));
      new Lwjgl3Application(var1, var2);
   }

   static String[] existingIcons() {
      java.util.List<String> found = new java.util.ArrayList<>();
      for (String path : WINDOW_ICONS) {
         if (DesktopLauncher.class.getClassLoader().getResource(path) != null) {
            found.add(path);
         }
      }
      return found.toArray(new String[0]);
   }

   /**
    * Older builds stored settings + accounts elsewhere ({@link ClientConfig#legacyDesktopPrefsDirs()}: the data folder, then
    * {@code ~/.prefs/}): on the first run of a new build, copy the files from the newest old location to {@code ~/.<prefs.folder>/} (old files are
    * NOT deleted) so players do not lose their saved account / server list.
    */
   static void migrateLegacyPrefs() {
      migrateLegacyPrefs(new File(System.getProperty("user.home")));
   }

   static void migrateLegacyPrefs(File home) {
      File newDir = new File(home, ClientConfig.desktopPrefsDir());
      for (String name : PREFS_NAMES) {
         File target = new File(newDir, ClientConfig.PREFS_PREFIX + name);
         if (target.exists()) {
            continue;
         }
         File legacy = findLegacy(home, name);
         if (legacy == null) {
            continue;
         }
         try {
            newDir.mkdirs();
            java.nio.file.Files.copy(legacy.toPath(), target.toPath(), java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
         } catch (Exception e) {
            System.out.println("Khong chep duoc cai dat cu " + legacy + ": " + e);
         }
      }
   }

   /** First old settings file found (with or without prefix), null = never existed. */
   static File findLegacy(File home, String name) {
      File current = new File(home, ClientConfig.desktopPrefsDir()).getAbsoluteFile();
      for (String dir : ClientConfig.legacyDesktopPrefsDirs()) {
         File legacyDir = new File(home, dir);
         if (legacyDir.getAbsoluteFile().equals(current)) {
            continue; // prefs.folder=prefs: the new location equals the old one
         }
         for (String fileName : new String[] {ClientConfig.PREFS_PREFIX + name, name}) {
            File f = new File(legacyDir, fileName);
            if (f.isFile()) {
               return f;
            }
         }
      }
      return null;
   }
}
