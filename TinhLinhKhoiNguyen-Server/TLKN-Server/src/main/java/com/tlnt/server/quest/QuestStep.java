package com.tlnt.server.quest;

import java.util.List;

/**
 * 1 bước nhiệm vụ.
 *
 * @param kind      cách tính tiến độ
 * @param name      1 dòng ở cột trái "Nhiệm vụ chính tuyến" + HUD
 * @param hint      "Gợi ý"
 * @param target    id NPC (TALK, cây táo, rương) / id quái (KILL) / id item (COLLECT) / id đá / id shop; 0 = không cần
 * @param count     số lượng cần ("(0/N)")
 * @param mapId     map của chỗ cần tới (dấu "!" trên NPC + mũi tên chỉ đường); 0 = không chỉ đường
 * @param dialog    TALK: các trang hội thoại (mỗi trang 1 hộp thoại)
 * @param skipLabel TALK: nút thứ 2 ở các trang chưa phải trang cuối ("Bỏ qua" / "Đóng")
 * @param guideNpc  NPC có dấu "!" + mũi tên chỉ tới (trên {@code mapId}) khi {@code target} dùng cho việc khác (id đá, id
 *                  shop); 0 = theo loại bước ({@link #markerNpc})
 * @param arrowMob  bước đánh / nhặt đồ từ quái này (hái cây: id cây): mũi tên chỉ dẫn tới map {@code mapId}, tới nơi
 *                  thì tắt (0 = chỉ tới NPC {@code target}); {@link StepKind#COLLECT_ELITE}: quái phải hạ
 * @param escortItem bước hộ tống: item thú cưng (loại 37, vd 290 Moara) đi theo người chơi suốt bước này
 *                  ({@code world/Escort}); bỏ xa / bị hạ gục / thoát game = thất bại, lùi về bước trước. 0 = không
 * @param escortMaps bước hộ tống: các map sinh vật được đi cùng — sang map khác = thất bại
 */
