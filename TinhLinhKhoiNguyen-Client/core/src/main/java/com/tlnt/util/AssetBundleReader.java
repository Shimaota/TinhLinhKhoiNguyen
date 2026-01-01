package com.tlnt.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class AssetBundleReader {
   public static final Map<String, Map<String, AssetBundleReader.BundleEntry>> BUNDLE_CACHE = new HashMap<>();

   public static byte[] readBundleEntry(FileHandle bundleFile, String entryName) {
      String cacheKey = bundleFile.path();
      Map<String, AssetBundleReader.BundleEntry> entries;
      if (BUNDLE_CACHE.containsKey(cacheKey)) {
         entries = BUNDLE_CACHE.get(cacheKey);
      } else {
         ByteBuffer buffer;
         int entryCount = (buffer = ByteBuffer.wrap(bundleFile.readBytes()).order(ByteOrder.BIG_ENDIAN)).getInt();
         int headerCount = buffer.getInt();

         for (int i = 0; i < headerCount; i++) {
            int pos = buffer.position();
            buffer.position(pos + 256);
            buffer.getInt();
         }

         HashMap<String, AssetBundleReader.BundleEntry> parsed = new HashMap<>();
         FileHandle parent = bundleFile.parent();
         String parentDir = parent != null ? parent.path() : null;

         for (int j = 0; j < entryCount; j++) {
            byte[] nameBytes = new byte[256];
            buffer.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8).trim();
            byte[] pathBytes = new byte[256];
            buffer.get(pathBytes);
            String relativePath = new String(pathBytes, StandardCharsets.UTF_8).trim();
            int offset = buffer.getInt();
            int length = buffer.getInt();
            String fullPath = (parentDir != null && !parentDir.isEmpty() ? parentDir + "/" : "") + relativePath;
            parsed.put(name, new AssetBundleReader.BundleEntry(name, fullPath, offset, length));
         }

         BUNDLE_CACHE.put(cacheKey, parsed);
         entries = parsed;
      }

      AssetBundleReader.BundleEntry entry;
      if ((entry = (AssetBundleReader.BundleEntry)entries.get(entryName)) == null) {
         return null;
      } else {
         FileHandle dataFile;
         if (!(dataFile = Gdx.files.internal(entry.filePath)).exists()) {
            return null;
         } else {
            try {
               byte[] result;
               try (InputStream in = dataFile.read()) {
                  long targetOffset = (long)entry.offset;
                  InputStream stream = in;
                  long skipped = 0L;
                  int skipOk = 1;

                  while (true) {
                     if (skipped >= targetOffset) {
                        skipOk = 1;
                        break;
                     }

                     long skipStep;
                     if ((skipStep = stream.skip(targetOffset - skipped)) <= 0L) {
                        skipOk = 0;
                        break;
                     }

                     skipped += skipStep;
                  }

                  if (skipOk == 0) {
                     return null;
                  }

                  result = new byte[entry.length];
                  int readTotal = 0;

                  int readCount;
                  while (readTotal < entry.length && (readCount = in.read(result, readTotal, entry.length - readTotal)) != -1) {
                     readTotal += readCount;
                  }

                  if (readTotal != entry.length) {
                     return null;
                  }
               }

               return result;
            } catch (IOException e) {
               return null;
            }
         }
      }
   }

   public static class BundleEntry {
      public String name;
      public String filePath;
      public int offset;
      public int length;

      public BundleEntry(String name, String filePath, int offset, int length) {
         this.name = name;
         this.filePath = filePath;
         this.offset = offset;
         this.length = length;
      }
   }
}
