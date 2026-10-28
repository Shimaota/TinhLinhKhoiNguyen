package com.tlnt.server.welfare;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * Phúc lợi (op 33, cửa sổ {@code GiftcodeWindow} — client đặt tên giftcode event): 3 tab {@link WelfareTab} + tab
 * Giftcode client có sẵn. Quà ở bảng SQL {@code tlnt_data.welfare_reward} ({@link WelfareRewards}), tiến độ ở
 * {@link WelfareState}. Lane GAME.
 *
 * <pre>
 * S→C 33/0 (1 LẦN mỗi lần vào game — client chỉ cộng thêm, không xoá): byte n tab, n×(byte id, str tên, str icon,
 *          byte m mốc, m×(byte id, str mô tả, byte trạng thái, byte k, k×item))
 * S→C 33/1 cập nhật 1 mốc: byte tab, byte mốc, str mô tả, byte trạng thái, byte k, k×item
 * C→S 33/0 "Nhận" (chỉ khi trạng thái 1): byte tab, byte mốc
 * trạng thái: 0 chưa đủ điều kiện, 1 nhận được (chấm đỏ), 2 đã nhận
 * </pre>
 */
public final class WelfareService {

   public static final byte LOCKED = 0;
   public static final byte CLAIMABLE = 1;
   public static final byte CLAIMED = 2;
   /** Chu kỳ cập nhật số phút online (giây). */
   public static final int TICK_SECONDS = 60;
   /** Giờ server quyết định lúc qua ngày / qua tuần. */
   static final ZoneId ZONE = ZoneId.systemDefault();

   private final ItemService items;
   private final Executor gamePool;

   public WelfareService(ItemService items, Executor gamePool) {
      this.items = items;
      this.gamePool = gamePool;
   }

   // ================================================================== vòng đời

   /** Hook vào game (trước khi vào khu -> gói 33/0 lúc vào khu đã có số mới). */
   public void onLogin(PlayerSession session, PlayerData d) {
      if (d.welfare == null) {
         d.welfare = new WelfareState();
      }
      long now = System.currentTimeMillis();
      d.welfare.start(now);
      d.welfare.roll(now, ZONE);
      d.welfare.lastSentMinutes = d.welfare.onlineMinutes(now);
   }

   /** Hook thoát game: cộng thời gian online TRƯỚC khi chụp dữ liệu lưu. */
   public void onLogout(PlayerData d) {
      if (d.welfare != null) {
         d.welfare.stop(System.currentTimeMillis());
      }
   }

   /** Scheduler mỗi {@link #TICK_SECONDS} giây: xếp {@link #tick} vào hàng đợi từng người. */
   public void tickAll(List<PlayerSession> sessions) {
      for (PlayerSession session : sessions) {
         PlayerData d = session.getPlayer();
         if (d != null && d.inWorld && d.welfare != null) {
            session.inbox().execute(() -> tick(session), gamePool);
         }
      }
   }

