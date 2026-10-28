package com.tlnt.server.combo;

import com.tlnt.server.item.RewardItem;
import com.tlnt.server.player.PlayerData;

import java.util.ArrayList;
import java.util.List;

/**
 * 1 danh mục gói combo (1 dòng {@code tlnt_data.combo}) — client chỉ hiện 1 danh mục (tab "Gói combo" của
 * {@code ShopWindow}): server lấy danh mục đang bật có id nhỏ nhất ({@link ComboService}).
 *
 * @param packs các gói theo thứ tự trong JSON (vị trí trong list = số thứ tự lưu "đã mua")
 */
record Combo(int id, String name, List<Pack> packs) {

   /** Mẫu item trả tiền ({@code priceItem} client vẽ icon): 1 vàng, 2 lam thạch, khác = vật phẩm trong túi. */
   static final int PAY_GOLD = 1;
   static final int PAY_DIAMOND = 2;

   /**
    * 1 gói.
    *
    * @param priceItem mẫu item trả tiền (xem {@link #PAY_GOLD} / {@link #PAY_DIAMOND}, khác = vật phẩm)
    * @param limit số lần 1 nhân vật được mua (0 = không giới hạn); mua đủ thì nút "Mua" xám
    * @param classId chỉ class này thấy gói (0 = mọi class) — như món shop "c"
    * @param gender chỉ nhân vật này thấy gói (0 = mọi nhân vật) — như món shop "g"
    */
   record Pack(String name, List<RewardItem> items, int priceItem, int price, int limit, int classId, int gender) {

      boolean visibleTo(PlayerData d) {
         return (classId == 0 || classId == d.classId) && (gender == 0 || gender == d.gender);
      }
   }

   /** Vị trí (trong {@link #packs}) các gói nhân vật này thấy — client gửi lại số thứ tự TRONG danh sách đã nhận. */
   List<Integer> visibleIndexes(PlayerData d) {
      List<Integer> list = new ArrayList<>();
      for (int i = 0; i < packs.size(); i++) {
         if (packs.get(i).visibleTo(d)) {
            list.add(i);
         }
      }
      return list;
   }

   /** Khoá lưu số lần đã mua trong {@link PlayerData#comboBought}: "idDanhMục:vịTríGói". */
   String key(int packIndex) {
      return id + ":" + packIndex;
   }
}
