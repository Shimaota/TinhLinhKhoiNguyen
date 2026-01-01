package com.tlnt.thirdparty.spine;

public final class PhysicsConstraintData extends ConstraintData {
   public BoneData bone;
   public float x;
   public float y;
   public float rotate;
   public float scaleX;
   public float shearX;
   public float limit;
   public float step;
   public float inertia;
   public float strength;
   public float damping;
   public float massInverse;
   public float wind;
   public float gravity;
   public float mix;
   public boolean inertiaGlobal;
   public boolean strengthGlobal;
   public boolean dampingGlobal;
   public boolean massGlobal;
   public boolean windGlobal;
   public boolean gravityGlobal;
   public boolean mixGlobal;

   public PhysicsConstraintData(String name) {
      super(name);
   }
}
