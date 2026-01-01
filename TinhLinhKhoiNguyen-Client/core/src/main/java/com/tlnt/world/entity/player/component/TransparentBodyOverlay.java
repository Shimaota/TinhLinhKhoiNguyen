package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.Settings;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.physics.Entity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.SimpleSpineSkeletonComponent;

public final class TransparentBodyOverlay extends SimpleSpineSkeletonComponent {
   public PlayerEntity projectile2;
   public AnimationStateDispatcher lastTemplate;

   public TransparentBodyOverlay(PlayerEntity player) {
      super(player.getEntity());
      this.projectile2 = player;
      this.setRenderOrder(6000000.0F);
      this.addAnimationListener(new TransparentOverlayAnimListener(this));
   }

   @Override
   public final void onStart() {
      Entity entity = this.getEntity();
      this.lastTemplate = entity.findComponent(AnimationStateDispatcher.class, true);
   }

   @Override
   public final void lateUpdate(float delta) {
      super.lateUpdate(delta);
      this.setVisible(
         ((!this.projectile2.isLocalPlayer() && Settings.hideOtherPlayers ? 0 : 1) != 0)
      );
   }

   @Override
   public final void update(float delta) {
      if (this.projectile2.POOL2.isTransparent) {
         this.clearPendingRequests();
         this.lastTemplate.resetState();
         String skeletonKey = this.projectile2.POOL2.skeletonKey;
         SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
         this.requestLayerChange(skeletonKey, 0.25F, mixConfig);
         this.setSkins(this.projectile2.POOL2.equipmentSkinKeys);
         this.projectile2.POOL2.isTransparent = false;
      }

      if (super.speed != null) {
         if (this.projectile2.weakenEffect.projectile2) {
            super.speed.setColor(Color.PURPLE);
         }

         super.speed.setColorRGBA(1.0F, 1.0F, 1.0F, 0.25F);
      }
   }

   @Override
   public final void onDestroy() {
      super.onDestroy();
      this.projectile2 = null;
   }
}
