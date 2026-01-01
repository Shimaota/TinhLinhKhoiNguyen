package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DataInput;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.SerializationException;
import com.tlnt.thirdparty.spine.attachment.Attachment;
import com.tlnt.thirdparty.spine.attachment.AttachmentType;
import com.tlnt.thirdparty.spine.attachment.BoundingBoxAttachment;
import com.tlnt.thirdparty.spine.attachment.ClippingAttachment;
import com.tlnt.thirdparty.spine.attachment.MeshAttachment;
import com.tlnt.thirdparty.spine.attachment.PathAttachment;
import com.tlnt.thirdparty.spine.attachment.PointAttachment;
import com.tlnt.thirdparty.spine.attachment.RegionAttachment;
import com.tlnt.thirdparty.spine.attachment.Sequence;
import com.tlnt.thirdparty.spine.attachment.VertexAttachment;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public final class SkeletonBinary extends SkeletonLoader {
   public final Array<SkeletonBinary.LinkedMesh> linkedMeshes = new Array<>();
   public static String[] strings;

   public SkeletonBinary(TextureAtlas atlas) {
      super(atlas);
   }

   public final SkeletonData readSkeletonData(FileHandle file) {
      if (file == null) {
         throw new IllegalArgumentException("file cannot be null.");
      } else {
         SkeletonData skeletonData;
         (skeletonData = this.readSkeletonData(file.read())).name = file.nameWithoutExtension();
         return skeletonData;
      }
   }

   public SkeletonData readSkeletonData(InputStream dataInput) {
      if (dataInput == null) {
         throw new IllegalArgumentException("dataInput cannot be null.");
      } else {
         float scale = super.scale;
         SkeletonBinary.SkeletonInput input = new SkeletonBinary.SkeletonInput(dataInput);
         SkeletonData skeletonData = new SkeletonData();

         try {
            long hash = input.readLong();
            skeletonData.hash = hash == 0L ? null : Long.toString(hash);
            skeletonData.version = input.readString();
            if (skeletonData.version.isEmpty()) {
               skeletonData.version = null;
            }

            skeletonData.x = input.readFloat();
            skeletonData.y = input.readFloat();
            skeletonData.width = input.readFloat();
            skeletonData.height = input.readFloat();
            skeletonData.referenceScale = input.readFloat() * scale;
            boolean nonessential;
            if (nonessential = input.readBoolean()) {
               skeletonData.fps = input.readFloat();
               skeletonData.imagesPath = input.readString();
               if (skeletonData.imagesPath.isEmpty()) {
                  skeletonData.imagesPath = null;
               }

               skeletonData.audioPath = input.readString();
               if (skeletonData.audioPath.isEmpty()) {
                  skeletonData.audioPath = null;
               }
            }

            int stringCount;
            String[] stringTable = input.strings = new String[stringCount = input.readInt(true)];

            for (int i = 0; i < stringCount; i++) {
               stringTable[i] = input.readString();
            }

            int boneCount;
            Object[] bones = skeletonData.bones.setSize(boneCount = input.readInt(true));

            for (int i = 0; i < boneCount; i++) {
               String name = input.readString();
               BoneData parent = i == 0 ? null : (BoneData)bones[input.readInt(true)];
               BoneData data;
               (data = new BoneData(i, name, parent)).rotation = input.readFloat();
               data.x = input.readFloat() * scale;
               data.y = input.readFloat() * scale;
               data.scaleX = input.readFloat();
               data.scaleY = input.readFloat();
               data.shearX = input.readFloat();
               data.shearY = input.readFloat();
               data.length = input.readFloat() * scale;
               data.inherit = BoneData.Inherit.VALUES[input.readByte()];
               data.skinRequired = input.readBoolean();
               if (nonessential) {
                  Color.rgba8888ToColor(data.color, input.readInt());
                  data.icon = input.readString();
                  data.visible = input.readBoolean();
               }

               bones[i] = data;
            }

            int slotCount;
            Object[] slots = skeletonData.slots.setSize(slotCount = input.readInt(true));

            for (int i = 0; i < slotCount; i++) {
               String name = input.readString();
               BoneData boneData = (BoneData)bones[input.readInt(true)];
               SlotData data;
               Color.rgba8888ToColor((data = new SlotData(i, name, boneData)).color, input.readInt());
               int darkColor;
               int darkColorValue = darkColor = input.readInt();
               if (darkColorValue != -1) {
                  Color.rgb888ToColor(data.darkColor = new Color(), darkColor);
               }

               data.attachmentName = input.readStringRef();
               data.blendMode = BlendMode.VALUES[input.readInt(true)];
               if (nonessential) {
                  data.visible = input.readBoolean();
               }

               slots[i] = data;
            }

            int ikCount;
            Object[] ikConstraints = skeletonData.ikConstraints.setSize(ikCount = input.readInt(true));

            for (int i = 0; i < ikCount; i++) {
               IkConstraintData data;
               (data = new IkConstraintData(input.readString())).order = input.readInt(true);
               int nn;
               Object[] constraintBones = data.bones.setSize(nn = input.readInt(true));

               for (int ii = 0; ii < nn; ii++) {
                  constraintBones[ii] = bones[input.readInt(true)];
               }

               data.target = (BoneData)bones[input.readInt(true)];
               int flags = input.read();
               data.skinRequired = (((flags & 1) != 0 ? 1 : 0) != 0);
               data.bendDirection = (flags & 2) != 0 ? 1 : -1;
               int bit;
               if ((flags & 4) != 0) {
                  bit = 1;
               } else {
                  bit = 0;
               }

               data.compress = (bit != 0);
               if ((flags & 8) != 0) {
                  bit = 1;
               } else {
                  bit = 0;
               }

               data.stretch = (bit != 0);
               if ((flags & 16) != 0) {
                  bit = 1;
               } else {
                  bit = 0;
               }

               data.uniform = (bit != 0);
               if ((flags & 32) != 0) {
                  data.mix = (flags & 64) != 0 ? input.readFloat() : 1.0F;
               }

               if ((flags & 128) != 0) {
                  data.softness = input.readFloat() * scale;
               }

               ikConstraints[i] = data;
            }

            int transformCount;
            Object[] transformConstraints = skeletonData.transformConstraints.setSize(transformCount = input.readInt(true));

            for (int i = 0; i < transformCount; i++) {
               TransformConstraintData data;
               TransformConstraintData newData = data = new TransformConstraintData(input.readString());
               newData.order = input.readInt(true);
               int nn;
               Object[] constraintBones = data.bones.setSize(nn = input.readInt(true));

               for (int ii = 0; ii < nn; ii++) {
                  constraintBones[ii] = bones[input.readInt(true)];
               }

               data.target = (BoneData)bones[input.readInt(true)];
               int flags = input.read();
               data.skinRequired = (((flags & 1) != 0 ? 1 : 0) != 0);
               int bit;
               if ((flags & 2) != 0) {
                  bit = 1;
               } else {
                  bit = 0;
               }

               data.local = (bit != 0);
               if ((flags & 4) != 0) {
                  bit = 1;
               } else {
                  bit = 0;
               }

               data.relative = (bit != 0);
               if ((flags & 8) != 0) {
                  data.offsetRotation = input.readFloat();
               }

               if ((flags & 16) != 0) {
                  data.offsetX = input.readFloat() * scale;
               }

               if ((flags & 32) != 0) {
                  data.offsetY = input.readFloat() * scale;
               }

               if ((flags & 64) != 0) {
                  data.offsetScaleX = input.readFloat();
               }

               if ((flags & 128) != 0) {
                  data.offsetScaleY = input.readFloat();
               }

               if (((flags = input.read()) & 1) != 0) {
                  data.offsetShearY = input.readFloat();
               }

               if ((flags & 2) != 0) {
                  data.mixRotate = input.readFloat();
               }

               if ((flags & 4) != 0) {
                  data.mixX = input.readFloat();
               }

               if ((flags & 8) != 0) {
                  data.mixY = input.readFloat();
               }

               if ((flags & 16) != 0) {
                  data.mixScaleX = input.readFloat();
               }

               if ((flags & 32) != 0) {
                  data.mixScaleY = input.readFloat();
               }

               if ((flags & 64) != 0) {
                  data.mixShearY = input.readFloat();
               }

               transformConstraints[i] = data;
            }

            int pathCount;
            Object[] pathConstraints = skeletonData.pathConstraints.setSize(pathCount = input.readInt(true));

            for (int i = 0; i < pathCount; i++) {
               PathConstraintData data;
               PathConstraintData newData = data = new PathConstraintData(input.readString());
               newData.order = input.readInt(true);
               data.skinRequired = input.readBoolean();
               int nn;
               Object[] constraintBones = data.bones.setSize(nn = input.readInt(true));

               for (int ii = 0; ii < nn; ii++) {
                  constraintBones[ii] = bones[input.readInt(true)];
               }

               data.target = (SlotData)slots[input.readInt(true)];
               int flags = input.read();
               data.positionMode = PathConstraintData.PositionMode.VALUES[flags & 1];
               data.spacingMode = PathConstraintData.SpacingMode.VALUES[flags >> 1 & 3];
               data.rotateMode = PathConstraintData.RotateMode.VALUES[flags >> 3 & 3];
               if ((flags & 128) != 0) {
                  data.offsetRotation = input.readFloat();
               }

               data.position = input.readFloat();
               if (data.positionMode == PathConstraintData.PositionMode.FIXED) {
                  data.position *= scale;
               }

               data.spacing = input.readFloat();
               if (data.spacingMode == PathConstraintData.SpacingMode.LENGTH || data.spacingMode == PathConstraintData.SpacingMode.FIXED) {
                  data.spacing *= scale;
               }

               data.mixRotate = input.readFloat();
               data.mixX = input.readFloat();
               data.mixY = input.readFloat();
               pathConstraints[i] = data;
            }

            int physicsCount;
            Object[] physicsConstraints = skeletonData.physicsConstraints.setSize(physicsCount = input.readInt(true));

            for (int i = 0; i < physicsCount; i++) {
               PhysicsConstraintData data;
               (data = new PhysicsConstraintData(input.readString())).order = input.readInt(true);
               data.bone = (BoneData)bones[input.readInt(true)];
               int flags = input.read();
               int bit;
               if ((flags & 1) != 0) {
                  bit = 1;
               } else {
                  bit = 0;
               }

               data.skinRequired = (bit != 0);
               if ((flags & 2) != 0) {
                  data.x = input.readFloat();
               }

               if ((flags & 4) != 0) {
                  data.y = input.readFloat();
               }

               if ((flags & 8) != 0) {
                  data.rotate = input.readFloat();
               }

               if ((flags & 16) != 0) {
                  data.scaleX = input.readFloat();
               }

               if ((flags & 32) != 0) {
                  data.shearX = input.readFloat();
               }

               data.limit = ((flags & 64) != 0 ? input.readFloat() : 5000.0F) * scale;
               data.step = 1.0F / (float)input.readUnsignedByte();
               data.inertia = input.readFloat();
               data.strength = input.readFloat();
               data.damping = input.readFloat();
               data.massInverse = (flags & 128) != 0 ? input.readFloat() : 1.0F;
               data.wind = input.readFloat();
               data.gravity = input.readFloat();
               int globalFlags = flags = input.read();
               if ((globalFlags & 1) != 0) {
                  data.inertiaGlobal = true;
               }

               if ((flags & 2) != 0) {
                  data.strengthGlobal = true;
               }

               if ((flags & 4) != 0) {
                  data.dampingGlobal = true;
               }

               if ((flags & 8) != 0) {
                  data.massGlobal = true;
               }

               if ((flags & 16) != 0) {
                  data.windGlobal = true;
               }

               if ((flags & 32) != 0) {
                  data.gravityGlobal = true;
               }

               if ((flags & 64) != 0) {
                  data.mixGlobal = true;
               }

               data.mix = (flags & 128) != 0 ? input.readFloat() : 1.0F;
               physicsConstraints[i] = data;
            }

            Skin defaultSkin;
            if ((defaultSkin = this.readSkin(input, skeletonData, true, nonessential)) != null) {
               skeletonData.defaultSkin = defaultSkin;
               skeletonData.skins.add(defaultSkin);
            }

            int skinIndex = skeletonData.skins.size;
            int skinCount;

            for (Object[] skins = skeletonData.skins.setSize(skinCount = skinIndex + input.readInt(true)); skinIndex < skinCount; skinIndex++) {
               skins[skinIndex] = this.readSkin(input, skeletonData, false, nonessential);
            }

            int linkedMeshCount = this.linkedMeshes.size;
            Object[] linkedMeshItems = this.linkedMeshes.items;

            for (int i = 0; i < linkedMeshCount; i++) {
               SkeletonBinary.LinkedMesh linkedMesh = (SkeletonBinary.LinkedMesh)linkedMeshItems[i];
               Attachment parent;
               if ((parent = skeletonData.skins.get(linkedMesh.skinIndex).getAttachment(linkedMesh.slotIndex, linkedMesh.parent)) == null) {
                  throw new SerializationException("Parent mesh not found: " + linkedMesh.parent);
               }

               linkedMesh.mesh.setTimelineAttachment((VertexAttachment)(linkedMesh.inheritTimelines ? (VertexAttachment)parent : linkedMesh.mesh));
               linkedMesh.mesh.setParentMesh((MeshAttachment)parent);
               if (linkedMesh.mesh.getSequence() == null) {
                  linkedMesh.mesh.updateRegion();
               }
            }

            this.linkedMeshes.clear();
            int eventCount;
            Object[] events = skeletonData.events.setSize(eventCount = input.readInt(true));

            for (int i = 0; i < eventCount; i++) {
               EventData data;
               (data = new EventData(input.readString())).intValue = input.readInt(false);
               data.floatValue = input.readFloat();
               data.stringValue = input.readString();
               data.audioPath = input.readString();
               if (data.audioPath != null) {
                  data.volume = input.readFloat();
                  data.balance = input.readFloat();
               }

               events[i] = data;
            }

            int animationCount;
            Object[] animations = skeletonData.animations.setSize(animationCount = input.readInt(true));

            for (int i = 0; i < animationCount; i++) {
               animations[i] = this.readAnimation(input, input.readString(), skeletonData);
            }
         } catch (IOException ex) {
            throw new SerializationException("Error reading skeleton file.", ex);
         } finally {
            try {
               input.close();
            } catch (IOException ignored) {
            }
         }

         return skeletonData;
      }
   }

   @Null
   public Skin readSkin(SkeletonBinary.SkeletonInput input, SkeletonData skeletonData, boolean defaultSkin, boolean nonessential) throws java.io.IOException {
      int slotCount;
      Skin skin;
      if (defaultSkin) {
         if ((slotCount = input.readInt(true)) == 0) {
            return null;
         }

         skin = new Skin("default");
      } else {
         skin = new Skin(input.readString());
         if (nonessential) {
            Color.rgba8888ToColor(skin.color, input.readInt());
         }

         Object[] bones = skin.bones.setSize(input.readInt(true));
         Object[] items = skeletonData.bones.items;
         int i = 0;

         for (int n = skin.bones.size; i < n; i++) {
            bones[i] = items[input.readInt(true)];
         }

         items = skeletonData.ikConstraints.items;
         i = 0;

         for (int n = input.readInt(true); i < n; i++) {
            skin.constraints.add((ConstraintData)items[input.readInt(true)]);
         }

         items = skeletonData.transformConstraints.items;
         i = 0;

         for (int n = input.readInt(true); i < n; i++) {
            skin.constraints.add((ConstraintData)items[input.readInt(true)]);
         }

         items = skeletonData.pathConstraints.items;
         i = 0;

         for (int n = input.readInt(true); i < n; i++) {
            skin.constraints.add((ConstraintData)items[input.readInt(true)]);
         }

         items = skeletonData.physicsConstraints.items;
         i = 0;

         for (int n = input.readInt(true); i < n; i++) {
            skin.constraints.add((ConstraintData)items[input.readInt(true)]);
         }

         skin.constraints.shrink();
         slotCount = input.readInt(true);
      }

      for (int i = 0; i < slotCount; i++) {
         int slotIndex = input.readInt(true);
         int ii = 0;

         for (int nn = input.readInt(true); ii < nn; ii++) {
            String name = input.readStringRef();
            Attachment attachment;
            if ((attachment = this.readAttachment(input, skeletonData, skin, slotIndex, name, nonessential)) != null) {
               skin.setAttachment(slotIndex, name, attachment);
            }
         }
      }

      return skin;
   }

   public Attachment readAttachment(SkeletonBinary.SkeletonInput input, SkeletonData skeletonData, Skin skin, int slotIndex, String attachmentName, boolean nonessential) {
      float scale = this.scale;

      try {
         int flags = input.readByte();
         String name = (flags & 8) != 0 ? input.readStringRef() : attachmentName;
         switch (AttachmentType.VALUES[flags & 7]) {
            case AttachmentType.REGION: {
               String path = (flags & 16) != 0 ? input.readStringRef() : null;
               int color = (flags & 32) != 0 ? input.readInt() : -1;
               Sequence sequence = (flags & 64) != 0 ? readSequence(input) : null;
               float rotation = (flags & 128) != 0 ? input.readFloat() : 0.0F;
               float x = input.readFloat();
               float y = input.readFloat();
               float scaleX = input.readFloat();
               float scaleY = input.readFloat();
               float width = input.readFloat();
               float height = input.readFloat();
               if (path == null) {
                  path = name;
               }

               RegionAttachment region = this.attachmentLoader.newRegionAttachment(name, path, sequence);
               region.setX(x * scale);
               region.setY(y * scale);
               region.setScaleX(scaleX);
               region.setScaleY(scaleY);
               region.setRotation(rotation);
               region.setWidth(width * scale);
               region.setHeight(height * scale);
               Color.rgba8888ToColor(region.getColor(), color);
               region.setSequence(sequence);
               if (sequence == null) {
                  region.updateRegion();
               }

               return region;
            }
            case AttachmentType.BOUNDINGBOX: {
               SkeletonBinary.Vertices vertices = this.readVertices(input, (flags & 16) != 0);
               int color = nonessential ? input.readInt() : 0;
               BoundingBoxAttachment box = this.attachmentLoader.newBoundingBoxAttachment(name);
               box.setWorldVerticesLength(vertices.length);
               box.setVertices(vertices.vertices);
               box.setBones(vertices.bones);
               if (nonessential) {
                  Color.rgba8888ToColor(box.getColor(), color);
               }

               return box;
            }
            case AttachmentType.MESH: {
               String path = (flags & 16) != 0 ? input.readStringRef() : name;
               int color = (flags & 32) != 0 ? input.readInt() : -1;
               Sequence sequence = (flags & 64) != 0 ? readSequence(input) : null;
               int hullLength = input.readInt(true);
               SkeletonBinary.Vertices vertices = this.readVertices(input, (flags & 128) != 0);
               float[] uvs = readFloatArray(input, vertices.length, 1.0F);
               short[] triangles = readShortArray(input, (vertices.length - hullLength - 2) * 3);
               short[] edges = null;
               float width = 0.0F;
               float height = 0.0F;
               if (nonessential) {
                  edges = readShortArray(input, input.readInt(true));
                  width = input.readFloat();
                  height = input.readFloat();
               }

               MeshAttachment mesh = this.attachmentLoader.newMeshAttachment(name, path, sequence);
               Color.rgba8888ToColor(mesh.getColor(), color);
               mesh.setBones(vertices.bones);
               mesh.setVertices(vertices.vertices);
               mesh.setWorldVerticesLength(vertices.length);
               mesh.setTriangles(triangles);
               mesh.setRegionUVs(uvs);
               if (sequence == null) {
                  mesh.updateRegion();
               }

               mesh.setHullLength(hullLength << 1);
               mesh.setSequence(sequence);
               if (nonessential) {
                  mesh.setEdges(edges);
                  mesh.setWidth(width * scale);
                  mesh.setHeight(height * scale);
               }

               return mesh;
            }
            case AttachmentType.LINKEDMESH: {
               String path = (flags & 16) != 0 ? input.readStringRef() : name;
               int color = (flags & 32) != 0 ? input.readInt() : -1;
               Sequence sequence = (flags & 64) != 0 ? readSequence(input) : null;
               boolean inheritTimelines = (flags & 128) != 0;
               int skinIndex = input.readInt(true);
               String parent = input.readStringRef();
               float width = 0.0F;
               float height = 0.0F;
               if (nonessential) {
                  width = input.readFloat();
                  height = input.readFloat();
               }

               MeshAttachment mesh = this.attachmentLoader.newMeshAttachment(name, path, sequence);
               Color.rgba8888ToColor(mesh.getColor(), color);
               mesh.setSequence(sequence);
               if (nonessential) {
                  mesh.setWidth(width * scale);
                  mesh.setHeight(height * scale);
               }

               this.linkedMeshes.add(new SkeletonBinary.LinkedMesh(mesh, skinIndex, slotIndex, parent, inheritTimelines));
               return mesh;
            }
            case AttachmentType.PATH: {
               boolean closed = (flags & 16) != 0;
               boolean constantSpeed = (flags & 32) != 0;
               SkeletonBinary.Vertices vertices = this.readVertices(input, (flags & 64) != 0);
               float[] lengths = new float[vertices.length / 6];

               for (int i = 0; i < lengths.length; i++) {
                  lengths[i] = input.readFloat() * scale;
               }

               int color = nonessential ? input.readInt() : 0;
               PathAttachment path = this.attachmentLoader.newPathAttachment(name);
               path.setClosed(closed);
               path.setConstantSpeed(constantSpeed);
               path.setWorldVerticesLength(vertices.length);
               path.setVertices(vertices.vertices);
               path.setBones(vertices.bones);
               path.setLengths(lengths);
               if (nonessential) {
                  Color.rgba8888ToColor(path.getColor(), color);
               }

               return path;
            }
            case AttachmentType.POINT: {
               input.readFloat();
               input.readFloat();
               input.readFloat();
               int color = nonessential ? input.readInt() : 0;
               PointAttachment point = this.attachmentLoader.newPointAttachment(name);
               if (nonessential) {
                  Color.rgba8888ToColor(point.getColor(), color);
               }

               return point;
            }
            case AttachmentType.CLIPPING: {
               int endSlotIndex = input.readInt(true);
               SkeletonBinary.Vertices vertices = this.readVertices(input, (flags & 16) != 0);
               int color = nonessential ? input.readInt() : 0;
               ClippingAttachment clip = this.attachmentLoader.newClippingAttachment(name);
               clip.setEndSlot((SlotData)skeletonData.slots.get(endSlotIndex));
               clip.setWorldVerticesLength(vertices.length);
               clip.setVertices(vertices.vertices);
               clip.setBones(vertices.bones);
               if (nonessential) {
                  Color.rgba8888ToColor(clip.getColor(), color);
               }

               return clip;
            }
         }
      } catch (Exception ex) {
         ex.printStackTrace();
      }

      return null;
   }

   public static Sequence readSequence(SkeletonBinary.SkeletonInput input) throws java.io.IOException {
      Sequence sequence;
      (sequence = new Sequence(input.readInt(true))).setStart(input.readInt(true));
      sequence.setDigits(input.readInt(true));
      sequence.setSetupIndex(input.readInt(true));
      return sequence;
   }

   public SkeletonBinary.Vertices readVertices(SkeletonBinary.SkeletonInput input, boolean weighted) throws java.io.IOException {
      float scale = super.scale;
      int vertexCount = input.readInt(true);
      SkeletonBinary.Vertices vertices;
      SkeletonBinary.Vertices newVertices = vertices = new SkeletonBinary.Vertices();
      newVertices.length = vertexCount << 1;
      if (!weighted) {
         vertices.vertices = readFloatArray(input, vertices.length, scale);
         return vertices;
      } else {
         FloatArray weights = new FloatArray(vertices.length * 3 * 3);
         IntArray bonesArray = new IntArray(vertices.length * 3);

         for (int i = 0; i < vertexCount; i++) {
            int boneCount = input.readInt(true);
            bonesArray.add(boneCount);

            for (int ii = 0; ii < boneCount; ii++) {
               bonesArray.add(input.readInt(true));
               weights.add(input.readFloat() * scale);
               weights.add(input.readFloat() * scale);
               weights.add(input.readFloat());
            }
         }

         vertices.vertices = weights.toArray();
         vertices.bones = bonesArray.toArray();
         return vertices;
      }
   }

   public static float[] readFloatArray(SkeletonBinary.SkeletonInput input, int n, float scale) throws java.io.IOException {
      float[] array = new float[n];
      if (scale == 1.0F) {
         for (int i = 0; i < n; i++) {
            array[i] = input.readFloat();
         }
      } else {
         for (int i = 0; i < n; i++) {
            array[i] = input.readFloat() * scale;
         }
      }

      return array;
   }

   public static short[] readShortArray(SkeletonBinary.SkeletonInput input, int n) throws java.io.IOException {
      short[] array = new short[n];

      for (int i = 0; i < n; i++) {
         array[i] = (short)input.readInt(true);
      }

      return array;
   }

   public Animation readAnimation(SkeletonBinary.SkeletonInput input, String name, SkeletonData skeletonData) throws java.io.IOException {
      Array timelines = new Array(input.readInt(true));
      float scale = super.scale;
      int i = 0;

      for (int n = input.readInt(true); i < n; i++) {
         int slotIndex = input.readInt(true);
         int ii = 0;

         label536:
         for (int nn = input.readInt(true); ii < nn; ii++) {
            byte timelineType = input.readByte();
            int frameCount;
            int frameLast = (frameCount = input.readInt(true)) - 1;
            switch (timelineType) {
               case 0:
                  Animation.AttachmentTimeline attachmentTimeline = new Animation.AttachmentTimeline(frameCount, slotIndex);

                  for (int frame = 0; frame < frameCount; frame++) {
                     attachmentTimeline.setFrame(frame, input.readFloat(), input.readStringRef());
                  }

                  timelines.add(attachmentTimeline);
                  break;
               case 1:
                  Animation.RGBATimeline rgbaTimeline = new Animation.RGBATimeline(frameCount, input.readInt(true), slotIndex);
                  float rgbaTime = input.readFloat();
                  float rgbaR = (float)input.read() / 255.0F;
                  float rgbaG = (float)input.read() / 255.0F;
                  float rgbaB = (float)input.read() / 255.0F;
                  float rgbaA = (float)input.read() / 255.0F;
                  int rgbaFrame = 0;
                  int rgbaBezier = 0;

                  while (true) {
                     rgbaTimeline.setFrame(rgbaFrame, rgbaTime, rgbaR, rgbaG, rgbaB, rgbaA);
                     if (rgbaFrame == frameLast) {
                        timelines.add(rgbaTimeline);
                        continue label536;
                     }

                     float rgbaTime2 = input.readFloat();
                     float rgbaR2 = (float)input.read() / 255.0F;
                     float rgbaG2 = (float)input.read() / 255.0F;
                     float rgbaB2 = (float)input.read() / 255.0F;
                     float rgbaA2 = (float)input.read() / 255.0F;
                     switch (input.readByte()) {
                        case 1:
                           rgbaTimeline.PROPERTY_IDS[rgbaFrame] = 1.0F;
                           break;
                        case 2:
                           int rgbaBezierIndex = rgbaBezier++;
                           setBezier(input, rgbaTimeline, rgbaBezierIndex, rgbaFrame, 0, rgbaTime, rgbaTime2, rgbaR, rgbaR2, 1.0F);
                           rgbaBezierIndex = rgbaBezier++;
                           setBezier(input, rgbaTimeline, rgbaBezierIndex, rgbaFrame, 1, rgbaTime, rgbaTime2, rgbaG, rgbaG2, 1.0F);
                           setBezier(input, rgbaTimeline, rgbaBezier++, rgbaFrame, 2, rgbaTime, rgbaTime2, rgbaB, rgbaB2, 1.0F);
                           rgbaBezierIndex = rgbaBezier++;
                           setBezier(input, rgbaTimeline, rgbaBezierIndex, rgbaFrame, 3, rgbaTime, rgbaTime2, rgbaA, rgbaA2, 1.0F);
                     }

                     rgbaTime = rgbaTime2;
                     rgbaR = rgbaR2;
                     rgbaG = rgbaG2;
                     rgbaB = rgbaB2;
                     rgbaA = rgbaA2;
                     rgbaFrame++;
                  }
               case 2:
                  Animation.RGBTimeline rgbTimeline = new Animation.RGBTimeline(frameCount, input.readInt(true), slotIndex);
                  float rgbTime = input.readFloat();
                  float rgbR = (float)input.read() / 255.0F;
                  float rgbG = (float)input.read() / 255.0F;
                  float rgbB = (float)input.read() / 255.0F;
                  int rgbFrame = 0;
                  int rgbBezier = 0;

                  while (true) {
                     rgbTimeline.setFrame(rgbFrame, rgbTime, rgbR, rgbG, rgbB);
                     if (rgbFrame == frameLast) {
                        timelines.add(rgbTimeline);
                        continue label536;
                     }

                     float rgbTime2 = input.readFloat();
                     float rgbR2 = (float)input.read() / 255.0F;
                     float rgbG2 = (float)input.read() / 255.0F;
                     float rgbB2 = (float)input.read() / 255.0F;
                     switch (input.readByte()) {
                        case 1:
                           rgbTimeline.PROPERTY_IDS[rgbFrame] = 1.0F;
                           break;
                        case 2:
                           int rgbBezierIndex = rgbBezier++;
                           setBezier(input, rgbTimeline, rgbBezierIndex, rgbFrame, 0, rgbTime, rgbTime2, rgbR, rgbR2, 1.0F);
                           setBezier(input, rgbTimeline, rgbBezier++, rgbFrame, 1, rgbTime, rgbTime2, rgbG, rgbG2, 1.0F);
                           rgbBezierIndex = rgbBezier++;
                           setBezier(input, rgbTimeline, rgbBezierIndex, rgbFrame, 2, rgbTime, rgbTime2, rgbB, rgbB2, 1.0F);
                     }

                     rgbTime = rgbTime2;
                     rgbR = rgbR2;
                     rgbG = rgbG2;
                     rgbB = rgbB2;
                     rgbFrame++;
                  }
               case 3:
                  Animation.RGBA2Timeline rgba2Timeline = new Animation.RGBA2Timeline(frameCount, input.readInt(true), slotIndex);
                  float rgba2Time = input.readFloat();
                  float lightR = (float)input.read() / 255.0F;
                  float lightG = (float)input.read() / 255.0F;
                  float lightB = (float)input.read() / 255.0F;
                  float lightA = (float)input.read() / 255.0F;
                  float darkR = (float)input.read() / 255.0F;
                  float darkG = (float)input.read() / 255.0F;
                  float darkB = (float)input.read() / 255.0F;
                  int rgba2Frame = 0;
                  int rgba2Bezier = 0;

                  while (true) {
                     rgba2Timeline.setFrame(rgba2Frame, rgba2Time, lightR, lightG, lightB, lightA, darkR, darkG, darkB);
                     if (rgba2Frame == frameLast) {
                        timelines.add(rgba2Timeline);
                        continue label536;
                     }

                     float rgba2Time2 = input.readFloat();
                     float lightR2 = (float)input.read() / 255.0F;
                     float lightG2 = (float)input.read() / 255.0F;
                     float lightB2 = (float)input.read() / 255.0F;
                     float lightA2 = (float)input.read() / 255.0F;
                     float darkR2 = (float)input.read() / 255.0F;
                     float darkG2 = (float)input.read() / 255.0F;
                     float darkB2 = (float)input.read() / 255.0F;
                     switch (input.readByte()) {
                        case 1:
                           rgba2Timeline.PROPERTY_IDS[rgba2Frame] = 1.0F;
                           break;
                        case 2:
                           setBezier(input, rgba2Timeline, rgba2Bezier++, rgba2Frame, 0, rgba2Time, rgba2Time2, lightR, lightR2, 1.0F);
                           setBezier(input, rgba2Timeline, rgba2Bezier++, rgba2Frame, 1, rgba2Time, rgba2Time2, lightG, lightG2, 1.0F);
                           setBezier(input, rgba2Timeline, rgba2Bezier++, rgba2Frame, 2, rgba2Time, rgba2Time2, lightB, lightB2, 1.0F);
                           setBezier(input, rgba2Timeline, rgba2Bezier++, rgba2Frame, 3, rgba2Time, rgba2Time2, lightA, lightA2, 1.0F);
                           int rgba2BezierIndex = rgba2Bezier++;
                           setBezier(input, rgba2Timeline, rgba2BezierIndex, rgba2Frame, 4, rgba2Time, rgba2Time2, darkR, darkR2, 1.0F);
                           setBezier(input, rgba2Timeline, rgba2Bezier++, rgba2Frame, 5, rgba2Time, rgba2Time2, darkG, darkG2, 1.0F);
                           setBezier(input, rgba2Timeline, rgba2Bezier++, rgba2Frame, 6, rgba2Time, rgba2Time2, darkB, darkB2, 1.0F);
                     }

                     rgba2Time = rgba2Time2;
                     lightR = lightR2;
                     lightG = lightG2;
                     lightB = lightB2;
                     lightA = lightA2;
                     darkR = darkR2;
                     darkG = darkG2;
                     darkB = darkB2;
                     rgba2Frame++;
                  }
               case 4:
                  Animation.RGB2Timeline rgb2Timeline = new Animation.RGB2Timeline(frameCount, input.readInt(true), slotIndex);
                  float rgb2Time = input.readFloat();
                  float rgb2LightR = (float)input.read() / 255.0F;
                  float rgb2LightG = (float)input.read() / 255.0F;
                  float rgb2LightB = (float)input.read() / 255.0F;
                  float rgb2DarkR = (float)input.read() / 255.0F;
                  float rgb2DarkG = (float)input.read() / 255.0F;
                  float rgb2DarkB = (float)input.read() / 255.0F;
                  int rgb2Frame = 0;
                  int rgb2Bezier = 0;

                  while (true) {
                     rgb2Timeline.setFrame(rgb2Frame, rgb2Time, rgb2LightR, rgb2LightG, rgb2LightB, rgb2DarkR, rgb2DarkG, rgb2DarkB);
                     if (rgb2Frame == frameLast) {
                        timelines.add(rgb2Timeline);
                        continue label536;
                     }

                     float rgb2Time2 = input.readFloat();
                     float rgb2LightR2 = (float)input.read() / 255.0F;
                     float rgb2LightG2 = (float)input.read() / 255.0F;
                     float rgb2LightB2 = (float)input.read() / 255.0F;
                     float rgb2DarkR2 = (float)input.read() / 255.0F;
                     float rgb2DarkG2 = (float)input.read() / 255.0F;
                     float rgb2DarkB2 = (float)input.read() / 255.0F;
                     switch (input.readByte()) {
                        case 1:
                           rgb2Timeline.PROPERTY_IDS[rgb2Frame] = 1.0F;
                           break;
                        case 2:
                           setBezier(input, rgb2Timeline, rgb2Bezier++, rgb2Frame, 0, rgb2Time, rgb2Time2, rgb2LightR, rgb2LightR2, 1.0F);
                           int rgb2BezierIndex = rgb2Bezier++;
                           setBezier(input, rgb2Timeline, rgb2BezierIndex, rgb2Frame, 1, rgb2Time, rgb2Time2, rgb2LightG, rgb2LightG2, 1.0F);
                           setBezier(input, rgb2Timeline, rgb2Bezier++, rgb2Frame, 2, rgb2Time, rgb2Time2, rgb2LightB, rgb2LightB2, 1.0F);
                           setBezier(input, rgb2Timeline, rgb2Bezier++, rgb2Frame, 3, rgb2Time, rgb2Time2, rgb2DarkR, rgb2DarkR2, 1.0F);
                           setBezier(input, rgb2Timeline, rgb2Bezier++, rgb2Frame, 4, rgb2Time, rgb2Time2, rgb2DarkG, rgb2DarkG2, 1.0F);
                           rgb2BezierIndex = rgb2Bezier++;
                           setBezier(input, rgb2Timeline, rgb2BezierIndex, rgb2Frame, 5, rgb2Time, rgb2Time2, rgb2DarkB, rgb2DarkB2, 1.0F);
                     }

                     rgb2Time = rgb2Time2;
                     rgb2LightR = rgb2LightR2;
                     rgb2LightG = rgb2LightG2;
                     rgb2LightB = rgb2LightB2;
                     rgb2DarkR = rgb2DarkR2;
                     rgb2DarkG = rgb2DarkG2;
                     rgb2DarkB = rgb2DarkB2;
                     rgb2Frame++;
                  }
               case 5:
                  Animation.AlphaTimeline alphaTimeline = new Animation.AlphaTimeline(frameCount, input.readInt(true), slotIndex);
                  float alphaTime = input.readFloat();
                  float alpha = (float)input.read() / 255.0F;
                  int alphaFrame = 0;
                  int alphaBezier = 0;

                  while (true) {
                     alphaTimeline.setFrame(alphaFrame, alphaTime, alpha);
                     if (alphaFrame == frameLast) {
                        timelines.add(alphaTimeline);
                        break;
                     }

                     float alphaTime2 = input.readFloat();
                     float alpha2 = (float)input.read() / 255.0F;
                     switch (input.readByte()) {
                        case 1:
                           alphaTimeline.PROPERTY_IDS[alphaFrame] = 1.0F;
                           break;
                        case 2:
                           setBezier(input, alphaTimeline, alphaBezier++, alphaFrame, 0, alphaTime, alphaTime2, alpha, alpha2, 1.0F);
                     }

                     alphaTime = alphaTime2;
                     alpha = alpha2;
                     alphaFrame++;
                  }
            }
         }
      }

      i = 0;

      for (int n = input.readInt(true); i < n; i++) {
         int boneIndex = input.readInt(true);
         int ii = 0;

         for (int nn = input.readInt(true); ii < nn; ii++) {
            byte type = input.readByte();
            int frameCount = input.readInt(true);
            if (type != 10) {
               int bezierCount = input.readInt(true);
               switch (type) {
                  case 0:
                     this.readTimeline(input, timelines, new Animation.RotateTimeline(frameCount, bezierCount, boneIndex), 1.0F);
                     break;
                  case 1:
                     this.readTimeline(input, timelines, new Animation.TranslateTimeline(frameCount, bezierCount, boneIndex), scale);
                     break;
                  case 2:
                     this.readTimeline(input, timelines, new Animation.TranslateXTimeline(frameCount, bezierCount, boneIndex), scale);
                     break;
                  case 3:
                     this.readTimeline(input, timelines, new Animation.TranslateYTimeline(frameCount, bezierCount, boneIndex), scale);
                     break;
                  case 4:
                     this.readTimeline(input, timelines, new Animation.ScaleTimeline(frameCount, bezierCount, boneIndex), 1.0F);
                     break;
                  case 5:
                     this.readTimeline(input, timelines, new Animation.ScaleXTimeline(frameCount, bezierCount, boneIndex), 1.0F);
                     break;
                  case 6:
                     this.readTimeline(input, timelines, new Animation.ScaleYTimeline(frameCount, bezierCount, boneIndex), 1.0F);
                     break;
                  case 7:
                     this.readTimeline(input, timelines, new Animation.ShearTimeline(frameCount, bezierCount, boneIndex), 1.0F);
                     break;
                  case 8:
                     this.readTimeline(input, timelines, new Animation.ShearXTimeline(frameCount, bezierCount, boneIndex), 1.0F);
                     break;
                  case 9:
                     this.readTimeline(input, timelines, new Animation.ShearYTimeline(frameCount, bezierCount, boneIndex), 1.0F);
               }
            } else {
               Animation.InheritTimeline timeline = new Animation.InheritTimeline(frameCount, boneIndex);

               for (int frame = 0; frame < frameCount; frame++) {
                  timeline.setFrame(frame, input.readFloat(), BoneData.Inherit.VALUES[input.readByte()]);
               }

               timelines.add(timeline);
            }
         }
      }

      i = 0;

      for (int n = input.readInt(true); i < n; i++) {
         int index = input.readInt(true);
         int frameCount;
         int frameCountValue = frameCount = input.readInt(true);
         int frameLast = frameCountValue - 1;
         Animation.IkConstraintTimeline timeline = new Animation.IkConstraintTimeline(frameCount, input.readInt(true), index);
         int flags = input.read();
         float time = input.readFloat();
         float mix = (flags & 1) != 0
            ? ((flags & 2) != 0 ? input.readFloat() : 1.0F)
            : 0.0F;
         float softness = (flags & 4) != 0 ? input.readFloat() * scale : 0.0F;
         int frame = 0;
         int bezier = 0;

         while (true) {
            int bendDirection;
            if ((flags & 8) != 0) {
               bendDirection = 1;
            } else {
               bendDirection = -1;
            }

            int compressBit;
            if ((flags & 16) != 0) {
               compressBit = 1;
            } else {
               compressBit = 0;
            }

            timeline.setFrame(
               frame,
               time,
               mix,
               softness,
               bendDirection,
               (compressBit != 0),
               (((flags & 32) != 0 ? 1 : 0) != 0)
            );
            if (frame == frameLast) {
               timelines.add(timeline);
               break;
            }

            flags = input.read();
            float time2 = input.readFloat();
            float mixValue;
            if ((flags & 1) != 0) {
               mixValue = (flags & 2) != 0 ? input.readFloat() : 1.0F;
            } else {
               mixValue = 0.0F;
            }

            float mix2 = mixValue;
            float softness2 = (flags & 4) != 0 ? input.readFloat() * scale : 0.0F;
            if ((flags & 64) != 0) {
               timeline.PROPERTY_IDS[frame] = 1.0F;
            } else {
               if ((flags & 128) != 0) {
                  int bezierIndex = bezier++;
                  setBezier(input, timeline, bezierIndex, frame, 0, time, time2, mix, mix2, 1.0F);
                  bezierIndex = bezier++;
                  setBezier(input, timeline, bezierIndex, frame, 1, time, time2, softness, softness2, scale);
               }
            }

            time = time2;
            mix = mix2;
            softness = softness2;
            frame++;
         }
      }

      i = 0;

      for (int n = input.readInt(true); i < n; i++) {
         int index = input.readInt(true);
         int frameCount;
         int frameCountValue = frameCount = input.readInt(true);
         int frameLast = frameCountValue - 1;
         Animation.TransformConstraintTimeline timeline = new Animation.TransformConstraintTimeline(
            frameCount, input.readInt(true), index
         );
         float time = input.readFloat();
         float mixRotate = input.readFloat();
         float mixX = input.readFloat();
         float mixY = input.readFloat();
         float mixScaleX = input.readFloat();
         float mixScaleY = input.readFloat();
         float mixShearY = input.readFloat();
         int frame = 0;
         int bezier = 0;

         while (true) {
            timeline.setFrame(frame, time, mixRotate, mixX, mixY, mixScaleX, mixScaleY, mixShearY);
            if (frame == frameLast) {
               timelines.add(timeline);
               break;
            }

            float time2 = input.readFloat();
            float mixRotate2 = input.readFloat();
            float mixX2 = input.readFloat();
            float mixY2 = input.readFloat();
            float mixScaleX2 = input.readFloat();
            float mixScaleY2 = input.readFloat();
            float mixShearY2 = input.readFloat();
            switch (input.readByte()) {
               case 1:
                  timeline.PROPERTY_IDS[frame] = 1.0F;
                  break;
               case 2:
                  setBezier(input, timeline, bezier++, frame, 0, time, time2, mixRotate, mixRotate2, 1.0F);
                  setBezier(input, timeline, bezier++, frame, 1, time, time2, mixX, mixX2, 1.0F);
                  int bezierIndex = bezier++;
                  setBezier(input, timeline, bezierIndex, frame, 2, time, time2, mixY, mixY2, 1.0F);
                  bezierIndex = bezier++;
                  setBezier(input, timeline, bezierIndex, frame, 3, time, time2, mixScaleX, mixScaleX2, 1.0F);
                  setBezier(input, timeline, bezier++, frame, 4, time, time2, mixScaleY, mixScaleY2, 1.0F);
                  bezierIndex = bezier++;
                  setBezier(input, timeline, bezierIndex, frame, 5, time, time2, mixShearY, mixShearY2, 1.0F);
            }

            time = time2;
            mixRotate = mixRotate2;
            mixX = mixX2;
            mixY = mixY2;
            mixScaleX = mixScaleX2;
            mixScaleY = mixScaleY2;
            mixShearY = mixShearY2;
            frame++;
         }
      }

      i = 0;

      for (int n = input.readInt(true); i < n; i++) {
         int index = input.readInt(true);
         PathConstraintData data = skeletonData.pathConstraints.get(index);
         int ii = 0;

         for (int nn = input.readInt(true); ii < nn; ii++) {
            byte type = input.readByte();
            int frameCount = input.readInt(true);
            int bezierCount = input.readInt(true);
            switch (type) {
               case 0:
                  this.readTimeline(
                     input,
                     timelines,
                     new Animation.PathConstraintPositionTimeline(frameCount, bezierCount, index),
                     data.positionMode == PathConstraintData.PositionMode.FIXED ? scale : 1.0F
                  );
                  break;
               case 1:
                  this.readTimeline(
                     input,
                     timelines,
                     new Animation.PathConstraintSpacingTimeline(frameCount, bezierCount, index),
                     data.spacingMode != PathConstraintData.SpacingMode.LENGTH && data.spacingMode != PathConstraintData.SpacingMode.FIXED ? 1.0F : scale
                  );
                  break;
               case 2:
                  Animation.PathConstraintMixTimeline timeline = new Animation.PathConstraintMixTimeline(frameCount, bezierCount, index);
                  float time = input.readFloat();
                  float mixRotate = input.readFloat();
                  float mixX = input.readFloat();
                  float mixY = input.readFloat();
                  int frame = 0;
                  int bezier = 0;
                  int frameLast = timeline.getFrameCount() - 1;

                  while (true) {
                     timeline.setFrame(frame, time, mixRotate, mixX, mixY);
                     if (frame == frameLast) {
                        timelines.add(timeline);
                        break;
                     }

                     float time2 = input.readFloat();
                     float mixRotate2 = input.readFloat();
                     float mixX2 = input.readFloat();
                     float mixY2 = input.readFloat();
                     switch (input.readByte()) {
                        case 1:
                           timeline.PROPERTY_IDS[frame] = 1.0F;
                           break;
                        case 2:
                           setBezier(input, timeline, bezier++, frame, 0, time, time2, mixRotate, mixRotate2, 1.0F);
                           int bezierIndex = bezier++;
                           setBezier(input, timeline, bezierIndex, frame, 1, time, time2, mixX, mixX2, 1.0F);
                           setBezier(input, timeline, bezier++, frame, 2, time, time2, mixY, mixY2, 1.0F);
                     }

                     time = time2;
                     mixRotate = mixRotate2;
                     mixX = mixX2;
                     mixY = mixY2;
                     frame++;
                  }
            }
         }
      }

      i = 0;

      for (int n = input.readInt(true); i < n; i++) {
         int index = input.readInt(true) - 1;
         int ii = 0;

         for (int nn = input.readInt(true); ii < nn; ii++) {
            byte type = input.readByte();
            int frameCount = input.readInt(true);
            if (type != 8) {
               int bezierCount = input.readInt(true);
               switch (type) {
                  case 0:
                     this.readTimeline(input, timelines, new Animation.PhysicsConstraintInertiaTimeline(frameCount, bezierCount, index), 1.0F);
                     break;
                  case 1:
                     this.readTimeline(input, timelines, new Animation.PhysicsConstraintStrengthTimeline(frameCount, bezierCount, index), 1.0F);
                     break;
                  case 2:
                     this.readTimeline(input, timelines, new Animation.PhysicsConstraintDampingTimeline(frameCount, bezierCount, index), 1.0F);
                  case 3:
                  default:
                     break;
                  case 4:
                     this.readTimeline(input, timelines, new Animation.PhysicsConstraintMassTimeline(frameCount, bezierCount, index), 1.0F);
                     break;
                  case 5:
                     this.readTimeline(input, timelines, new Animation.PhysicsConstraintWindTimeline(frameCount, bezierCount, index), 1.0F);
                     break;
                  case 6:
                     this.readTimeline(input, timelines, new Animation.PhysicsConstraintGravityTimeline(frameCount, bezierCount, index), 1.0F);
                     break;
                  case 7:
                     this.readTimeline(input, timelines, new Animation.PhysicsConstraintMixTimeline(frameCount, bezierCount, index), 1.0F);
               }
            } else {
               Animation.PhysicsConstraintResetTimeline timeline = new Animation.PhysicsConstraintResetTimeline(frameCount, index);

               for (int frame = 0; frame < frameCount; frame++) {
                  float time = input.readFloat();
                  timeline.frames[frame] = time;
               }

               timelines.add(timeline);
            }
         }
      }

      i = 0;

      for (int n = input.readInt(true); i < n; i++) {
         Skin skin = skeletonData.skins.get(input.readInt(true));
         int ii = 0;

         for (int nn = input.readInt(true); ii < nn; ii++) {
            int slotIndex = input.readInt(true);
            int iii = 0;

            label395:
            for (int nnn = input.readInt(true); iii < nnn; iii++) {
               String attachmentName = input.readStringRef();
               Attachment attachment;
               if ((attachment = skin.getAttachment(slotIndex, attachmentName)) == null) {
                  throw new SerializationException("Timeline attachment not found: " + attachmentName);
               }

               byte timelineType = input.readByte();
               int frameCount;
               int frameCountValue = frameCount = input.readInt(true);
               int frameLast = frameCountValue - 1;
               switch (timelineType) {
                  case 0:
                     VertexAttachment vertexAttachment;
                     int weighted = (vertexAttachment = (VertexAttachment)attachment).getBones() != null
                        ? 1
                        : 0;
                     float[] vertices = vertexAttachment.getVertices();
                     int deformLength = weighted != 0 ? vertices.length / 3 << 1 : vertices.length;
                     Animation.DeformTimeline deformTimeline = new Animation.DeformTimeline(
                        frameCount, input.readInt(true), slotIndex, vertexAttachment
                     );
                     float time = input.readFloat();
                     int frame = 0;
                     int bezier = 0;

                     while (true) {
                        float[] deform;
                        int end;
                        if ((end = input.readInt(true)) == 0) {
                           deform = weighted != 0 ? new float[deformLength] : vertices;
                        } else {
                           deform = new float[deformLength];
                           int start = input.readInt(true);
                           end += start;
                           if (scale == 1.0F) {
                              for (int v = start; v < end; v++) {
                                 deform[v] = input.readFloat();
                              }
                           } else {
                              for (int v = start; v < end; v++) {
                                 deform[v] = input.readFloat() * scale;
                              }
                           }

                           if (weighted == 0) {
                              int v = 0;

                              for (int vn = deform.length; v < vn; v++) {
                                 deform[v] += vertices[v];
                              }
                           }
                        }

                        deformTimeline.setFrame(frame, time, deform);
                        if (frame == frameLast) {
                           timelines.add(deformTimeline);
                           continue label395;
                        }

                        float time2 = input.readFloat();
                        switch (input.readByte()) {
                           case 1:
                              deformTimeline.PROPERTY_IDS[frame] = 1.0F;
                              break;
                           case 2:
                              int bezierIndex = bezier++;
                              setBezier(input, deformTimeline, bezierIndex, frame, 0, time, time2, 0.0F, 1.0F, 1.0F);
                        }

                        time = time2;
                        frame++;
                     }
                  case 1:
                     Animation.SequenceTimeline sequenceTimeline = new Animation.SequenceTimeline(frameCount, slotIndex, attachment);

                     for (int sequenceFrame = 0; sequenceFrame < frameCount; sequenceFrame++) {
                        float sequenceTime = input.readFloat();
                        int modeAndIndex = input.readInt();
                        int mode = Sequence.SequenceMode.VALUES[modeAndIndex & 15];
                        sequenceTimeline.setFrame(sequenceFrame, sequenceTime, mode, modeAndIndex >> 4, input.readFloat());
                     }

                     timelines.add(sequenceTimeline);
               }
            }
         }
      }

      if ((i = input.readInt(true)) > 0) {
         Animation.DrawOrderTimeline timeline = new Animation.DrawOrderTimeline(i);
         int slotCount = skeletonData.slots.size;

         for (int frame = 0; frame < i; frame++) {
            float time = input.readFloat();
            int offsetCount = input.readInt(true);
            int[] drawOrder = new int[slotCount];

            for (int ii = slotCount - 1; ii >= 0; ii--) {
               drawOrder[ii] = -1;
            }

            int[] unchanged = new int[slotCount - offsetCount];
            int originalIndex = 0;
            int unchangedIndex = 0;

            for (int ii = 0; ii < offsetCount; ii++) {
               int slotIndex = input.readInt(true);

               while (originalIndex != slotIndex) {
                  unchanged[unchangedIndex++] = originalIndex++;
               }

               drawOrder[originalIndex + input.readInt(true)] = originalIndex++;
            }

            while (originalIndex < slotCount) {
               unchanged[unchangedIndex++] = originalIndex++;
            }

            for (int ii = slotCount - 1; ii >= 0; ii--) {
               if (drawOrder[ii] == -1) {
                  drawOrder[ii] = unchanged[--unchangedIndex];
               }
            }

            timeline.setFrame(frame, time, drawOrder);
         }

         timelines.add(timeline);
      }

      int eventCount;
      if ((eventCount = input.readInt(true)) > 0) {
         Animation.EventTimeline timeline = new Animation.EventTimeline(eventCount);

         for (int frame = 0; frame < eventCount; frame++) {
            float time = input.readFloat();
            EventData eventData = skeletonData.events.get(input.readInt(true));
            Event event;
            (event = new Event(time, eventData)).intValue = input.readInt(false);
            event.floatValue = input.readFloat();
            event.stringValue = input.readString();
            if (event.stringValue == null) {
               event.stringValue = eventData.stringValue;
            }

            if (event.getData().audioPath != null) {
               event.volume = input.readFloat();
               event.balance = input.readFloat();
            }

            timeline.setFrame(frame, event);
         }

         timelines.add(timeline);
      }

      float duration = 0.0F;
      Object[] items = timelines.items;
      int timelineIndex = 0;

      for (int n = timelines.size; timelineIndex < n; timelineIndex++) {
         duration = Math.max(duration, ((Animation.Timeline)items[timelineIndex]).getDuration());
      }

      return new Animation(name, timelines, duration);
   }

   public void readTimeline(SkeletonBinary.SkeletonInput input, Array<Animation.Timeline> timelines, Animation.CurveTimeline1 timeline, float scale) throws java.io.IOException {
      float time = input.readFloat();
      float value = input.readFloat() * scale;
      int frame = 0;
      int bezier = 0;
      int frameCount = timeline.getFrameCount();
      int frameLast = frameCount - 1;

      while (true) {
         timeline.setFrame(frame, time, value);
         if (frame == frameLast) {
            timelines.add(timeline);
            return;
         }

         float time2 = input.readFloat();
         float value2 = input.readFloat() * scale;
         switch (input.readByte()) {
            case 1:
               timeline.PROPERTY_IDS[frame] = 1.0F;
               break;
            case 2:
               setBezier(input, timeline, bezier++, frame, 0, time, time2, value, value2, scale);
         }

         time = time2;
         value = value2;
         frame++;
      }
   }

   public void readTimeline(SkeletonBinary.SkeletonInput input, Array<Animation.Timeline> timelines, Animation.CurveTimeline2 timeline, float scale) throws java.io.IOException {
      float time = input.readFloat();
      float value1 = input.readFloat() * scale;
      float value2 = input.readFloat() * scale;
      int frame = 0;
      int bezier = 0;
      int frameCount = timeline.getFrameCount();
      int frameLast = frameCount - 1;

      while (true) {
         timeline.setFrame(frame, time, value1, value2);
         if (frame == frameLast) {
            timelines.add(timeline);
            return;
         }

         float time2 = input.readFloat();
         float nvalue1 = input.readFloat() * scale;
         float nvalue2 = input.readFloat() * scale;
         switch (input.readByte()) {
            case 1:
               timeline.PROPERTY_IDS[frame] = 1.0F;
               break;
            case 2:
               setBezier(input, timeline, bezier++, frame, 0, time, time2, value1, nvalue1, scale);
               int bezierIndex = bezier++;
               setBezier(input, timeline, bezierIndex, frame, 1, time, time2, value2, nvalue2, scale);
         }

         time = time2;
         value1 = nvalue1;
         value2 = nvalue2;
         frame++;
      }
   }

   public static void setBezier(
      SkeletonBinary.SkeletonInput input, Animation.CurveTimeline timeline, int bezier, int frame, int value, float time1, float time2, float value1, float value2, float scale
   ) throws java.io.IOException {
      timeline.setBezier(bezier, frame, value, time1, value1, input.readFloat(), input.readFloat() * scale, input.readFloat(), input.readFloat() * scale, time2, value2);
   }

   public static class LinkedMesh {
      public String parent;
      public int skinIndex;
      public int slotIndex;
      public MeshAttachment mesh;
      public boolean inheritTimelines;

      public LinkedMesh(MeshAttachment mesh, int skinIndex, int slotIndex, String parent, boolean inheritTimelines) {
         this.mesh = mesh;
         this.skinIndex = skinIndex;
         this.slotIndex = slotIndex;
         this.parent = parent;
         this.inheritTimelines = inheritTimelines;
      }
   }

   public static class SkeletonInput extends DataInput {
      public char[] chars = new char[32];
      public String[] strings;

      public SkeletonInput(InputStream input) {
         super(input);
      }

      @Null
      public final String readStringRef() throws java.io.IOException {
         int index;
         return (index = this.readInt(true)) == 0 ? null : this.strings[index - 1];
      }

      @Override
      public final String readString() {
       try {
         int byteCount;
         switch (byteCount = this.readInt(true)) {
            case 0:
               return null;
            case 1:
               return "";
            default:
               if (this.chars.length < --byteCount) {
                  this.chars = new char[byteCount];
               }

               char[] chars = this.chars;
               int charCount = 0;
               int i = 0;

               while (i < byteCount) {
                  int b;
                  int lead = b = this.read();
                  switch (lead >> 4) {
                     case -1:
                        throw new EOFException();
                     case 12:
                     case 13:
                        int index2 = charCount++;
                        int high2 = b & 31;
                        high2 <<= 6;
                        int low2 = this.read();
                        chars[index2] = (char)(high2 | low2 & 63);
                        i += 2;
                        break;
                     case 14:
                        int index3 = charCount++;
                        int high3 = b & 15;
                        high3 <<= 12;
                        int mid3 = this.read();
                        mid3 &= 63;
                        chars[index3] = (char)(high3 | mid3 << 6 | this.read() & 63);
                        i += 3;
                        break;
                     default:
                        chars[charCount++] = (char)b;
                        i++;
                  }
               }

               return new String(chars, 0, charCount);
         }
       } catch (java.io.IOException ex) {
          throw new RuntimeException(ex);
       }
      }
   }

   public static class Vertices {
      public int length;
      public int[] bones;
      public float[] vertices;
   }
}
