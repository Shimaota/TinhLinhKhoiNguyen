package com.tlnt.model.friendentry;

import com.badlogic.gdx.utils.Pool;

public final class FriendInviteEntryPool extends Pool<FriendInviteEntry> {

   @Override
   public FriendInviteEntry newObject() {
      return new com.tlnt.model.friendentry.FriendInviteEntry();
   }
}
