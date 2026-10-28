package com.tlnt.server.item;

import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Kho có số ô cố định: hành trang (bag), rương (box), đồ đang mặc (body, 10 ô theo {@code item_type.equip_slot}).
 * Trong RAM là mảng ô ({@code null} = trống) để tra theo ô O(1) — đúng cách client đánh số ({@code item.slotIndex}).
 *
 * Lưu DB dạng GỌN (chỉ ô có đồ, khoá ngắn — xem {@link ItemCodec}): {@code {"3":{"i":38,"l":1}}}, kho trống là
 * {@code {}}.
 */
public final class ItemContainer {

   private ItemData[] slots;

   public ItemContainer(int size) {
      this.slots = new ItemData[Math.max(0, size)];
   }

   public int size() {
      return slots.length;
   }

   /** Đổi số ô (mở rộng). Thu nhỏ chỉ khi các ô bị cắt đang trống. */
   public boolean resize(int size) {
      for (int i = size; i < slots.length; i++) {
         if (slots[i] != null) {
            return false;
         }
      }
      slots = Arrays.copyOf(slots, Math.max(0, size));
      return true;
   }

   /** Bản sao sâu (từng món cũng sao) — thử thay đổi trên bản sao, được hết mới thay kho thật (giao dịch). */
   public ItemContainer deepCopy() {
      ItemContainer copy = new ItemContainer(slots.length);
      for (int i = 0; i < slots.length; i++) {
         copy.slots[i] = slots[i] == null ? null : slots[i].copy();
      }
      return copy;
   }

   public boolean valid(int slot) {
      return slot >= 0 && slot < slots.length;
   }

   public ItemData get(int slot) {
      return valid(slot) ? slots[slot] : null;
   }

   public void set(int slot, ItemData item) {
      slots[slot] = item;
   }

   public ItemData take(int slot) {
      ItemData item = slots[slot];
      slots[slot] = null;
      return item;
   }

   /** Ô trống đầu tiên, -1 nếu đầy. */
   public int firstEmpty() {
      for (int i = 0; i < slots.length; i++) {
         if (slots[i] == null) {
            return i;
         }
      }
      return -1;
   }

   public int countEmpty() {
      int n = 0;
      for (ItemData item : slots) {
         if (item == null) {
            n++;
         }
      }
      return n;
   }

   /** Còn đủ chỗ cho cả {@code item} không (cùng cách tính như {@link #add}, không đổi kho). */
   public boolean canFit(ItemData item) {
      if (!item.stackable()) {
         return firstEmpty() >= 0;
      }
      long space = 0;
      for (ItemData slot : slots) {
         if (slot == null) {
            space += ItemData.MAX_STACK;
         } else if (slot.canStackWith(item)) {
            space += Math.max(0, ItemData.MAX_STACK - slot.quantity);
         }
         if (space >= item.quantity) {
            return true;
         }
      }
      return false;
   }

   /**
    * Cho item vào kho: cộng dồn vào các chồng cùng loại (mỗi ô tối đa {@link ItemData#MAX_STACK}), phần dư sang ô trống
    * (mỗi ô tối đa {@link ItemData#MAX_STACK}). TẤT CẢ HOẶC KHÔNG: không đủ chỗ cho hết -> -1, kho không đổi.
    * {@code item} có thể được đặt thẳng vào 1 ô (giữ nguyên object, số lượng = phần còn lại) — người gọi không dùng lại.
    * @return ô đầu tiên nhận đồ, -1 nếu không đủ chỗ.
    */
   public int add(ItemData item) {
      if (!item.stackable()) {
         int empty = firstEmpty();
         if (empty >= 0) {
            slots[empty] = item;
         }
         return empty;
      }
      int need = item.quantity;
      long space = 0;
      for (ItemData slot : slots) {
         if (slot == null) {
            space += ItemData.MAX_STACK;
         } else if (slot.canStackWith(item)) {
            space += Math.max(0, ItemData.MAX_STACK - slot.quantity);
         }
         if (space >= need) {
            break;
         }
      }
      if (space < need) {
         return -1;
      }
      int first = -1;
      for (int i = 0; i < slots.length && need > 0; i++) { // 1. lấp đầy chồng sẵn có
         if (slots[i] != null && slots[i].canStackWith(item) && slots[i].quantity < ItemData.MAX_STACK) {
            int moved = Math.min(need, ItemData.MAX_STACK - slots[i].quantity);
            slots[i].quantity += moved;
            need -= moved;
            first = first < 0 ? i : first;
         }
      }
      for (int i = 0; i < slots.length && need > 0; i++) { // 2. phần dư sang ô trống
         if (slots[i] == null) {
            int chunk = Math.min(need, ItemData.MAX_STACK);
            need -= chunk;
            ItemData placed = need > 0 ? item.copy() : item; // chồng cuối dùng chính object
            placed.quantity = chunk;
            slots[i] = placed;
            first = first < 0 ? i : first;
         }
      }
      return first;
   }

