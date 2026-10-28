package com.tlnt.server.shop;

import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcContext;
import com.tlnt.server.npc.NpcService;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cửa hàng NPC (op 55; client cửa sổ {@code MarketSellWindow} = {@code SHOP_POPUP} — tên sai). Dữ liệu ở
 * {@code tlnt_data.shop} ({@link ShopRepository}), nạp lại mỗi phút. Lane GAME.
 *
 * <pre>
 * S→C 55/0 mở shop: int shopId, str tên, byte 1 (= mở cửa sổ), byte n tab, n×(str tên tab, str icon, int m,
 *          m×(item, int mẫu tiền tệ (1 vàng / 2 lam thạch), int giá))
 * C→S 55/0 mua ({@code sendEnhanceEquipment} — tên sai; nút "Mua" / "Mua nhiều"): int shopId, int tab, int ô, int số lần
 * </pre>
 * Mỗi nhân vật chỉ thấy tab / món hợp class + giới tính ({@link Shop#viewFor}; tab / món không ghi = mua chung); danh
 * sách đã lọc giữ trong {@code PlayerData.shopTabs} để khớp chỉ số client gửi khi mua.
 * Chỉ mua được shop vừa mở từ NPC và còn đứng gần NPC đó. Trả tiền + cho đồ dưới {@code synchronized (player)}, cho đồ
 * trên bản sao túi (thiếu chỗ thì không trừ tiền).
 */
public final class ShopService {

   /** Số lần mua tối đa mỗi lần bấm "Mua nhiều". */
   public static final int MAX_BUY = 999;
   private static final long BUY_COOLDOWN_MS = 300;

   private static volatile Map<Integer, Shop> shops = Map.of();

   private final ShopRepository repository;
   private final ItemService items;
   private final World world;
   private volatile TribeShop tribeShop;

   public ShopService(ShopRepository repository, ItemService items, World world) {
      this.repository = repository;
      this.items = items;
      this.world = world;
      reload();
   }

   /** Đọc lại bảng shop (luồng blocking). Lỗi thì giữ bản cũ. */
   public void reload() {
      Map<Integer, Shop> loaded = repository.loadAll();
      if (loaded != null) {
         shops = Map.copyOf(loaded);
      }
   }

   /**
    * Món bán đầu tiên có mẫu {@code templateId} trong mọi shop (shop id nhỏ trước) — hộp quà tạo item "chỉ số random
    * giống shop" ({@code item/GiftBoxes}). null = không shop nào bán.
    */
   public static RewardItem goods(int templateId) {
      Map<Integer, Shop> current = shops;
      return current.keySet().stream().sorted().map(current::get)
            .flatMap(shop -> shop.tabs().stream()).flatMap(tab -> tab.goods().stream())
            .map(Shop.Goods::item).filter(item -> item.templateId() == templateId)
            .findFirst().orElse(null);
   }

   /** NPC mở shop {@code shopId}: nhớ shop + vị trí NPC (mua chỉ khi còn đứng gần), gửi danh sách hàng. */
   public void open(NpcContext ctx, int shopId) {
      Shop shop = shops.get(shopId);
      if (shop == null) {
         ctx.send(Packets.notice("Cửa hàng đang đóng cửa."));
         return;
      }
      PlayerData d = ctx.data();
      List<Shop.Tab> tabs = shop.viewFor(d.classId, d.gender);
      if (tabs.isEmpty()) {
         ctx.send(Packets.notice("Cửa hàng không có hàng dành cho bạn."));
         return;
      }
      d.shopId = shopId;
      d.shopTabs = tabs;
      d.shopMapId = ctx.mapId();
      d.shopX = ctx.spawn().x();
      d.shopY = ctx.spawn().y();
      ctx.send(packet(shop, tabs));
   }

   /**
    * Mở shop {@code shopId} KHÔNG qua NPC (Huy hiệu Tinh Linh): chỗ = {@code mapId} (= {@code SpiritBadgeService.REMOTE_MAP}),
    * mua được khi huy hiệu còn hạn ({@link #buy}).
    */
   public void openRemote(PlayerSession session, PlayerData d, int shopId, int mapId) {
      Shop shop = shops.get(shopId);
      if (shop == null) {
         session.send(Packets.notice("Cửa hàng đang đóng cửa."));
         return;
      }
      List<Shop.Tab> tabs = shop.viewFor(d.classId, d.gender);
      if (tabs.isEmpty()) {
         session.send(Packets.notice("Cửa hàng không có hàng dành cho bạn."));
         return;
      }
      d.shopId = shopId;
      d.shopTabs = tabs;
      d.shopMapId = mapId;
      session.send(packet(shop, tabs));
   }

   /** Cửa hàng Bộ lạc (gắn sau khi tạo TribeService), null = chưa có. */
   public void setTribeShop(TribeShop tribeShop) {
      this.tribeShop = tribeShop;
   }

   /**
    * C→S 55/1 ({@code sendRequestEnhance} — tên sai): mở tab "Cửa hàng" của cửa sổ Bộ lạc -> gửi shop
    * {@code tribe.shopId} với byte kiểu 2 (client không mở cửa sổ shop, tab Bộ lạc tự hiện tab đầu). Không cần đứng
    * gần NPC.
    */
   public void openTribe(PlayerSession session) {
      PlayerData d = session.getPlayer();
      TribeShop tribe = tribeShop;
      Shop shop = tribe == null ? null : shops.get(tribe.shopId());
      if (d == null || !d.inWorld || shop == null || !tribe.isMember(d.getId())) {
         return;
      }
      List<Shop.Tab> tabs = shop.viewFor(d.classId, d.gender);
      d.shopId = shop.id();
      d.shopTabs = tabs;
      session.send(packet(shop, tabs, SHOP_TYPE_TRIBE));
   }

   /** C→S 55/0. */
   public void buy(PlayerSession session, int shopId, int tabIndex, int slot, int times) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastShopBuyAt < BUY_COOLDOWN_MS) {
         session.send(Packets.notice("Thao tác quá nhanh."));
         return;
      }
      d.lastShopBuyAt = now;
      if (shopId == com.tlnt.server.event.MidAutumn.SHOP && !com.tlnt.server.event.GameEvents.midAutumn()) {
         session.send(Packets.notice("Lễ hội Trăng rằm đã kết thúc, cửa hàng Trung thu đã đóng."));
         return;
      }
      List<Shop.Tab> tabs = d.shopTabs;
      Shop.Goods goods = !shops.containsKey(shopId) || shopId != d.shopId || tabIndex < 0 || tabIndex >= tabs.size()
            ? null : get(tabs.get(tabIndex).goods(), slot);
      if (goods == null) {
         session.send(Packets.notice("Vật phẩm không còn bán."));
         return;
      }
      TribeShop tribe = tribeShop;
      if (tribe != null && shopId == tribe.shopId()) {
         if (times <= 0 || times > MAX_BUY) {
            session.send(Packets.notice("Số lượng mua từ 1 đến " + MAX_BUY + "."));
            return;
         }
         tribe.buy(session, goods.currency(), (long) goods.price() * times, create(goods, times, now),
               name(goods) + (times > 1 ? " x" + times : ""));
         return;
      }
      WorldPlayer p = world.online(session);
      if (!com.tlnt.server.badge.SpiritBadgeService.remote(d, d.shopMapId) // mở từ Huy hiệu Tinh Linh: không cần đứng gần
            && (p == null || p.zone() == null || p.zone().map().template().id() != d.shopMapId
            || !NpcService.near(p, d.shopX, d.shopY))) {
         session.send(Packets.notice("Hãy đứng gần người bán."));
         return;
      }
      if (times <= 0 || times > MAX_BUY) {
         session.send(Packets.notice("Số lượng mua từ 1 đến " + MAX_BUY + "."));
         return;
      }
      long cost = (long) goods.price() * times;
      List<ItemData> bought = create(goods, times, now);
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể mua."));
            return;
         }
         if (d.fishing != null) {
            session.send(Packets.notice("Đang câu cá, không thể mua."));
            return;
         }
         UpgradeService.cancel(session, d); // huỷ lựa chọn trong lò rèn
         if (goods.currency() == Shop.CUR_ITEM) {
            payWithItem(session, d, goods, bought, times, cost, now);
            return;
         }
         boolean diamond = goods.currency() == Shop.CUR_DIAMOND;
         if (diamond && cost > d.diamond) {
            session.send(Packets.notice("Không đủ " + format(cost) + " lam thạch."));
            return;
         }
         long fromLocked = goods.currency() == Shop.CUR_LOCKED_GOLD ? Math.min(d.lockedGold, cost) : 0;
         long fromGold = diamond ? 0 : cost - fromLocked;
         if (fromGold > d.gold) {
            session.send(Packets.notice("Không đủ " + format(cost) + " vàng."));
            return;
         }
         String error = ItemService.give(d, bought, now); // bản sao túi: thiếu chỗ thì không đổi gì
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
         if (diamond) {
            d.diamond -= cost;
         }
         d.lockedGold -= fromLocked;
         d.gold -= fromGold;
      }
      items.syncBagAndCurrency(session);
      session.send(Packets.notice("Mua thành công " + name(goods) + (times > 1 ? " x" + times : "") + "."));
      QuestEvents.fire(session, StepKind.BUY_SHOP, shopId);
      com.tlnt.server.daily.DailyEvents.bought(session, bought); // NV hằng ngày "Mua Đùi Gà"
      Log.debug("Shop #%d: '%s' mua mon %d x%d gia %d", shopId, d.name, goods.item().templateId(), times, cost);
   }

   /**
    * Trả bằng vật phẩm (gọi dưới {@code synchronized (d)}): trừ {@code cost} món {@code payItem} (khoá trước) rồi cho
    * hàng, cả hai trên 1 bản sao túi -> thiếu chỗ thì không mất gì. Có dùng món khoá thì hàng mua ra khoá hết.
    */
   private void payWithItem(PlayerSession session, PlayerData d, Shop.Goods goods, List<ItemData> bought, int times,
         long cost, long now) {
      ItemContainer bag = d.bag.deepCopy();
      if (ItemService.count(bag, goods.payItem(), now) < cost) {
         session.send(Packets.notice("Không đủ " + format(cost) + " " + itemName(goods.payItem()) + "."));
         return;
      }
      if (ItemService.remove(bag, goods.payItem(), cost, now)) {
         for (ItemData item : bought) {
            item.locked = true;
         }
      }
      String error = ItemService.give(d, bag, bought, now);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      items.syncBagAndCurrency(session);
      session.send(Packets.notice("Mua thành công " + name(goods) + (times > 1 ? " x" + times : "") + "."));
      QuestEvents.fire(session, StepKind.BUY_SHOP, d.shopId);
      com.tlnt.server.daily.DailyEvents.bought(session, bought);
      Log.debug("Shop #%d: '%s' mua mon %d, tra %d x mon %d", d.shopId, d.name, goods.item().templateId(), cost,
            goods.payItem());
   }

   /** Đồ nhận được khi mua {@code times} lần: đồ cộng dồn được gộp 1 món, đồ trang bị mỗi lần 1 món riêng. */
   static List<ItemData> create(Shop.Goods goods, int times, long now) {
      ItemData one = goods.item().create(now);
      ItemPack.ItemType type = one.type();
      List<ItemData> list = new ArrayList<>();
      if (type != null && type.equippable()) {
         for (int i = 0; i < times; i++) {
            list.add(i == 0 ? one : goods.item().create(now));
         }
      } else {
         one.quantity = (int) Math.min(Integer.MAX_VALUE, (long) one.quantity * times);
         list.add(one);
      }
      return list;
   }

   private static Message packet(Shop shop, List<Shop.Tab> tabs) {
      return packet(shop, tabs, SHOP_TYPE_NPC);
   }

   /** Byte kiểu shop: 1 = shop NPC (client mở cửa sổ SHOP_POPUP), 2 = Cửa hàng Bộ lạc (tab trong cửa sổ Bộ lạc). */
   private static final int SHOP_TYPE_NPC = 1;
   private static final int SHOP_TYPE_TRIBE = 2;

   private static Message packet(Shop shop, List<Shop.Tab> tabs, int type) {
      long now = System.currentTimeMillis();
      Message msg = new Message(Opcode.SHOP);
      msg.writeByte(0);
      msg.writeInt(shop.id());
      msg.writeString(shop.name());
      msg.writeByte(type);
      msg.writeByte(tabs.size());
      for (Shop.Tab tab : tabs) {
         msg.writeString(tab.name());
         msg.writeString(tab.icon());
         msg.writeInt(tab.goods().size());
         for (Shop.Goods goods : tab.goods()) {
            ItemPackets.writePreview(msg, goods.item(), now); // option khoảng hiện "min-max", mua mới random
            msg.writeInt(goods.currencyTemplateId());
            msg.writeInt(goods.price());
         }
      }
      return msg;
   }

   private static Shop.Goods get(List<Shop.Goods> list, int slot) {
      return slot >= 0 && slot < list.size() ? list.get(slot) : null;
   }

   private static String name(Shop.Goods goods) {
      return itemName(goods.item().templateId());
   }

   private static String itemName(int templateId) {
      ItemPack.ItemTemplate t = com.tlnt.server.data.GameDataCache.itemTemplate(templateId);
      return t != null ? t.name() : "vật phẩm";
   }

   private static String format(long value) {
      return String.format(Locale.US, "%,d", value).replace(',', '.');
   }
}
