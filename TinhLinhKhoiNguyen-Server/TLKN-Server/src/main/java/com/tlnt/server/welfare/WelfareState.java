package com.tlnt.server.welfare;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Tiến độ phúc lợi của 1 nhân vật (cột {@code player.welfare}, JSON Gson — field transient không lưu).
 *
 * <ul>
 *   <li>Quà online: thời gian online TRONG NGÀY (giờ server), qua 0h thì về 0 và mọi mốc nhận lại được.</li>
 *   <li>Điểm danh: mỗi ngày trong tuần 1 mốc (thứ Hai -> mốc thứ Hai...), chỉ nhận được mốc của hôm nay; sang tuần mới
 *       (thứ Hai) thì reset.</li>
 *   <li>Quà nạp: mốc theo tổng lam thạch đã nạp ({@code player.recharge_total}), mỗi mốc nhận 1 lần, không reset.</li>
 * </ul>
 * Khoá: {@code this} (luồng session + luồng thoát game cùng đụng thời gian online).
 */
public final class WelfareState {

   /** Ngày (epochDay giờ server) của {@link #onlineMs} / {@link #onlineClaimed}. */
   int day;
   /** Thời gian online trong {@link #day}, đã cộng tới {@link #segmentStart}. */
   long onlineMs;
   /** Bit i = đã nhận mốc online i. */
   int onlineClaimed;
   /** epochDay của thứ Hai tuần điểm danh hiện tại. */
   long week;
   /** Bit 0..6 = đã điểm danh thứ Hai..Chủ nhật. */
   int checkinClaimed;
   /** Bit i = đã nhận mốc nạp i. */
   int rechargeClaimed;

   /** Lúc bắt đầu đoạn online đang đếm (ms), 0 = không online. */
   private transient long segmentStart;
   /** Số phút online đã gửi client lần cuối (chỉ gửi lại mốc có số phút đổi). */
   transient int lastSentMinutes = -1;

   /** Vào game: bắt đầu đếm. */
   synchronized void start(long now) {
      segmentStart = now;
   }

   /** Thoát game: cộng đoạn đang đếm rồi dừng (chạy trước khi chụp dữ liệu lưu). */
   synchronized void stop(long now) {
      flush(now);
      segmentStart = 0;
   }

   /** Cộng đoạn đang đếm vào {@link #onlineMs} (để lượt lưu có số mới). */
   synchronized void flush(long now) {
      if (segmentStart > 0 && now > segmentStart) {
         onlineMs += now - segmentStart;
         segmentStart = now;
      }
   }

   /**
    * Sang ngày mới -> online về 0 + mốc online nhận lại; sang tuần mới -> điểm danh reset.
    * @return true nếu có reset (cần gửi lại các mốc)
    */
   synchronized boolean roll(long now, ZoneId zone) {
      LocalDate today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone);
      boolean changed = false;
      if (day != today.toEpochDay()) {
         day = (int) today.toEpochDay();
         onlineMs = 0;
         onlineClaimed = 0;
         long startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli();
         if (segmentStart > 0 && segmentStart < startOfToday) {
            segmentStart = startOfToday; // phần online trước 0h thuộc ngày cũ
         }
         changed = true;
      }
      long monday = today.with(DayOfWeek.MONDAY).toEpochDay();
      if (week != monday) {
         week = monday;
         checkinClaimed = 0;
         changed = true;
      }
      return changed;
   }

   /** Số phút online hôm nay (tính cả đoạn đang đếm). */
   synchronized int onlineMinutes(long now) {
      long ms = onlineMs + (segmentStart > 0 && now > segmentStart ? now - segmentStart : 0);
      return (int) Math.min(Integer.MAX_VALUE, ms / 60_000);
   }

   synchronized boolean isClaimed(WelfareTab tab, int index) {
      return (bits(tab) & (1 << index)) != 0;
   }

   synchronized void markClaimed(WelfareTab tab, int index) {
      switch (tab) {
         case ONLINE -> onlineClaimed |= 1 << index;
         case CHECKIN -> checkinClaimed |= 1 << index;
         case RECHARGE -> rechargeClaimed |= 1 << index;
      }
   }

   private int bits(WelfareTab tab) {
      return switch (tab) {
         case ONLINE -> onlineClaimed;
         case CHECKIN -> checkinClaimed;
         case RECHARGE -> rechargeClaimed;
      };
   }
}
