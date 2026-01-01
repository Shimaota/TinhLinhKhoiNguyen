package com.tlnt.world.anim.player.attack.axe;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.mob.MobEntity;

public final class AxeSkill3Anim extends AnimationBehavior<PlayerEntity> {
   public static final AxeSkill3Anim INSTANCE = new AxeSkill3Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity entity, String eventName) {
      PlayerEntity player = (PlayerEntity)entity;
      if (eventName.equals("axe/skill3")) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onExit(PlayerEntity entity) {
      PlayerEntity player;
      (player = (PlayerEntity)entity).target2.setEnabled(false);
   }

   @Override
   public final void onEnter(PlayerEntity entity) {
      String animName = "axe/skill3";
      PlayerEntity player;
      (player = (PlayerEntity)entity).movementComponent.playAnimation(animName, false, 1.3F);
      player.target2.equipAxe();
      animName = "skill3";
      player.target2.playAnimation(animName, false, 1.3F);
      if (!player.skeleton.aoeHitTargets.isEmpty()) {
         ArrayIterator iter = player.skeleton.aoeHitTargets.iterator();

         while (iter.hasNext()) {
            WorldEntity target;
            if ((target = (WorldEntity)iter.next()).getEntityType() == EntityType.PLAYER) {
               PlayerEntity targetPlayer = (PlayerEntity)target;
               PooledFloatingEffect effect;
               PooledFloatingEffect effectRef = effect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
               String[] skins = new String[1];
               skins[0] = player.POOL2.weaponTexture;
               effectRef.tryApplySkins(skins);
               effect.tryPlayAnimation("hit4", false, 1.0F);
               targetPlayer.floatingEffectManager.addEffect(effect);
            } else if (target.getEntityType() == EntityType.MOB) {
               MobEntity targetMob = (MobEntity)target;
               PooledFloatingEffect mobEffect;
               PooledFloatingEffect mobEffectRef = mobEffect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
               String[] mobSkins = new String[1];
               mobSkins[0] = player.POOL2.weaponTexture;
               mobEffectRef.tryApplySkins(mobSkins);
               mobEffect.tryPlayAnimation("hit4", false, 1.0F);
               targetMob.floatingEffectManager.addEffect(mobEffect);
            }
         }

         player.skeleton.aoeHitTargets.clear();
      }

      AudioManager.playSound("axe_skill_3", player.getCenterPosition());
   }
}
