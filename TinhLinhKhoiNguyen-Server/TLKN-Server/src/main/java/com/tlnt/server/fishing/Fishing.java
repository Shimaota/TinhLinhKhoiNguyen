package com.tlnt.server.fishing;

import com.tlnt.server.world.Zone;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 1 lần câu đang diễn ra của 1 người ({@code PlayerData.fishing}). Chỉ luồng session của người đó ghi; service khác
 * chỉ kiểm tra khác null để chặn thao tác (giao dịch, bán, thư, kí gửi, item...).
 *
 * <p>Thanh kéo cần (op 57 sub 2, client {@code EntityHpBarPanel}): kim chạy qua lại 0 -> 1 -> 0 ... mỗi lượt
 * {@link #period} ms. Vùng VÀNG (thanh trái client, ảnh ui/178-180) rộng; vùng XANH (thanh phải, ui/181-183) hẹp, nằm
 * trong vùng vàng và vẽ đè lên.
 */
public final class Fishing {

   public enum Phase {
      /** Đã quăng cần, chờ cá cắn. */
      WAITING,
      /** Cá cắn câu: đang hiện thanh kéo cần. */
      HOOKED
   }

   /** Kim đang ở vùng nào lúc bấm kéo cần. */
   public enum Hit { GREEN, YELLOW, MISS }

   final Zone zone;
   final float x;
   final float y;
   final long castAt;
   /** Lúc cá cắn câu (hiện thanh kéo cần). */
   final long biteAt;
   Phase phase = Phase.WAITING;
   /** Lúc gửi thanh kéo cần + hạn chót phải kéo. */
   long hookAt;
   long hookDeadline;
   int period;
   float yellowStart;
   float yellowEnd;
   float greenStart;
   float greenEnd;

   Fishing(Zone zone, float x, float y, long castAt, long biteAt) {
      this.zone = zone;
      this.x = x;
      this.y = y;
      this.castAt = castAt;
      this.biteAt = biteAt;
   }

   public Phase phase() {
      return phase;
   }

   /** Cá cắn: random tốc độ kim + vị trí 2 vùng. */
   void hook(long now) {
      ThreadLocalRandom r = ThreadLocalRandom.current();
      phase = Phase.HOOKED;
      hookAt = now;
      hookDeadline = now + FishingService.HOOK_WINDOW_MS;
      period = r.nextInt(FishingService.MIN_PERIOD_MS, FishingService.MAX_PERIOD_MS + 1);
      float yellowWidth = random(r, FishingService.YELLOW_MIN, FishingService.YELLOW_MAX);
      yellowStart = random(r, 0.08f, 0.92f - yellowWidth);
      yellowEnd = yellowStart + yellowWidth;
      float greenWidth = random(r, FishingService.GREEN_MIN, FishingService.GREEN_MAX);
      greenStart = random(r, yellowStart, yellowEnd - greenWidth);
      greenEnd = greenStart + greenWidth;
   }

   private static float random(ThreadLocalRandom r, float min, float max) {
      return max <= min ? min : min + r.nextFloat() * (max - min);
   }

   /** Vị trí kim (0..1) sau {@code elapsedMs} kể từ lúc hiện thanh — y hệt công thức client. */
   float markerAt(long elapsedMs) {
      if (elapsedMs < 0) {
         return 0f;
      }
      float t = (float) (elapsedMs % period) / period;
      return (elapsedMs / period) % 2 == 0 ? t : 1f - t;
   }

   /**
    * Kim ở vùng nào lúc người chơi bấm. Gói bấm tới server trễ 1 khoảng ping (không biết chính xác) -> xét mọi thời
    * điểm trong {@code [now - lagMs, now]}, lấy kết quả tốt nhất.
    */
   Hit judge(long now, long lagMs) {
      long elapsed = now - hookAt;
      Hit best = Hit.MISS;
      for (long e = elapsed; e >= Math.max(0, elapsed - lagMs); e -= 10) {
         float pos = markerAt(e);
         if (pos >= greenStart && pos <= greenEnd) {
            return Hit.GREEN;
         }
         if (pos >= yellowStart && pos <= yellowEnd) {
            best = Hit.YELLOW;
         }
      }
      return best;
   }
}
