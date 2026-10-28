package com.tlnt.server.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bộ đếm hoạt động sự kiện Trung thu cho admin panel (CHỈ RAM, tính từ lúc server khởi động). Mọi luồng (atomic).
 */
public final class EventStats {

   static final AtomicLong CHECK_INS = new AtomicLong();
   static final AtomicLong CAKES_BAKED = new AtomicLong();
   static final AtomicLong CAKES_OPENED = new AtomicLong();
   static final AtomicLong LANTERNS = new AtomicLong();
   static final AtomicLong CARROTS_THROWN = new AtomicLong();
   static final AtomicLong CARROTS_EATEN = new AtomicLong();
   static final AtomicLong CARROTS_PICKED = new AtomicLong();
   static final AtomicLong BOSS_KILLS = new AtomicLong();
   static final AtomicLong POINTS_GIVEN = new AtomicLong();

   private EventStats() {
   }

   /** Dòng "tên -> số" cho bảng thống kê. */
   static Map<String, Long> snapshot() {
      Map<String, Long> m = new LinkedHashMap<>();
      m.put("Lượt điểm danh nhận quà", CHECK_INS.get());
      m.put("Bánh đã làm", CAKES_BAKED.get());
      m.put("Bánh đã mở", CAKES_OPENED.get());
      m.put("Đèn hoa đăng đã thả", LANTERNS.get());
      m.put("Cà rốt đã hái", CARROTS_PICKED.get());
      m.put("Cà rốt đã ném cho thỏ", CARROTS_THROWN.get());
      m.put("Cà rốt thỏ đã ăn", CARROTS_EATEN.get());
      m.put("Lần hạ Cổ thần Dạ xoa", BOSS_KILLS.get());
      m.put("Tổng điểm sự kiện đã phát", POINTS_GIVEN.get());
      return m;
   }
}
