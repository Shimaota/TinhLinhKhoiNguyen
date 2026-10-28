package com.tlnt.server.player;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tlnt.server.db.Database;
import com.tlnt.server.db.SqlWork;
import com.tlnt.server.farm.FarmState;
import com.tlnt.server.item.ItemCodec;
import com.tlnt.server.item.ItemCollection;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.social.FriendEntry;
import com.tlnt.server.welfare.WelfareState;
import com.tlnt.server.achievement.AchievementState;
import com.tlnt.server.daily.DailyState;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Đọc/ghi bảng {@code player} (xem {@code sql/tlnt_player.sql}).
 *
 * Mỗi cột khai báo đúng 1 lần trong {@link #COLUMNS} (tên cột + cách đọc từ {@link PlayerData} + cách
 * nạp ngược lại), rồi SELECT/INSERT/UPDATE đều sinh từ danh sách đó -> thêm cột chỉ sửa 1 chỗ, không
 * bao giờ lệch thứ tự tham số. Cột kiểu danh sách/đối tượng (túi đồ, kỹ năng...) lưu JSON.
 */
public final class PlayerRepository {

   private static final Gson GSON = new Gson();

   private static final Type SKILL_LIST = new TypeToken<List<SkillData>>() { }.getType();
   private static final Type FRIEND_LIST = new TypeToken<List<FriendEntry>>() { }.getType();
   private static final Type GIFTCODE_MAP = new TypeToken<java.util.HashMap<String, Integer>>() { }.getType();
   private static final Type BUFF_MAP = new TypeToken<java.util.HashMap<Integer, Long>>() { }.getType();

   /** Cột JSON theo tên (admin panel đọc / ghi từng cột bằng đúng bộ chuyển của lượt lưu — {@link #adminReadJson}). */
   private static final java.util.Map<String, JsonColumn<?>> JSON_COLUMNS = new java.util.LinkedHashMap<>();

   /** 1 cột JSON: đọc từ PlayerData (null = cột NULL), ghi vào PlayerData, kiểu Gson. */
   private record JsonColumn<T>(Function<PlayerData, T> read, BiConsumer<PlayerData, T> write, Type type) {
   }

