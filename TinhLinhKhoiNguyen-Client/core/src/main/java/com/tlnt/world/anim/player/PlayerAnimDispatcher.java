package com.tlnt.world.anim.player;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.anim.player.attack.NormalAttackAnim;
import com.tlnt.world.anim.player.attack.axe.AxeAttackAnim;
import com.tlnt.world.anim.player.attack.axe.AxeSkill1Anim;
import com.tlnt.world.anim.player.attack.axe.AxeSkill2Anim;
import com.tlnt.world.anim.player.attack.axe.AxeSkill2Variant3Anim;
import com.tlnt.world.anim.player.attack.axe.AxeSkill3Anim;
import com.tlnt.world.anim.player.attack.axe.AxeSkill4Anim;
import com.tlnt.world.anim.player.attack.axe.AxeSkill5Anim;
import com.tlnt.world.anim.player.attack.axe.AxeSkill5Variant3Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill10Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill11Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill1Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill2Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill3Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill4Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill5Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill6Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill7Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill8Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill9Anim;
import com.tlnt.world.anim.player.attack.boss.BossSkill9Variant3Anim;
import com.tlnt.world.anim.player.attack.bow.BowAttackAnim;
import com.tlnt.world.anim.player.attack.bow.BowSkill1Anim;
import com.tlnt.world.anim.player.attack.bow.BowSkill2Anim;
import com.tlnt.world.anim.player.attack.bow.BowSkill3Anim;
import com.tlnt.world.anim.player.attack.bow.BowSkill4Anim;
import com.tlnt.world.anim.player.attack.bow.BowSkill5Anim;
import com.tlnt.world.anim.player.attack.bow.BowSkill5Variant3Anim;
import com.tlnt.world.anim.player.attack.slingshot.SlingshotAttackAnim;
import com.tlnt.world.anim.player.attack.slingshot.SlingshotSkill1Anim;
import com.tlnt.world.anim.player.attack.slingshot.SlingshotSkill2Anim;
import com.tlnt.world.anim.player.attack.slingshot.SlingshotSkill3Anim;
import com.tlnt.world.anim.player.attack.slingshot.SlingshotSkill4Anim;
import com.tlnt.world.anim.player.attack.slingshot.SlingshotSkill5Anim;
import com.tlnt.world.anim.player.attack.slingshot.SlingshotSkill5Variant3Anim;
import com.tlnt.world.anim.player.attack.spear.SpearAttackAnim;
import com.tlnt.world.anim.player.attack.spear.SpearSkill1Anim;
import com.tlnt.world.anim.player.attack.spear.SpearSkill2Anim;
import com.tlnt.world.anim.player.attack.spear.SpearSkill3Anim;
import com.tlnt.world.anim.player.attack.spear.SpearSkill4Anim;
import com.tlnt.world.anim.player.attack.spear.SpearSkill5Anim;
import com.tlnt.world.anim.player.attack.spear.SpearSkill5Variant3Anim;
import com.tlnt.world.anim.player.attack.whip.WhipAttackAnim;
import com.tlnt.world.anim.player.attack.whip.WhipSkill1Anim;
import com.tlnt.world.anim.player.attack.whip.WhipSkill2Anim;
import com.tlnt.world.anim.player.attack.whip.WhipSkill3Anim;
import com.tlnt.world.anim.player.attack.whip.WhipSkill4Anim;
import com.tlnt.world.anim.player.attack.whip.WhipSkill5Anim;
import com.tlnt.world.anim.player.attack.whip.WhipSkill5Variant3Anim;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;

public final class PlayerAnimDispatcher extends AnimationStateDispatcher<PlayerEntity> {
   /** A one-shot attack animation has finished but the state is still attacking after this many seconds -> go back to IDLE automatically. */
   private static final float STUCK_ATTACK_SECONDS = 0.35F;
   private float completeFor;

   public PlayerAnimDispatcher(PlayerEntity entity) {
      super(entity);
   }

   /**
    * Safety net: an attack state (not a *_START charge / loop waiting for the end packet) whose NON-looping body animation has finished
    * -> the complete event should have returned it to IDLE; if that was missed (skeleton changed mid-way...) go to IDLE so the next attack
    * packet replays the animation instead of standing still.
    */
   @Override
   protected void afterUpdate(float delta) {
      AnimationState state = this.projectile.getAnimationState();
      if (!isOneShotAttack(state)) {
         this.completeFor = 0.0F;
         return;
      }
      com.tlnt.thirdparty.spine.AnimationState spine = this.projectile.movementComponent == null ? null
            : this.projectile.movementComponent.active;
      com.tlnt.thirdparty.spine.AnimationState.TrackEntry entry = spine == null ? null : spine.getCurrent(0);
      if (entry == null || entry.loop || entry.next != null
            || entry.trackTime < entry.animationEnd - entry.animationStart) {
         this.completeFor = 0.0F;
         return;
      }
      this.completeFor += delta;
      if (this.completeFor >= STUCK_ATTACK_SECONDS) {
         this.completeFor = 0.0F;
         this.projectile.setAnimationState(AnimationState.IDLE);
      }
   }

   static boolean isOneShotAttack(AnimationState state) {
      return state.ordinal() >= AnimationState.MELEE_NORMAL_ATTACK.ordinal()
            && state.ordinal() <= AnimationState.STICK_SKILL_11.ordinal()
            && !state.name().endsWith("_START");
   }

