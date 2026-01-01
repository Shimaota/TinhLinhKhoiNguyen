package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.tlnt.thirdparty.spine.attachment.Attachment;
import com.tlnt.thirdparty.spine.attachment.PathAttachment;
import com.tlnt.thirdparty.spine.utils.SpineUtils;

public final class Skeleton {
   public final SkeletonData data;
   public final Array<Bone> bones;
   public final Array<Slot> slots;
   public Array<Slot> drawOrder;
   public final Array<IkConstraint> ikConstraints;
   public final Array<TransformConstraint> transformConstraints;
   public final Array<PathConstraint> pathConstraints;
   public final Array<PhysicsConstraint> physicsConstraints;
   public Array<Updatable> updateCache = new Array<>();
   @Null
   public Skin skin;
   public final Color color;
   public float x;
   public float y;
   public float scaleX = 1.0F;
   public float scaleY = 1.0F;
   public float time;

   public Skeleton(SkeletonData data) {
      if (data == null) {
         throw new IllegalArgumentException("data cannot be null.");
      } else {
         this.data = data;
         this.bones = new Array<>(data.bones.size);
         Object[] bones = this.bones.items;
         Array.ArrayIterator boneDataIter = data.bones.iterator();

         while (boneDataIter.hasNext()) {
            BoneData boneData;
            Bone bone;
            if ((boneData = (BoneData)boneDataIter.next()).parent == null) {
               bone = new Bone(boneData, this, null);
            } else {
               Bone parent = (Bone)bones[boneData.parent.index];
               bone = new Bone(boneData, this, parent);
               parent.children.add(bone);
            }

            this.bones.add(bone);
         }

         this.slots = new Array<>(data.slots.size);
         this.drawOrder = new Array<>(data.slots.size);

         for (SlotData slotData : data.slots) {
            Bone slotBone = (Bone)bones[slotData.boneData.index];
            Slot slot = new Slot(slotData, slotBone);
            this.slots.add(slot);
            this.drawOrder.add(slot);
         }

         this.ikConstraints = new Array<>(data.ikConstraints.size);

         for (IkConstraintData ikConstraintData : data.ikConstraints) {
            this.ikConstraints.add(new IkConstraint(ikConstraintData, this));
         }

         this.transformConstraints = new Array<>(data.transformConstraints.size);

         for (TransformConstraintData transformConstraintData : data.transformConstraints) {
            this.transformConstraints.add(new TransformConstraint(transformConstraintData, this));
         }

         this.pathConstraints = new Array<>(data.pathConstraints.size);

         for (PathConstraintData pathConstraintData : data.pathConstraints) {
            this.pathConstraints.add(new PathConstraint(pathConstraintData, this));
         }

         this.physicsConstraints = new Array<>(data.physicsConstraints.size);

         for (PhysicsConstraintData physicsConstraintData : data.physicsConstraints) {
            this.physicsConstraints.add(new PhysicsConstraint(physicsConstraintData, this));
         }

         this.color = new Color(1.0F, 1.0F, 1.0F, 1.0F);
         this.updateCache();
      }
   }

