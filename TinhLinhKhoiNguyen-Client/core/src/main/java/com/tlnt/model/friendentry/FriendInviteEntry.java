package com.tlnt.model.friendentry;

import com.badlogic.gdx.utils.Pool;

public final class FriendInviteEntry implements Pool.Poolable {
   public static final Pool<FriendInviteEntry> POOL = new FriendInviteEntryPool();
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
