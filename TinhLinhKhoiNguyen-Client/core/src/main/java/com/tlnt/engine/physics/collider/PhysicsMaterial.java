package com.tlnt.engine.physics.collider;

public final class PhysicsMaterial {
   public float friction;
   public float restitution;
   public float density;

   public PhysicsMaterial() {
   }

   public PhysicsMaterial(float friction, float restitution, float density) {
      this.friction = 0.0F;
      this.restitution = 0.0F;
      this.density = 0.0F;
   }
}
