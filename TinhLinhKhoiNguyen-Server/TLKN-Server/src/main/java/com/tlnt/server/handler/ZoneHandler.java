package com.tlnt.server.handler;

import com.tlnt.server.item.UsableItems;
import com.tlnt.server.linhmoc.LinhMocRules;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;

/**
 * Opcode 25 C→S (tên method phía client đều sai):
 *
 * <pre>
 * sub | client                                          | chức năng
 * 0   | nút Khu vực trên HUD -> sendRequestInventory    | xin danh sách khu (server trả 25 sub 0, client mở popup)
 * 1   | chọn khu trong RegionInfoPopup -> sendSplitItem | đổi khu (int zoneId)
 * 2   | bấm điểm trên bản đồ thế giới -> sendSellItem   | dịch chuyển (int pageId, int mapId), tốn 1 Bùa dịch chuyển
 * 3   | nút thoát trên HUD -> sendRequestShop           | rời phó bản (nút chỉ hiện ở map phó bản): Tế đàn 34-40 -> làng,
 *     |                                                 | Rừng Linh Mộc (linhmoc.maps) -> cạnh Bác Rano
 * </pre>
 */
public final class ZoneHandler implements PacketHandler {

   private final World world;
   private final UsableItems usables;

   public ZoneHandler(World world, UsableItems usables) {
      this.world = world;
      this.usables = usables;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      byte sub = message.readByte();
      switch (sub) {
         case 0 -> world.sendZoneList(session);
         case 1 -> world.changeZone(session, message.readInt());
         case 2 -> {
            message.readInt(); // trang bản đồ thế giới, không cần
            usables.teleport(session, message.readInt()); // trừ bùa rồi World.teleportToMap
         }
         case 3 -> { // 2 hàm tự kiểm tra lại trên luồng khu: không ở phó bản của mình thì bỏ qua
            PlayerData d = session.getPlayer();
            if (d != null && LinhMocRules.isDungeonMap(d.mapId)) {
               world.leaveLinhMoc(session, LinhMocRules.config().text("exit"), null);
            } else if (d != null && com.tlnt.server.thunder.ThunderRules.isThunderMap(d.mapId)) {
               world.leaveThunder(session, "Bạn đã rời Lôi vực Sấm sét.");
            } else if (d != null && com.tlnt.server.world.Arena.isArenaMap(d.mapId)) {
               world.leaveArena(session, "Bạn đã rời đấu trường.");
            } else {
               world.leaveAltar(session, null); // không báo chữ (đồng hồ / nút Thoát đã đủ)
            }
         }
         default -> Log.debug("Session #%d gui opcode 25 sub %d khong ho tro", session.getId(), sub);
      }
   }
}
