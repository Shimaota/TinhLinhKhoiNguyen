package com.tlnt.server.farm;

import java.util.List;

/**
 * Nông trại của 1 nhân vật (cột JSON {@code player.farm}). Chỉ luồng session ghi; luồng tick khu đọc {@link #apple} /
 * {@link #pigs} (volatile, bản bất biến) để gửi trạng thái lúc vào map.
 */
public final class FarmState {

   /** Cây táo; null = chưa từng đụng tới (tạo lúc cần, {@link #apple(long)}). */
   public volatile AppleTree apple;
   /** Heo đang nuôi (bản bất biến — đổi = thay cả danh sách); null = chưa nuôi con nào. */
   public volatile List<Pig> pigs;
   /** Id con heo kế tiếp (id thực thể op 52 trong khu nông trại riêng). */
   public int nextPigId = 1;

   /**
    * 1 con heo: thả ở ({@code x}, {@code y}) lúc {@code bornAt} (ms), lớn sau {@link PigService#GROW_MS}.
    *
    * @param type {@code tlnt_data.animal_type.id} (1 = Heo con / Heo trưởng thành)
    */
   public record Pig(int id, int type, long bornAt, float x, float y) {

      public boolean mature(long now) {
         return now - bornAt >= PigService.GROW_MS;
      }

      /** ms còn lại tới lúc lớn (0 = đã lớn). */
      public int remainingMs(long now) {
         return (int) Math.max(0, Math.min(Integer.MAX_VALUE, bornAt + PigService.GROW_MS - now));
      }
   }

   /** Danh sách heo (không null). */
   public List<Pig> pigs() {
      List<Pig> list = pigs;
      return list == null ? List.of() : list;
   }

   /** Không có gì để lưu (chưa từng có cây táo / heo). */
   public boolean isEmpty() {
      return apple == null && pigs().isEmpty();
   }

   /** Cây táo đã chốt lúc {@code now} (nâng cấp tới giờ thì lên cấp). Chưa có thì trồng cây cấp 1. Chỉ luồng session. */
   public AppleTree apple(long now) {
      AppleTree tree = apple;
      AppleTree settled = tree == null ? AppleTree.fresh(now) : tree.settle(now);
      if (settled != tree) {
         apple = settled;
      }
      return settled;
   }
}
