package com.tlnt.server.ranking;

import java.util.Locale;

/**
 * Các bảng xếp hạng (tab cửa sổ {@code RankingWindow}, thứ tự enum = thứ tự tab; {@link #id} = categoryId client gửi
 * lại). Thêm bảng = thêm 1 dòng ở đây + số liệu trong {@link RankingService.Row}.
 */
public enum RankType {

   /** Cấp cao trước, bằng cấp thì EXP cao trước. */
   LEVEL(0, "Cấp độ", "icon/272") {
      @Override
      long value(RankingService.Row r) {
         return r.level();
      }

      @Override
      long tiebreak(RankingService.Row r) {
         return r.exp();
      }

      @Override
      String format(long value) {
         return "Cấp " + value;
      }
   },
   /** Tổng vàng nhặt từ đất (quái / boss rơi) — {@code item/GroundItemService}. */
   SCAVENGER(2, "Ve chai", "icon/275") {
      @Override
      long value(RankingService.Row r) {
         return r.gold();
      }

      @Override
      String format(long value) {
         return number(value) + " vàng";
      }
   },
   /** Điểm Chiến binh: hạ Tinh anh +5, Thủ lĩnh +10 (người kết liễu — {@code world/ZoneCombat.onMobKilled}). */
   WARRIOR(1, "Chiến binh", "icon/274") {
      @Override
      long value(RankingService.Row r) {
         return r.warrior();
      }

      @Override
      String format(long value) {
         return number(value) + " điểm";
      }
   },
   /** Điểm cường hóa của 10 món đang mặc ({@link EnhanceScore}). */
   ENHANCE(4, "Cường hóa", "icon/276") {
      @Override
      long value(RankingService.Row r) {
         return r.enhance();
      }

      @Override
      String format(long value) {
         return number(value) + " điểm";
      }
   },
   /** Điểm sự kiện ({@code RankStats.eventPoints}) — Trung thu: bánh 15, cà rốt 1, cho thỏ ăn 3, đèn 5, boss 25. */
   EVENT(5, "Sự kiện", "icon/277") {
      @Override
      long value(RankingService.Row r) {
         return r.event();
      }

      @Override
      String format(long value) {
         return number(value) + " điểm";
      }
   };

   public final int id;
   public final String title;
   /** Icon tab ({@code DataGame/icon/...}). */
   public final String icon;

   RankType(int id, String title, String icon) {
      this.id = id;
      this.title = title;
      this.icon = icon;
   }

   abstract long value(RankingService.Row r);

   long tiebreak(RankingService.Row r) {
      return 0;
   }

   abstract String format(long value);

   /** Bảng cấp độ thì ai cũng có hạng; bảng khác chỉ người có điểm &gt; 0. */
   boolean ranked(long value) {
      return this == LEVEL || value > 0;
   }

   public static RankType of(int id) {
      for (RankType type : values()) {
         if (type.id == id) {
            return type;
         }
      }
      return null;
   }

   static String number(long value) {
      return String.format(Locale.US, "%,d", value).replace(',', '.');
   }
}
