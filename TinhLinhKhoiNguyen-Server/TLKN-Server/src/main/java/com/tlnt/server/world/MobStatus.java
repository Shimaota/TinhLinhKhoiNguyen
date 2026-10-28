package com.tlnt.server.world;

/**
 * Hiệu ứng lên quái = sub của op 35 phía client ({@code MessageHandler case 35}, bản cho quái = sub lẻ). Có thời gian
 * (trói / suy yếu / choáng / băng): int id, bool bật, int thời gian ms — client tự hết giờ. Tức thì (sét): chỉ int id.
 * Quái đi lang thang ở client nên hiệu ứng chỉ là hình ảnh + client dừng quái (server không có AI quái).
 */
public enum MobStatus {
   ROOT(1, true),
   /** Thiêu đốt (client {@code BurnEffect}, vfxs/burn) — sát thương theo giây do server trừ. */
   BURN(3, true),
   WEAKEN(5, true),
   STUN(7, true),
   ICE(9, true),
   /** Tia sét đánh xuống vị trí quái ({@code LightningStrikeEffect}). */
   LIGHTNING(11, false),
   /** Gai đất trồi lên tại mục tiêu của {@code stick/skill7} ({@code EarthSpikeEffect}). */
   EARTH_SPIKE(13, false);

   final int sub;
   final boolean timed;

   MobStatus(int sub, boolean timed) {
      this.sub = sub;
      this.timed = timed;
   }

   /** Cột {@code skill_effect.status}: ice | weaken | lightning | stun | root; khác -> null. */
   public static MobStatus of(String name) {
      if (name == null) {
         return null;
      }
      return switch (name) {
         case "root" -> ROOT;
         case "burn" -> BURN;
         case "weaken" -> WEAKEN;
         case "stun" -> STUN;
         case "ice" -> ICE;
         case "lightning" -> LIGHTNING;
         default -> null;
      };
   }
}
