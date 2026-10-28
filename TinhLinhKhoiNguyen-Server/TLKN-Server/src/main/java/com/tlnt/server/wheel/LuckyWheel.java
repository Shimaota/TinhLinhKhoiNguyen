package com.tlnt.server.wheel;

import com.tlnt.server.item.RewardItem;

import java.util.List;

/**
 * Vòng quay may mắn (1 dòng {@code tlnt_data.lucky_wheel}). Client vẽ đúng 12 ô ({@code LuckyWheelPanel}), chữ cố
 * định "Cần 1 Đồng xu may mắn để quay" -> mặc định 1 xu (mẫu 307) / lần.
 *
 * @param costItem mẫu item tốn mỗi lần quay (Đồng xu may mắn)
 * @param costQty số item tốn mỗi lần quay
 * @param slots các ô theo thứ tự trên vòng quay
 * @param totalWeight tổng trọng số mọi ô (> 0)
 */
record LuckyWheel(int id, String name, int costItem, int costQty, List<Slot> slots, int totalWeight) {

   /**
    * 1 ô.
    *
    * @param weight trọng số (tỉ lệ trúng = weight / tổng) — nhỏ = hiếm
    * @param permPercent % ra VĨNH VIỄN cho món có hạn dùng "HSD {0} ngày" (dòng 57), hiện "Có tỉ lệ vĩnh viễn"
    *        (dòng 55) — chỉ có nghĩa khi món có dòng 57
    */
   record Slot(RewardItem item, int weight, int permPercent) {
   }

   /** % trúng ô {@code index} (để log / thông báo món hiếm). */
   double percent(int index) {
      return slots.get(index).weight() * 100.0 / totalWeight;
   }
}
