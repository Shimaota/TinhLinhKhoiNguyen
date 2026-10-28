package com.tlnt.server.ranking;

import com.tlnt.server.item.RewardItem;
import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình Top tuần trong RAM (nạp lại mỗi phút từ {@code tlnt_data.weekly_top}). Chỉ dòng có {@link WeeklyKind}
 * tương ứng (id 1-4) mới hiện / trả thưởng.
 */
public final class WeeklyRewards {

   /** 1 bảng tuần: {@code tops.get(0..2)} = quà hạng 1..3. */
   public record Event(int id, String name, List<List<RewardItem>> tops) {

      /** Quà hạng {@code rank} (1-3), item thật tạo lúc {@code now}. */
      public List<ItemData> create(int rank, long now) {
         List<ItemData> items = new ArrayList<>();
         if (rank >= 1 && rank <= tops.size()) {
            for (RewardItem r : tops.get(rank - 1)) {
               items.add(r.create(now));
            }
         }
         return items;
      }
   }

   private static volatile List<Event> events = List.of();

   private WeeklyRewards() {
   }

   static List<Event> events() {
      return events;
   }

   static void load(List<Event> loaded) {
      List<Event> valid = new ArrayList<>();
      for (Event e : loaded) {
         if (WeeklyKind.of(e.id()) != null) {
            valid.add(e);
         }
      }
      events = List.copyOf(valid);
   }
}
