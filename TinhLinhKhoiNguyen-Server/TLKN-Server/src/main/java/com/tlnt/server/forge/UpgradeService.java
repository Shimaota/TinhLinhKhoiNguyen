package com.tlnt.server.forge;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nâng cấp trang bị (ForgeWindow tab "Nâng cấp", op 44 chế độ 0). Luật: {@link UpgradeRules}. Lane GAME, luồng session
 * (túi đồ chỉ luồng session sửa).
 *
 * <pre>
 * C→S 44: byte chế độ (0), byte thao tác:
 *   0 "Chọn" món trong túi   int ô túi, int số lượng  ({@code sendPvpAction} — tên sai)
 *       trang bị -> ô Trang bị; Đá Mặt trời / Mặt trăng -> ô Nguyên liệu (số viên); bùa bảo hiểm / Ngọc trai đen
 *       -> ô Bảo hiểm
 *   1 "Bỏ ra" 1 ô lò rèn     int ô (0 nguyên liệu, 1 bảo hiểm, 2 trang bị)  ({@code sendPvpChallenge})
 *   2 nút "Nâng cấp"         int 1  ({@code sendPvpAccept})
 * S→C 44/1 chế độ 0: item nguyên liệu, item bảo hiểm, item trang bị, item kết quả (xem trước cấp kế), chuỗi mô tả
 * S→C 44/2 chế độ 0: byte 1 thành công / 0 thất bại (hiệu ứng ở ô trang bị)
 * </pre>
 * Món chỉ được CHỌN (vẫn nằm trong túi); lúc bấm Nâng cấp kiểm tra lại mọi thứ rồi mới trừ đá / bảo hiểm / vàng.
 * Chỉ nâng trang bị trong hành trang (đang mặc thì tháo ra trước).
 *
 * <p><b>Tránh xung đột</b>: client đóng cửa sổ KHÔNG báo server, nên không chặn thao tác khác (chặn sẽ kẹt sau khi
 * tắt tab). Thay vào đó mọi thao tác đổi túi / vàng khác (mặc, dùng, vứt, cất rương, giao dịch, kí gửi, thư, shop)
 * gọi {@link #cancel} = HUỶ lựa chọn trong lò (ô lò trống lại). Lúc bấm Nâng cấp vẫn kiểm tra lại đúng món + đủ đá +
 * đủ vàng nên không thể nâng / trừ nhầm. Đi xa thợ rèn / đổi map / mở lại lò cũng huỷ.
 */
public final class UpgradeService {

   /** Đứng cách thợ rèn tối đa bấy nhiêu pixel (mỗi trục) mới tính là đang ở lò. */
   public static final float FORGE_REACH_PX = 600f;

   /** Lựa chọn đang đặt trong lò rèn (RAM, không lưu). */
   public static final class State {
      /** Vị trí thợ rèn đã mở lò (map + toạ độ NPC). */
      int mapId = -1;
      float x;
      float y;
      ItemData equip;
      int equipSlot = -1;
      int stoneTemplate;
      int stones;
      int insuranceTemplate;
   }

   private static final Locale VI = Locale.forLanguageTag("vi-VN");

   private final ItemService items;

   public UpgradeService(ItemService items) {
      this.items = items;
   }

   /** Mở cửa sổ lò rèn ở NPC (map, x, y): client xoá hết ô -> server cũng xoá lựa chọn cũ. */
   public static void open(PlayerData d, int mapId, float x, float y) {
      State st = new State();
      st.mapId = mapId;
      st.x = x;
      st.y = y;
      d.forgeUpgrade = st;
   }

   /**
    * Thao tác khác đổi túi / vàng (mặc, dùng, giao dịch, kí gửi, thư, shop...): huỷ lựa chọn trong lò (luồng session
    * của chính người đó). Có món đang đặt thì gửi lò trống (nếu cửa sổ còn mở, client thấy ô trống).
    */
   public static void cancel(PlayerSession session, PlayerData d) {
      SoulService.cancel(session, d); // tab Linh hồn cũng giữ món trong túi -> huỷ cùng lúc
      ElementService.cancel(session, d); // tab Nguyên tố cũng vậy
      TransmuteService.cancel(session, d); // tab Chuyển hóa cũng vậy
      State st = d.forgeUpgrade;
      if (st == null || (st.equip == null && st.stoneTemplate == 0 && st.insuranceTemplate == 0)) {
         return;
      }
      st.equip = null;
      st.equipSlot = -1;
      st.stoneTemplate = 0;
      st.stones = 0;
      st.insuranceTemplate = 0;
      session.send(statePacket(st));
   }

   private static boolean near(PlayerData d, State st) {
      Float x = d.x;
      Float y = d.y;
      return com.tlnt.server.badge.SpiritBadgeService.remote(d, st.mapId) // mở từ Huy hiệu Tinh Linh
            || st.mapId >= 0 && d.mapId == st.mapId && x != null && y != null && Math.abs(x - st.x) <= FORGE_REACH_PX
            && Math.abs(y - st.y) <= FORGE_REACH_PX;
   }

   // ================================================================== thao tác

   /** Thao tác 0: chọn 1 món trong túi. */
   public void choose(PlayerSession session, int bagSlot, int quantity) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      State st = state(d);
      long now = System.currentTimeMillis();
      if (UpgradeRules.upgradable(item)) {
         if (item.starCount >= UpgradeRules.MAX_LEVEL) {
            session.send(Packets.notice("Trang bị đã đạt cấp tối đa."));
            return;
         }
         st.equip = item;
         st.equipSlot = bagSlot;
         clampStones(d, st, now);
      } else if (UpgradeRules.gradeOfStone(item.templateId) != UpgradeRules.GRADE_NONE) {
         st.stoneTemplate = item.templateId;
         st.stones = (int) Math.min(Math.max(1, quantity), ItemService.count(d.bag, item.templateId, now));
         clampStones(d, st, now);
      } else if (UpgradeRules.isInsurance(item.templateId)) {
         st.insuranceTemplate = item.templateId;
      } else {
         session.send(Packets.notice("Vật phẩm này không dùng để nâng cấp."));
         return;
      }
      sendState(session, d);
   }

   /** Thao tác 1: bỏ 1 ô ra (0 nguyên liệu, 1 bảo hiểm, 2 trang bị). */
   public void remove(PlayerSession session, int forgeSlot) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = state(d);
      switch (forgeSlot) {
         case 0 -> {
            st.stoneTemplate = 0;
            st.stones = 0;
         }
         case 1 -> st.insuranceTemplate = 0;
         case 2 -> {
            st.equip = null;
            st.equipSlot = -1;
         }
         default -> {
            return;
         }
      }
      sendState(session, d);
   }

   /** Thao tác 2: nút "Nâng cấp". */
   public void upgrade(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      State st = state(d);
      long now = System.currentTimeMillis();
      String error = check(d, st, now);
      if (error != null) {
         session.send(Packets.notice(error));
         sendState(session, d);
         return;
      }
      ItemData equip = st.equip;
      int level = equip.starCount;
      UpgradeRules.Step step = UpgradeRules.step(level);
      int chance = UpgradeRules.chancePermille(step, st.stones);
      boolean pearl = st.insuranceTemplate == UpgradeRules.BLACK_PEARL; // đã kiểm tra cấp ≥ 4
      boolean spirit = st.insuranceTemplate == UpgradeRules.INSURANCE_SPIRIT; // mọi cấp, thất bại hoàn tài nguyên
      boolean useInsurance = !pearl && st.insuranceTemplate != 0 && (spirit || level >= UpgradeRules.DOWNGRADE_FROM_LEVEL);
      boolean success = ThreadLocalRandom.current().nextInt(1000) < chance;
      boolean refund = spirit && !success;
      int stoneUsed = st.stoneTemplate;
      com.tlnt.server.ranking.RankingService.beforeUpgrade(d, equip); // Cường hóa tuần: ghi cấp đầu tuần của món
      synchronized (d) { // vàng / túi: cùng khoá với thư / kí gửi
         if (!refund) {
            payGold(d, step.gold());
            ItemService.remove(d.bag, st.stoneTemplate, st.stones, now);
         }
         if (useInsurance || pearl) { // Ngọc trai đen: mất mỗi lần nâng (thành công hay thất bại)
            ItemService.remove(d.bag, st.insuranceTemplate, 1, now);
         }
         if (success) {
            if (equip.starGrade == UpgradeRules.GRADE_NONE) {
               equip.starGrade = UpgradeRules.gradeOfStone(st.stoneTemplate); // lần đầu: chọn Nhật / Nguyệt ấn
            }
            UpgradeRules.levelUp(equip, pearl ? UpgradeRules.PEARL_PERCENT : UpgradeRules.PERCENT_PER_LEVEL);
         } else if (pearl) {
            UpgradeRules.setLevel(equip, UpgradeRules.PEARL_FAIL_LEVEL);
         } else if (level >= UpgradeRules.DOWNGRADE_FROM_LEVEL && !useInsurance) {
            UpgradeRules.setLevel(equip, level - 1);
         }
      }
      String name = name(equip.templateId);
      String text;
      if (success) {
         text = "Nâng cấp thành công! " + name + " đạt cấp " + equip.starCount + (pearl ? " (Ngọc trai đen: +"
               + UpgradeRules.PEARL_PERCENT + "% chỉ số)." : ".");
      } else if (pearl) {
         text = "Nâng cấp thất bại! Ngọc trai đen vỡ, " + name + " quay về cấp " + equip.starCount + ".";
      } else if (refund) {
         text = "Nâng cấp thất bại! Bùa bảo hiểm Tinh linh đã giữ " + name + " ở cấp " + equip.starCount
               + " và giữ lại nguyên liệu, vàng.";
      } else if (useInsurance) {
         text = "Nâng cấp thất bại! Bùa bảo hiểm đã giữ " + name + " ở cấp " + equip.starCount + ".";
      } else if (level >= UpgradeRules.DOWNGRADE_FROM_LEVEL) {
         text = "Nâng cấp thất bại! " + name + " bị giảm xuống cấp " + equip.starCount + ".";
      } else {
         text = "Nâng cấp thất bại! Mất nguyên liệu.";
      }
      Log.debug("'%s' nang cap item %d cap %d -> %d (%s, ti le %d/1000)", d.name, equip.templateId, level,
            equip.starCount, success ? "thanh cong" : "that bai", chance);
      items.syncBagAndCurrency(session);
      Message result = new Message(Opcode.FORGE);
      result.writeByte(2);
      result.writeByte(ForgePackets.MODE_UPGRADE);
      result.writeByte(success ? 1 : 0);
      session.send(result);
      if (success) { // thành công: đá tự bị đẩy ra khỏi ô nguyên liệu (cấp mới cần số đá khác), chọn lại mới nâng tiếp
         st.stoneTemplate = 0;
         st.stones = 0;
      } else { // thất bại: giữ đá trong ô (bớt theo số còn lại) để bấm nâng tiếp ngay
         clampStones(d, st, now);
      }
      if (st.insuranceTemplate != 0 && ItemService.count(d.bag, st.insuranceTemplate, now) <= 0) {
         st.insuranceTemplate = 0;
      }
      if (equip.starCount >= UpgradeRules.MAX_LEVEL) {
         text += " Đã đạt cấp tối đa.";
      }
      sendState(session, d);
      session.send(Packets.notice(text));
      QuestEvents.fire(session, StepKind.UPGRADE_STONE, stoneUsed); // nhiệm vụ: thành công hay thất bại đều tính
      if (success) { // 17.1 + thành tựu "Cường hóa trang bị N sao": chỉ tính lúc NÂNG THÀNH CÔNG, = số sao vừa đạt
         QuestEvents.fire(session, StepKind.UPGRADE_STAR, 0, equip.starCount);
         com.tlnt.server.achievement.AchievementEvents.add(session,
               com.tlnt.server.achievement.AchievementType.UPGRADE_STAR, equip.starCount);
      }
   }

   /** Lý do không nâng được, null = được. */
   private static String check(PlayerData d, State st, long now) {
      ItemData equip = st.equip;
      if (equip == null || st.equipSlot < 0 || d.bag.get(st.equipSlot) != equip) {
         st.equip = null;
         st.equipSlot = -1;
         return "Hãy chọn trang bị cần nâng cấp.";
      }
      UpgradeRules.Step step = UpgradeRules.step(equip.starCount);
      if (step == null) {
         return "Trang bị đã đạt cấp tối đa.";
      }
      if (st.stoneTemplate == 0 || st.stones <= 0) {
         return "Hãy chọn Đá Mặt trời hoặc Đá Mặt trăng.";
      }
      byte grade = UpgradeRules.gradeOfStone(st.stoneTemplate);
      if (equip.starGrade != UpgradeRules.GRADE_NONE && equip.starGrade != grade) {
         return "Trang bị " + (equip.starGrade == UpgradeRules.GRADE_SUN ? "Nhật ấn chỉ nâng bằng Đá Mặt trời."
               : "Nguyệt ấn chỉ nâng bằng Đá Mặt trăng.");
      }
      if (ItemService.count(d.bag, st.stoneTemplate, now) < st.stones) {
         return "Không đủ " + name(st.stoneTemplate) + ".";
      }
      if (st.insuranceTemplate != 0) {
         if (ItemService.count(d.bag, st.insuranceTemplate, now) <= 0) {
            st.insuranceTemplate = 0;
            return "Không còn bùa bảo hiểm.";
         }
         if (st.insuranceTemplate == UpgradeRules.BLACK_PEARL && equip.starCount < UpgradeRules.PEARL_MIN_LEVEL) {
            return name(UpgradeRules.BLACK_PEARL) + " chỉ dùng cho trang bị từ cấp " + UpgradeRules.PEARL_MIN_LEVEL
                  + " trở lên.";
         }
         if (st.insuranceTemplate == UpgradeRules.INSURANCE_1 && equip.starCount >= UpgradeRules.INSURANCE_1_MAX_LEVEL) {
            return name(UpgradeRules.INSURANCE_1) + " chỉ dùng cho trang bị dưới " + UpgradeRules.INSURANCE_1_MAX_LEVEL
                  + " sao.";
         }
      }
      if (d.gold + d.lockedGold < step.gold()) {
         return "Không đủ vàng (cần " + money(step.gold()) + ").";
      }
      return null;
   }

   /** Trừ vàng khoá trước, thiếu thì trừ vàng thường (đã kiểm tra đủ). */
   static void payGold(PlayerData d, long amount) {
      long fromLocked = Math.min(d.lockedGold, amount);
      d.lockedGold -= fromLocked;
      d.gold -= amount - fromLocked;
   }

   /** Số đá: không quá số đang có, không quá số cần để đạt tỉ lệ tối đa (thừa không có ích). */
   private static void clampStones(PlayerData d, State st, long now) {
      if (st.stoneTemplate == 0) {
         return;
      }
      long have = ItemService.count(d.bag, st.stoneTemplate, now);
      UpgradeRules.Step step = st.equip != null ? UpgradeRules.step(st.equip.starCount) : null;
      long max = step != null ? Math.min(have, step.stones()) : have;
      st.stones = (int) Math.min(st.stones, max);
      if (st.stones <= 0) {
         st.stoneTemplate = 0;
         st.stones = 0;
      }
   }

   // ================================================================== gói S→C

   /** Op 44/1 chế độ 0: 4 ô + mô tả. */
   void sendState(PlayerSession session, PlayerData d) {
      session.send(statePacket(state(d)));
   }

   /** Gói trạng thái tab Nâng cấp (gửi ngay sau khi mở lò để có mô tả). */
   public static Message statePacket(PlayerData d) {
      return statePacket(state(d));
   }

   private static Message statePacket(State st) {
      Message msg = new Message(Opcode.FORGE);
      msg.writeByte(1);
      msg.writeByte(ForgePackets.MODE_UPGRADE);
      ItemPackets.writeItem(msg, st.stoneTemplate != 0 ? ItemData.of(st.stoneTemplate, st.stones) : null);
      ItemPackets.writeItem(msg, st.insuranceTemplate != 0 ? ItemData.of(st.insuranceTemplate, 1) : null);
      ItemPackets.writeItem(msg, st.equip);
      ItemPackets.writeItem(msg, preview(st));
      msg.writeString(describe(st));
      return msg;
   }

   /** Trang bị ở cấp kế (thành công) để xem trước chỉ số. */
   private static ItemData preview(State st) {
      if (st.equip == null || st.equip.starCount >= UpgradeRules.MAX_LEVEL) {
         return null;
      }
      ItemData next = st.equip.copy();
      UpgradeRules.levelUp(next, st.insuranceTemplate == UpgradeRules.BLACK_PEARL
            && next.starCount >= UpgradeRules.PEARL_MIN_LEVEL ? UpgradeRules.PEARL_PERCENT : UpgradeRules.PERCENT_PER_LEVEL);
      if (next.starGrade == UpgradeRules.GRADE_NONE && st.stoneTemplate != 0) {
         next.starGrade = UpgradeRules.gradeOfStone(st.stoneTemplate);
      }
      return next;
   }

   /** Màu nâu của chữ trên nền giấy (client dùng ở hộp mời / tooltip: [#8B4E23]). */
   static final String BROWN = "[#8B4E23]";

   /**
    * 4 dòng mô tả (màu nâu), luôn đủ 4 dòng — chưa chọn đủ thì số = 0:
    * <pre>
    * Phí nâng cấp: x Vàng
    * Tỉ lệ thành công: 0.0% (Nếu thất bại sẽ mất nguyên liệu)
    * Trang bị từ cấp 2 trở lên, nếu không có bảo hiểm thất bại sẽ bị giảm 1 cấp trang bị
    * Cần x đá nâng cấp đạt tỉ lệ thành công tối đa là 0.0%
    * </pre>
    */
   static String describe(State st) {
      if (st.equip != null && st.equip.starCount >= UpgradeRules.MAX_LEVEL) {
         return BROWN + "Trang bị đã đạt cấp tối đa (" + UpgradeRules.MAX_LEVEL + ").";
      }
      UpgradeRules.Step step = st.equip != null ? UpgradeRules.step(st.equip.starCount) : null;
      long gold = step != null ? step.gold() : 0;
      int chance = step != null ? UpgradeRules.chancePermille(step, st.stones) : 0;
      int stones = step != null ? step.stones() : 0;
      int maxRate = step != null ? step.ratePermille() : 0;
      return BROWN + "Phí nâng cấp: " + String.format(VI, "%,d", gold) + " Vàng\n"
            + "Tỉ lệ thành công: " + percent(chance) + " (Nếu thất bại sẽ mất nguyên liệu)\n"
            + (st.insuranceTemplate == UpgradeRules.BLACK_PEARL
                  ? "Ngọc trai đen: thành công +" + UpgradeRules.PEARL_PERCENT + "% chỉ số thay vì "
                        + UpgradeRules.PERCENT_PER_LEVEL + "%, thất bại trang bị về cấp " + UpgradeRules.PEARL_FAIL_LEVEL
                  : "Trang bị từ cấp 2 trở lên, nếu không có bảo hiểm thất bại sẽ bị giảm 1 cấp trang bị") + "\n"
            + "Cần " + stones + " đá nâng cấp đạt tỉ lệ thành công tối đa là " + percent(maxRate);
   }

   // ================================================================== nội bộ

   private static State state(PlayerData d) {
      State st = d.forgeUpgrade;
      if (st == null) {
         st = new State();
         d.forgeUpgrade = st;
      }
      return st;
   }

   /** Người chơi thao tác được (đang đứng ở lò đã mở, không giao dịch / câu cá). */
   private static PlayerData player(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      State st = d.forgeUpgrade;
      if (st == null || !near(d, st)) {
         d.forgeUpgrade = null;
         session.send(Packets.notice("Hãy đến gặp thợ rèn để cường hóa."));
         return null;
      }
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể nâng cấp."));
         return null;
      }
      if (d.fishing != null) {
         session.send(Packets.notice("Đang câu cá, không thể nâng cấp."));
         return null;
      }
      return d;
   }

   static String money(long gold) {
      return String.format(VI, "%,d", gold) + " vàng";
   }

   /** "37.5%" (dấu chấm thập phân như mẫu). */
   static String percent(int permille) {
      return String.format(Locale.ROOT, "%.1f%%", permille / 10.0);
   }

   static String name(int templateId) {
      ItemPack.ItemTemplate t = GameDataCache.itemTemplate(templateId);
      return t != null ? t.name() : "vật phẩm #" + templateId;
   }
}
