package com.tlnt.server.mail;

import com.tlnt.server.net.Message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Hòm thư của 1 người trong RAM: hộp thư đến + thư đã gửi, mỗi hộp mới nhất trước. Nạp từ SQL lúc vào game; từ đó
 * đây là bản chuẩn cho chính chủ (chỉ chủ xoá / nhận / đọc thư của mình, người khác chỉ THÊM thư mới) — nên nhận thư
 * không bao giờ dựa vào bản cũ trong DB.
 *
 * Lưu kiểu CUỐN CHIẾU: mỗi hộp tối đa {@link #MAX_PER_BOX} thư, thêm thư mới mà đầy thì bỏ thư cũ nhất (ưu tiên bỏ
 * thư đã nhận / không đính kèm trước, hết loại đó mới bỏ thư còn đồ). Thư quá {@link #TTL_MS} tự xoá.
 *
 * Khoá: {@code this} — luồng session sửa, luồng tick khu đọc lúc gửi trạng thái đầu khi vào game.
 */
public final class Mailbox {

   /** Client ghi "Hòm thư có giới hạn 50 thư và thư chỉ tồn tại trong 7 ngày". */
   public static final int MAX_PER_BOX = 50;
   public static final long TTL_MS = 7L * 24 * 60 * 60 * 1000;

   private final List<Mail> inbox = new ArrayList<>();
   private final List<Mail> sent = new ArrayList<>();

   /**
    * Tạo từ các thư đọc SQL (thứ tự bất kỳ). Thư hết hạn / thừa (quá 50) không vào hòm mà cho vào {@code dropped} để
    * xoá khỏi DB.
    */
   public Mailbox(List<Mail> loaded, long now, List<Mail> dropped) {
      List<Mail> sorted = new ArrayList<>(loaded);
      sorted.sort((a, b) -> Integer.compare(a.id, b.id)); // cũ -> mới, add() đưa thư mới lên đầu
      for (Mail mail : sorted) {
         if (mail.isExpired(now)) {
            dropped.add(mail);
         } else {
            dropped.addAll(add(mail));
         }
      }
   }

   private List<Mail> list(byte box) {
      return box == Mail.SENT ? sent : inbox;
   }

   public synchronized Mail find(byte box, int id) {
      for (Mail mail : list(box)) {
         if (mail.id == id) {
            return mail;
         }
      }
      return null;
   }

   /** Thêm thư (bỏ qua nếu đã có id đó). @return thư bị đẩy ra do đầy hộp (cần xoá khỏi DB) */
   public synchronized List<Mail> add(Mail mail) {
      List<Mail> list = list(mail.box);
      List<Mail> evicted = new ArrayList<>(0);
      for (Mail existing : list) {
         if (existing.id == mail.id) {
            return evicted;
         }
      }
      list.add(0, mail);
      while (list.size() > MAX_PER_BOX) {
         int victim = list.size() - 1;
         for (int i = list.size() - 1; i >= 0; i--) {
            if (!list.get(i).hasUnclaimedItems()) {
               victim = i;
               break;
            }
         }
         evicted.add(list.remove(victim));
      }
      return evicted;
   }

   /** Gỡ thư quá hạn ở cả 2 hộp. @return thư đã gỡ (cần xoá khỏi DB) */
   public synchronized List<Mail> purgeExpired(long now) {
      List<Mail> removed = new ArrayList<>(0);
      for (List<Mail> list : List.of(inbox, sent)) {
         for (Iterator<Mail> it = list.iterator(); it.hasNext(); ) {
            Mail mail = it.next();
            if (mail.isExpired(now)) {
               it.remove();
               removed.add(mail);
            }
         }
      }
      return removed;
   }

   /**
    * Xoá các thư {@code ids} ở hộp {@code box}. Thư còn đồ chưa nhận thì KHÔNG xoá (đếm vào {@code keptWithItems[0]}).
    * @return thư đã xoá
    */
   public synchronized List<Mail> remove(byte box, int[] ids, int[] keptWithItems) {
      List<Mail> list = list(box);
      List<Mail> removed = new ArrayList<>(ids.length);
      for (Iterator<Mail> it = list.iterator(); it.hasNext(); ) {
         Mail mail = it.next();
         if (!contains(ids, mail.id)) {
            continue;
         }
         if (mail.hasUnclaimedItems()) {
            keptWithItems[0]++;
            continue;
         }
         it.remove();
         removed.add(mail);
      }
      return removed;
   }

   /** Thư đến còn đồ chưa nhận, cũ trước (nhận tất cả theo thứ tự tới khi túi đầy). */
   public synchronized List<Mail> unclaimed() {
      List<Mail> result = new ArrayList<>();
      for (int i = inbox.size() - 1; i >= 0; i--) {
         if (inbox.get(i).hasUnclaimedItems()) {
            result.add(inbox.get(i));
         }
      }
      return result;
   }

   /** Đánh dấu đã đọc. @return true nếu vừa đổi */
   public synchronized boolean markRead(Mail mail) {
      if (mail.read) {
         return false;
      }
      mail.read = true;
      return true;
   }

   /** Đánh dấu đã nhận đồ (kèm đã đọc). @return false nếu đã nhận rồi */
   public synchronized boolean markClaimed(Mail mail) {
      if (mail.claimed) {
         return false;
      }
      mail.claimed = true;
      mail.read = true;
      return true;
   }

   /** Op 40 sub 0 / sub 1 (hộp {@code box}). */
   public synchronized Message packet(byte box) {
      return MailPackets.list(box, list(box));
   }

   private static boolean contains(int[] ids, int id) {
      for (int x : ids) {
         if (x == id) {
            return true;
         }
      }
      return false;
   }
}
