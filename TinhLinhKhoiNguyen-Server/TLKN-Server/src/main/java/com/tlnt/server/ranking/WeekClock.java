package com.tlnt.server.ranking;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Tuần của Top tuần: từ thứ Hai 00:00 tới thứ Hai kế tiếp 00:00 (giờ máy chủ, như nhiệm vụ hằng ngày / phúc lợi). Mã
 * tuần = số tuần kể từ thứ Hai 1970-01-05 + 1 (luôn &gt; 0; 0 = "chưa có tuần nào").
 */
public final class WeekClock {

   static final ZoneId ZONE = ZoneId.systemDefault();
   /** 1970-01-05 là thứ Hai = ngày epoch 4. */
   private static final long FIRST_MONDAY_EPOCH_DAY = 4;

   private WeekClock() {
   }

   public static int weekId(long nowMs) {
      return weekId(nowMs, ZONE);
   }

   static int weekId(long nowMs, ZoneId zone) {
      LocalDate monday = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      return (int) ((monday.toEpochDay() - FIRST_MONDAY_EPOCH_DAY) / 7) + 1;
   }

   /** Thời điểm (epoch ms) tuần {@code weekId} kết thúc = thứ Hai 00:00 của tuần sau. */
   public static long weekEnd(int weekId) {
      return weekEnd(weekId, ZONE);
   }

   static long weekEnd(int weekId, ZoneId zone) {
      return LocalDate.ofEpochDay(FIRST_MONDAY_EPOCH_DAY + 7L * weekId).atStartOfDay(zone).toInstant().toEpochMilli();
   }
}