   /** Qua ngày / tuần -> gửi lại mọi mốc; không thì chỉ gửi mốc online có số phút hiển thị đổi. */
   void tick(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || d.welfare == null) {
         return;
      }
      long now = System.currentTimeMillis();
      WelfareState w = d.welfare;
      boolean rolled = w.roll(now, ZONE);
      w.flush(now);
      int minutes = w.onlineMinutes(now);
      if (rolled) {
         for (WelfareTab tab : new WelfareTab[] {WelfareTab.ONLINE, WelfareTab.CHECKIN}) {
            for (int i = 0; i < WelfareRewards.of(tab).size(); i++) {
               session.send(milestonePacket(d, tab, i, now));
            }
         }
      } else if (minutes != w.lastSentMinutes) {
         List<WelfareRewards.Milestone> online = WelfareRewards.of(WelfareTab.ONLINE);
         for (int i = 0; i < online.size(); i++) {
            int target = online.get(i).target();
            if (!w.isClaimed(WelfareTab.ONLINE, i)
                  && Math.min(minutes, target) != Math.min(Math.max(w.lastSentMinutes, 0), target)) {
               session.send(milestonePacket(d, WelfareTab.ONLINE, i, now));
            }
         }
      }
      w.lastSentMinutes = minutes;
   }

   // ================================================================== nhận quà (C→S 33/0)

   public void claim(PlayerSession session, int tabId, int index) {
      PlayerData d = session.getPlayer();
      WelfareTab tab = WelfareTab.of(tabId);
      if (d == null || !d.inWorld || d.welfare == null || tab == null
            || index < 0 || index >= WelfareRewards.of(tab).size()) {
         return;
      }
      long now = System.currentTimeMillis();
      List<ItemData> rewards;
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể nhận quà."));
            return;
         }
         d.welfare.roll(now, ZONE);
         byte status = status(d, tab, index, now);
         if (status != CLAIMABLE) {
            session.send(Packets.notice(status == CLAIMED ? "Bạn đã nhận phần quà này." : "Chưa đủ điều kiện nhận quà."));
            session.send(milestonePacket(d, tab, index, now));
            return;
         }
         rewards = WelfareRewards.of(tab).get(index).create(now);
         String error = ItemService.give(d, rewards, now); // bản sao túi: thiếu chỗ thì không đổi gì
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
         d.welfare.markClaimed(tab, index);
      }
      if (tab == WelfareTab.CHECKIN) { // điểm danh
         com.tlnt.server.player.ActivityPoints.add(session, d, com.tlnt.server.player.ActivityPoints.CHECK_IN);
      }
      session.send(milestonePacket(d, tab, index, now));
      items.syncBagAndCurrency(session);
      session.send(ItemPackets.rewardPopup(rewards));
      ItemPackets.receivedNotices(rewards).forEach(session::send); // mỗi món 1 dòng
   }

   /**
    * Cộng lam thạch đã nạp (logic nạp làm sau sẽ gọi hàm này trên luồng session) rồi gửi lại các mốc quà nạp.
    */
   public void addRecharge(PlayerSession session, long amount) {
      PlayerData d = session.getPlayer();
      if (d == null || amount <= 0) {
         return;
      }
      synchronized (d) {
         d.rechargeTotal += amount;
      }
      com.tlnt.server.ranking.RankingService.recharged(d, amount); // Top tuần Giàu có
      if (d.inWorld && d.welfare != null) {
         long now = System.currentTimeMillis();
         for (int i = 0; i < WelfareRewards.of(WelfareTab.RECHARGE).size(); i++) {
            session.send(milestonePacket(d, WelfareTab.RECHARGE, i, now));
         }
      }
   }

   // ================================================================== gói

   /** S→C 33/0: cả 3 tab (gửi đúng 1 lần mỗi lần vào game, trong trạng thái đầu). */
   public static Message fullPacket(PlayerData d) {
      long now = System.currentTimeMillis();
      Message msg = new Message(Opcode.WELFARE);
      msg.writeByte(0);
      msg.writeByte(WelfareTab.values().length);
      for (WelfareTab tab : WelfareTab.values()) {
         msg.writeByte(tab.id);
         msg.writeString(tab.title);
         msg.writeString(tab.icon);
         List<WelfareRewards.Milestone> milestones = WelfareRewards.of(tab);
         msg.writeByte(milestones.size());
         for (int i = 0; i < milestones.size(); i++) {
            msg.writeByte(i);
            writeMilestone(msg, d, tab, i, now);
         }
      }
      return msg;
   }

   /** S→C 33/1: 1 mốc. */
   static Message milestonePacket(PlayerData d, WelfareTab tab, int index, long now) {
      Message msg = new Message(Opcode.WELFARE);
      msg.writeByte(1);
      msg.writeByte(tab.id);
      msg.writeByte(index);
      writeMilestone(msg, d, tab, index, now);
      return msg;
   }

   private static void writeMilestone(Message msg, PlayerData d, WelfareTab tab, int index, long now) {
      WelfareRewards.Milestone milestone = WelfareRewards.of(tab).get(index);
      msg.writeString(description(d, tab, index, now));
      msg.writeByte(status(d, tab, index, now));
      List<ItemData> preview = milestone.create(now); // hạn dùng hiện đúng như lúc nhận ngay bây giờ
      msg.writeByte(preview.size());
      for (ItemData item : preview) {
         ItemPackets.writeItem(msg, item);
      }
   }

   static byte status(PlayerData d, WelfareTab tab, int index, long now) {
      WelfareState w = d.welfare;
      if (w == null) {
         return LOCKED;
      }
      if (w.isClaimed(tab, index)) {
         return CLAIMED;
      }
      int target = WelfareRewards.of(tab).get(index).target();
      boolean reached = switch (tab) {
         case ONLINE -> w.onlineMinutes(now) >= target;
         case CHECKIN -> todayIndex(now) == target;
         case RECHARGE -> d.rechargeTotal >= target;
      };
      return reached ? CLAIMABLE : LOCKED;
   }

   static String description(PlayerData d, WelfareTab tab, int index, long now) {
      int target = WelfareRewards.of(tab).get(index).target();
      return switch (tab) {
         case ONLINE -> "Online " + Math.min(d.welfare == null ? 0 : d.welfare.onlineMinutes(now), target) + "/"
               + target + " Phút";
         case CHECKIN -> WelfareRewards.DAY_NAMES[target] + (todayIndex(now) == target ? " (Hôm nay)" : "");
         case RECHARGE -> "Nạp " + format(Math.min(d.rechargeTotal, target)) + "/" + format(target) + " Lam thạch";
      };
   }

   /** Thứ hôm nay theo giờ server: thứ Hai = 0 ... Chủ nhật = 6. */
   static int todayIndex(long now) {
      return LocalDate.ofInstant(Instant.ofEpochMilli(now), ZONE).getDayOfWeek().getValue() - 1;
   }

   private static String format(long value) {
      return String.format(Locale.US, "%,d", value).replace(',', '.');
   }
}
