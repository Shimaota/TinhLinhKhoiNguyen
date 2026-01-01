package com.tlnt.world.projectile.spear.component;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.spear.SpearProjectileEntity;

public final class SpearProjectileMovementComponent extends BaseComponent {
   public SpearProjectileEntity projectile;
   public float speed = 25.0F;

   public SpearProjectileMovementComponent(SpearProjectileEntity projectile) {
      super(projectile.getEntity());
      this.projectile = projectile;
   }

   @Override
   public final void update(float delta) {
      if (this.projectile.getTarget() != null) {
         Vector2 dir;
         float dist = (dir = this.projectile.getTarget().getCenterPosition().cpy().sub(this.projectile.getPosition())).len();
         if (dir.x != 0.0F) {
            this.projectile.POOL2.setMixDuration(dir.angleDeg() + 180.0F);
         }

         if (dir.nor().scl(this.speed).scl(delta).len() > dist) {
            if (this.projectile.getTarget().getEntityType() == EntityType.PLAYER) {
               PlayerEntity player = (PlayerEntity)this.projectile.getTarget();
               PooledFloatingEffect playerEffect;
               PooledFloatingEffect playerObtained = playerEffect = PooledFloatingEffect.POOL.obtain().withDirection(this.projectile.getOwner().getDirection());
               String[] playerSkins = new String[1];
               playerSkins[0] = this.projectile.getOwner().POOL2.weaponTexture;
               playerObtained.tryApplySkins(playerSkins);
               String playerAnimName = "hit5";
               playerEffect.tryPlayAnimation(playerAnimName, false, 1.0F);
               player.floatingEffectManager.addEffect(playerEffect);
            } else if (this.projectile.getTarget().getEntityType() == EntityType.MOB) {
               MobEntity mob = (MobEntity)this.projectile.getTarget();
               PooledFloatingEffect mobEffect;
               PooledFloatingEffect mobObtained = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(this.projectile.getOwner().getDirection());
               String[] mobSkins = new String[1];
               mobSkins[0] = this.projectile.getOwner().POOL2.weaponTexture;
               mobObtained.tryApplySkins(mobSkins);
               mobEffect.tryPlayAnimation("hit5", false, 1.0F);
               mob.floatingEffectManager.addEffect(mobEffect);
            }

            GameMain.instance.getLoginScreen().box2dDebugRenderer.startShake(0.5F, 0.05F);
            AudioManager.playSound("skill5_type2_hit", this.projectile.getCenterPosition());
            SpearProjectileEntity.release(this.projectile);
         } else {
            this.projectile.getPosition().add(dir);
         }
      }
   }

}
