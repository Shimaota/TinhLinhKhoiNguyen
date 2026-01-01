package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.math.Vector2;

public final class CircleDrawer extends Drawer<BatchManager> {
   public static Vector2 center = new Vector2();
   public static Vector2 radius = new Vector2();

   public CircleDrawer(BatchManager batchManager, AbstractShapeDrawer drawer) {
      super(batchManager, drawer);
   }

   public final void drawPolygonOutline(float centreX, float centreY, int sides, float radiusX, float radiusY, float rotation, float lineWidth, int joinType, float startAngle, float radians) {
      radians = Math.min((float) (Math.PI * 2), (float) (Math.PI * 2));
      startAngle = 0.5F * lineWidth;
      center.set(centreX, centreY);
      radius.set(radiusX, radiusY);
      boolean wasCaching = super.batchManager.startCaching();
      if (joinType == JoinType.NONE) {
         float arcStart = 0.0F;
         Vector2 radiusVec = radius;
         radiusY = lineWidth;
         Vector2 centreVec = center;
         CircleDrawer circleDrawer = this;
         float angleInterval = (float) (Math.PI * 2) / (float)sides;
         float endAngle = arcStart + radians;
         float cos = (float)Math.cos((double)angleInterval);
         float sin = (float)Math.sin((double)angleInterval);
         startAngle = (float)Math.cos((double)rotation);
         float sinRot = (float)Math.sin((double)rotation);
         int start = (int)Math.ceil((double)((float)sides * (arcStart / (float) (Math.PI * 2))));
         int end = (int)Math.floor((double)((float)sides * (endAngle / (float) (Math.PI * 2)))) + 1;
         DIR.set(1.0F, 0.0F).rotateRad(Math.min((float)start * angleInterval, endAngle));
         VEC_A.set(1.0F, 0.0F).rotateRad(arcStart).scl(radiusVec);
         VEC_B.set(DIR).scl(radiusVec);

         for (int i = start; i <= end; i++) {
            float x1 = VEC_A.x * startAngle - VEC_A.y * sinRot + centreVec.x;
            float y1 = VEC_A.x * sinRot + VEC_A.y * startAngle + centreVec.y;
            radiusX = VEC_B.x * startAngle - VEC_B.y * sinRot + centreVec.x;
            float y2 = VEC_B.x * sinRot + VEC_B.y * startAngle + centreVec.y;
            circleDrawer.drawer.lineDrawer.pushLine(x1, y1, radiusX, y2, radiusY, false);
            if (i < end - 1) {
               VEC_A.set(VEC_B);
               DIR.set(DIR.x * cos - DIR.y * sin, DIR.x * sin + DIR.y * cos);
               VEC_B.set(DIR).scl(radiusVec);
            } else {
               if (i == end - 1) {
                  VEC_A.set(VEC_B);
                  VEC_B.set(1.0F, 0.0F).rotateRad(endAngle).scl(radiusVec);
               }
            }
         }
      } else {
         int smoothFlag;
         if (joinType == JoinType.SMOOTH) {
            smoothFlag = 1;
         } else {
            smoothFlag = 0;
         }

         int smooth = smoothFlag;
         float joinArcStart = 0.0F;
         Vector2 joinRadiusVec = radius;
         radiusY = startAngle;
         Vector2 joinCentreVec = center;
         CircleDrawer joinCircleDrawer = this;
         float color = super.batchManager.floatBits;
         boolean full = ShapeUtils.epsilonEquals(radians, (float) (Math.PI * 2));
         float joinAngleInterval = (float) (Math.PI * 2) / (float)sides;
         startAngle = joinArcStart + radians;
         float joinCos = (float)Math.cos((double)joinAngleInterval);
         float joinSin = (float)Math.sin((double)joinAngleInterval);
         float joinCosRot = (float)Math.cos((double)rotation);
         rotation = (float)Math.sin((double)rotation);
         int joinStart;
         int joinEnd;
         if (full) {
            joinStart = 1;
            joinEnd = sides;
            DIR.set(1.0F, 0.0F).rotateRad(joinAngleInterval * 1.0F);
            VEC_A.set(1.0F, 0.0F).rotateRad(-1.0F * joinAngleInterval).scl(joinRadiusVec);
            VEC_C.set(DIR).scl(joinRadiusVec);
            VEC_B.set(1.0F, 0.0F).rotateRad(joinAngleInterval * 0.0F).scl(joinRadiusVec);
         } else {
            if (ShapeUtils.epsilonEquals((float)(joinStart = (int)Math.ceil((double)((float)sides * (joinArcStart / (float) (Math.PI * 2))))) * joinAngleInterval, joinArcStart)) {
               joinStart++;
            }

            joinEnd = Math.min((int)Math.floor((double)((float)sides * (startAngle / (float) (Math.PI * 2)))) + 1, joinStart + sides);
            DIR.set(1.0F, 0.0F).rotateRad(Math.min((float)joinStart * joinAngleInterval, startAngle));
            Vector2 vecA = VEC_A.set(1.0F, 0.0F);
            vecA.rotateRad((float)(joinStart - 1) * joinAngleInterval).scl(joinRadiusVec);
            VEC_B.set(1.0F, 0.0F).rotateRad(joinArcStart).scl(joinRadiusVec);
            VEC_C.set(DIR).scl(joinRadiusVec);
         }

         for (int j = joinStart; j <= joinEnd; j++) {
            joinCircleDrawer.batchManager.ensureSpaceForQuad();
            if (!full && j == joinStart) {
               GeometryUtils.prepareRadialEndpoint(VEC_B, VEC_D, VEC_E, radiusY);
            } else if (smooth != 0) {
               GeometryUtils.prepareSmoothJoin(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, radiusY, true);
            } else {
               GeometryUtils.preparePointyJoin(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, radiusY);
            }

            joinCircleDrawer.setVertex1(VEC_E.x * joinCosRot - VEC_E.y * rotation + joinCentreVec.x, VEC_E.x * rotation + VEC_E.y * joinCosRot + joinCentreVec.y);
            joinCircleDrawer.setVertex2(VEC_D.x * joinCosRot - VEC_D.y * rotation + joinCentreVec.x, VEC_D.x * rotation + VEC_D.y * joinCosRot + joinCentreVec.y);
            if (!full && j >= joinEnd) {
               VEC_B.set(1.0F, 0.0F).rotateRad(startAngle).scl(joinRadiusVec);
            } else {
               VEC_A.set(VEC_B);
               VEC_B.set(VEC_C);
               DIR.set(DIR.x * joinCos - DIR.y * joinSin, DIR.x * joinSin + DIR.y * joinCos);
               VEC_C.set(DIR).scl(joinRadiusVec);
            }

            if (!full && j >= joinEnd) {
               GeometryUtils.prepareRadialEndpoint(VEC_B, VEC_D, VEC_E, radiusY);
            } else if (smooth != 0) {
               GeometryUtils.prepareSmoothJoin(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, radiusY, false);
            } else {
               GeometryUtils.preparePointyJoin(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, radiusY);
            }

            joinCircleDrawer.setVertex3(VEC_D.x * joinCosRot - VEC_D.y * rotation + joinCentreVec.x, VEC_D.x * rotation + VEC_D.y * joinCosRot + joinCentreVec.y);
            joinCircleDrawer.setVertex4(VEC_E.x * joinCosRot - VEC_E.y * rotation + joinCentreVec.x, VEC_E.x * rotation + VEC_E.y * joinCosRot + joinCentreVec.y);
            joinCircleDrawer.setColors(color, color, color, color);
            joinCircleDrawer.batchManager.pushQuad();
            if (smooth != 0 && (full || j < joinEnd)) {
               joinCircleDrawer.drawSmoothJoinFill(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, joinCentreVec, joinCosRot, rotation, radiusY);
            }
         }
      }

      if (!wasCaching) {
         super.batchManager.endCaching();
      }
   }
}
