package com.tlnt.world.effect;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class ReviveEffect extends SpineSkeletonComponent {
   public GameObject projectile2;

   public ReviveEffect(PlayerEntity owner) {
      super(owner.getEntity());
      this.projectile2 = owner;
      this.setRenderOrder(this.projectile2.getRenderOrder());
      String skeletonPath = "skeletons/vfxs/revive";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
      this.requestLayerChange(skeletonPath, 0.25F, mixConfig);
      this.setEnabled(false);
      this.addAnimationListener(new ReviveEffectAnimationListener(this));
   }

}