   /** Danh sách đủ ô (null = trống) để gửi client (opcode 22 sub 0/1/2). */
   public List<ItemData> asList() {
      return Arrays.asList(slots);
   }

   // ------------------------------------------------------------------ hết hạn

   /** Gỡ mọi món hết hạn, cho vào {@code removed}. @return có gỡ món nào không. */
   public boolean removeExpired(long now, List<ItemData> removed) {
      boolean any = false;
      for (int i = 0; i < slots.length; i++) {
         if (slots[i] != null && slots[i].isExpired(now)) {
            removed.add(slots[i]);
            slots[i] = null;
            any = true;
         }
      }
      return any;
   }

   /** Hạn sớm nhất trong kho, {@link Long#MAX_VALUE} nếu không có món nào có hạn. */
   public long earliestExpire() {
      long min = Long.MAX_VALUE;
      for (ItemData item : slots) {
         if (item != null && item.expireTime > 0) {
            min = Math.min(min, item.expireTime);
         }
      }
      return min;
   }

   // ------------------------------------------------------------------ sắp xếp

   /**
    * Nút "Sắp xếp": gộp chồng cùng loại ({@link ItemData#canStackWith}), xếp theo loại -> mẫu -> sao (cao trước) ->
    * đã khoá -> số lượng (nhiều trước), dồn về từ ô 0.
    *
    * @return ô cũ -> ô mới của món còn lại (-1 = đã gộp vào chồng khác, xem {@code mergedInto}) để sửa ô dùng nhanh.
    */
   public int[] sort(int[] mergedInto) {
      int n = slots.length;
      int[] newSlotOfOld = new int[n];
      Arrays.fill(newSlotOfOld, -1);
      Arrays.fill(mergedInto, -1);

      // 1. Gộp chồng: món sau gộp vào món trước cùng loại.
      List<Integer> kept = new ArrayList<>();
      for (int i = 0; i < n; i++) {
         ItemData item = slots[i];
         if (item == null) {
            continue;
         }
         // Gộp tới khi chồng nhận đủ MAX_STACK; dư thì món này giữ phần còn lại ở ô riêng.
         for (int k : kept) {
            if (item.quantity > 0 && slots[k].canStackWith(item) && slots[k].quantity < ItemData.MAX_STACK) {
               int moved = Math.min(item.quantity, ItemData.MAX_STACK - slots[k].quantity);
               slots[k].quantity += moved;
               item.quantity -= moved;
               if (item.quantity == 0) {
                  mergedInto[i] = k;
               }
            }
         }
         if (item.quantity > 0) {
            kept.add(i);
         }
      }

      // 2. Sắp xếp các món còn lại.
      kept.sort(Comparator
            .comparingInt((Integer i) -> slots[i].typeId())
            .thenComparingInt(i -> slots[i].templateId)
            .thenComparingInt(i -> -slots[i].starGrade)
            .thenComparingInt(i -> -slots[i].starCount)
            .thenComparing(i -> !slots[i].locked)
            .thenComparingInt(i -> -slots[i].quantity));

      // 3. Dồn về đầu kho.
      ItemData[] sorted = new ItemData[n];
      for (int pos = 0; pos < kept.size(); pos++) {
         int old = kept.get(pos);
         sorted[pos] = slots[old];
         newSlotOfOld[old] = pos;
      }
      // ô cũ bị gộp -> trỏ theo ô mới của chồng nhận
      for (int i = 0; i < n; i++) {
         if (mergedInto[i] >= 0) {
            mergedInto[i] = newSlotOfOld[mergedInto[i]];
         }
      }
      slots = sorted;
      return newSlotOfOld;
   }

   // ------------------------------------------------------------------ lưu DB (định dạng: ItemCodec)

   /** JSON gọn {@code {"ô":item}}, chỉ ô có đồ. */
   public String toJson() {
      return ItemCodec.containerToJson(this);
   }

   /** Đọc JSON gọn {@code {"ô":item}}. */
   public static ItemContainer fromJson(String json, int size) {
      return ItemCodec.containerFromJson(json, size);
   }
}
