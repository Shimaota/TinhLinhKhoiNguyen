package com.tlnt.server.quest;

import com.tlnt.server.combat.MobRules;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.WorldPlayer;

import java.util.function.IntFunction;

/**
 * Điểm móc nhiệm vụ cho các hệ thống khác (item, NPC, cây táo, tiềm năng, cài đặt, đánh quái, vào map) — 1 dòng gọi
 * tĩnh, không phải nối thêm tham số khởi tạo. Chưa {@link #install} (test) thì không làm gì.
 */
public final class QuestEvents {

   private static volatile QuestService service;

   private QuestEvents() {
   }

   /** Gọi 1 lần lúc khởi động ({@code GameServer}). */
   public static void install(QuestService questService) {
      service = questService;
   }

   /** Dịch vụ nhiệm vụ (null khi chưa khởi động). */
   public static QuestService service() {
      return service;
   }

   /** Sự kiện trên luồng session của chính người chơi. */
   public static void fire(PlayerSession session, StepKind kind, int target) {
      fire(session, kind, target, 1);
   }

   /** Như trên, kèm số lượng / giá trị ({@link StepKind#UPGRADE_STAR}: số sao vừa đạt). */
   public static void fire(PlayerSession session, StepKind kind, int target, int amount) {
      QuestService s = service;
      if (s != null && !session.isBot()) {
         s.onEvent(session, kind, target, amount);
      }
   }

   /**
    * Hạ quái mẫu {@code mobTemplateId} biến thể {@code variant} — gọi trên LUỒNG TICK KHU cho người kết liễu VÀ đồng đội
    * cùng khu: CHỈ bước đánh quái ({@link StepKind#KILL}, {@link StepKind#KILL_ELITE}) được tính chung cả nhóm, các
    * bước khác (nhặt đồ rơi, nói chuyện...) mỗi người tự làm. Lọc nhanh rồi chuyển việc về hàng đợi session của người đó.
    */
   public static void mobKilled(WorldPlayer player, int mobTemplateId, int variant) {
      if (player.data().bot) {
         return;
      }
      QuestService s = service;
      QuestStep step = QuestService.currentStep(player.data().mainQuest);
      if (s == null || step == null || step.target() != mobTemplateId) {
         return;
      }
      boolean counts = step.kind() == StepKind.KILL
            || (step.kind() == StepKind.KILL_ELITE && variant != MobRules.VARIANT_NORMAL);
      if (counts) {
         post(s, player, step.kind(), mobTemplateId);
      }
   }

   /**
    * Vật phẩm nhiệm vụ rơi khi {@code player} hạ quái {@code mobTemplateId} biến thể {@code variant} (luồng tick khu): bước
    * nhặt đồ ({@link StepKind#COLLECT}) -> 1 món (khoá) từ quái bất kỳ trừ người rơm; {@link StepKind#COLLECT_ELITE} -> chỉ
    * Tinh anh / Thủ lĩnh của quái {@code arrowMob}. null = không rơi.
    */
   public static ItemData questDrop(WorldPlayer player, int mobTemplateId, int variant) {
      if (player.data().bot) {
         return null;
      }
      QuestStep step = QuestService.currentStep(player.data().mainQuest);
      if (service == null || step == null) {
         return null;
      }
      boolean drops = step.kind() == StepKind.COLLECT ? mobTemplateId != MainQuests.MOB_SCARECROW
            : step.kind() == StepKind.COLLECT_ELITE && mobTemplateId == step.arrowMob()
                  && variant != MobRules.VARIANT_NORMAL;
      if (!drops) {
         return null;
      }
      ItemData item = ItemData.of(step.target(), 1);
      item.locked = true;
      return item;
   }

   /** 1 đòn gây sát thương lên boss thế giới {@code worldBossId} (luồng tick khu): bước {@link StepKind#HIT_WORLD_BOSS}. */
   public static void worldBossHit(WorldPlayer player, int worldBossId) {
      QuestService s = service;
      QuestStep step = QuestService.currentStep(player.data().mainQuest);
      if (s != null && step != null && step.kind() == StepKind.HIT_WORLD_BOSS && step.target() == worldBossId) {
         post(s, player, StepKind.HIT_WORLD_BOSS, worldBossId);
      }
   }

