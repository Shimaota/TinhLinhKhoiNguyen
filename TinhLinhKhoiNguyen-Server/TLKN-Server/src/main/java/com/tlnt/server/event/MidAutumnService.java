package com.tlnt.server.event;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.item.GiftBoxes;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcAction;
import com.tlnt.server.npc.NpcContext;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.MainQuests;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.shop.ShopService;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.random.RandomGenerator;

/**
 * Sự kiện Trung thu (spec người dùng 2026-10-13) — mọi thứ chỉ có khi {@link GameEvents#midAutumn()} bật:
 * <ul>
 *   <li>NPC 21 Nguyệt linh thần (map 2, ẩn khi tắt — {@code map/MapPackets}): Nhận quà Trung thu (điểm danh 1 lần / ngày,
 *       phải xong NV 6), Làm bánh Trung thu (2 công thức {@link MidAutumn#BAKED} / {@link MidAutumn#SNOW}), Cửa hàng
 *       (shop {@link MidAutumn#SHOP}).</li>
 *   <li>Bánh = hộp quà ({@link MidAutumn#registerBoxes}) +15 điểm / cái ({@link #boxOpened}).</li>
 *   <li>Cà rốt: cây 11 ở map 1-4 (plant_template.harvest có "event") +1 điểm khi hái; dùng cà rốt gần Ngọc thố
 *       ({@link RabbitService}) -> thỏ ăn, +3 điểm + quà ({@link #rabbitRewards}).</li>
 *   <li>Đèn hoa đăng 354: đứng gần nước, tối đa {@link #LANTERNS_PER_WATER} đèn / vùng nước / khu, +5 điểm + quà.</li>
 *   <li>Tinh anh / Thủ lĩnh rơi nguyên liệu ({@link #eliteMaterials}); boss Cổ thần Dạ xoa (world_boss 11, "event") +25
 *       điểm cho người kết liễu ({@link #bossKilled}).</li>
 * </ul>
 * Điểm sự kiện = {@code RankStats.eventPoints} (bảng "Sự kiện" op 39). Trạng thái điểm danh = cột JSON
 * {@code player.event_state} ({@link EventState}).
 */
public final class MidAutumnService implements NpcAction {

   static final String NPC_TEXT = "Ánh trăng đêm nay thật đẹp..Chào mừng người đến với lễ hội Trăng rằm\n"
         + "Mong rằng ánh trăng sẽ mang đến cho Eldarah một mùa trung thu tốt lành";
   static final String BAKE_TEXT = "Hương bánh hòa cùng ánh trăng chính là nét đẹp của lễ hội Trăng rằm\n"
         + "Nếu ngươi đã chuẩn bị đủ nguyên liệu, ta sẽ giúp ngươi làm những chiếc bánh Trung thu\n"
         + "Nguyên liệu cần có:\n"
         + "[#FFF600]Bánh nướng => 5 Bột mì, 10 Đường, 10 Sữa tươi, 2 Trứng muối + 1 Mật ong[reset]\n"
         + "[#FFF600]Bánh dẻo => 5 Bột nếp, 10 Đường, 10 Sữa tươi, 2 Trứng muối + 1 Quả dừa[reset]";
   static final String CLOSED_TEXT = "Lễ hội Trăng rằm đã kết thúc. Hẹn gặp lại người vào mùa trăng sau!";
   static final String NOT_RUNNING = "Lễ hội Trăng rằm chưa diễn ra.";

   static final int PAGE_MAIN = 0;
   static final int PAGE_BAKE = 1;

   /** Thả đèn: đứng cách mặt nước tối đa bấy nhiêu px. */
   static final float LANTERN_REACH_PX = 200f;
   static final int LANTERNS_PER_WATER = 20;
   /** Tốc trôi đèn (px / s) — đèn trôi sang phải tới mép phải vùng nước rồi mất (op 80, client tự tính). */
   static final float LANTERN_SPEED_PX = 25f;
   /** Khoảng cách giữa 2 lần ném cà rốt của 1 người. */
   static final long CARROT_COOLDOWN_MS = 10_000;

   private static volatile MidAutumnService instance;

   private final World world;
   private final ItemService items;
   private final ShopService shops;
   private final Executor gamePool;

   public MidAutumnService(World world, ItemService items, ShopService shops, Executor gamePool) {
      this.world = world;
      this.items = items;
      this.shops = shops;
      this.gamePool = gamePool;
      MidAutumn.registerBoxes();
      instance = this;
   }

