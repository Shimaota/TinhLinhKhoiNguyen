package com.tlnt.world.anim.npc;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.engine.scene.GameObject;

public final class NpcIdleAnim extends AnimationBehavior<NpcEntity> {
   public static final NpcIdleAnim INSTANCE = new NpcIdleAnim();

   @Override
   public final void onEnter(NpcEntity entity) {
      String animName = "idle";
      NpcEntity npc;
      (npc = (NpcEntity)entity).skeleton.playAnimation(animName, true, 1.0F);
   }
}
