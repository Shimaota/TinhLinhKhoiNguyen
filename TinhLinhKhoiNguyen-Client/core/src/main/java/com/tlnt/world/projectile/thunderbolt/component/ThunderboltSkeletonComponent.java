package com.tlnt.world.projectile.thunderbolt.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.world.projectile.thunderbolt.ThunderboltProjectile;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class ThunderboltSkeletonComponent extends SpineSkeletonComponent {
   public ThunderboltProjectile projectile2;

   public ThunderboltSkeletonComponent(ThunderboltProjectile projectile) {
      super(projectile.getEntity());
      this.projectile2 = projectile;
      this.setRenderOrder(6500000.0F);
      this.requestLayerChange("skeletons/vfxs/thunderbolt", 0.35F, new SkeletonLoader.AnimationMixConfig(0.0F));
      this.addAnimationListener(new ThunderboltAnimationListener(this));
   }

}
