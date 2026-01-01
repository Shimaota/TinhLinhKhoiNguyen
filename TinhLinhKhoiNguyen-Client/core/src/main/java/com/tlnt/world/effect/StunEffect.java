package com.tlnt.world.effect;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class StunEffect extends SpineSkeletonComponent {
   public GameObject lastTemplate;
   public long effectStartTime;
   public int effectDuration;
   public boolean projectile2;

   public StunEffect(WorldEntity owner) {
      super(owner.getEntity());
      this.lastTemplate = owner;
      this.setRenderOrder(this.lastTemplate.getRenderOrder());
      this.requestLayerChange("skeletons/vfxs/stun", 0.25F, new SkeletonLoader.AnimationMixConfig(0.2F));
      this.setEnabled(false);
   }

   public final void startStun(int durationMs) {
      this.setSkeletonOffset(0.0F, this.lastTemplate.getHeight());
      this.effectStartTime = System.currentTimeMillis();
      this.effectDuration = durationMs;
      if (!this.projectile2) {
         this.projectile2 = true;
         String animName = "stun";
         this.playAnimation(animName, true, 1.0F);
         this.setEnabled(true);
         if (this.lastTemplate.getEntityType() == EntityType.MOB) {
            this.lastTemplate.setAnimationState(AnimationState.MOB_STUN);
         } else if (this.lastTemplate.getEntityType() == EntityType.PLAYER) {
            this.lastTemplate.setAnimationState(AnimationState.STUN);
         }

         AudioManager.loopSound(this, "stun", this.lastTemplate.getCenterPosition());
      }
   }

   public final void stopStun() {
      this.projectile2 = false;
      this.setEnabled(false);
      AudioManager.stopLoopedSound(this, "stun");
   }

   @Override
   public final void update(float delta) {
      if (this.projectile2 && GameUtils.hasElapsed(this.effectStartTime, this.effectDuration)) {
         if (this.lastTemplate.getEntityType() == EntityType.MOB) {
            this.lastTemplate.setAnimationState(AnimationState.MOB_IDLE);
         } else if (this.lastTemplate.getEntityType() == EntityType.PLAYER) {
            this.lastTemplate.setAnimationState(AnimationState.IDLE);
         }

         this.stopStun();
      }
   }

}