   private static final List<Column> COLUMNS = List.of(
         column("name", p -> p.name, (p, rs, c) -> p.name = rs.getString(c)),
         column("gender", p -> p.gender, (p, rs, c) -> p.gender = rs.getInt(c)),
         column("class_id", p -> p.classId, (p, rs, c) -> p.classId = rs.getInt(c)),
         column("level", p -> p.level, (p, rs, c) -> p.level = rs.getInt(c)),
         column("exp", p -> p.exp, (p, rs, c) -> p.exp = rs.getLong(c)),
         column("pk_points", p -> p.pkPoints, (p, rs, c) -> p.pkPoints = rs.getInt(c)),
         column("pk_points_at", p -> p.pkPointsAt, (p, rs, c) -> p.pkPointsAt = rs.getLong(c)),
         column("spirit_badge_until", p -> p.spiritBadgeUntil, (p, rs, c) -> p.spiritBadgeUntil = rs.getLong(c)),
         column("hp", p -> p.hp, (p, rs, c) -> p.hp = rs.getLong(c)),
         column("mp", p -> p.mp, (p, rs, c) -> p.mp = rs.getLong(c)),
         column("gold", p -> p.gold, (p, rs, c) -> p.gold = rs.getLong(c)),
         column("locked_gold", p -> p.lockedGold, (p, rs, c) -> p.lockedGold = rs.getLong(c)),
         column("diamond", p -> p.diamond, (p, rs, c) -> p.diamond = rs.getLong(c)),
         column("recharge_total", p -> p.rechargeTotal, (p, rs, c) -> p.rechargeTotal = rs.getLong(c)),
         json("welfare", p -> p.welfare, (p, v) -> p.welfare = v, WelfareState.class),
         column("dynamic_points", p -> p.dynamicPoints, (p, rs, c) -> p.dynamicPoints = rs.getInt(c)),
         column("market_sell_day", p -> p.marketSellDay, (p, rs, c) -> p.marketSellDay = rs.getInt(c)),
         column("market_sell_count", p -> p.marketSellCount, (p, rs, c) -> p.marketSellCount = rs.getInt(c)),
         json("ranking", p -> p.rankStats, (p, v) -> p.rankStats = v, PlayerData.RankStats.class),
         json("giftcodes", p -> p.giftcodes.isEmpty() ? null : p.giftcodes, (p, v) -> p.giftcodes = v, GIFTCODE_MAP),
         json("combos", p -> p.comboBought.isEmpty() ? null : p.comboBought, (p, v) -> p.comboBought = v, GIFTCODE_MAP),
         column("activity_points", p -> p.activityPoints, (p, rs, c) -> p.activityPoints = rs.getInt(c)),
         column("title_id", p -> p.titleId, (p, rs, c) -> p.titleId = rs.getInt(c)),
         column("talent_points", p -> p.talentPoints, (p, rs, c) -> p.talentPoints = rs.getInt(c)),
         json("talents", p -> p.talents, (p, v) -> p.talents = v, int[].class),
         column("map_id", p -> p.mapId, (p, rs, c) -> p.mapId = rs.getInt(c)),
         column("pos_x", p -> p.x, (p, rs, c) -> p.x = rs.getObject(c, Float.class)),
         column("pos_y", p -> p.y, (p, rs, c) -> p.y = rs.getObject(c, Float.class)),
         // Số ô phải nạp TRƯỚC item_bag / item_box (thứ tự trong list = thứ tự nạp).
         column("bag_size", p -> p.bag.size(), (p, rs, c) -> p.bag = new ItemContainer(size(rs.getInt(c), PlayerData.DEFAULT_BAG_SIZE))),
         column("box_size", p -> p.box.size(), (p, rs, c) -> p.box = new ItemContainer(size(rs.getInt(c), PlayerData.DEFAULT_BOX_SIZE))),
         container("item_bag", p -> p.bag, (p, v) -> p.bag = v),
         container("item_box", p -> p.box, (p, v) -> p.box = v),
         container("item_body", p -> p.body, (p, v) -> p.body = v),
         // Đeo lưng: bộ sưu tập {"sel","cap","items"}; dữ liệu cũ = 1 item lẻ -> bộ 1 món đang dùng.
         column("item_back", p -> ItemCodec.collectionToJson(p.backs),
               (p, rs, c) -> p.backs = ItemCodec.backsFromJson(rs.getString(c))),
         collection("item_costume", p -> p.costumes, (p, v) -> p.costumes = v),
         collection("item_title", p -> p.titles, (p, v) -> p.titles = v),
         collection("item_mask", p -> p.masks, (p, v) -> p.masks = v),
         collection("item_pet", p -> p.pets, (p, v) -> p.pets = v),
         json("quick_slots", p -> p.quickSlots, (p, v) -> p.quickSlots = v, int[].class),
         json("skills", p -> p.skills, (p, v) -> p.skills = v, SKILL_LIST),
         json("skill_bar", p -> p.skillBar, (p, v) -> p.skillBar = v, int[].class),
         json("settings", p -> p.settings, (p, v) -> p.settings = v, PlayerSettings.class),
         json("main_quest", p -> p.mainQuest, (p, v) -> p.mainQuest = v, PlayerData.MainQuestProgress.class),
         json("daily_quest", p -> p.dailyQuest == null || p.dailyQuest.day == 0 ? null : p.dailyQuest,
               (p, v) -> p.dailyQuest = v == null ? new DailyState() : v.normalize(), DailyState.class),
         json("achievement", p -> p.achievements == null || p.achievements.isEmpty() ? null : p.achievements,
               (p, v) -> p.achievements = v == null ? new AchievementState() : v.normalize(), AchievementState.class),
         json("farm", p -> p.farm.isEmpty() ? null : p.farm, (p, v) -> p.farm = v, FarmState.class),
         json("event_state", p -> p.eventState.isEmpty() ? null : p.eventState,
               (p, v) -> p.eventState = v == null ? new com.tlnt.server.event.EventState() : v,
               com.tlnt.server.event.EventState.class),
         column("altar_day", p -> p.altarDay, (p, rs, c) -> p.altarDay = rs.getInt(c)),
         column("linhmoc_day", p -> p.linhMocDay, (p, rs, c) -> p.linhMocDay = rs.getInt(c)),
         column("tribe_left_at", p -> p.tribeLeftAt, (p, rs, c) -> p.tribeLeftAt = rs.getLong(c)),
         json("friends", p -> p.friends, (p, v) -> p.friends = v, FRIEND_LIST),
         json("buffs", p -> p.buffs.isEmpty() ? null : p.buffs, (p, v) -> p.buffs = v, BUFF_MAP),
         json("item_daily", p -> p.itemDaily.used.isEmpty() ? null : p.itemDaily, (p, v) -> p.itemDaily = v,
               PlayerData.DailyItemUse.class),
         column("play_time_seconds", p -> p.playTimeSeconds, (p, rs, c) -> p.playTimeSeconds = rs.getLong(c)));

