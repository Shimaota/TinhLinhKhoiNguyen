package com.tlnt.world.effect;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class MobSkillEffect extends SpineSkeletonComponent {
   public GameObject projectile2;

   public MobSkillEffect(WorldEntity owner) {
      super(owner.getEntity());
      this.projectile2 = owner;
      this.setRenderOrder(this.projectile2.getRenderOrder());
      this.requestLayerChange("skeletons/vfxs/mob", 0.25F, new SkeletonLoader.AnimationMixConfig(0.2F));
      this.setEnabled(false);
      this.addAnimationListener(new MobSkillEffectAnimationListener(this));
   }

   public final void playSkillAnimation() {
      if (!this.isEnabled()) {
         this.setSkeletonOffset(0.0F, this.projectile2.getHeight() / 2.0F);
         this.playAnimation("skill1", false, 1.0F);
         this.setEnabled(true);
      }
   }

}
