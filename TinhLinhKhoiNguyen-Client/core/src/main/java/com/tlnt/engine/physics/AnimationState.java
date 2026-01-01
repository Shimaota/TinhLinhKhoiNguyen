package com.tlnt.engine.physics;

public enum AnimationState {
   DEFAULT(true, true, true),
   IDLE(true, true, true),
   MOVE(true, true, true),
   JUMP(true, true, true),
   FALL(true, true, true),
   HURT(true, true, true),
   PICK,
   DIE,
   STUN,
   CAST_LINE_FISHING,
   WAITING_FOR_BITE_FISHING,
   SET_HOOK_FISHING,
   MELEE_NORMAL_ATTACK,
   AXE_NORMAL_ATTACK,
   AXE_SKILL_1,
   AXE_SKILL_2_START,
   AXE_SKILL_2_END,
   AXE_SKILL_4,
   AXE_SKILL_3,
   AXE_SKILL_5_START,
   AXE_SKILL_5_END,
   BOW_NORMAL_ATTACK,
   BOW_SKILL_1,
   BOW_SKILL_2,
   BOW_SKILL_3,
   BOW_SKILL_4,
   BOW_SKILL_5_START,
   BOW_SKILL_5_END,
   SLINGSHOT_NORMAL_ATTACK,
   SLINGSHOT_SKILL_1,
   SLINGSHOT_SKILL_2,
   SLINGSHOT_SKILL_3,
   SLINGSHOT_SKILL_4,
   SLINGSHOT_SKILL_5_START,
   SLINGSHOT_SKILL_5_END,
   SPEAR_NORMAL_ATTACK,
   SPEAR_SKILL_1,
   SPEAR_SKILL_2,
   SPEAR_SKILL_3,
   SPEAR_SKILL_4,
   SPEAR_SKILL_5_START,
   SPEAR_SKILL_5_END,
   WHIP_NORMAL_ATTACK,
   WHIP_SKILL_1,
   WHIP_SKILL_2,
   WHIP_SKILL_3,
   WHIP_SKILL_4,
   WHIP_SKILL_5_START,
   WHIP_SKILL_5_END,
   WHIP_SKILL_6,
   STICK_SKILL_1,
   STICK_SKILL_2,
   STICK_SKILL_3,
   STICK_SKILL_4,
   STICK_SKILL_5,
   STICK_SKILL_6,
   STICK_SKILL_7,
   STICK_SKILL_8,
   STICK_SKILL_9_START,
   STICK_SKILL_9_END,
   STICK_SKILL_10,
   STICK_SKILL_11,
   MOB_IDLE(true, false, true),
   MOB_MOVE(true, false, true),
   MOB_HURT,
   MOB_DIE,
   MOB_ATTACK_1,
   MOB_ATTACK_2,
   MOB_SKILL_3,
   MOB_SKILL_4,
   MOB_STUN,
   PLANT_IDLE(true, false, true),
   ANIMAL_IDLE(true, false, true),
   ANIMAL_MOVE(true, false, true),
   ANIMAL_DIE(true, false, true),
   NPC_IDLE(false, false, false);

   public final boolean allowsMovement;
   public final boolean allowsInteraction;
   public final boolean interruptible;

   AnimationState(boolean allowsMovement, boolean allowsInteraction, boolean interruptible) {
      this.allowsMovement = allowsMovement;
      this.allowsInteraction = allowsInteraction;
      this.interruptible = interruptible;
   }

   AnimationState() {
      this(false, false, false);
   }

}
