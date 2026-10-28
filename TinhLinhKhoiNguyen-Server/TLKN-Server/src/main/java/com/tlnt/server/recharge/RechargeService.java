package com.tlnt.server.recharge;

import com.google.gson.JsonObject;
import com.tlnt.server.db.Database;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cửa sổ "Nạp tiền" (client mới {@code RechargeWindow}, decompile C0061 / C3757 — op 72). Server KHÔNG xử lý thanh
 * toán: chỉ gửi danh sách gói + link trang nạp (cổng thanh toán của chủ server) để client mở trình duyệt. Cộng lam thạch
 * sau khi thanh toán xong là việc của trang nạp ({@code WelfareService.addRecharge} — chưa làm).
 *
 * <pre>
 * S→C 72/0: int n, n × (int id, str tên, str icon, int giá VNĐ, str mô tả "Nhận ngay ...") — gửi lúc vào game
 * C→S 72/0: int id gói (bấm 1 gói)
 * S→C 72/1: str link -> client {@code Gdx.net.openURI}
 * </pre>
 * Cấu hình = 1 dòng bảng {@code tlnt_data.recharge} (id 1): {@code url} mẫu link với {account} {accountId} {player}
 * {playerId} {pack} {price} (giá trị đã mã hoá URL), {@code packs} JSON
 * {@code [{"id":1,"name":"Gói 20K","icon":"icon/1","price":20000,"desc":"200 Lam thạch"}, ...]}; url trống = chưa mở nạp.
 * Nạp lại mỗi phút ({@link #reload}, luồng blocking).
 */
public final class RechargeService {

   /** 1 gói nạp. */
   public record Pack(int id, String name, String icon, int price, String desc) {
   }

   record Config(String url, List<Pack> packs) {
   }

   private final Database dataDb;
   private volatile Config config = new Config("", List.of());

   public RechargeService(Database dataDb) {
      this.dataDb = dataDb;
   }

   /** Đọc lại bảng (lỗi thì giữ bản cũ). Luồng blocking. */
   public void reload() {
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT url, packs FROM recharge WHERE id = 1");
            ResultSet rs = ps.executeQuery()) {
         if (!rs.next()) {
            config = new Config("", List.of());
            return;
         }
         config = new Config(rs.getString(1) == null ? "" : rs.getString(1).trim(), parse(rs.getString(2)));
      } catch (SQLException e) {
         Log.warn("Khong doc duoc bang recharge (chay sql/migrations?): %s", e.getMessage());
      }
   }

   static List<Pack> parse(String json) {
      List<Pack> packs = new ArrayList<>();
      for (JsonObject o : RewardItem.objects(json, "recharge")) {
         if (!o.has("id")) {
            continue;
         }
         packs.add(new Pack(o.get("id").getAsInt(), str(o, "name"), str(o, "icon"),
               o.has("price") ? o.get("price").getAsInt() : 0, str(o, "desc")));
      }
      return List.copyOf(packs);
   }

   private static String str(JsonObject o, String key) {
      return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
   }

   /** Lúc vào game: danh sách gói (client chỉ lưu, mở cửa sổ khi bấm lam thạch ở thanh tiền). */
   public void onLogin(PlayerSession session, PlayerData d) {
      session.send(listPacket(config.packs()));
   }

   static Message listPacket(List<Pack> packs) {
      Message msg = new Message(Opcode.RECHARGE);
      msg.writeByte(0);
      msg.writeInt(packs.size());
      for (Pack p : packs) {
         msg.writeInt(p.id());
         msg.writeString(p.name());
         msg.writeString(p.icon());
         msg.writeInt(p.price());
         msg.writeString(p.desc());
      }
      return msg;
   }

   /** C→S 72/0: bấm 1 gói -> gửi link trang nạp. */
   public void choose(PlayerSession session, int packId) {
      PlayerData d = session.getPlayer();
      Config c = config;
      Pack pack = null;
      for (Pack p : c.packs()) {
         if (p.id() == packId) {
            pack = p;
         }
      }
      if (d == null || pack == null) {
         return;
      }
      if (c.url().isEmpty()) {
         session.send(Packets.notice("Chức năng nạp chưa mở."));
         return;
      }
      String account = session.getAccount() == null ? "" : session.getAccount().getUsername();
      String url = link(c.url(), account, d.getAccountId(), d.name, d.getId(), pack);
      Message msg = new Message(Opcode.RECHARGE);
      msg.writeByte(1);
      msg.writeString(url);
      session.send(msg);
      Log.debug("'%s' mo link nap goi %d", d.name, packId);
   }

   /** Thay {account} {accountId} {player} {playerId} {pack} {price} trong mẫu (giá trị mã hoá URL). */
   static String link(String template, String account, int accountId, String player, int playerId, Pack pack) {
      return template.replace("{accountId}", String.valueOf(accountId))
            .replace("{account}", enc(account))
            .replace("{playerId}", String.valueOf(playerId))
            .replace("{player}", enc(player))
            .replace("{pack}", String.valueOf(pack.id()))
            .replace("{price}", String.valueOf(pack.price()));
   }

   private static String enc(String s) {
      return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
   }
}
