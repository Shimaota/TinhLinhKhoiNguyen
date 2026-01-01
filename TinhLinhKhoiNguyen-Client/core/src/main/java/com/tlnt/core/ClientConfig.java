package com.tlnt.core;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Client "branding" settings — change the game name / data folder / default server WITHOUT touching code:
 *
 * <ol>
 *   <li>{@code client.properties} on the classpath (generated at build time from the repo-root {@code client.properties}
 *       by {@code :core:generateClientConfig}; override single keys with {@code TLNT_*} environment variables).</li>
 * </ol>
 * Only the copy inside the jar is used (the override file next to the game was dropped on 2026-10-19) — to change the
 * server, edit the repo-root {@code client.properties} and rebuild.
 * Read with {@code java.util.Properties} (no libGDX needed) so launchers can use it BEFORE creating Lwjgl3Application.
 */
public final class ClientConfig {

   public static final String FILE_NAME = "client.properties";

   private static final Properties PROPS = load();

   /** Name shown on the window / installer. */
   public static final String GAME_NAME = get("game.name", ClientConfigGenerated.GAME_NAME);
   /** Client version (sent in the op 0 handshake, shown in the window title). */
   public static final String VERSION = get("game.version", ClientConfigGenerated.VERSION);
   /**
    * Game data folder: PC = {@code <user.home>/AppData/LocalLow/<STORAGE_FOLDER>}, phone = external storage /
    * {@code <STORAGE_FOLDER>}. Holds downloaded data — the "Clear data" button ({@code FileStorage.wipeDataAndExit}) deletes
    * this WHOLE folder, so settings / saved accounts do NOT live here (see {@link #PREFS_FOLDER}).
    */
   public static final String STORAGE_FOLDER = safeFolder(get("storage.folder", GAME_NAME), ClientConfigGenerated.STORAGE_FOLDER);
   /**
    * PC settings + saved accounts folder: {@code <user.home>/.<PREFS_FOLDER>/} (e.g. {@code C:\Users\<user>\.tlnt\}),
    * SEPARATE from {@link #STORAGE_FOLDER} so clearing downloaded data keeps accounts. libGDX used to write to
    * {@code ~/.prefs/} (shared by every libGDX game). The leading dot is added automatically — write {@code tlnt}, not {@code .tlnt}.
    */
   public static final String PREFS_FOLDER = safeFolder(get("prefs.folder", "tlnt"), ClientConfigGenerated.PREFS_FOLDER);
   /** Preferences file name prefix ({@code <prefix>setting}, {@code <prefix>account}) — Android uses it as the SharedPreferences name. */
   public static final String PREFS_PREFIX = get("prefs.prefix", ClientConfigGenerated.PREFS_PREFIX);
   /** Default server when the player has not chosen one (the server list sent by the server still takes priority). */
   public static final String SERVER_NAME = get("server.name", ClientConfigGenerated.SERVER_NAME);
   public static final String SERVER_HOST = get("server.host", ClientConfigGenerated.SERVER_HOST);
   public static final int SERVER_PORT = getInt("server.port", ClientConfigGenerated.SERVER_PORT);
   public static final String SERVER_WS_URL = get("server.ws.url", ClientConfigGenerated.SERVER_WS_URL);

   private ClientConfig() {
   }

   /** PC Preferences folder, relative to {@code user.home} (libGDX {@code FileType.External}): {@code .<PREFS_FOLDER>/}. */
   public static String desktopPrefsDir() {
      return "." + PREFS_FOLDER + "/";
   }

   /**
    * Locations where older builds stored Preferences (relative to {@code user.home}), newest first: the 2026-10-17 build in
    * the data folder, then libGDX's default {@code .prefs/}. DesktopLauncher copies them to {@link #desktopPrefsDir()} if missing.
    */
   public static String[] legacyDesktopPrefsDirs() {
      return new String[] {"AppData/LocalLow/" + STORAGE_FOLDER + "/settings/", ".prefs/"};
   }

   public static String get(String key, String def) {
      String v = PROPS.getProperty(key);
      return v == null || v.trim().isEmpty() ? def : v.trim();
   }

   public static int getInt(String key, int def) {
      try {
         return Integer.parseInt(get(key, String.valueOf(def)));
      } catch (NumberFormatException e) {
         return def;
      }
   }

   private static Properties load() {
      Properties p = new Properties();
      try (InputStream in = ClientConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
         if (in != null) {
            read(p, in);
         }
      } catch (Exception ignored) {
         // missing file / read error: use the defaults in code
      }
      return p;
   }

   private static void read(Properties p, InputStream in) throws java.io.IOException {
      try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) { // UTF-8: Vietnamese names with diacritics
         p.load(r);
      }
   }

   /** Safe folder name: strips characters Windows forbids ({@code \ / : * ? " < > |}) and leading/trailing dots / spaces. */
   static String safeFolder(String name, String def) {
      String s = name == null ? "" : name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").trim();
      while (s.endsWith(".")) {
         s = s.substring(0, s.length() - 1).trim();
      }
      while (s.startsWith(".")) {
         s = s.substring(1).trim();
      }
      return s.isEmpty() ? def : s;
   }
}
