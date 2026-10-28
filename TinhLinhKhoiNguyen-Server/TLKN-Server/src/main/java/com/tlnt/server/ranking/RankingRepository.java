package com.tlnt.server.ranking;

import com.google.gson.Gson;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.player.PlayerData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL của bảng xếp hạng: số liệu MỌI nhân vật (1 lần lúc khởi động — sau đó sống trong RAM), cấu hình Top tuần
 * ({@code tlnt_data.weekly_top}) và cờ "tuần đã trả thưởng" ({@code tlnt_server.server_state}).
 */
public final class RankingRepository {

   private static final Gson GSON = new Gson();
   /** Khoá trong {@code server_state}: mã tuần ({@link WeekClock}) gần nhất đã gửi thư thưởng Top tuần. */
   static final String KEY_REWARDED_WEEK = "weekly_top_rewarded";

   private final Database playerDb;
   private final Database dataDb;

   public RankingRepository(Database playerDb, Database dataDb) {
      this.playerDb = playerDb;
      this.dataDb = dataDb;
   }

   public List<RankingService.Row> loadAll() throws SQLException {
      List<RankingService.Row> rows = new ArrayList<>();
      try (Connection conn = playerDb.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                  "SELECT id, name, gender, level, exp, recharge_total, ranking, item_body FROM player")) {
         ps.setFetchSize(1_000);
         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               PlayerData.RankStats stats = parse(rs.getString(7));
               ItemContainer body = ItemContainer.fromJson(rs.getString(8), PlayerData.BODY_SLOTS);
               rows.add(RankingService.Row.of(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
                     rs.getLong(5), rs.getLong(6), stats, body.asList()));
            }
         }
      }
      return rows;
   }

   /** Tuần đã trả thưởng, 0 = chưa có dòng. */
   int rewardedWeek() throws SQLException {
      try (Connection conn = playerDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT v FROM server_state WHERE k = ?")) {
         ps.setString(1, KEY_REWARDED_WEEK);
         try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Integer.parseInt(rs.getString(1).trim()) : 0;
         }
      } catch (NumberFormatException e) {
         return 0;
      }
   }

   /** Ghi cờ tuần đã trả thưởng — trong CÙNG transaction với các thư thưởng (không trả 2 lần, không mất thư). */
   static void saveRewardedWeek(Connection conn, int week) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO server_state (k, v) VALUES (?, ?) ON DUPLICATE KEY UPDATE v = VALUES(v)")) {
         ps.setString(1, KEY_REWARDED_WEEK);
         ps.setString(2, String.valueOf(week));
         ps.executeUpdate();
      }
   }

   void saveRewardedWeek(int week) throws SQLException {
      try (Connection conn = playerDb.getConnection()) {
         saveRewardedWeek(conn, week);
      }
   }

   /** {@code tlnt_data.weekly_top}: tên + quà top 1/2/3 (JSON {@link RewardItem}, mặc định khoá), sắp theo {@code sort}. */
   List<WeeklyRewards.Event> loadWeeklyEvents() throws SQLException {
      List<WeeklyRewards.Event> events = new ArrayList<>();
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                  "SELECT id, name, top1, top2, top3 FROM weekly_top ORDER BY sort, id");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            int id = rs.getInt(1);
            String where = "weekly_top#" + id;
            events.add(new WeeklyRewards.Event(id, rs.getString(2), List.of(
                  RewardItem.list(rs.getString(3), true, where + ".top1"),
                  RewardItem.list(rs.getString(4), true, where + ".top2"),
                  RewardItem.list(rs.getString(5), true, where + ".top3"))));
         }
      }
      return events;
   }

   private static PlayerData.RankStats parse(String json) {
      try {
         PlayerData.RankStats s = json == null ? null : GSON.fromJson(json, PlayerData.RankStats.class);
         return s == null ? new PlayerData.RankStats() : s;
      } catch (RuntimeException e) {
         return new PlayerData.RankStats();
      }
   }
}
