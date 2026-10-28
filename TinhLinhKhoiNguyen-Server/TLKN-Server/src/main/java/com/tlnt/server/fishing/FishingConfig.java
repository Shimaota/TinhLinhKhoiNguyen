package com.tlnt.server.fishing;

import com.tlnt.server.item.RewardItem;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cấu hình câu cá (1 dòng bảng {@code tlnt_data.fishing}, {@link FishingRepository}): cần câu phải cầm, mồi tốn mỗi
 * lần quăng cần, bảng vật phẩm câu được.
 *
 * <p>Mỗi món có 2 tỉ lệ (%): {@code w} khi kéo lúc kim ở vùng VÀNG, {@code wg} khi ở vùng XANH (khó hơn -> đồ giá trị
 * hơn). Tổng &lt; 100 -> phần còn lại là "cá sổng" (mất mồi, không được gì); tổng &gt; 100 -> chia theo tỉ lệ.
 *
 * @param rodItem {@code item_template.id} cần câu (phải đang cầm ở ô vũ khí)
 * @param baitItem {@code item_template.id} mồi câu (tốn 1 mỗi lần quăng cần, câu được hay hụt đều mất)
 */
public record FishingConfig(int rodItem, int baitItem, List<Loot> loot) {

   /** 1 món câu được + tỉ lệ (%) ở vùng vàng / vùng xanh. */
   public record Loot(RewardItem item, int weight, int greenWeight) {
   }

   /**
    * Dùng khi bảng {@code fishing} chưa có / đọc lỗi. Vùng vàng: 3 loại cá 30% mỗi loại, 10 mảnh vũ khí 1% mỗi loại.
    * Vùng xanh: cá 10%, mảnh 7%. Tất cả KHOÁ.
    */
   public static final FishingConfig DEFAULT = new FishingConfig(164, 152, List.of(
         loot(251, 30, 10), loot(253, 30, 10), loot(254, 30, 10),
         loot(177, 1, 7), loot(178, 1, 7), loot(179, 1, 7), loot(180, 1, 7), loot(181, 1, 7),
         loot(182, 1, 7), loot(183, 1, 7), loot(184, 1, 7), loot(185, 1, 7), loot(186, 1, 7)));

   private static Loot loot(int templateId, int weight, int greenWeight) {
      return new Loot(new RewardItem(templateId, 1, true, new int[0][], 0), weight, greenWeight);
   }

   /** Tổng tỉ lệ các món của 1 vùng. */
   public int totalWeight(boolean green) {
      int total = 0;
      for (Loot l : loot) {
         total += Math.max(0, green ? l.greenWeight() : l.weight());
      }
      return total;
   }

   /** Quay 1 lần theo bảng của vùng. @return món câu được, null = cá sổng (không được gì) */
   public RewardItem roll(boolean green) {
      int total = totalWeight(green);
      if (total <= 0) {
         return null;
      }
      int r = ThreadLocalRandom.current().nextInt(Math.max(100, total));
      for (Loot l : loot) {
         r -= Math.max(0, green ? l.greenWeight() : l.weight());
         if (r < 0) {
            return l.item();
         }
      }
      return null;
   }
}
