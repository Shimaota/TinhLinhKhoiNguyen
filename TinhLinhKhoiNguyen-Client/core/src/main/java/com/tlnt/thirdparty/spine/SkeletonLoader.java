package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.tlnt.thirdparty.spine.attachment.AtlasAttachmentLoader;

public abstract class SkeletonLoader {
   public final AtlasAttachmentLoader attachmentLoader;
   public float scale = 1.0F;

   public SkeletonLoader(TextureAtlas atlas) {
      this.attachmentLoader = new AtlasAttachmentLoader(atlas);
   }

   public final void setScale(float scale) {
      if (scale == 0.0F) {
         throw new IllegalArgumentException("scale cannot be 0.");
      } else {
         this.scale = scale;
      }
   }

}
