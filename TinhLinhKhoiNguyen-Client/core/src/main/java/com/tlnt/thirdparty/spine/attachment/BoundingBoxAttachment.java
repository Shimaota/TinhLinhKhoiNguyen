package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.graphics.Color;

public final class BoundingBoxAttachment extends VertexAttachment {
   public Color color = new Color(0.38F, 0.94F, 0.0F, 1.0F);

   public BoundingBoxAttachment(String name) {
      super(name);
   }

   public final Color getColor() {
      return this.color;
   }
}
