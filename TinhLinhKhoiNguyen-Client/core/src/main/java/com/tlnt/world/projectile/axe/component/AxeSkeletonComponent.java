package com.tlnt.world.projectile.axe.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.world.projectile.axe.AxeProjectileEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class AxeSkeletonComponent extends SpineSkeletonComponent {
   public AxeProjectileEntity projectile2;

   public AxeSkeletonComponent(AxeProjectileEntity projectile) {
      super(projectile.getEntity());
      this.projectile2 = projectile;
      this.setRenderOrder(6500000.0F);
      String skeletonPath = "skeletons/vfxs/axe";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      this.requestLayerChange(skeletonPath, 0.25F, mixConfig);
   }

}
