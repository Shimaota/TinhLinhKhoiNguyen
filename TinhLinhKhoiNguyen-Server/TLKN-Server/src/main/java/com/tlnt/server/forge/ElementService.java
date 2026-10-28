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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Khảm đá nguyên tố (ForgeWindow tab Nguyên tố, op 44 chế độ 2 — {@code ForgeElementPanel}). Luật: {@link ElementRules}.
 * Lane GAME, luồng session.
 *
 * <pre>
 * C→S 44: byte chế độ (2), byte thao tác:
 *   0 "Chọn" món trong túi   int ô túi, int số lượng ({@code sendPvpAction})
 *       trang bị -> ô "Trang bị"; Đá nguyên tố (127-133) -> ô "Nguyên liệu"; Cỏ may mắn (171) -> ô "Cỏ may mắn"
 *   1 "Lấy ra" 1 ô lò         int ô (0 trang bị, 1 cỏ may mắn, 2 nguyên liệu) ({@code sendPvpChallenge})
 *   2 nút "Khảm"              int 1 ({@code sendPvpAccept})
 *   3 "Gỡ khảm" 1 đá          int vị trí đá trên trang bị 0..4 ({@code sendPvpDecline})
 * S→C 44/1 chế độ 2: byte số lỗ đã mở, item trang bị (ĐỦ 5 phần tử ngọc khảm, lỗ trống = -1 — client đọc thẳng
 *          elementStones[0..4]), item cỏ may mắn, item nguyên liệu, chuỗi mô tả
 * S→C 44/2 chế độ 2: byte 1 thành công / 0 thất bại (hiệu ứng ở ô trang bị)
 * </pre>
 * Đá khảm nằm trong {@link ItemData#elementStones} (cột JSON "es"), chỉ số cộng thẳng khi mặc ({@code item/ItemStats}),
 * client hiện ở mục đá nguyên tố của tooltip. Món chỉ được CHỌN (vẫn trong túi); thao tác túi khác huỷ lựa chọn
 * ({@link #cancel}, gọi qua {@link UpgradeService#cancel}).
 */
public final class ElementService {

   /** Lựa chọn đang đặt trong tab Nguyên tố (RAM, không lưu). */
   public static final class State {
      int mapId = -1;
      float x;
      float y;
      ItemData equip;
      int equipSlot = -1;
      int stoneTemplate;
      boolean luck;
   }

   private final ItemService items;

   public ElementService(ItemService items) {
      this.items = items;
   }

   /** Mở tab Nguyên tố ở NPC (map, x, y): client xoá hết ô -> server cũng xoá lựa chọn cũ. */
   public static void open(PlayerData d, int mapId, float x, float y) {
      State st = new State();
      st.mapId = mapId;
      st.x = x;
      st.y = y;
      d.forgeElement = st;
   }

   /** Thao tác túi / vàng khác: huỷ lựa chọn (luồng session của chính người đó). */
   public static void cancel(PlayerSession session, PlayerData d) {
      State st = d.forgeElement;
      if (st == null || (st.equip == null && st.stoneTemplate == 0 && !st.luck)) {
         return;
      }
      st.equip = null;
      st.equipSlot = -1;
      st.stoneTemplate = 0;
      st.luck = false;
      session.send(statePacket(d, st));
   }

   // ================================================================== thao tác

   /** Thao tác 0: chọn 1 món trong túi. */
   public void choose(PlayerSession session, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      State st = d.forgeElement;
      if (UpgradeRules.upgradable(item)) {
         st.equip = item;
         st.equipSlot = bagSlot;
      } else if (ElementRules.isStone(item.templateId)) {
         st.stoneTemplate = item.templateId;
      } else if (item.templateId == SoulRules.LUCK_HERB) {
         st.luck = true;
      } else {
         session.send(Packets.notice("Vật phẩm này không dùng để khảm nguyên tố."));
         return;
      }
      sendState(session, d);
   }

   /** Thao tác 1: lấy 1 ô ra (0 trang bị, 1 cỏ may mắn, 2 nguyên liệu). */
   public void remove(PlayerSession session, int forgeSlot) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = d.forgeElement;
      switch (forgeSlot) {
         case 0 -> {
            st.equip = null;
            st.equipSlot = -1;
         }
         case 1 -> st.luck = false;
         case 2 -> st.stoneTemplate = 0;
         default -> {
            return;
         }
      }
      sendState(session, d);
   }

   /** Thao tác 2: nút "Khảm" — lỗ mới cấp 1 hoặc lỗ cùng loại lên 1 cấp. */
   public void socket(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = d.forgeElement;
      long now = System.currentTimeMillis();
      String error = check(d, st, now);
      if (error != null) {
         session.send(Packets.notice(error));
         sendState(session, d);
         return;
      }
      ItemData equip = st.equip;
      int stone = st.stoneTemplate;
      int index = ElementRules.indexOf(equip, stone);
      int next = index < 0 ? 1 : equip.elementStones.get(index).level() + 1;
      int cost = ElementRules.cost(next);
      boolean luck = st.luck;
      int chance = ElementRules.ratePercent(next, luck);
      boolean success = ThreadLocalRandom.current().nextInt(100) < chance;
      synchronized (d) { // túi: cùng khoá với thư / kí gửi
         if (luck) {
            ItemService.remove(d.bag, SoulRules.LUCK_HERB, 1, now);
         }
         if (success || !luck) { // thất bại có cỏ: chỉ mất cỏ
            ItemService.remove(d.bag, stone, cost, now);
            if (success) {
               ItemData.ElementStone made = ElementRules.make(stone, next);
               if (index < 0) {
                  equip.elementStones.removeIf(java.util.Objects::isNull); // lỗ trống (dữ liệu cũ) dồn lại
                  equip.elementStones.add(made);
               } else {
                  equip.elementStones.set(index, made);
               }
               equip.locked = true; // khảm thành công: trang bị luôn bị khoá
            }
         }
      }
      String stoneName = UpgradeService.name(stone);
      String text = success ? "Khảm thành công! " + stoneName + " đạt cấp " + next + " ("
            + ElementRules.describe(stone, next) + ")."
            : luck ? "Khảm thất bại! Chỉ mất " + UpgradeService.name(SoulRules.LUCK_HERB) + "."
            : "Khảm thất bại! Mất " + cost + " " + stoneName + ".";
      Log.debug("'%s' kham da %d cap %d vao item %d (%s, ti le %d%%)", d.name, stone, next, equip.templateId,
            success ? "thanh cong" : "that bai", chance);
      items.syncBagAndCurrency(session);
      Message result = new Message(Opcode.FORGE);
      result.writeByte(2);
      result.writeByte(ForgePackets.MODE_ELEMENT);
      result.writeByte(success ? 1 : 0);
      session.send(result);
      if (ItemService.count(d.bag, stone, now) <= 0) {
         st.stoneTemplate = 0;
      }
      if (st.luck && ItemService.count(d.bag, SoulRules.LUCK_HERB, now) <= 0) {
         st.luck = false;
      }
      sendState(session, d);
      session.send(Packets.notice(text));
   }

   /**
    * Thao tác 3: "Gỡ khảm" đá thứ {@code index} trên trang bị đang đặt -> nhận lại 50% số đá đã cần tới cấp đó
    * ({@link ElementRules#refund}), đá trả về luôn KHOÁ. Túi không đủ chỗ thì không gỡ.
    */
   public void detach(PlayerSession session, int index) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = d.forgeElement;
      ItemData equip = st.equip;
      if (equip == null || d.bag.get(st.equipSlot) != equip || index < 0 || index >= equip.elementStones.size()
            || equip.elementStones.get(index) == null) {
         sendState(session, d);
         return;
      }
      ItemData.ElementStone stone = equip.elementStones.get(index);
      int refund = ElementRules.refund(stone.level());
      long now = System.currentTimeMillis();
      synchronized (d) {
         if (refund > 0) {
            ItemData back = ItemData.of(stone.templateId(), refund);
            back.locked = true; // đá gỡ ra luôn khoá
            String error = ItemService.give(d, List.of(back), now);
            if (error != null) {
               session.send(Packets.notice(error));
               return;
            }
         }
         equip.elementStones.remove(index);
      }
      items.syncBagAndCurrency(session);
      sendState(session, d);
      session.send(Packets.notice("Đã gỡ " + UpgradeService.name(stone.templateId()) + " cấp " + stone.level()
            + ", nhận lại " + refund + " viên."));
   }

   /** Lý do không khảm được, null = được. */
   private static String check(PlayerData d, State st, long now) {
      ItemData equip = st.equip;
      if (equip == null || st.equipSlot < 0 || d.bag.get(st.equipSlot) != equip) {
         st.equip = null;
         st.equipSlot = -1;
         return "Hãy chọn trang bị cần khảm.";
      }
      int stone = st.stoneTemplate;
      if (!ElementRules.isStone(stone)) {
         return "Hãy chọn Đá nguyên tố cần khảm.";
      }
      int index = ElementRules.indexOf(equip, stone);
      if (index < 0 && ElementRules.used(equip) >= SoulRules.openSockets(equip)) {
         return "Trang bị đã hết lỗ khảm.";
      }
      int next = index < 0 ? 1 : equip.elementStones.get(index).level() + 1;
      if (next > ElementRules.MAX_LEVEL) {
         return UpgradeService.name(stone) + " đã đạt cấp tối đa.";
      }
      if (ItemService.count(d.bag, stone, now) < ElementRules.cost(next)) {
         return "Cần " + ElementRules.cost(next) + " " + UpgradeService.name(stone) + ".";
      }
      if (st.luck && ItemService.count(d.bag, SoulRules.LUCK_HERB, now) <= 0) {
         st.luck = false;
         return "Không còn " + UpgradeService.name(SoulRules.LUCK_HERB) + ".";
      }
      return null;
   }

   // ================================================================== gói S→C

   private static void sendState(PlayerSession session, PlayerData d) {
      session.send(statePacket(d, d.forgeElement));
   }

   /** Gói trạng thái tab Nguyên tố (gửi ngay sau khi mở lò để có mô tả). */
   public static Message statePacket(PlayerData d) {
      State st = d.forgeElement;
      return statePacket(d, st != null ? st : new State());
   }

   private static Message statePacket(PlayerData d, State st) {
      long now = System.currentTimeMillis();
      Message msg = new Message(Opcode.FORGE);
      msg.writeByte(1);
      msg.writeByte(ForgePackets.MODE_ELEMENT);
      msg.writeByte(st.equip != null ? SoulRules.openSockets(st.equip) : 0);
      ItemPackets.writeItem(msg, padded(st.equip));
      ItemPackets.writeItem(msg, st.luck ? ItemData.of(SoulRules.LUCK_HERB, 1) : null);
      ItemData material = null;
      if (st.stoneTemplate != 0) { // số hiện = số đang có, tối đa số cần cho cấp kế
         long have = ItemService.count(d.bag, st.stoneTemplate, now);
         int need = st.equip != null ? ElementRules.cost(nextLevel(st)) : 0;
         material = ItemData.of(st.stoneTemplate, (int) Math.max(1, need > 0 ? Math.min(have, need) : have));
      }
      ItemPackets.writeItem(msg, material);
      msg.writeString(describe(st));
      return msg;
   }

   private static int nextLevel(State st) {
      int index = ElementRules.indexOf(st.equip, st.stoneTemplate);
      return index < 0 ? 1 : st.equip.elementStones.get(index).level() + 1;
   }

   /** Bản sao trang bị có ĐỦ {@link SoulRules#MAX_SOCKETS} phần tử đá (lỗ trống = null) cho {@code ForgeElementPanel}. */
   private static ItemData padded(ItemData equip) {
      if (equip == null) {
         return null;
      }
      ItemData copy = equip.copy();
      copy.elementStones.removeIf(java.util.Objects::isNull);
      while (copy.elementStones.size() < SoulRules.MAX_SOCKETS) {
         copy.elementStones.add(null);
      }
      return copy;
   }

   /**
    * Mô tả (màu nâu), theo spec:
    * <pre>
    * Cấp kế tiếp 1 (+1% Sát thương chí mạng)
    * Cần 100 Đá nguyên tố Kim
    * 100.0%(Nếu thất bại chỉ mất cỏ may mắn)    | không cỏ: 90.0%(Nếu thất bại sẽ mất nguyên liệu)
    * </pre>
    */
   static String describe(State st) {
      StringBuilder sb = new StringBuilder(UpgradeService.BROWN);

      if (st.equip == null || !ElementRules.isStone(st.stoneTemplate)) {
         return sb.append("Hãy đặt trang bị và Đá nguyên tố cần khảm\n").toString();
      }
      int index = ElementRules.indexOf(st.equip, st.stoneTemplate);
      if (index < 0 && ElementRules.used(st.equip) >= SoulRules.openSockets(st.equip)) {
         return sb.append("Trang bị đã hết lỗ khảm").toString();
      }
      int next = nextLevel(st);
      String stoneName = UpgradeService.name(st.stoneTemplate);
      if (next > ElementRules.MAX_LEVEL) {
         return sb.append(stoneName).append(" đã đạt cấp tối đa").toString();
      }
      int chance = ElementRules.ratePercent(next, st.luck);
      sb.append("Cấp kế tiếp ").append(next).append(" (").append(ElementRules.describe(st.stoneTemplate, next))
            .append(")\n")
            .append("Cần ").append(ElementRules.cost(next)).append(' ').append(stoneName).append('\n')
            .append(String.format(Locale.ROOT, "%.1f%%", (double) chance))
            .append(st.luck ? "(Nếu thất bại chỉ mất cỏ may mắn)" : "(Nếu thất bại sẽ mất nguyên liệu)").append('\n');
      return sb.toString();
   }

   // ================================================================== nội bộ

   /** Người chơi thao tác được (đang đứng ở lò đã mở tab Nguyên tố, không giao dịch / câu cá). */
   private static PlayerData player(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      State st = d.forgeElement;
      if (st == null || !near(d, st)) {
         d.forgeElement = null;
         session.send(Packets.notice("Hãy đến gặp thợ rèn để khảm nguyên tố."));
         return null;
      }
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể khảm nguyên tố."));
         return null;
      }
      if (d.fishing != null) {
         session.send(Packets.notice("Đang câu cá, không thể khảm nguyên tố."));
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
