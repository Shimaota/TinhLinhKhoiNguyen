package com.tlnt.server.data;

import com.tlnt.server.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đọc version của 4 file dữ liệu tĩnh {@code game/item/map/skill} trong DataGame. Mỗi file này do
 * {@code GameDataLoader} phía client ghi/đọc bằng {@code DataOutputStream}, luôn bắt đầu bằng 1 số {@code int}
 * version (4 byte đầu tiên) — nên server chỉ cần đọc đúng 4 byte đó, KHÔNG cần đọc/parse toàn bộ nội dung file.
 *
 * <p>Nguồn đọc: thư mục {@code datagame.dir} nếu có file trên máy server; không có (DataGame nằm ở máy web / CDN
 * khác) thì tải 4 byte đầu qua HTTP từ {@code datagame.httpBaseUrl} — đúng link client dùng để tải.
 */
public final class DataVersionService {

   public static final String GAME = "game";
   public static final String MAP = "map";
   public static final String ITEM = "item";
   public static final String SKILL = "skill";

   private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

   /** null = không có thư mục trên máy server, chỉ đọc qua HTTP. */
   private final Path dataGameDir;
   private final String httpBaseUrl;
   private final Map<String, Integer> versions = new ConcurrentHashMap<>();
   private HttpClient http;

   public DataVersionService(Path dataGameDir, String httpBaseUrl) {
      this.dataGameDir = dataGameDir;
      this.httpBaseUrl = httpBaseUrl;
      reload();
   }

   /** Đọc lại version của cả 4 file. Gọi khi khởi động, hoặc sau khi admin thay dữ liệu mới. */
   public void reload() {
      for (String key : new String[] {GAME, MAP, ITEM, SKILL}) {
         versions.put(key, readVersion(key));
      }
      Log.info("Data version: game=%d map=%d item=%d skill=%d",
            versions.get(GAME), versions.get(MAP), versions.get(ITEM), versions.get(SKILL));
   }

   public int getVersion(String key) {
      return versions.getOrDefault(key, 0);
   }

   private int readVersion(String key) {
      if (dataGameDir != null) {
         Path file = dataGameDir.resolve(key);
         if (Files.isRegularFile(file)) {
            try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
               return in.readInt();
            } catch (IOException e) {
               Log.error("Loi doc version tu " + file.toAbsolutePath(), e);
               return 0;
            }
         }
      }
      if (httpBaseUrl == null || httpBaseUrl.isEmpty()) {
         Log.warn("Khong tim thay file du lieu '%s' (datagame.dir=%s, khong co datagame.httpBaseUrl), coi version = 0",
               key, dataGameDir == null ? "" : dataGameDir.toAbsolutePath());
         return 0;
      }
      return readVersionHttp(key);
   }

   /** Tải 4 byte đầu file qua HTTP (Range; web server bỏ qua Range thì vẫn chỉ đọc 4 byte rồi đóng). */
   private int readVersionHttp(String key) {
      String url = httpBaseUrl.endsWith("/") ? httpBaseUrl + key : httpBaseUrl + "/" + key;
      try {
         if (http == null) {
            http = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT)
                  .followRedirects(HttpClient.Redirect.NORMAL).build();
         }
         HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(HTTP_TIMEOUT)
               .header("Range", "bytes=0-3").GET().build();
         HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
         try (DataInputStream in = new DataInputStream(response.body())) {
            int status = response.statusCode();
            if (status != 200 && status != 206) {
               Log.warn("Tai version '%s' that bai: HTTP %d tu %s, coi version = 0", key, status, url);
               return 0;
            }
            return in.readInt();
         }
      } catch (IOException | IllegalArgumentException e) {
         Log.warn("Khong tai duoc version '%s' tu %s (%s), coi version = 0", key, url, e.toString());
         return 0;
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         return 0;
      }
   }
}