   private static final String COLUMN_LIST = COLUMNS.stream().map(Column::name).collect(Collectors.joining(", "));
   private static final String SELECT_BY_ACCOUNT = "SELECT id, account_id, " + COLUMN_LIST
         + " FROM player WHERE account_id = ?";
   private static final String INSERT = "INSERT INTO player (account_id, " + COLUMN_LIST + ") VALUES (?"
         + ", ?".repeat(COLUMNS.size()) + ")";
   private static final String UPDATE = "UPDATE player SET "
         + COLUMNS.stream().map(c -> c.name() + " = ?").collect(Collectors.joining(", "));

   public enum CreateResult { CREATED, NAME_TAKEN, ALREADY_HAS_CHARACTER }

   /**
    * Bản chụp giá trị các cột tại 1 thời điểm (đã serialize JSON xong). Tạo trên luồng đang sở hữu
    * {@link PlayerData}, rồi mới đưa sang luồng ghi DB -> luồng ghi không bao giờ đọc object đang bị sửa.
    */
   public record Snapshot(int playerId, String name, Object[] values) {
   }

   private final Database db;

   public PlayerRepository(Database db) {
      this.db = db;
   }

   public static Snapshot snapshot(PlayerData player) {
      com.tlnt.server.item.GearCharges.writeBack(player); // lượt phụ kiện (bộ đếm RAM) -> dòng chỉ số trước khi chụp
      Object[] values = new Object[COLUMNS.size()];
      for (int i = 0; i < values.length; i++) {
         values[i] = COLUMNS.get(i).read().apply(player);
      }
      return new Snapshot(player.getId(), player.name, values);
   }

