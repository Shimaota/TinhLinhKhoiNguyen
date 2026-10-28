package com.tlnt.server.social;

import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.World;

/**
 * Điểm sát khí ({@link PkRules#MAX_PK_POINTS}, luật ở {@link PkRules}): cộng khi hạ gục bằng cờ Thử thách, giảm theo thời
 * gian, đổi mức phạt thì tính lại chỉ số. Mọi hàm chạy trên LUỒNG SESSION (tính lại chỉ số đọc túi đồ).
 */
public final class PkPoints {

   private PkPoints() {
   }

   /** Vào game: trừ phần đã giảm trong lúc offline (chỉ số tính sau đó lúc vào khu). */
   public static void onLogin(PlayerSession session, PlayerData d) {
      if (apply(d, System.currentTimeMillis())) {
         ItemService.refreshLook(d);
      }
   }

   /** Mỗi 5 s (cùng nhịp hồi HP / MP): đủ 60 phút không hạ ai thì -1; đổi mức phạt -> tính lại + phát chỉ số. */
   public static void tick(PlayerSession session, World world, PlayerData d, long now) {
      int before = d.pkPoints;
      if (before <= 0 || !apply(d, now)) {
         return;
      }
      if (PkRules.statPercentLeft(before) != PkRules.statPercentLeft(d.pkPoints)) {
         refresh(world, d);
      }
      session.send(Packets.notice("Điểm sát khí giảm còn " + d.pkPoints + "/" + PkRules.MAX_PK_POINTS + "."));
   }

   /** Vừa +1 sát khí (luồng tick khu đã cộng): chạm mức phạt mới -> tính lại chỉ số + báo. */
   public static void added(PlayerSession session, World world, PlayerData d, int before, int after) {
      int left = PkRules.statPercentLeft(after);
      if (PkRules.statPercentLeft(before) == left) {
         return;
      }
      refresh(world, d);
      session.send(Packets.notice("Điểm sát khí đạt " + after + ", toàn bộ chỉ số chiến đấu bị giảm " + (100 - left)
            + "%! Dùng Bùa thanh tẩy để xoá sát khí."));
   }

   /** Bùa thanh tẩy vừa xoá {@code before} điểm: đang bị phạt thì tính lại chỉ số. */
   public static void cleansed(World world, PlayerData d, int before) {
      if (PkRules.statPercentLeft(before) < 100) {
         refresh(world, d);
      }
   }

   /** @return true nếu điểm đổi */
   private static boolean apply(PlayerData d, long now) {
      synchronized (d) { // luồng tick khu cộng điểm dưới cùng khoá
         long[] r = PkRules.decay(d.pkPoints, d.pkPointsAt, now);
         boolean changed = r[0] != d.pkPoints;
         d.pkPoints = (int) r[0];
         d.pkPointsAt = r[1];
         return changed;
      }
   }

   private static void refresh(World world, PlayerData d) {
      ItemService.refreshLook(d);
      if (d.inWorld) {
         world.broadcastStats(d);
      }
   }
}
