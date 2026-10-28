package com.tlnt.server.market;

import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.mail.Mail;
import com.tlnt.server.mail.MailRepository;
import com.tlnt.server.mail.MailService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.WorldPackets;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kí gửi (op 36, cửa sổ {@code MarketWindow}: tab "Ký gửi" = Bán / Đang bán, tab "Cửa hàng" = mua). Lane GAME.
 *
 * <h2>Luật</h2>
 * <ul>
 *   <li>Chỉ bán món KHÔNG khoá, chưa hết hạn (món có hạn sử dụng vẫn bán được — hạn vẫn chạy; hết hạn trong lúc treo
 *       thì không mua được, hết giờ kí gửi về thư người bán), không phải tiền tệ. Mua / bán bằng vàng KHÔNG khoá.</li>
 *   <li>Cửa hàng tối đa {@link #MAX_LISTINGS} món (cả server), mỗi người không giới hạn riêng. Đang treo dưới
 *       {@link #FREE_LISTINGS} món thì đăng miễn phí; đang treo từ {@link #FREE_LISTINGS} món trở lên thì mỗi lần
 *       đăng thêm tốn {@link #EXTRA_LISTING_DYNAMIC_POINTS} điểm hoạt động.</li>
 *   <li>Phí = {@code market_fee.percent}% giá bán (8h / 16h / 24h), trừ NGAY lúc đăng bán — bán được hay không đều
 *       mất, gỡ xuống cũng không hoàn.</li>
 *   <li>Bán được: người mua nhận món qua THƯ, người bán nhận đủ giá bán (vàng không khoá) qua THƯ. Gỡ xuống / hết hạn:
 *       món về thư người bán.</li>
 * </ul>
 *
 * <h2>Vì sao không nhân bản / mất đồ</h2>
 * Danh sách đang bán nằm trong RAM ({@link #active}), mọi thay đổi dưới 1 khoá {@link #lock}; thứ tự khoá luôn là
 * {@code synchronized (player)} -> {@link #lock}. Lượt ghi DB (lưu nhân vật + dòng kí gửi + thư) được XẾP HÀNG vào
 * db-writer ngay trong khoá -> thứ tự ghi = thứ tự trong RAM (không bao giờ "đã bán" ghi trước "đăng bán"), và túi đồ /
 * vàng + dòng kí gửi + thư luôn nằm trong 1 transaction. Đang giao dịch thì chặn (túi đóng băng).
 */
public final class MarketService {

   /** Cửa hàng kí gửi chứa tối đa chừng này món (cả server). */
   public static final int MAX_LISTINGS = 300;
   /** Mỗi người treo miễn phí chừng này món cùng lúc; món thứ 6 trở lên tốn {@link #EXTRA_LISTING_DYNAMIC_POINTS}. */
   public static final int FREE_LISTINGS = 5;
   public static final int EXTRA_LISTING_DYNAMIC_POINTS = 15;
   /** Giá tối đa (client gửi int). */
   public static final int MAX_PRICE = 2_000_000_000;
   /** Chu kỳ dọn món hết hạn (giây). */
   public static final int EXPIRE_CHECK_SECONDS = 30;
   private static final long ACTION_COOLDOWN_MS = 1_000;
   private static final Pattern HOURS = Pattern.compile("(\\d+)\\s*h", Pattern.CASE_INSENSITIVE);

   private final PlayerService players;
   private final MailService mails;
   private final ItemService items;
   private final AtomicInteger ids;
   private final Object lock = new Object();
   /** Món đang bán, cũ -> mới. Chỉ đụng trong {@link #lock}. */
   private final LinkedHashMap<Integer, Listing> active = new LinkedHashMap<>();

   public MarketService(MarketRepository repository, PlayerService players, MailService mails, ItemService items)
         throws SQLException {
      this.players = players;
      this.mails = mails;
      this.items = items;
      this.ids = new AtomicInteger(repository.maxId());
      for (Listing l : repository.loadActive()) {
         active.put(l.id(), l);
      }
      Log.info("Ki gui: %d mon dang ban", active.size());
   }

   // ================================================================== op 36 C→S

   /** Sub 0: mở cửa sổ -> gửi danh sách. */
   public void open(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d != null && d.inWorld) {
         sendList(session, d);
      }
   }

   /**
    * Sub 1 "Bán": ô túi, số lượng, giá, dòng phí ({@code market_fee}). Trừ món + phí ngay, món vào danh sách đang bán.
    */
   public void sell(PlayerSession session, int bagSlot, int quantity, int price, int feeIndex) {
      PlayerData d = ready(session);
      if (d == null) {
         return;
      }
      List<GamePack.MarketFee> fees = GameDataCache.game().marketFees();
      GamePack.MarketFee option = feeIndex >= 0 && feeIndex < fees.size() ? fees.get(feeIndex) : null;
      if (option == null) {
         session.send(Packets.notice("Thời gian bán không hợp lệ."));
         return;
      }
      if (price <= 0 || price > MAX_PRICE) {
         session.send(Packets.notice("Giá bán không hợp lệ."));
         return;
      }
      long fee = fee(price, option.percent());
      long now = System.currentTimeMillis();
      Listing listing;
      int spentDynamic;
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         ItemData item = d.bag.get(bagSlot);
         String error = ItemService.checkTransferable(item, quantity, now);
         if (error != null) {
            session.send(Packets.notice(item != null && item.locked ? "Không thể bán vật phẩm đã khoá." : error));
            return;
         }
         if (d.gold < fee) {
            session.send(Packets.notice("Không đủ " + format(fee) + " vàng phí kí gửi."));
            return;
         }
         synchronized (lock) {
            if (active.size() >= MAX_LISTINGS) {
               session.send(Packets.notice("Cửa hàng kí gửi đã đầy " + MAX_LISTINGS + " vật phẩm, hãy thử lại sau."));
               return;
            }
            int dynamicCost = countOf(d.getId()) >= FREE_LISTINGS ? EXTRA_LISTING_DYNAMIC_POINTS : 0;
            if (d.dynamicPoints < dynamicCost) {
               session.send(Packets.notice("Bạn đang treo " + FREE_LISTINGS + " vật phẩm, mỗi vật phẩm kí gửi thêm cần "
                     + EXTRA_LISTING_DYNAMIC_POINTS + " điểm hoạt động."));
               return;
            }
            ItemData sold = ItemService.takeFromBag(d, bagSlot, quantity);
            d.gold -= fee; // phí mất luôn, bán được hay không
            d.dynamicPoints -= dynamicCost;
            spentDynamic = dynamicCost;
            listing = new Listing(ids.incrementAndGet(), d.getId(), d.name, sold, price, fee, feeIndex, now,
                  now + durationMs(option));
            active.put(listing.id(), listing);
            players.saveWith(d, conn -> MarketRepository.insert(conn, listing));
         }
      }
      session.send(MarketPackets.clearSellItem());
      com.tlnt.server.achievement.AchievementEvents.add(session, com.tlnt.server.achievement.AchievementType.MARKET_SELL, 1); // thành tựu: niêm yết lên Ký gửi
      com.tlnt.server.daily.DailyEvents.add(session, com.tlnt.server.daily.DailyKind.MARKET_SELL, 0, 1); // NV hằng ngày "Bán Vật Phẩm Ký Gửi"
      items.syncBagAndCurrency(session);
      if (spentDynamic > 0) {
         session.send(WorldPackets.dynamicPoints(d.dynamicPoints));
      }
      sendList(session, d);
      session.send(Packets.notice("Kí gửi thành công"));
      Log.debug("Ki gui #%d: '%s' ban %s gia %d", listing.id(), d.name, describe(listing.item()), price);
   }

   /** Sub 2 "Mua" (tab "Cửa hàng"): trừ vàng người mua; món -> thư người mua, tiền -> thư người bán. */
   public void buy(PlayerSession session, int listingId) {
      PlayerData d = ready(session);
      if (d == null) {
         return;
      }
      long now = System.currentTimeMillis();
      Listing listing;
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         synchronized (lock) {
            listing = active.get(listingId);
            String error = listing == null || now >= listing.expireAt()
                  ? "Vật phẩm đã được bán hoặc không còn kí gửi."
                  : listing.item().isExpired(now) ? "Vật phẩm đã hết hạn sử dụng."
                  : listing.sellerId() == d.getId() ? "Không thể mua vật phẩm của chính mình."
                  : d.gold < listing.price() ? "Không đủ " + format(listing.price()) + " vàng."
                  : null;
            if (error != null) {
               session.send(Packets.notice(error));
               sendList(session, d);
               return;
            }
            active.remove(listingId);
            d.gold -= listing.price();
            String name = describe(listing.item());
            Mail toBuyer = mails.systemMail(d.getId(), d.name, "Kí gửi: mua thành công",
                  "Bạn đã mua " + name + " của " + listing.sellerName() + " với giá " + format(listing.price())
                        + " vàng.", List.of(listing.item()));
            ItemData money = ItemData.of(ItemService.GOLD_TEMPLATE_ID, listing.price()); // vàng KHÔNG khoá
            Mail toSeller = mails.systemMail(listing.sellerId(), listing.sellerName(), "Kí gửi: đã bán",
                  name + " đã bán cho " + d.name + " được " + format(listing.price()) + " vàng.", List.of(money));
            int buyerId = d.getId();
            players.saveWith(d, conn -> {
               MarketRepository.close(conn, listingId, Listing.SOLD, buyerId, now);
               MailRepository.insert(conn, toBuyer);
               MailRepository.insert(conn, toSeller);
            }).thenAccept(ok -> {
               if (ok) {
                  mails.deliver(toBuyer);
                  mails.deliver(toSeller);
               }
            });
         }
      }
      items.syncBagAndCurrency(session);
      sendList(session, d);
      session.send(Packets.notice("Mua thành công! Vật phẩm đã được gửi vào hòm thư."));
      Log.debug("Ki gui #%d: '%s' mua cua '%s' gia %d", listingId, d.name, listing.sellerName(), listing.price());
   }

   /** Sub 3 "Gỡ xuống" (tab "Đang bán"): món về thư người bán, phí không hoàn. */
   public void cancel(PlayerSession session, int listingId) {
      PlayerData d = ready(session);
      if (d == null) {
         return;
      }
      long now = System.currentTimeMillis();
      synchronized (lock) {
         Listing listing = active.get(listingId);
         if (listing == null || listing.sellerId() != d.getId()) {
            session.send(Packets.notice("Vật phẩm đã được bán hoặc không còn kí gửi."));
         } else {
            active.remove(listingId);
            returnToSeller(listing, Listing.CANCELLED, "Kí gửi: gỡ xuống",
                  describe(listing.item()) + " đã được gỡ khỏi kí gửi.", now);
            session.send(Packets.notice("Đã gỡ xuống, vật phẩm được gửi về hòm thư."));
         }
      }
      sendList(session, d);
   }

   /** Sub 4 "Rao bán" trên món đang bán của mình: món đang bán rồi, hết hạn thì tự về thư. */
   public void relist(PlayerSession session, int listingId) {
      session.send(Packets.notice("Vật phẩm đang được rao bán, hết hạn sẽ tự gửi về hòm thư."));
   }

   // ================================================================== admin panel

   /** Bản chụp danh sách đang bán (cũ -> mới). Mọi luồng. */
   public List<Listing> adminList() {
      synchronized (lock) {
         return new ArrayList<>(active.values());
      }
   }

   /**
    * Admin gỡ món đang bán: món về thư người bán (như người bán tự gỡ, kèm lý do). @return false nếu món đã bán / đã gỡ
    */
   public boolean adminCancel(int listingId, String reason) {
      long now = System.currentTimeMillis();
      synchronized (lock) {
         Listing listing = active.remove(listingId);
         if (listing == null) {
            return false;
         }
         String why = reason == null || reason.isBlank() ? "" : " Lý do: " + reason.trim();
         returnToSeller(listing, Listing.CANCELLED, "Kí gửi: quản trị viên gỡ xuống",
               describe(listing.item()) + " đã bị quản trị viên gỡ khỏi kí gửi, được gửi trả lại." + why, now);
         return true;
      }
   }

   /** Chạy định kỳ (scheduler): món hết hạn -> về thư người bán. */
   public void expireTick() {
      long now = System.currentTimeMillis();
      synchronized (lock) {
         for (Iterator<Listing> it = active.values().iterator(); it.hasNext(); ) {
            Listing listing = it.next();
            if (now >= listing.expireAt()) {
               it.remove();
               returnToSeller(listing, Listing.EXPIRED, "Kí gửi: hết hạn",
                     describe(listing.item()) + " đã hết thời gian kí gửi, được gửi trả lại.", now);
            }
         }
      }
   }

   // ================================================================== nội bộ

   /** Đang giữ {@link #lock}: đóng món + thư trả đồ cho người bán trong 1 transaction (xếp hàng ngay trong khoá). */
   private void returnToSeller(Listing listing, byte status, String subject, String content, long now) {
      Mail mail = mails.systemMail(listing.sellerId(), listing.sellerName(), subject, content, List.of(listing.item()));
      players.runOnWriter(conn -> {
         MarketRepository.close(conn, listing.id(), status, 0, now);
         MailRepository.insert(conn, mail);
      }).thenAccept(ok -> {
         if (ok) {
            mails.deliver(mail);
         }
      });
   }

   private void sendList(PlayerSession session, PlayerData d) {
      long now = System.currentTimeMillis();
      List<Listing> shown = new ArrayList<>(MAX_LISTINGS);
      synchronized (lock) {
         List<Listing> all = new ArrayList<>(active.values());
         for (int i = all.size() - 1; i >= 0; i--) { // mới nhất trước (tối đa MAX_LISTINGS món)
            if (now < all.get(i).expireAt()) {
               shown.add(all.get(i));
            }
         }
      }
      session.send(MarketPackets.clear());
      session.send(MarketPackets.list(shown, now));
   }

   private int countOf(int sellerId) {
      int n = 0;
      for (Listing l : active.values()) {
         if (l.sellerId() == sellerId) {
            n++;
         }
      }
      return n;
   }

   /** Đã vào game, không giao dịch, không bấm quá nhanh. */
   private static PlayerData ready(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể kí gửi."));
         return null;
      }
      if (d.fishing != null) {
         session.send(Packets.notice("Đang câu cá, không thể kí gửi."));
         return null;
      }
      UpgradeService.cancel(session, d); // huỷ lựa chọn trong lò rèn
      long now = System.currentTimeMillis();
      if (now - d.lastMarketAt < ACTION_COOLDOWN_MS) {
         session.send(Packets.notice("Thao tác quá nhanh."));
         return null;
      }
      d.lastMarketAt = now;
      return d;
   }

   /** Phí = {@code percent}% giá, làm tròn lên, tối thiểu 1. */
   static long fee(int price, int percent) {
      return Math.max(1, ((long) price * percent + 99) / 100);
   }

   /** Thời gian bán theo tên dòng phí ("8h", "16h", "24h"); tên lạ thì 8h × (thứ tự + 1). */
   static long durationMs(GamePack.MarketFee option) {
      Matcher m = HOURS.matcher(option.name());
      long hours = m.find() ? Long.parseLong(m.group(1)) : 8L * (option.index() + 1);
      return hours * 3_600_000L;
   }

   static String format(long value) {
      return String.format(Locale.US, "%,d", value).replace(',', '.');
   }

   private static String describe(ItemData item) {
      String name = item.template() != null ? item.template().name() : "vật phẩm";
      return item.quantity > 1 ? name + " x" + item.quantity : name;
   }
}
