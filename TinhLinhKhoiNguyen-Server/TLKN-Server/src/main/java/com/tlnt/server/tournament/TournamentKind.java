package com.tlnt.server.tournament;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

/**
 * 5 giải của Giải đấu Bộ lạc (hướng dẫn {@code tlnt_data.guide_topic} 1/9 + spec người dùng 2026-10-11).
 *
 * <pre>
 * giải         thể thức   cấp       mở (giờ server)        lệ phí
 * Nhi đồng     DAILY      5-25      09:00 hằng ngày        50.000 vàng
 * Chiến binh   DAILY      26-45     12:00 hằng ngày        200.000 vàng
 * Thủ lĩnh     DAILY      46+       17:00 hằng ngày        500.000 vàng
 * Anh hùng     HERO       5+        20:00 Thứ 2, 4, 6      —
 * Hợp chiến    TEAM       5+        20:00 Thứ 3, 5, 7      — (phải có tổ đội)
 * </pre>
 */
public enum TournamentKind {

   KID("Giải Nhi đồng", Format.DAILY, 5, 25, 9, 50_000L, EnumSet.allOf(DayOfWeek.class)),
   WARRIOR("Giải Chiến binh", Format.DAILY, 26, 45, 12, 200_000L, EnumSet.allOf(DayOfWeek.class)),
   LEADER("Giải Thủ lĩnh", Format.DAILY, 46, Integer.MAX_VALUE, 17, 500_000L, EnumSet.allOf(DayOfWeek.class)),
   HERO("Giải Anh hùng", Format.HERO, 5, Integer.MAX_VALUE, 20, 0L,
         EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
   TEAM("Giải Hợp chiến", Format.TEAM, 5, Integer.MAX_VALUE, 20, 0L,
         EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY));

   /**
    * DAILY: đăng ký ở Tư tế (mất lệ phí) rồi 1 vs 1 loại trực tiếp. HERO: vòng loại hỗn chiến rồi 16 người 1 vs 1. TEAM:
    * vòng loại theo tổ đội rồi 16 tổ đội 5 vs 5.
    */
   public enum Format {
      DAILY, HERO, TEAM
   }

   public final String title;
   public final Format format;
   public final int minLevel;
   public final int maxLevel;
   /** Giờ mở đăng ký / tập trung (phút 0). */
   public final int openHour;
   public final long fee;
   public final Set<DayOfWeek> days;

   TournamentKind(String title, Format format, int minLevel, int maxLevel, int openHour, long fee,
         Set<DayOfWeek> days) {
      this.title = title;
      this.format = format;
      this.minLevel = minLevel;
      this.maxLevel = maxLevel;
      this.openHour = openHour;
      this.fee = fee;
      this.days = days;
   }

   public boolean daily() {
      return format == Format.DAILY;
   }

   public boolean team() {
      return format == Format.TEAM;
   }

   public boolean levelOk(int level) {
      return level >= minLevel && level <= maxLevel;
   }

   /** "cấp 5 - 25" / "cấp 46 trở lên". */
   public String levelText() {
      return maxLevel == Integer.MAX_VALUE ? "cấp " + minLevel + " trở lên" : "cấp " + minLevel + " - " + maxLevel;
   }
}
