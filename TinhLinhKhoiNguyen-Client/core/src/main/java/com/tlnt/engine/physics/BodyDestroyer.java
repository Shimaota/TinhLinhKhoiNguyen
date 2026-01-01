package com.tlnt.engine.physics;

import com.badlogic.gdx.physics.box2d.Body;

public final class BodyDestroyer implements Runnable {
   public com.tlnt.engine.physics.PhysicsWorld world;
   public com.badlogic.gdx.physics.box2d.Body body;
   public BodyDestroyer(PhysicsWorld world, Body body) {
      super();
      this.world = world;
      this.body = body;
   }

   @Override
   public final void run() {
      this.world.contactDispatcher.removeContactsForBody(this.world.bodyComponentMap.remove(this.body));
      this.world.getBox2dWorld().destroyBody(this.body);
   }
}
