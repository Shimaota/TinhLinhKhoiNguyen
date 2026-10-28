package com.tlnt.server.bot;

import com.tlnt.server.item.RewardItem;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Ngoại trang / danh hiệu / mặt nạ / thú cưng / đeo lưng bot được dùng — 5 danh sách JSON trong {@code
 * tlnt_data.bot_profile} (admin tự đặt món + dòng chỉ số, kể cả khoảng [min, max] roll mỗi bot; ghi 1 món nhiều lần =
 * gặp nhiều hơn). Hạt giống (migration): chỉ món có chỉ số ở nguồn dễ kiếm — shop, Cẩm nang, thành tựu, mốc phúc lợi,
 * NV hằng ngày (×2), combo / vòng quay (×1); không món boss thế giới / Top tuần / chỉ giftcode. Bất biến.
 */
final class BotCosmetics {

   static final int COSTUME = 19;
   static final int TITLE = 20;
   static final int MASK = 36;
   static final int PET = 37;
   static final int BACK = 48;

   private final Map<Integer, List<RewardItem>> byType;

   BotCosmetics(Map<Integer, List<RewardItem>> byType) {
      this.byType = Map.copyOf(byType);
   }

   static BotCosmetics empty() {
      return new BotCosmetics(Map.of());
   }

   /** 1 món loại {@code typeId} (đều nhau theo số lần ghi), null = danh sách trống. */
   RewardItem pick(int typeId, Random rnd) {
      List<RewardItem> list = byType.get(typeId);
      return list == null || list.isEmpty() ? null : list.get(rnd.nextInt(list.size()));
   }

   int count(int typeId) {
      List<RewardItem> list = byType.get(typeId);
      return list == null ? 0 : list.size();
   }
}
