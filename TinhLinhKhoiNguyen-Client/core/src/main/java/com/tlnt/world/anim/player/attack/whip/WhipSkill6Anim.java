package com.tlnt.world.anim.player.attack.whip;

import com.badlogic.gdx.utils.Array.ArrayIterator;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.player.PlayerEntity;

/**
 * Skill 42 {@code whip/skill6} (Whip) — port of decompiled {@code C0157}: on enter plays "whip/skill6" ×1.3 + the whip weapon effect
 * "skill6"; every target hit (mob / player, packet op 21 case 42) shows a "hit4" slash and sound {@code whip_skill_6}; when the
 * animation ends, back to IDLE; on exit turns off the weapon effect.
 */
public final class WhipSkill6Anim extends AnimationBehavior<PlayerEntity> {
   public static final WhipSkill6Anim INSTANCE = new WhipSkill6Anim();

   @Override
   public final void onAnimationEvent(PlayerEntity player, String eventName) {
      if (eventName.equals("whip/skill6")) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onUpdate(PlayerEntity player, float delta) {
      // the animation-end event is not fired for players in this port -> return to IDLE when it finishes
      if (player.movementComponent.getAnimationProgress(0) >= 1.0F) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onExit(PlayerEntity player) {
      player.target2.setEnabled(false);
   }

   @Override
   public final void onEnter(PlayerEntity player) {
      player.movementComponent.playAnimation("whip/skill6", false, 1.3F);
      player.target2.equipWhip();
      player.target2.playAnimation("skill6", false, 1.3F);
      if (!player.skeleton.aoeHitTargets.isEmpty()) {
         ArrayIterator iter = player.skeleton.aoeHitTargets.iterator();

         while (iter.hasNext()) {
            WorldEntity target = (WorldEntity)iter.next();
            if (target == null) {
               continue;
            }

            PooledFloatingEffect effect = PooledFloatingEffect.POOL.obtain().withDirection(player.getDirection());
            effect.tryApplySkins(player.POOL2.weaponTexture);
            effect.tryPlayAnimation("hit4", false, 1.0F);
            if (target.getEntityType() == EntityType.PLAYER) {
               ((PlayerEntity)target).floatingEffectManager.addEffect(effect);
            } else if (target.getEntityType() == EntityType.MOB) {
               ((MobEntity)target).floatingEffectManager.addEffect(effect);
            } else {
               PooledFloatingEffect.POOL.free(effect);
            }
         }

         player.skeleton.aoeHitTargets.clear();
      }

      AudioManager.playSound("whip_skill_6", player.getCenterPosition());
   }
}
