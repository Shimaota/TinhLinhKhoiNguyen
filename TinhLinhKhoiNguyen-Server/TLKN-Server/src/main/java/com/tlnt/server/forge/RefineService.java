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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Luyện đá (ForgeWindow tab "Luyện đá", op 44 chế độ 5 — Ông Morgi > Chế tác > Tinh luyện đá): 1 Đá khởi nguyên
 * ({@link #ORIGIN_STONE}) × {@link #STONES_EACH} + {@link #GOLD_EACH} vàng -> 1 Đá Mặt trời hoặc 1 Đá Mặt trăng, tỉ lệ 100%.
 *
 * <pre>
 * C→S 44 chế độ 5: 0 "Chọn" đá khởi nguyên (int ô túi, int số lượng = số đặt vào ô) | 1 "Bỏ ra" (int ô) |
 *                  3 byte loại (0 Tinh luyện đá mặt trời, 1 mặt trăng — nút gạt) | 2 int số lần (1 / 10 / 100)
 * S→C 44/1 chế độ 5: item đá khởi nguyên (số đang có), item kết quả, byte loại, chuỗi mô tả
 * S→C 44/2 chế độ 5: byte 1 thành công
 * </pre>
 * Tinh luyện N lần = N × 10 đá + N × {@link #GOLD_EACH} vàng (vàng khoá trừ trước), ra N viên; đá không đủ N lần thì
 * luyện được bao nhiêu lần luyện bấy nhiêu. Dùng đá KHOÁ trước; viên ra có dùng đá khoá thì KHOÁ.
 */
public final class RefineService {

   public static final int ORIGIN_STONE = 274;
   public static final long GOLD_EACH = 2_000;
   /** Số Đá khởi nguyên cho 1 viên đá mặt trời / mặt trăng. */
   public static final int STONES_EACH = 10;
   public static final int MAX_TIMES = 100;

   /** Lựa chọn tab Luyện đá (RAM). */
   public static final class State {
      /** Số Đá khởi nguyên đã đặt vào ô (số người chơi nhập lúc "Chọn"; vẫn nằm trong túi). */
      int stones;
      /** 0 = đá mặt trời, 1 = đá mặt trăng. */
      int option;
   }

   private static final Locale VI = Locale.forLanguageTag("vi-VN");

   private final ItemService items;

   public RefineService(ItemService items) {
      this.items = items;
   }

   /** Mở tab Luyện đá: client xoá hết ô -> server cũng xoá. */
   public static void open(PlayerData d) {
      d.forgeRefine = new State();
   }

   /** Thao tác 0: đặt {@code quantity} Đá khởi nguyên (số nhập ở hộp số lượng) vào ô. */
   public void choose(PlayerSession session, int bagSlot, int quantity) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      if (item.templateId != ORIGIN_STONE) {
         session.send(Packets.notice("Chỉ tinh luyện được Đá khởi nguyên."));
         return;
      }
      long have = ItemService.count(d.bag, ORIGIN_STONE, System.currentTimeMillis());
      state(d).stones = (int) Math.min(Math.max(1, quantity), have);
      sendState(session, d);
   }

   /** Thao tác 1: bỏ đá ra. */
   public void remove(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      state(d).stones = 0;
      sendState(session, d);
   }

   /** Thao tác 3: đổi loại đá kết quả (0 mặt trời, 1 mặt trăng). */
   public void setOption(PlayerSession session, int option) {
      PlayerData d = player(session);
      if (d == null || (option != 0 && option != 1)) {
         return;
      }
      state(d).option = option;
      sendState(session, d);
   }

   /** Thao tác 2: tinh luyện {@code times} lần (1 / 10 / 100). */
   public void refine(PlayerSession session, int times) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = state(d);
      long now = System.currentTimeMillis();
      long placed = Math.min(st.stones, ItemService.count(d.bag, ORIGIN_STONE, now)); // chỉ dùng số đã đặt vào ô
      int n = (int) Math.min(Math.max(1, Math.min(times, MAX_TIMES)), placed / STONES_EACH);
      String error = placed <= 0 ? "Hãy chọn Đá khởi nguyên."
            : n <= 0 ? "Cần " + STONES_EACH + " Đá khởi nguyên để tinh luyện 1 viên."
            : null;
      long cost = n * GOLD_EACH;
      if (error == null && d.gold + d.lockedGold < cost) {
         error = "Không đủ vàng (cần " + String.format(VI, "%,d", cost) + " Vàng).";
      }
      if (error != null) {
         session.send(Packets.notice(error));
         sendState(session, d);
         return;
      }
      int resultId = st.option == 1 ? UpgradeRules.MOON_STONE : UpgradeRules.SUN_STONE;
      String problem;
      synchronized (d) { // vàng / túi: cùng khoá với thư / kí gửi
         long lockedUsed = Math.min((long) n * STONES_EACH, countLocked(d, now)); // đá khoá bị dùng trước
         int fromLocked = (int) ((lockedUsed + STONES_EACH - 1) / STONES_EACH); // viên có dính đá khoá -> khoá
         List<ItemData> out = new ArrayList<>(2);
         if (fromLocked > 0) {
            ItemData a = ItemData.of(resultId, fromLocked);
            a.locked = true;
            a.createdTime = now;
            out.add(a);
         }
         if (n - fromLocked > 0) {
            ItemData b = ItemData.of(resultId, n - fromLocked);
            b.createdTime = now;
            out.add(b);
         }
         com.tlnt.server.item.ItemContainer bag = d.bag.deepCopy();
         ItemService.remove(bag, ORIGIN_STONE, (long) n * STONES_EACH, now); // đá khoá trước
         problem = ItemService.give(d, bag, out, now); // trên bản sao: thiếu chỗ thì không đổi gì
         if (problem == null) {
            long fromLockedGold = Math.min(d.lockedGold, cost);
            d.lockedGold -= fromLockedGold;
            d.gold -= cost - fromLockedGold;
         }
      }
      if (problem != null) {
         session.send(Packets.notice(problem));
         return;
      }
      Log.debug("'%s' tinh luyen %d da -> item %d", d.name, n, resultId);
      items.syncBagAndCurrency(session);
      Message result = new Message(Opcode.FORGE);
      result.writeByte(2);
      result.writeByte(ForgePackets.MODE_REFINE);
      result.writeByte(1);
      session.send(result);
      st.stones = (int) Math.min(placed - (long) n * STONES_EACH, ItemService.count(d.bag, ORIGIN_STONE, now)); // còn lại
      sendState(session, d);
      com.tlnt.server.achievement.AchievementEvents.add(session, com.tlnt.server.achievement.AchievementType.REFINE_STONE, (long) n * STONES_EACH); // thành tựu: số đá đã luyện
      com.tlnt.server.daily.DailyEvents.add(session, com.tlnt.server.daily.DailyKind.REFINE, 0, (long) n * STONES_EACH);
      session.send(Packets.notice("Tinh luyện thành công " + n + " " + (st.option == 1 ? "Đá Mặt trăng" : "Đá Mặt trời")
            + " (" + String.format(VI, "%,d", cost) + " Vàng)."));
   }

   /** Op 44/1 chế độ 5. */
   void sendState(PlayerSession session, PlayerData d) {
      session.send(statePacket(d));
   }

   /** Gói trạng thái tab (gửi ngay sau khi mở lò để có mô tả + nút gạt đúng). */
   public static Message statePacket(PlayerData d) {
      State st = state(d);
      long have = Math.min(st.stones, ItemService.count(d.bag, ORIGIN_STONE, System.currentTimeMillis()));
      Message msg = new Message(Opcode.FORGE);
      msg.writeByte(1);
      msg.writeByte(ForgePackets.MODE_REFINE);
      ItemPackets.writeItem(msg, have > 0 ? ItemData.of(ORIGIN_STONE, (int) Math.min(Integer.MAX_VALUE, have)) : null);
      ItemPackets.writeItem(msg, ItemData.of(st.option == 1 ? UpgradeRules.MOON_STONE : UpgradeRules.SUN_STONE, 1));
      msg.writeByte(st.option);
      msg.writeString(describe());
      return msg;
   }

   /** Mô tả (màu nâu như tab Nâng cấp). */
   static String describe() {
      return UpgradeService.BROWN + "Chi phí tinh luyện: " + String.format(VI, "%,d", GOLD_EACH) + " Vàng\n"
            + "Cần 10 Đá khởi nguyên để chế tạo thành 1 Đá Mặt trời hoặc 1 Đá Mặt trăng\n"
            + "Tỉ lệ thành công: 100%\n";
   }

   private static long countLocked(PlayerData d, long now) {
      long n = 0;
      for (int slot = 0; slot < d.bag.size(); slot++) {
         ItemData item = d.bag.get(slot);
         if (item != null && item.templateId == ORIGIN_STONE && item.locked && !item.isExpired(now)) {
            n += item.quantity;
         }
      }
      return n;
   }

   private static State state(PlayerData d) {
      State st = d.forgeRefine;
      if (st == null) {
         st = new State();
         d.forgeRefine = st;
      }
      return st;
   }

   private static PlayerData player(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể tinh luyện."));
         return null;
      }
      if (d.fishing != null) {
         session.send(Packets.notice("Đang câu cá, không thể tinh luyện."));
         return null;
      }
      return d;
   }
}
