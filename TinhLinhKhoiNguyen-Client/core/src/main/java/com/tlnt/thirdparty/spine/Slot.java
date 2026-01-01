package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.tlnt.thirdparty.spine.attachment.Attachment;
import com.tlnt.thirdparty.spine.attachment.VertexAttachment;

public final class Slot {
   public final SlotData data;
   public final Bone bone;
   public Color color = new Color();
   @Null
   public final Color darkColor;
   @Null
   public Attachment attachment;
   public int sequenceIndex;
   public FloatArray deform = new FloatArray();
   public int attachmentState;

   public Slot(SlotData data, Bone bone) {
      if (data == null) {
         throw new IllegalArgumentException("data cannot be null.");
      } else if (bone == null) {
         throw new IllegalArgumentException("bone cannot be null.");
      } else {
         this.data = data;
         this.bone = bone;
         this.darkColor = data.darkColor == null ? null : new Color();
         this.setToSetupPose();
      }
   }

   public final SlotData getData() {
      return this.data;
   }

   public final Bone getBone() {
      return this.bone;
   }

   public final Skeleton getSkeleton() {
      return this.bone.skeleton;
   }

   public final void setAttachment(@Null Attachment attachment) {
      if (this.attachment != attachment) {
         if (!(attachment instanceof VertexAttachment)
            || !(this.attachment instanceof VertexAttachment)
            || ((VertexAttachment)attachment).getTimelineAttachment() != ((VertexAttachment)this.attachment).getTimelineAttachment()) {
            this.deform.clear();
         }

         this.attachment = attachment;
         this.sequenceIndex = -1;
      }
   }

   public final int getSequenceIndex() {
      return this.sequenceIndex;
   }

   public final FloatArray getDeform() {
      return this.deform;
   }

   public final void setToSetupPose() {
      this.color.set(this.data.color);
      if (this.darkColor != null) {
         this.darkColor.set(this.data.darkColor);
      }

      if (this.data.attachmentName == null) {
         this.setAttachment(null);
      } else {
         this.attachment = null;
         this.setAttachment(this.bone.skeleton.getAttachment(this.data.index, this.data.attachmentName));
      }
   }

   @Override
   public final String toString() {
      return this.data.name;
   }

}
