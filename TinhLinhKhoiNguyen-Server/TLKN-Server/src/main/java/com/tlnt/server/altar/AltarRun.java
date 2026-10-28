package com.tlnt.server.altar;

/**
 * 1 lượt Tế đàn Thiên tích đang chạy — CHỈ TRONG RAM ({@code AltarService.runs} theo id nhân vật + gắn vào
 * {@code PlayerData.altarRun}): thoát game vào lại vẫn tiếp tục, tắt máy chủ là mất lượt (SQL chỉ lưu ngày đã đi
 * lượt gần nhất: cột {@code player.altar_day}).
 *
 * <p>Luồng session ghi; luồng tick khu đọc ({@link #activeEnd}, {@link #sealed}) -> trường volatile. {@link #start} = mã
 * lượt (phân biệt khu cũ / mới), {@link #cleared} = số tầng đã diệt hết LIÊN TIẾP từ map 1 (= cấp rương).
 */
public final class AltarRun {

   /** Lúc bắt đầu (epoch ms), 0 = không có lượt nào. */
   public volatile long start;
   /** Giờ kết thúc (epoch ms); 0 = {@code start} + {@link AltarRules#RUN_MS} (lệnh test {@code /tedan het} rút ngắn). */
   public volatile long end;
   public volatile int cleared;
   /** Map Tế đàn đang / vừa đứng (vào lại thì về đây). */
   public volatile int map;
   /** Đã làm Nghi thức Ấn linh trong lượt này. */
   public volatile boolean sealed;
   /** Tên chủ lượt — gửi thư thưởng lúc họ offline. */
   public final String ownerName;
   private boolean done;

   public AltarRun() {
      this(0, "");
   }

   public AltarRun(long start, String ownerName) {
      this.start = start;
      this.ownerName = ownerName;
      this.map = AltarRules.FIRST_MAP;
   }

   /** Giờ kết thúc của lượt đang chạy (chưa hết giờ, chưa chốt thưởng); 0 = không có lượt nào đang chạy. */
   public long activeEnd(long now) {
      long end = endAt();
      return start > 0 && !isDone() && now < end ? end : 0;
   }

   public long endAt() {
      return end > 0 ? end : start + AltarRules.RUN_MS;
   }

   /** Đã hết giờ mà chưa chốt thưởng. */
   public boolean expiredUnsettled(long now) {
      return start > 0 && !isDone() && now >= endAt();
   }

   public synchronized boolean isDone() {
      return done;
   }

   /** Đánh dấu đã chốt thưởng; false = đã chốt rồi (chỉ 1 nơi được gửi thư — hẹn giờ / vào game / diệt hết). */
   public synchronized boolean tryFinish() {
      if (start == 0 || done) {
         return false;
      }
      done = true;
      return true;
   }
}
