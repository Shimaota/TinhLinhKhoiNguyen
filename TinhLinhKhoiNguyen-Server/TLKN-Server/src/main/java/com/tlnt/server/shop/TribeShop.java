package com.tlnt.server.shop;

import com.tlnt.server.player.ItemData;
import com.tlnt.server.session.PlayerSession;

import java.util.List;

/**
 * Cửa hàng Bộ lạc (tab "Cửa hàng" của cửa sổ Bộ lạc, shop kiểu 2): {@link ShopService} kiểm tra món + số lần như shop
 * thường rồi chuyển việc trả tiền / nhận hàng sang đây (trả bằng ngân sách bộ lạc, hàng vào Rương bộ lạc).
 */
public interface TribeShop {

   /** Shop id ({@code tlnt_data.shop.id}) của Cửa hàng Bộ lạc. */
   int shopId();

   /** Người này có bộ lạc không (không có thì không mở cửa hàng). */
   boolean isMember(int playerId);

   /**
    * @param currency {@link Shop#CUR_GOLD} / {@link Shop#CUR_DIAMOND}...
    * @param cost tổng giá ({@code price × số lần})
    * @param items hàng đã tạo sẵn
    * @param label tên món + số lượng (thông báo / lịch sử)
    */
   void buy(PlayerSession session, int currency, long cost, List<ItemData> items, String label);
}
