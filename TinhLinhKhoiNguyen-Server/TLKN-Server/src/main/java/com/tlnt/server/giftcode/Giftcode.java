package com.tlnt.server.giftcode;

import com.tlnt.server.item.RewardItem;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 1 giftcode (1 dòng {@code tlnt_data.giftcode} + các dòng quà {@code giftcode_reward}). Bất biến.
 *
 * @param code mã, CHỮ HOA (nhập không phân biệt hoa thường)
 * @param startAt / endAt epoch ms, 0 = không giới hạn
 * @param maxLevel 0 = không giới hạn
 * @param classIds loại vũ khí được nhập (1 Chưa vũ khí, 2 Rìu, 3 Cung, 4 Ná, 5 Giáo, 6 Roi), rỗng = mọi class
 * @param genders nhân vật được nhập (1 Rinn, 2 Lyria, 3 Bruk), rỗng = tất cả
 * @param playerNames tên nhân vật được nhập (chữ thường), rỗng = ai cũng được
 * @param maxUses tổng lượt nhập cả server, 0 = không giới hạn
 * @param usesPerPlayer số lần 1 nhân vật được nhập, 0 = không giới hạn
 */
public record Giftcode(String code, String name, boolean listed, long startAt, long endAt, int minLevel, int maxLevel,
      Set<Integer> classIds, Set<Integer> genders, Set<String> playerNames, int maxUses, int usesPerPlayer,
      List<Reward> rewards) {

   /**
    * 1 món quà của mã. {@code classId} / {@code gender} = 0: ai cũng nhận; khác 0: chỉ nhân vật đúng class / giới tính
    * nhận món này -> cùng 1 mã, mỗi class / nhân vật nhận quà khác nhau.
    */
   public record Reward(int classId, int gender, RewardItem item) {
   }

   /** Điều kiện nhập (trừ số lượt). @return câu báo lỗi, null = được */
   String checkEligible(PlayerData d, long now) {
      if (startAt > 0 && now < startAt) {
         return "Mã quà tặng chưa đến thời gian sử dụng.";
      }
      if (endAt > 0 && now >= endAt) {
         return "Mã quà tặng đã hết hạn.";
      }
      if (d.level < minLevel) {
         return "Cần đạt cấp " + minLevel + " để nhập mã này.";
      }
      if (maxLevel > 0 && d.level > maxLevel) {
         return "Mã này chỉ dành cho nhân vật từ cấp " + maxLevel + " trở xuống.";
      }
      if (!classIds.isEmpty() && !classIds.contains(d.classId)) {
         return "Mã này không dành cho loại vũ khí của bạn.";
      }
      if (!genders.isEmpty() && !genders.contains(d.gender)) {
         return "Mã này không dành cho nhân vật của bạn.";
      }
      if (!playerNames.isEmpty() && !playerNames.contains(d.name.toLowerCase(Locale.ROOT))) {
         return "Mã này không dành cho bạn.";
      }
      return null;
   }

   /** Quà nhân vật {@code d} nhận được (lọc theo class / giới tính). */
   List<ItemData> rewardsFor(PlayerData d, long now) {
      List<ItemData> items = new ArrayList<>();
      for (Reward reward : rewards) {
         if ((reward.classId() == 0 || reward.classId() == d.classId)
               && (reward.gender() == 0 || reward.gender() == d.gender)) {
            items.add(reward.item().create(now));
         }
      }
      return items;
   }

   /** Nhân vật đã nhập {@code used} lần thì hết lượt chưa ({@link #usesPerPlayer} 0 = không bao giờ hết). */
   boolean usedUp(int used) {
      return usesPerPlayer > 0 && used >= usesPerPlayer;
   }

   boolean isActive(long now) {
      return (startAt == 0 || now >= startAt) && (endAt == 0 || now < endAt);
   }
}
