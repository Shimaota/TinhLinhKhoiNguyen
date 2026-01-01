package com.tlnt.world.anim.mob;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.engine.scene.GameObject;

public final class MobStunAnim extends AnimationBehavior<MobEntity> {
   public static final MobStunAnim INSTANCE = new MobStunAnim();

   @Override
   public final void onEnter(MobEntity entity) {
      String animName = "stun";
      MobEntity mob;
      (mob = (MobEntity)entity).POOL.playAnimation(animName, true, 1.0F);
   }
}
