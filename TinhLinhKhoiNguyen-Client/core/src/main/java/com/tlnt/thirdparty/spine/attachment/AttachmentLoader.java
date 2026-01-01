package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.utils.Null;

public interface AttachmentLoader {
   @Null
   RegionAttachment newRegionAttachment(String name, String path, @Null Sequence sequence);

   @Null
   MeshAttachment newMeshAttachment(String name, String path, @Null Sequence sequence);

   @Null
   BoundingBoxAttachment newBoundingBoxAttachment(String name);

   @Null
   ClippingAttachment newClippingAttachment(String name);

   @Null
   PathAttachment newPathAttachment(String name);

   @Null
   PointAttachment newPointAttachment(String name);
}
