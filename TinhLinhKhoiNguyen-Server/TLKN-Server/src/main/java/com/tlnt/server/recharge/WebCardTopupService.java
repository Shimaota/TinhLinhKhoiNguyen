package com.tlnt.server.recharge;

import com.tlnt.server.db.Database;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.ranking.RankingService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn nạp thẻ cào của web PHP (thư mục {@code web/}): web chỉ ghi đơn vào {@code tlnt_server.card_charge} (thẻ + kết quả
 * gạch thẻ, cột {@code diamond} = số lam thạch). Server cộng các đơn đã duyệt LÚC NHÂN VẬT VÀO GAME ({@link #onLogin},
 * móc {@code PlayerService.onLogin}, luồng blocking, trước khi vào khu) — không ai sửa nhân vật đang nằm trong RAM, web
 * chạy được cả khi server game tắt. Đang chơi mà thẻ mới duyệt -> lần vào game sau mới nhận.
 *
 * <p>Chống cộng 2 lần: {@code credited} 0 -> 2 (nhận đơn, {@code WHERE credited = 0}) -> 1 ghi CÙNG transaction với
 * lượt lưu nhân vật ({@link PlayerService#saveWith}). Lỗi ghi / sập server giữa chừng -> đơn kẹt ở 2, không tự cộng lại
 * (cảnh báo lúc khởi động, xem tay).
 */
public final class WebCardTopupService {

   private final Database playerDb;
   private final PlayerService players;

   public WebCardTopupService(Database playerDb, PlayerService players) {
      this.playerDb = playerDb;
      this.players = players;
   }

   /** Lúc khởi động: báo đơn kẹt ở "đang cộng" (server tắt ngang lần trước). */
   public void warnStuck() {
      try (Connection c = playerDb.getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM card_charge WHERE credited = 2");
            ResultSet rs = ps.executeQuery()) {
         if (rs.next() && rs.getInt(1) > 0) {
            Log.warn("Nap the web: %d don card_charge ket o credited=2 (server tat ngang), kiem tra tay", rs.getInt(1));
         }
      } catch (SQLException e) {
         Log.warn("Nap the web: chua co bang card_charge (chay sql/migrations/2026-10-21_web_card_charge.sql): %s",
               e.getMessage());
      }
   }

   /** Móc vào game (luồng blocking, trước khi vào khu): cộng mọi đơn đã duyệt chưa nhận của tài khoản. */
   public void onLogin(PlayerSession session, PlayerData d) {
      if (d.bot) {
         return;
      }
      List<Long> ids = new ArrayList<>();
      long total = 0;
      try (Connection c = playerDb.getConnection()) {
         List<long[]> rows = new ArrayList<>();
         try (PreparedStatement ps = c.prepareStatement("SELECT id, diamond FROM card_charge"
               + " WHERE account_id = ? AND credited = 0 AND status IN (1, 2) AND diamond > 0 ORDER BY id")) {
            ps.setInt(1, d.getAccountId());
            try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                  rows.add(new long[] {rs.getLong(1), rs.getLong(2)});
               }
            }
         }
         try (PreparedStatement claim = c.prepareStatement(
               "UPDATE card_charge SET credited = 2 WHERE id = ? AND credited = 0")) {
            for (long[] row : rows) {
               claim.setLong(1, row[0]);
               if (claim.executeUpdate() == 1) {
                  ids.add(row[0]);
                  total += row[1];
               }
            }
         }
      } catch (SQLException e) {
         Log.error("Nap the web: loi doc don nap tai khoan #" + d.getAccountId(), e);
         return;
      }
      if (ids.isEmpty()) {
         return;
      }
      long amount = total;
      synchronized (d) {
         d.diamond += amount;
         d.rechargeTotal += amount; // mốc Quà nạp: WelfareService.onLogin chạy SAU móc này
         RankingService.recharged(d, amount); // Top tuần Giàu có
      }
      boolean saved = players.saveWith(d, conn -> markCredited(conn, ids)).join();
      if (!saved) {
         // RAM đã cộng, lượt lưu thường sau sẽ ghi lam thạch; đơn giữ 2 -> không bao giờ cộng lại
         Log.warn("Nap the web: khong ghi duoc %d don cua tai khoan #%d (giu credited=2, kiem tra tay)", ids.size(),
               d.getAccountId());
      }
      session.send(Packets.notice("Đã nhận " + amount + " Lam thạch từ " + ids.size() + " đơn nạp thẻ."));
      Log.info("Nap the web: +%d lam thach nhan vat #%d (%d don)", amount, d.getId(), ids.size());
   }

   private static void markCredited(Connection conn, List<Long> ids) throws SQLException {
      try (PreparedStatement ps = conn.prepareStatement(
            "UPDATE card_charge SET credited = 1, credited_at = NOW() WHERE id = ? AND credited = 2")) {
         for (long id : ids) {
            ps.setLong(1, id);
            ps.addBatch();
         }
         ps.executeBatch();
      }
   }
}
