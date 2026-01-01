package com.tlnt.world.effect;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.physics.Entity;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.skeleton.SimpleSpineSkeletonComponent;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class WeakenEffect extends SpineSkeletonComponent {
   public GameObject lastTemplate;
   public long effectStartTime;
   public int effectDuration;
   public boolean projectile2;

   public WeakenEffect(WorldEntity owner) {
      super(owner.getEntity());
      this.lastTemplate = owner;
      this.setRenderOrder(this.lastTemplate.getRenderOrder());
      String skeletonPath = "skeletons/vfxs/weaken";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
      this.requestLayerChange(skeletonPath, 0.25F, mixConfig);
      this.setEnabled(false);
   }

   public final void startWeaken(int durationMs) {
      this.effectStartTime = System.currentTimeMillis();
      this.effectDuration = durationMs;
      if (!this.projectile2) {
         this.projectile2 = true;
         this.playAnimation("weaken", true, 1.0F);
         this.setEnabled(true);
         Entity ownerEntity = this.lastTemplate.getEntity();
         ownerEntity.findComponent(SimpleSpineSkeletonComponent.class, true).setTintColor(Color.PURPLE);
      }
   }

   public final void stopWeaken() {
      this.projectile2 = false;
      this.setEnabled(false);
      this.lastTemplate.getEntity().findComponent(SimpleSpineSkeletonComponent.class, true).setTintColor(Color.WHITE);
   }

   @Override
   public final void update(float delta) {
      if (this.projectile2 && GameUtils.hasElapsed(this.effectStartTime, this.effectDuration)) {
         this.stopWeaken();
      }
   }

}
