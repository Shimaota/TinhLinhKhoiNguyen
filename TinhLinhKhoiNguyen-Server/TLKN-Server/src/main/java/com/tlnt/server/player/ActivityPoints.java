package com.tlnt.server.player;

import com.tlnt.server.item.GearCharges;
import com.tlnt.server.net.Packets;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.WorldPackets;

/**
 * Điểm hoạt động ({@code PlayerData.dynamicPoints}, op 23/9) theo hướng dẫn game "Điểm hoạt động"
 * ({@code tlnt_data.guide_topic} 0/3). Nguồn đã có từ trước (giữ số cũ): tham gia Tế đàn / Rừng Linh Mộc 25, Lôi vực 10,
 * boss phó bản 25, NV hằng ngày 25 ({@code daily_quest}), hạ người ở Lôi vực 2, giải đấu Bộ lạc (thắng vòng 5 / vô địch 50 —
 * {@code tournament/TournamentRules}). Lưu theo lượt lưu nhân vật (thoát game / lượt lưu kế).
 */
public final class ActivityPoints {

   public static final int PIG_HARVEST = 20;
   /** Hái cây (Quả mọng, Hoa ma thuật, Nấm hương... — {@code plant/PlantService}). */
   public static final int PLANT_HARVEST = 5;
   /** Câu được cá (câu hụt / kéo rác không tính). */
   public static final int FISH = 1;
   public static final int ELITE = 1;
   public static final int LEADER = 2;
   /** Boss thế giới (người kết liễu): boss quái (Vệ thần) 25, boss người chơi giả (Tinh linh) 50. */
   public static final int GUARDIAN_BOSS = 25;
   public static final int SPIRIT_BOSS = 50;
   public static final int CHECK_IN = 20;
   public static final int BEEHIVE = 5;
   public static final int COCONUT = 5;
   public static final int FOX = 10;
   public static final int MAIN_QUEST = 50;
   public static final int ACHIEVEMENT = 100;

   private ActivityPoints() {
   }

   /** Cộng {@code amount} + gửi op 23/9 + "Bạn vừa nhận được N điểm hoạt động". Luồng session. */
   public static void add(PlayerSession session, PlayerData d, int amount) {
      if (d == null || amount <= 0) {
         return;
      }
      int points;
      synchronized (d) {
         points = d.dynamicPoints += amount;
      }
      session.send(WorldPackets.dynamicPoints(points));
      session.send(Packets.notice("Bạn vừa nhận được " + amount + " điểm hoạt động"));
   }

   /** Như {@link #add} từ luồng bất kỳ (luồng tick khu): xếp vào hàng đợi session. */
   public static void post(PlayerSession session, int amount) {
      if (session == null || amount <= 0) {
         return;
      }
      PlayerData d = session.getPlayer();
      GearCharges.post(session, () -> {
         if (d != null && session.getPlayer() == d) {
            add(session, d, amount);
         }
      });
   }
}
