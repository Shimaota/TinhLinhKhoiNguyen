package com.tlnt.world.anim.player.attack.boss;

import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.player.PlayerEntity;

/**
 * Skill 43 {@code stick/skill11} of the staff boss (Cổ thần Dạ xoa) — port of decompiled {@code C3976x2b622830}: on enter plays the
 * animation + sound {@code boss_skill_special}; when the animation finishes (progress ≥ 1) or an animation event fires, back to IDLE; on exit
 * turns off the weapon effect ({@code C3553.m16539} = {@code WeaponEquipEffect.setEnabled(false)}). The server packet op 21 case 43
 * has no payload ({@code ServerPacketHandler}).
 */
public final class BossSkill11Anim extends AnimationBehavior<PlayerEntity> {
   public static final BossSkill11Anim INSTANCE = new BossSkill11Anim();

   @Override
   public final void onExit(PlayerEntity player) {
      player.target2.setEnabled(false);
   }

   @Override
   public final void onUpdate(PlayerEntity player, float delta) {
      if (player.movementComponent.getAnimationProgress(0) >= 1.0F) {
         player.setAnimationState(AnimationState.IDLE);
      }
   }

   @Override
   public final void onEnter(PlayerEntity player) {
      player.movementComponent.playAnimation("stick/skill11", false, 1.0F);
      AudioManager.playSound("boss_skill_special", player.getCenterPosition());
   }

   @Override
   public final void onAnimationEvent(PlayerEntity player, String eventName) {
      player.setAnimationState(AnimationState.IDLE);
   }
}
