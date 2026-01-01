package com.tlnt.world.projectile.slingshot.component;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.slingshot.SlingshotProjectile;

public final class SlingshotMovementComponent extends BaseComponent {
   public SlingshotProjectile projectile;
   public float speed = 25.0F;

   public SlingshotMovementComponent(SlingshotProjectile projectile) {
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
               PooledFloatingEffect playerObtained = playerEffect = PooledFloatingEffect.POOL.obtain().withDirection(this.projectile.getShooter().getDirection());
               String[] playerSkins = new String[1];
               playerSkins[0] = this.projectile.getShooter().POOL2.weaponTexture;
               playerObtained.tryApplySkins(playerSkins);
               String playerAnimName = this.projectile.getDamageText();
               playerEffect.tryPlayAnimation(playerAnimName, false, 1.0F);
               player.floatingEffectManager.addEffect(playerEffect);
            } else if (this.projectile.getTarget().getEntityType() == EntityType.MOB) {
               MobEntity mob = (MobEntity)this.projectile.getTarget();
               PooledFloatingEffect mobEffect;
               PooledFloatingEffect mobObtained = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(this.projectile.getShooter().getDirection());
               String[] mobSkins = new String[1];
               mobSkins[0] = this.projectile.getShooter().POOL2.weaponTexture;
               mobObtained.tryApplySkins(mobSkins);
               mobEffect.tryPlayAnimation(this.projectile.getDamageText(), false, 1.0F);
               mob.floatingEffectManager.addEffect(mobEffect);
            }

            if (this.projectile.isScreenShake()) {
               GameMain.instance.getLoginScreen().box2dDebugRenderer.startShake(0.5F, 0.05F);
            }

            String animName = this.projectile.POOL2.getCurrentAnimationName();
            int animIndex = -1;
            switch (animName.hashCode()) {
               case -925678121:
                  if (animName.equals("rock_1")) {
                     animIndex = 0;
                  }
                  break;
               case -925678120:
                  if (animName.equals("rock_2")) {
                     animIndex = 1;
                  }
                  break;
               case -925678119:
                  if (animName.equals("rock_3")) {
                     animIndex = 2;
                  }
            }

            switch (animIndex) {
               case 0:
                  AudioManager.playSound("hit_1", this.projectile.getCenterPosition());
                  break;
               case 1:
                  AudioManager.playSound("hit_2", this.projectile.getCenterPosition());
                  break;
               case 2:
                  AudioManager.playSound("skill5_type2_hit", this.projectile.getCenterPosition());
            }

            SlingshotProjectile.recycle(this.projectile);
         } else {
            this.projectile.getPosition().add(dir);
         }
      }
   }

}
