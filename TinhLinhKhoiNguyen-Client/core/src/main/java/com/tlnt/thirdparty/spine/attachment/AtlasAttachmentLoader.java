package com.tlnt.thirdparty.spine.attachment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Null;

public final class AtlasAttachmentLoader implements AttachmentLoader {
   public TextureAtlas atlas;
   private static TextureAtlas.AtlasRegion placeholderRegion;

   public AtlasAttachmentLoader(TextureAtlas atlas) {
      if (atlas == null) {
         throw new IllegalArgumentException("atlas cannot be null.");
      } else {
         this.atlas = atlas;
      }
   }

   private static synchronized TextureAtlas.AtlasRegion getPlaceholderRegion() {
      if (placeholderRegion == null) {
         Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
         pixmap.drawPixel(0, 0, 0); // fully transparent pixel
         Texture texture = new Texture(pixmap);
         pixmap.dispose();
         placeholderRegion = new TextureAtlas.AtlasRegion(texture, 0, 0, 1, 1);
         placeholderRegion.name = "placeholder";
      }
      return placeholderRegion;
   }

   private TextureAtlas.AtlasRegion findSafeRegion(String path) {
      if (path == null) return getPlaceholderRegion();
      TextureAtlas.AtlasRegion region = this.atlas.findRegion(path);
      if (region != null) return region;

      // Try stripped filename if path contains directory prefix (e.g. Bruk_5_img/xxx -> xxx)
      if (path.contains("/")) {
         String simpleName = path.substring(path.lastIndexOf('/') + 1);
         region = this.atlas.findRegion(simpleName);
         if (region != null) return region;
      }

      // Try replacing common typos like triple 'lll' with double 'll' (e.g. skulll_clavicle -> skull_clavicle)
      if (path.contains("lll")) {
         String fixed = path.replace("lll", "ll");
         region = this.atlas.findRegion(fixed);
         if (region != null) return region;
         if (fixed.contains("/")) {
            String simpleFixed = fixed.substring(fixed.lastIndexOf('/') + 1);
            region = this.atlas.findRegion(simpleFixed);
            if (region != null) return region;
         }
      }

      Gdx.app.error("AtlasAttachmentLoader", "Region not found in atlas: " + path + " -> using placeholder");
      return getPlaceholderRegion();
   }

   public void loadSequence(String name, String basePath, Sequence sequence) {
      TextureRegion[] regions = sequence.getRegions();
      int i = 0;

      for (int n = regions.length; i < n; i++) {
         String path = sequence.getPath(basePath, i);
         regions[i] = this.findSafeRegion(path);
      }
   }

   @Override
   public final RegionAttachment newRegionAttachment(String name, String path, @Null Sequence sequence) {
      RegionAttachment attachment = new RegionAttachment(name);
      if (sequence != null) {
         this.loadSequence(name, path, sequence);
      } else {
         TextureAtlas.AtlasRegion region = this.findSafeRegion(path);
         attachment.setRegion(region);
      }

      return attachment;
   }

   @Override
   public final MeshAttachment newMeshAttachment(String name, String path, @Null Sequence sequence) {
      MeshAttachment attachment = new MeshAttachment(name);
      if (sequence != null) {
         this.loadSequence(name, path, sequence);
      } else {
         TextureAtlas.AtlasRegion region = this.findSafeRegion(path);
         attachment.setRegion(region);
      }

      return attachment;
   }

   @Override
   public final BoundingBoxAttachment newBoundingBoxAttachment(String name) {
      return new BoundingBoxAttachment(name);
   }

   @Override
   public final ClippingAttachment newClippingAttachment(String name) {
      return new ClippingAttachment(name);
   }

   @Override
   public final PathAttachment newPathAttachment(String name) {
      return new PathAttachment(name);
   }

   @Override
   public final PointAttachment newPointAttachment(String name) {
      return new PointAttachment(name);
   }

}
