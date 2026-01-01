package com.tlnt.world.effect;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.Entity;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.skeleton.SimpleSpineSkeletonComponent;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class IceSpikeEffect extends SpineSkeletonComponent {
   public GameObject lastTemplate;
   public long effectStartTime;
   public int effectDuration;
   public boolean projectile2;

   public IceSpikeEffect(WorldEntity owner) {
      super(owner.getEntity());
      this.lastTemplate = owner;
      this.setRenderOrder(this.lastTemplate.getRenderOrder());
      String skeletonPath = "skeletons/vfxs/ice_spike";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
      this.requestLayerChange(skeletonPath, 0.25F, mixConfig);
      this.setEnabled(false);
      this.addAnimationListener(new IceSpikeEffectAnimationListener(this));
   }

   public final void startIceSpike(int durationMs) {
      this.effectStartTime = System.currentTimeMillis();
      this.effectDuration = durationMs;
      if (!this.projectile2) {
         this.projectile2 = true;
         this.playAnimation("ice_spike_1", false, 1.0F);
         this.setEnabled(true);
         Entity ownerEntity = this.lastTemplate.getEntity();
         ownerEntity.findComponent(SimpleSpineSkeletonComponent.class, true).setTimeScale(0.0F);
         ownerEntity = this.lastTemplate.getEntity();
         ownerEntity.findComponent(SimpleSpineSkeletonComponent.class, true).setTintColor(Color.SKY);
         AudioManager.playSound("ice_spike", this.lastTemplate.getCenterPosition());
      }
   }

   public final void stopIceSpike() {
      this.projectile2 = false;
      Entity ownerEntity = this.lastTemplate.getEntity();
      ownerEntity.findComponent(SimpleSpineSkeletonComponent.class, true).setTimeScale(1.0F);
      this.lastTemplate.getEntity().findComponent(SimpleSpineSkeletonComponent.class, true).setTintColor(Color.WHITE);
      this.setEnabled(false);
   }

   @Override
   public final void update(float delta) {
      if (this.projectile2 && GameUtils.hasElapsed(this.effectStartTime, this.effectDuration)) {
         this.projectile2 = false;
         String animName = "ice_spike_3";
         this.playAnimation(animName, false, 1.0F);
      }
   }

}
