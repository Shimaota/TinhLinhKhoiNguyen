package com.tlnt.world.label;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

public final class TitleSkeletonLabel extends FloatingOverlay {
   public PlayerEntity playerEntity;
   public Skeleton skeletonRenderer;
   public AnimationState animationState;
   public String currentTitleKey;

   public TitleSkeletonLabel(PlayerEntity playerEntity) {
      super(new Vector2(0.0F, 65.0F));
      this.playerEntity = playerEntity;
   }

   @Override
   public final void refresh() {
      if (this.playerEntity.POOL2.titleKey != null && !this.playerEntity.POOL2.titleKey.isEmpty()) {
         if (!this.playerEntity.POOL2.titleKey.equals(this.currentTitleKey) || this.skeletonRenderer == null) {
            this.setVisible(true);
            this.currentTitleKey = this.playerEntity.POOL2.titleKey;
            NamedPair skeletonPair;
            if ((skeletonPair = SkeletonLoader.obtainSkeleton(this.currentTitleKey, 1.0F, new SkeletonLoader.AnimationMixConfig())) != null) {
               this.animationState = (AnimationState)skeletonPair.first;
               this.skeletonRenderer = (Skeleton)skeletonPair.second;
               this.animationState.setAnimation(0, "idle", true);
            }
         }
      } else {
         this.skeletonRenderer = null;
         this.animationState = null;
         this.setVisible(false);
      }
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      if (this.skeletonRenderer != null) {
         this.animationState.update(Gdx.graphics.getDeltaTime());
         this.animationState.apply(this.skeletonRenderer);
         this.skeletonRenderer.update(Gdx.graphics.getDeltaTime());
         this.skeletonRenderer.setPosition(x, y);
         this.skeletonRenderer.setScale(25.0F, 25.0F);
         this.skeletonRenderer.updateWorldTransform(Skeleton.Physics.UPDATE);
      }

      if (this.skeletonRenderer != null) {
         ctx.drawSkeleton(this.skeletonRenderer);
      }
   }

}
