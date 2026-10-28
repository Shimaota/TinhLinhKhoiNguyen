package com.tlnt.server.farm;

import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcAction;
import com.tlnt.server.npc.NpcContext;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * NPC 18 Cây táo ở Nông trại (map riêng 5 — mỗi nhân vật 1 cây, trạng thái {@link FarmState#apple}, logic ra quả /
 * nâng cấp ở {@link AppleTree}).
 *
 * <p>Hộp thoại: "Cây táo cấp x / Sản lượng tối đa" [Thu hoạch, Nâng cấp]; đang nâng cấp thì chỉ [Nâng cấp nhanh].
 * Thu hoạch = hái HẾT quả trên cây, mỗi quả ngẫu nhiên Táo đỏ / Táo xanh cùng cấp cây hoặc Táo vàng (đều khoá); số táo trong
 * hành trang + số quả hái vượt giới hạn mang ({@link #CARRY_LIMIT}, có Túi đựng Táo thì {@link #CARRY_LIMIT_WITH_BAG})
 * thì không hái quả nào. Nâng cấp: trả vàng (vàng khoá trước, thiếu thì vàng thường), quả đang có đứng yên tới lúc nâng xong; chờ hết giờ hoặc trả
 * lam thạch nâng nhanh.
 *
 * <p>Luồng session của người bấm; vàng / túi sửa dưới {@code synchronized (d)} (giao dịch chốt trên luồng người kia).
 * Đổi xong gửi lại chữ trên đầu cây qua {@link World#refreshAppleTree} (op 52 sub 2).
 */
public final class AppleTreeService implements NpcAction {

   /** {@code npc_template.id} Cây táo / map Nông trại. */
   public static final int NPC_ID = 18;
   public static final int FARM_MAP_ID = 5;

   /** {@code item_template.id}: Táo đỏ cấp 1..10 = 214..223, Táo xanh cấp 1..10 = 224..233, Táo vàng 234. */
   static final int RED_APPLE_LV1 = 214;
   static final int GREEN_APPLE_LV1 = 224;
   static final int GOLDEN_APPLE = 234;
   /** Túi đựng Táo: có trong hành trang thì mang được {@link #CARRY_LIMIT_WITH_BAG} quả. */
   static final int APPLE_BAG = 294;
   static final int CARRY_LIMIT = 10;
   static final int CARRY_LIMIT_WITH_BAG = 99;
   /** Tỉ lệ mỗi quả (%): đỏ, xanh; còn lại = Táo vàng. */
   static final int RED_PERCENT = 49;
   static final int GREEN_PERCENT = 49;

   /** Trang hộp thoại. */
   private static final int PAGE_MAIN = 0;
   private static final int PAGE_UPGRADE = 1;
   private static final int PAGE_UPGRADING = 2;
   private static final int PAGE_QUICK = 3;

   private final World world;
   private final ItemService items;
   private final PlayerService players;

   public AppleTreeService(World world, ItemService items, PlayerService players) {
      this.world = world;
      this.items = items;
      this.players = players;
   }

   @Override
   public void onTalk(NpcContext ctx) {
      long now = System.currentTimeMillis();
      AppleTree tree = ctx.data().farm.apple(now);
      if (tree.upgrading()) {
         ctx.say(PAGE_UPGRADING, "Cây táo cấp " + tree.level() + "\n"
               + "Đang nâng cấp lên cấp " + (tree.level() + 1) + "\n"
               + "Thời gian còn lại: " + duration(tree.upgradeEnd() - now),
               "Nâng cấp nhanh");
      } else {
         ctx.say(PAGE_MAIN, "Cây táo cấp " + tree.level() + "\n"
               + "Sản lượng tối đa: " + AppleTree.maxFruits(tree.level()) + " Quả táo",
               "Thu hoạch", "Nâng cấp");
      }
   }

   @Override
   public void onChoice(NpcContext ctx, int page, int option) {
      if (option != 0 && !(page == PAGE_MAIN && option == 1)) {
         return; // không có nút này
      }
      switch (page) {
         case PAGE_MAIN -> {
            if (option == 0) {
               harvest(ctx);
            } else {
               showUpgrade(ctx);
            }
         }
         case PAGE_UPGRADE -> upgrade(ctx);
         case PAGE_UPGRADING -> showQuickUpgrade(ctx);
         case PAGE_QUICK -> quickUpgrade(ctx);
         default -> {
         }
      }
   }

   // ================================================================== thu hoạch

   private void harvest(NpcContext ctx) {
      harvest(ctx.session(), ctx.data());
   }

   /** Thu hoạch hết quả (bấm Cây táo, hoặc từ xa qua Huy hiệu Tinh Linh). Luồng session. */
   public void harvest(PlayerSession session, PlayerData d) {
      long now = System.currentTimeMillis();
      AppleTree tree = d.farm.apple(now);
      if (tree.upgrading()) {
         session.send(Packets.notice("Cây đang nâng cấp, không thể thu hoạch."));
         return;
      }
      int fruits = tree.fruits(now);
      if (fruits <= 0) {
         session.send(Packets.notice("Cây chưa có quả nào."));
         return;
      }
      int[] rolled = roll(fruits, ThreadLocalRandom.current());
      List<ItemData> apples = apples(tree.level(), rolled);
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể thu hoạch."));
            return;
         }
         int limit = carryLimit(d, now);
         if (carried(d.bag, now) + fruits > limit) {
            session.send(Packets.notice("Chỉ đem tối đa " + limit + " táo trên hành trang"));
            return;
         }
         String error = ItemService.give(d, apples, now); // bản sao túi: thiếu chỗ thì không đổi gì
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
         d.farm.apple = tree.harvested(now);
      }
      items.syncBagAndCurrency(session);
      world.refreshAppleTree(session);
      ItemPackets.receivedNotices(apples).forEach(session::send); // mỗi loại táo 1 dòng
      QuestEvents.fire(session, StepKind.HARVEST_APPLE, NPC_ID);
      com.tlnt.server.daily.DailyEvents.add(session, com.tlnt.server.daily.DailyKind.APPLE, 0, fruits); // NV hằng ngày "Thu hoạch Táo"
      Log.debug("Cay tao: '%s' cap %d hai %d qua (do %d, xanh %d, vang %d)", d.name, tree.level(), fruits,
            rolled[0], rolled[1], rolled[2]);
   }

   /** Chia {@code fruits} quả theo tỉ lệ: [đỏ, xanh, vàng]. */
   static int[] roll(int fruits, Random random) {
      int[] counts = new int[3];
      for (int i = 0; i < fruits; i++) {
         int r = random.nextInt(100);
         counts[r < RED_PERCENT ? 0 : r < RED_PERCENT + GREEN_PERCENT ? 1 : 2]++;
      }
      return counts;
   }

   /** Món nhận được (đều khoá): táo đỏ / xanh cùng cấp cây, táo vàng. Bỏ loại 0 quả. */
   static List<ItemData> apples(int level, int[] counts) {
      int lv = Math.max(1, Math.min(AppleTree.MAX_LEVEL, level)) - 1;
      List<ItemData> list = new ArrayList<>(3);
      int[] ids = {RED_APPLE_LV1 + lv, GREEN_APPLE_LV1 + lv, GOLDEN_APPLE};
      for (int i = 0; i < ids.length; i++) {
         if (counts[i] > 0) {
            ItemData item = ItemData.of(ids[i], counts[i]);
            item.locked = true; // táo cây nông trại luôn khoá
            list.add(item);
         }
      }
      return list;
   }

   /** Số táo (đỏ / xanh mọi cấp + vàng, khoá lẫn không) đang có trong {@code bag}. */
   static long carried(ItemContainer bag, long now) {
      long total = 0;
      for (int id = RED_APPLE_LV1; id <= GOLDEN_APPLE; id++) {
         total += ItemService.count(bag, id, now);
      }
      return total;
   }

   /** Lượng hồi của Táo đỏ (HP) / Táo xanh (MP) cấp 1..10 — theo mô tả item 214..233. */
   private static final long[] APPLE_HEAL = {2_000, 5_000, 10_000, 25_000, 50_000, 80_000, 135_000, 200_000, 300_000,
         500_000};

   /**
    * Lượng hồi 1 quả: {HP, MP}; Táo vàng hồi 100% HP + MP. Không phải táo -> null. Táo không mang chỉ số trên item
    * (tạo từ cây / đồ khởi đầu / thưởng) nên lượng hồi lấy ở đây.
    */
   public static long[] recovery(int templateId, long maxHp, long maxMp) {
      if (templateId == GOLDEN_APPLE) {
         return new long[] {maxHp, maxMp};
      }
      if (templateId >= RED_APPLE_LV1 && templateId < RED_APPLE_LV1 + AppleTree.MAX_LEVEL) {
         return new long[] {APPLE_HEAL[templateId - RED_APPLE_LV1], 0};
      }
      if (templateId >= GREEN_APPLE_LV1 && templateId < GREEN_APPLE_LV1 + AppleTree.MAX_LEVEL) {
         return new long[] {0, APPLE_HEAL[templateId - GREEN_APPLE_LV1]};
      }
      return null;
   }

   /** Táo đỏ / xanh mọi cấp hoặc Táo vàng. */
   public static boolean isApple(int templateId) {
      return templateId >= RED_APPLE_LV1 && templateId <= GOLDEN_APPLE;
   }

   static int carryLimit(PlayerData d, long now) {
      for (ItemData back : d.backs.items) { // bộ sưu tập đeo lưng cộng chỉ số cả bộ -> món nào trong bộ cũng tính
         if (unlimitedApples(back, now)) {
            return Integer.MAX_VALUE;
         }
      }
      return ItemService.count(d.bag, APPLE_BAG, now) > 0 ? CARRY_LIMIT_WITH_BAG : CARRY_LIMIT;
   }

   /** Đeo lưng Giỏ táo (item 353) hoặc món có dòng 72 "Không giới hạn số lượng Táo trong hành trang": mang táo không giới hạn. */
   static final int APPLE_BASKET = 353;
   static final int STAT_UNLIMITED_APPLES = 72;

   static boolean unlimitedApples(ItemData back, long now) {
      if (back == null || back.expireTime > 0 && back.expireTime <= now) {
         return false;
      }
      if (back.templateId == APPLE_BASKET) {
         return true;
      }
      for (ItemData.StatValue s : back.stats) {
         if (s.statTypeId() == STAT_UNLIMITED_APPLES) {
            return true;
         }
      }
      return false;
   }

   // ================================================================== nâng cấp

   private void showUpgrade(NpcContext ctx) {
      AppleTree tree = ctx.data().farm.apple(System.currentTimeMillis());
      if (tree.upgrading()) {
         onTalk(ctx);
         return;
      }
      if (tree.maxLevel()) {
         ctx.send(Packets.notice("Cây táo đã đạt cấp tối đa."));
         return;
      }
      ctx.say(PAGE_UPGRADE, "Nâng cấp cây lên cấp " + (tree.level() + 1) + "\n"
            + "Chi phí: " + format(AppleTree.upgradeGold(tree.level())) + " vàng\n"
            + "Thời gian nâng cấp: " + duration(AppleTree.upgradeMs(tree.level())),
            "Nâng cấp");
   }

   private void upgrade(NpcContext ctx) {
      PlayerSession session = ctx.session();
      PlayerData d = ctx.data();
      long now = System.currentTimeMillis();
      AppleTree tree = d.farm.apple(now);
      if (tree.upgrading() || tree.maxLevel()) {
         return;
      }
      long cost = AppleTree.upgradeGold(tree.level());
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể nâng cấp."));
            return;
         }
         long fromLocked = Math.min(d.lockedGold, cost); // vàng khoá trước (thưởng nhiệm vụ), thiếu thì vàng thường
         if (d.gold < cost - fromLocked) {
            session.send(Packets.notice("Không đủ " + format(cost) + " vàng."));
            return;
         }
         d.lockedGold -= fromLocked;
         d.gold -= cost - fromLocked;
         d.farm.apple = tree.upgradeStarted(now);
      }
      afterChange(session, d);
      session.send(Packets.notice("Bắt đầu nâng cấp cây táo lên cấp " + (tree.level() + 1) + "."));
      QuestEvents.fire(session, StepKind.UPGRADE_APPLE_TREE, NPC_ID);
      Log.debug("Cay tao: '%s' nang cap len %d, tra %d vang", d.name, tree.level() + 1, cost);
   }

   private void showQuickUpgrade(NpcContext ctx) {
      AppleTree tree = ctx.data().farm.apple(System.currentTimeMillis());
      if (!tree.upgrading()) {
         onTalk(ctx); // vừa nâng xong
         return;
      }
      ctx.say(PAGE_QUICK, "Nâng cấp nhanh\n"
            + "Chi phí: " + format(AppleTree.quickDiamond(tree.level())) + " Lam thạch",
            "Nâng cấp nhanh");
   }

   private void quickUpgrade(NpcContext ctx) {
      PlayerSession session = ctx.session();
      PlayerData d = ctx.data();
      long now = System.currentTimeMillis();
      AppleTree tree = d.farm.apple(now);
      if (!tree.upgrading()) {
         session.send(Packets.notice("Cây táo đã nâng cấp xong."));
         return;
      }
      long cost = AppleTree.quickDiamond(tree.level());
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể nâng cấp."));
            return;
         }
         if (d.diamond < cost) {
            session.send(Packets.notice("Không đủ " + format(cost) + " lam thạch."));
            return;
         }
         d.diamond -= cost;
         d.farm.apple = tree.upgradeFinished(now);
      }
      afterChange(session, d);
      session.send(Packets.notice("Cây táo đã lên cấp " + (tree.level() + 1) + "."));
      Log.debug("Cay tao: '%s' nang nhanh len %d, tra %d lam thach", d.name, tree.level() + 1, cost);
   }

   /** Vừa trừ tiền: gửi lại tiền + chữ trên cây, lưu ngay (tiền + cây cùng 1 dòng player). */
   private void afterChange(PlayerSession session, PlayerData d) {
      items.syncBagAndCurrency(session);
      world.refreshAppleTree(session);
      players.saveAsync(d, false);
   }

   // ================================================================== chữ

   /** "3 ngày", "30 phút", "1 ngày 2 giờ 5 phút" (làm tròn lên phút; dưới 1 phút = giây). */
   static String duration(long ms) {
      long seconds = Math.max(0, (ms + 999) / 1000);
      if (seconds < 60) {
         return seconds + " giây";
      }
      long minutes = (seconds + 59) / 60;
      long days = minutes / (24 * 60);
      long hours = minutes / 60 % 24;
      long mins = minutes % 60;
      StringBuilder sb = new StringBuilder();
      if (days > 0) {
         sb.append(days).append(" ngày");
      }
      if (hours > 0) {
         sb.append(sb.isEmpty() ? "" : " ").append(hours).append(" giờ");
      }
      if (mins > 0) {
         sb.append(sb.isEmpty() ? "" : " ").append(mins).append(" phút");
      }
      return sb.toString();
   }

   private static String format(long value) {
      return String.format(Locale.US, "%,d", value).replace(',', '.');
   }
}
