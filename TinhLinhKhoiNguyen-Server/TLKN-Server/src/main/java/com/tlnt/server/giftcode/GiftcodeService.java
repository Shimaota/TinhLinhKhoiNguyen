package com.tlnt.server.giftcode;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Giftcode (op 33 sub 1, tab "Giftcode" cửa sổ phúc lợi). Lane GAME — không SQL lúc nhập: định nghĩa mã trong RAM
 * ({@link #reload} mỗi phút), số lần đã nhập nằm trong dòng nhân vật (cột JSON {@code player.giftcodes}), tổng lượt
 * cả server đếm trong RAM (khởi động cộng từ cột đó).
 *
 * <h2>Các kiểu mã (cột bảng {@code tlnt_data.giftcode})</h2>
 * <ul>
 *   <li>Quà khác nhau theo class / nhân vật: cột JSON {@code rewards}, mỗi món có "c" (loại vũ khí) / "g" (nhân vật)
 *       riêng (0 = ai cũng nhận).</li>
 *   <li>Chỉ cho class / nhân vật / tên nhất định: {@code class_ids}, {@code genders}, {@code player_names} (CSV).</li>
 *   <li>Cấp: {@code min_level} / {@code max_level}. Thời gian: {@code start_at} / {@code end_at}.</li>
 *   <li>Số lượt: {@code max_uses} (cả server, 0 = vô hạn), {@code uses_per_player} (mỗi nhân vật, 0 = vô hạn).</li>
 *   <li>{@code listed} = 1: hiện ở khung "Danh sách Giftcode" (S→C 33/2).</li>
 * </ul>
 *
 * <h2>Chống nhân bản</h2>
 * Giữ chỗ 1 lượt toàn server (atomic) -> cho quà trên bản sao túi (thiếu chỗ thì trả lại lượt, không đổi gì) -> túi
 * mới + lượt nhập nằm cùng 1 dòng {@code player} -> ghi cùng 1 câu UPDATE, dưới {@code synchronized (player)}. Nhập sai 5 lần liên tiếp -> khoá nhập 1 phút (chống dò mã).
 */
public final class GiftcodeService {

   private static final long COOLDOWN_MS = 2_000;
   private static final int MAX_FAILS = 5;
   private static final long FAIL_LOCK_MS = 60_000;
   private static final int MAX_CODE_LENGTH = 32;

   /** Định nghĩa mã, key = mã chữ hoa. Thay cả map mỗi lần nạp lại. */
   private static volatile Map<String, Giftcode> codes = Map.of();

   private final GiftcodeRepository repository;
   private final PlayerService players;
   private final ItemService items;
   /** Tổng lượt đã nhập mỗi mã (cả server). */
   private final Map<String, AtomicInteger> totals = new ConcurrentHashMap<>();

   public GiftcodeService(GiftcodeRepository repository, PlayerService players, ItemService items)
         throws SQLException {
      this.repository = repository;
      this.players = players;
      this.items = items;
      repository.loadTotals().forEach((code, n) -> totals.put(code, new AtomicInteger(n)));
      reload();
   }

   /** Thế giới (để gửi lại danh sách cho người đang online khi admin đổi mã hiện); null = chưa gắn. */
   private volatile com.tlnt.server.world.World world;
   /** Các mã đang hiện ở khung "Danh sách Giftcode" lần nạp trước (so để biết có cần gửi lại không). */
   private volatile String listedSignature = "";

   public void setWorld(com.tlnt.server.world.World world) {
      this.world = world;
   }

   /**
    * Đọc lại bảng giftcode (luồng blocking). Lỗi đọc thì giữ bản cũ. Bộ mã HIỆN ở khung "Danh sách Giftcode" đổi
    * (admin bật "Hiện ở danh sách", mã hết / tới hạn) -> gửi lại S→C 33/2 cho mọi người đang online (trước đây chỉ gửi lúc
    * vào game -> người đang chơi không thấy mã mới).
    */
   public void reload() {
      Map<String, Giftcode> loaded = repository.loadCodes();
      if (loaded != null) {
         codes = Map.copyOf(loaded);
      }
      long now = System.currentTimeMillis();
      List<String> listed = new ArrayList<>();
      for (Giftcode g : codes.values()) {
         if (g.listed() && g.isActive(now)) {
            listed.add(g.code() + "|" + g.name());
         }
      }
      java.util.Collections.sort(listed);
      String signature = String.join(",", listed);
      if (!signature.equals(listedSignature)) {
         listedSignature = signature;
         com.tlnt.server.world.World w = world;
         if (w != null) {
            for (com.tlnt.server.world.WorldPlayer p : w.onlinePlayers()) {
               p.send(listPacket(p.data()));
            }
         }
      }
   }

   /** Op 33 sub 1: nhập mã. */
   public void redeem(PlayerSession session, String input) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastGiftcodeAt < COOLDOWN_MS) {
         session.send(Packets.notice("Thao tác quá nhanh."));
         return;
      }
      if (d.giftcodeFails >= MAX_FAILS && now - d.lastGiftcodeAt < FAIL_LOCK_MS) {
         session.send(Packets.notice("Nhập sai quá nhiều lần, hãy thử lại sau 1 phút."));
         return;
      }
      d.lastGiftcodeAt = now;
      if (d.giftcodes == null) {
         d.giftcodes = new HashMap<>();
      }
      String code = GiftcodeRepository.normalize(input);
      Giftcode giftcode = code.length() > MAX_CODE_LENGTH ? null : codes.get(code);
      if (giftcode == null) {
         d.giftcodeFails = d.giftcodeFails >= MAX_FAILS ? 1 : d.giftcodeFails + 1;
         session.send(Packets.notice("Mã quà tặng không tồn tại."));
         return;
      }
      d.giftcodeFails = 0;
      String error = giftcode.checkEligible(d, now);
      int used = d.giftcodes.getOrDefault(code, 0);
      if (error == null && giftcode.usedUp(used)) {
         error = giftcode.usesPerPlayer() == 1 ? "Bạn đã nhập mã này rồi."
               : "Bạn đã dùng hết " + giftcode.usesPerPlayer() + " lượt của mã này.";
      }
      List<ItemData> rewards = error == null ? giftcode.rewardsFor(d, now) : List.of();
      if (error == null && rewards.isEmpty()) {
         error = "Mã này không có quà cho nhân vật của bạn.";
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      AtomicInteger total = totals.computeIfAbsent(code, c -> new AtomicInteger());
      if (!reserve(total, giftcode.maxUses())) {
         session.send(Packets.notice("Mã quà tặng đã hết lượt sử dụng."));
         return;
      }
      boolean granted = false;
      try {
         synchronized (d) {
            if (session.getPlayer() != d) {
               return;
            }
            if (d.trade != null) {
               session.send(Packets.notice("Đang giao dịch, không thể nhận quà."));
               return;
            }
            error = ItemService.give(d, rewards, now); // bản sao túi: thiếu chỗ thì không đổi gì
            if (error != null) {
               session.send(Packets.notice(error));
               return;
            }
            d.giftcodes.put(code, used + 1);
            players.saveAsync(d, false); // túi đồ + lượt nhập cùng 1 dòng player -> luôn ghi cùng lúc
            granted = true;
         }
      } finally {
         if (!granted) {
            total.decrementAndGet(); // trả lại lượt đã giữ
         }
      }
      items.syncBagAndCurrency(session);
      session.send(ItemPackets.rewardPopup(rewards));
      session.send(Packets.notice("Nhập mã " + code + " thành công!"));
      ItemPackets.receivedNotices(rewards).forEach(session::send); // mỗi món 1 dòng
      session.send(listPacket(d));
      Log.debug("Giftcode %s: '%s' nhan %d mon", code, d.name, rewards.size());
   }

   /** Giữ 1 lượt nếu còn ({@code max} 0 = vô hạn). */
   private static boolean reserve(AtomicInteger total, int max) {
      while (true) {
         int n = total.get();
         if (max > 0 && n >= max) {
            return false;
         }
         if (total.compareAndSet(n, n + 1)) {
            return true;
         }
      }
   }

   /**
    * S→C 33/2 ({@code giftcodeListText}): chữ khung "Danh sách Giftcode" = các mã {@code listed} đang trong thời gian,
    * đánh dấu mã đã nhập. Gửi lúc vào game + sau mỗi lần nhập.
    */
   public static Message listPacket(PlayerData d) {
      long now = System.currentTimeMillis();
      List<Giftcode> shown = new ArrayList<>();
      for (Giftcode g : codes.values()) {
         if (g.listed() && g.isActive(now)) {
            shown.add(g);
         }
      }
      shown.sort((a, b) -> a.code().compareTo(b.code()));
      StringBuilder text = new StringBuilder();
      for (Giftcode g : shown) {
         int used = d.giftcodes == null ? 0 : d.giftcodes.getOrDefault(g.code(), 0);
         if (text.length() > 0) {
            text.append('\n');
         }
         text.append("[#FFF600]").append(g.code()).append("[reset] - ").append(g.name());
         if (g.usedUp(used)) {
            text.append(" [#9E9E9E](Đã nhập)[reset]");
         }
      }
      Message msg = new Message(Opcode.WELFARE);
      msg.writeByte(2);
      msg.writeString(text.length() == 0 ? "Chưa có giftcode nào." : text.toString());
      return msg;
   }
}
