package com.tlnt.screen.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.DebugCursorText;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.net.NetworkClient;
import com.tlnt.screen.BaseScreen;
import com.tlnt.screen.game.dialog.LoginDialogManager;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;
import com.tlnt.util.GameUtils;

public final class GameScreen extends BaseScreen {
   public LoginDialogManager active;

   @Override
   public final void onShow() {
      AudioManager.playMusic("sound_1");
   }

   @Override
   public final void onCreate() {
      this.active = new LoginDialogManager(this);
   }

   @Override
   public final void onUpdate(float delta) {
   }

   @Override
   public final void renderOverlay(RenderContext ctx, float delta) {
      ctx.beginHudBatch();
      if (!DebugCursorText.text.isEmpty()) {
         Vector2 cursorPos = this.getStage().screenToStageCoordinates(new Vector2((float)Gdx.input.getX(), (float)Gdx.input.getY()));
         float phase = (float)(System.currentTimeMillis() % 3000L) / 3000.0F;
         float red = 0.5F + 0.5F * MathUtils.sin((float) (Math.PI * 2) * phase);
         float green = 0.5F + 0.5F * MathUtils.sin((float) (Math.PI * 2) * phase + (float) (Math.PI * 2.0 / 3.0));
         phase = 0.5F + 0.5F * MathUtils.sin((float) (Math.PI * 2) * phase + (float) (Math.PI * 4.0 / 3.0));
         Color cursorColor = new Color(red, green, phase, 1.0F);
         ctx.drawText(FontManager.DEFAULT_FONT, DebugCursorText.text, cursorPos.x, cursorPos.y + 20.0F, Anchor.CENTER, 30.0F, cursorColor);
      }

      // iPhone: rounded corners + home indicator cover the bottom-left corner -> push the text up / inwards
      boolean isIOS = Gdx.app.getType() == Application.ApplicationType.iOS;
      float infoX = isIOS ? 40.0F : 10.0F;
      float infoY = isIOS ? 50.0F : 10.0F;
      ctx.drawText(FontManager.PLAIN_FONT, "Zoom: " + ctx.getWorldCamera().zoom, infoX, infoY + 20.0F, Anchor.MIDDLE_LEFT, 20.0F, Color.WHITE);
      ctx.drawText(
         FontManager.PLAIN_FONT,
         "FPS: " + Gdx.graphics.getFramesPerSecond() + " - Ping: " + GameState.pingMs + " ms",
         infoX,
         infoY,
         Anchor.MIDDLE_LEFT,
         20.0F,
         Color.WHITE
      );
      Color statusColor = NetworkClient.connected ? Color.GREEN : (NetworkClient.connecting ? Color.GRAY : Color.RED);
      float statusAlpha = !NetworkClient.connected && NetworkClient.connecting ? GameUtils.oscillate(0.1F, 0.0F, 1.0F) : 1.0F;
      ctx.getShapeDrawer().setColorRGBA(statusColor.r, statusColor.g, statusColor.b, statusAlpha);
      ShapeDrawer shapeDrawer = ctx.getShapeDrawer();
      float circleX = (float)(WorldConfig.VIEWPORT_WIDTH - 15);
      shapeDrawer.drawFilledCircle(circleX, (float)(WorldConfig.VIEWPORT_HEIGHT - 15), 10.0F);
      ctx.getShapeDrawer().setColorRGBA(1.0F, 1.0F, 1.0F, statusAlpha);
      shapeDrawer = ctx.getShapeDrawer();
      shapeDrawer.drawCircle(
         (float)(WorldConfig.VIEWPORT_WIDTH - 15),
         (float)(WorldConfig.VIEWPORT_HEIGHT - 15),
         10.0F,
         2.0F
      );
      ctx.endBatch();
   }

   @Override
   public final void onDispose() {
   }

   @Override
   public final float computeWorldZoom(int width, int height) {
      if (width > 0 && height > 0) {
         float zoomX = 1920.0F / (float)width;
         float zoomY = 1080.0F / (float)height;
         return Math.max(zoomX, zoomY);
      } else {
         return 1.0F;
      }
   }

}
