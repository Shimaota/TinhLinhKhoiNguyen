package com.tlnt.world.projectile.bow.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.world.projectile.bow.BowArrowProjectile;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class BowArrowSkeletonComponent extends SpineSkeletonComponent {
   public BowArrowProjectile projectile2;

   public BowArrowSkeletonComponent(BowArrowProjectile projectile) {
      super(projectile.getEntity());
      this.projectile2 = projectile;
      this.setRenderOrder(6500000.0F);
      this.requestLayerChange("skeletons/vfxs/bow", 0.25F, new SkeletonLoader.AnimationMixConfig(0.0F));
   }

}
