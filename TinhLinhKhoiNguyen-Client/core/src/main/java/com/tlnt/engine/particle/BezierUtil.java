package com.tlnt.engine.particle;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

public final class BezierUtil {
   public static void evalCubicBezier(float t, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float[] out) {
      float u;
      float uu;
      float uuu = (uu = (u = 1.0F - t) * u) * u;
      float tt;
      float ttt = (tt = t * t) * t;
      out[0] = uuu * x0 + 3.0F * uu * t * x1 + 3.0F * u * tt * x2 + ttt * x3;
      out[1] = uuu * y0 + 3.0F * uu * t * y1 + 3.0F * u * tt * y2 + ttt * y3;
   }

   public static float evalCurve(float t, Array<CurvePoint> points, float minValue, float maxValue) {
      if (points != null && points.size != 0) {
         t = MathUtils.clamp(t, 0.0F, 1.0F);
         float startTime = 0.0F;
         float startValue = 0.0F;
         float endTime = 1.0F;
         float endValue = 0.0F;

         for (int i = points.size - 1; i >= 0; i--) {
            CurvePoint point = (CurvePoint)points.get(i);
            if (t >= point.time) {
               startTime = point.time;
               startValue = point.value;
               if (i < points.size - 1) {
                  CurvePoint next;
                  endTime = (next = (CurvePoint)points.get(i + 1)).time;
                  endValue = next.value;
                  int handleFlag;
                  if (!(Math.abs(point.handleOutX - point.time) > 1.0E-5F) && !(Math.abs(point.handleOutY - point.value) > 1.0E-5F)) {
                     handleFlag = 0;
                  } else {
                     handleFlag = 1;
                  }

                  int outHandleFlag = handleFlag;
                  if (!(Math.abs(next.handleInX - next.time) > 1.0E-5F) && !(Math.abs(next.handleInY - next.value) > 1.0E-5F)) {
                     handleFlag = 0;
                  } else {
                     handleFlag = 1;
                  }

                  int inHandleFlag = handleFlag;
                  if ((outHandleFlag == 0 && inHandleFlag == 0 ? 0 : 1) != 0) {
                     outHandleFlag = 64;
                     float sampleT = t;
                     float p0x = point.time;
                     float p0y = point.value;
                     startTime = point.handleOutX;
                     startValue = point.handleOutY;
                     endTime = next.handleInX;
                     endValue = next.handleInY;
                     float p3x = next.time;
                     t = next.value;
                     float result;
                     if (sampleT <= p0x) {
                        result = p0y;
                     } else {
                        label65: {
                           if (!(sampleT >= p3x)) {
                              float[] out = new float[2];
                              float prevX = p0x;

                              for (int step = 1; step <= 64; step++) {
                                 evalCubicBezier((float)step / 64.0F, p0x, p0y, startTime, startValue, endTime, endValue, p3x, t, out);
                                 if (out[0] >= sampleT) {
                                    float prevT = (float)(step - 1) / 64.0F;
                                    float segDx;
                                    float frac = (segDx = out[0] - prevX) <= 0.0F ? 0.0F : (sampleT - prevX) / segDx;
                                    float refinedT;
                                    evalCubicBezier(refinedT = prevT + frac * (1.0F / 64.0F), p0x, p0y, startTime, startValue, endTime, endValue, p3x, t, out);
                                    result = out[1];
                                    break label65;
                                 }

                                 prevX = out[0];
                              }
                           }

                           result = t;
                        }
                     }

                     t = result;
                     return minValue + (maxValue - minValue) * t;
                  }
               } else {
                  endTime = 1.0F;
                  endValue = startValue;
               }
               break;
            }
         }

         float clampedT = t;
         if (t < startTime) {
            clampedT = startTime;
         }

         if (clampedT > endTime) {
            clampedT = endTime;
         }

         float timeRange;
         clampedT = (timeRange = endTime - startTime) <= 0.0F ? startValue : startValue + (endValue - startValue) * (clampedT - startTime) / timeRange;
         return minValue + (maxValue - minValue) * clampedT;
      } else {
         return minValue;
      }
   }
}
