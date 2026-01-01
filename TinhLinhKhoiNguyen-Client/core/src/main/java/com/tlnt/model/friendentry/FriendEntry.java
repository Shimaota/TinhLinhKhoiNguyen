package com.tlnt.model.friendentry;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.engine.pool.ObjectPool;

public final class FriendEntry implements ObjectPool.Poolable {
   public static final Pool<FriendEntry> POOL = new FriendEntryPool();
   public int index;
   public int playerId;
   public String name;
   public String avatarIcon;
   public int level;
   public boolean online;

   @Override
   public final void reset() {
      this.name = null;
   }
}
