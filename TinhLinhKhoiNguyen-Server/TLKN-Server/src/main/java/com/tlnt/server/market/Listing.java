package com.tlnt.server.market;

import com.tlnt.server.player.ItemData;

/**
 * 1 món đang kí gửi (1 dòng bảng {@code market_listing} trạng thái {@link #ACTIVE}). Bất biến: {@link #item} là bản
 * riêng của món kí gửi, không ai sửa.
 *
 * @param fee phí đã thu lúc đăng bán (vàng không khoá, không hoàn lại)
 * @param feeIndex thứ tự dòng {@code market_fee} (8h / 16h / 24h)
 */
public record Listing(int id, int sellerId, String sellerName, ItemData item, int price, long fee, int feeIndex,
      long createdAt, long expireAt) {

   public static final byte ACTIVE = 0;
   public static final byte SOLD = 1;
   public static final byte CANCELLED = 2;
   public static final byte EXPIRED = 3;
}
