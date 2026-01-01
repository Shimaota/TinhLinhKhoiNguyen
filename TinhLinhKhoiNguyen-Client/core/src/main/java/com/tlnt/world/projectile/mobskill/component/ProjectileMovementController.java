package com.tlnt.world.projectile.mobskill.component;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.mobskill.MobSkillProjectile;

public final class ProjectileMovementController extends BaseComponent {
   public MobSkillProjectile projectile;
   public float speed = 20.0F;
   public boolean active;

   public ProjectileMovementController(MobSkillProjectile projectile) {
      super(projectile.getEntity());
      this.projectile = projectile;
   }

   @Override
   public final void onEnable() {
      this.active = true;
   }

   @Override
   public final void update(float delta) {
      if (this.projectile.getTarget() != null && this.active) {
         Vector2 dir;
         float dist = (dir = this.projectile.getTarget().getCenterPosition().cpy().sub(this.projectile.getPosition())).len();
         if (dir.x != 0.0F) {
            this.projectile.POOL2.setMixDuration(dir.angleDeg() + 180.0F);
         }

         if (dir.nor().scl(this.speed).scl(delta).len() > dist) {
            switch (this.projectile.getTarget().getEntityType()) {
               case PLAYER:
                  ((PlayerEntity)this.projectile.getTarget()).mobSkillEffect.playSkillAnimation();
                  break;
               case MOB:
                  ((MobEntity)this.projectile.getTarget()).skillEffect.playSkillAnimation();
            }

            if (!this.projectile.POOL2.getCurrentAnimationName().equals("explosion")) {
               this.active = false;
               String animName = "explosion";
               this.projectile.POOL2.playAnimation(animName, false, 1.0F);
               AudioManager.playSound("hit_4", this.projectile.getCenterPosition());
               return;
            }
         } else {
            this.projectile.getPosition().add(dir);
         }
      }
   }

}
