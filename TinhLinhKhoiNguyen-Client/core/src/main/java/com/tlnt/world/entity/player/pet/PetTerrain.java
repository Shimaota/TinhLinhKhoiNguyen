package com.tlnt.world.entity.player.pet;

import java.util.ArrayList;
import java.util.List;

/**
 * Terrain of the current map for pet movement ({@link PetBrain}): a copy of the collision boxes / slopes received in op 6, in
 * PIXELS like the server (client entity positions = px / 100). Reloaded on every map enter ({@code MessageHandler} case 6).
 */
public final class PetTerrain {

   /** A box ({@code points} null) or a triangular slope ({@code points} = x1,y1,x2,y2,x3,y3). */
   public static final class Collider {
      final float x;
      final float y;
      final float w;
      final float h;
      final boolean oneWay;
      final float[] points;

      public Collider(float x, float y, float w, float h, boolean oneWay) {
         this(x, y, w, h, oneWay, null);
      }

      Collider(float x, float y, float w, float h, boolean oneWay, float[] points) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.oneWay = oneWay;
         this.points = points;
      }

      public static Collider triangle(float[] points, boolean oneWay) {
         return new Collider(0, 0, 0, 0, oneWay, points.clone());
      }

      boolean isTriangle() {
         return points != null && points.length == 6;
      }
   }

   /** Current map (the client is only on one map at a time). */
   public static final PetTerrain CURRENT = new PetTerrain();

   final List<Collider> colliders = new ArrayList<Collider>();
   /** Map width (px), 0 = no clamping. */
   float width;

   public void reset(float width) {
      this.colliders.clear();
      this.width = width;
   }

   public void add(Collider collider) {
      this.colliders.add(collider);
   }

   public boolean isEmpty() {
      return this.colliders.isEmpty();
   }

   float clampX(float x) {
      return this.width <= 0 ? x : Math.max(0, Math.min(this.width, x));
   }
}
