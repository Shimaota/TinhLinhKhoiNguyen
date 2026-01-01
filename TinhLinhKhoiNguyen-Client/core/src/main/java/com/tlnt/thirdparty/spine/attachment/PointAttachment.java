package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.graphics.Color;

public final class PointAttachment extends Attachment {
   public Color bones = new Color(0.9451F, 0.9451F, 0.0F, 1.0F);

   public PointAttachment(String name) {
      super(name);
   }

   public final Color getColor() {
      return this.bones;
   }
}
