package com.tlnt.server.forge;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Chuyển hóa trang bị (ForgeWindow tab "Chuyển hóa", op 44 chế độ 6 — client {@code ForgeTransmutePanel}, port
 * decompile C3694). Lane GAME, luồng session. Luật: {@link TransmuteRules}.
 *
 * <pre>
 * C→S 44: byte chế độ (6), byte thao tác:
 *   0 "Chọn" món trong túi   int ô túi, int số lượng ({@code sendPvpAction})
 *       Bùa chuyển hóa 331 -> ô bùa; trang bị đã cường hóa -> ô "Trang bị chuyển hóa"; chưa nâng -> ô "Trang bị mới"
 *   1 "Lấy ra" 1 ô lò         int ô (0 trang bị chuyển hóa, 1 trang bị mới, 2 bùa) ({@code sendPvpChallenge})
 *   2 nút "Chuyển hóa"        int 1 ({@code sendPvpAccept})
 * S→C 44/1 chế độ 6: item trang bị chuyển hóa, item trang bị mới, item bùa, chuỗi mô tả
 * S→C 44/2 chế độ 6: byte 1 thành công / 0 thất bại (hiệu ứng ở ô "Trang bị mới")
 * </pre>
 * Như các tab khác: món chỉ được CHỌN (vẫn nằm trong túi), bấm nút mới kiểm tra lại + trừ; thao tác túi khác huỷ lựa
 * chọn ({@link #cancel}, gọi qua {@link UpgradeService#cancel}).
 */
public final class TransmuteService {

   /** Lựa chọn đang đặt trong tab Chuyển hóa (RAM, không lưu). */
   public static final class State {
      int mapId = -1;
      float x;
      float y;
      ItemData from;
      int fromSlot = -1;
      ItemData to;
      int toSlot = -1;
      ItemData charm;
      int charmSlot = -1;
   }

   private final ItemService items;

   public TransmuteService(ItemService items) {
      this.items = items;
   }

   /** Mở tab Chuyển hóa ở NPC (map, x, y): client xoá hết ô -> server cũng xoá lựa chọn cũ. */
   public static void open(PlayerData d, int mapId, float x, float y) {
      State st = new State();
      st.mapId = mapId;
      st.x = x;
      st.y = y;
      d.forgeTransmute = st;
   }

   /** Thao tác túi / vàng khác: huỷ lựa chọn (luồng session của chính người đó). */
   public static void cancel(PlayerSession session, PlayerData d) {
      State st = d.forgeTransmute;
      if (st == null || (st.from == null && st.to == null && st.charm == null)) {
         return;
      }
      clear(st);
      session.send(statePacket(st));
   }

   private static void clear(State st) {
      st.from = null;
      st.fromSlot = -1;
      st.to = null;
      st.toSlot = -1;
      st.charm = null;
      st.charmSlot = -1;
   }

   // ================================================================== thao tác

   /** Thao tác 0: chọn 1 món trong túi. */
   public void choose(PlayerSession session, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      State st = d.forgeTransmute;
      if (item.isExpired(System.currentTimeMillis())) {
         session.send(Packets.notice("Vật phẩm đã hết hạn sử dụng."));
         return;
      }
      if (item.templateId == TransmuteRules.CHARM) {
         st.charm = item;
         st.charmSlot = bagSlot;
      } else if (!TransmuteRules.eligibleKind(item)) {
         session.send(Packets.notice(UpgradeRules.upgradable(item)
               ? "Trang bị Thủ lĩnh và vũ khí Lục linh không thể chuyển hóa." : "Vật phẩm này không thể chuyển hóa."));
         return;
      } else if (TransmuteRules.upgraded(item)) {
         if (TransmuteRules.hasSockets(item)) {
            session.send(Packets.notice("Hãy tháo hết đá nguyên tố và bùa linh hồn khỏi trang bị trước khi chuyển hóa."));
            return;
         }
         st.from = item;
         st.fromSlot = bagSlot;
      } else if (TransmuteRules.pristine(item)) {
         st.to = item;
         st.toSlot = bagSlot;
      } else {
         session.send(Packets.notice(item.starCount > 0
               ? "Trang bị chuyển hóa phải được cường hóa Nhật ấn hoặc Nguyệt ấn."
               : "Trang bị mới phải chưa được nâng cấp, khảm đá hay khảm bùa."));
         return;
      }
      sendState(session, st);
   }

   /** Thao tác 1: lấy 1 ô ra (0 trang bị chuyển hóa, 1 trang bị mới, 2 bùa). */
   public void remove(PlayerSession session, int forgeSlot) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = d.forgeTransmute;
      switch (forgeSlot) {
         case 0 -> {
            st.from = null;
            st.fromSlot = -1;
         }
         case 1 -> {
            st.to = null;
            st.toSlot = -1;
         }
         case 2 -> {
            st.charm = null;
            st.charmSlot = -1;
         }
         default -> {
            return;
         }
      }
      sendState(session, st);
   }

   /** Thao tác 2: nút "Chuyển hóa". */
   public void transmute(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = d.forgeTransmute;
      long now = System.currentTimeMillis();
      String error = check(d, st, now);
      if (error != null) {
         session.send(Packets.notice(error));
         sendState(session, st);
         return;
      }
      ItemData from = st.from;
      ItemData to = st.to;
      boolean success = ThreadLocalRandom.current().nextInt(100) < TransmuteRules.PERCENT;
      int level = from.starCount;
      synchronized (d) { // vàng / túi: cùng khoá với thư / kí gửi
         UpgradeService.payGold(d, TransmuteRules.GOLD);
         ItemData charm = st.charm; // bùa mất cả khi thất bại
         if (charm.quantity <= 1) {
            d.bag.take(st.charmSlot);
         } else {
            charm.quantity--;
         }
         if (success) {
            com.tlnt.server.ranking.RankingService.beforeUpgrade(d, from); // Cường hóa tuần: cấp đầu tuần
            TransmuteRules.transfer(from, to);
            d.bag.take(st.fromSlot); // mất trang bị cũ
         } else {
            TransmuteRules.fail(from);
         }
      }
      String fromName = UpgradeService.name(from.templateId);
      String toName = UpgradeService.name(to.templateId);
      String text = success
            ? "Chuyển hóa thành công! " + toName + " đạt cấp " + to.starCount + " (" + gradeName(to.starGrade)
                  + "), đã dùng 1 " + UpgradeService.name(TransmuteRules.CHARM) + "."
            : "Chuyển hóa thất bại! " + fromName + " vẫn giữ cấp " + from.starCount + ", mất "
                  + UpgradeService.name(TransmuteRules.CHARM) + ".";
      Log.debug("'%s' chuyen hoa item %d cap %d -> item %d (%s)", d.name, from.templateId, level, to.templateId,
            success ? "thanh cong" : "that bai");
      items.syncBagAndCurrency(session);
      Message result = new Message(Opcode.FORGE);
      result.writeByte(2);
      result.writeByte(ForgePackets.MODE_TRANSMUTE);
      result.writeByte(success ? 1 : 0);
      session.send(result);
      if (success) { // trang bị cũ đã mất; trang bị mới giờ đã nâng -> không đặt lại vào ô nào
         st.from = null;
         st.fromSlot = -1;
         st.to = null;
         st.toSlot = -1;
      }
      ItemData left = st.charmSlot >= 0 ? d.bag.get(st.charmSlot) : null;
      if (left == null || left.templateId != TransmuteRules.CHARM) {
         st.charm = null;
         st.charmSlot = -1;
      } else {
         st.charm = left; // cả chồng: còn lại vẫn nằm ô cũ
      }
      sendState(session, st);
      session.send(Packets.notice(text));
   }

   /** Lý do không chuyển hóa được, null = được. */
   private static String check(PlayerData d, State st, long now) {
      if (st.from == null || st.fromSlot < 0 || d.bag.get(st.fromSlot) != st.from) {
         st.from = null;
         st.fromSlot = -1;
         return "Hãy chọn trang bị cần chuyển hóa.";
      }
      if (st.to == null || st.toSlot < 0 || d.bag.get(st.toSlot) != st.to) {
         st.to = null;
         st.toSlot = -1;
         return "Hãy chọn trang bị mới.";
      }
      ItemData charm = st.charm;
      if (charm == null || st.charmSlot < 0 || d.bag.get(st.charmSlot) != charm
            || charm.templateId != TransmuteRules.CHARM) {
         st.charm = null;
         st.charmSlot = -1;
         return "Cần " + UpgradeService.name(TransmuteRules.CHARM) + ".";
      }
      if (charm.isExpired(now) || st.from.isExpired(now) || st.to.isExpired(now)) {
         return "Vật phẩm đã hết hạn sử dụng.";
      }
      String error = TransmuteRules.checkPair(st.from, st.to);
      if (error != null) {
         return error;
      }
      if (d.gold + d.lockedGold < TransmuteRules.GOLD) {
         return "Không đủ vàng (cần " + UpgradeService.money(TransmuteRules.GOLD) + ").";
      }
      return null;
   }

   private static String gradeName(byte grade) {
      return grade == UpgradeRules.GRADE_SUN ? "Nhật ấn" : grade == UpgradeRules.GRADE_MOON ? "Nguyệt ấn" : "";
   }

   // ================================================================== gói S→C

   private static void sendState(PlayerSession session, State st) {
      session.send(statePacket(st));
   }

   /** Gói trạng thái tab Chuyển hóa (gửi ngay sau khi mở lò để có mô tả). */
   public static Message statePacket(PlayerData d) {
      State st = d.forgeTransmute;
      return statePacket(st != null ? st : new State());
   }

   private static Message statePacket(State st) {
      Message msg = new Message(Opcode.FORGE);
      msg.writeByte(1);
      msg.writeByte(ForgePackets.MODE_TRANSMUTE);
      ItemPackets.writeItem(msg, st.from);
      ItemPackets.writeItem(msg, st.to);
      ItemPackets.writeItem(msg, st.charm == null ? null : one(st.charm));
      msg.writeString(describe(st));
      return msg;
   }

   /** Ô bùa hiện 1 cái (dùng 1 / lần). */
   private static ItemData one(ItemData charm) {
      ItemData copy = charm.copy();
      copy.quantity = 1;
      return copy;
   }

   /**
    * Mô tả (màu nâu): phí + tỉ lệ + hậu quả thất bại; đủ 2 trang bị thì thêm kết quả dự kiến hoặc lý do không được.
    */
   static String describe(State st) {
      StringBuilder sb = new StringBuilder(UpgradeService.BROWN);
      sb.append("Phí chuyển hóa: ").append(String.format(java.util.Locale.ROOT, "%,d", TransmuteRules.GOLD)
            .replace(',', '.')).append(" Vàng + 1 ").append(UpgradeService.name(TransmuteRules.CHARM)).append('\n');
      sb.append("Tỉ lệ thành công: ").append(TransmuteRules.PERCENT).append("%\n");
      sb.append("Trang bị mới phải là trang bị chưa cường hóa\n");
      sb.append("Trang bị cũ sẽ biến mất khỏi hành trang khi thành công\n");
      sb.append("Chuyển cấp cường hóa của trang bị cũ sang trang bị mới\n");
      if (st.from != null && st.to != null) {
         String error = TransmuteRules.checkPair(st.from, st.to);
         if (error != null) {
            sb.append("[#FF0000]").append(error);
         } else {
            sb.append(UpgradeService.name(st.to.templateId)).append(" sẽ nhận cấp ").append(st.from.starCount)
                  .append(" (").append(gradeName(st.from.starGrade)).append("), trang bị mới sẽ bị khóa.");
         }
      }
      return sb.toString();
   }

   // ================================================================== nội bộ

   /** Người chơi thao tác được (đang đứng ở lò đã mở tab Chuyển hóa, không giao dịch / câu cá). */
   private static PlayerData player(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      State st = d.forgeTransmute;
      if (st == null || !near(d, st)) {
         d.forgeTransmute = null;
         session.send(Packets.notice("Hãy đến gặp thợ rèn để chuyển hóa trang bị."));
         return null;
      }
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể chuyển hóa."));
         return null;
      }
      if (d.fishing != null) {
         session.send(Packets.notice("Đang câu cá, không thể chuyển hóa."));
         return null;
      }
      return d;
   }

   private static boolean near(PlayerData d, State st) {
      Float x = d.x;
      Float y = d.y;
      return com.tlnt.server.badge.SpiritBadgeService.remote(d, st.mapId) // mở từ Huy hiệu Tinh Linh
            || st.mapId >= 0 && d.mapId == st.mapId && x != null && y != null
            && Math.abs(x - st.x) <= UpgradeService.FORGE_REACH_PX && Math.abs(y - st.y) <= UpgradeService.FORGE_REACH_PX;
   }
}
