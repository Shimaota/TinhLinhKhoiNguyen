package com.tlnt.server.achievement;

import com.tlnt.server.item.RewardItem;

import java.util.List;

/**
 * 1 thành tựu (1 dòng {@code tlnt_data.achievement}).
 *
 * @param param mốc của chuỗi {@link AchievementType#max} (cấp / số sao), 0 với chuỗi đếm
 * @param target số cần đạt (chuỗi max: 1)
 */
public record Achievement(int id, AchievementType type, int param, String title, String description, int target,
      List<RewardItem> rewards, int sort) {

   public static final byte IN_PROGRESS = 0;
   public static final byte CLAIMABLE = 1;
   public static final byte CLAIMED = 2;

   /** Tiến độ hiện ở client với bộ đếm {@code count}. */
   public int progress(long count) {
      if (type.max) {
         return count >= param ? target : 0;
      }
      return (int) Math.min(count, target);
   }

   /** 0 đang làm / 1 nhận được / 2 đã nhận. */
   public byte status(long count, boolean claimed) {
      return claimed ? CLAIMED : progress(count) >= target ? CLAIMABLE : IN_PROGRESS;
   }
}