   // ================================================================== NPC 21 Nguyệt linh thần

   @Override
   public void onTalk(NpcContext ctx) {
      if (!GameEvents.midAutumn()) {
         ctx.say(PAGE_MAIN, CLOSED_TEXT, "Đóng");
         return;
      }
      ctx.say(PAGE_MAIN, NPC_TEXT, "Nhận quà Trung thu", "Làm bánh Trung thu", "Cửa hàng", "Đóng");
   }

   @Override
   public void onChoice(NpcContext ctx, int page, int option) {
      if (!GameEvents.midAutumn()) {
         ctx.session().send(Packets.notice(CLOSED_TEXT));
         return;
      }
      if (page == PAGE_BAKE) {
         switch (option) {
            case 0 -> bake(ctx.session(), ctx.data(), MidAutumn.BAKED);
            case 1 -> bake(ctx.session(), ctx.data(), MidAutumn.SNOW);
            default -> {
            }
         }
         return;
      }
      switch (option) {
         case 0 -> checkIn(ctx.session(), ctx.data());
         case 1 -> ctx.say(PAGE_BAKE, BAKE_TEXT, "Làm bánh Nướng", "Làm bánh dẻo");
         case 2 -> shops.open(ctx, MidAutumn.SHOP);
         default -> {
         }
      }
   }

