package com.tlnt.model.friendentry;

import com.badlogic.gdx.utils.Pool;

public final class FriendEntryPool extends Pool<FriendEntry> {

   @Override
   public FriendEntry newObject() {
      return new com.tlnt.model.friendentry.FriendEntry();
   }
}
