package com.tlnt.core.asset;

public final class PendingAnimationRequest {
   public boolean pending;
   public String animationName;
   public boolean loop;
   public float timeScale;

   public final void clear() {
      this.pending = false;
      this.animationName = null;
   }
}
