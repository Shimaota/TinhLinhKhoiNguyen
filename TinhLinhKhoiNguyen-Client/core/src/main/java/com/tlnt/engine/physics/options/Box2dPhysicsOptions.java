package com.tlnt.engine.physics.options;

import java.util.Objects;

public final class Box2dPhysicsOptions {
   public float timeStep = 0.008333334F;
   public float maxFixedFrameTime = 0.25F;
   public boolean interpolateMovement;
   public int velocityIterations;
   public int positionIterations;

   public Box2dPhysicsOptions() {
      this.interpolateMovement = true;
      this.velocityIterations = 8;
      this.positionIterations = 4;
   }

   public final float getTimeStep() {
      return this.timeStep;
   }

   public final float getMaxFixedFrameTime() {
      return this.maxFixedFrameTime;
   }

   public final boolean isInterpolateMovement() {
      return this.interpolateMovement;
   }

   public final int getVelocityIterations() {
      return this.velocityIterations;
   }

   public final int getPositionIterations() {
      return this.positionIterations;
   }

   @Override
   public final boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null || this.getClass() != obj.getClass()) {
         return false;
      } else {
         Box2dPhysicsOptions other;
         return ((Float.compare((other = (Box2dPhysicsOptions)obj).timeStep, this.timeStep) == 0
               && Float.compare(other.maxFixedFrameTime, this.maxFixedFrameTime) == 0
               && this.interpolateMovement == other.interpolateMovement
               && this.velocityIterations == other.velocityIterations
               && this.positionIterations == other.positionIterations
            ? 1
            : 0) != 0);
      }
   }

   @Override
   public final int hashCode() {
      Object[] values = new Object[5];
      values[0] = this.timeStep;
      values[1] = this.maxFixedFrameTime;
      values[2] = this.interpolateMovement;
      values[3] = this.velocityIterations;
      values[4] = this.positionIterations;
      return Objects.hash(values);
   }

   @Override
   public final String toString() {
      return "Box2dPhysicsOptions{timeStep="
         + this.timeStep
         + ", maxFixedFrameTime="
         + this.maxFixedFrameTime
         + ", interpolateMovement="
         + this.interpolateMovement
         + ", velocityIteration="
         + this.velocityIterations
         + ", positionIterations="
         + this.positionIterations
         + "}";
   }
}
