package com.tlnt.server.item;

import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.List;

/**
 * Bộ sưu tập có 1 món đang dùng: ngoại trang (loại 19), danh hiệu (20), mặt nạ (36), thú cưng (37).
 * Client: danh sách opcode 22 sub 4/6/8/9 (list item + int ô đang dùng, -1 = không), panel "Trang phục",
 * "Danh hiệu", "Mặt nạ", "Thú cưng".
 *
 * Chỉ số: cộng TẤT CẢ món trong bộ sưu tập (vd 10 ngoại trang -> cộng chỉ số cả 10); món đang dùng
 * ({@link #selected}) chỉ quyết định ngoại hình. Danh sách luôn gọn (không có null). Lưu DB: {@link ItemCodec}.
 */
public final class ItemCollection {

   /** Số ô mặc định mỗi bộ sưu tập. */
   public static final int DEFAULT_CAPACITY = 9;
   /** Tối đa 30 ô mỗi bộ sưu tập, mở thêm từng ô bằng vàng (op 48/50/64/66 sub 2). */
   public static final int MAX_CAPACITY = 30;

   public List<ItemData> items = new ArrayList<>();
   /** Index món đang dùng trong {@link #items}, -1 = không dùng. */
   public int selected = -1;
   /** Số ô hiện có (lưu DB khi khác mặc định), tăng bằng nút "Mở rộng". */
   public int capacity = DEFAULT_CAPACITY;

   public ItemData getSelected() {
      return selected >= 0 && selected < items.size() ? items.get(selected) : null;
   }

   public boolean isFull() {
      return items.size() >= capacity;
   }

   public ItemData get(int index) {
      return index >= 0 && index < items.size() ? items.get(index) : null;
   }

   /** Thêm vào cuối, trả về index. */
   public int add(ItemData item) {
      items.add(item);
      return items.size() - 1;
   }

   /** Gỡ món ở {@code index}, dời {@link #selected} cho đúng. */
   public ItemData remove(int index) {
      ItemData removed = items.remove(index);
      if (selected == index) {
         selected = -1;
      } else if (selected > index) {
         selected--;
      }
      return removed;
   }

   /** Gỡ mọi món hết hạn (dời {@link #selected} cho đúng). @return có gỡ món nào không. */
   public boolean removeExpired(long now, List<ItemData> removed) {
      boolean any = false;
      for (int i = items.size() - 1; i >= 0; i--) {
         if (items.get(i).isExpired(now)) {
            removed.add(remove(i));
            any = true;
         }
      }
      return any;
   }

   /** Hạn sớm nhất, {@link Long#MAX_VALUE} nếu không có món nào có hạn. */
   public long earliestExpire() {
      long min = Long.MAX_VALUE;
      for (ItemData item : items) {
         if (item.expireTime > 0) {
            min = Math.min(min, item.expireTime);
         }
      }
      return min;
   }

   /** Sửa dữ liệu hỏng/cũ sau khi đọc DB: bỏ null, kẹp {@link #selected}. */
   public ItemCollection normalize() {
      if (items == null) {
         items = new ArrayList<>();
      }
      items.removeIf(java.util.Objects::isNull);
      if (selected < -1 || selected >= items.size()) {
         selected = -1;
      }
      capacity = Math.max(DEFAULT_CAPACITY, Math.min(MAX_CAPACITY, capacity));
      return this;
   }
}
