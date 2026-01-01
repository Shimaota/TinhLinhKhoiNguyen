package com.tlnt.world.anim.npc;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;

public final class NpcAnimDispatcher extends AnimationStateDispatcher<NpcEntity> {
   public NpcAnimDispatcher(NpcEntity entity) {
      super(entity);
   }

   @Override
   public final AnimationBehavior<NpcEntity> getAnimationBehavior(AnimationState state) {
      switch (state) {
         case NPC_IDLE:
            return NpcIdleAnim.INSTANCE;
         default:
            return null;
      }
   }
}
