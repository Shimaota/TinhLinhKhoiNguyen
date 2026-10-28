package com.tlnt.server.trade;

import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.social.Invites;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Giao dịch giữa 2 người (op 56 + lời mời op 32 loại {@link Packets#CONFIRM_TRADE}). Lane GAME: mỗi gói chạy trên luồng
 * session của người gửi.
 *
 * <h2>Luồng</h2>
 * <ol>
 *   <li>Menu người chơi "Giao dịch" -> op 56 sub 0 (id) -> hộp mời bên kia. Đồng ý (op 32) -> mở cửa sổ 2 bên
 *       (S→C 56/0).</li>
 *   <li>"Khoá" -> op 56 sub 1 (vàng, [ô túi, số lượng]): kiểm tra ngay trên túi của chính mình, gửi bên kia xem
 *       (S→C 56/1). Khoá rồi không sửa được.</li>
 *   <li>"Đồng ý" -> op 56 sub 2: chỉ khi CẢ 2 đã khoá (đồng ý trước khi thấy đồ bên kia = bị lừa). Bên kia thấy
 *       S→C 56/2. Bên thứ 2 đồng ý -> chốt.</li>
 *   <li>Đóng cửa sổ -> op 56 sub 3 = huỷ (S→C 56/3 đóng cửa sổ 2 bên).</li>
 * </ol>
 *
 * <h2>Vì sao không nhân bản / mất đồ</h2>
 * <ul>
 *   <li>Từ lúc mở tới lúc đóng, {@link PlayerData#trade} khác null -> {@link ItemService} chặn mọi thao tác túi đồ của
 *       cả 2 (dùng, vứt, cất rương, mặc, sắp xếp, dọn hết hạn...). Vàng chỉ giao dịch đụng tới. Gói khoá / đồng ý của
 *       mỗi người đi sau mọi thao tác trước đó của chính họ (hàng đợi tuần tự) -> lúc chốt không còn ai sửa túi.</li>
 *   <li>Chốt trong {@code synchronized (trade)} trên luồng người đồng ý sau: kiểm tra lại TẤT CẢ (còn online, cùng
 *       khu, đứng gần, còn sống, món trong ô vẫn là đúng object lúc khoá, không khoá, chưa hết hạn, đủ số lượng, đủ
 *       vàng, túi 2 bên đủ chỗ) rồi làm trên BẢN SAO túi 2 bên; hỏng bất kỳ bước nào thì huỷ, không đổi gì. Được hết
 *       mới thay túi + vàng 2 bên cùng lúc.</li>
 *   <li>Thoát game: hook {@link #onLogout} huỷ giao dịch (cùng khoá) TRƯỚC khi chụp dữ liệu lưu -> hoặc đã chốt xong,
 *       hoặc chưa chốt gì. Chốt xong lưu DB ngay cả 2 người (mỗi người trên luồng của mình) để sập server cũng không
 *       ai giữ được cả đồ cũ lẫn đồ mới.</li>
 * </ul>
 */
public final class TradeService {

   /** Tối đa số món mỗi bên (client giới hạn 12 ô). */
   static final int MAX_OFFER_ITEMS = 12;
   /** Vàng tối đa mỗi bên mỗi lần (client kẹp 50.000.000). */
   static final long MAX_OFFER_GOLD = 50_000_000L;
   /** Vàng tối đa 1 nhân vật được giữ (giao dịch làm vượt -> từ chối). */
   static final long MAX_GOLD = 999_999_999_999L;
   /** Khoảng cách tối đa giữa 2 người (pixel, cả trục x lẫn y) lúc mời / mở / khoá / chốt. */
   static final float RANGE_PX = 400f;
   private static final long INVITE_TTL_MS = 20_000;
   private static final long INVITE_COOLDOWN_MS = 3_000;
   /** Giao dịch mở quá lâu -> thao tác kế tiếp sẽ huỷ (tránh túi đồ bị đóng băng mãi). */
   private static final long MAX_TRADE_MS = 5 * 60_000;

   private final World world;
   private final ItemService items;
   private final PlayerService players;
   private final Executor gamePool;
   private final Invites invites = new Invites(INVITE_TTL_MS);
   /**
    * Mở giao dịch: kiểm tra "cả 2 chưa giao dịch" + gán {@link PlayerData#trade} phải liền 1 khối (A mời B và C cùng
    * lúc, B và C đồng ý trên 2 luồng). Mở giao dịch hiếm -> 1 khoá chung đủ nhẹ.
    */
   private final Object openLock = new Object();

   public TradeService(World world, ItemService items, PlayerService players, Executor gamePool) {
      this.world = world;
      this.items = items;
      this.players = players;
      this.gamePool = gamePool;
   }

   // ================================================================== mời / mở

   /** Op 56 sub 0: mời {@code targetId} giao dịch. */
   public void invite(PlayerSession session, int targetId) {
      WorldPlayer me = world.online(session);
      if (me == null) {
         return;
      }
      PlayerData d = me.data();
      long now = System.currentTimeMillis();
      if (d.trade != null) {
         me.send(Packets.notice("Bạn đang giao dịch."));
         return;
      }
      if (d.fishing != null) {
         me.send(Packets.notice("Đang câu cá, không thể giao dịch."));
         return;
      }
      UpgradeService.cancel(session, d); // huỷ lựa chọn trong lò rèn
      if (now - d.lastInviteAt < INVITE_COOLDOWN_MS) {
         me.send(Packets.notice("Thao tác quá nhanh."));
         return;
      }
      if (targetId == me.id()) {
         return;
      }
      WorldPlayer target = world.online(targetId);
      String error = target == null ? "Người chơi không còn trực tuyến."
            : target.data().trade != null ? "Đối phương đang giao dịch với người khác."
            : target.data().fishing != null ? "Đối phương đang câu cá."
            : checkPair(me, target);
      if (error != null) {
         me.send(Packets.notice(error));
         return;
      }
      d.lastInviteAt = now;
      invites.put(targetId, me.id(), now);
      target.send(Packets.invitation(Packets.CONFIRM_TRADE, "Giao dịch", d.name + " muốn giao dịch với bạn."));
      me.send(Packets.notice("Đã gửi lời mời giao dịch tới " + target.data().name + "."));
   }

   /** Op 32 loại {@link Packets#CONFIRM_TRADE}: người được mời bấm "Đồng ý" -> mở cửa sổ giao dịch 2 bên. */
   public void acceptInvite(PlayerSession session) {
      WorldPlayer me = world.online(session);
      if (me == null) {
         return;
      }
      int fromId = invites.take(me.id(), System.currentTimeMillis());
      WorldPlayer inviter = fromId == 0 ? null : world.online(fromId);
      if (inviter == null) {
         me.send(Packets.notice(fromId == 0 ? "Lời mời giao dịch đã hết hạn." : "Người mời không còn trực tuyến."));
         return;
      }
      UpgradeService.cancel(session, me.data()); // huỷ lựa chọn trong lò rèn
      String error = me.data().fishing != null ? "Đang câu cá, không thể giao dịch."
            : inviter.data().fishing != null ? "Đối phương đang câu cá."
            : checkPair(me, inviter);
      if (error != null) {
         me.send(Packets.notice(error));
         return;
      }
      Trade trade;
      synchronized (openLock) {
         if (me.data().trade != null || inviter.data().trade != null) {
            me.send(Packets.notice("Một trong hai người đang giao dịch."));
            return;
         }
         trade = new Trade(inviter, me, System.currentTimeMillis());
         inviter.data().trade = trade;
         me.data().trade = trade;
      }
      inviter.send(TradePackets.open(me.data()));
      me.send(TradePackets.open(inviter.data()));
      Log.debug("Mo giao dich #%d '%s' <-> #%d '%s'", inviter.id(), inviter.data().name, me.id(), me.data().name);
   }

   // ================================================================== khoá / đồng ý / huỷ

   /**
    * Op 56 sub 1 "Khoá": {@code gold} + các ô túi {@code slots} với số lượng {@code quantities}. Kiểm tra trên túi của
    * chính mình (đang bị đóng băng). Sai bất kỳ gì -> huỷ cả giao dịch (client đã tự đánh dấu "đã khoá", không mở lại
    * được).
    */
   public void lock(PlayerSession session, long gold, int[] slots, int[] quantities) {
      PlayerData d = session.getPlayer();
      Trade trade = d == null ? null : d.trade;
      if (trade == null) {
         return;
      }
      synchronized (trade) {
         Trade.Side me = trade.side(d);
         if (trade.closed || me == null || me.offer != null) {
            return;
         }
         String error = checkActive(trade);
         if (error == null) {
            error = checkOffer(d, gold, slots, quantities, System.currentTimeMillis());
         }
         if (error != null) {
            close(trade, error);
            return;
         }
         ItemData[] originals = new ItemData[slots.length];
         for (int i = 0; i < slots.length; i++) {
            originals[i] = d.bag.get(slots[i]);
         }
         me.offer = new Trade.Offer(gold, slots.clone(), quantities.clone(), originals);
         trade.other(me).player.send(TradePackets.partnerOffer(me.offer));
      }
   }

   /** Op 56 sub 2 "Đồng ý". Bên thứ 2 đồng ý -> chốt ngay trên luồng này. */
   public void accept(PlayerSession session) {
      PlayerData d = session.getPlayer();
      Trade trade = d == null ? null : d.trade;
      if (trade == null) {
         return;
      }
      synchronized (trade) {
         Trade.Side me = trade.side(d);
         if (trade.closed || me == null || me.accepted) {
            return;
         }
         Trade.Side other = trade.other(me);
         if (me.offer == null) {
            me.player.send(Packets.notice("Hãy khoá vật phẩm trước khi đồng ý."));
            return;
         }
         if (other.offer == null) {
            me.player.send(Packets.notice("Chờ đối phương khoá vật phẩm rồi bấm Đồng ý."));
            return;
         }
         me.accepted = true;
         other.player.send(TradePackets.partnerAccepted());
         if (other.accepted) {
            commit(trade);
         }
      }
   }

   /** Op 56 sub 3: đóng cửa sổ giao dịch = huỷ. */
   public void cancel(PlayerSession session) {
      PlayerData d = session.getPlayer();
      Trade trade = d == null ? null : d.trade;
      if (trade != null) {
         close(trade, d.name + " đã huỷ giao dịch.");
      }
   }

   /** Hook đăng xuất ({@code PlayerService.onLogout}): huỷ giao dịch + bỏ lời mời, TRƯỚC khi chụp dữ liệu lưu. */
   public void onLogout(PlayerData d) {
      invites.remove(d.getId());
      Trade trade = d.trade;
      if (trade != null) {
         close(trade, d.name + " đã rời game, giao dịch bị huỷ.");
      }
   }

   // ================================================================== chốt

   /** Đang giữ khoá {@code trade}, cả 2 đã khoá + đồng ý. */
   private void commit(Trade trade) {
      PlayerData a = trade.a.data();
      PlayerData b = trade.b.data();
      String error = checkActive(trade);
      if (error == null) {
         error = exchange(a, trade.a.offer, b, trade.b.offer, System.currentTimeMillis());
      }
      if (error != null) {
         close(trade, error);
         return;
      }
      // Túi + vàng 2 bên đã đổi xong -> mới gỡ khoá đóng băng (volatile: luồng kia thấy túi mới khi thấy null).
      trade.closed = true;
      a.trade = null;
      b.trade = null;
      for (Trade.Side side : new Trade.Side[] {trade.a, trade.b}) {
         WorldPlayer p = side.player;
         p.send(TradePackets.close());
         p.send(Packets.notice("Giao dịch thành công."));
         // Gửi lại túi + lưu DB trên luồng session của chính người đó (không đụng dữ liệu của họ từ luồng này nữa).
         PlayerSession s = p.session();
         s.inbox().execute(() -> {
            items.afterTrade(s);
            if (s.getPlayer() == side.data()) {
               players.saveAsync(side.data(), false);
            }
         }, gamePool);
      }
      Log.debug("Giao dich xong #%d '%s' (%d vang, %d mon) <-> #%d '%s' (%d vang, %d mon)", a.getId(), a.name,
            trade.a.offer.gold(), trade.a.offer.size(), b.getId(), b.name, trade.b.offer.gold(), trade.b.offer.size());
   }

   /**
    * Đổi đồ + vàng giữa {@code a} và {@code b}. Làm trên bản sao túi 2 bên; hỏng bất kỳ bước nào -> trả câu báo lỗi,
    * KHÔNG đổi gì. Được hết -> thay túi + vàng cả 2 rồi trả null. Hàm thuần (không gửi gói) để test.
    */
   static String exchange(PlayerData a, Trade.Offer offerA, PlayerData b, Trade.Offer offerB, long now) {
      String error = verifyOffer(a, offerA, now);
      if (error == null) {
         error = verifyOffer(b, offerB, now);
      }
      if (error != null) {
         return error;
      }
      long goldA = a.gold - offerA.gold() + offerB.gold();
      long goldB = b.gold - offerB.gold() + offerA.gold();
      if (goldA > MAX_GOLD || goldB > MAX_GOLD) {
         return "Số vàng sau giao dịch vượt quá giới hạn.";
      }
      // túi gốc không còn ô trống mà vẫn nhận đồ -> chặn luôn (kể cả món cộng dồn được)
      if (offerB.size() > 0 && a.bag.firstEmpty() < 0) {
         return "Hành trang của " + a.name + " đã đầy.";
      }
      if (offerA.size() > 0 && b.bag.firstEmpty() < 0) {
         return "Hành trang của " + b.name + " đã đầy.";
      }
      ItemContainer bagA = a.bag.deepCopy();
      ItemContainer bagB = b.bag.deepCopy();
      List<ItemData> fromA = takeOffered(bagA, offerA);
      List<ItemData> fromB = takeOffered(bagB, offerB);
      for (ItemData item : fromB) {
         if (bagA.add(item) < 0) {
            return "Hành trang của " + a.name + " không đủ chỗ.";
         }
      }
      for (ItemData item : fromA) {
         if (bagB.add(item) < 0) {
            return "Hành trang của " + b.name + " không đủ chỗ.";
         }
      }
      a.bag = bagA;
      b.bag = bagB;
      a.gold = goldA;
      b.gold = goldB;
      return null;
   }

   /** Món + vàng lúc khoá vẫn còn nguyên trong túi (cùng object, chưa khoá, chưa hết hạn, đủ số lượng) + đủ vàng. */
   private static String verifyOffer(PlayerData d, Trade.Offer offer, long now) {
      if (offer.gold() < 0 || offer.gold() > d.gold) {
         return d.name + " không đủ vàng.";
      }
      for (int i = 0; i < offer.size(); i++) {
         ItemData item = d.bag.get(offer.slots()[i]);
         if (item == null || item != offer.originals()[i] || item.locked || item.isExpired(now)
               || item.quantity < offer.quantities()[i]) {
            return "Vật phẩm giao dịch của " + d.name + " đã thay đổi.";
         }
      }
      return null;
   }

   /** Lấy các món đã khoá ra khỏi (bản sao) túi: đủ cả chồng thì lấy nguyên ô, không thì tách ra. */
   private static List<ItemData> takeOffered(ItemContainer bag, Trade.Offer offer) {
      List<ItemData> taken = new ArrayList<>(offer.size());
      for (int i = 0; i < offer.size(); i++) {
         int slot = offer.slots()[i];
         int quantity = offer.quantities()[i];
         ItemData item = bag.get(slot);
         if (quantity >= item.quantity) {
            taken.add(bag.take(slot));
         } else {
            ItemData part = item.copy();
            part.quantity = quantity;
            item.quantity -= quantity;
            taken.add(part);
         }
      }
      return taken;
   }

   // ================================================================== kiểm tra

   /** Nội dung "Khoá" hợp lệ trên túi của {@code d}: số món, ô trùng, món khoá / hết hạn, số lượng, vàng. */
   static String checkOffer(PlayerData d, long gold, int[] slots, int[] quantities, long now) {
      if (slots.length != quantities.length || slots.length > MAX_OFFER_ITEMS) {
         return "Tối đa " + MAX_OFFER_ITEMS + " vật phẩm mỗi lần giao dịch.";
      }
      if (gold < 0 || gold > MAX_OFFER_GOLD) {
         return "Số vàng giao dịch không hợp lệ.";
      }
      if (gold > d.gold) {
         return d.name + " không đủ vàng.";
      }
      for (int i = 0; i < slots.length; i++) {
         for (int k = 0; k < i; k++) {
            if (slots[k] == slots[i]) {
               return "Vật phẩm giao dịch không hợp lệ.";
            }
         }
         ItemData item = d.bag.get(slots[i]);
         if (item == null || item.template() == null || quantities[i] <= 0 || quantities[i] > item.quantity) {
            return "Vật phẩm giao dịch không hợp lệ.";
         }
         if (item.locked) {
            return "Không thể giao dịch vật phẩm đã khoá.";
         }
         if (item.isExpired(now)) {
            return "Vật phẩm đã hết hạn sử dụng.";
         }
      }
      return null;
   }

   /** Giao dịch còn hợp lệ: chưa quá lâu + 2 người vẫn online / cùng khu / đứng gần / còn sống. */
   private String checkActive(Trade trade) {
      if (System.currentTimeMillis() - trade.openedAt > MAX_TRADE_MS) {
         return "Giao dịch quá thời gian, đã huỷ.";
      }
      WorldPlayer a = world.online(trade.a.player.id());
      WorldPlayer b = world.online(trade.b.player.id());
      if (a != trade.a.player || b != trade.b.player) {
         return "Đối phương đã rời game, giao dịch bị huỷ.";
      }
      return checkPair(a, b);
   }

   /**
    * 2 người giao dịch được với nhau lúc này không. Vị trí / khu do luồng tick ghi, ở đây đọc không khoá (lệch vài
    * frame không sao — mỗi bước đều kiểm tra lại).
    */
   private static String checkPair(WorldPlayer me, WorldPlayer other) {
      if (!me.data().inWorld || !other.data().inWorld || me.zone() == null || me.zone() != other.zone()) {
         return "Đối phương không ở cùng khu vực.";
      }
      if (me.data().hp <= 0 || other.data().hp <= 0) {
         return "Không thể giao dịch khi đã tử vong.";
      }
      if (Math.abs(me.x() - other.x()) > RANGE_PX || Math.abs(me.y() - other.y()) > RANGE_PX) {
         return "Khoảng cách quá xa, không thể giao dịch.";
      }
      return null;
   }

   /** Huỷ (nếu chưa đóng): gỡ đóng băng túi 2 bên, đóng cửa sổ 2 bên, báo lý do. */
   private static void close(Trade trade, String reason) {
      synchronized (trade) {
         if (trade.closed) {
            return;
         }
         trade.closed = true;
         for (Trade.Side side : new Trade.Side[] {trade.a, trade.b}) {
            if (side.data().trade == trade) {
               side.data().trade = null;
            }
            side.player.send(TradePackets.close());
            side.player.send(Packets.notice(reason));
         }
      }
   }
}