   /**
    * Boss {@code player} đã gây sát thương vừa chết (luồng tick khu): đang ở bước {@link StepKind#COLLECT_BOSS} -> 1 món
    * nhiệm vụ (khoá) để rơi RIÊNG cho họ; null = không.
    */
   public static ItemData bossDrop(WorldPlayer player) {
      if (player.data().bot) {
         return null;
      }
      QuestStep step = QuestService.currentStep(player.data().mainQuest);
      if (service == null || step == null || step.kind() != StepKind.COLLECT_BOSS) {
         return null;
      }
      ItemData item = ItemData.of(step.target(), 1);
      item.locked = true;
      return item;
   }

   /** 1 đòn gây sát thương lên boss Tế đàn ở map {@code mapId} (luồng tick khu): bước {@link StepKind#HIT_BOSS}. */
   public static void bossHit(WorldPlayer player, int mapId) {
      QuestService s = service;
      QuestStep step = QuestService.currentStep(player.data().mainQuest);
      if (s != null && step != null && step.kind() == StepKind.HIT_BOSS && step.target() == mapId) {
         post(s, player, StepKind.HIT_BOSS, mapId);
      }
   }

   /** Lên cấp trên LUỒNG TICK KHU (EXP đánh quái): tính lại bước "Đạt cấp". */
   public static void levelChanged(WorldPlayer player) {
      QuestService s = service;
      QuestStep step = QuestService.currentStep(player.data().mainQuest);
      if (s != null && step != null && step.kind() == StepKind.REACH_LEVEL) {
         post(s, player, StepKind.REACH_LEVEL, 0);
      }
   }

   /**
    * Sự kiện cho người chơi {@code playerId} từ BẤT KỲ luồng nào (vd trưởng nhóm duyệt -> người được vào tổ đội):
    * chuyển về hàng đợi session của chính người đó. Offline thì bỏ.
    */
   public static void fireFor(int playerId, StepKind kind, int target) {
      QuestService s = service;
      WorldPlayer player = s == null ? null : s.online(playerId);
      if (player != null) {
         post(s, player, kind, target);
      }
   }

   private static void post(QuestService s, WorldPlayer player, StepKind kind, int target) {
      if (player.data().bot) {
         return;
      }
      PlayerSession session = player.session();
      session.inbox().execute(() -> s.onEvent(session, kind, target, 1), s.gamePool());
   }

   /**
    * Vừa vào khu (luồng tick khu): lần đầu vào game gửi cả nhiệm vụ, mọi lần gửi mũi tên chỉ đường theo map mới.
    * Chỉ đọc trạng thái nhiệm vụ (bất biến) — nhân vật chưa có nhiệm vụ thì nhận nhiệm vụ 1 ở đây (chưa ai khác sửa).
    */
   public static void joinedMap(WorldPlayer player, boolean firstJoin, IntFunction<MapTemplate> maps) {
      QuestService s = service;
      if (s == null || player.data().bot) {
         return;
      }
      PlayerData d = player.data();
      PlayerData.MainQuestProgress q = d.mainQuest;
      if (q == null) {
         q = QuestService.progress(d);
      }
      MainQuest quest = MainQuests.get(q.questId());
      if (firstJoin) {
         player.send(QuestPackets.full(quest, q, System.currentTimeMillis()));
      }
      QuestStep step = quest.step(q.stage());
      if (!com.tlnt.server.worldboss.WorldBossPath.guide(player, d, maps)) { // đang "Tìm đường" tới boss: mũi tên boss
         player.send(QuestGuide.arrow(step, d.mapId, maps));
      }
      if (step != null && step.kind() == StepKind.REACH_MAP && step.target() == d.mapId) {
         post(s, player, StepKind.REACH_MAP, d.mapId);
      }
   }

   /** Đang ở bước có sinh vật hộ tống ({@link QuestStep#escortItem}). */
   public static boolean atEscortStep(PlayerData d) {
      QuestStep step = QuestService.currentStep(d.mainQuest);
      return step != null && step.escortItem() > 0;
   }

   /**
    * Hộ tống thất bại (luồng tick khu — sinh vật đã bị xoá): chuyển về hàng đợi session, lùi về bước trước
    * ({@link QuestService#escortFailed}).
    */
   public static void escortFailed(WorldPlayer player, String reason) {
      QuestService s = service;
      if (s != null) {
         PlayerSession session = player.session();
         session.inbox().execute(() -> s.escortFailed(session, reason), s.gamePool());
      }
   }

   /** Đi được tới map {@code mapId} chưa (cửa dịch chuyển / bản đồ thế giới). */
   public static boolean canEnter(PlayerData d, int mapId) {
      return MainQuests.canEnter(d.mainQuest, mapId);
   }

   public static boolean chestUnlocked(PlayerData d) {
      return MainQuests.chestUnlocked(d.mainQuest);
   }
}
