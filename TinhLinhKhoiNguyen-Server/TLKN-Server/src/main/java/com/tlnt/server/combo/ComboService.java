package com.tlnt.server.combo;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gói combo (op 68; client tab "Gói combo" của {@code ShopWindow}, nút HUD {@code EventPopupClickListener}). Dữ liệu
 * {@code tlnt_data.combo} ({@link ComboRepository}, nạp lại mỗi phút), đã mua lưu cột JSON {@code player.combos}.
 * Lane GAME, luồng session.
 *
 * <pre>
 * C→S 68/0 mở tab ({@code sendRequestSocialList} — tên sai)
 * C→S 68/1 "Mua" ({@code sendSocialAction}): int id danh mục, int số thứ tự gói (trong danh sách đã nhận)
 * S→C 68/0 ({@code handleDungeonUpdate} — tên sai): int id, str tên, byte n gói × (str tên, byte m × item,
 *          int mẫu item trả tiền (icon giá), int giá, bool đã mua (nút "Mua" xám))
 * S→C 68/1: int số thứ tự gói, bool đã mua
 * </pre>
 * Mua: trả tiền + nhận đồ trên BẢN SAO túi (thiếu chỗ / thiếu tiền thì không đổi gì), dưới {@code synchronized (d)},
 * rồi lưu DB ngay (tốn lam thạch — như giftcode).
 */
public final class ComboService {

   /** Tối thiểu giữa 2 lần bấm mua (chống bấm liên tục). */
   private static final long BUY_COOLDOWN_MS = 500;
   private static final Locale VI = Locale.forLanguageTag("vi-VN");

   private final ComboRepository repository;
   private final PlayerService players;
   private final ItemService items;
   /** Danh mục đang bán (id nhỏ nhất đang bật), null = không có. Thay cả tham chiếu khi nạp lại. */
   private volatile Combo current;

   public ComboService(ComboRepository repository, PlayerService players, ItemService items) {
      this.repository = repository;
      this.players = players;
      this.items = items;
      reload();
   }

   /** Nạp lại từ SQL (luồng blocking). Lỗi đọc thì giữ bản cũ. */
   public void reload() {
      List<Combo> loaded = repository.loadActive();
      if (loaded != null) {
         current = loaded.isEmpty() ? null : loaded.get(0);
      }
   }

   // ================================================================== C→S 68/0

   public void open(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      Combo combo = current;
      if (combo == null) {
         session.send(Packets.notice("Hiện chưa có gói combo nào."));
         return;
      }
      session.send(listPacket(combo, d));
   }

   // ================================================================== C→S 68/1

