package com.tlnt.server.config;

import com.tlnt.server.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Properties;

/**
 * Cấu hình server, đọc từ {@code server.properties} đặt cạnh file jar khi chạy (ưu tiên) hoặc từ classpath (giá trị
 * mặc định đóng gói sẵn). MỌI khoá đều có mặc định ở đây -> file cũ thiếu khoá mới vẫn chạy.
 * Bảng đầy đủ các khoá: {@code README.md} (mục Configuration reference).
 */
public final class ServerConfig {

   private static final String FILE_NAME = "server.properties";
   private static final int CPUS = Runtime.getRuntime().availableProcessors();

   private final Properties props;

   private ServerConfig(Properties props) {
      this.props = props;
   }

   public static ServerConfig load() {
      Properties props = new Properties();
      Path external = Path.of(FILE_NAME);
      if (Files.isRegularFile(external)) {
         try (InputStream in = Files.newInputStream(external)) {
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8)); // UTF-8: chữ tiếng Việt ghi thẳng
            Log.info("Da nap cau hinh tu %s", external.toAbsolutePath());
         } catch (IOException e) {
            Log.error("Khong doc duoc " + external.toAbsolutePath(), e);
         }
      } else {
         try (InputStream in = ServerConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in != null) {
               props.load(new InputStreamReader(in, StandardCharsets.UTF_8)); // UTF-8: chữ tiếng Việt ghi thẳng
               Log.info("Da nap cau hinh mac dinh dong goi trong jar (khong thay %s canh jar)", FILE_NAME);
            }
         } catch (IOException e) {
            Log.error("Khong doc duoc server.properties trong classpath", e);
         }
      }
      ServerConfig config = new ServerConfig(props);
      // Kiểm tra khoá AES ngay lúc khởi động để báo lỗi sớm.
      config.getTextureDecryptKey();
      config.getAtlasDecryptKey();
      return config;
   }

   private String str(String key, String def) {
      return props.getProperty(key, def).trim();
   }

   private boolean bool(String key, boolean def) {
      String value = props.getProperty(key);
      return value == null || value.isBlank() ? def : Boolean.parseBoolean(value.trim());
   }

   private int integer(String key, int def) {
      String value = props.getProperty(key);
      return value == null || value.isBlank() ? def : Integer.parseInt(value.trim());
   }

   private double decimal(String key, double def) {
      String value = props.getProperty(key);
      return value == null || value.isBlank() ? def : Double.parseDouble(value.trim());
   }

   // ------------------------------------------------------------------ mạng

   public int getTcpPort() {
      return integer("server.port", 7502);
   }

   /**
    * Cổng WebSocket cho client web (trình duyệt không mở được TCP), 0 = tắt. Client web mặc định nối cổng TCP + 1.
    * Chỉ có ở {@code net.transport=netty}.
    */
   public int getWsPort() {
      return integer("ws.port", 0);
   }

   /** {@code netty} (mặc định, chịu tải lớn) hoặc {@code kryonet} (tầng cũ, chỉ để dự phòng). */
   public String getTransport() {
      return str("net.transport", "netty").toLowerCase(Locale.ROOT);
   }

   /** Luồng I/O của Netty. */
   public int getIoThreads() {
      return integer("net.ioThreads", Math.max(2, CPUS));
   }

   /** Frame lớn nhất client được gửi lên (gói C→S đều nhỏ; chặn client gửi rác làm tốn RAM). */
   public int getMaxFrameBytes() {
      return integer("net.maxFrameBytes", 64 * 1024);
   }

   /** Gói chờ gửi cho 1 client vượt mức này (client treo / mạng quá chậm) -> ngắt kết nối. */
   public int getMaxPendingWriteBytes() {
      return integer("net.maxPendingWriteBytes", 2 * 1024 * 1024);
   }

   /** Chỉ cho net.transport=kryonet: bộ đệm ghi / đọc mỗi kết nối (KryoNet cấp phát cố định). */
   public int getKryoNetWriteBufferBytes() {
      return integer("net.kryonet.writeBufferBytes", 65536);
   }

   public int getKryoNetObjectBufferBytes() {
      return integer("net.kryonet.objectBufferBytes", 65536);
   }

   // ------------------------------------------------------------------ luồng

   /** Pool xử lý gói game (nhẹ, không SQL). Khoá cũ {@code server.workerThreads} vẫn dùng được. */
   public int getGameThreads() {
      return integer("server.gameThreads", integer("server.workerThreads", Math.max(2, CPUS)));
   }

   /** Pool cho việc chặn: SQL đăng nhập/tạo nhân vật, băm mật khẩu, vẽ captcha. */
   public int getBlockingThreads() {
      return integer("server.blockingThreads", Math.max(4, CPUS * 2));
   }

   /** Số luồng tick khu. */
   public int getTickThreads() {
      return integer("world.tickThreads", Math.max(1, CPUS));
   }

   /** Chu kỳ tick của mỗi khu (lệnh, chống hack di chuyển, mô phỏng thú cưng, quái đánh). */
   public long getWorldTickMillis() {
      return integer("world.tickMillis", 33);
   }

   /**
    * Chu kỳ PHÁT vị trí người / thú cưng cho người khác (gộp, tách khỏi tick). Client nội suy với độ trễ 100 ms
    * ({@code ClickMoveController}) -> giữ ≤ 50 ms (≥ 2 mốc trong khoảng trễ). Số lần ghi socket ∝ 1000 / giá trị này.
    */
   public long getMoveBroadcastMillis() {
      return integer("world.moveBroadcastMillis", 50);
   }

   // ------------------------------------------------------------------ SQL

   public String getDbUrl() {
      return str("db.url", "jdbc:mysql://127.0.0.1:3306/tlnt_data"
            + "?characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true");
   }

   public String getPlayerDbUrl() {
      return str("db.player.url", "jdbc:mysql://127.0.0.1:3306/tlnt_server"
            + "?characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true");
   }

   public String getDbUser() {
      return str("db.user", "root");
   }

   public String getDbPassword() {
      return props.getProperty("db.password", "");
   }

   /** Pool kết nối DB dữ liệu tĩnh (chỉ đọc lúc khởi động). */
   public int getDbPoolSize() {
      return integer("db.poolSize", 4);
   }

   /** Pool kết nối DB tài khoản/nhân vật: đủ cho mọi luồng blocking + luồng ghi. */
   public int getPlayerDbPoolSize() {
      return integer("db.player.poolSize", getBlockingThreads() + 2);
   }

   // ------------------------------------------------------------------ dữ liệu / người chơi

   /**
    * Thư mục DataGame trên máy server (chỉ để đọc version 4 file game/map/item/skill). Đường dẫn tương đối tính từ thư
    * mục chạy server. Để trống = DataGame nằm ở máy web / CDN khác -> server đọc version qua {@code datagame.httpBaseUrl}.
    */
   public Path getDataGameDir() {
      String dir = str("datagame.dir", "DataGame");
      return dir.isEmpty() ? null : Path.of(dir);
   }

   public String getHttpBaseUrl() {
      return str("datagame.httpBaseUrl", "http://127.0.0.1:8080/DataGame");
   }

   /** Sau khi kick vì đăng nhập trùng, tài khoản phải chờ bấy nhiêu giây mới đăng nhập lại được. */
   public int getReloginDelaySeconds() {
      return integer("login.reloginDelaySeconds", 10);
   }

   // ------------------------------------------------------------------ chống dò mật khẩu / nghẽn đăng nhập

   /** Số kết nối cùng lúc tối đa mỗi IP (0 = không giới hạn). Nhiều người chung 1 mạng (quán net) -> tăng lên. */
   public int getMaxConnectionsPerIp() {
      return integer("security.maxConnectionsPerIp", 10);
   }

   /** Số lần thử đăng nhập tối đa / phút / IP. */
   public int getLoginsPerIpPerMinute() {
      return integer("security.loginsPerIpPerMinute", 10);
   }

   /** Số lần xin captcha (form đăng ký) tối đa / phút / IP. */
   public int getCaptchasPerIpPerMinute() {
      return integer("security.captchasPerIpPerMinute", 20);
   }

   /** Sai mật khẩu bấy nhiêu lần trong 15 phút -> khoá đăng nhập tài khoản đó (0 = tắt). */
   public int getFailsPerAccount() {
      return integer("security.failsPerAccount", 10);
   }

   public int getAccountLockMinutes() {
      return integer("security.accountLockMinutes", 5);
   }

   /** Miễn giới hạn cho 127.0.0.1 (bot tải, chạy thử trên máy). */
   public boolean isExemptLoopback() {
      return bool("security.exemptLoopback", true);
   }

   /** Tắt server: số người đăng xuất (lưu) mỗi giây — đá dần dần để DB không bị dồn 1 lúc. */
   public int getShutdownKickPerSecond() {
      return integer("shutdown.kickPerSecond", 200);
   }

   /**
    * Lệnh test trong ô chat (/hp, /mp, /cd, /full...) cho MỌI người chơi. Mặc định TẮT: chỉ bật trên máy test, lỡ
    * quên thì server thật cũng không có lệnh gian lận.
    */
   public boolean isChatTestCommands() {
      return bool("chat.testCommands", false);
   }

   /** Nhân EXP đánh quái toàn server: 1 = gốc (mặc định), 2 = x2 (sự kiện)... */
   public int getExpRate() {
      return Math.max(1, integer("exp.rate", 1));
   }

   /** EXP gốc / 1 sát thương / 1 cấp quái ở map thường ({@code combat/MobRules#hitExp}). */
   public double getExpField() {
      return decimal("exp.field", 0.0018);
   }

   /** Người cao hơn quái 1..9 cấp: mỗi cấp trừ bấy nhiêu hệ số EXP. */
   public double getExpPenaltyStep() {
      return decimal("exp.penaltyStep", 0.1);
   }

   /** Hệ số EXP thấp nhất khi người cao hơn quái (cũng là hệ số ở Tế đàn khi cao hơn ≥ 10 cấp). */
   public double getExpPenaltyMin() {
      return decimal("exp.penaltyMin", 0.2);
   }

   /** Rừng Linh Mộc: EXP / 1 sát thương = giá trị này × cấp lượt (trừ khi {@code linhmoc.expFactor} &gt; 0). */
   public double getExpDungeon() {
      return decimal("exp.dungeon", 0.02);
   }

   /** Quái đánh trả người đánh nó (op 20 sub 1, mỗi 3,5 s). Tắt = quái chỉ đứng chịu đòn như trước. */
   public boolean isMobAttack() {
      return bool("mob.attack", true);
   }

   /** Quái thường hồi sinh sau bấy nhiêu giây (game gốc ~3,5 s). 0 = dùng {@code respawn} riêng của từng điểm sinh. */
   public int getMobRespawnSeconds() {
      return integer("mob.respawnSeconds", 3);
   }

   /** Thành tựu "Đánh bại N Vệ thần": {@code world_boss.id} tính là Vệ thần (cách nhau dấu phẩy). */
   public java.util.Set<Integer> getAchievementGuardianBosses() {
      return ids(str("achievement.guardianBosses", "1,9,11,13,15"));
   }

   /** Thành tựu "Đánh bại N Tinh linh": {@code world_boss.id} tính là Tinh linh. */
   public java.util.Set<Integer> getAchievementSpiritBosses() {
      return ids(str("achievement.spiritBosses", "2,10,12,14,16"));
   }

   private static java.util.Set<Integer> ids(String csv) {
      java.util.Set<Integer> out = new java.util.TreeSet<>();
      for (String part : csv.split(",")) {
         if (!part.isBlank()) {
            out.add(Integer.parseInt(part.trim()));
         }
      }
      return out;
   }

   /** Bộ lạc: mọi khoá {@code tribe.*} (mặc định nằm trong {@code TribeConfig}). */
   public com.tlnt.server.tribe.TribeConfig getTribe() {
      return com.tlnt.server.tribe.TribeConfig.parse(props::getProperty);
   }

   /** Phó bản Rừng Linh Mộc: mọi khoá {@code linhmoc.*} (mặc định nằm trong {@code LinhMocConfig}). */
   public com.tlnt.server.linhmoc.LinhMocConfig getLinhMoc() {
      return com.tlnt.server.linhmoc.LinhMocConfig.parse(props::getProperty);
   }

   /** Map nhân vật mới xuất hiện. */
   public int getStartMapId() {
      return integer("player.startMapId", 2);
   }

   /** Khoá giải mã ảnh (TextureAsset), null nếu chưa cấu hình. */
   public byte[] getTextureDecryptKey() {
      return parseHexKey(str("assets.textureDecryptKey", ""), "assets.textureDecryptKey");
   }

   /** Khoá giải mã atlas skeleton (EncryptedTextureAtlas), null nếu chưa cấu hình. */
   public byte[] getAtlasDecryptKey() {
      return parseHexKey(str("assets.atlasDecryptKey", ""), "assets.atlasDecryptKey");
   }

   // ------------------------------------------------------------------ vận hành

   /** DEBUG / INFO / WARN / ERROR. */
   public Log.Level getLogLevel() {
      return Log.Level.valueOf(str("log.level", "INFO").toUpperCase(Locale.ROOT));
   }

   /** Màu log: auto (bật khi terminal hiểu ANSI) / on / off. */
   public String getLogColor() {
      return str("log.color", "auto");
   }

   /** In thống kê (online, tick, bộ nhớ) mỗi bấy nhiêu giây; 0 = tắt. */
   /** Cổng admin panel web (chỉ nghe 127.0.0.1 — {@code admin/web/AdminServer}); 0 = tắt. */
   public int getAdminPort() {
      return integer("admin.port", 8090);
   }

   /** Mật khẩu admin panel (để trống = không hỏi, vì panel chỉ mở trên chính máy chủ). */
   public String getAdminPassword() {
      return str("admin.password", "");
   }

   /**
    * Thư mục ảnh client (đã giải mã) cho admin panel: {@code icon/N.png}, {@code ui/N.png}, {@code Saira-Bold.ttf}. Đường
    * dẫn tương đối tính từ thư mục chạy server.
    */
   public java.nio.file.Path getAdminAssetsDir() {
      return java.nio.file.Path.of(str("admin.assetsDir", "Assest"));
   }

   /** Mọi khoá đang đọc (admin panel xem; giá trị khoá có chữ password / key / secret bị che). */
   public java.util.Map<String, String> masked() {
      java.util.Map<String, String> out = new java.util.TreeMap<>();
      for (String k : props.stringPropertyNames()) {
         String low = k.toLowerCase(java.util.Locale.ROOT);
         boolean secret = low.contains("password") || low.contains("key") || low.contains("secret");
         String v = props.getProperty(k, "");
         out.put(k, secret && !v.isBlank() ? "••••••" : v);
      }
      return out;
   }

   public int getStatsIntervalSeconds() {
      return integer("stats.intervalSeconds", 60);
   }

   /** Khoá AES dạng hex (32/48/64 ký tự = 16/24/32 byte). Rỗng -> null (không gửi opcode 59). */
   private static byte[] parseHexKey(String hex, String name) {
      if (hex.isEmpty()) {
         return null;
      }
      if (!hex.matches("[0-9a-fA-F]+") || (hex.length() != 32 && hex.length() != 48 && hex.length() != 64)) {
         throw new IllegalArgumentException(name + " phai la hex 32/48/64 ky tu (khoa AES 16/24/32 byte)");
      }
      return HexFormat.of().parseHex(hex);
   }
}
