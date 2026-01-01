package com.tlnt.thirdparty.spine.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.SkeletonRenderer;

public final class SpineSkeletonActor extends Actor {
   public SkeletonRenderer renderer;
   public Skeleton skeleton;
   public AnimationState animationState;
   public boolean resetBlendFunction = true;

   @Override
   public final void act(float delta) {
      this.animationState.update(delta);
      this.animationState.apply(this.skeleton);
      super.act(delta);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      int blendSrc = batch.getBlendSrcFunc();
      int blendDst = batch.getBlendDstFunc();
      int blendSrcAlpha = batch.getBlendSrcFuncAlpha();
      int blendDstAlpha = batch.getBlendDstFuncAlpha();
      Color color;
      float oldAlpha = (color = this.skeleton.getColor()).a;
      this.skeleton.getColor().a *= parentAlpha;
      this.skeleton.setPosition(this.getX(), this.getY());
      this.skeleton.updateWorldTransform(Skeleton.Physics.UPDATE);
      this.skeleton.update(Gdx.graphics.getDeltaTime());
      this.renderer.draw(batch, this.skeleton);
      if (this.resetBlendFunction) {
         batch.setBlendFunctionSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha);
      }

      color.a = oldAlpha;
   }

   public final void setRenderer(SkeletonRenderer renderer) {
      this.renderer = renderer;
   }

   public final void setSkeleton(Skeleton skeleton) {
      this.skeleton = skeleton;
   }

   public final AnimationState getAnimationState() {
      return this.animationState;
   }

   public final void setAnimationState(AnimationState animationState) {
      this.animationState = animationState;
   }
}
