package com.tlnt.server.forge;

import com.tlnt.server.item.ItemContainer;
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
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pha chế ma thuật (ForgeWindow tab "Pha chế", op 44 chế độ 3 — NPC 8 Olivar ở Rạch cá sấu > "Chế tạo Ma thuật"):
 * N Hoa (loại 28, {@link #RECIPES}) + 1 Bình nước phép ({@link #BOTTLE}) + {@link #DIAMOND_COST} lam thạch -> 1 nước
 * ma thuật tương ứng. Tỉ lệ = N × {@link #RATE_PER_FLOWER}% (tối đa {@link #MAX_FLOWERS} hoa = 90%). Thất bại = mất hoa +
 * bình (lam thạch vẫn mất).
 *
 * <pre>
 * C→S 44 chế độ 3: 0 "Chọn" (int ô túi, int số lượng) — hoa -> ô "Nguyên liệu" (số lượng = số hoa, tối đa 9), bình -> ô
 *                  "Bình nước" | 1 "Bỏ ra" (int ô: 0 hoa, 1 bình) | 2 nút "Pha chế" (int số lần — client luôn 1)
 * S→C 44/1 chế độ 3: item hoa, item bình, item thành phẩm (xem trước), chuỗi mô tả
 * S→C 44/2 chế độ 3: byte 1 thành công / 0 thất bại (hiệu ứng ở ô bình)
 * </pre>
 * Món chỉ được CHỌN (vẫn nằm trong túi, lưu theo mẫu + số lượng); lúc bấm Pha chế kiểm tra lại đủ hoa / bình / lam
 * thạch rồi mới trừ. Thành phẩm KHOÁ nếu dùng hoa / bình khoá (đồ khoá được dùng trước). Đứng xa Olivar thì không pha.
 */
public final class BrewService {

   public static final int NPC_OLIVAR = 8;
   /** Bình nước phép. */
   public static final int BOTTLE = 201;
   public static final long DIAMOND_COST = 5;
   public static final int RATE_PER_FLOWER = 10;
   public static final int MAX_FLOWERS = 9;
   /** Hoa -> nước ma thuật (theo mô tả item): Huyết lan -> Tập trung, Mộc diên -> Trường sinh, Tử nguyệt -> Bạo kích,
    *  Lam tinh -> Tinh thần, Hỏa diệp -> Bất khuất, Mộng huyền -> Tốc độ. */
   public static final Map<Integer, Integer> RECIPES = Map.of(193, 162, 194, 158, 195, 160, 196, 159, 197, 163,
         198, 161);

   /** Lựa chọn tab Pha chế (RAM). */
   public static final class State {
      int mapId = -1;
      float x;
      float y;
      /** Mẫu hoa đã đặt (0 = trống) + số hoa. */
      int flower;
      int flowers;
      boolean bottle;
   }

   private final ItemService items;

   public BrewService(ItemService items) {
      this.items = items;
   }

   /** Mở tab ở NPC (map, x, y): client xoá hết ô -> server cũng xoá. */
   public static void open(PlayerData d, int mapId, float x, float y) {
      State st = new State();
      st.mapId = mapId;
      st.x = x;
      st.y = y;
      d.forgeBrew = st;
   }

   /** Tỉ lệ thành công (%) với {@code flowers} hoa. */
   public static int rate(int flowers) {
      return Math.min(MAX_FLOWERS, Math.max(0, flowers)) * RATE_PER_FLOWER;
   }

   /** Thao tác 0: đặt hoa (số lượng nhập, tối đa 9) / bình nước phép vào lò. */
   public void choose(PlayerSession session, int bagSlot, int quantity) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      State st = state(d);
      long now = System.currentTimeMillis();
      if (item.templateId == BOTTLE) {
         st.bottle = true;
      } else if (RECIPES.containsKey(item.templateId)) {
         long have = ItemService.count(d.bag, item.templateId, now);
         st.flower = item.templateId;
         st.flowers = (int) Math.min(Math.min(MAX_FLOWERS, Math.max(1, quantity)), have);
         if (quantity > MAX_FLOWERS) {
            session.send(Packets.notice("Tối đa " + MAX_FLOWERS + " hoa (tỉ lệ " + rate(MAX_FLOWERS) + "%)."));
         }
      } else {
         session.send(Packets.notice("Chỉ dùng được Hoa ma thuật và Bình nước phép."));
         return;
      }
      sendState(session, d);
   }

   /** Thao tác 1: bỏ ô {@code slot} ra (0 hoa, 1 bình). */
   public void remove(PlayerSession session, int slot) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = state(d);
      if (slot == 0) {
         st.flower = 0;
         st.flowers = 0;
      } else if (slot == 1) {
         st.bottle = false;
      }
      sendState(session, d);
   }

   /** Thao tác 2: pha chế 1 lần. */
   public void brew(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = state(d);
      long now = System.currentTimeMillis();
      int flowers = (int) Math.min(st.flowers, st.flower == 0 ? 0 : ItemService.count(d.bag, st.flower, now));
      boolean bottle = st.bottle && ItemService.count(d.bag, BOTTLE, now) > 0;
      String error = flowers <= 0 ? "Hãy chọn Hoa ma thuật."
            : !bottle ? "Hãy chọn Bình nước phép."
            : !near(d, st) ? "Hãy đứng gần Olivar để pha chế."
            : d.diamond < DIAMOND_COST ? "Không đủ lam thạch (cần " + DIAMOND_COST + ")."
            : null;
      if (error != null) {
         session.send(Packets.notice(error));
         st.flowers = flowers;
         st.bottle = bottle;
         sendState(session, d);
         return;
      }
      int resultId = RECIPES.get(st.flower);
      boolean success = ThreadLocalRandom.current().nextInt(100) < rate(flowers);
      String problem = null;
      synchronized (d) { // túi / lam thạch: cùng khoá với thư / kí gửi / shop
         boolean locked = countLocked(d, st.flower, now) > 0 || countLocked(d, BOTTLE, now) > 0; // đồ khoá dùng trước
         ItemContainer bag = d.bag.deepCopy();
         ItemService.remove(bag, st.flower, flowers, now);
         ItemService.remove(bag, BOTTLE, 1, now);
         if (success) {
            ItemData out = ItemData.of(resultId, 1);
            out.locked = locked;
            out.createdTime = now;
            problem = ItemService.give(d, bag, List.of(out), now); // trên bản sao: thiếu chỗ thì không đổi gì
         } else {
            problem = ItemService.give(d, bag, List.of(), now); // thất bại: chỉ mất nguyên liệu
         }
         if (problem == null) {
            d.diamond -= DIAMOND_COST;
         }
      }
      if (problem != null) {
         session.send(Packets.notice(problem));
         return;
      }
      Log.debug("'%s' pha che %d hoa %d -> %s", d.name, flowers, st.flower, success ? "thanh cong" : "that bai");
      items.syncBagAndCurrency(session);
      Message result = new Message(Opcode.FORGE);
      result.writeByte(2);
      result.writeByte(ForgePackets.MODE_BREW);
      result.writeByte(success ? 1 : 0);
      session.send(result);
      // giữ lựa chọn cho lần sau nếu còn đồ
      st.flowers = (int) Math.min(st.flowers, ItemService.count(d.bag, st.flower, now));
      if (st.flowers == 0) {
         st.flower = 0;
      }
      st.bottle = ItemService.count(d.bag, BOTTLE, now) > 0;
      sendState(session, d);
      session.send(Packets.notice(success ? "Pha chế thành công!" : "Pha chế thất bại! Mất nguyên liệu."));
      if (success) {
         com.tlnt.server.achievement.AchievementEvents.add(session, com.tlnt.server.achievement.AchievementType.BREW, 1);
         com.tlnt.server.quest.QuestEvents.fire(session, com.tlnt.server.quest.StepKind.BREW, 0); // NV 19.2
      }
   }

   void sendState(PlayerSession session, PlayerData d) {
      session.send(statePacket(d));
   }

   /** Op 44/1 chế độ 3 (gửi ngay sau khi mở để có mô tả). */
   public static Message statePacket(PlayerData d) {
      State st = state(d);
      Message msg = new Message(Opcode.FORGE);
      msg.writeByte(1);
      msg.writeByte(ForgePackets.MODE_BREW);
      ItemPackets.writeItem(msg, st.flower != 0 && st.flowers > 0 ? ItemData.of(st.flower, st.flowers) : null);
      ItemPackets.writeItem(msg, st.bottle ? ItemData.of(BOTTLE, 1) : null);
      ItemPackets.writeItem(msg, st.flower != 0 ? ItemData.of(RECIPES.get(st.flower), 1) : null);
      msg.writeString(describe(st.flower != 0 ? st.flowers : 0));
      return msg;
   }

   /** Mô tả (màu nâu như tab Nâng cấp). */
   static String describe(int flowers) {
      return UpgradeService.BROWN + "Chi phí pha chế: " + DIAMOND_COST + " Lam thạch\n"
            + "Tỉ lệ thành công: " + rate(flowers) + "% (nếu thất bại sẽ mất nguyên liệu)\n"
            + "Lưu ý: Thêm nhiều hoa để tăng tỉ lệ thành công, mỗi hoa +" + RATE_PER_FLOWER + "% (tối đa "
            + rate(MAX_FLOWERS) + "%)\n";
   }

   private static long countLocked(PlayerData d, int templateId, long now) {
      long n = 0;
      for (int slot = 0; slot < d.bag.size(); slot++) {
         ItemData item = d.bag.get(slot);
         if (item != null && item.templateId == templateId && item.locked && !item.isExpired(now)) {
            n += item.quantity;
         }
      }
      return n;
   }

   private static boolean near(PlayerData d, State st) {
      Float x = d.x;
      Float y = d.y;
      return st.mapId >= 0 && d.mapId == st.mapId && x != null && y != null
            && Math.abs(x - st.x) <= UpgradeService.FORGE_REACH_PX && Math.abs(y - st.y) <= UpgradeService.FORGE_REACH_PX;
   }

   private static State state(PlayerData d) {
      State st = d.forgeBrew;
      if (st == null) {
         st = new State();
         d.forgeBrew = st;
      }
      return st;
   }

   private static PlayerData player(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể pha chế."));
         return null;
      }
      if (d.fishing != null) {
         session.send(Packets.notice("Đang câu cá, không thể pha chế."));
         return null;
      }
      return d;
   }
}
