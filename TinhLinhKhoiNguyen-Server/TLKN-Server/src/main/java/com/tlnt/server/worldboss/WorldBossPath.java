package com.tlnt.server.worldboss;

import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestGuide;
import com.tlnt.server.quest.QuestPackets;
import com.tlnt.server.world.WorldPlayer;

import java.util.function.IntFunction;

/**
 * "Tìm đường" tới map có boss thế giới (bảng Boss op 100 — {@link WorldBossList#confirmPath}): mỗi lần vào map mới, mũi
 * tên op 63 chỉ vào cửa đầu tiên trên đường ngắn nhất tới map boss ({@link QuestGuide#firstDoor}) + op 100/1 bật lại tự
 * tìm đường ở client (nhân vật tự chạy tới cửa rồi tự vào). Tới map boss / hết hạn / hết đường -> thôi, trả lại mũi tên
 * nhiệm vụ. Chỉ dẫn TỚI MAP, không chọn khu (người dùng 2026-10-18).
 */
public final class WorldBossPath {

   private WorldBossPath() {
   }

   /**
    * Vừa vào khu (luồng tick khu — {@code QuestEvents.joinedMap}). @return true = đang tìm đường tới boss (đã gửi mũi tên
    * boss), người gọi KHÔNG gửi mũi tên nhiệm vụ
    */
   public static boolean guide(WorldPlayer player, PlayerData d, IntFunction<MapTemplate> maps) {
      int target = d.bossPathMap;
      if (target <= 0) {
         return false;
      }
      if (d.mapId == target) {
         d.bossPathMap = 0;
         MapTemplate map = maps.apply(target);
         player.send(QuestPackets.arrowOff()); // tắt tự tìm đường, rồi người gọi gửi lại mũi tên nhiệm vụ
         player.send(Packets.notice("Đã tới " + (map != null ? map.name() : "map có boss") + "."));
         return false;
      }
      MapTemplate.Waypoint door = System.currentTimeMillis() > d.bossPathUntil ? null
            : QuestGuide.firstDoor(d.mapId, target, maps);
      if (door == null) {
         d.bossPathMap = 0;
         player.send(QuestPackets.arrowOff());
         return false;
      }
      player.send(QuestPackets.arrow(door.x(), door.y(), door.w(), door.h()));
      player.send(WorldBossList.autoPathPacket());
      return true;
   }
}
