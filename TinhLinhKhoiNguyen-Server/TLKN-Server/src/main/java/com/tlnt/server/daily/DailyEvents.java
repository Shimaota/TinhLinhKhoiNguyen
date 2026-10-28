package com.tlnt.server.daily;

import com.tlnt.server.combat.MobRules;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.WorldPlayer;

import java.util.List;

/**
 * Móc tĩnh cho hệ thống khác báo tiến độ nhiệm vụ hằng ngày (như {@code achievement/AchievementEvents}). Gọi từ BẤT KỲ
 * luồng nào: việc được chuyển về hàng đợi session của chính người chơi ({@link DailyQuestService#onEvent}); loại không có
 * NV đang mở thì bỏ ngay ({@link DailyState#watch}). Chưa khởi động -> bỏ qua.
 */
public final class DailyEvents {

   private static volatile DailyQuestService service;

   private DailyEvents() {
   }

   static void install(DailyQuestService s) {
      service = s;
   }

   public static DailyQuestService service() {
      return service;
   }

   /** +{@code amount} cho NV loại {@code kind} mục tiêu {@code target} (0 = loại không có mục tiêu). */
   public static void add(PlayerSession session, DailyKind kind, int target, long amount) {
      DailyQuestService s = service;
      if (s == null || session == null || amount <= 0 || session.isBot()) {
         return;
      }
      PlayerData d = session.getPlayer();
      DailyState state = d == null ? null : d.dailyQuest;
      if (state == null || (state.watch & kind.bit()) == 0) {
         return;
      }
      session.inbox().execute(() -> s.onEvent(session, kind, target, amount), s.gamePool);
   }

   public static void add(WorldPlayer player, DailyKind kind, int target, long amount) {
      if (player != null) {
         add(player.session(), kind, target, amount);
      }
   }

   /** Đồ vừa vào túi do NHẶT dưới đất / HÁI cây (vàng nhặt = item 1). */
   public static void obtained(PlayerSession session, List<ItemData> items) {
      for (ItemData item : items) {
         if (item != null) {
            add(session, DailyKind.PICK, item.templateId, item.quantity);
         }
      }
   }

   /** Đồ vừa MUA ở shop NPC. */
   public static void bought(PlayerSession session, List<ItemData> items) {
      for (ItemData item : items) {
         if (item != null) {
            add(session, DailyKind.BUY, item.templateId, item.quantity);
         }
      }
   }

   /**
    * Quái mẫu {@code mobTemplateId} biến thể {@code variant} bị hạ (luồng tick khu) — gọi cho người kết liễu VÀ đồng
    * đội cùng khu (như bước đánh quái nhiệm vụ chính tuyến).
    */
   public static void mobKilled(WorldPlayer player, int mobTemplateId, int variant) {
      add(player, DailyKind.KILL, mobTemplateId, 1);
      if (variant != MobRules.VARIANT_NORMAL) {
         add(player, DailyKind.KILL_ELITE, mobTemplateId, 1);
      }
   }
}
