package com.tlnt.world.projectile.axe.component;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.axe.AxeProjectileEntity;

public final class AxeProjectileHitComponent extends BaseComponent {
   public AxeProjectileEntity projectile;
   public float speed = 25.0F;

   public AxeProjectileHitComponent(AxeProjectileEntity projectile) {
      super(projectile.getEntity());
      this.projectile = projectile;
   }

   @Override
   public final void update(float delta) {
      if (this.projectile.getTarget() != null) {
         Vector2 dir;
         float dist = (dir = this.projectile.getTarget().getCenterPosition().cpy().sub(this.projectile.getPosition())).len();
         if (dir.nor().scl(this.speed).scl(delta).len() > dist) {
            if (this.projectile.getTarget().getEntityType() == EntityType.PLAYER) {
               PlayerEntity player = (PlayerEntity)this.projectile.getTarget();
               PooledFloatingEffect playerEffect;
               PooledFloatingEffect playerObtained = playerEffect = PooledFloatingEffect.POOL.obtain().withDirection(this.projectile.getOwner().getDirection());
               String[] playerSkins = new String[1];
               playerSkins[0] = this.projectile.getOwner().POOL2.weaponTexture;
               playerObtained.tryApplySkins(playerSkins);
               playerEffect.tryPlayAnimation("hit1", false, 1.0F);
               player.floatingEffectManager.addEffect(playerEffect);
            } else if (this.projectile.getTarget().getEntityType() == EntityType.MOB) {
               MobEntity mob = (MobEntity)this.projectile.getTarget();
               PooledFloatingEffect mobEffect;
               PooledFloatingEffect mobObtained = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(this.projectile.getOwner().getDirection());
               String[] mobSkins = new String[1];
               mobSkins[0] = this.projectile.getOwner().POOL2.weaponTexture;
               mobObtained.tryApplySkins(mobSkins);
               String animName = "hit1";
               mobEffect.tryPlayAnimation(animName, false, 1.0F);
               mob.floatingEffectManager.addEffect(mobEffect);
            }

            AudioManager.playSound("axe_skill_4_hit", this.projectile.getCenterPosition());
            AxeProjectileEntity.release(this.projectile);
         } else {
            this.projectile.getPosition().add(dir);
         }
      }
   }

}
