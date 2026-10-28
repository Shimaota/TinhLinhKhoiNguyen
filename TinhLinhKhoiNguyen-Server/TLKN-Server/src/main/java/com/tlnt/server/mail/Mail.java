package com.tlnt.server.mail;

import com.google.gson.JsonArray;
import com.tlnt.server.item.ItemCodec;
import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1 lá thư (1 dòng bảng {@code mail}). Mỗi lá thuộc đúng 1 người ({@link #ownerId}) và 1 hộp ({@link #box}): gửi 1 thư
 * = 2 dòng (hòm thư người nhận + "thư đã gửi" của người gửi, món đính kèm ở bản người gửi chỉ để xem).
 *
 * {@link #read} / {@link #claimed} chỉ luồng session của chủ thư sửa (dưới khoá {@link Mailbox}).
 */
public final class Mail {

   public static final byte INBOX = 0;
   public static final byte SENT = 1;

   public final int id;
   public final int ownerId;
   public final byte box;
   public final String senderName;
   public final String receiverName;
   public final String subject;
   public final String content;
   public final long createdAt;
   /** Món đính kèm (bản sao riêng của thư, không ai sửa). Rỗng = không đính kèm. */
   public final List<ItemData> items;
   /** JSON món đính kèm chụp lúc tạo (luồng db-writer ghi chuỗi này, không đọc lại object). null = không có. */
   final String itemsJson;
   boolean read;
   boolean claimed;

   public Mail(int id, int ownerId, byte box, String senderName, String receiverName, String subject, String content,
         long createdAt, List<ItemData> items, boolean read, boolean claimed) {
      this.id = id;
      this.ownerId = ownerId;
      this.box = box;
      this.senderName = senderName;
      this.receiverName = receiverName;
      this.subject = subject;
      this.content = content;
      this.createdAt = createdAt;
      this.items = Collections.unmodifiableList(new ArrayList<>(items));
      this.itemsJson = toJson(this.items);
      this.read = read;
      this.claimed = claimed;
   }

   /** Còn món đính kèm chưa nhận (chỉ hòm thư). */
   public boolean hasUnclaimedItems() {
      return box == INBOX && !claimed && !items.isEmpty();
   }

   public boolean isExpired(long now) {
      return now - createdAt >= Mailbox.TTL_MS;
   }

   /** Bản ở hộp "thư đã gửi" của người gửi (id riêng, món đính kèm chỉ để xem). */
   public Mail sentCopy(int copyId, int senderId) {
      List<ItemData> copies = new ArrayList<>(items.size());
      for (ItemData item : items) {
         copies.add(item.copy());
      }
      return new Mail(copyId, senderId, SENT, senderName, receiverName, subject, content, createdAt, copies, true, true);
   }

   private static String toJson(List<ItemData> items) {
      if (items.isEmpty()) {
         return null;
      }
      JsonArray array = new JsonArray();
      for (ItemData item : items) {
         array.add(ItemCodec.toJson(item));
      }
      return array.toString();
   }
}
