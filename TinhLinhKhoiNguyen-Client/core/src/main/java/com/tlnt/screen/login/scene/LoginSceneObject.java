package com.tlnt.screen.login.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;

public abstract class LoginSceneObject {
   public static final Matrix4 SAVED_TRANSFORM = new Matrix4();
   public static final Matrix4 TARGET_TRANSFORM = new Matrix4();
   public static final Vector2 BUTTON_CENTER = new Vector2();
   public LoginScreen loginScreen;
   public Button targetButton;
   public Table targetTable;
   public Vector2 worldPosition = new Vector2(-1.0F, -1.0F);
   public Skeleton skeletonRenderer;
   public AnimationState animationState;

   public LoginSceneObject(LoginScreen screen) {
      this.loginScreen = screen;
      NamedPair skeletonPair;
      if ((skeletonPair = SkeletonLoader.obtainSkeleton("ui/target", 50.0F, new SkeletonLoader.AnimationMixConfig(0.0F))) != null) {
         this.animationState = (AnimationState)skeletonPair.first;
         this.skeletonRenderer = (Skeleton)skeletonPair.second;
         this.animationState.setTimeScale(0.75F);
         String animationName = "target2";
         this.animationState.setAnimation(0, animationName, true);
      }
   }

   public final void clearTarget() {
      this.targetButton = null;
      this.targetTable = null;
      this.worldPosition.set(-1.0F, -1.0F);
   }

   public final void updateAnimation(float delta) {
      if (this.animationState != null && this.skeletonRenderer != null) {
         this.animationState.update(delta);
         this.animationState.apply(this.skeletonRenderer);
         this.skeletonRenderer.update(delta);
         this.skeletonRenderer.updateWorldTransform(Skeleton.Physics.UPDATE);
      }

      this.updateTarget();
   }

   public void updateTarget() {
   }

   public final void render(RenderContext ctx) {
      Batch batch = ctx.getBatch();
      if (this.targetButton != null && this.skeletonRenderer != null) {
         BUTTON_CENTER.set(this.targetButton.getWidth() * 0.5F, this.targetButton.getHeight() * 0.5F);
         this.targetButton.localToStageCoordinates(BUTTON_CENTER);
         SAVED_TRANSFORM.set(batch.getTransformMatrix());
         TARGET_TRANSFORM.set(SAVED_TRANSFORM);
         TARGET_TRANSFORM.translate(BUTTON_CENTER.x, BUTTON_CENTER.y, 0.0F);
         batch.setTransformMatrix(TARGET_TRANSFORM);
         this.skeletonRenderer.setPosition(0.0F, 0.0F);
         this.skeletonRenderer.updateWorldTransform(Skeleton.Physics.UPDATE);
         ctx.drawSkeleton(this.skeletonRenderer);
         batch.setTransformMatrix(SAVED_TRANSFORM);
      }
   }

   public boolean isActive() {
      return false;
   }

   static {
      new Color(0.0F, 0.0F, 0.0F, 0.75F);
   }

}
