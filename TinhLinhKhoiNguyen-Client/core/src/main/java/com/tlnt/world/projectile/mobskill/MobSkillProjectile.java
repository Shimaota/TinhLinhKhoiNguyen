package com.tlnt.world.projectile.mobskill;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.projectile.mobskill.component.MobProjectileSprite;
import com.tlnt.world.projectile.mobskill.component.ProjectileMovementController;

public final class MobSkillProjectile extends GameObject {
   public static final Array<MobSkillProjectile> skeleton = new Array<>();
   public static final ObjectPool<MobSkillProjectile> POOL = new MobSkillProjectilePool();
   public GameObject owner;
   public MobProjectileSprite POOL2 = new MobProjectileSprite(this);
   public ProjectileMovementController target = new ProjectileMovementController(this);

   public static MobSkillProjectile create(float x, float y) {
      MobSkillProjectile projectile;
      (projectile = POOL.obtain()).teleportTo(x, y);
      return projectile;
   }

   public static void release(MobSkillProjectile projectile) {
      POOL.free(projectile);
   }

   public static void clearAll() {
      for (MobSkillProjectile projectile : skeleton) {
         POOL.free(projectile);
      }

      skeleton.clear();
   }

   public MobSkillProjectile(LoginScreen screen) {
      super(screen, EntityType.DART);
      this.setRenderOrder(6500000.0F);
   }

   @Override
   public final void onStart() {
      this.setDirection(Direction.LEFT);
   }

   public final MobSkillProjectile setTarget(GameObject target) {
      this.owner = target;
      return this;
   }

   public final GameObject getTarget() {
      return this.owner;
   }

   @Override
   public final void dispose() {
   }
}
