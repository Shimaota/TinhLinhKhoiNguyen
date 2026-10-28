package com.tlnt.server.shop;

import com.tlnt.server.item.RewardItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 1 cửa hàng NPC (1 dòng {@code tlnt_data.shop}, TLNT-Editor sửa): tên + các tab, mỗi tab các món bán.
 *
 * @param id shop id (client gửi lại khi mua)
 */
public record Shop(int id, String name, List<Tab> tabs) {

   /** Tiền tệ trả: vàng không khoá / vàng khoá (thiếu thì bù vàng thường) / lam thạch ({@code player.diamond}). */
   public static final int CUR_GOLD = 0;
   public static final int CUR_LOCKED_GOLD = 1;
   public static final int CUR_DIAMOND = 2;
   /** Trả bằng vật phẩm {@link Goods#payItem} trong hành trang (dùng món khoá thì hàng mua ra cũng khoá). */
   public static final int CUR_ITEM = 3;

   /**
    * Các tab nhân vật {@code classId} / {@code gender} nhìn thấy: tab lọc theo {@link Tab#classIds} / {@link Tab#genders},
    * hàng trong tab lọc theo {@link Goods#classId} / {@link Goods#gender}; tab lọc xong không còn món nào thì ẩn.
    * Client gửi lại chỉ số tab / ô theo ĐÚNG danh sách này -> giữ nó từ lúc mở đến lúc mua.
    */
   public List<Tab> viewFor(int classId, int gender) {
      List<Tab> view = new ArrayList<>(tabs.size());
      for (Tab tab : tabs) {
         if (!tab.visibleTo(classId, gender)) {
            continue;
         }
         List<Goods> goods = new ArrayList<>(tab.goods().size());
         for (Goods g : tab.goods()) {
            if (g.visibleTo(classId, gender)) {
               goods.add(g);
            }
         }
         if (!goods.isEmpty()) {
            view.add(goods.size() == tab.goods().size() ? tab : new Tab(tab.name(), tab.icon(), tab.classIds(),
                  tab.genders(), List.copyOf(goods)));
         }
      }
      return List.copyOf(view);
   }

   /**
    * @param icon icon tab ({@code icon/...})
    * @param classIds loại vũ khí thấy tab (1 Chưa vũ khí, 2 Rìu, 3 Cung, 4 Ná, 5 Giáo, 6 Roi), rỗng = mọi class
    * @param genders nhân vật thấy tab (1 Rinn, 2 Lyria, 3 Bruk), rỗng = tất cả
    */
   public record Tab(String name, String icon, Set<Integer> classIds, Set<Integer> genders, List<Goods> goods) {

      boolean visibleTo(int classId, int gender) {
         return (classIds.isEmpty() || classIds.contains(classId)) && (genders.isEmpty() || genders.contains(gender));
      }
   }

   /**
    * 1 món bán: {@code item} = món nhận được mỗi lần mua 1 (số lượng trong đó), {@code price} = giá 1 lần mua,
    * {@code currency} = {@link #CUR_GOLD}..., {@code payItem} = mẫu item trả khi {@link #CUR_ITEM} (còn lại 0),
    * {@code classId} / {@code gender} = 0: ai cũng thấy; khác 0: chỉ class / nhân vật đó thấy món này.
    */
   public record Goods(RewardItem item, int price, int currency, int payItem, int classId, int gender) {

      boolean visibleTo(int playerClass, int playerGender) {
         return (classId == 0 || classId == playerClass) && (gender == 0 || gender == playerGender);
      }

      /** Mẫu item hiện làm icon tiền tệ ở client (item 1 Vàng / 2 Lam thạch / vật phẩm trả). */
      int currencyTemplateId() {
         return switch (currency) {
            case CUR_DIAMOND -> 2;
            case CUR_ITEM -> payItem;
            default -> 1;
         };
      }
   }
}
