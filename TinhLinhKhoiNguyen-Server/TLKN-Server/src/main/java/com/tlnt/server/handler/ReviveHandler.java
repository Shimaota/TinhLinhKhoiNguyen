package com.tlnt.server.handler;

import com.tlnt.server.daily.DailyEvents;
import com.tlnt.server.daily.DailyKind;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.World;

/**
 * Opcode 28 C→S: nút ở hộp "Kiệt sức" ({@code DeathRespawnPopup}, client tự mở khi HP của mình ≤ 0). Tên method sai:
 *
 * <pre>
 * sub | method client        | ý nghĩa thật
 * 0   | sendRequestQuest     | "Hồi sinh (1 cỏ hồi sinh)": tại chỗ, đầy HP / MP
 * 1   | sendRequestDungeon   | "Về làng": điểm xuất hiện map khởi đầu, đầy HP / MP
 * </pre>
 * Nút "Chờ" không gửi gì.
 */
public final class ReviveHandler implements PacketHandler {

   private final World world;
   private final ItemService items;

   public ReviveHandler(World world, ItemService items) {
      this.world = world;
      this.items = items;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || !d.isDead()) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> reviveWithHerb(session, d, world, items);
         case 1 -> {
            if (com.tlnt.server.world.Arena.isArenaMap(d.mapId)) { // đấu trường: về làng = bỏ cuộc / bị loại
               world.leaveArena(session, "Bạn đã rời đấu trường.");
               return;
            }
            world.reviveInVillage(session);
            DailyEvents.add(session, DailyKind.REVIVE, 0, 1);
         }
         default -> {
         }
      }
   }

   /**
    * "Hồi sinh (1 cỏ hồi sinh)": tại chỗ, đầy HP / MP. Dùng chung cho nút ở hộp Kiệt sức và ô "Tự động hồi sinh"
    * ({@code auto/OnlineAutoTask}). Không được ở Lôi vực / đấu trường (2 nơi này tự hồi sinh theo luật riêng) — báo lý do.
    * Luồng session. @return true nếu đã hồi sinh
    */
   public static boolean reviveWithHerb(PlayerSession session, PlayerData d, World world, ItemService items) {
      if (com.tlnt.server.thunder.ThunderRules.isThunderMap(d.mapId)) {
         session.send(Packets.notice(com.tlnt.server.thunder.ThunderRules.NO_REVIVE_HERB));
         return false;
      }
      if (com.tlnt.server.world.Arena.isArenaMap(d.mapId)) {
         session.send(Packets.notice(com.tlnt.server.tournament.TournamentRules.NO_REVIVE_HERB));
         return false;
      }
      if (!items.consumeOne(session, ItemService.REVIVE_HERB_TEMPLATE_ID)) {
         session.send(Packets.notice("Cần 1 Cỏ hồi sinh trong hành trang."));
         return false;
      }
      world.reviveHere(session, ItemService.REVIVE_HERB_HP_PERCENT);
      com.tlnt.server.achievement.AchievementEvents.add(session, com.tlnt.server.achievement.AchievementType.REVIVE, 1);
      DailyEvents.add(session, DailyKind.REVIVE, 0, 1); // NV hằng ngày "Hồi sinh"
      return true;
   }
}
