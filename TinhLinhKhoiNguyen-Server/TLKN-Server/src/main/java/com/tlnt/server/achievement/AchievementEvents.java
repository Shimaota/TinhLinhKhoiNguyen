package com.tlnt.server.achievement;

import com.tlnt.server.combat.MobRules;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.WorldPlayer;

/**
 * Móc tĩnh cho hệ thống khác báo sự kiện thành tựu (như {@code quest/QuestEvents}). Gọi từ BẤT KỲ luồng nào: việc
 * được chuyển về hàng đợi session của chính người chơi ({@link AchievementService#onEvent}). Chưa khởi động -> bỏ qua.
 */
public final class AchievementEvents {

   private static volatile AchievementService service;

   private AchievementEvents() {
   }

   static void install(AchievementService s) {
      service = s;
   }

   static AchievementService service() {
      return service;
   }

   /** Sự kiện của người chơi sở hữu {@code session} (+{@code amount}; chuỗi max: giá trị mới). */
   public static void add(PlayerSession session, AchievementType type, long amount) {
      AchievementService s = service;
      if (s != null && session != null && amount > 0 && !session.isBot()) {
         session.inbox().execute(() -> s.onEvent(session, type, amount), s.gamePool);
      }
   }

   public static void add(WorldPlayer player, AchievementType type, long amount) {
      if (player != null) {
         add(player.session(), type, amount);
      }
   }

   /** Quái bị {@code killer} kết liễu (luồng tick khu): Tinh anh / Thủ lĩnh. */
   public static void mobKilled(WorldPlayer killer, int variant) {
      if (variant == MobRules.VARIANT_ELITE) {
         add(killer, AchievementType.ELITE, 1);
      } else if (variant == MobRules.VARIANT_BOSS) {
         add(killer, AchievementType.LEADER, 1);
      }
   }

   /** Boss thế giới {@code worldBossId} bị {@code killer} kết liễu (luồng tick khu; null = không có người kết liễu). */
   public static void worldBossKilled(WorldPlayer killer, int worldBossId) {
      AchievementService s = service;
      if (s == null || killer == null) {
         return;
      }
      if (s.guardianBosses.contains(worldBossId)) {
         add(killer, AchievementType.GUARDIAN, 1);
      }
      if (s.spiritBosses.contains(worldBossId)) {
         add(killer, AchievementType.SPIRIT, 1);
      }
   }
}
