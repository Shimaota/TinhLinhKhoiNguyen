package com.tlnt.server.badge;

import com.tlnt.server.farm.AppleTreeService;
import com.tlnt.server.forge.CraftService;
import com.tlnt.server.forge.ElementService;
import com.tlnt.server.forge.ForgePackets;
import com.tlnt.server.forge.RefineService;
import com.tlnt.server.forge.SoulService;
import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.shop.ShopService;
import com.tlnt.server.util.Log;

/**
 * Huy hiệu Tinh Linh (item 123 = 7 ngày, 360 = 1 ngày): "Sử dụng" cộng dồn thời hạn ({@code PlayerData.spiritBadgeUntil},
 * cột spirit_badge_until), không giới hạn. Còn hạn -> client hiện nút Tiện ích trên HUD ({@code ChatButton}, icon ui/36)
 * + đếm ngược trong cửa sổ "Tiện ích" ({@code CountdownPopup}); client tự bật thêm: dịch chuyển tới quái khi tự đánh,
 * ô "Tự thu hoạch táo" trong cài đặt.
 *
 * <pre>
 * S→C 45/0: bool còn hạn, [long ms còn lại]          -> GameState.appleEventState (tên client sai)
 * S→C 45/1: byte n, n × str tên nút                   -> mở cửa sổ Tiện ích
 * C→S 45/0: bấm nút Tiện ích ({@code sendRequestDuel} — tên sai)
 * C→S 45/1: byte vị trí nút ({@code sendDuelAction} — tên sai)
 * </pre>
 * Các nút mở tính năng TỪ XA (không cần đứng gần NPC): shop / rương / lò rèn lưu "chỗ" = {@link #REMOTE_MAP}, các chỗ
 * kiểm tra đứng gần coi {@link #remote} là hợp lệ khi huy hiệu còn hạn. Hết hạn bấm nút -> {@link #NEED_BADGE}.
 * Luồng session.
 */
public final class SpiritBadgeService {

   public static final int BADGE_7_DAYS = 123;
   public static final int BADGE_1_DAY = 360;
   /** "Map" của shop / rương / lò mở từ huy hiệu (không phải map thật). */
   public static final int REMOTE_MAP = -2;
   static final long DAY_MS = 86_400_000L;
   public static final String NEED_BADGE = "Cần có Huy hiệu tinh linh ( Mua tại cửa hàng Tư tế ) để sử dụng !";

   /** Nút trong cửa sổ Tiện ích (thứ tự = byte client gửi). */
   enum Option {
      OFFLINE_TRAINING("Luyện tập Offline"),
      WORLD_BOSS("Boss thế giới"),
      CHEST("Rương đồ"),
      GROCERY("Cửa hàng tạp hóa"),
      BLACKSMITH("Cửa hàng thợ rèn"),
      REFINE("Tinh luyện đá"),
      UPGRADE("Nâng cấp trang bị"),
      SOUL("Khảm bùa linh hồn"),
      ELEMENT("Khảm đá nguyên tố"),
      CRAFT("Rèn linh thạch"),
      TRANSMUTE("Chuyển hóa trang bị"),
      PRIEST_SHOP("Cửa hàng tư tế"),
      PHARMACY("Cửa hàng dược phẩm"),
      APPLE("Thu hoạch táo"),
      FISH_EXCHANGE("Đổi cá");

      final String label;

      Option(String label) {
         this.label = label;
      }
   }

   /** Shop id (tlnt_data.shop) — như NPC: 1 Selami, 4 Morgi, 3 Buraen (tư thế), 2 Nimora, 5 Tarae. */
   static final int SHOP_GROCERY = 1;
   static final int SHOP_PHARMACY = 2;
   static final int SHOP_PRIEST = 3;
   static final int SHOP_BLACKSMITH = 4;
   static final int SHOP_FISH = 5;

   private final ShopService shops;
   private final AppleTreeService apples;

   public SpiritBadgeService(ShopService shops, AppleTreeService apples) {
      this.shops = shops;
      this.apples = apples;
   }

   // ================================================================== trạng thái

   public static boolean active(PlayerData d, long now) {
      return d.spiritBadgeUntil > now;
   }

   /** Shop / rương / lò mở từ huy hiệu ({@code mapId} = {@link #REMOTE_MAP}) và huy hiệu còn hạn. */
   public static boolean remote(PlayerData d, int mapId) {
      return mapId == REMOTE_MAP && active(d, System.currentTimeMillis());
   }

   /** S→C 45/0. */
   static Message status(PlayerData d, long now) {
      Message msg = new Message(com.tlnt.server.net.Opcode.UTILITY);
      msg.writeByte(0);
      boolean on = active(d, now);
      msg.writeBoolean(on);
      if (on) {
         msg.writeLong(d.spiritBadgeUntil - now);
      }
      return msg;
   }

   private static void sendStatus(PlayerSession session, PlayerData d, long now) {
      d.spiritBadgeShown = active(d, now);
      session.send(status(d, now));
   }

   /** Vào game: gửi trạng thái (còn hạn thì client hiện nút). */
   public void onLogin(PlayerSession session, PlayerData d) {
      if (d.spiritBadgeUntil > 0) {
         sendStatus(session, d, System.currentTimeMillis());
      }
   }

