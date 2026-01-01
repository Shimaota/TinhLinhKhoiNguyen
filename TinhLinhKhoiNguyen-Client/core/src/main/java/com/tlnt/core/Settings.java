package com.tlnt.core;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.model.account.SavedAccount;
import com.tlnt.model.server.ServerInfo;
import com.tlnt.util.FileStorage;

public final class Settings {
   public static boolean assigningHpPotionSlot;
   public static boolean assigningMpPotionSlot;
   public static boolean assigningStaminaFoodSlot;
   public static int pendingQuickSlotItemId;
   public static boolean hideEffects;
   public static boolean isCapturingKeybind;
   public static int pendingKeybindIndex;
   public static int[] keybindings;
   public static boolean[] autoSkillSlotEnabled = new boolean[6];
   public static boolean autoIgnorePlayersAndBosses;
   public static boolean autoAvoidEliteMobs;
   public static boolean autoRevive;
   public static boolean autoPickupItems;
   public static boolean autoUseStaminaItems;
   public static boolean autoUseHpItems;
   public static boolean autoUseMpItems;
   public static boolean autoHarvestApples;
   public static int autoHpThresholdPercent;
   public static int autoMpThresholdPercent;
   /** "Attack range" (decompiled C3418.f12249): px measured from {@link #autoAttackAnchor}, 0 = unlimited. */
   public static int autoAttackRange;
   /** Position when auto-attack was turned ON (decompiled C3418.f12233, Box2D coordinates) — auto-attack only targets mobs within range of it. */
   public static final com.badlogic.gdx.math.Vector2 autoAttackAnchor = new com.badlogic.gdx.math.Vector2();
   public static boolean hideOtherPlayers;
   public static Array<SavedAccount> savedAccounts = new Array<>();
   public static Preferences prefs;
   public static Preferences accountPrefs;

   public static void loadSettings() {
      GameState.serverList.clear();
      int count = prefs.getInteger("amountserver", 0);

      for (int i = 0; i < count; i++) {
         String serverName = prefs.getString("servername_" + i, "");
         String serverHost = prefs.getString("serverhost_" + i, "");
         int serverPort = prefs.getInteger("serverport_" + i, 0);
         GameState.serverList.add(new ServerInfo(serverName, serverHost, serverPort));
      }

      AudioManager.setSoundVolume(prefs.getFloat("soundvolume", 50.0F));
      AudioManager.setMusicVolume(prefs.getFloat("musicvolume", 50.0F));
      savedAccounts.clear();
      count = accountPrefs.getInteger("amount", 0);

      for (int j = 0; j < count; j++) {
         savedAccounts.add(new SavedAccount(accountPrefs.getString("username" + j, ""), accountPrefs.getString("password" + j, "")));
      }

      hideEffects = prefs.getBoolean("hideenv", false);
   }

   public static void clearPreferences() {
      prefs.clear();
      prefs.flush();
   }

   public static void saveHideEffects(boolean hide) {
      prefs.putBoolean("hideenv", hide);
      prefs.flush();
   }

   public static void saveSoundVolume(float volume) {
      prefs.putFloat("soundvolume", volume);
      prefs.flush();
   }

   public static void saveMusicVolume(float volume) {
      prefs.putFloat("musicvolume", volume);
      prefs.flush();
   }

   public static void saveDisplaySettings(boolean smallScreen, boolean fullscreen) {
      prefs.putBoolean("smallscreen", smallScreen);
      prefs.putBoolean("fullscreen", fullscreen);
      prefs.flush();
   }

   public static void saveSelectedServer(String serverName, String serverHost, int serverPort) {
      prefs.putString("servername", serverName);
      prefs.putString("serverhost", serverHost);
      prefs.putInteger("serverport", serverPort);
      prefs.flush();
   }

   public static void saveServerEntry(int index, String serverName, String serverHost, int serverPort) {
      prefs.putString("servername_" + index, serverName);
      prefs.putString("serverhost_" + index, serverHost);
      prefs.putInteger("serverport_" + index, serverPort);
      String countKey = "amountserver";
      prefs.putInteger(countKey, index + 1);
      prefs.flush();
   }

   public static void saveServerCount(int count) {
      prefs.putInteger("amountserver", count);
      prefs.flush();
   }

   public static void saveAccount(String username, String password) {
      if (!username.isEmpty() && !password.isEmpty()) {
         savedAccounts.add(new SavedAccount(username, password));
         if (savedAccounts.size > 5) {
            savedAccounts.removeIndex(0);
         }

         persistSavedAccounts();
         GameMain.postFloatingMessage("Lưu thông tin đăng nhập thành công!");
      } else {
         GameMain.postFloatingMessage("Thông tin tài khoản và mật khẩu không được để trống!");
      }
   }

   public static void removeSavedAccount(SavedAccount account) {
      savedAccounts.removeValue(account, true);
      persistSavedAccounts();
   }

   public static void persistSavedAccounts() {
      accountPrefs.clear();
      accountPrefs.putInteger("amount", savedAccounts.size);

      for (int i = 0; i < savedAccounts.size; i++) {
         SavedAccount account = savedAccounts.get(i);
         accountPrefs.putString("username" + i, account.username);
         accountPrefs.putString("password" + i, account.password);
      }

      accountPrefs.flush();
   }

   public static String getServerName() {
      return prefs.getString("servername", ClientConfig.SERVER_NAME);
   }

   public static String getServerHost() {
      String hostKey = "serverhost";
      return prefs.getString(hostKey, ClientConfig.SERVER_HOST);
   }

   public static int getServerPort() {
      return prefs.getInteger("serverport", ClientConfig.SERVER_PORT);
   }

   public static boolean isSmallScreen() {
      return prefs.getBoolean("smallscreen", false);
   }

   public static boolean isFullscreen() {
      String fullscreenKey = "fullscreen";
      return prefs.getBoolean(fullscreenKey, false);
   }

   static {
      prefs = FileStorage.getPreferences("setting");
      pendingQuickSlotItemId = -1;
      keybindings = new int[16];
      accountPrefs = FileStorage.getPreferences("account");
   }

}
