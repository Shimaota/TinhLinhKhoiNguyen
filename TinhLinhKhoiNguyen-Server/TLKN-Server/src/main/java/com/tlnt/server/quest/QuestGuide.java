package com.tlnt.server.quest;

import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Message;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Mũi tên chỉ đường (op 63) cho bước đang làm. Bước ở NPC: cùng map -> chỉ thẳng vào NPC. Bước đánh quái / nhặt đồ rơi từ
 * quái: chỉ dẫn TỚI MAP có quái, vào tới map thì tắt (quái rải khắp map, người chơi tự tìm). Khác map -> chỉ vào cửa
 * dịch chuyển đầu tiên trên đường ngắn nhất (theo số cửa) tới map đích. Không có mục tiêu -> tắt.
 */
public final class QuestGuide {

   /** Vùng quanh NPC: đứng trong vùng này thì mũi tên ẩn (pixel). */
   private static final float NPC_HALF_W = 150f;
   private static final float NPC_BELOW = 100f;
   private static final float NPC_H = 400f;

   private QuestGuide() {
   }

   /** Gói op 63 cho người đang ở map {@code currentMapId}; {@code maps} tra mẫu map (null = không có). */
   static Message arrow(QuestStep step, int currentMapId, IntFunction<MapTemplate> maps) {
      if (step == null || step.mapId() <= 0) {
         return QuestPackets.arrowOff();
      }
      MapTemplate here = maps.apply(currentMapId);
      if (here == null) {
         return QuestPackets.arrowOff();
      }
      boolean harvest = step.kind() == StepKind.HARVEST_PLANT || step.kind() == StepKind.HARVEST_FLOWER;
      if (harvest && growsOn(step.arrowMob(), currentMapId)) {
         return QuestPackets.arrowOff(); // hái cây: đứng ở BẤT KỲ map cây mọc (Nấm hương: 6 / 7 / 8) = đã tới nơi
      }
      if (currentMapId == step.mapId()) {
         float[] spot = step.arrowMob() > 0 ? null : npcSpot(step, here); // đã tới map có quái: thôi chỉ
         return spot == null ? QuestPackets.arrowOff() : QuestPackets.arrow(spot[0], spot[1], spot[2], spot[3]);
      }
      MapTemplate.Waypoint door = firstDoor(currentMapId, step.mapId(), maps);
      return door == null ? QuestPackets.arrowOff() : QuestPackets.arrow(door.x(), door.y(), door.w(), door.h());
   }

   /** Cây {@code plantId} mọc được ở map {@code mapId} (cột JSON {@code plant_template.harvest.maps}). */
   static boolean growsOn(int plantId, int mapId) {
      com.tlnt.server.plant.PlantConfig.Entry e = com.tlnt.server.plant.PlantConfig.current().entry(plantId);
      return e != null && e.maps().contains(mapId);
   }

   /** Vùng quanh NPC {@link QuestStep#markerNpc} trên map, null nếu không có.  */
   static float[] npcSpot(QuestStep step, MapTemplate map) {
      for (MapTemplate.NpcSpawn npc : map.npcs()) {
         if (npc.id() == step.markerNpc()) {
            return new float[] {npc.x() - NPC_HALF_W, npc.y() - NPC_BELOW, NPC_HALF_W * 2, NPC_H};
         }
      }
      return null;
   }

   /** Cửa ở map {@code from} mở đầu đường ngắn nhất tới {@code to} (tìm theo chiều rộng qua các cửa). */
   public static MapTemplate.Waypoint firstDoor(int from, int to, IntFunction<MapTemplate> maps) {
      Map<Integer, MapTemplate.Waypoint> firstStep = new HashMap<>();
      ArrayDeque<Integer> queue = new ArrayDeque<>();
      queue.add(from);
      firstStep.put(from, null);
      while (!queue.isEmpty()) {
         int mapId = queue.poll();
         MapTemplate map = maps.apply(mapId);
         if (map == null) {
            continue;
         }
         for (MapTemplate.Waypoint door : map.waypoints()) {
            Integer next = door.targetMapId();
            if (next == null || firstStep.containsKey(next)) {
               continue;
            }
            MapTemplate.Waypoint first = mapId == from ? door : firstStep.get(mapId);
            if (next == to) {
               return first;
            }
            firstStep.put(next, first);
            queue.add(next);
         }
      }
      return null;
   }
}
