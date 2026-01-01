package com.tlnt.screen;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tlnt.core.DebugCursorText;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.camera.PingSender;
import com.tlnt.engine.input.ActorDebugInspector;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.physics.PhysicsWorld;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;

public abstract class BaseScreen extends Scene2dScreen {
   public boolean active;
   public final PhysicsWorld debugScrollPane;
   public final Box2DDebugRenderer box2dDebugRenderer;
   public InputMultiplexer editorViewport;

   public BaseScreen() {
      this(WorldConfig.GRAVITY);
   }

   public BaseScreen(Vector2 gravity) {
      this.debugScrollPane = new PhysicsWorld(new World(gravity, true));
      this.box2dDebugRenderer = new Box2DDebugRenderer();
      new PingSender(this);
      this.onCreate();
   }

   public abstract void onCreate();

   @Override
   public final void show() {
      InputHandler.getInstance().setRenderContext(super.renderContext);
      if (this.editorViewport == null) {
         this.editorViewport = this.createInputMultiplexer();
      }

      Gdx.input.setInputProcessor(this.editorViewport);
      this.onShow();
   }

   public InputMultiplexer createInputMultiplexer() {
      InputMultiplexer multiplexer;
      (multiplexer = new InputMultiplexer()).addProcessor(new ActorDebugInspector(this.getStage()));
      multiplexer.addProcessor(this.getStage());
      multiplexer.addProcessor(InputHandler.getInstance());
      multiplexer.addProcessor(this);
      return multiplexer;
   }

   public void onShow() {
   }

   @Override
   public final void update(float delta) {
      this.onUpdate(delta);
      this.debugScrollPane.update(delta);
   }

   public abstract void onUpdate(float delta);

   @Override
   public final void renderWorld(RenderContext ctx) {
      ScreenUtils.clear(this.isEditorMode() ? Color.BLACK : GameMain.BG_COLOR);
      if (this.isEditorMode()) {
         this.getEditorViewport().apply();
         ctx.useDebugCamera();
         ctx.restartBatch();
         this.debugScrollPane.renderDebug(ctx);
         this.debugScrollPane.render(ctx);
         ctx.endBatch();
         this.debugScrollPane.processQueues();
         this.box2dDebugRenderer.render(this.debugScrollPane.getBox2dWorld(), this.getEditorCamera().combined);
      } else {
         this.getWorldViewport().apply();
         ctx.useWorldCamera();
         ctx.restartBatch();
         this.debugScrollPane.renderDebug(ctx);
         this.debugScrollPane.render(ctx);
         ctx.endBatch();
         this.debugScrollPane.processQueues();
      }
   }

   @Override
   public void renderOverlay(RenderContext ctx, float delta) {
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
      ctx.endBatch();
   }

   public final PhysicsWorld getPhysicsWorld() {
      return this.debugScrollPane;
   }

   @Override
   public final void dispose() {
      this.debugScrollPane.dispose();
      this.box2dDebugRenderer.dispose();
      this.onDispose();
   }

   public abstract void onDispose();
}
