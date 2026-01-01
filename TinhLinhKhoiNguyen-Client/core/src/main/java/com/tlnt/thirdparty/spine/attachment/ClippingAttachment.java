package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Null;
import com.tlnt.thirdparty.spine.SlotData;

public final class ClippingAttachment extends VertexAttachment {
   @Null
   public SlotData color;
   public Color closed = new Color(0.2275F, 0.2275F, 0.8078F, 1.0F);

   public ClippingAttachment(String name) {
      super(name);
   }

   @Null
   public final SlotData getEndSlot() {
      return this.color;
   }

   public final void setEndSlot(@Null SlotData endSlot) {
      this.color = endSlot;
   }

   public final Color getColor() {
      return this.closed;
   }
}
