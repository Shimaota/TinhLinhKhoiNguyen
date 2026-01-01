package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;

public final class PolygonDrawer extends Drawer<BatchManager> {
   public FloatArray path = new FloatArray();
   public FloatArray tempPath = new FloatArray();
   public static Vector2 vecD0 = new Vector2();
   public static Vector2 vecE0 = new Vector2();

   public PolygonDrawer(BatchManager batchManager, AbstractShapeDrawer drawer) {
      super(batchManager, drawer);
   }

   public final void drawPath(float[] userPath, float lineWidth, int joinType, boolean open) {
      this.drawPath(userPath, 0, userPath.length, lineWidth, joinType, open);
   }

   public final void drawPath(float[] userPath, int start, int end, float lineWidth, int joinType, boolean open) {
      int pathLength = userPath.length;
      if (pathLength >= 4) {
         this.path.add(userPath[start]);
         this.path.add(userPath[start + 1]);

         for (int i = start + 2; i < end; i += 2) {
            if (ShapeUtils.epsilonEquals(userPath[i - 2], userPath[i])) {
               if (ShapeUtils.epsilonEquals(userPath[i - 1], userPath[i + 1])) {
                  continue;
               }
            }

            float x = userPath[i];
            this.path.add(x, userPath[i + 1]);
         }

         if (this.path.size < 4) {
            this.path.clear();
         } else if (this.path.size == 4) {
            float x1 = this.path.items[0];
            float y1 = this.path.items[1];
            float x2 = this.path.items[2];
            float y2 = this.path.items[3];
            super.drawer.lineDrawer.drawLine(x1, y1, x2, y2, lineWidth, false);
            this.path.clear();
         } else {
            boolean wasCaching = super.batchManager.startCaching();
            if (joinType == JoinType.NONE) {
               this.drawPathNoJoin(this.path.items, this.path.size, lineWidth, open);
            } else {
               int pointyJoin;
               if (joinType == JoinType.POINTY) {
                  pointyJoin = 1;
               } else {
                  pointyJoin = 0;
               }

               this.drawPathWithJoin(this.path.items, this.path.size, lineWidth, open, (pointyJoin != 0));
            }

            if (!wasCaching) {
               super.batchManager.endCaching();
            }

            this.path.clear();
         }
      }
   }

   public void drawPathNoJoin(float[] path, int size, float lineWidth, boolean open) {
      int n = open ? size - 2 : size;

      for (int i = 0; i < n; i += 2) {
         float x1 = path[i];
         float y1 = path[i + 1];
         float x2 = path[(i + 2) % size];
         float y2 = path[(i + 3) % size];
         super.drawer.lineDrawer.drawLine(x1, y1, x2, y2, lineWidth, false);
      }
   }

   public void drawPathWithJoin(float[] path, int size, float lineWidth, boolean open, boolean pointyJoin) {
      lineWidth = 0.5F * lineWidth;
      float c = super.batchManager.floatBits;
      super.batchManager.ensureSpaceForQuad();

      for (int i = 2; i < size - 2; i += 2) {
         VEC_A.set(path[i - 2], path[i - 1]);
         float coord = path[i];
         VEC_B.set(coord, path[i + 1]);
         coord = path[i + 2];
         VEC_C.set(coord, path[i + 3]);
         if (pointyJoin) {
            GeometryUtils.preparePointyJoin(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, lineWidth);
         } else {
            GeometryUtils.prepareSmoothJoin(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, lineWidth, false);
         }

         this.setVertex3(VEC_D);
         this.setVertex4(VEC_E);
         if (i == 2) {
            if (open) {
               float pathX = path[2];
               coord = path[3];
               GeometryUtils.prepareFlatEndpoint(pathX, coord, path[0], path[1], VEC_D, VEC_E, lineWidth);
               this.setVertex1(VEC_E);
               this.setVertex2(VEC_D);
            } else {
               VEC_1.set(path[size - 2], path[size - 1]);
               if (pointyJoin) {
                  GeometryUtils.preparePointyJoin(VEC_1, VEC_A, VEC_B, vecD0, vecE0, lineWidth);
               } else {
                  GeometryUtils.prepareSmoothJoin(VEC_1, VEC_A, VEC_B, vecD0, vecE0, lineWidth, true);
               }

               this.setVertex1(vecE0);
               this.setVertex2(vecD0);
            }
         }

         float x3;
         float y3;
         float x4;
         float y4;
         if (pointyJoin) {
            x3 = this.getX3();
            y3 = this.getY3();
            x4 = this.getX4();
            y4 = this.getY4();
         } else {
            GeometryUtils.prepareSmoothJoin(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, lineWidth, true);
            x3 = VEC_D.x;
            y3 = VEC_D.y;
            x4 = VEC_E.x;
            y4 = VEC_E.y;
         }

         this.setColors(c, c, c, c);
         super.batchManager.pushQuad();
         if (!pointyJoin) {
            this.drawSmoothJoinFill(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, lineWidth);
         }

         super.batchManager.ensureSpaceForQuad();
         this.setVertex1(x4, y4);
         this.setVertex2(x3, y3);
      }

      if (open) {
         GeometryUtils.prepareFlatEndpoint(VEC_B, VEC_C, VEC_D, VEC_E, lineWidth);
         this.setVertex3(VEC_E);
         this.setVertex4(VEC_D);
         this.setColors(c, c, c, c);
         super.batchManager.pushQuad();
      } else if (pointyJoin) {
         VEC_A.set(path[0], path[1]);
         GeometryUtils.preparePointyJoin(VEC_B, VEC_C, VEC_A, VEC_D, VEC_E, lineWidth);
         this.setVertex3(VEC_D);
         this.setVertex4(VEC_E);
         this.setColors(c, c, c, c);
         super.batchManager.pushQuad();
         super.batchManager.ensureSpaceForQuad();
         this.setVertex1(VEC_D);
         this.setVertex2(VEC_E);
         this.setVertex3(vecE0);
         this.setVertex4(vecD0);
         this.setColors(c, c, c, c);
         super.batchManager.pushQuad();
      } else {
         VEC_A.set(VEC_B);
         VEC_B.set(VEC_C);
         VEC_C.set(path[0], path[1]);
         GeometryUtils.prepareSmoothJoin(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, lineWidth, false);
         this.setVertex3(VEC_D);
         this.setVertex4(VEC_E);
         this.setColors(c, c, c, c);
         super.batchManager.pushQuad();
         this.drawSmoothJoinFill(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, lineWidth);
         super.batchManager.ensureSpaceForQuad();
         GeometryUtils.prepareSmoothJoin(VEC_A, VEC_B, VEC_C, VEC_D, VEC_E, lineWidth, true);
         this.setVertex3(VEC_E);
         this.setVertex4(VEC_D);
         float ax = path[2];
         VEC_A.set(ax, path[3]);
         GeometryUtils.prepareSmoothJoin(VEC_B, VEC_C, VEC_A, VEC_D, VEC_E, lineWidth, false);
         this.setVertex1(VEC_D);
         this.setVertex2(VEC_E);
         this.setColors(c, c, c, c);
         super.batchManager.pushQuad();
         this.drawSmoothJoinFill(VEC_B, VEC_C, VEC_A, VEC_D, VEC_E, lineWidth);
      }
   }
}
