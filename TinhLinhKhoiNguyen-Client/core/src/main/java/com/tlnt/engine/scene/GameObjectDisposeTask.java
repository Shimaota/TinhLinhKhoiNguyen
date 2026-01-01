package com.tlnt.engine.scene;

public final class GameObjectDisposeTask implements Runnable {
   public com.tlnt.engine.scene.GameObject gameObject;
   public GameObjectDisposeTask(GameObject gameObject) {
      super();
      this.gameObject = gameObject;
   }

   @Override
   public final void run() {
      this.gameObject.skeleton = true;
      this.gameObject.dispose();
      this.gameObject.owner = null;
      this.gameObject.target = null;
      this.gameObject.movementComponent = null;
   }
}
