package com.tlnt.world.projectile.slingshot.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.world.projectile.slingshot.SlingshotProjectile;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class SlingshotSkeletonComponent extends SpineSkeletonComponent {
   public SlingshotProjectile projectile2;

   public SlingshotSkeletonComponent(SlingshotProjectile projectile) {
      super(projectile.getEntity());
      this.projectile2 = projectile;
      this.setRenderOrder(6500000.0F);
      String skeletonPath = "skeletons/vfxs/slingshot";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      this.requestLayerChange(skeletonPath, 0.25F, mixConfig);
   }

}
