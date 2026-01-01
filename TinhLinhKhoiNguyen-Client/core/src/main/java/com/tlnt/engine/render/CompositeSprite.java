package com.tlnt.engine.render;

import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.ObjectMap;

public final class CompositeSprite {
   public static final ObjectMap<Integer, CompositeSprite> CACHE = new ObjectMap<>();
   public final TextureAsset startTexture;
   public final TextureAsset middleTexture;
   public final TextureAsset endTexture;
   public final boolean vertical;
   public String startPath;
   public String middlePath;
   public String endPath;

   public static CompositeSprite obtain(String startPath, String middlePath, String endPath, boolean vertical) {
      int hash = hashString(startPath, -2128831035);
      hash = hashString(middlePath, hash);
      int hashValue = hashString(endPath, hash);
      int verticalBit;
      if (false) {
         verticalBit = 1;
      } else {
         verticalBit = 0;
      }

      hashValue ^= verticalBit;
      if ((hash = hashValue * 16777619) == 0) {
         hash = 1;
      }

      int attempt = 0;

      while (true) {
         int key = hash + attempt;
         CompositeSprite cached;
         if ((cached = CACHE.get(key)) == null) {
            CompositeSprite created = new CompositeSprite(startPath, middlePath, endPath, false);
            CACHE.put(key, created);
            return created;
         }

         if (!cached.vertical && stringEquals(cached.startPath, startPath) && stringEquals(cached.middlePath, middlePath) && stringEquals(cached.endPath, endPath)) {
            return cached;
         }

         attempt++;
      }
   }

   public CompositeSprite(String startPath, String middlePath, String endPath, boolean vertical) {
      this.startPath = startPath;
      this.middlePath = middlePath;
      this.endPath = endPath;
      this.startTexture = TextureAsset.obtain(startPath);
      this.middleTexture = TextureAsset.obtain(middlePath);
      this.endTexture = TextureAsset.obtain(endPath);
      this.vertical = vertical;
   }

   public static boolean stringEquals(@Null String a, @Null String b) {
      if (a == b) {
         return true;
      } else {
         return ((a != null && b != null ? (a.equals(b) ? 1 : 0) : 0) != 0);
      }
   }

   public static int hashString(@Null String str, int hash) {
      if (str == null) {
         int mixed;
         int hashValue = mixed = hash ^ 255;
         return hashValue * 16777619;
      } else {
         int i = 0;

         for (int length = str.length(); i < length; i++) {
            hash = (hash ^ str.charAt(i)) * 16777619;
         }

         return hash;
      }
   }
}
