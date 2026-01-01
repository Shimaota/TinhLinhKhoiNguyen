package com.tlnt.thirdparty.shapedrawer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;

public abstract class AbstractShapeDrawer {
   public final BatchManager batchManager;
   public float defaultLineWidth = 1.0F;
   public boolean defaultSnap;
   public final LineDrawer lineDrawer;
   public final PolygonDrawer polygonDrawer;
   public final CircleDrawer circleDrawer;
   public final FilledPolygonDrawer filledPolygonDrawer;
   public SideEstimator rectPathPoints;

   public AbstractShapeDrawer(Batch batch, TextureRegion region, DefaultSideEstimator sideEstimator) {
      this.defaultSnap = false;
      if (batch instanceof PolygonBatch) {
         PolygonBatchManager polygonBatchManager = new PolygonBatchManager((PolygonBatch)batch, region);
         this.filledPolygonDrawer = new FilledPolygonDrawer.PolygonBatchedFilledPolygonDrawer(polygonBatchManager, this);
         this.batchManager = polygonBatchManager;
      } else {
         this.batchManager = new BatchManager(batch, region);
         this.filledPolygonDrawer = new FilledPolygonDrawer.BatchedFilledPolygonDrawer(this.batchManager, this);
      }

      this.lineDrawer = new LineDrawer(this.batchManager, this);
      this.polygonDrawer = new PolygonDrawer(this.batchManager, this);
      this.circleDrawer = new CircleDrawer(this.batchManager, this);
      this.rectPathPoints = sideEstimator;
   }

   public final int estimateSidesRequired(float radiusX, float radiusY) {
      return this.rectPathPoints.estimateSidesRequired(this.batchManager.getPixelSize(), radiusX, radiusY);
   }

   public final float setColor(Color color) {
      return this.setPackedColor(color.toFloatBits());
   }

   public final float setPackedColor(float floatBits) {
      return this.batchManager.setPackedColor(floatBits);
   }

   public final float setColorRGBA(float r, float g, float b, float a) {
      return this.setPackedColor(Color.toFloatBits(r, g, b, a));
   }

   static {
      new Matrix4();
      float[] unusedFloats = new float[6];
   }
}