public record QuestStep(StepKind kind, String name, String hint, int target, int count, int mapId,
      List<String> dialog, String skipLabel, int guideNpc, int arrowMob, int escortItem, List<Integer> escortMaps) {

   static QuestStep talk(String name, int npcId, int mapId, String hint, String skipLabel, String... pages) {
      return new QuestStep(StepKind.TALK, name, hint, npcId, 1, mapId, List.of(pages), skipLabel, 0, 0, 0, List.of());
   }

   static QuestStep kill(String name, int mobId, int count, int mapId, String hint) {
      return new QuestStep(StepKind.KILL, name, hint, mobId, count, mapId, List.of(), "", 0, mobId, 0, List.of());
   }

   /** Hạ {@code count} con Tinh anh / Thủ lĩnh của quái {@code mobId} (đồng đội cùng khu cũng được tính). */
   static QuestStep killElite(String name, int mobId, int count, int mapId, String hint) {
      return new QuestStep(StepKind.KILL_ELITE, name, hint, mobId, count, mapId, List.of(), "", 0, mobId, 0, List.of());
   }

   /** Nhặt đủ {@code count} món {@code itemId} rơi riêng khi hạ quái; mũi tên chỉ quái {@code arrowMob} ở {@code mapId}. */
   static QuestStep collect(String name, int itemId, int count, int mapId, int arrowMob, String hint) {
      return new QuestStep(StepKind.COLLECT, name, hint, itemId, count, mapId, List.of(), "", 0, arrowMob, 0, List.of());
   }

   /** Bước làm 1 việc ở NPC / vật {@code target} trên map {@code mapId}. */
   static QuestStep at(StepKind kind, String name, int target, int mapId, String hint) {
      return new QuestStep(kind, name, hint, target, 1, mapId, List.of(), "", 0, 0, 0, List.of());
   }

   /**
    * Tới được map {@code targetMap} bằng NPC {@code npcId} (không có cửa dịch chuyển — vd Tế đàn qua Tư tế Buraen): dấu
    * "!" + mũi tên chỉ tới NPC ở map {@code npcMap}.
    */
   static QuestStep reachVia(String name, int targetMap, int npcId, int npcMap, String hint) {
      return new QuestStep(StepKind.REACH_MAP, name, hint, targetMap, 1, npcMap, List.of(), "", npcId, 0, 0, List.of());
   }

   /** Gây sát thương {@code count} lần lên boss Tế đàn ở map {@code mapId} (không chỉ đường — vào bằng Tư tế). */
   static QuestStep hitBoss(String name, int mapId, int count, String hint) {
      return new QuestStep(StepKind.HIT_BOSS, name, hint, mapId, count, 0, List.of(), "", 0, 0, 0, List.of());
   }

   /** Gây sát thương {@code count} lần lên boss thế giới {@code worldBossId} (không chỉ đường — chú ý thông báo hệ thống). */
   static QuestStep hitWorldBoss(String name, int worldBossId, int count, String hint) {
      return new QuestStep(StepKind.HIT_WORLD_BOSS, name, hint, worldBossId, count, 0, List.of(), "", 0, 0, 0, List.of());
   }

   /** Nhặt đủ {@code count} món {@code itemId} rơi riêng khi tham gia hạ boss (không chỉ đường). */
   static QuestStep collectBoss(String name, int itemId, int count, String hint) {
      return new QuestStep(StepKind.COLLECT_BOSS, name, hint, itemId, count, 0, List.of(), "", 0, 0, 0, List.of());
   }

   /**
    * Nhặt đủ {@code count} món {@code itemId} rơi riêng khi hạ Tinh anh / Thủ lĩnh của quái {@code mobId}; mũi tên chỉ
    * tới map {@code mapId} có quái đó.
    */
   static QuestStep collectElite(String name, int itemId, int count, int mapId, int mobId, String hint) {
      return new QuestStep(StepKind.COLLECT_ELITE, name, hint, itemId, count, mapId, List.of(), "", 0, mobId, 0,
            List.of());
   }

   /** Hái {@code count} cây {@code plantId}; mũi tên chỉ tới map {@code mapId}, tới nơi thì tắt (cây mọc rải rác). */
   static QuestStep harvest(String name, int plantId, int count, int mapId, String hint) {
      return new QuestStep(StepKind.HARVEST_PLANT, name, hint, plantId, count, mapId, List.of(), "", 0, plantId, 0,
            List.of());
   }

   /**
    * Hái {@code count} Hoa bất kỳ; mũi tên chỉ tới map {@code mapId}, đứng ở map cây {@code flowerPlant} mọc thì tắt (mọi
    * cây hoa mọc cùng các map).
    */
   static QuestStep harvestFlower(String name, int count, int mapId, int flowerPlant, String hint) {
      return new QuestStep(StepKind.HARVEST_FLOWER, name, hint, 0, count, mapId, List.of(), "", 0, flowerPlant, 0,
            List.of());
   }

   /** Tới được map {@code mapId} (mũi tên chỉ cửa dịch chuyển, tới nơi thì tắt). */
   static QuestStep reach(String name, int mapId, String hint) {
      return new QuestStep(StepKind.REACH_MAP, name, hint, mapId, 1, mapId, List.of(), "", 0, 0, 0, List.of());
   }

   /**
    * Như bước này nhưng có thú cưng {@code itemId} đi theo cần hộ tống ({@link #escortItem}), chỉ trong các map
    * {@code maps} ({@link #escortMaps}).
    */
   QuestStep escorting(int itemId, Integer... maps) {
      return new QuestStep(kind, name, hint, target, count, mapId, dialog, skipLabel, guideNpc, arrowMob, itemId,
            List.of(maps));
   }

   /** Bước thao tác giao diện (không chỉ đường). */
   static QuestStep ui(StepKind kind, String name, int count, String hint) {
      return new QuestStep(kind, name, hint, 0, count, 0, List.of(), "", 0, 0, 0, List.of());
   }

   /** Như {@link #ui} nhưng sự kiện phải đúng {@code target} (id đá, id shop...). */
   static QuestStep ui(StepKind kind, String name, int target, int count, String hint) {
      return new QuestStep(kind, name, hint, target, count, 0, List.of(), "", 0, 0, 0, List.of());
   }

   /**
    * Bước làm việc ở NPC {@code npcId} (map {@code mapId}: dấu "!" + mũi tên) nhưng sự kiện khớp theo {@code target}
    * khác (id đá, id shop).
    */
   static QuestStep atNpc(StepKind kind, String name, int target, int npcId, int mapId, String hint) {
      return new QuestStep(kind, name, hint, target, 1, mapId, List.of(), "", npcId, 0, 0, List.of());
   }

   /** NPC có dấu "!" trên đầu (client {@code NpcReadyMarker}) + mũi tên chỉ tới; 0 = không có. */
   public int markerNpc() {
      if (guideNpc > 0) {
         return guideNpc;
      }
      return switch (kind) {
         case TALK, HARVEST_APPLE, OPEN_CHEST, TAKE_FROM_CHEST, UPGRADE_APPLE_TREE, CRAFT_WEAPON -> target;
         default -> 0;
      };
   }

   /** Tiến độ cộng dồn theo từng sự kiện (khác: tính lại từ trạng thái hiện có / xong luôn). */
   boolean accumulates() {
      return kind == StepKind.KILL || kind == StepKind.KILL_ELITE || kind == StepKind.FRIEND_INVITE
            || kind == StepKind.HIT_BOSS || kind == StepKind.HARVEST_PLANT || kind == StepKind.HIT_WORLD_BOSS
            || kind == StepKind.HARVEST_FLOWER;
   }
}
