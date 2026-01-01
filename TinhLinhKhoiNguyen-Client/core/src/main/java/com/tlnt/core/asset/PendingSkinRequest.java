package com.tlnt.core.asset;

public final class PendingSkinRequest {
   public boolean pending;
   public String[] skinNames;

   public final void clear() {
      this.pending = false;
      this.skinNames = null;
   }
}
