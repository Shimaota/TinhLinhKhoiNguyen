package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Array;

public final class IkConstraintData extends ConstraintData {
   public final Array<BoneData> bones = new Array<>();
   public BoneData target;
   public int bendDirection;
   public boolean compress;
   public boolean stretch;
   public boolean uniform;
   public float mix;
   public float softness;

   public IkConstraintData(String name) {
      super(name);
   }
}
