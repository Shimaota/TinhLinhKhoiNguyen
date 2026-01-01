package com.tlnt.core.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.util.AesDecryptor;
import com.tlnt.util.GameUtils;

public final class EncryptedTextureAtlas extends TextureAtlas {

   public EncryptedTextureAtlas(FileHandle atlasFile) {
      super(atlasFile, atlasFile.parent());
   }

   public static Texture loadEncryptedTexture(FileHandle file, Pixmap.Format format, boolean useMipMaps, byte[] key) {
      byte[] fileBytes = null;

      try {
         return GameUtils.decodeTexture(fileBytes = file.readBytes());
      } catch (Exception decodeError) {
         if (fileBytes == null || key == null) {
            // Decryption key not received yet (opcode 59) or the file could not be read.
            Gdx.app.error("EncryptedTextureAtlas", "Khong giai ma duoc " + file.path() + (key == null ? " (chua co khoa opcode 59)" : ""));
            return createPlaceholderTexture();
         }

         try {
            long start = System.currentTimeMillis();
            byte[] decrypted = AesDecryptor.decryptAesCbc(fileBytes, key);
            long decryptedAt = System.currentTimeMillis();
            Pixmap pixmap = new Pixmap(decrypted, 0, decrypted.length);
            Texture texture = new Texture(pixmap, format, useMipMaps);
            pixmap.dispose();
            long end = System.currentTimeMillis();
            // Time the loading of each atlas page (runs on the render thread -> a slow page causes a hitch on map change)
            if (end - start >= 30L) {
               Gdx.app.log("EncryptedTextureAtlas", file.name() + " " + fileBytes.length / 1024 + " KB: giai ma " + (decryptedAt - start)
                  + " ms, tao texture " + (end - decryptedAt) + " ms");
            }
            return texture;
         } catch (Exception e) {
            Gdx.app.error("EncryptedTextureAtlas", "Sai khoa giai ma " + file.path());
            return createPlaceholderTexture();
         }
      }
   }

   /** Private transparent 1x1 texture for this atlas (the atlas can be disposed without affecting anything else). */
   private static Texture createPlaceholderTexture() {
      Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
      Texture texture = new Texture(pixmap);
      pixmap.dispose();
      return texture;
   }

   @Override
   public final void load(TextureAtlas.TextureAtlasData data) {
      this.getTextures().ensureCapacity(data.getPages().size);
      Array.ArrayIterator it = data.getPages().iterator();

      while (it.hasNext()) {
         TextureAtlas.TextureAtlasData.Page page;
         if ((page = (TextureAtlas.TextureAtlasData.Page)it.next()).texture == null) {
            page.texture = loadEncryptedTexture(page.textureFile, page.format, page.useMipMaps, GameState.atlasDecryptKey);
         }

         page.texture.setFilter(page.minFilter, page.magFilter);
         page.texture.setWrap(page.uWrap, page.vWrap);
         this.getTextures().add(page.texture);
      }

      this.getRegions().ensureCapacity(data.getRegions().size);

      for (TextureAtlas.TextureAtlasData.Region region : data.getRegions()) {
         TextureAtlas.AtlasRegion atlasRegion;
         (atlasRegion = new TextureAtlas.AtlasRegion(
               region.page.texture, region.left, region.top, region.rotate ? region.height : region.width, region.rotate ? region.width : region.height
            ))
            .index = region.index;
         atlasRegion.name = region.name;
         atlasRegion.offsetX = region.offsetX;
         atlasRegion.offsetY = region.offsetY;
         atlasRegion.originalHeight = region.originalHeight;
         atlasRegion.originalWidth = region.originalWidth;
         atlasRegion.rotate = region.rotate;
         atlasRegion.degrees = region.degrees;
         atlasRegion.names = region.names;
         atlasRegion.values = region.values;
         if (region.flip) {
            atlasRegion.flip(false, true);
         }

         this.getRegions().add(atlasRegion);
      }
   }

}
