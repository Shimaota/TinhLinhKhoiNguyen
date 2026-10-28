package com.tlnt.server.forge;

import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rèn linh thạch (ForgeWindow tab "Chế tác", op 44 chế độ 4 — Ông Morgi > "Chế tác" > "Rèn linh thạch"): ghép
 * {@link #PIECES_NEEDED} Mảnh vũ khí (loại 33, item 177-186) + {@link #STONES_NEEDED} Đá linh thạch ({@link #STONE}) +
 * {@link #GOLD_COST} vàng -> vũ khí Bất hoại / Lục linh tương ứng (KHOÁ, chỉ số random trong khoảng {@link #RECIPES}).
 * Tỉ lệ {@link #BASE_RATE}%, đặt 1 Cỏ may mắn ({@link #CLOVER}) -> {@link #CLOVER_RATE}%. Thất bại = mất mảnh + đá + cỏ
 * (vàng vẫn mất).
 *
 * <pre>
 * C→S 44 chế độ 4: 0 "Chọn" (int ô túi, int số lượng) — mảnh -> ô "Phôi trang bị", Đá linh thạch / Cỏ may mắn -> ô riêng
 *                  | 1 "Bỏ ra" (int ô: 0 đá linh thạch, 1 cỏ may mắn, 2 phôi) | 2 nút "Ghép" (int số lần — client luôn 1)
 * S→C 44/1 chế độ 4: item đá linh thạch, item cỏ may mắn, item phôi, item thành phẩm (xem trước dạng khoảng), chuỗi
 * S→C 44/2 chế độ 4: byte 1 thành công / 0 thất bại (hiệu ứng ở ô phôi)
 * </pre>
 * Món chỉ được CHỌN (vẫn nằm trong túi); lúc bấm Ghép kiểm tra lại đủ rồi mới trừ, trên bản sao túi. Vàng khoá dùng
 * trước. Đứng xa Morgi thì không rèn.
 */
public final class CraftService {

   public static final int STONE = 255;
   public static final int CLOVER = 171;
   public static final int PIECES_NEEDED = 1000;
   public static final int STONES_NEEDED = 10;
   public static final long GOLD_COST = 100_000_000L;
   public static final int BASE_RATE = 25;
   public static final int CLOVER_RATE = 50;

   /** Dòng chỉ số (stat_type). */
   private static final int HP = 3;
   private static final int MP = 5;
   private static final int ATTACK = 7;
   private static final int CRIT = 11;
   private static final int CRIT_DAMAGE_PERCENT = 14;
   private static final int ARMOR = 15;
   private static final int ARMOR_PEN = 17;
   private static final int EVASION = 19;
   private static final int ACCURACY = 21;
   private static final int SPEED_PERCENT = 24;
   private static final int AMP_PERCENT = 44;

   /** Mảnh -> vũ khí (khoá, chỉ số khoảng [min, max]). */
   static final Map<Integer, RewardItem> RECIPES = Map.of(
         177, weapon(101, ATTACK, 255, 345, HP, 1600, 2200, ARMOR, 215, 285), // Rìu Bất hoại
         179, weapon(106, ATTACK, 255, 304, CRIT, 680, 920, CRIT_DAMAGE_PERCENT, 5, 6), // Giáo Bất hoại
         181, weapon(111, ATTACK, 283, 382, EVASION, 680, 920, SPEED_PERCENT, 10, 13), // Roi da Bất hoại
         183, weapon(116, ATTACK, 225, 304, ACCURACY, 680, 920, ARMOR_PEN, 215, 285), // Cung tên Bất hoại
         185, weapon(121, ATTACK, 200, 270, MP, 1600, 2200, AMP_PERCENT, 5, 6), // Ná thun Bất hoại
         178, weapon(102, ATTACK, 380, 515, HP, 2500, 3400, ARMOR, 310, 410), // Rìu Lục linh
         180, weapon(107, ATTACK, 335, 452, CRIT, 1000, 1360, CRIT_DAMAGE_PERCENT, 7, 8), // Giáo Lục linh
         182, weapon(112, ATTACK, 420, 567, EVASION, 1000, 1360, SPEED_PERCENT, 13, 16), // Roi da Lục linh
         184, weapon(117, ATTACK, 335, 452, ACCURACY, 1000, 1360, ARMOR_PEN, 310, 410), // Cung tên Lục linh
         186, weapon(122, ATTACK, 300, 405, MP, 2500, 3400, AMP_PERCENT, 7, 8)); // Ná thun Lục linh

   /** Lựa chọn tab Chế tác (RAM). */
   public static final class State {
      int mapId = -1;
      float x;
      float y;
      /** Mẫu mảnh đã đặt (0 = trống). */
      int piece;
      boolean stone;
      boolean clover;
   }

   private final ItemService items;

   public CraftService(ItemService items) {
      this.items = items;
   }

   /** 3 dòng {stat, min, max}. */
   private static RewardItem weapon(int templateId, int... lines) {
      int n = lines.length / 3;
      int[][] min = new int[n][];
      int[][] max = new int[n][];
      for (int i = 0; i < n; i++) {
         min[i] = new int[] {lines[i * 3], lines[i * 3 + 1]};
         max[i] = new int[] {lines[i * 3], lines[i * 3 + 2]};
      }
      return new RewardItem(templateId, 1, true, min, max, 0);
   }

   /** Mở tab ở NPC (map, x, y): client xoá hết ô -> server cũng xoá. */
   public static void open(PlayerData d, int mapId, float x, float y) {
      State st = new State();
      st.mapId = mapId;
      st.x = x;
      st.y = y;
      d.forgeCraft = st;
   }

   static int rate(boolean clover) {
      return clover ? CLOVER_RATE : BASE_RATE;
   }

   /** Thao tác 0: đặt mảnh / đá linh thạch / cỏ may mắn vào lò (số lượng nhập bỏ qua: dùng đúng số cần). */
   public void choose(PlayerSession session, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      State st = state(d);
      if (RECIPES.containsKey(item.templateId)) {
         st.piece = item.templateId;
      } else if (item.templateId == STONE) {
         st.stone = true;
      } else if (item.templateId == CLOVER) {
         st.clover = true;
      } else {
         session.send(Packets.notice("Chỉ dùng được Mảnh vũ khí, Đá linh thạch và Cỏ may mắn."));
         return;
      }
      sendState(session, d);
   }

   /** Thao tác 1: bỏ ô {@code slot} ra (0 đá linh thạch, 1 cỏ may mắn, 2 phôi). */
   public void remove(PlayerSession session, int slot) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = state(d);
      switch (slot) {
         case 0 -> st.stone = false;
         case 1 -> st.clover = false;
         case 2 -> st.piece = 0;
         default -> {
         }
      }
      sendState(session, d);
   }

   /** Thao tác 2: ghép 1 lần. */
   public void craft(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = state(d);
      long now = System.currentTimeMillis();
      long pieces = st.piece == 0 ? 0 : ItemService.count(d.bag, st.piece, now);
      long stones = ItemService.count(d.bag, STONE, now);
      boolean clover = st.clover && ItemService.count(d.bag, CLOVER, now) > 0;
      String error = st.piece == 0 ? "Hãy chọn Mảnh vũ khí."
            : pieces < PIECES_NEEDED ? "Cần " + PIECES_NEEDED + " phôi trang bị (đang có " + pieces + ")."
            : !st.stone || stones < STONES_NEEDED ? "Cần " + STONES_NEEDED + " Đá linh thạch để rèn trang bị."
            : !near(d, st) ? "Hãy đứng gần Ông Morgi để rèn."
            : d.gold + d.lockedGold < GOLD_COST ? "Không đủ vàng (cần " + goldText() + " vàng)."
            : null;
      if (error != null) {
         session.send(Packets.notice(error));
         st.clover = clover;
         sendState(session, d);
         return;
      }
      int piece = st.piece;
      RewardItem recipe = RECIPES.get(piece);
      boolean success = ThreadLocalRandom.current().nextInt(100) < rate(clover);
      String problem;
      synchronized (d) { // túi / vàng: cùng khoá với thư / kí gửi / shop
         UpgradeService.cancel(session, d); // huỷ lựa chọn ở tab Nâng cấp (túi sắp đổi)
         ItemContainer bag = d.bag.deepCopy();
         ItemService.remove(bag, piece, PIECES_NEEDED, now);
         ItemService.remove(bag, STONE, STONES_NEEDED, now);
         if (clover) {
            ItemService.remove(bag, CLOVER, 1, now);
         }
         List<ItemData> out = success ? List.of(recipe.create(now)) : List.of(); // thành phẩm luôn KHOÁ
         problem = ItemService.give(d, bag, out, now); // trên bản sao: thiếu chỗ thì không đổi gì
         if (problem == null) {
            long fromLocked = Math.min(d.lockedGold, GOLD_COST);
            d.lockedGold -= fromLocked;
            d.gold -= GOLD_COST - fromLocked;
         }
      }
      if (problem != null) {
         session.send(Packets.notice(problem));
         return;
      }
      Log.debug("'%s' ren linh thach manh %d (co may man %s) -> %s", d.name, piece, clover,
            success ? "thanh cong" : "that bai");
      items.syncBagAndCurrency(session);
      Message result = new Message(Opcode.FORGE);
      result.writeByte(2);
      result.writeByte(ForgePackets.MODE_CRAFT);
      result.writeByte(success ? 1 : 0);
      session.send(result);
      if (success) { // thành công: đẩy hết nguyên liệu ra khỏi lò
         st.piece = 0;
         st.stone = false;
         st.clover = false;
      } else if (ItemService.count(d.bag, piece, now) == 0) { // thất bại: giữ lựa chọn nếu còn đồ
         st.piece = 0;
      }
      if (!success) {
         st.stone = ItemService.count(d.bag, STONE, now) > 0;
         st.clover = clover && ItemService.count(d.bag, CLOVER, now) > 0;
      }
      sendState(session, d);
      session.send(Packets.notice(success ? "Rèn thành công!" : "Rèn thất bại! Mất nguyên liệu."));
   }

   void sendState(PlayerSession session, PlayerData d) {
      session.send(statePacket(d));
   }

   /** Op 44/1 chế độ 4 (gửi ngay sau khi mở để có mô tả). Số lượng ở ô = số đang có, tối đa số cần. */
   public static Message statePacket(PlayerData d) {
      State st = state(d);
      long now = System.currentTimeMillis();
      Message msg = new Message(Opcode.FORGE);
      msg.writeByte(1);
      msg.writeByte(ForgePackets.MODE_CRAFT);
      ItemPackets.writeItem(msg, st.stone ? shown(d, STONE, STONES_NEEDED, now) : null);
      ItemPackets.writeItem(msg, st.clover ? ItemData.of(CLOVER, 1) : null);
      ItemPackets.writeItem(msg, st.piece != 0 ? shown(d, st.piece, PIECES_NEEDED, now) : null);
      if (st.piece != 0) {
         ItemPackets.writePreviewRanged(msg, RECIPES.get(st.piece), now);
      } else {
         ItemPackets.writeItem(msg, null);
      }
      boolean any = st.piece != 0 || st.stone || st.clover;
      msg.writeString(any ? describe(st.clover) : ""); // mô tả chỉ hiện khi đã cho nguyên liệu vào
      return msg;
   }

   /** Món hiện ở ô: số lượng = min(đang có, cần); không còn món nào -> null. */
   private static ItemData shown(PlayerData d, int templateId, int needed, long now) {
      long have = ItemService.count(d.bag, templateId, now);
      return have <= 0 ? null : ItemData.of(templateId, (int) Math.min(have, needed));
   }

   /** Mô tả (màu nâu như tab Nâng cấp). */
   static String describe(boolean clover) {
      return UpgradeService.BROWN + "Chi phí nâng cấp " + goldText() + " vàng\n"
            + "Tỉ lệ thành công: " + rate(clover) + "% (Nếu thất bại sẽ mất nguyên liệu)\n"
            + "Sử dụng cỏ may mắn để tăng tỉ lệ thành công\n"
            + "Cần " + PIECES_NEEDED + " phôi trang bị và " + STONES_NEEDED + " đá linh thạch để rèn trang bị";
   }

   /** 100.000.000 -> "100.0m". */
   static String goldText() {
      return String.format(java.util.Locale.ROOT, "%.1fm", GOLD_COST / 1_000_000.0);
   }

   private static boolean near(PlayerData d, State st) {
      Float x = d.x;
      Float y = d.y;
      return com.tlnt.server.badge.SpiritBadgeService.remote(d, st.mapId) // mở từ Huy hiệu Tinh Linh
            || st.mapId >= 0 && d.mapId == st.mapId && x != null && y != null
            && Math.abs(x - st.x) <= UpgradeService.FORGE_REACH_PX && Math.abs(y - st.y) <= UpgradeService.FORGE_REACH_PX;
   }

   private static State state(PlayerData d) {
      State st = d.forgeCraft;
      if (st == null) {
         st = new State();
         d.forgeCraft = st;
      }
      return st;
   }

   private static PlayerData player(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể rèn."));
         return null;
      }
      if (d.fishing != null) {
         session.send(Packets.notice("Đang câu cá, không thể rèn."));
         return null;
      }
      return d;
   }
}
