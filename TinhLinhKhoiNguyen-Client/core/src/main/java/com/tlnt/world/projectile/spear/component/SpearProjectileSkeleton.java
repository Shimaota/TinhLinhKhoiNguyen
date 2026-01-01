package com.tlnt.world.projectile.spear.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.world.projectile.spear.SpearProjectileEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class SpearProjectileSkeleton extends SpineSkeletonComponent {
   public SpearProjectileEntity projectile2;

   public SpearProjectileSkeleton(SpearProjectileEntity projectile) {
      super(projectile.getEntity());
      this.projectile2 = projectile;
      this.setRenderOrder(6500000.0F);
      String skeletonPath = "skeletons/vfxs/spear";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      this.requestLayerChange(skeletonPath, 0.25F, mixConfig);
   }

}
