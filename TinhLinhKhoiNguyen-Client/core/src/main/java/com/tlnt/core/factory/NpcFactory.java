package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.npc.NpcEntity;

public final class NpcFactory {
   public static final ObjectPool<NpcEntity> NPC_POOL = new NpcPool();

   public static NpcEntity obtainNpc(int id, float x, float y) {
      NpcEntity npc;
      (npc = NPC_POOL.obtain()).setId(id);
      npc.teleportTo(x, y);
      return npc;
   }

   public static void freeNpc(NpcEntity npc) {
      NPC_POOL.free(npc);
   }
}