   /** Mỗi 5 s ({@code ItemBuffs.regen}): client đang hiện nút mà đã hết hạn -> tắt. */
   public static void tick(PlayerSession session, PlayerData d, long now) {
      if (d.spiritBadgeShown && !active(d, now)) {
         sendStatus(session, d, now);
         session.send(Packets.notice("Huy hiệu Tinh Linh đã hết hạn."));
      }
   }

   /** Item "Sử dụng" ({@code UsableItems}): cộng dồn {@code days} × số món. */
   public static void use(PlayerSession session, PlayerData d, int bagSlot, ItemData item, int quantity, int days,
         ItemService items, PlayerService players) {
      int count = Math.max(1, Math.min(quantity, item.quantity));
      long now = System.currentTimeMillis();
      synchronized (d) {
         item.quantity -= count;
         if (item.quantity <= 0) {
            d.bag.take(bagSlot);
         }
         d.spiritBadgeUntil = Math.max(now, d.spiritBadgeUntil) + count * days * DAY_MS;
      }
      items.syncBagAndCurrency(session);
      players.saveAsync(d, false);
      sendStatus(session, d, now);
      session.send(Packets.notice("Huy hiệu Tinh Linh: +" + count * days + " ngày, còn " + left(d.spiritBadgeUntil - now)
            + "."));
   }

   static String left(long ms) {
      long minutes = Math.max(0, ms / 60_000);
      long days = minutes / 1440;
      long hours = minutes % 1440 / 60;
      return (days > 0 ? days + " ngày " : "") + hours + " giờ " + minutes % 60 + " phút";
   }

   // ================================================================== C→S 45

   /** 45/0: bấm nút Tiện ích. */
   public void openMenu(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      if (!active(d, now)) {
         session.send(Packets.notice(NEED_BADGE));
         sendStatus(session, d, now);
         return;
      }
      Message msg = new Message(com.tlnt.server.net.Opcode.UTILITY);
      msg.writeByte(1);
      Option[] all = Option.values();
      msg.writeByte(all.length);
      for (Option o : all) {
         msg.writeString(o.label);
      }
      session.send(msg);
   }

   /** 45/1: bấm 1 nút trong cửa sổ Tiện ích. */
   public void choose(PlayerSession session, int index) {
      PlayerData d = session.getPlayer();
      Option[] all = Option.values();
      if (d == null || !d.inWorld || index < 0 || index >= all.length) {
         return;
      }
      long now = System.currentTimeMillis();
      if (!active(d, now)) {
         session.send(Packets.notice(NEED_BADGE));
         sendStatus(session, d, now);
         return;
      }
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể dùng tiện ích."));
         return;
      }
      Option option = all[index];
      Log.debug("Huy hieu: '%s' chon %s", d.name, option);
      switch (option) {
         case OFFLINE_TRAINING -> session.send(Packets.openAutoSettings()); // tab tự đánh + nút "Luyện tập Offline" (op 77)
         case WORLD_BOSS -> session.send(com.tlnt.server.worldboss.WorldBossList.openPacket()); // bảng Boss thế giới (op 100/2)
         case TRANSMUTE -> session.send(Packets.notice(option.label + " sắp ra mắt."));
         case CHEST -> openChest(session, d);
         case GROCERY -> shops.openRemote(session, d, SHOP_GROCERY, REMOTE_MAP);
         case BLACKSMITH -> shops.openRemote(session, d, SHOP_BLACKSMITH, REMOTE_MAP);
         case PRIEST_SHOP -> shops.openRemote(session, d, SHOP_PRIEST, REMOTE_MAP);
         case PHARMACY -> shops.openRemote(session, d, SHOP_PHARMACY, REMOTE_MAP);
         case FISH_EXCHANGE -> shops.openRemote(session, d, SHOP_FISH, REMOTE_MAP);
         case REFINE -> {
            RefineService.open(d);
            session.send(ForgePackets.open(ForgePackets.MODE_REFINE));
            session.send(RefineService.statePacket(d));
         }
         case UPGRADE -> {
            UpgradeService.open(d, REMOTE_MAP, 0, 0);
            session.send(ForgePackets.open(ForgePackets.MODE_UPGRADE));
            session.send(UpgradeService.statePacket(d));
         }
         case SOUL -> {
            SoulService.open(d, REMOTE_MAP, 0, 0);
            session.send(ForgePackets.open(ForgePackets.MODE_SOUL));
            session.send(SoulService.statePacket(d));
         }
         case ELEMENT -> {
            ElementService.open(d, REMOTE_MAP, 0, 0);
            session.send(ForgePackets.open(ForgePackets.MODE_ELEMENT));
            session.send(ElementService.statePacket(d));
         }
         case CRAFT -> {
            CraftService.open(d, REMOTE_MAP, 0, 0);
            session.send(ForgePackets.open(ForgePackets.MODE_CRAFT));
            session.send(CraftService.statePacket(d));
         }
         case APPLE -> apples.harvest(session, d);
         default -> {
         }
      }
   }

   /** Rương đồ từ xa (như bấm NPC Rương đồ, chỗ = {@link #REMOTE_MAP}). */
   private static void openChest(PlayerSession session, PlayerData d) {
      if (!QuestEvents.chestUnlocked(d)) { // mở từ bước "Mở rương đồ" (nhiệm vụ 3)
         session.send(Packets.notice("Bạn chưa thể mở rương"));
         return;
      }
      d.boxMapId = REMOTE_MAP;
      session.send(ItemPackets.container(ItemPackets.SUB_BOX, d.box));
      session.send(ItemPackets.openBox());
   }
}
