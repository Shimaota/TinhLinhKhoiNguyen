package com.tlnt.world.projectile.feather.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.world.projectile.feather.FeatherProjectileEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class FeatherSkeletonComponent extends SpineSkeletonComponent {
   public FeatherProjectileEntity projectile2;

   public FeatherSkeletonComponent(FeatherProjectileEntity projectile) {
      super(projectile.getEntity());
      this.projectile2 = projectile;
      this.setRenderOrder(6500000.0F);
      this.requestLayerChange("skeletons/vfxs/feathers", 0.35F, new SkeletonLoader.AnimationMixConfig(0.0F));
      this.addAnimationListener(new FeatherAnimationCompleteListener(this));
   }

}
