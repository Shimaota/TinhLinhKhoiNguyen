package com.tlnt.screen;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.tlnt.core.GameMain;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.KeyboardInsets;
import com.tlnt.ui.widget.PooledLabel;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.TextField;

public class Scene2dScreen extends ScreenAdapter {
   public Table active;
   public ScrollPane debugScrollPane;
   public OrthographicCamera box2dDebugRenderer;
   public ScreenViewport editorViewport;
   public OrthographicCamera cameraTarget;
   public ScreenViewport loginSceneController;
   public OrthographicCamera uiCamera;
   public FitViewport uiViewport;
   public final RenderContext renderContext;
   public final Stage stage;
   public int lastKeyboardAdjustVersion;
   public Actor lastKeyboardFocus;
   public boolean editorMode;
   public ObjectMap<OrthographicCamera, String> cameraNames;

   public Scene2dScreen() {
      this.editorMode = false;
      this.cameraNames = new ObjectMap<>();
      this.cameraTarget = new OrthographicCamera();
      this.loginSceneController = new ScreenViewport(this.cameraTarget);
      this.loginSceneController.setUnitsPerPixel(0.01F);
      this.cameraNames.put(this.cameraTarget, "World camera");
      this.uiCamera = new OrthographicCamera();
      this.uiCamera.setToOrtho(false, (float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT);
      this.uiViewport = new FitViewport((float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT, this.uiCamera);
      this.cameraNames.put(this.uiCamera, "Screen camera");
      this.box2dDebugRenderer = new OrthographicCamera();
      this.editorViewport = new ScreenViewport(this.box2dDebugRenderer);
      this.editorViewport.setUnitsPerPixel(0.01F);
      this.box2dDebugRenderer.position.set(0.0F, 0.0F, 0.0F);
      this.box2dDebugRenderer.zoom = 1.5F;
      this.box2dDebugRenderer.update();
      this.cameraNames.put(this.box2dDebugRenderer, "Editor camera");
      this.renderContext = new RenderContext(
         this.box2dDebugRenderer, this.cameraTarget, this.uiCamera, this.editorViewport, this.loginSceneController, this.uiViewport
      );
      this.renderContext.setBatch(GameMain.instance.getBatch());
      this.stage = new Stage(this.uiViewport, this.renderContext.getBatch());
      if (Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS || Gdx.app.getType() == Application.ApplicationType.WebGL) {
         // Phone / Mobile Web: tapping outside the text field -> drop focus -> TextField hides the soft keyboard.
         this.stage.addCaptureListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
               Actor focus = Scene2dScreen.this.stage.getKeyboardFocus();
               if (focus instanceof TextField && !(event.getTarget() != null && event.getTarget().isDescendantOf(focus))) {
                  Scene2dScreen.this.stage.setKeyboardFocus(null);
               }

               return false;
            }
         });
      }

      this.debugScrollPane = ScrollPaneFactory.createScrollPane(this.active = new Table().top().left().padBottom(10.0F));
      this.debugScrollPane.setSize(400.0F, 600.0F);
      float topY = (float)WorldConfig.VIEWPORT_HEIGHT;
      this.debugScrollPane.setPosition(0.0F, topY, 10);
      this.stage.addActor(this.debugScrollPane);
      this.debugScrollPane.setVisible(false);
   }

   @Override
   public final void render(float delta) {
      // The soft keyboard (Android/iOS) covers the focused field -> shift the UI camera so the field sits right above the keyboard.
      // Keyboard height is written to KeyboardInsets by the launcher; recomputed when the keyboard resizes or the focused field changes.
      int kbVersion = KeyboardInsets.getVersion();
      Actor focused = this.stage.getKeyboardFocus();
      if (this.lastKeyboardAdjustVersion != kbVersion || this.lastKeyboardFocus != focused) {
         this.lastKeyboardAdjustVersion = kbVersion;
         this.lastKeyboardFocus = focused;
         Camera camera = this.stage.getCamera();
         Viewport viewport = this.stage.getViewport();
         float worldHeight = viewport.getWorldHeight();
         float centerY = worldHeight / 2.0F;
         float shift = 0.0F;
         float kbPixels = KeyboardInsets.getHeightFraction() * (float)Gdx.graphics.getHeight();
         if (focused != null && kbPixels > 0.0F && viewport.getScreenHeight() > 0) {
            // FitViewport has black bars: drop the bottom bar, then convert pixels -> UI units.
            float kbTop = (kbPixels - (float)viewport.getScreenY()) * worldHeight / (float)viewport.getScreenHeight() + 40.0F;
            Vector2 bottom = focused.localToStageCoordinates(new Vector2(0.0F, 0.0F));
            if (bottom.y < kbTop) {
               float top = bottom.y + focused.getHeight();
               shift = Math.min(kbTop - bottom.y, Math.max(0.0F, worldHeight - top));
            }
         }

         camera.position.y = centerY - shift;
         camera.update();
      }

      this.update(delta);
      this.renderWorld(this.renderContext);
      this.stage.act(delta);
      this.renderContext.useHudCamera();
      this.stage.draw();
      this.renderOverlay(this.renderContext, delta);
   }

   public final void showFloatingText(String text, SequenceAction action) {
      if (text != null && action != null) {
         PooledLabel label;
         (label = PooledLabel.POOL.obtain()).setAlignment(2);
         label.setText(text);
         label.addAction(Actions.sequence(action, Actions.removeActor()));
         this.stage.addActor(label);
      }
   }

   public void update(float delta) {
   }

   public void renderWorld(RenderContext ctx) {
   }

   public void renderOverlay(RenderContext ctx, float delta) {
   }

   @Override
   public void resize(int width, int height) {
      if (width != 0 && height != 0) {
         WorldConfig.updateViewport(width, height);
         this.cameraTarget.zoom = this.computeWorldZoom(width, height);
         this.loginSceneController.update(width, height);
         this.uiViewport.setWorldSize((float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT);
         this.uiViewport.update(width, height, true);
         this.editorViewport.update(width, height);
      }
   }

   public float computeWorldZoom(int width, int height) {
      if (width > 0 && height > 0) {
         float zoomX = 1500.0F / (float)width;
         float zoomY = 720.0F / (float)height;
         return Math.max(zoomX, zoomY);
      } else {
         return 1.0F;
      }
   }

   public final Stage getStage() {
      return this.stage;
   }

   public final OrthographicCamera getWorldCamera() {
      return this.cameraTarget;
   }

   public final boolean isEditorMode() {
      return this.editorMode;
   }

   public final OrthographicCamera getEditorCamera() {
      return this.box2dDebugRenderer;
   }

   public final RenderContext getRenderContext() {
      return this.renderContext;
   }

   public final Viewport getEditorViewport() {
      return this.editorViewport;
   }

   public final Viewport getWorldViewport() {
      return this.loginSceneController;
   }

   public final Viewport getUiViewport() {
      return this.uiViewport;
   }

   public final void clearFloatingTexts() {
      for (int i = this.stage.getActors().size - 1; i >= 0; i--) {
         Actor actor;
         if ((actor = this.stage.getActors().get(i)) instanceof PooledLabel) {
            actor.clearActions();
            actor.remove();
         }
      }
   }

}
