package com.tlnt.engine.physics;

import java.util.Comparator;

public final class ComponentRenderComparator implements Comparator<Component> {
   public com.tlnt.engine.physics.PhysicsWorld world;
   public ComponentRenderComparator(PhysicsWorld world) {
      super();
      this.world = world;
   }

   public final int compare(Component o1, Component o2) {
      return Float.compare(o1.getRenderOrder(), o2.getRenderOrder());
   }
}
