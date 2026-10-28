package com.tlnt.server.wheel;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Vòng quay may mắn (op 71; client tab "Vòng quay" của {@code ShopWindow}). Dữ liệu {@code tlnt_data.lucky_wheel}
 * ({@link LuckyWheelRepository}, nạp lại mỗi phút). Lane GAME, luồng session.
 *
 * <pre>
 * C→S 71/0 mở tab ({@code sendRequestNotification} — tên sai)
 * C→S 71/1 quay ({@code sendNotificationAction}): byte 0 = "Quay 1 lần", 1 = "Quay 10 lần"
 * S→C 71/0 ({@code handleCraftBook} — tên sai): byte n × item (thứ tự ô; option khoảng hiện "min-max")
 * S→C 71/1: byte ô trúng (client đánh dấu ô đó; quay 10 lần = ô của lần cuối)
 * </pre>
 * Mỗi lần quay chọn 1 ô theo TRỌNG SỐ ({@link LuckyWheel.Slot#weight}): ô trọng số nhỏ = hiếm. Món có dòng 57 "HSD {0}
 * ngày" (thường là khoảng [1, 3]) nhận hạn dùng ngẫu nhiên đó, riêng {@link LuckyWheel.Slot#permPercent}% ra vĩnh viễn;
 * 2 dòng 57 / 55 chỉ để hiện trong ô xem trước, món nhận được không giữ.
 * Trừ xu + nhận đồ trên 1 BẢN SAO túi (thiếu xu / thiếu chỗ thì không mất gì), rồi lưu DB ngay.
 */
public final class LuckyWheelService {

   /** Dòng chỉ số "HSD {0} ngày" / "Có tỉ lệ vĩnh viễn" (bảng stat_type). */
   static final int STAT_EXPIRE_DAYS = 57;
   static final int STAT_PERMANENT_CHANCE = 55;
   /** % vĩnh viễn khi ô có dòng 55 mà không ghi "perm". */
   static final int DEFAULT_PERM_PERCENT = 5;
   /** Thao tác 71/1: byte 1 = quay 10 lần. */
   static final int SPIN_TEN = 1;
   static final int TEN = 10;
   /** Ô có tỉ lệ ≤ bấy nhiêu % thì báo chúc mừng. */
   static final double RARE_PERCENT = 1.0;
   private static final long SPIN_COOLDOWN_MS = 500;
   private static final long DAY_MS = 86_400_000L;

   private final LuckyWheelRepository repository;
   private final PlayerService players;
   private final ItemService items;
   private volatile LuckyWheel wheel;

   public LuckyWheelService(LuckyWheelRepository repository, PlayerService players, ItemService items) {
      this.repository = repository;
      this.players = players;
      this.items = items;
      reload();
   }

   /** Nạp lại từ SQL (luồng blocking). Lỗi đọc thì giữ bản cũ. */
   public void reload() {
      try {
         wheel = repository.loadActive();
      } catch (SQLException e) {
         Log.warn("Khong doc duoc bang lucky_wheel (chay sql/migrations?): %s", e.getMessage());
      }
   }

   // ================================================================== C→S 71/0

   public void open(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      LuckyWheel w = wheel;
      if (w == null) {
         session.send(Packets.notice("Vòng quay đang tạm đóng."));
         return;
      }
      session.send(listPacket(w));
   }

   // ================================================================== C→S 71/1

   public void spin(PlayerSession session, int action) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastWheelSpinAt < SPIN_COOLDOWN_MS) {
         return;
      }
      d.lastWheelSpinAt = now;
      LuckyWheel w = wheel;
      if (w == null) {
         session.send(Packets.notice("Vòng quay đang tạm đóng."));
         return;
      }
      int times = action == SPIN_TEN ? TEN : 1;
      long cost = (long) w.costQty() * times;
      Random random = ThreadLocalRandom.current();
      int[] hits = new int[times];
      List<ItemData> rewards = new ArrayList<>(times);
      for (int k = 0; k < times; k++) {
         hits[k] = pick(w, random.nextInt(w.totalWeight()));
         rewards.add(create(w.slots().get(hits[k]), now, random));
      }
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể quay."));
            return;
         }
         if (d.fishing != null) {
            session.send(Packets.notice("Đang câu cá, không thể quay."));
            return;
         }
         ItemContainer bag = d.bag.deepCopy();
         if (ItemService.count(bag, w.costItem(), now) < cost) {
            session.send(Packets.notice("Cần " + cost + " " + itemName(w.costItem()) + " để quay"
                  + (times > 1 ? " " + times + " lần." : ".")));
            return;
         }
         UpgradeService.cancel(session, d); // huỷ lựa chọn trong lò rèn (túi sắp đổi)
         ItemService.remove(bag, w.costItem(), cost, now);
         String error = ItemService.give(d, bag, rewards, now); // thiếu chỗ thì không đổi gì (xu vẫn còn)
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
         players.saveAsync(d, false); // xu + phần thưởng cùng 1 dòng player -> ghi ngay
      }
      items.syncBagAndCurrency(session);
      session.send(winPacket(hits[times - 1]));
      session.send(ItemPackets.rewardPopup(rewards));
      for (int k = 0; k < times; k++) {
         if (w.percent(hits[k]) <= RARE_PERCENT) {
            session.send(Packets.notice("Chúc mừng! Bạn quay được " + itemName(rewards.get(k).templateId) + "!"));
         }
      }
      Log.debug("Vong quay: '%s' quay %d lan, trung o %s", d.name, times, java.util.Arrays.toString(hits));
   }

   // ================================================================== luật

   /** Ô trúng với số ngẫu nhiên {@code roll} trong [0, tổng trọng số): cộng dồn trọng số theo thứ tự ô. */
   static int pick(LuckyWheel w, int roll) {
      int acc = 0;
      for (int i = 0; i < w.slots().size(); i++) {
         acc += w.slots().get(i).weight();
         if (roll < acc) {
            return i;
         }
      }
      return w.slots().size() - 1;
   }

   /**
    * Tạo món của ô: option khoảng random như mọi RewardItem; dòng 57 "HSD {0} ngày" -> hạn dùng đúng số ngày đó (trừ
    * {@code permPercent}% vĩnh viễn); bỏ dòng 57 / 55 khỏi món nhận được.
    */
   static ItemData create(LuckyWheel.Slot slot, long now, Random random) {
      return applyExpiry(slot.item().create(now), slot.permPercent(), now, random);
   }

   /**
    * Món vừa tạo từ RewardItem có dòng 57 "HSD {0} ngày" -> hạn dùng đúng số ngày đó (trừ {@code permPercent}% vĩnh
    * viễn); bỏ dòng 57 / 55 khỏi món. Dùng chung cho quà ngẫu nhiên khác (nhiệm vụ hằng ngày).
    */
   public static ItemData applyExpiry(ItemData item, int permPercent, long now, Random random) {
      int days = 0;
      for (int k = item.stats.size() - 1; k >= 0; k--) {
         ItemData.StatValue stat = item.stats.get(k);
         if (stat.statTypeId() == STAT_EXPIRE_DAYS) {
            days = stat.values() != null && stat.values().length > 0 ? stat.values()[0] : 0;
            item.stats.remove(k);
         } else if (stat.statTypeId() == STAT_PERMANENT_CHANCE) {
            item.stats.remove(k);
         }
      }
      if (days > 0) {
         boolean permanent = permPercent > 0 && random.nextInt(100) < permPercent;
         item.expireTime = permanent ? ItemData.PERMANENT : now + days * DAY_MS;
      }
      return item;
   }

   /** Món cấu hình có dòng chỉ số {@code statTypeId} (kể cả dòng không có giá trị như [55]). */
   static boolean hasLine(RewardItem item, int statTypeId) {
      for (int[] line : item.stats()) {
         if (line != null && line.length > 0 && line[0] == statTypeId) {
            return true;
         }
      }
      return false;
   }

   // ================================================================== gói S→C

   /** 71/0: các ô, xem trước dạng khoảng (hiện "HSD 1-3 ngày", "Có tỉ lệ vĩnh viễn", option "min-max"). */
   static Message listPacket(LuckyWheel w) {
      long now = System.currentTimeMillis();
      Message msg = new Message(Opcode.LUCKY_WHEEL);
      msg.writeByte(0);
      msg.writeByte(w.slots().size());
      for (LuckyWheel.Slot slot : w.slots()) {
         ItemPackets.writePreviewRanged(msg, slot.item(), now);
      }
      return msg;
   }

   static Message winPacket(int slot) {
      Message msg = new Message(Opcode.LUCKY_WHEEL);
      msg.writeByte(1);
      msg.writeByte(slot);
      return msg;
   }

   private static String itemName(int templateId) {
      ItemPack.ItemTemplate t = GameDataCache.itemTemplate(templateId);
      return t != null ? t.name() : "vật phẩm #" + templateId;
   }
}
