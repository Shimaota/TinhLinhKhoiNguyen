package com.tlnt.core.factory;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.mob.MobEntity;

public final class MobFactory {
   public static final ObjectPool<MobEntity> MOB_POOL = new MobPool();

   public static MobEntity obtainMob(int id, float x, float y) {
      MobEntity mob;
      (mob = MOB_POOL.obtain()).setId(id);
      mob.setAnimationState(AnimationState.MOB_IDLE);
      mob.POOL2.spawnPosition.set(x, y);
      mob.teleportTo(x, y);
      if (mob.wanderAI != null) {
         mob.wanderAI.projectile.set(-1.0F, -1.0F);
         mob.wanderAI.startWaitPause();
      }

      return mob;
   }

   public static void freeMob(MobEntity mob) {
      MOB_POOL.free(mob);
   }
}
