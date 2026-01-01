package com.tlnt.world.projectile.mobskill.component;

import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.world.projectile.mobskill.MobSkillProjectile;

public final class ProjectileExplosionListener extends AnimationState.AnimationStateAdapter {
   public com.tlnt.world.projectile.mobskill.component.MobProjectileSprite spriteComponent;

   public ProjectileExplosionListener(MobProjectileSprite spriteComponent) {
      super();
      this.spriteComponent = spriteComponent;
   }

   @Override
   public final void complete(AnimationState.TrackEntry entry) {
      String animName = entry.getAnimation().getName();
      if (animName.equals("explosion")) {
         MobSkillProjectile.release(this.spriteComponent.projectile2);
      }
   }

}
