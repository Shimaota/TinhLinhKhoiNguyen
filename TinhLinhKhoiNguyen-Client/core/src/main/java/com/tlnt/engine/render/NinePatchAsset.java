package com.tlnt.engine.render;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.utils.ObjectMap;

public final class NinePatchAsset {
   public static final ObjectMap<String, NinePatchAsset> CACHE = new ObjectMap<>();
   public TextureAsset texture;
   public int left;
   public int right;
   public int top;
   public int bottom;
   public NinePatch ninePatch;

   public static NinePatchAsset obtain(String path, int pad) {
      String key = path + "." + pad + "." + pad + "." + pad + "." + pad;
      Object asset;
      if ((asset = CACHE.get(key)) == null) {
         asset = new NinePatchAsset(TextureAsset.obtain(path), pad, pad, pad, pad);
         CACHE.put(key, (NinePatchAsset)asset);
      }

      return (NinePatchAsset)asset;
   }

   public NinePatchAsset(TextureAsset texture, int left, int right, int top, int bottom) {
      this.texture = texture;
      this.left = left;
      this.right = right;
      this.top = top;
      this.bottom = bottom;
   }

   public final NinePatch getNinePatch() {
      if (this.ninePatch == null && this.texture != null && this.texture.getRegion() != null) {
         this.ninePatch = new NinePatch(this.texture.getRegion(), this.left, this.right, this.top, this.bottom);
      }

      return this.ninePatch;
   }
}
