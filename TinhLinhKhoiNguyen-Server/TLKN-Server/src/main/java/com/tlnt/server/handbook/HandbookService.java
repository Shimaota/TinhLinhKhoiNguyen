package com.tlnt.server.handbook;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packet;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Cẩm nang (cửa sổ HandbookWindow): chữ / bố cục từng mục nằm trong data game của client, server chỉ gửi QUÀ hiện ở mỗi mục
 * — op {@value Opcode#HANDBOOK} S→C (client {@code handleSkillSlots} — tên sai nghĩa): byte số chương, mỗi chương (byte id,
 * byte số mục, mỗi mục (byte id mục, byte số món, các món như {@code readItemFromMessage})). Mục không có trong data game
 * làm client lỗi (tìm mục không thấy -> null) nên đọc chính bảng mục của data game: cột {@code tlnt_data.guide_topic.rewards}
 * (cột CHỈ SQL — TLNT-Editor nhập data pack không ghi đè; dữ liệu dump game gốc, migration 2026-10-12_guide_topic_rewards.sql),
 * id chương = {@code guide_category.id}, id mục = {@code guide_topic.id}. Gói mã hoá 1 lần lúc nạp, gửi lúc vào game.
 */
public final class HandbookService {

   /** 1 mục: chương, mục, quà. */
   record Topic(int chapter, int topic, List<RewardItem> rewards) {
   }

   private final Database dataDb;
   private volatile Packet packet;

   public HandbookService(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** Đọc lại bảng (SQL — pool blocking / luồng admin). Lỗi thì giữ bản cũ. */
   public void reload() {
      List<Topic> topics = new ArrayList<>();
      Set<Integer> missing = new TreeSet<>();
      try (Connection c = dataDb.getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT c.id, t.id, t.rewards FROM guide_topic t JOIN guide_category c"
                  + " ON c.idx = t.category_idx ORDER BY c.idx, t.idx");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            int chapter = rs.getInt(1);
            int topic = rs.getInt(2);
            List<RewardItem> rewards = new ArrayList<>();
            for (RewardItem r : RewardItem.list(rs.getString(3), false, "handbook " + chapter + "." + topic)) {
               if (GameDataCache.itemTemplate(r.templateId()) != null) {
                  rewards.add(r);
               } else {
                  missing.add(r.templateId());
               }
            }
            topics.add(new Topic(chapter, topic, rewards));
         }
      } catch (SQLException e) {
         Log.warn("Khong doc duoc guide_topic.rewards (chay sql/migrations?): %s", e.getMessage());
         return;
      }
      if (!missing.isEmpty()) {
         Log.warn("Cam nang: bo qua qua co mau item chua co: %s", missing);
      }
      packet = encode(topics, System.currentTimeMillis());
   }

   /** Gói op 61 từ danh sách mục (gom theo chương, giữ thứ tự). Tối đa 127 món / mục (byte). */
   static Packet encode(List<Topic> topics, long now) {
      Map<Integer, List<Topic>> byChapter = new LinkedHashMap<>();
      for (Topic t : topics) {
         byChapter.computeIfAbsent(t.chapter(), k -> new ArrayList<>()).add(t);
      }
      Message msg = new Message(Opcode.HANDBOOK);
      msg.writeByte(byChapter.size());
      for (Map.Entry<Integer, List<Topic>> e : byChapter.entrySet()) {
         msg.writeByte(e.getKey());
         msg.writeByte(e.getValue().size());
         for (Topic t : e.getValue()) {
            msg.writeByte(t.topic());
            int n = Math.min(127, t.rewards().size());
            msg.writeByte(n);
            for (int k = 0; k < n; k++) {
               ItemPackets.writePreviewRanged(msg, t.rewards().get(k), now);
            }
         }
      }
      return msg.toPacket();
   }

   /** Hook vào game: gửi quà các mục cẩm nang (luồng session). */
   public void onLogin(PlayerSession session, PlayerData d) {
      Packet p = packet;
      if (p != null) {
         session.send(p);
      }
   }
}
