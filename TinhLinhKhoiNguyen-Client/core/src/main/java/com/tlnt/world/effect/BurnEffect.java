package com.tlnt.world.effect;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class BurnEffect extends SpineSkeletonComponent {
   public GameObject projectile2;
   public long lastTemplate;
   public int effectStartTime;
   public boolean effectDuration;

   public BurnEffect(WorldEntity owner) {
      super(owner.getEntity());
      this.projectile2 = owner;
      this.setRenderOrder(this.projectile2.getRenderOrder());
      String skeletonPath = "skeletons/vfxs/burn";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
      this.requestLayerChange(skeletonPath, 0.25F, mixConfig);
      this.setEnabled(false);
   }

   public final void startBurn(int durationMs) {
      this.lastTemplate = System.currentTimeMillis();
      this.effectStartTime = durationMs;
      if (!this.effectDuration) {
         this.effectDuration = true;
         String animName = "burn";
         this.playAnimation(animName, true, 1.0F);
         this.setEnabled(true);
         AudioManager.loopSound(this, "burn", this.projectile2.getCenterPosition());
      }
   }

   public final void stopBurn() {
      this.effectDuration = false;
      this.setEnabled(false);
      AudioManager.stopLoopedSound(this, "burn");
   }

   @Override
   public final void update(float delta) {
      if (this.effectDuration && GameUtils.hasElapsed(this.lastTemplate, this.effectStartTime)) {
         this.stopBurn();
      }
   }

}
