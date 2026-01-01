package com.tlnt.engine.particle;

import com.badlogic.gdx.utils.Array;

public final class CurvePoint {
   public float time;
   public float value;
   public boolean deletable;
   public float handleOutX;
   public float handleOutY;
   public float handleInX;
   public float handleInY;

   public CurvePoint(float time, float value, boolean deletable) {
      this.time = time;
      this.value = value;
      this.deletable = deletable;
      this.handleOutX = time;
      this.handleOutY = value;
      this.handleInX = time;
      this.handleInY = value;
   }

   public static void copyPoints(Array<CurvePoint> source, Array<CurvePoint> target) {
      if (source != null && target != null) {
         int sourceCount = source.size;
         int targetCount = target.size;
         if (target.size < sourceCount) {
            for (int i = targetCount; i < sourceCount; i++) {
               target.add(new CurvePoint(0.0F, 0.0F, true));
            }
         }

         for (int i = 0; i < sourceCount; i++) {
            CurvePoint src = (CurvePoint)source.get(i);
            CurvePoint dst;
            (dst = (CurvePoint)target.get(i)).time = src.time;
            dst.value = src.value;
            int deletableFlag;
            if (i != 0) {
               deletableFlag = 1;
            } else {
               deletableFlag = 0;
            }

            dst.deletable = (deletableFlag != 0);
            dst.handleOutX = src.handleOutX;
            dst.handleOutY = src.handleOutY;
            dst.handleInX = src.handleInX;
            dst.handleInY = src.handleInY;
         }

         if (targetCount > sourceCount) {
            target.truncate(sourceCount);
         }
      }
   }
}
