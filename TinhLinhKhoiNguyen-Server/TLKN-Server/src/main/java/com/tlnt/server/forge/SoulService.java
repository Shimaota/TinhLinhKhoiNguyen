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
 * Khảm bùa linh hồn (ForgeWindow tab "Linh hồn", op 44 chế độ 1 — {@code ForgeSoulPanel}). Lane GAME, luồng session.
 *
 * <pre>
 * C→S 44: byte chế độ (1), byte thao tác:
 *   0 "Chọn" món trong túi   int ô túi, int số lượng ({@code sendPvpAction})
 *       trang bị -> ô "Trang bị"; bùa linh hồn (loại 14) -> ô "Nguyên liệu"; Cỏ may mắn (171) -> ô "Cỏ may mắn"
 *   1 "Lấy ra" 1 ô lò         int ô (0 trang bị, 1 cỏ may mắn, 2 nguyên liệu) ({@code sendPvpChallenge})
 *   2 nút "Khảm"              int 1 ({@code sendPvpAccept})
 *   3 "Gỡ khảm" 1 bùa         int vị trí bùa trên trang bị 0..4 ({@code sendPvpDecline})
 * S→C 44/1 chế độ 1: byte số lỗ đã mở, item trang bị (ĐỦ 5 bùa, lỗ trống = -1 — client đọc thẳng soulCharms[0..4]),
 *          item cỏ may mắn, item nguyên liệu, chuỗi mô tả
 * S→C 44/2 chế độ 1: byte 1 thành công / 0 thất bại (hiệu ứng ở ô trang bị)
 * </pre>
 * Luật: {@link SoulRules}. Bùa khảm vào nằm trong {@link ItemData#soulCharms} (lưu cùng item, cột JSON "sc"); chỉ số
 * của bùa cộng thẳng vào nhân vật khi mặc đồ ({@code item/ItemStats}), client hiện ở mục "Bùa linh hồn" của tooltip.
 * Giống tab Nâng cấp: món chỉ được CHỌN (vẫn trong túi), bấm Khảm mới kiểm tra lại + trừ; thao tác túi khác huỷ lựa
 * chọn ({@link #cancel}, gọi qua {@link UpgradeService#cancel}).
 */
public final class SoulService {

   /** Lựa chọn đang đặt trong tab Linh hồn (RAM, không lưu). */
   public static final class State {
      int mapId = -1;
      float x;
      float y;
      ItemData equip;
      int equipSlot = -1;
      ItemData charm;
      int charmSlot = -1;
      boolean luck;
   }

   private final ItemService items;

   public SoulService(ItemService items) {
      this.items = items;
   }

   /** Mở tab Linh hồn ở NPC (map, x, y): client xoá hết ô -> server cũng xoá lựa chọn cũ. */
   public static void open(PlayerData d, int mapId, float x, float y) {
      State st = new State();
      st.mapId = mapId;
      st.x = x;
      st.y = y;
      d.forgeSoul = st;
   }

   /** Thao tác túi / vàng khác: huỷ lựa chọn (luồng session của chính người đó). */
   public static void cancel(PlayerSession session, PlayerData d) {
      State st = d.forgeSoul;
      if (st == null || (st.equip == null && st.charm == null && !st.luck)) {
         return;
      }
      clear(st);
      session.send(statePacket(st));
   }

   private static void clear(State st) {
      st.equip = null;
      st.equipSlot = -1;
      st.charm = null;
      st.charmSlot = -1;
      st.luck = false;
   }

   // ================================================================== thao tác

   /** Thao tác 0: chọn 1 món trong túi. */
   public void choose(PlayerSession session, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      State st = d.forgeSoul;
      if (UpgradeRules.upgradable(item)) {
         st.equip = item;
         st.equipSlot = bagSlot;
      } else if (SoulRules.isCharm(item)) {
         if (item.isExpired(System.currentTimeMillis())) {
            session.send(Packets.notice("Vật phẩm đã hết hạn sử dụng."));
            return;
         }
         st.charm = item;
         st.charmSlot = bagSlot;
      } else if (item.templateId == SoulRules.LUCK_HERB) {
         st.luck = true;
      } else {
         session.send(Packets.notice("Vật phẩm này không dùng để khảm."));
         return;
      }
      sendState(session, st);
   }

   /** Thao tác 1: lấy 1 ô ra (0 trang bị, 1 cỏ may mắn, 2 nguyên liệu). */
   public void remove(PlayerSession session, int forgeSlot) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = d.forgeSoul;
      switch (forgeSlot) {
         case 0 -> {
            st.equip = null;
            st.equipSlot = -1;
         }
         case 1 -> st.luck = false;
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

   /** Thao tác 2: nút "Khảm". */
   public void socket(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = d.forgeSoul;
      long now = System.currentTimeMillis();
      String error = check(d, st, now);
      if (error != null) {
         session.send(Packets.notice(error));
         sendState(session, st);
         return;
      }
      ItemData equip = st.equip;
      ItemData charm = st.charm;
      boolean luck = st.luck;
      int chance = SoulRules.chancePercent(equip, charm.templateId, luck);
      boolean success = ThreadLocalRandom.current().nextInt(100) < chance;
      synchronized (d) { // vàng / túi: cùng khoá với thư / kí gửi
         UpgradeService.payGold(d, SoulRules.GOLD);
         if (luck) {
            ItemService.remove(d.bag, SoulRules.LUCK_HERB, 1, now);
         }
         if (success || !luck) { // thành công: bùa vào trang bị; thất bại không có cỏ: mất bùa
            ItemData one = takeOne(d, st.charmSlot, charm);
            if (success) {
               equip.soulCharms.add(one);
               if (one.locked) {
                  equip.locked = true; // bùa khoá -> trang bị khoá (không đem bùa khoá đi bán qua trang bị)
               }
            }
         }
      }
      String name = UpgradeService.name(equip.templateId);
      String text;
      if (success) {
         text = "Khảm thành công " + UpgradeService.name(charm.templateId) + " vào " + name + ".";
      } else if (luck) {
         text = "Khảm thất bại! Chỉ mất " + UpgradeService.name(SoulRules.LUCK_HERB) + ".";
      } else {
         text = "Khảm thất bại! Mất nguyên liệu.";
      }
      Log.debug("'%s' kham bua %d vao item %d (%s, ti le %d%%)", d.name, charm.templateId, equip.templateId,
            success ? "thanh cong" : "that bai", chance);
      items.syncBagAndCurrency(session);
      Message result = new Message(Opcode.FORGE);
      result.writeByte(2);
      result.writeByte(ForgePackets.MODE_SOUL);
      result.writeByte(success ? 1 : 0);
      session.send(result);
      // bùa đã dùng (hoặc hết) -> ô nguyên liệu trống; cỏ may mắn giữ nếu còn
      if (st.charmSlot < 0 || d.bag.get(st.charmSlot) == null || !SoulRules.isCharm(d.bag.get(st.charmSlot))) {
         st.charm = null;
         st.charmSlot = -1;
      } else {
         st.charm = d.bag.get(st.charmSlot); // cả chồng: còn lại vẫn nằm ô cũ
      }
      if (st.luck && ItemService.count(d.bag, SoulRules.LUCK_HERB, now) <= 0) {
         st.luck = false;
      }
      sendState(session, st);
      session.send(Packets.notice(text));
   }

   /** Thao tác 3: "Gỡ khảm" bùa thứ {@code index} trên trang bị đang đặt -> bùa về hành trang (cần 1 ô trống). */
   public void detach(PlayerSession session, int index) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = d.forgeSoul;
      ItemData equip = st.equip;
      if (equip == null || d.bag.get(st.equipSlot) != equip || index < 0 || index >= equip.soulCharms.size()) {
         sendState(session, st);
         return;
      }
      synchronized (d) {
         if (d.bag.firstEmpty() < 0) {
            session.send(Packets.notice("Cần ít nhất 1 ô trong hành trang"));
            return;
         }
         ItemData charm = equip.soulCharms.remove(index);
         d.bag.add(charm);
      }
      items.syncBagAndCurrency(session);
      sendState(session, st);
      session.send(Packets.notice("Đã gỡ bùa linh hồn khỏi " + UpgradeService.name(equip.templateId) + "."));
   }

   /** Lấy 1 bùa ở ô túi: cả chồng 1 món thì lấy nguyên món, nhiều hơn thì tách bản sao (số lượng 1). */
   private static ItemData takeOne(PlayerData d, int slot, ItemData charm) {
      if (charm.quantity <= 1) {
         d.bag.take(slot);
         return charm;
      }
      ItemData one = charm.copy();
      one.quantity = 1;
      charm.quantity--;
      return one;
   }

   /** Lý do không khảm được, null = được. */
   private static String check(PlayerData d, State st, long now) {
      ItemData equip = st.equip;
      if (equip == null || st.equipSlot < 0 || d.bag.get(st.equipSlot) != equip) {
         st.equip = null;
         st.equipSlot = -1;
         return "Hãy chọn trang bị cần khảm.";
      }
      if (equip.soulCharms.size() >= SoulRules.openSockets(equip)) {
         return "Trang bị đã hết lỗ khảm.";
      }
      ItemData charm = st.charm;
      if (charm == null || st.charmSlot < 0 || d.bag.get(st.charmSlot) != charm || !SoulRules.isCharm(charm)) {
         st.charm = null;
         st.charmSlot = -1;
         return "Hãy chọn bùa linh hồn cần khảm.";
      }
      if (charm.isExpired(now)) {
         return "Bùa linh hồn đã hết hạn sử dụng.";
      }
      if (st.luck && ItemService.count(d.bag, SoulRules.LUCK_HERB, now) <= 0) {
         st.luck = false;
         return "Không còn " + UpgradeService.name(SoulRules.LUCK_HERB) + ".";
      }
      if (d.gold + d.lockedGold < SoulRules.GOLD) {
         return "Không đủ vàng (cần " + UpgradeService.money(SoulRules.GOLD) + ").";
      }
      return null;
   }

   // ================================================================== gói S→C

   private static void sendState(PlayerSession session, State st) {
      session.send(statePacket(st));
   }

   /** Gói trạng thái tab Linh hồn (gửi ngay sau khi mở lò để có mô tả). */
   public static Message statePacket(PlayerData d) {
      State st = d.forgeSoul;
      return statePacket(st != null ? st : new State());
   }

   private static Message statePacket(State st) {
      Message msg = new Message(Opcode.FORGE);
      msg.writeByte(1);
      msg.writeByte(ForgePackets.MODE_SOUL);
      msg.writeByte(st.equip != null ? SoulRules.openSockets(st.equip) : 0);
      ItemPackets.writeItem(msg, padded(st.equip));
      ItemPackets.writeItem(msg, st.luck ? ItemData.of(SoulRules.LUCK_HERB, 1) : null);
      ItemPackets.writeItem(msg, st.charm);
      msg.writeString(describe(st));
      return msg;
   }

   /** Bản sao trang bị có ĐỦ {@link SoulRules#MAX_SOCKETS} phần tử bùa (lỗ trống = null) cho {@code ForgeSoulPanel}. */
   private static ItemData padded(ItemData equip) {
      if (equip == null) {
         return null;
      }
      ItemData copy = equip.copy();
      while (copy.soulCharms.size() < SoulRules.MAX_SOCKETS) {
         copy.soulCharms.add(null);
      }
      return copy;
   }

   /**
    * Mô tả (màu nâu):
    * <pre>
    * Chi phí: 100.0k
    * Tỉ lệ thành công: 50% (Nếu thất bại sẽ mất nguyên liệu)
    * </pre>
    * Trang bị đặt vào đã hết lỗ khảm -> CHỈ 1 dòng "Trang bị đã hết lỗ khảm" (không hiện chi phí / tỉ lệ).
    */
   static String describe(State st) {
      StringBuilder sb = new StringBuilder(UpgradeService.BROWN);
      if (st.equip != null && st.equip.soulCharms.size() >= SoulRules.openSockets(st.equip)) {
         return sb.append("Trang bị đã hết lỗ khảm").toString();
      }
      sb.append("Chi phí: ").append(SoulRules.shortGold(SoulRules.GOLD)).append('\n');
      int chance = st.equip != null && st.charm != null
            ? SoulRules.chancePercent(st.equip, st.charm.templateId, st.luck)
            : SoulRules.BASE_PERCENT + (st.luck ? SoulRules.LUCK_PERCENT : 0);
      sb.append("Tỉ lệ thành công: ").append(chance).append("% (")
            .append(st.luck ? "Nếu thất bại chỉ mất " + UpgradeService.name(SoulRules.LUCK_HERB)
                  : "Nếu thất bại sẽ mất nguyên liệu")
            .append(")\n");
      return sb.toString();
   }

   // ================================================================== nội bộ

   /** Người chơi thao tác được (đang đứng ở lò đã mở tab Linh hồn, không giao dịch / câu cá). */
   private static PlayerData player(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      State st = d.forgeSoul;
      if (st == null || !near(d, st)) {
         d.forgeSoul = null;
         session.send(Packets.notice("Hãy đến gặp thợ rèn để khảm bùa."));
         return null;
      }
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể khảm bùa."));
         return null;
      }
      if (d.fishing != null) {
         session.send(Packets.notice("Đang câu cá, không thể khảm bùa."));
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
