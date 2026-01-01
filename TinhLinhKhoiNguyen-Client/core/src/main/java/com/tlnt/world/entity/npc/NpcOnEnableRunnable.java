package com.tlnt.world.entity.npc;

import com.tlnt.engine.physics.Direction;

public final class NpcOnEnableRunnable implements Runnable {
   public com.tlnt.world.entity.npc.NpcEntity npcEntity;
   public NpcOnEnableRunnable(NpcEntity npcEntity) {
      super();
      this.npcEntity = npcEntity;
   }

   @Override
   public final void run() {
      this.npcEntity.setDirection(this.npcEntity.POOL2.direction == 1 ? Direction.RIGHT : Direction.LEFT);
      this.npcEntity.owner.setEnabled(this.npcEntity.POOL2.shadowEnabled);
   }
}