   public void buy(PlayerSession session, int comboId, int shownIndex) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastComboBuyAt < BUY_COOLDOWN_MS) {
         return;
      }
      d.lastComboBuyAt = now;
      Combo combo = current;
      List<Integer> visible = combo == null || combo.id() != comboId ? List.of() : combo.visibleIndexes(d);
      if (shownIndex < 0 || shownIndex >= visible.size()) {
         session.send(Packets.notice("Gói combo không còn bán."));
         if (combo != null) {
            session.send(listPacket(combo, d)); // danh mục đổi trong lúc đang mở: gửi lại bản mới
         }
         return;
      }
      int packIndex = visible.get(shownIndex);
      Combo.Pack pack = combo.packs().get(packIndex);
      String key = combo.key(packIndex);
      List<ItemData> rewards = create(pack.items(), now);
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
         if (pack.limit() > 0 && d.comboBought.getOrDefault(key, 0) >= pack.limit()) {
            session.send(Packets.notice("Bạn đã mua gói này rồi."));
            session.send(boughtPacket(shownIndex, true));
            return;
         }
         UpgradeService.cancel(session, d); // huỷ lựa chọn trong lò rèn (túi sắp đổi)
         String error = pay(d, pack, rewards, now);
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
         d.comboBought.merge(key, 1, Integer::sum);
         players.saveAsync(d, false); // tiền + đồ + lượt mua cùng 1 dòng player -> ghi ngay, cùng lúc
      }
      items.syncBagAndCurrency(session);
      session.send(ItemPackets.rewardPopup(rewards));
      session.send(boughtPacket(shownIndex, purchased(pack, d, key)));
      session.send(Packets.notice("Mua thành công " + pack.name() + "!"));
      Log.debug("Combo #%d: '%s' mua goi %d (%s) gia %d x item %d", combo.id(), d.name, packIndex, pack.name(),
            pack.price(), pack.priceItem());
   }

   /**
    * Trả tiền + cho đồ (gọi dưới {@code synchronized (d)}). Vàng / lam thạch: kiểm tra đủ rồi mới cho đồ; vật phẩm: trừ
    * + cho trên cùng 1 bản sao túi. @return câu báo lỗi, null = xong
    */
   static String pay(PlayerData d, Combo.Pack pack, List<ItemData> rewards, long now) {
      long cost = pack.price();
      switch (pack.priceItem()) {
         case Combo.PAY_DIAMOND -> {
            if (cost > d.diamond) {
               return "Không đủ " + format(cost) + " lam thạch.";
            }
            String error = ItemService.give(d, rewards, now);
            if (error == null) {
               d.diamond -= cost;
            }
            return error;
         }
         case Combo.PAY_GOLD -> {
            long fromLocked = Math.min(d.lockedGold, cost);
            if (cost - fromLocked > d.gold) {
               return "Không đủ " + format(cost) + " vàng.";
            }
            String error = ItemService.give(d, rewards, now);
            if (error == null) {
               d.lockedGold -= fromLocked;
               d.gold -= cost - fromLocked;
            }
            return error;
         }
         default -> {
            ItemContainer bag = d.bag.deepCopy();
            if (ItemService.count(bag, pack.priceItem(), now) < cost) {
               return "Không đủ " + format(cost) + " " + itemName(pack.priceItem()) + ".";
            }
            ItemService.remove(bag, pack.priceItem(), cost, now);
            return ItemService.give(d, bag, rewards, now);
         }
      }
   }

   // ================================================================== gói S→C

   /** 68/0: các gói nhân vật này thấy (lọc class / nhân vật), món xem trước hiện khoảng "min-max" nếu có. */
   static Message listPacket(Combo combo, PlayerData d) {
      long now = System.currentTimeMillis();
      List<Integer> visible = combo.visibleIndexes(d);
      Message msg = new Message(Opcode.COMBO);
      msg.writeByte(0);
      msg.writeInt(combo.id());
      msg.writeString(combo.name());
      msg.writeByte(visible.size());
      for (int index : visible) {
         Combo.Pack pack = combo.packs().get(index);
         msg.writeString(pack.name());
         msg.writeByte(pack.items().size());
         for (RewardItem item : pack.items()) {
            ItemPackets.writePreview(msg, item, now);
         }
         msg.writeInt(pack.priceItem());
         msg.writeInt(pack.price());
         msg.writeBoolean(purchased(pack, d, combo.key(index)));
      }
      return msg;
   }

   /** 68/1: nút "Mua" của gói thứ {@code shownIndex} xám / sáng. */
   static Message boughtPacket(int shownIndex, boolean purchased) {
      Message msg = new Message(Opcode.COMBO);
      msg.writeByte(1);
      msg.writeInt(shownIndex);
      msg.writeBoolean(purchased);
      return msg;
   }

   static boolean purchased(Combo.Pack pack, PlayerData d, String key) {
      return pack.limit() > 0 && d.comboBought.getOrDefault(key, 0) >= pack.limit();
   }

   private static List<ItemData> create(List<RewardItem> list, long now) {
      List<ItemData> out = new ArrayList<>(list.size());
      for (RewardItem item : list) {
         out.add(item.create(now));
      }
      return out;
   }

   private static String itemName(int templateId) {
      ItemPack.ItemTemplate t = GameDataCache.itemTemplate(templateId);
      return t != null ? t.name() : "vật phẩm #" + templateId;
   }

   private static String format(long value) {
      return String.format(VI, "%,d", value);
   }
}
