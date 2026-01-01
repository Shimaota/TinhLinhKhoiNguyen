package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Null;

public interface HasTextureRegion {
   TextureRegion getRegion();

   void setRegion(TextureRegion region);

   void updateRegion();

   @Null
   Sequence getSequence();
}
