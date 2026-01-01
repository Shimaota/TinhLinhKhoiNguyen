package com.tlnt.world.effect;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public class TargetMarkerEffect extends SpineSkeletonComponent {
   public GameObject projectile2;

   public TargetMarkerEffect(WorldEntity owner) {
      super(owner.getEntity());
      this.projectile2 = owner;
      this.setRenderOrder(this.projectile2.getRenderOrder());
      String skeletonPath = "ui/target";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
      this.requestLayerChange(skeletonPath, 0.25F, mixConfig);
      this.setEnabled(false);
      this.addAnimationListener(new TargetMarkerEffectAnimationListener(this));
   }

   public final void showTargetMarker() {
      this.setSkeletonOffset(0.0F, this.projectile2.getHeight() / 2.0F);
      String animName = "target1";
      this.playAnimation(animName, false, 1.0F);
      this.setEnabled(true);
   }

}