   /** Quà điểm danh: 1 lần / ngày, phải xong NV 6 "Ấn Ký Nhật Nguyệt". Luồng session. */
   void checkIn(PlayerSession session, PlayerData d) {
      if (MainQuests.currentQuest(d.mainQuest) <= MidAutumn.CHECK_IN_QUEST) {
         session.send(Packets.notice("Hãy hoàn thành nhiệm vụ Ấn Ký Nhật Nguyệt rồi quay lại nhận quà nhé!"));
         return;
      }
      int today = (int) LocalDate.now().toEpochDay();
      if (d.eventState.checkInDay == today) {
         session.send(Packets.notice("Hôm nay người đã nhận quà Trung thu rồi, hãy quay lại vào ngày mai."));
         return;
      }
      long now = System.currentTimeMillis();
      List<ItemData> gifts = GiftBoxes.roll(MidAutumn.CHECK_IN_BOX, true, d.gender, now,
            java.util.concurrent.ThreadLocalRandom.current());
      String error;
      synchronized (d) {
         error = ItemService.give(d, gifts, now);
         if (error == null) {
            d.eventState.checkInDay = today;
         }
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      showReward(session, gifts);
      EventStats.CHECK_INS.incrementAndGet();
      Log.debug("'%s' nhan qua diem danh Trung thu", Log.ascii(d.name));
   }

   /** Làm 1 chiếc bánh: đủ mọi nguyên liệu (khoá trước) thì trừ + nhận bánh khoá. Luồng session. */
   void bake(PlayerSession session, PlayerData d, MidAutumn.Recipe recipe) {
      long now = System.currentTimeMillis();
      StringBuilder missing = new StringBuilder();
      for (int[] need : recipe.needs()) {
         long have = ItemService.count(d.bag, need[0], now);
         if (have < need[1]) {
            missing.append(missing.isEmpty() ? "" : ", ").append(need[1] - have).append(' ').append(itemName(need[0]));
         }
      }
      if (!missing.isEmpty()) {
         session.send(Packets.notice("Chưa đủ nguyên liệu làm " + recipe.name() + ", còn thiếu " + missing + "."));
         return;
      }
      ItemData cake = ItemData.of(recipe.cake(), 1);
      cake.locked = true;
      cake.createdTime = now;
      String error;
      synchronized (d) {
         com.tlnt.server.item.ItemContainer bag = d.bag.deepCopy();
         for (int[] need : recipe.needs()) {
            ItemService.remove(bag, need[0], need[1], now);
         }
         error = ItemService.give(d, bag, List.of(cake), now);
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      showReward(session, List.of(cake));
      EventStats.CAKES_BAKED.incrementAndGet();
      Log.debug("'%s' lam %s", Log.ascii(d.name), Log.ascii(recipe.name()));
   }

   // ================================================================== item

   /** Mở bánh (hộp quà — {@code UsableItems.openBox}): +15 điểm / cái. Luồng session. */
   public static void boxOpened(PlayerSession session, PlayerData d, int templateId, int count) {
      if ((templateId == MidAutumn.SNOW_CAKE || templateId == MidAutumn.BAKED_CAKE) && count > 0) {
         addPoints(session, d, (long) MidAutumn.POINTS_CAKE * count);
         EventStats.CAKES_OPENED.addAndGet(count);
      }
   }

   /** Dùng Cà rốt / Đèn hoa đăng ({@code UsableItems}). Luồng session. */
   public static void useItem(PlayerSession session, PlayerData d, int bagSlot, ItemData item) {
      MidAutumnService s = instance;
      if (s == null || !GameEvents.midAutumn()) {
         session.send(Packets.notice(NOT_RUNNING));
         return;
      }
      if (item.templateId == MidAutumn.CARROT) {
         s.throwCarrot(session, d, bagSlot, item);
      } else {
         s.releaseLantern(session, d, bagSlot, item);
      }
   }

   private void throwCarrot(PlayerSession session, PlayerData d, int bagSlot, ItemData item) {
      long now = System.currentTimeMillis();
      long wait = d.eventState.lastCarrotAt + CARROT_COOLDOWN_MS - now;
      if (wait > 0) {
         session.send(Packets.notice("Hãy đợi " + (wait + 999) / 1000 + " giây nữa mới ném được cà rốt."));
         return;
      }
      d.eventState.lastCarrotAt = now;
      world.feedRabbit(session,
            () -> session.inbox().execute(() -> {
               if (consume(session, d, bagSlot, item)) {
                  session.send(Packets.notice("Đã ném 1 Cà rốt cho Ngọc thố."));
                  EventStats.CARROTS_THROWN.incrementAndGet();
               }
            }, gamePool),
            () -> session.inbox().execute(() -> {
               d.eventState.lastCarrotAt = 0; // không ném được: không tính hồi
               session.send(Packets.notice("Không có Ngọc thố nào ở gần, hãy lại gần Ngọc thố rồi ném cà rốt."));
            }, gamePool));
   }

   private void releaseLantern(PlayerSession session, PlayerData d, int bagSlot, ItemData item) {
      if (d.bag.countEmpty() == 0) {
         session.send(Packets.notice("Hành trang đã đầy."));
         return;
      }
      world.releaseLantern(session, LANTERN_REACH_PX, LANTERNS_PER_WATER, LANTERN_SPEED_PX,
            () -> session.inbox().execute(() -> lanternReleased(session, d, bagSlot, item), gamePool),
            text -> session.inbox().execute(() -> session.send(Packets.notice(text)), gamePool));
   }

   /** Đèn đã trôi trên mặt nước: trừ 1 đèn, quà, +5 điểm. Luồng session. */
   private void lanternReleased(PlayerSession session, PlayerData d, int bagSlot, ItemData item) {
      if (!consume(session, d, bagSlot, item)) {
         return;
      }
      long now = System.currentTimeMillis();
      List<ItemData> gifts = GiftBoxes.roll(MidAutumn.LANTERN_BOX, true, d.gender, now,
            java.util.concurrent.ThreadLocalRandom.current());
      String error;
      synchronized (d) {
         error = ItemService.give(d, gifts, now);
      }
      if (error != null) {
         session.send(Packets.notice(error + ", quà thả đèn bị mất."));
      } else {
         showReward(session, gifts);
      }
      addPoints(session, d, MidAutumn.POINTS_LANTERN);
      EventStats.LANTERNS.incrementAndGet();
   }

   /** Trừ đúng chồng đã dùng; ưu tiên ô ban đầu, rồi tìm cùng vật phẩm nếu nó được chuyển ô trong lúc chờ. */
   private boolean consume(PlayerSession session, PlayerData d, int bagSlot, ItemData usedItem) {
      long now = System.currentTimeMillis();
      boolean ok;
      synchronized (d) {
         int sourceSlot = bagSlot;
         if (d.bag.get(sourceSlot) != usedItem) {
            sourceSlot = -1;
            for (int slot = 0; slot < d.bag.size(); slot++) {
               if (d.bag.get(slot) == usedItem) {
                  sourceSlot = slot;
                  break;
               }
            }
         }
         ItemData selected = sourceSlot < 0 ? null : d.bag.get(sourceSlot);
         ok = selected == usedItem && selected.templateId == usedItem.templateId && !selected.isExpired(now)
               && selected.quantity > 0;
         if (ok) {
            if (selected.quantity == 1) {
               d.bag.take(sourceSlot);
            } else {
               selected.quantity--;
            }
         }
      }
      if (!ok) {
         session.send(Packets.notice("Không còn " + itemName(usedItem.templateId) + " ở vật phẩm đã chọn."));
         return false;
      }
      items.syncBagAndCurrency(session);
      return true;
   }

   // ================================================================== móc từ hệ thống khác

   /** Tinh anh ({@code leader} = false) / Thủ lĩnh bị hạ: nguyên liệu bánh KHÔNG khoá; rỗng khi tắt sự kiện. */
   public static List<ItemData> eliteMaterials(boolean leader, RandomGenerator random) {
      if (!GameEvents.midAutumn()) {
         return List.of();
      }
      int[] qty = leader ? MidAutumn.LEADER_QTY : MidAutumn.ELITE_QTY;
      List<ItemData> out = new ArrayList<>();
      for (int templateId : MidAutumn.ELITE_MATERIALS) {
         ItemData item = ItemData.of(templateId, random.nextInt(qty[0], qty[1] + 1));
         item.locked = false;
         out.add(item);
      }
      return out;
   }

   /** Quà mỗi củ cà rốt Ngọc thố ăn: Trứng muối + Sữa tươi (10% không khoá) + 100-5.000 vàng khoá. */
   public static List<ItemData> rabbitRewards(RandomGenerator random) {
      List<ItemData> out = new ArrayList<>();
      for (int templateId : new int[] {MidAutumn.SALTED_EGG, MidAutumn.MILK}) {
         ItemData item = ItemData.of(templateId, 1);
         item.locked = random.nextInt(100) >= MidAutumn.RABBIT_UNLOCK_PERCENT;
         out.add(item);
      }
      ItemData gold = ItemData.of(ItemService.GOLD_TEMPLATE_ID,
            random.nextInt(MidAutumn.RABBIT_GOLD[0], MidAutumn.RABBIT_GOLD[1] + 1));
      gold.locked = true;
      out.add(gold);
      return out;
   }

   /** Ngọc thố vừa ăn cà rốt của người này: +3 điểm. Luồng khu (cộng điểm có khoá riêng). */
   public static void rabbitFed(PlayerSession session, PlayerData d) {
      addPoints(session, d, MidAutumn.POINTS_FEED);
      EventStats.CARROTS_EATEN.incrementAndGet();
   }

   /** Hái cà rốt (cây 11): +1 điểm. Luồng session. */
   public static void plantHarvested(PlayerSession session, PlayerData d, int plantTemplateId) {
      if (plantTemplateId == MidAutumn.CARROT_PLANT && GameEvents.midAutumn()) {
         addPoints(session, d, MidAutumn.POINTS_CARROT_PICK);
         EventStats.CARROTS_PICKED.incrementAndGet();
      }
   }

   /** Boss thế giới bị hạ: Cổ thần Dạ xoa -> +25 điểm cho người kết liễu (nếu online). Mọi luồng. */
   public static void bossKilled(int bossId, int killerId) {
      MidAutumnService s = instance;
      if (s == null || bossId != MidAutumn.BOSS_ID || killerId == 0) {
         return;
      }
      EventStats.BOSS_KILLS.incrementAndGet();
      com.tlnt.server.world.WorldPlayer p = s.world.online(killerId);
      if (p != null) {
         addPoints(p.session(), p.data(), MidAutumn.POINTS_BOSS);
      }
   }

   /** Cộng điểm sự kiện + báo. Mọi luồng (RankStats có khoá riêng, session.send an toàn). */
   static void addPoints(PlayerSession session, PlayerData d, long points) {
      if (d == null || points <= 0) {
         return;
      }
      d.rankStats.addEvent(points);
      EventStats.POINTS_GIVEN.addAndGet(points);
      if (session != null) {
         session.send(Packets.notice("+" + points + " điểm sự kiện Trung thu (tổng " + d.rankStats.eventPoints + ")."));
      }
   }

   private void showReward(PlayerSession session, List<ItemData> gifts) {
      items.syncBagAndCurrency(session);
      session.send(ItemPackets.rewardPopup(gifts));
      ItemPackets.receivedNotices(gifts).forEach(session::send);
   }

   static String itemName(int templateId) {
      var t = GameDataCache.itemTemplate(templateId);
      return t != null ? t.name() : "item #" + templateId;
   }
}
