package com.tlnt.server.welfare;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Bảng quà phúc lợi trong RAM, nạp từ {@code tlnt_data.welfare} ({@link WelfareRepository}) — sửa quà = sửa SQL / TLNT-Editor
 * (server tự nạp lại mỗi phút, người vào game sau thấy quà mới). Snapshot bất biến, thay tham chiếu volatile.
 *
 * Mốc đã nhận lưu theo SỐ THỨ TỰ mốc -> đừng chèn mốc vào giữa tab đang có người nhận; tối đa {@link #MAX_MILESTONES}.
 */
final class WelfareRewards {

   /** 1 mốc: mục tiêu (phút online / thứ trong tuần 0..6 / lam thạch đã nạp) + quà. */
   record Milestone(int target, List<RewardItem> rewards) {

      List<ItemData> create(long now) {
         List<ItemData> items = new ArrayList<>(rewards.size());
         for (RewardItem reward : rewards) {
            items.add(reward.create(now));
         }
         return items;
      }
   }

   static final int MAX_MILESTONES = 31;
   static final String[] DAY_NAMES = {"Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy", "Chủ nhật"};

   private static volatile Map<WelfareTab, List<Milestone>> table = build(Map.of());

   private WelfareRewards() {
   }

   static List<Milestone> of(WelfareTab tab) {
      return table.get(tab);
   }

   /** Thay cả bảng. Mốc điểm danh ngoài 0..6 bị bỏ, quá {@link #MAX_MILESTONES} mốc bị cắt, món sai mẫu thì cảnh báo. */
   static void load(Map<WelfareTab, List<Milestone>> loaded) {
      for (Map.Entry<WelfareTab, List<Milestone>> e : loaded.entrySet()) {
         for (Milestone m : e.getValue()) {
            for (RewardItem item : m.rewards()) {
               if (GameDataCache.itemTemplate(item.templateId()) == null) {
                  Log.warn("welfare tab %s: khong co item_template id=%d", e.getKey(), item.templateId());
               }
            }
         }
      }
      table = build(loaded);
   }

   private static Map<WelfareTab, List<Milestone>> build(Map<WelfareTab, List<Milestone>> loaded) {
      Map<WelfareTab, List<Milestone>> map = new EnumMap<>(WelfareTab.class);
      for (WelfareTab tab : WelfareTab.values()) {
         List<Milestone> list = new ArrayList<>(loaded.getOrDefault(tab, List.of()));
         if (tab == WelfareTab.CHECKIN) {
            list.removeIf(m -> m.target() < 0 || m.target() >= DAY_NAMES.length);
         }
         map.put(tab, List.copyOf(list.subList(0, Math.min(list.size(), MAX_MILESTONES))));
      }
      return map;
   }
}
