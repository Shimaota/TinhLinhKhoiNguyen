package com.tlnt.world.effect;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class RootEffect extends SpineSkeletonComponent {
   public GameObject lastTemplate;
   public long effectStartTime;
   public int effectDuration;
   public boolean projectile2;

   public RootEffect(WorldEntity owner) {
      super(owner.getEntity());
      this.lastTemplate = owner;
      this.setRenderOrder(this.lastTemplate.getRenderOrder());
      String skeletonPath = "skeletons/vfxs/root_skill";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
      this.requestLayerChange(skeletonPath, 0.5F, mixConfig);
      this.setEnabled(false);
      this.addAnimationListener(new RootEffectAnimationListener(this));
   }

   public final void startRoot(int durationMs) {
      this.effectStartTime = System.currentTimeMillis();
      this.effectDuration = durationMs;
      if (!this.projectile2) {
         this.projectile2 = true;
         String animName = "tied_1";
         this.playAnimation(animName, false, 1.0F);
         this.setEnabled(true);
         AudioManager.playSound("plant_growing", this.lastTemplate.getCenterPosition());
      }
   }

   public final void stopRoot() {
      this.projectile2 = false;
      this.setEnabled(false);
   }

   @Override
   public final void update(float delta) {
      if (this.projectile2 && GameUtils.hasElapsed(this.effectStartTime, this.effectDuration)) {
         this.projectile2 = false;
         String animName = "tied_3";
         this.playAnimation(animName, false, 1.0F);
      }
   }

}
