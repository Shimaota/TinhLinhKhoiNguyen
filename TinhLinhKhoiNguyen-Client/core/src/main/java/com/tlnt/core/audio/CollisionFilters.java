package com.tlnt.core.audio;

import com.badlogic.gdx.physics.box2d.Filter;

public final class CollisionFilters {
   public static final CollisionFilters.CategoryMaskFilter PLAYER;
   public static final CollisionFilters.CategoryMaskFilter TERRAIN = new CollisionFilters.CategoryMaskFilter(2, 25);
   public static final CollisionFilters.CategoryMaskFilter WAYPOINT;
   public static final CollisionFilters.CategoryMaskFilter MAP_ITEM = new CollisionFilters.CategoryMaskFilter(16, 2);

   static {
      PLAYER = new CollisionFilters.CategoryMaskFilter(1, 6);
      WAYPOINT = new CollisionFilters.CategoryMaskFilter(4, 1);
      new CollisionFilters.CategoryMaskFilter(8, 2);
   }

   public static class CategoryMaskFilter extends Filter {
      public CategoryMaskFilter(int category, int mask) {
         super.categoryBits = (short)category;
         super.maskBits = (short)mask;
      }
   }
}
