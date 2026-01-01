package com.tlnt.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.screen.Scene2dScreen;
import com.tlnt.screen.charselect.CharacterSelectionScreen;
import com.tlnt.screen.game.GameScreen;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.util.GameUtils;
import java.util.regex.Pattern;

public final class GameMain extends Game {
   public static final Pattern FLOATING_TEXT_PATTERN = Pattern.compile("\\[([\\d?]+)(?::([\\d?]+))?(?::([\\d?]+))?(?::([\\d?.]+))?\\](.*)");
   public static final Color BG_COLOR = new Color(0.25F, 0.25F, 0.25F, 1.0F);
   public static GameMain instance;
   public PolygonSpriteBatch batch;
   public LoginScreen loginScreen;
   public GameScreen gameScreen;
   public CharacterSelectionScreen mapLoadingScreen;
   public long lastGcTime;

   @Override
   public final void create() {
      // Set first: errors while loading settings / skeletons are also written to crash.txt.
      Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
      Settings.loadSettings();
      SkeletonLoader.init();
      instance = this;
      this.batch = new PolygonSpriteBatch();
      Gdx.graphics.setVSync(true);
      GameDataLoader.loadAllDataFromCache();
      this.showLoginScreen();
      this.showGameScreen();
      ServerPacketSender.getInstance();
      // GLProfiler removed (the decompiled build enabled it without reading any stats): it intercepts every GL call and calls glGetError after each one
      // -> slows phone GPUs, Android log spammed with "glGetError exceeded".
      applyDisplayMode();
   }

   public static void applyDisplayMode() {
      if (Settings.isFullscreen()) {
         Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
         Gdx.graphics.setFullscreenMode(displayMode);
      } else {
         Graphics graphics = Gdx.graphics;
         int windowWidth = 1280 / (Settings.isSmallScreen() ? 2 : 1);
         int baseHeight = 720;
         int heightDivisor;
         if (Settings.isSmallScreen()) {
            heightDivisor = 2;
         } else {
            heightDivisor = 1;
         }

         graphics.setWindowedMode(windowWidth, baseHeight / heightDivisor);
      }
   }

   @Override
   public final void render() {
      if (GameUtils.hasElapsed(this.lastGcTime, 60000)) {
         System.gc();
         this.lastGcTime = System.currentTimeMillis();
      }

      AudioManager.finishLoading();
      InputHandler.getInstance();
      if (InputHandler.isKeyJustPressed(141)) {
         if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(1280, 720);
         } else {
            Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(displayMode);
         }
      } else {
         InputHandler.getInstance();
         if (InputHandler.isKeyJustPressed(111)) {
            if (Gdx.graphics.isFullscreen()) {
               Gdx.graphics.setWindowedMode(1280, 720);
            }
         } else {
            InputHandler.getInstance();
            if (InputHandler.isKeyJustPressed(53)) {
               System.gc();
               System.out.println("GC");
            } else {
               InputHandler.getInstance();
               InputHandler.isKeyJustPressed(44);
            }
         }
      }

      ScreenUtils.clear(BG_COLOR);
      super.render();
   }

   public final void showLoginScreen() {
      if (this.loginScreen == null) {
         this.loginScreen = new LoginScreen();
      }

      this.setScreen(this.loginScreen);
   }

   public final void showGameScreen() {
      if (this.gameScreen == null) {
         this.gameScreen = new GameScreen();
      }

      this.setScreen(this.gameScreen);
   }

   public final void showMapLoadingScreen() {
      if (this.mapLoadingScreen == null) {
         this.mapLoadingScreen = new CharacterSelectionScreen();
      }

      this.setScreen(this.mapLoadingScreen);
   }

   public final LoginScreen getLoginScreen() {
      return this.loginScreen;
   }

   public final GameScreen getGameScreen() {
      return this.gameScreen;
   }

   public final Batch getBatch() {
      return this.batch;
   }

   @Override
   public final void dispose() {
      if (this.loginScreen != null) {
         this.loginScreen.dispose();
      }

      if (this.gameScreen != null) {
         this.gameScreen.dispose();
      }

      TaskScheduler.shutdownNow();
      if (Gdx.app != null) {
         Gdx.app.exit();
      }
   }

   public static void postFloatingMessage(String message) {
      Gdx.app.postRunnable(new FloatingMessageTask(message));
   }

   @Override
   public final void setScreen(Screen screen) {
      Screen current;
      if ((current = this.getScreen()) instanceof Scene2dScreen) {
         Scene2dScreen scene2dScreen;
         (scene2dScreen = (Scene2dScreen)current).clearFloatingTexts();
      }

      super.setScreen(screen);
   }

   public static void printArgs(String[] args) {
      String[] argArray;
      int argCount = (argArray = args).length;

      for (int i = 0; i < argCount; i++) {
         String arg = argArray[i];
         System.out.println(arg);
      }
   }

   static {
      try {
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

}
