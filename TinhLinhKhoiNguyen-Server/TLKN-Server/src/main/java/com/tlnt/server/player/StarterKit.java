package com.tlnt.server.player;

import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.util.Log;

import java.util.List;

/**
 * Đồ có sẵn của nhân vật mới tạo ({@link PlayerService} lúc tạo, trước khi INSERT): hành trang giống nhau cho cả 3
 * nhân vật + 1 áo trong rương theo nhân vật. Mọi món đều KHOÁ (không giao dịch / kí gửi) để khỏi tạo acc phụ lấy đồ.
 * Option dạng khoảng -> random mỗi nhân vật 1 lần ({@link RewardItem#create}).
 */
final class StarterKit {

   /** Dòng chỉ số 15 = Giáp. */
   private static final int STAT_ARMOR = 15;

   private static final List<RewardItem> BAG = List.of(
         item(238, 5),       // Cỏ hồi sinh
         item(150, 10),      // Đùi gà
         item(153, 5),       // Thuốc Trí tuệ I
         item(154, 5),       // Thuốc Sức mạnh I
         item(155, 5),       // Thuốc Cứng cáp I
         item(156, 5),       // Thuốc Sinh lực
         item(157, 5),       // Thuốc Trí lực
         item(224, 10),      // Táo xanh cấp 1
         item(214, 10),      // Táo đỏ cấp 1
         expiring(294, 7),   // Túi đựng Táo - 7 ngày
         expiring(323, 3));  // Rương Ngoại trang - 3 ngày

   private StarterKit() {
   }

   static void give(PlayerData player, long now) {
      for (RewardItem reward : BAG) {
         put(player, player.bag, reward, now);
      }
      put(player, player.box, chestArmor(player.gender), now);
   }

   /** Áo trong rương theo nhân vật (đúng {@code item_template.class_id} để mặc được). */
   private static RewardItem chestArmor(int gender) {
      return switch (gender) {
         case Gender.LYRIA -> armor(8, 51, 69);    // Áo sợi lanh
         case Gender.BRUK -> armor(68, 107, 145);  // Áo thổ cẩm
         default -> armor(38, 67, 91);             // Áo lông sói (Rinn)
      };
   }

   private static void put(PlayerData player, ItemContainer box, RewardItem reward, long now) {
      if (box.add(reward.create(now)) < 0) {
         Log.warn("Nhan vat moi '%s': kho day, bo qua item %d", player.name, reward.templateId());
      }
   }

   private static RewardItem item(int templateId, int quantity) {
      return new RewardItem(templateId, quantity, true, new int[0][], 0);
   }

   private static RewardItem expiring(int templateId, int days) {
      return new RewardItem(templateId, 1, true, new int[0][], days);
   }

   private static RewardItem armor(int templateId, int min, int max) {
      return new RewardItem(templateId, 1, true, new int[][] {{STAT_ARMOR, min}}, new int[][] {{STAT_ARMOR, max}}, 0);
   }
}
