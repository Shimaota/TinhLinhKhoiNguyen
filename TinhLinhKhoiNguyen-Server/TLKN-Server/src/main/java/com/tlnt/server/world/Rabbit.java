package com.tlnt.server.world;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ngọc thố (sự kiện Trung thu, {@code event/RabbitService}): người chơi giả char_template 21 do {@link AltarBoss} điều
 * khiển ở chế độ thỏ — chạy qua chạy lại khắp map, KHÔNG đánh ai, KHÔNG chết (hiện thanh HP 1.000 nhưng mọi đòn = 0).
 * Người chơi đứng gần dùng Cà rốt -> củ cà rốt "ném" ra trước mặt họ ({@link #addCarrot}) — hiện bằng vật trang trí
 * op 81 (icon Cà rốt trên mặt đất, client {@code MapPropEntity}); thỏ chạy tới (tốc như thú cưng {@link #RUN_SPEED}) ăn
 * từng củ theo thứ tự ném (op 81 sub 2 gỡ củ), chat {@link #EAT_CHAT}, rơi quà riêng cho người ném + điểm sự kiện. Ăn
 * đủ {@link #MAX_EATEN} củ thì chạy sang map khác (củ còn lại mất). CHỈ luồng tick khu đụng tới (trừ {@link #slot}).
 */
public final class Rabbit {

   public static final String NAME = "Ngọc thố";
   public static final String EAT_CHAT = "Măm măm";
   public static final int MAX_EATEN = 3;
   /** Đứng ăn bấy lâu rồi mới rơi quà. */
   static final long EAT_MS = 1_500;
   /** Củ cà rốt nằm đất quá lâu (thỏ không tới được) thì mất. */
   static final long CARROT_TTL_MS = 30_000;
   /** Đứng cách thỏ trong khoảng này mới ném được. */
   public static final float FEED_RANGE_X = 800f;
   public static final float FEED_RANGE_Y = 400f;
   /** Tới gần củ cà rốt chừng này là ăn. */
   static final float EAT_REACH_PX = 60f;
   /** Tốc chạy px / s = thú cưng game gốc (4,5 px × 60 tick). */
   public static final float RUN_SPEED = 270f;
   /** Đi dạo như thú cưng: mỗi lượt đi 100..540 px rồi nghỉ 0,8..2,5 s. */
   static final float WANDER_MIN_PX = 100f;
   static final float WANDER_MAX_PX = 540f;
   static final long WANDER_PAUSE_MIN_MS = 800;
   static final long WANDER_PAUSE_MAX_MS = 2_500;
   /** Củ ném ra trước mặt người ném bấy nhiêu px (đặt trên mặt đất). */
   static final float THROW_AHEAD_PX = 90f;
   /** Id vật trang trí op 81 của củ cà rốt (số âm, riêng từng củ). */
   private static final AtomicInteger CARROT_IDS = new AtomicInteger();

   /** Ăn xong củ cuối: đứng thêm bấy lâu rồi mới chạy sang map khác. */
   static final long LEAVE_DELAY_MS = 2_000;

   /** 1 củ cà rốt dưới đất. */
   record Carrot(int id, float x, float y, int ownerId, long thrownAt) {
   }

   /** Ảnh củ cà rốt gốc trong client; item template id 351 không phải id của texture. */
   static String carrotTexture() {
      return "icon/939";
   }

   public final int slot;
   final ArrayDeque<Carrot> carrots = new ArrayDeque<>();
   int eaten;
   long eatUntil;
   Carrot eating;
   boolean leaving;

   public Rabbit(int slot) {
      this.slot = slot;
   }

   /** Còn nhận cà rốt (chưa ăn đủ / chưa bỏ đi) — vẫn nhận khi đã có củ chờ: người ném sau có thể mất củ. */
   boolean accepting() {
      return !leaving && eaten < MAX_EATEN;
   }

   Carrot addCarrot(float x, float y, int ownerId, long now) {
      int id = -1 - (CARROT_IDS.getAndIncrement() & 0x3FFFFFFF);
      Carrot c = new Carrot(id, x, y, ownerId, now);
      carrots.addLast(c);
      return c;
   }

   /** Củ kế tiếp còn hạn; củ quá hạn bị bỏ và báo cho {@code expired} (gỡ khỏi client). */
   Carrot next(long now, java.util.function.IntConsumer expired) {
      while (!carrots.isEmpty() && now - carrots.peekFirst().thrownAt() > CARROT_TTL_MS) {
         expired.accept(carrots.pollFirst().id());
      }
      return carrots.peekFirst();
   }
}
