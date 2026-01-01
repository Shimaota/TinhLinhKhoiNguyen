package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Null;

public final class SlotData {
   public final int index;
   public final String name;
   public final BoneData boneData;
   public final Color color = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   @Null
   public Color darkColor;
   @Null
   public String attachmentName;
   public BlendMode blendMode;
   public boolean visible;

   public SlotData(int index, String name, BoneData boneData) {
      this.visible = true;
      if (index < 0) {
         throw new IllegalArgumentException("index must be >= 0.");
      } else if (name == null) {
         throw new IllegalArgumentException("name cannot be null.");
      } else if (boneData == null) {
         throw new IllegalArgumentException("boneData cannot be null.");
      } else {
         this.index = index;
         this.name = name;
         this.boneData = boneData;
      }
   }

   @Override
   public final String toString() {
      return this.name;
   }

}
