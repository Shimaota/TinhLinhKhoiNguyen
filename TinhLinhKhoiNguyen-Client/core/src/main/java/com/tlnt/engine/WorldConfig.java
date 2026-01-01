package com.tlnt.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

public final class WorldConfig {
   public static final Vector2 GRAVITY = new Vector2(0.0F, -49.0F);
   public static int VIEWPORT_WIDTH;
   public static int VIEWPORT_HEIGHT;

   static {
      int screenWidth = Gdx.graphics != null ? Gdx.graphics.getWidth() : 1920;
      int screenHeight = Gdx.graphics != null ? Gdx.graphics.getHeight() : 1080;
      updateViewport(screenWidth, screenHeight);
   }

   public static void updateViewport(int screenWidth, int screenHeight) {
      if (screenWidth <= 0 || screenHeight <= 0) {
         VIEWPORT_WIDTH = 1920;
         VIEWPORT_HEIGHT = 1080;
         return;
      }
      float aspectRatio = (float)screenWidth / (float)screenHeight;
      if (aspectRatio >= 1.7777778F) {
         VIEWPORT_HEIGHT = 1080;
         VIEWPORT_WIDTH = Math.round(1080.0F * aspectRatio);
      } else {
         VIEWPORT_WIDTH = 1920;
         VIEWPORT_HEIGHT = Math.round(1920.0F / aspectRatio);
      }
   }
}
