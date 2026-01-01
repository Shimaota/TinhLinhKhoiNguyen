package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.core.GameMain;

public final class NpcPool extends ObjectPool<NpcEntity> {
   @Override
   public NpcEntity newObject() {
      return new NpcEntity(com.tlnt.core.GameMain.instance.getLoginScreen(), 0, 0.0F, 0.0F);
   }

   @Override
   public final NpcEntity obtain() {
      NpcEntity npc;
      (npc = (NpcEntity)super.obtain()).setActive(true);
      return npc;
   }

   @Override
   public final void free(NpcEntity obj) {
      obj.setActive(false);
      super.free(obj);
   }
}