   public void updateCache() {
      Array updateCache = this.updateCache;
      this.updateCache.clear();
      int boneCount = this.bones.size;
      Object[] bones = this.bones.items;

      for (int i = 0; i < boneCount; i++) {
         Bone bone;
         (bone = (Bone)bones[i]).sorted = bone.data.skinRequired;
         int activeFlag;
         if (!bone.sorted) {
            activeFlag = 1;
         } else {
            activeFlag = 0;
         }

         bone.active = (activeFlag != 0);
      }

      if (this.skin != null) {
         Object[] skinBones = this.skin.bones.items;
         int ii = 0;

         for (int nn = this.skin.bones.size; ii < nn; ii++) {
            Bone skinBone = (Bone)bones[((BoneData)skinBones[ii]).index];

            // enable the skin's bones + ALL parent bones (original spine: "while (bone != null)"; the decompiled code had "== null"
            // -> only the last bone was enabled, skinRequired parents were not counted -> mesh vertices ended up at (0, 0), e.g. Bruk's shoulder cloth)
            do {
               skinBone.sorted = false;
               skinBone.active = true;
            } while ((skinBone = skinBone.parent) != null);
         }
      }

      int ikCount = this.ikConstraints.size;
      int transformCount = this.transformConstraints.size;
      int pathCount = this.pathConstraints.size;
      int physicsCount = this.physicsConstraints.size;
      Object[] ikConstraints = this.ikConstraints.items;
      Object[] transformConstraints = this.transformConstraints.items;
      Object[] pathConstraints = this.pathConstraints.items;
      Object[] physicsConstraints = this.physicsConstraints.items;
      int constraintCount = ikCount + transformCount + pathCount + physicsCount;

      label79:
      for (int order = 0; order < constraintCount; order++) {
         for (int ikIndex = 0; ikIndex < ikCount; ikIndex++) {
            IkConstraint ikConstraint;
            if ((ikConstraint = (IkConstraint)ikConstraints[ikIndex]).data.order == order) {
               this.sortIkConstraint(ikConstraint);
               continue label79;
            }
         }


         for (int transformIndex = 0; transformIndex < transformCount; transformIndex++) {
            TransformConstraint transformConstraint;
            if ((transformConstraint = (TransformConstraint)transformConstraints[transformIndex]).data.order == order) {
               this.sortTransformConstraint(transformConstraint);
               continue label79;
            }
         }

         for (int pathIndex = 0; pathIndex < pathCount; pathIndex++) {
            PathConstraint pathConstraint;
            if ((pathConstraint = (PathConstraint)pathConstraints[pathIndex]).data.order == order) {
               this.sortPathConstraint(pathConstraint);
               continue label79;
            }
         }


         for (int physicsIndex = 0; physicsIndex < physicsCount; physicsIndex++) {
            PhysicsConstraint physicsConstraint;
            if ((physicsConstraint = (PhysicsConstraint)physicsConstraints[physicsIndex]).data.order == order) {
               this.sortPhysicsConstraint(physicsConstraint);
               break;
            }
         }
      }


      for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
         this.sortBone((Bone)bones[boneIndex]);
      }
   }

   public void sortIkConstraint(IkConstraint constraint) {
      constraint.active = ((!constraint.target.active
            || constraint.data.skinRequired && (this.skin == null || !this.skin.constraints.contains(constraint.data, true))
         ? 0
         : 1) != 0);
      if (constraint.active) {
         Bone target = constraint.target;
         this.sortBone(target);
         Array constrained = constraint.bones;
         Bone parent = constraint.bones.first();
         this.sortBone(parent);
         if (constrained.size == 1) {
            this.updateCache.add(constraint);
            this.sortReset(parent.children);
         } else {
            target = (Bone)constrained.peek();
            this.sortBone(target);
            this.updateCache.add(constraint);
            this.sortReset(parent.children);
            target.sorted = true;
         }
      }
   }

   public void sortTransformConstraint(TransformConstraint constraint) {
      int activeFlag;
      if (!constraint.target.active
         || constraint.data.skinRequired && (this.skin == null || !this.skin.constraints.contains(constraint.data, true))) {
         activeFlag = 0;
      } else {
         activeFlag = 1;
      }

      constraint.active = (activeFlag != 0);
      if (constraint.active) {
         this.sortBone(constraint.target);
         Object[] constrained = constraint.bones.items;
         int boneCount = constraint.bones.size;
         if (constraint.data.local) {
            for (int i = 0; i < boneCount; i++) {
               Bone child = (Bone)constrained[i];
               this.sortBone(child.parent);
               this.sortBone(child);
            }
         } else {
            for (int ii = 0; ii < boneCount; ii++) {
               this.sortBone((Bone)constrained[ii]);
            }
         }

         this.updateCache.add(constraint);

         for (int j = 0; j < boneCount; j++) {
            this.sortReset(((Bone)constrained[j]).children);
         }

         for (int k = 0; k < boneCount; k++) {
            ((Bone)constrained[k]).sorted = true;
         }
      }
   }

   public void sortPathConstraint(PathConstraint constraint) {
      int activeFlag;
      label64: {
         label63: {
            if (constraint.target.bone.active) {
               if (!constraint.data.skinRequired) {
                  break label63;
               }

               if (this.skin != null) {
                  if (this.skin.constraints.contains(constraint.data, true)) {
                     break label63;
                  }
               }
            }

            activeFlag = 0;
            break label64;
         }

         activeFlag = 1;
      }

      constraint.active = (activeFlag != 0);
      if (constraint.active) {
         Slot slot = constraint.target;
         Slot targetSlot = constraint.target;
         int slotIndex = constraint.target.data.index;
         Bone slotBone = slot.bone;
         if (this.skin != null) {
            this.sortPathSlotAttachments(this.skin, slotIndex, slotBone);
         }

         if (this.data.defaultSkin != null && this.data.defaultSkin != this.skin) {
            this.sortPathSlotAttachments(this.data.defaultSkin, slotIndex, slotBone);
         }

         Attachment attachment;
         if ((attachment = slot.attachment) instanceof PathAttachment) {
            this.sortPathAttachmentBones(attachment, slotBone);
         }

         Object[] constrained = constraint.bones.items;
         int boneCount = constraint.bones.size;

         for (int i = 0; i < boneCount; i++) {
            this.sortBone((Bone)constrained[i]);
         }

         this.updateCache.add(constraint);

         for (int ii = 0; ii < boneCount; ii++) {
            this.sortReset(((Bone)constrained[ii]).children);
         }

         for (int j = 0; j < boneCount; j++) {
            Bone bone = (Bone)constrained[j];
            bone.sorted = true;
         }
      }
   }

   public void sortPathSlotAttachments(Skin skin, int slotIndex, Bone slotBone) {
      Object[] entries = skin.attachments.orderedItems().items;
      int i = 0;

      for (int n = skin.attachments.size; i < n; i++) {
         Skin.SkinEntry entry;
         if ((entry = (Skin.SkinEntry)entries[i]).slotIndex == slotIndex) {
            this.sortPathAttachmentBones(entry.attachment, slotBone);
         }
      }
   }

   public void sortPathAttachmentBones(Attachment attachment, Bone slotBone) {
      if (attachment instanceof PathAttachment) {
         int[] pathBones;
         if ((pathBones = ((PathAttachment)attachment).getBones()) == null) {
            this.sortBone(slotBone);
         } else {
            Object[] bones = this.bones.items;
            int i = 0;
            int n = pathBones.length;

            while (i < n) {
               int nn;
               nn = (nn = pathBones[i++]) + i;

               while (i < nn) {
                  this.sortBone((Bone)bones[pathBones[i++]]);
               }
            }
         }
      }
   }

   public void sortPhysicsConstraint(PhysicsConstraint constraint) {
      Bone bone;
      int activeFlag;
      label23: {
         label22: {
            bone = constraint.bone;
            if (bone.active) {
               if (!constraint.data.skinRequired) {
                  break label22;
               }

               if (this.skin != null) {
                  if (this.skin.constraints.contains(constraint.data, true)) {
                     break label22;
                  }
               }
            }

            activeFlag = 0;
            break label23;
         }

         activeFlag = 1;
      }

      constraint.active = (activeFlag != 0);
      if (constraint.active) {
         this.sortBone(bone);
         this.updateCache.add(constraint);
         this.sortReset(bone.children);
         bone.sorted = true;
      }
   }

   public void sortBone(Bone bone) {
      if (!bone.sorted) {
         Bone parent = bone.parent;
         if (bone.parent != null) {
            this.sortBone(parent);
         }

         bone.sorted = true;
         this.updateCache.add(bone);
      }
   }

   public void sortReset(Array<Bone> bones) {
      Object[] items = bones.items;
      int i = 0;

      for (int n = bones.size; i < n; i++) {
         Bone bone;
         if ((bone = (Bone)items[i]).active) {
            if (bone.sorted) {
               this.sortReset(bone.children);
            }

            bone.sorted = false;
         }
      }
   }

   public final void updateWorldTransform(int physics) {
      Object[] bones = this.bones.items;
      int i = 0;

      for (int n = this.bones.size; i < n; i++) {
         Bone bone;
         (bone = (Bone)bones[i]).ax = bone.x;
         bone.ay = bone.y;
         bone.arotation = bone.rotation;
         bone.ascaleX = bone.scaleX;
         bone.ascaleY = bone.scaleY;
         bone.ashearX = bone.shearX;
         bone.ashearY = bone.shearY;
      }

      Object[] updateCache = this.updateCache.items;
      int ii = 0;

      for (int nn = this.updateCache.size; ii < nn; ii++) {
         ((Updatable)updateCache[ii]).update(physics);
      }
   }

   public final void setSlotsToSetupPose() {
      Object[] slots = this.slots.items;
      int n = this.slots.size;
      SpineUtils.arraycopy(slots, 0, this.drawOrder.items, 0, n);

      for (int i = 0; i < n; i++) {
         ((Slot)slots[i]).setToSetupPose();
      }
   }

   public final SkeletonData getData() {
      return this.data;
   }

   public final Array<Bone> getBones() {
      return this.bones;
   }

   public final Bone getRootBone() {
      return this.bones.size == 0 ? null : this.bones.first();
   }

   public final void setSkin(@Null Skin newSkin) {
      if (newSkin != this.skin) {
         if (this.skin != null) {
            Skin oldSkin = this.skin;
            Skin targetSkin = newSkin;
            Object[] slots = this.slots.items;
            Array.ArrayIterator entryIter = oldSkin.attachments.orderedItems().iterator();

            while (entryIter.hasNext()) {
               Skin.SkinEntry entry;
               int slotIndex = (entry = (Skin.SkinEntry)entryIter.next()).slotIndex;
               Slot slot;
               Attachment attachment;
               if ((slot = (Slot)slots[slotIndex]).attachment == entry.attachment && (attachment = targetSkin.getAttachment(slotIndex, entry.name)) != null) {
                  slot.setAttachment(attachment);
               }
            }
         } else {
            Object[] slotItems = this.slots.items;
            int i = 0;

            for (int n = this.slots.size; i < n; i++) {
               Slot setupSlot;
               String attachmentName;
               Attachment setupAttachment;
               if ((attachmentName = (setupSlot = (Slot)slotItems[i]).data.attachmentName) != null && (setupAttachment = newSkin.getAttachment(i, attachmentName)) != null) {
                  setupSlot.setAttachment(setupAttachment);
               }
            }
         }

         this.skin = newSkin;
         this.updateCache();
      }
   }

   @Null
   public final Attachment getAttachment(int slotIndex, String attachmentName) {
      if (attachmentName == null) {
         throw new IllegalArgumentException("attachmentName cannot be null.");
      } else {
         Attachment attachment;
         if (this.skin != null && (attachment = this.skin.getAttachment(slotIndex, attachmentName)) != null) {
            return attachment;
         } else {
            return this.data.defaultSkin != null ? this.data.defaultSkin.getAttachment(slotIndex, attachmentName) : null;
         }
      }
   }

   public final Color getColor() {
      return this.color;
   }

   public final void setColor(Color color) {
      if (color == null) {
         throw new IllegalArgumentException("color cannot be null.");
      } else {
         this.color.set(color);
      }
   }

   public final void setColorRGBA(float r, float g, float b, float a) {
      this.color.set(1.0F, 1.0F, 1.0F, 0.25F);
   }

   public final void setScale(float scaleX, float scaleY) {
      this.scaleX = scaleX;
      this.scaleY = scaleY;
   }

   public final void setPosition(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public final void update(float delta) {
      this.time += delta;
   }

   @Override
   public final String toString() {
      return this.data.name != null ? this.data.name : super.toString();
   }

   public static final class Physics {
      public static final int NONE = 1;
      public static final int RESET = 2;
      public static final int UPDATE = 3;
      public static final int POSE = 4;
      private static final int[] $VALUES;

      public static int[] values() {
         return (int[])$VALUES.clone();
      }

      static {
         int[] values = new int[4];
         values[0] = NONE;
         values[1] = RESET;
         values[2] = UPDATE;
         values[3] = POSE;
         $VALUES = values;
      }
   }
}
