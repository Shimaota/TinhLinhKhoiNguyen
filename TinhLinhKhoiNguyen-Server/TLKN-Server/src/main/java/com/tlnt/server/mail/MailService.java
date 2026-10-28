package com.tlnt.server.mail;

import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hòm thư (op 40, cửa sổ {@code MailWindow}: "Hòm thư" / "Gửi thư"). Handler chạy lane BLOCKING (tra tên người nhận
 * bằng SQL), vẫn tuần tự theo session như lane GAME.
 *
 * <h2>Lưu trữ</h2>
 * Mọi thư nằm ở bảng {@code mail}. Người nhận OFFLINE: thư chỉ được INSERT vào SQL, lần vào game sau nạp lên. Người
 * nhận ONLINE: sau khi INSERT xong, thư được đẩy vào {@link Mailbox} trong RAM của họ (trên hàng đợi session của họ)
 * và gửi lại danh sách. Chỉ CHỦ thư được đọc / nhận / xoá thư của mình -> RAM là bản chuẩn, SQL ghi theo sau qua
 * db-writer (1 luồng, đúng thứ tự). Mỗi hộp giữ tối đa 50 thư kiểu cuốn chiếu, thư quá 7 ngày tự xoá ({@link Mailbox}).
 *
 * <h2>Vì sao không nhân bản / mất đồ</h2>
 * <ul>
 *   <li>Gửi: phí {@link #SEND_FEE} vàng KHÔNG khoá, tối đa {@link #MAX_ITEMS} món, chỉ món không khoá / chưa hết hạn /
 *       đúng mẫu + số lượng client báo / ô không trùng; đang giao dịch thì chặn (túi đóng băng). Trừ túi + vàng và ghi
 *       2 dòng thư trong CÙNG 1 transaction với lượt lưu nhân vật ({@code PlayerService.saveWith}), dưới
 *       {@code synchronized (player)} — lượt lưu lúc thoát game cũng chụp dưới khoá đó, nên sập server / thoát giữa
 *       chừng thì hoặc đã trừ đồ + có thư, hoặc chưa có gì.</li>
 *   <li>Nhận: cờ "đã nhận" đổi trong RAM (bản chuẩn) rồi mới cho đồ; cho đồ làm trên bản sao túi, thiếu chỗ thì
 *       không đổi gì. Cờ "đã nhận" + túi mới ghi cùng 1 transaction.</li>
 * </ul>
 */
public final class MailService {

   /** Phí gửi 1 thư (vàng không khoá) — client ghi "Phí gửi thư: 100.000 vàng". */
   public static final long SEND_FEE = 100_000L;
   /** Số món đính kèm tối đa (client cho chọn 6 ô, luật server 5). */
   public static final int MAX_ITEMS = 5;
   /** Tên người gửi của thư hệ thống (kí gửi...). */
   public static final String SYSTEM_SENDER = "Hệ thống";
   static final int MAX_SUBJECT = 50;
   static final int MAX_CONTENT = 500;
   private static final int MAX_NAME = 16;
   private static final long SEND_COOLDOWN_MS = 3_000;

   private final MailRepository repository;
   private final PlayerService players;
   private final ItemService items;
   private final Executor gamePool;
   /** Server tự cấp id thư (client dùng id int để đọc / nhận / xoá). */
   private final AtomicInteger ids;
   /**
    * Người đang online có hòm thư trong RAM. Đăng ký TRƯỚC khi đọc SQL lúc vào game: thư chèn vào DB sau lúc đó chắc
    * chắn được đẩy tới (trùng thì {@link Mailbox#add} bỏ qua).
    */
   private final Map<Integer, Owner> online = new ConcurrentHashMap<>();

   private record Owner(PlayerSession session, PlayerData data) {
   }

   public MailService(MailRepository repository, PlayerService players, ItemService items, Executor gamePool)
         throws SQLException {
      this.repository = repository;
      this.players = players;
      this.items = items;
      this.gamePool = gamePool;
      this.ids = new AtomicInteger(repository.maxId());
   }

   // ================================================================== vòng đời

   /** Hook vào game (luồng blocking, trước khi vào khu): nạp hòm thư, xoá thư quá hạn / thừa. */
   public void onLogin(PlayerSession session, PlayerData d) {
      online.put(d.getId(), new Owner(session, d));
      long now = System.currentTimeMillis();
      List<Mail> dropped = new ArrayList<>();
      try {
         d.mailbox = new Mailbox(repository.load(d.getId()), now, dropped);
      } catch (SQLException e) {
         Log.error("Khong nap duoc hom thu nhan vat #" + d.getId(), e);
         d.mailbox = new Mailbox(List.of(), now, dropped);
      }
      deleteLater(dropped);
   }

   /** Hook thoát game. */
   public void onLogout(PlayerData d) {
      online.computeIfPresent(d.getId(), (id, owner) -> owner.data() == d ? null : owner);
   }

   // ================================================================== thao tác của chủ thư (op 40)

   /** Sub 0: mở 1 thư chưa đọc ở hòm thư -> đã đọc. */
   public void read(PlayerSession session, int mailId) {
      PlayerData d = owner(session);
      Mail mail = d == null ? null : d.mailbox.find(Mail.INBOX, mailId);
      if (mail == null || !d.mailbox.markRead(mail)) {
         return;
      }
      int ownerId = d.getId();
      players.runOnWriter(conn -> MailRepository.markRead(conn, ownerId, mailId));
      session.send(d.mailbox.packet(Mail.INBOX));
   }

   /**
    * Sub 1 "Gửi thư": tên người nhận, chủ đề, nội dung, các món ({@code slots} ô túi, {@code templateIds} mẫu,
    * {@code quantities} số lượng). Tra người nhận bằng SQL (lane BLOCKING).
    */
   public void send(PlayerSession session, String recipient, String subject, String content, int[] slots,
         int[] templateIds, int[] quantities) throws SQLException {
      PlayerData d = owner(session);
      if (d == null) {
         return;
      }
      UpgradeService.cancel(session, d); // huỷ lựa chọn trong lò rèn
      long now = System.currentTimeMillis();
      String name = clean(recipient, MAX_NAME);
      subject = clean(subject, MAX_SUBJECT);
      content = clean(content, MAX_CONTENT);
      String error = name.isEmpty() || subject.isEmpty() || content.isEmpty()
            ? "Hãy nhập đủ tên người nhận, chủ đề và nội dung."
            : now - d.lastMailAt < SEND_COOLDOWN_MS ? "Thao tác quá nhanh, hãy thử lại sau giây lát."
            : checkSend(d, slots, templateIds, quantities, now);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      d.lastMailAt = now;
      MailRepository.PlayerRef target = repository.findPlayer(name);
      if (target == null) {
         session.send(Packets.notice("Không tìm thấy người chơi " + name + "."));
         return;
      }
      if (target.id() == d.getId()) {
         session.send(Packets.notice("Không thể gửi thư cho chính mình."));
         return;
      }
      Mail incoming;
      synchronized (d) { // cùng khoá với lượt lưu lúc thoát game
         if (session.getPlayer() != d) {
            return;
         }
         error = checkSend(d, slots, templateIds, quantities, now); // kiểm tra lại: SQL vừa chạy xong
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
         List<ItemData> attached = new ArrayList<>(slots.length);
         for (int i = 0; i < slots.length; i++) {
            attached.add(ItemService.takeFromBag(d, slots[i], quantities[i]));
         }
         d.gold -= SEND_FEE;
         incoming = new Mail(nextId(), target.id(), Mail.INBOX, d.name, target.name(), subject, content, now, attached,
               false, false);
         Mail sent = incoming.sentCopy(nextId(), d.getId());
         List<Mail> evicted = d.mailbox.add(sent);
         evicted.addAll(d.mailbox.purgeExpired(now));
         players.saveWith(d, conn -> {
            MailRepository.insert(conn, incoming);
            MailRepository.insert(conn, sent);
            MailRepository.delete(conn, evicted);
         }).thenAccept(ok -> {
            if (ok) {
               deliver(incoming);
            }
         });
      }
      items.syncBagAndCurrency(session);
      session.send(d.mailbox.packet(Mail.SENT));
      session.send(Packets.notice("Đã gửi thư cho " + target.name() + "."));
      Log.debug("Thu #%d: '%s' -> '%s' (%d mon)", incoming.id, d.name, target.name(), incoming.items.size());
   }

   /** Sub 3 "Nhận thư" (chi tiết 1 thư). */
   public void claim(PlayerSession session, int mailId) {
      PlayerData d = owner(session);
      Mail mail = d == null ? null : d.mailbox.find(Mail.INBOX, mailId);
      if (mail != null) {
         claim(session, d, List.of(mail));
      }
   }

   /** Sub 5 "Nhận thư" ở danh sách: nhận mọi thư còn đồ (cũ trước) tới khi túi đầy. */
   public void claimAll(PlayerSession session) {
      PlayerData d = owner(session);
      if (d != null) {
         claim(session, d, d.mailbox.unclaimed());
      }
   }

   private void claim(PlayerSession session, PlayerData d, List<Mail> mails) {
      long now = System.currentTimeMillis();
      List<Mail> claimed = new ArrayList<>();
      List<ItemData> received = new ArrayList<>();
      String error = null;
      boolean expired = false;
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể nhận thư."));
            return;
         }
         if (d.fishing != null) {
            session.send(Packets.notice("Đang câu cá, không thể nhận thư."));
            return;
         }
         UpgradeService.cancel(session, d); // huỷ lựa chọn trong lò rèn
         for (Mail mail : mails) {
            if (!mail.hasUnclaimedItems()) {
               continue;
            }
            if (mail.isExpired(now)) {
               expired = true;
               continue;
            }
            error = ItemService.give(d, mail.items, now); // bản sao túi: thiếu chỗ thì không đổi gì
            if (error != null) {
               break;
            }
            d.mailbox.markClaimed(mail);
            claimed.add(mail);
            received.addAll(mail.items);
         }
         if (!claimed.isEmpty()) {
            int ownerId = d.getId();
            players.saveWith(d, conn -> {
               for (Mail mail : claimed) {
                  MailRepository.markClaimed(conn, ownerId, mail.id);
               }
            });
         }
      }
      if (error != null) {
         session.send(Packets.notice(error));
      } else if (expired) {
         session.send(Packets.notice("Thư đã hết hạn."));
      } else if (claimed.isEmpty() && mails.size() > 1) {
         session.send(Packets.notice("Không có thư nào cần nhận."));
      }
      if (!claimed.isEmpty()) {
         items.syncBagAndCurrency(session);
         session.send(ItemPackets.rewardPopup(received));
      }
      session.send(d.mailbox.packet(Mail.INBOX));
   }

   /** Sub 4 "Xóa thư": hộp {@code box} (0 hòm thư, 1 đã gửi), các id. Thư còn đồ chưa nhận thì giữ lại. */
   public void delete(PlayerSession session, byte box, int[] mailIds) {
      PlayerData d = owner(session);
      if (d == null || (box != Mail.INBOX && box != Mail.SENT)) {
         return;
      }
      int[] kept = {0};
      List<Mail> removed = d.mailbox.remove(box, mailIds, kept);
      deleteLater(removed);
      if (kept[0] > 0) {
         session.send(Packets.notice("Không thể xoá thư còn vật phẩm chưa nhận."));
      }
      session.send(d.mailbox.packet(box));
   }

   // ================================================================== thư hệ thống (kí gửi...)

   public int nextId() {
      return ids.incrementAndGet();
   }

   /** Thư hệ thống vào hòm thư của {@code ownerId} (chưa ghi DB: người gọi INSERT trong transaction của mình). */
   public Mail systemMail(int ownerId, String ownerName, String subject, String content, List<ItemData> attached) {
      return new Mail(nextId(), ownerId, Mail.INBOX, SYSTEM_SENDER, ownerName, subject, content,
            System.currentTimeMillis(), attached, false, false);
   }

   /**
    * Thư đã INSERT (transaction đã commit): chủ thư đang online thì đẩy vào hòm thư RAM của họ, trên hàng đợi session
    * của họ. Offline thì thôi — lần vào game sau nạp từ SQL.
    */
   public void deliver(Mail mail) {
      Owner owner = online.get(mail.ownerId);
      if (owner != null) {
         owner.session().inbox().execute(() -> append(owner, mail), gamePool);
      }
   }

   private void append(Owner owner, Mail mail) {
      PlayerData d = owner.data();
      if (owner.session().getPlayer() != d || d.mailbox == null) {
         return; // đã thoát: thư nằm trong SQL
      }
      List<Mail> evicted = d.mailbox.add(mail);
      evicted.addAll(d.mailbox.purgeExpired(System.currentTimeMillis()));
      deleteLater(evicted);
      owner.session().send(d.mailbox.packet(Mail.INBOX));
      if (d.inWorld) {
         owner.session().send(Packets.notice("Bạn có thư mới từ " + mail.senderName + "."));
      }
   }

   // ================================================================== nội bộ

   /**
    * Kiểm tra 1 lần gửi trên túi hiện tại: đủ phí, ≤ {@link #MAX_ITEMS} món, ô không trùng, đúng mẫu, số lượng hợp lệ,
    * món không khoá / chưa hết hạn, không đang giao dịch.
    */
   static String checkSend(PlayerData d, int[] slots, int[] templateIds, int[] quantities, long now) {
      if (d.trade != null) {
         return "Đang giao dịch, không thể gửi thư.";
      }
      if (d.fishing != null) {
         return "Đang câu cá, không thể gửi thư.";
      }
      if (slots.length > MAX_ITEMS) {
         return "Chỉ gửi được tối đa " + MAX_ITEMS + " vật phẩm mỗi thư.";
      }
      if (d.gold < SEND_FEE) {
         return "Không đủ 100.000 vàng phí gửi thư.";
      }
      for (int i = 0; i < slots.length; i++) {
         for (int k = 0; k < i; k++) {
            if (slots[k] == slots[i]) {
               return "Vật phẩm gửi không hợp lệ.";
            }
         }
         ItemData item = d.bag.get(slots[i]);
         if (item == null || item.templateId != templateIds[i]) {
            return "Vật phẩm gửi đã thay đổi, hãy chọn lại.";
         }
         String error = ItemService.checkTransferable(item, quantities[i], now);
         if (error != null) {
            return item.locked ? "Không thể gửi vật phẩm đã khoá." : error;
         }
      }
      return null;
   }

   /** Người gửi gói đã vào game xong + đã nạp hòm thư. */
   private static PlayerData owner(PlayerSession session) {
      PlayerData d = session.getPlayer();
      return d == null || !d.inWorld || d.mailbox == null ? null : d;
   }

   private void deleteLater(List<Mail> mails) {
      if (!mails.isEmpty()) {
         List<Mail> copy = List.copyOf(mails);
         players.runOnWriter(conn -> MailRepository.delete(conn, copy));
      }
   }

   /** Bỏ khoảng trắng 2 đầu + ký tự điều khiển, chuẩn hoá NFC, cắt độ dài. */
   private static String clean(String text, int maxLength) {
      if (text == null) {
         return "";
      }
      String s = Normalizer.normalize(text, Normalizer.Form.NFC).replaceAll("[\\p{Cntrl}&&[^\n]]", "").trim();
      return s.length() > maxLength ? s.substring(0, maxLength) : s;
   }
}
