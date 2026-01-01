package com.tlnt.engine.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.tlnt.core.GameState;
import com.tlnt.core.TaskScheduler;
import com.tlnt.util.AesDecryptor;
import com.tlnt.util.AssetBundleReader;
import com.tlnt.util.FileStorage;
import com.tlnt.util.GameUtils;
import com.tlnt.util.HttpClient;
import java.util.concurrent.TimeUnit;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

public final class TextureAsset implements Disposable {
   public static final ObjectMap<String, TextureAsset> CACHE = new ObjectMap<>();
   public static final ObjectMap<String, TextureAtlas> ATLAS_CACHE = new ObjectMap<>();
   public String path;
   public boolean loaded;
   public TextureRegion region;
   public long lastFetchTime;
   public long lastAccessTime;

   public static TextureAsset obtain(String path) {
      TextureAsset asset;
      if ((asset = CACHE.get(path)) == null) {
         TextureAsset created = asset = new TextureAsset(path);
         created.loaded = false;
         CACHE.put(path, asset);
         asset.getRegion();
      }

      return asset;
   }

   public TextureAsset() {
   }

   public TextureAsset(TextureAsset source, int x, int y, int width, int height) {
      this.region = new TextureRegion(source.getRegion(), x, y, width, height);
   }

   public TextureAsset(String path) {
      this.path = path;
   }

   public final int getWidth() {
      return this.region != null ? this.region.getRegionWidth() : 0;
   }

   public final int getHeight() {
      if (this.region != null) {
         return this.region.getRegionHeight();
      } else {
         return 0;
      }
   }

   public final TextureRegion getRegion() {
      this.lastAccessTime = System.currentTimeMillis();
      if (this.region == null && this.path != null) {
         // Item data from the server uses Windows-style paths (icon\101): Windows accepts '\', Android / iOS do not.
         String normalized = this.path.replace('\\', '/');
         String fileName;
         String pngName = fileName = normalized.endsWith(".png") ? normalized : normalized + ".png";
         if (pngName.contains("/")) {
            String atlasName = fileName.substring(0, fileName.lastIndexOf("/"));
            int nameStart = fileName.lastIndexOf("/") + 1;
            String regionName = fileName.substring(nameStart, fileName.lastIndexOf(".png"));
            TextureAtlas atlas;
            if ((atlas = ATLAS_CACHE.get(atlasName)) == null) {
               FileHandle atlasFile;
               if ((atlasFile = Gdx.files.internal("images/" + atlasName + ".atlas")).exists()) {
                  atlas = new TextureAtlas(atlasFile);
                  ATLAS_CACHE.put(atlasName, atlas);
               } else if ((atlasFile = FileStorage.getExternalFile(atlasName + ".atlas")).exists()) {
                  atlas = new TextureAtlas(atlasFile);
                  ATLAS_CACHE.put(atlasName, atlas);
               }
            }

            if (atlas != null) {
               this.region = atlas.findRegion(regionName);
               return this.region;
            }
         }

         if (this.path.startsWith("http")) {
            if (GameUtils.hasElapsed(this.lastFetchTime, 5000)) {
               this.lastFetchTime = System.currentTimeMillis();
               TaskScheduler.schedule(() -> {
                  byte[] bytes = HttpClient.downloadBytes(this.path, 1, null, null);
                  Gdx.app.postRunnable(() -> this.applyTextureBytes(bytes));
               }, 0L, TimeUnit.MILLISECONDS);
            }
         } else {
            FileHandle file;
            if ((file = Gdx.files.internal("images/" + fileName)).exists()) {
               this.region = new TextureRegion(new Texture(file));
            } else if ((file = FileStorage.getExternalFile(fileName)).exists()) {
               if (GameState.textureDecryptKey != null || GameUtils.hasElapsed(this.lastFetchTime, 1000)) {
                  this.lastFetchTime = System.currentTimeMillis();
                  byte[] fileBytes = file.readBytes();

                  try {
                     this.region = new TextureRegion(GameUtils.decodeTexture(fileBytes));
                  } catch (Exception e) {
                     if (GameState.textureDecryptKey != null && GameState.textureDecryptKey.length > 0) {
                        try {
                           byte[] decrypted = AesDecryptor.decryptAesCbc(fileBytes, GameState.textureDecryptKey);
                           if (decrypted != null) {
                              this.region = new TextureRegion(GameUtils.decodeTexture(decrypted));
                           }
                        } catch (Throwable e2) {
                           logFailure(fileName, "giai ma loi (key " + GameState.textureDecryptKey.length + " byte, file " + fileBytes.length + " byte): " + e2);
                        }
                     }
                  }
               }
            } else {
               FileHandle indexFile;
               if (!(indexFile = Gdx.files.internal("packs/images.index")).exists()) {
                  indexFile = FileStorage.getExternalFile("packs/images.index");
               }

               byte[] entryBytes;
               if (indexFile.exists() && (entryBytes = AssetBundleReader.readBundleEntry(indexFile, fileName)) != null) {
                  this.region = new TextureRegion(createTexture(entryBytes));
               } else {
                  logFailure(fileName, "khong thay (" + FileStorage.getExternalFile(fileName).path() + ")");
               }
            }
         }

         if (this.region != null) {
            this.region.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
         }
      }

      return this.region;
   }

   private static final ObjectMap<String, Boolean> LOGGED_FAILURES = new ObjectMap<>();

   // getRegion retries every frame while the image is missing -> log each path only once
   private static void logFailure(String fileName, String reason) {
      if (LOGGED_FAILURES.containsKey(fileName)) return;
      LOGGED_FAILURES.put(fileName, Boolean.TRUE);
      Gdx.app.error("TextureAsset", fileName + ": " + reason);
   }

   public final void applyTextureBytes(byte[] bytes) {
      this.region = new TextureRegion(createTexture(bytes));
   }

   public static Texture createTexture(byte[] bytes) {
      Pixmap pixmap;
      try {
         pixmap = new Pixmap(bytes, 0, bytes.length);
      } catch (Exception e) {
         pixmap = new Pixmap(bytes, 0, bytes.length);
      }

      Texture texture = new Texture(pixmap);
      pixmap.dispose();
      return texture;
   }

   @Override
   public final void dispose() {
      if (this.region != null && this.region.getTexture() != null) {
         this.region.getTexture().dispose();
      }

      this.path = this.path;
      this.region = null;
   }

}