   @Override
   public final AnimationBehavior<PlayerEntity> getAnimationBehavior(AnimationState state) {
      switch (state) {
         case IDLE:
            return PlayerIdleAnim.INSTANCE;
         case MOVE:
            return PlayerMoveAnim.INSTANCE;
         case JUMP:
            return PlayerJumpAnim.INSTANCE;
         case FALL:
            return PlayerFallAnim.INSTANCE;
         case HURT:
            return PlayerHitAnim.INSTANCE;
         case STUN:
            return PlayerStunAnim.INSTANCE;
         case DIE:
            return PlayerDieAnim.INSTANCE;
         case PICK:
            return PickAnim.INSTANCE;
         case CAST_LINE_FISHING:
            return FishingStartAnim.INSTANCE;
         case WAITING_FOR_BITE_FISHING:
            return FishingReelAnim.INSTANCE;
         case SET_HOOK_FISHING:
            return FishingEndAnim.INSTANCE;
         case MELEE_NORMAL_ATTACK:
            return NormalAttackAnim.INSTANCE;
         case AXE_NORMAL_ATTACK:
            return AxeAttackAnim.INSTANCE;
         case AXE_SKILL_1:
            return AxeSkill1Anim.INSTANCE;
         case AXE_SKILL_2_START:
            return AxeSkill2Anim.INSTANCE;
         case AXE_SKILL_2_END:
            return AxeSkill2Variant3Anim.INSTANCE;
         case AXE_SKILL_3:
            return AxeSkill3Anim.INSTANCE;
         case AXE_SKILL_4:
            return AxeSkill4Anim.INSTANCE;
         case AXE_SKILL_5_START:
            return AxeSkill5Anim.INSTANCE;
         case AXE_SKILL_5_END:
            return AxeSkill5Variant3Anim.INSTANCE;
         case BOW_NORMAL_ATTACK:
            return BowAttackAnim.INSTANCE;
         case BOW_SKILL_1:
            return BowSkill1Anim.INSTANCE;
         case BOW_SKILL_2:
            return BowSkill2Anim.INSTANCE;
         case BOW_SKILL_3:
            return BowSkill3Anim.INSTANCE;
         case BOW_SKILL_4:
            return BowSkill4Anim.INSTANCE;
         case BOW_SKILL_5_START:
            return BowSkill5Anim.INSTANCE;
         case BOW_SKILL_5_END:
            return BowSkill5Variant3Anim.INSTANCE;
         case SLINGSHOT_NORMAL_ATTACK:
            return SlingshotAttackAnim.INSTANCE;
         case SLINGSHOT_SKILL_1:
            return SlingshotSkill1Anim.INSTANCE;
         case SLINGSHOT_SKILL_2:
            return SlingshotSkill2Anim.INSTANCE;
         case SLINGSHOT_SKILL_3:
            return SlingshotSkill3Anim.INSTANCE;
         case SLINGSHOT_SKILL_4:
            return SlingshotSkill4Anim.INSTANCE;
         case SLINGSHOT_SKILL_5_START:
            return SlingshotSkill5Anim.INSTANCE;
         case SLINGSHOT_SKILL_5_END:
            return SlingshotSkill5Variant3Anim.INSTANCE;
         case SPEAR_NORMAL_ATTACK:
            return SpearAttackAnim.INSTANCE;
         case SPEAR_SKILL_1:
            return SpearSkill1Anim.INSTANCE;
         case SPEAR_SKILL_2:
            return SpearSkill2Anim.INSTANCE;
         case SPEAR_SKILL_3:
            return SpearSkill3Anim.INSTANCE;
         case SPEAR_SKILL_4:
            return SpearSkill4Anim.INSTANCE;
         case SPEAR_SKILL_5_START:
            return SpearSkill5Anim.INSTANCE;
         case SPEAR_SKILL_5_END:
            return SpearSkill5Variant3Anim.INSTANCE;
         case WHIP_NORMAL_ATTACK:
            return WhipAttackAnim.INSTANCE;
         case WHIP_SKILL_1:
            return WhipSkill1Anim.INSTANCE;
         case WHIP_SKILL_2:
            return WhipSkill2Anim.INSTANCE;
         case WHIP_SKILL_3:
            return WhipSkill3Anim.INSTANCE;
         case WHIP_SKILL_4:
            return WhipSkill4Anim.INSTANCE;
         case WHIP_SKILL_5_START:
            return WhipSkill5Anim.INSTANCE;
         case WHIP_SKILL_5_END:
            return WhipSkill5Variant3Anim.INSTANCE;
         case WHIP_SKILL_6:
            return com.tlnt.world.anim.player.attack.whip.WhipSkill6Anim.INSTANCE;
         case STICK_SKILL_1:
            return BossSkill1Anim.INSTANCE;
         case STICK_SKILL_2:
            return BossSkill2Anim.INSTANCE;
         case STICK_SKILL_3:
            return BossSkill3Anim.INSTANCE;
         case STICK_SKILL_4:
            return BossSkill4Anim.INSTANCE;
         case STICK_SKILL_5:
            return BossSkill5Anim.INSTANCE;
         case STICK_SKILL_6:
            return BossSkill6Anim.INSTANCE;
         case STICK_SKILL_7:
            return BossSkill7Anim.INSTANCE;
         case STICK_SKILL_8:
            return BossSkill8Anim.INSTANCE;
         case STICK_SKILL_9_START:
            return BossSkill9Anim.INSTANCE;
         case STICK_SKILL_9_END:
            return BossSkill9Variant3Anim.INSTANCE;
         case STICK_SKILL_10:
            return BossSkill10Anim.INSTANCE;
         case STICK_SKILL_11:
            return BossSkill11Anim.INSTANCE;
         default:
            return null;
      }
   }
}
