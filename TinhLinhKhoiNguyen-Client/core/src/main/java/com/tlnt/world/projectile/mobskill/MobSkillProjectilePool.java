package com.tlnt.world.projectile.mobskill;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.core.GameMain;

public final class MobSkillProjectilePool extends ObjectPool<MobSkillProjectile> {

   @Override
   public MobSkillProjectile newObject() {
      return new com.tlnt.world.projectile.mobskill.MobSkillProjectile(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final MobSkillProjectile obtain() {
      MobSkillProjectile projectile;
      MobSkillProjectile obtained = projectile = (MobSkillProjectile)super.obtain();
      obtained.setActive(true);
      MobSkillProjectile.skeleton.add(projectile);
      return projectile;
   }

   @Override
   public final void free(MobSkillProjectile obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
