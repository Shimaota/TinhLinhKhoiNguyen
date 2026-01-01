package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Array;

public final class TransformConstraintData extends ConstraintData {
   public final Array<BoneData> bones = new Array<>();
   public BoneData target;
   public float mixRotate;
   public float mixX;
   public float mixY;
   public float mixScaleX;
   public float mixScaleY;
   public float mixShearY;
   public float offsetRotation;
   public float offsetX;
   public float offsetY;
   public float offsetScaleX;
   public float offsetScaleY;
   public float offsetShearY;
   public boolean relative;
   public boolean local;

   public TransformConstraintData(String name) {
      super(name);
   }
}
