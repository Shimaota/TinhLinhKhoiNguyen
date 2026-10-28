package com.tlnt.server.market;

import com.tlnt.server.db.Database;
import com.tlnt.server.item.ItemCodec;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng {@code market_listing} (DB nhân vật). Đọc lúc khởi động; mọi lệnh GHI là hàm tĩnh chạy trong transaction của
 * db-writer, cùng transaction với lượt lưu người bán / người mua và thư trả đồ / trả vàng.
 */
public final class MarketRepository {

   private final Database db;

   public MarketRepository(Database db) {
      this.db = db;
   }

   public int maxId() throws SQLException {
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(id), 0) FROM market_listing");
            ResultSet rs = ps.executeQuery()) {
         rs.next();
         return rs.getInt(1);
      }
   }

   /** Mọi món đang bán (kể cả đã quá hạn lúc server tắt — lượt dọn đầu tiên trả về thư). Cũ trước. */
   public List<Listing> loadActive() throws SQLException {
      List<Listing> listings = new ArrayList<>();
      try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT id, seller_id, seller_name, item, price, fee,"
                  + " fee_index, created_at, expire_at FROM market_listing WHERE status = ? ORDER BY id")) {
         ps.setByte(1, Listing.ACTIVE);
         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               ItemData item = ItemCodec.itemFromJson(rs.getString("item"));
               if (item == null) {
                  Log.warn("Bo qua ki gui #%d: JSON vat pham hong", rs.getInt("id"));
                  continue;
               }
               listings.add(new Listing(rs.getInt("id"), rs.getInt("seller_id"), rs.getString("seller_name"),
                     item.normalize(), rs.getInt("price"), rs.getLong("fee"), rs.getInt("fee_index"),
                     rs.getLong("created_at"), rs.getLong("expire_at")));
            }
         }
      }
      return listings;
   }

   public static void insert(Connection conn, Listing l) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement("INSERT INTO market_listing (id, seller_id, seller_name, item,"
            + " price, fee, fee_index, created_at, expire_at, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
         ps.setInt(1, l.id());
         ps.setInt(2, l.sellerId());
         ps.setString(3, l.sellerName());
         ps.setString(4, ItemCodec.itemToJson(l.item()));
         ps.setInt(5, l.price());
         ps.setLong(6, l.fee());
         ps.setInt(7, l.feeIndex());
         ps.setLong(8, l.createdAt());
         ps.setLong(9, l.expireAt());
         ps.setByte(10, Listing.ACTIVE);
         ps.executeUpdate();
      }
   }

   /** Đóng 1 món đang bán: {@code status} = đã bán / gỡ xuống / hết hạn; {@code buyerId} 0 nếu không có. */
   public static void close(Connection conn, int listingId, byte status, int buyerId, long now) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement("UPDATE market_listing SET status = ?, buyer_id = ?,"
            + " closed_at = ? WHERE id = ? AND status = ?")) {
         ps.setByte(1, status);
         ps.setInt(2, buyerId);
         ps.setLong(3, now);
         ps.setInt(4, listingId);
         ps.setByte(5, Listing.ACTIVE);
         if (ps.executeUpdate() != 1) {
            // RAM (bản chuẩn) và DB lệch — chỉ xảy ra khi lượt ghi đăng bán trước đó lỗi. Vẫn commit phần còn lại (thư
            // trả đồ / trả vàng) để khớp với RAM, không rollback làm người mua mất vàng.
            Log.warn("Ki gui #%d khong con o trang thai dang ban trong DB", listingId);
         }
      }
   }
}
