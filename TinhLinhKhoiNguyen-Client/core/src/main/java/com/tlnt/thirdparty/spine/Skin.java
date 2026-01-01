package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.OrderedSet;
import com.tlnt.thirdparty.spine.attachment.Attachment;

public final class Skin {
   public final String name;
   public final OrderedSet<Skin.SkinEntry> attachments = new OrderedSet<>();
   public final Array<BoneData> bones;
   public final Array<ConstraintData> constraints;
   public final Skin.SkinEntry lookup;
   public final Color color;

   public Skin(String name) {
      this.bones = new Array<>(0);
      this.constraints = new Array<>(0);
      this.lookup = new Skin.SkinEntry(0, "", null);
      this.color = new Color(0.99607843F, 0.61960787F, 0.30980393F, 1.0F);
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null.");
      } else {
         this.name = name;
         this.attachments.orderedItems().ordered = false;
      }
   }

   public final void setAttachment(int slotIndex, String name, Attachment attachment) {
      if (attachment == null) {
         throw new IllegalArgumentException("attachment cannot be null.");
      } else {
         Skin.SkinEntry newEntry = new Skin.SkinEntry(slotIndex, name, attachment);
         if (!this.attachments.add(newEntry)) {
            this.attachments.get(newEntry).attachment = attachment;
         }
      }
   }

   public final void addSkin(Skin skin) {
      if (skin == null) {
         throw new IllegalArgumentException("skin cannot be null.");
      } else {
         for (BoneData boneData : skin.bones) {
            if (!this.bones.contains(boneData, true)) {
               this.bones.add(boneData);
            }
         }

         for (ConstraintData constraintData : skin.constraints) {
            if (!this.constraints.contains(constraintData, true)) {
               this.constraints.add(constraintData);
            }
         }

         for (Skin.SkinEntry entry : skin.attachments.orderedItems()) {
            this.setAttachment(entry.slotIndex, entry.name, entry.attachment);
         }
      }
   }

   @Null
   public final Attachment getAttachment(int slotIndex, String name) {
      this.lookup.set(slotIndex, name);
      Skin.SkinEntry entry;
      return (entry = this.attachments.get(this.lookup)) != null ? entry.attachment : null;
   }

   @Override
   public final String toString() {
      return this.name;
   }

   public static class SkinEntry {
      public int slotIndex;
      public String name;
      @Null
      public Attachment attachment;
      public int hashCode;

      public SkinEntry(int slotIndex, String name, @Null Attachment attachment) {
         this.set(slotIndex, name);
         this.attachment = attachment;
      }

      public final void set(int slotIndex, String name) {
         if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be >= 0.");
         } else if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
         } else {
            this.slotIndex = slotIndex;
            this.name = name;
            this.hashCode = name.hashCode() + slotIndex * 37;
         }
      }

      @Override
      public final int hashCode() {
         return this.hashCode;
      }

      @Override
      public final boolean equals(Object object) {
         if (object == null) {
            return false;
         } else {
            Skin.SkinEntry other = (Skin.SkinEntry)object;
            return ((this.slotIndex != other.slotIndex ? 0 : (this.name.equals(other.name) ? 1 : 0)) != 0);
         }
      }

      @Override
      public final String toString() {
         return this.slotIndex + ":" + this.name;
      }

   }
}
