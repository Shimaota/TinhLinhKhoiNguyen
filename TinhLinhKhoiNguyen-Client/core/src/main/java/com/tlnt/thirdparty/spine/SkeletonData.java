package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;

public final class SkeletonData {
   @Null
   public String name;
   public final Array<BoneData> bones = new Array<>();
   public final Array<SlotData> slots = new Array<>();
   public final Array<Skin> skins = new Array<>();
   @Null
   public Skin defaultSkin;
   public final Array<EventData> events = new Array<>();
   public final Array<Animation> animations = new Array<>();
   public final Array<IkConstraintData> ikConstraints = new Array<>();
   public final Array<TransformConstraintData> transformConstraints = new Array<>();
   public final Array<PathConstraintData> pathConstraints = new Array<>();
   public final Array<PhysicsConstraintData> physicsConstraints = new Array<>();
   public float x;
   public float y;
   public float width;
   public float height;
   public float referenceScale = 100.0F;
   @Null
   public String version;
   @Null
   public String hash;
   public float fps = 30.0F;
   @Null
   public String imagesPath;
   @Null
   public String audioPath;

   @Null
   public final Skin findSkin(String skinName) {
      if (skinName == null) {
         throw new IllegalArgumentException("skinName cannot be null.");
      } else {
         Array.ArrayIterator iterator = this.skins.iterator();

         while (iterator.hasNext()) {
            Skin skin;
            if ((skin = (Skin)iterator.next()).name.equals(skinName)) {
               return skin;
            }
         }

         return null;
      }
   }

   @Null
   public final Animation findAnimation(String animationName) {
      if (animationName == null) {
         throw new IllegalArgumentException("animationName cannot be null.");
      } else {
         Object[] animations = this.animations.items;
         int i = 0;

         for (int n = this.animations.size; i < n; i++) {
            Animation animation;
            if ((animation = (Animation)animations[i]).name.equals(animationName)) {
               return animation;
            }
         }

         return null;
      }
   }

   @Override
   public final String toString() {
      return this.name != null ? this.name : super.toString();
   }

}