   /** Mỗi tài khoản tối đa 1 nhân vật; null nếu chưa tạo. */
   public PlayerData findByAccountId(int accountId) throws SQLException {
      try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(SELECT_BY_ACCOUNT)) {
         ps.setInt(1, accountId);
         try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
               return null;
            }
            PlayerData player = new PlayerData(rs.getInt("id"), rs.getInt("account_id"));
            for (Column column : COLUMNS) {
               column.load().load(player, rs, column.name());
            }
            return player;
         }
      }
   }

   /**
    * Tên cũ đã đổi ({@code player_name_history}): GIỮ cho chủ cũ — người khác không đặt / tạo nhân vật bằng tên đó, chủ
    * cũ đổi lại được. Cùng collation với {@code player.name} (không phân biệt hoa thường / dấu).
    */
   private static final String RESERVED_BY = "SELECT player_id FROM player_name_history WHERE name = ? FOR UPDATE";

   /**
    * Đổi tên (Thẻ đổi tên), 1 transaction: tên mới không được là tên cũ của NGƯỜI KHÁC, {@code uk_player_name} chặn
    * trùng tên đang dùng; tên cũ vào lịch sử (giữ cho mình), tên mới nếu là tên cũ của mình thì bỏ khỏi lịch sử.
    * @return false nếu tên đã có người dùng / đang được giữ cho người khác
    */
   public boolean rename(int playerId, String oldName, String name) throws SQLException {
      try (Connection conn = db.getConnection()) {
         boolean autoCommit = conn.getAutoCommit();
         conn.setAutoCommit(false);
         try {
            int owner = reservedBy(conn, name);
            if (owner != 0 && owner != playerId) {
               conn.rollback();
               return false;
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE player SET name = ? WHERE id = ?")) {
               ps.setString(1, name);
               ps.setInt(2, playerId);
               ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                  "DELETE FROM player_name_history WHERE name = ? AND player_id = ?")) {
               ps.setString(1, name);
               ps.setInt(2, playerId);
               ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO player_name_history (name, player_id)"
                  + " VALUES (?, ?) ON DUPLICATE KEY UPDATE changed_at = NOW()")) {
               ps.setString(1, oldName); // chỉ trùng được với dòng của chính mình (tên đang dùng không thể đang bị giữ)
               ps.setInt(2, playerId);
               ps.executeUpdate();
            }
            conn.commit();
            return true;
         } catch (SQLIntegrityConstraintViolationException duplicate) {
            conn.rollback();
            return false;
         } catch (SQLException e) {
            conn.rollback();
            throw e;
         } finally {
            conn.setAutoCommit(autoCommit);
         }
      }
   }

   /** Ai đang giữ tên cũ {@code name}; 0 = không ai. Khoá dòng / khoảng trống tới hết transaction. */
   private static int reservedBy(Connection conn, String name) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement(RESERVED_BY)) {
         ps.setString(1, name);
         try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
         }
      }
   }

   /** Ghi nhân vật mới; thành công thì gán id DB vào {@code player}. */
   public CreateResult insert(PlayerData player) throws SQLException {
      Snapshot snapshot = snapshot(player);
      try (Connection conn = db.getConnection()) {
         boolean autoCommit = conn.getAutoCommit();
         conn.setAutoCommit(false);
         try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            if (reservedBy(conn, player.name) != 0) { // tên cũ của người đã đổi tên -> giữ cho họ
               conn.rollback();
               return CreateResult.NAME_TAKEN;
            }
            ps.setInt(1, player.getAccountId());
            bind(ps, snapshot.values(), 2);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
               keys.next();
               player.assignId(keys.getInt(1));
            }
            conn.commit();
            return CreateResult.CREATED;
         } catch (SQLIntegrityConstraintViolationException duplicate) {
            conn.rollback();
            String message = String.valueOf(duplicate.getMessage());
            return message.contains("uk_player_name") ? CreateResult.NAME_TAKEN : CreateResult.ALREADY_HAS_CHARACTER;
         } catch (SQLException e) {
            conn.rollback();
            throw e;
         } finally {
            conn.setAutoCommit(autoCommit);
         }
      }
   }

   public void save(Snapshot snapshot, boolean logout) throws SQLException {
      String sql = UPDATE + (logout ? ", last_logout_at = NOW()" : "") + " WHERE id = ?";
      try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
         int next = bind(ps, snapshot.values(), 1);
         ps.setInt(next, snapshot.playerId());
         ps.executeUpdate();
      }
   }

   /** UPDATE cho lô: cờ thoát game là tham số -> mọi người trong lô dùng CHUNG 1 câu (JDBC batch được). */
   private static final String UPDATE_BATCH = UPDATE + ", last_logout_at = IF(?, NOW(), last_logout_at) WHERE id = ?";

   /**
    * Lưu nhiều nhân vật trong 1 transaction bằng JDBC batch (thứ tự giữ nguyên: cùng 1 người xuất hiện 2 lần thì bản
    * sau ghi đè bản trước). Lỗi -> rollback cả lô, người gọi lưu lại từng người.
    */
   public void saveBatch(List<Snapshot> snapshots, boolean[] logout) throws SQLException {
      try (Connection conn = db.getConnection()) {
         boolean autoCommit = conn.getAutoCommit();
         conn.setAutoCommit(false);
         try (PreparedStatement ps = conn.prepareStatement(UPDATE_BATCH)) {
            for (int i = 0; i < snapshots.size(); i++) {
               Snapshot snapshot = snapshots.get(i);
               int next = bind(ps, snapshot.values(), 1);
               ps.setBoolean(next, logout[i]);
               ps.setInt(next + 1, snapshot.playerId());
               ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
         } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
         } finally {
            conn.setAutoCommit(autoCommit);
         }
      }
   }

   /** Câu UPDATE đầy đủ giá trị (đã escape) — ghi ra file khi lưu thất bại để admin chạy tay. */
   static String updateSql(Snapshot snapshot, boolean logout) {
      StringBuilder sb = new StringBuilder("UPDATE player SET ");
      Object[] values = snapshot.values();
      for (int i = 0; i < COLUMNS.size(); i++) {
         if (i > 0) {
            sb.append(", ");
         }
         sb.append(COLUMNS.get(i).name()).append(" = ").append(sqlLiteral(values[i]));
      }
      if (logout) {
         sb.append(", last_logout_at = NOW()");
      }
      return sb.append(" WHERE id = ").append(snapshot.playerId()).toString();
   }

   private static String sqlLiteral(Object value) {
      if (value == null) {
         return "NULL";
      }
      if (value instanceof Number) {
         return value.toString();
      }
      if (value instanceof Boolean b) {
         return b ? "1" : "0";
      }
      String text = value.toString();
      StringBuilder sb = new StringBuilder(text.length() + 2).append('\'');
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         switch (c) {
            case '\'' -> sb.append("\\'");
            case '\\' -> sb.append("\\\\");
            case '\n' -> sb.append("\\n");
            case '\r' -> sb.append("\\r");
            case '\0' -> sb.append("\\0");
            case (char) 26 -> sb.append("\\Z");
            default -> sb.append(c);
         }
      }
      return sb.append('\'').toString();
   }

   /**
    * 1 transaction: UPDATE nhân vật ({@code snapshot}, null = không lưu nhân vật) rồi {@code extra} (thư, kí gửi...).
    * Lỗi bất kỳ đâu -> rollback cả hai (không bao giờ có chuyện túi đồ đã trừ mà thư chưa ghi, hoặc ngược lại).
    */
   public void saveInTransaction(Snapshot snapshot, SqlWork extra) throws SQLException {
      try (Connection conn = db.getConnection()) {
         boolean autoCommit = conn.getAutoCommit();
         conn.setAutoCommit(false);
         try {
            if (snapshot != null) {
               try (PreparedStatement ps = conn.prepareStatement(UPDATE + " WHERE id = ?")) {
                  int next = bind(ps, snapshot.values(), 1);
                  ps.setInt(next, snapshot.playerId());
                  ps.executeUpdate();
               }
            }
            if (extra != null) {
               extra.run(conn);
            }
            conn.commit();
         } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
         } finally {
            conn.setAutoCommit(autoCommit);
         }
      }
   }

   public void markLogin(int playerId) throws SQLException {
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("UPDATE player SET last_login_at = NOW() WHERE id = ?")) {
         ps.setInt(1, playerId);
         ps.executeUpdate();
      }
   }

   private static int bind(PreparedStatement ps, Object[] values, int firstIndex) throws SQLException {
      int index = firstIndex;
      for (Object value : values) {
         ps.setObject(index++, value);
      }
      return index;
   }

   // ------------------------------------------------------------------ định nghĩa cột

   @FunctionalInterface
   private interface Loader {
      void load(PlayerData player, ResultSet rs, String column) throws SQLException;
   }

   private record Column(String name, Function<PlayerData, Object> read, Loader load) {
   }

   private static Column column(String name, Function<PlayerData, Object> read, Loader load) {
      return new Column(name, read, load);
   }

   private static int size(int stored, int fallback) {
      return stored > 0 ? stored : fallback;
   }

   /**
    * Cột kho có ô (item_bag / item_box / item_body): JSON gọn {@code {"ô":item}}, chỉ ô có đồ ({@link ItemCodec}).
    * Số ô lấy từ kho hiện tại của PlayerData (bag_size / box_size đã nạp trước, body cố định 10).
    */
   private static Column container(String name, Function<PlayerData, ItemContainer> read,
         BiConsumer<PlayerData, ItemContainer> write) {
      return new Column(name,
            p -> read.apply(p).toJson(),
            (p, rs, c) -> write.accept(p, ItemContainer.fromJson(rs.getString(c), read.apply(p).size())));
   }

   /** Cột bộ sưu tập: JSON gọn {@code {"sel","cap","items"}} ({@link ItemCodec}); NULL = bộ sưu tập rỗng. */
   private static Column collection(String name, Function<PlayerData, ItemCollection> read,
         BiConsumer<PlayerData, ItemCollection> write) {
      return new Column(name,
            p -> ItemCodec.collectionToJson(read.apply(p)),
            (p, rs, c) -> write.accept(p, ItemCodec.collectionFromJson(rs.getString(c))));
   }

   /** Tên các cột JSON (theo thứ tự khai báo). */
   public static java.util.Set<String> adminJsonColumns() {
      return java.util.Collections.unmodifiableSet(JSON_COLUMNS.keySet());
   }

   /** Admin panel: giá trị cột JSON {@code column} của nhân vật (như lúc lưu); null = NULL. */
   public static String adminReadJson(PlayerData p, String column) {
      JsonColumn<?> c = JSON_COLUMNS.get(column);
      if (c == null) {
         throw new IllegalArgumentException("Khong co cot JSON " + column);
      }
      Object value = c.read().apply(p);
      return value == null ? null : GSON.toJson(value, c.type());
   }

   /**
    * Admin panel: ghi cột JSON {@code column} từ chuỗi {@code json} (null / rỗng = về mặc định như cột NULL lúc nạp: không
    * gọi ghi — người gọi tự đặt mặc định nếu cần) bằng đúng bộ đọc lúc nạp. JSON sai kiểu -> ném lỗi Gson.
    */
   @SuppressWarnings("unchecked")
   public static void adminWriteJson(PlayerData p, String column, String json) {
      JsonColumn<Object> c = (JsonColumn<Object>) JSON_COLUMNS.get(column);
      if (c == null) {
         throw new IllegalArgumentException("Khong co cot JSON " + column);
      }
      Object value = json == null || json.isBlank() ? null : GSON.fromJson(json, c.type());
      if (value == null) {
         throw new IllegalArgumentException("JSON rong");
      }
      c.write().accept(p, value);
   }

   /** Cột JSON: null trong DB giữ nguyên giá trị mặc định của {@link PlayerData}. */
   private static <T> Column json(String name, Function<PlayerData, T> read, BiConsumer<PlayerData, T> write, Type type) {
      JSON_COLUMNS.put(name, new JsonColumn<>(read, write, type));
      return new Column(name,
            p -> {
               T value = read.apply(p);
               return value == null ? null : GSON.toJson(value, type);
            },
            (p, rs, c) -> {
               String text = rs.getString(c);
               if (text != null) {
                  write.accept(p, GSON.fromJson(text, type));
               }
            });
   }
}
