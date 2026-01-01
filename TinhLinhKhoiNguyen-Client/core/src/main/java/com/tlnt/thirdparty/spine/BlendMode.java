package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.graphics.g2d.Batch;

public enum BlendMode {
   NORMAL(770, 1, 771, 1),
   ADDITIVE(770, 1, 1, 1),
   MULTIPLY(774, 774, 771, 771),
   SCREEN(1, 1, 769, 769);

   public int source;
   public int sourcePMA;
   public int dest;
   public int sourceAlpha;
   public static final BlendMode[] VALUES;

   BlendMode(int source, int sourcePMA, int dest, int sourceAlpha) {
      this.source = source;
      this.sourcePMA = sourcePMA;
      this.dest = dest;
      this.sourceAlpha = sourceAlpha;
   }

   public final void apply(Batch batch, boolean premultipliedAlpha) {
      batch.setBlendFunctionSeparate(premultipliedAlpha ? this.sourcePMA : this.source, this.dest, this.sourceAlpha, this.dest);
   }

   static {
      BlendMode[] values = new BlendMode[]{NORMAL, ADDITIVE, MULTIPLY, SCREEN};
      VALUES = (BlendMode[])values.clone();
   }

}
