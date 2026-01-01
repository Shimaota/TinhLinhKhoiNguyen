package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.graphics.Color;

public final class PathAttachment extends VertexAttachment {
   public float[] color;
   public boolean closed;
   public boolean constantSpeed;
   public Color color2 = new Color(1.0F, 0.5F, 0.0F, 1.0F);

   public PathAttachment(String name) {
      super(name);
   }

   public final boolean getClosed() {
      return this.closed;
   }

   public final void setClosed(boolean closed) {
      this.closed = closed;
   }

   public final boolean getConstantSpeed() {
      return this.constantSpeed;
   }

   public final void setConstantSpeed(boolean constantSpeed) {
      this.constantSpeed = constantSpeed;
   }

   public final float[] getLengths() {
      return this.color;
   }

   public final void setLengths(float[] lengths) {
      this.color = lengths;
   }

   public final Color getColor() {
      return this.color2;
   }
}
