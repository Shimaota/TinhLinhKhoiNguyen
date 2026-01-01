package com.tlnt.ui.hud.itemslot;

import com.badlogic.gdx.Gdx;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;

public final class SlotSpineEffect {
   public AnimationState animationState;
   public Skeleton skeleton;

   public SlotSpineEffect(String skeletonName, float scale) {
      NamedPair pair;
      if ((pair = SkeletonLoader.obtainSkeleton(skeletonName, 3.5F, new SkeletonLoader.AnimationMixConfig())) != null) {
         this.animationState = (AnimationState)pair.first;
         this.skeleton = (Skeleton)pair.second;
      }
   }

   public final void setAnimation(String animationName, boolean loop) {
      if (this.animationState != null) {
         this.animationState.setAnimation(0, animationName, false);
      }
   }

   public final void renderAtPosition(RenderContext renderContext, float x, float y, float width, float height) {
      if (this.skeleton != null) {
         this.animationState.update(Gdx.graphics.getDeltaTime());
         this.animationState.apply(this.skeleton);
         this.skeleton.update(Gdx.graphics.getDeltaTime());
         this.skeleton.setPosition(x + width / 2.0F, y + height / 2.0F);
         this.skeleton.setScale(25.0F, 25.0F);
         this.skeleton.updateWorldTransform(Skeleton.Physics.UPDATE);
      }

      if (this.skeleton != null) {
         renderContext.drawSkeleton(this.skeleton);
      }
   }
}
