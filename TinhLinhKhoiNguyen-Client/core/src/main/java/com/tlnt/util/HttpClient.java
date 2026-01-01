package com.tlnt.util;

import com.badlogic.gdx.files.FileHandle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpClient {

   public interface IHttpHandler {
      byte[] downloadBytes(String urlString, int retries, HttpClient.ProgressCallback callback, Map<String, String> headers);
      boolean downloadToFile(String urlString, int retries, FileHandle targetFile, HttpClient.ProgressCallback callback, Map<String, String> headers);
   }

   private static IHttpHandler customHandler;

   public static void setCustomHandler(IHttpHandler handler) {
      customHandler = handler;
   }

   public static byte[] downloadBytes(String urlString, int retries, HttpClient.ProgressCallback callback, Map<String, String> headers) {
      if (customHandler != null) {
         return customHandler.downloadBytes(urlString, retries, callback, headers);
      }
      while (retries >= 0) {
         HttpURLConnection conn = null;
         try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            if (headers != null) {
               for (Map.Entry<String, String> entry : headers.entrySet()) {
                  conn.setRequestProperty(entry.getKey(), entry.getValue());
               }
            }
            conn.connect();

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
               throw new IOException("Unexpected response code " + code);
            }

            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
               byte[] buffer = new byte[8192];
               int read;
               while ((read = in.read(buffer)) != -1) {
                  out.write(buffer, 0, read);
                  if (callback != null) {
                     callback.onBytesDownloaded(read);
                  }
               }
               return out.toByteArray();
            }
         } catch (Exception e) {
            e.printStackTrace();
            if (retries == 0) {
               return null;
            }
            GameUtils.sleepMillis(1000);
            retries--;
         } finally {
            if (conn != null) {
               try {
                  conn.disconnect();
               } catch (Throwable ignored) {
               }
            }
         }
      }
      return null;
   }

   public static boolean downloadToFile(String urlString, int retries, FileHandle targetFile, HttpClient.ProgressCallback callback, Map<String, String> headers) {
      if (customHandler != null) {
         return customHandler.downloadToFile(urlString, retries, targetFile, callback, headers);
      }
      for (int attempt = 0; attempt <= retries; attempt++) {
         long counted = 0L;
         OutputStream out = null;
         HttpURLConnection conn = null;

         try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            if (headers != null) {
               for (Map.Entry<String, String> entry : headers.entrySet()) {
                  conn.setRequestProperty(entry.getKey(), entry.getValue());
               }
            }
            conn.connect();

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
               throw new IOException("Unexpected response code " + code);
            }

            long expected = -1;
            String cl = conn.getHeaderField("Content-Length");
            if (cl != null) {
               try {
                  expected = Long.parseLong(cl);
               } catch (Exception ignored) {
               }
            }

            try (InputStream in = conn.getInputStream()) {
               out = targetFile.write(false);
               byte[] buf = new byte[16384];
               int count;
               while ((count = in.read(buf)) != -1) {
                  out.write(buf, 0, count);
                  counted += count;
                  if (callback != null) {
                     callback.onBytesDownloaded(count);
                  }
               }
            }

            out.close();
            out = null;
            if (expected >= 0L && counted != expected) {
               throw new IOException("Incomplete " + counted + "/" + expected + ": " + urlString);
            }

            return true;
         } catch (Exception e) {
            e.printStackTrace();
            if (callback != null && counted > 0L) {
               callback.onBytesDownloaded((int)-counted);
            }

            if (attempt < retries) {
               GameUtils.sleepMillis(1000 * (attempt + 1));
            }
         } finally {
            if (out != null) {
               try {
                  out.close();
               } catch (IOException ignored) {
               }
            }
            if (conn != null) {
               try {
                  conn.disconnect();
               } catch (Throwable ignored) {
               }
            }
         }
      }

      return false;
   }

   public static String fetchString(String urlString, Map<String, String> headers) {
      byte[] bytes = downloadBytes(urlString, 1, null, headers);
      if (bytes != null && bytes.length > 0) {
         return new String(bytes, StandardCharsets.UTF_8);
      }
      return "";
   }

   public abstract static class ProgressCallback {
      public abstract void onBytesDownloaded(int bytes);
   }
}
