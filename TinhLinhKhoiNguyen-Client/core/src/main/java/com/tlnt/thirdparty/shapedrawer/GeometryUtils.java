package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.math.Vector2;

public final class GeometryUtils {
   public static Vector2 edgeAB = new Vector2();
   public static Vector2 edgeBC = new Vector2();
   public static Vector2 perp = new Vector2();

   public static float preparePointyJoin(Vector2 A, Vector2 B, Vector2 C, Vector2 D, Vector2 E, float halfLineWidth) {
      edgeAB.set(B).sub(A);
      edgeBC.set(C).sub(B);
      float angle;
      if (!ShapeUtils.epsilonEquals(angle = ShapeUtils.angleRad(edgeAB, edgeBC), 0.0F) && !ShapeUtils.epsilonEquals(angle, (float) (Math.PI * 2))) {
         float len = (float)((double)halfLineWidth / Math.sin((double)angle));
         int bendsLeft = angle < 0.0F ? 1 : 0;
         edgeAB.setLength(len);
         edgeBC.setLength(len);
         C = bendsLeft != 0 ? D : E;
         D = bendsLeft != 0 ? E : D;
         C.set(B).sub(edgeAB).add(edgeBC);
         D.set(B).add(edgeAB).sub(edgeBC);
         return angle;
      } else {
         prepareStraightJoin(B, D, E, halfLineWidth);
         return angle;
      }
   }

   public static boolean prepareSmoothJoin(Vector2 A, Vector2 B, Vector2 C, Vector2 D, Vector2 E, float halfLineWidth, boolean startOfEdge) {
      edgeAB.set(B).sub(A);
      edgeBC.set(C).sub(B);
      float angle;
      if (!ShapeUtils.epsilonEquals(angle = ShapeUtils.angleRad(edgeAB, edgeBC), 0.0F) && !ShapeUtils.epsilonEquals(angle, (float) (Math.PI * 2))) {
         float len = (float)((double)halfLineWidth / Math.sin((double)angle));
         edgeAB.setLength(len);
         edgeBC.setLength(len);
         int bendsLeft;
         C = (bendsLeft = angle < 0.0F ? 1 : 0) != 0 ? D : E;
         D = bendsLeft != 0 ? E : D;
         C.set(B).sub(edgeAB).add(edgeBC);
         C = startOfEdge ? edgeBC : edgeAB;
         if (bendsLeft != 0) {
            perp.set(C.y, -C.x);
         } else {
            perp.set(-C.y, C.x);
         }

         perp.setLength(halfLineWidth);
         D.set(B).add(perp);
         return (bendsLeft != 0);
      } else {
         prepareStraightJoin(B, D, E, halfLineWidth);
         return true;
      }
   }

   public static void prepareStraightJoin(Vector2 B, Vector2 D, Vector2 E, float halfLineWidth) {
      edgeAB.setLength(halfLineWidth);
      D.set(-edgeAB.y, edgeAB.x).add(B);
      E.set(edgeAB.y, -edgeAB.x).add(B);
   }

   public static void prepareFlatEndpoint(float pathPointX, float pathPointY, float endPointX, float endPointY, Vector2 D, Vector2 E, float halfLineWidth) {
      perp.set(endPointX, endPointY).sub(pathPointX, pathPointY).setLength(halfLineWidth);
      D.set(perp.y, -perp.x).add(endPointX, endPointY);
      E.set(-perp.y, perp.x).add(endPointX, endPointY);
   }

   public static void prepareFlatEndpoint(Vector2 pathPoint, Vector2 endPoint, Vector2 D, Vector2 E, float halfLineWidth) {
      prepareFlatEndpoint(pathPoint.x, pathPoint.y, endPoint.x, endPoint.y, D, E, halfLineWidth);
   }

   public static void prepareRadialEndpoint(Vector2 A, Vector2 D, Vector2 E, float halfLineWidth) {
      perp.set(A).setLength(halfLineWidth);
      D.set(A).sub(perp);
      E.set(A).add(perp);
   }
}
