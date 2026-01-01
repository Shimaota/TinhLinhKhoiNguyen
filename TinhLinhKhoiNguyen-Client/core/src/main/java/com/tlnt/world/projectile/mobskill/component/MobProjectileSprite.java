package com.tlnt.world.projectile.mobskill.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.world.projectile.mobskill.MobSkillProjectile;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class MobProjectileSprite extends SpineSkeletonComponent {
   public MobSkillProjectile projectile2;

   public MobProjectileSprite(MobSkillProjectile projectile) {
      super(projectile.getEntity());
      this.projectile2 = projectile;
      this.setRenderOrder(6500000.0F);
      String skeletonPath = "skeletons/vfxs/mob";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      this.requestLayerChange(skeletonPath, 0.35F, mixConfig);
      this.addAnimationListener(new ProjectileExplosionListener(this));
   }

}
