package com.tlnt.world.effect;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class MobSpawnEffect extends SpineSkeletonComponent {
   public GameObject projectile2;

   public MobSpawnEffect(MobEntity owner) {
      super(owner.getEntity());
      this.projectile2 = owner;
      this.setRenderOrder(this.projectile2.getRenderOrder());
      String skeletonPath = "skeletons/vfxs/spawn_mob";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
      this.requestLayerChange(skeletonPath, 0.25F, mixConfig);
      this.setEnabled(false);
      this.addAnimationListener(new MobSpawnEffectAnimationListener(this));
   }

   public final void playSpawnAnimation() {
      this.setSkeletonOffset(0.0F, this.projectile2.getHeight() / 2.0F);
      String animName = "spawn_mob";
      this.playAnimation(animName, false, 1.0F);
      this.setEnabled(true);
   }

}
