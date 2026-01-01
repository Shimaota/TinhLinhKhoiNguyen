package com.tlnt.web.net;

import com.badlogic.gdx.files.FileHandle;
import com.tlnt.util.HttpClient;
import java.io.OutputStream;
import java.util.Map;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

public class TeaVMHttpHandler implements HttpClient.IHttpHandler {

   @JSFunctor
   public interface JsDownloadCallback extends JSObject {
      void onComplete(ArrayBuffer buffer, String error);
   }

   @JSBody(params = { "url", "cb" }, script =
      "fetch(url)\n" +
      "  .then(function(res) {\n" +
      "    if (!res.ok) {\n" +
      "      cb(null, 'HTTP ' + res.status + ' ' + res.statusText);\n" +
      "      return null;\n" +
      "    }\n" +
      "    return res.arrayBuffer();\n" +
      "  })\n" +
      "  .then(function(buf) {\n" +
      "    if (buf) { cb(buf, null); }\n" +
      "  })\n" +
      "  .catch(function(err) {\n" +
      "    cb(null, String(err));\n" +
      "  });\n"
   )
   private static native void fetchArrayBufferJs(String url, JsDownloadCallback cb);

   @Async
   public static native byte[] fetchBytesAsync(String url);

   private static void fetchBytesAsync(String url, AsyncCallback<byte[]> callback) {
      try {
         fetchArrayBufferJs(url, new JsDownloadCallback() {
            @Override
            public void onComplete(ArrayBuffer buffer, String error) {
               if (error != null || buffer == null) {
                  if (error != null) {
                     System.err.println("TeaVMHttpHandler fetch error for [" + url + "]: " + error);
                  }
                  callback.complete(null);
                  return;
               }
               Int8Array int8 = Int8Array.create(buffer);
               byte[] bytes = new byte[int8.getLength()];
               for (int i = 0; i < bytes.length; i++) {
                  bytes[i] = int8.get(i);
               }
               callback.complete(bytes);
            }
         });
      } catch (Throwable t) {
         System.err.println("TeaVMHttpHandler exception: " + t.getMessage());
         callback.complete(null);
      }
   }

   @Override
   public byte[] downloadBytes(String urlString, int retries, HttpClient.ProgressCallback callback, Map<String, String> headers) {
      System.out.println("TeaVMHttpHandler: bat dau tai " + urlString);
      for (int i = 0; i <= retries; i++) {
         try {
            byte[] bytes = fetchBytesAsync(urlString);
            if (bytes != null && bytes.length > 0) {
               if (callback != null) {
                  callback.onBytesDownloaded(bytes.length);
               }
               System.out.println("TeaVMHttpHandler: da tai xong " + bytes.length + " bytes tu " + urlString);
               return bytes;
            }
         } catch (Throwable t) {
            System.err.println("TeaVMHttpHandler lan thu " + (i + 1) + " that bai: " + t.getMessage());
         }
         try {
            Thread.sleep(1000);
         } catch (Throwable ignored) {
         }
      }
      return null;
   }

   @Override
   public boolean downloadToFile(String urlString, int retries, FileHandle targetFile, HttpClient.ProgressCallback callback, Map<String, String> headers) {
      byte[] bytes = downloadBytes(urlString, retries, callback, headers);
      if (bytes == null || bytes.length == 0) {
         return false;
      }
      try {
         if (targetFile.parent() != null) {
            targetFile.parent().mkdirs();
         }
         try (OutputStream out = targetFile.write(false)) {
            out.write(bytes);
            out.flush();
         }
         return true;
      } catch (Throwable t) {
         System.err.println("TeaVMHttpHandler loi ghi file: " + t.getMessage());
         return false;
      }
   }
}
