package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.ui.window.GamePanel;

public final class SkeletonActor extends GamePanel {
   public AnimationState animState;
   public Skeleton skeleton;

   public SkeletonActor(RenderContext renderContext, String skeletonName, float scale) {
      super(renderContext);
      NamedPair pair;
      if ((pair = SkeletonLoader.obtainSkeleton(skeletonName, 5.0F, new SkeletonLoader.AnimationMixConfig())) != null) {
         this.animState = (AnimationState)pair.first;
         this.skeleton = (Skeleton)pair.second;
      }
   }

   public final void playAnimation(String animationName, boolean loop) {
      if (this.animState != null) {
         this.animState.setAnimation(0, animationName, false);
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.skeleton != null) {
         super.renderContext.drawSkeleton(this.skeleton);
      }
   }

   @Override
   public final void onRender(float delta) {
      if (this.skeleton != null) {
         this.animState.update(delta);
         this.animState.apply(this.skeleton);
         this.skeleton.update(Gdx.graphics.getDeltaTime());
         this.skeleton.setPosition(this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F);
         this.skeleton.setScale(25.0F, 25.0F);
         this.skeleton.updateWorldTransform(Skeleton.Physics.UPDATE);
      }
   }
}
