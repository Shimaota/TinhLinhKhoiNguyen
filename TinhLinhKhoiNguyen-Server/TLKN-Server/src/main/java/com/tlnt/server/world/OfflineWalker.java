package com.tlnt.server.world;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Chân của nhân vật đang Luyện tập Offline ({@code offline/OfflineTrainingService}): server tự CHẠY nhân vật trên mặt đất
 * tới quái / đồ rơi như người chơi thật — chạy ngang, nhảy lên bậc, rơi khỏi mép ({@link BossMover} + {@link NavGrid}, cùng
 * trọng lực boss / thú cưng), tốc chạy = tốc chạy thật của nhân vật — thay vì dịch chuyển (trông như hack). Vị trí đi qua
 * đường phát gộp op 5 bình thường của khu, người xung quanh thấy nhân vật chạy + nội suy như mọi người khác.
 *
 * <p>Đích không tới được (tường cao, tầng không có đường) quá {@link #STUCK_MS} không lại gần thêm -> bỏ con đó
 * {@link #UNREACHABLE_MS}, chọn con khác. CHỈ luồng tick của khu đụng tới.
 */
final class OfflineWalker {

   /** Tới đích khi cách bấy nhiêu px. */
   static final float ARRIVE_PX = 20f;
   /** Đồ rơi của mình trong bán kính này (nhưng ngoài tầm tự nhặt) -> chạy tới nhặt trước khi đánh tiếp. */
   static final float LOOT_SEEK_PX = 1_000f;
   /** Không lại gần đích thêm {@link #PROGRESS_PX} trong bấy lâu = kẹt. */
   static final long STUCK_MS = 4_000;
   static final float PROGRESS_PX = 25f;
   static final long UNREACHABLE_MS = 30_000;
   static final float MIN_RUN_SPEED = 450f;
   /** Tầm ngang tối thiểu (client {@code AttackReach.MIN_HORIZONTAL} 0,3 m). */
   static final float MIN_HORIZONTAL_PX = 30f;
   /** Tầm dọc cộng thêm (client {@code AttackReach.VERTICAL_SLACK} 1 m — server cho 200 px). */
   static final float VERTICAL_SLACK_PX = 100f;
   /** Chiêu cận chiến: rangeY ≤ bấy nhiêu (client {@code AttackReach.MELEE_MAX_RANGE_Y}). */
   static final int MELEE_MAX_RANGE_Y = 150;
   /** Cận chiến đánh quái THẤP hơn mình: chỉ khi lệch ≤ bấy nhiêu (phải xuống tầng của quái — client 0,5 m). */
   static final float MELEE_BELOW_PX = 50f;
   /** Đứng lùi vào trong tầm ngang (client {@code InteractionController.APPROACH_FACTOR}). */
   static final float APPROACH_FACTOR = 0.8f;
   /** Nhảy cao nhất (nhảy kép, như bục cao nhất {@link NavGrid#MAX_JUMP_PX} bot vẫn nhảy lên được). */
   static final float MAX_HOP_PX = NavGrid.MAX_JUMP_PX;
   /** Quá bấy nhiêu thì tách làm 2 lần nhảy (lần 2 ở đỉnh lần 1) — trông như người chơi nhảy kép. */
   static final float SINGLE_HOP_PX = 300f;
   static final float HOP_MARGIN_PX = 40f;
   /** Nhảy bấy nhiêu lần vẫn không với tới -> bỏ con này. */
   static final int MAX_HOPS_PER_TARGET = 3;

   /** Chỗ đứng lúc bắt đầu treo = gốc "Phạm vi đánh" (decompile: chỗ bật tự đánh C3418.f12233). */
   float anchorX;
   float anchorY;
   BossMover mover;
   boolean walking;
   float goalX;
   float goalY;
   /** Quái đang nhắm (đánh / chạy tới), -1 = không — bot khác né con này ({@code OfflineQuery.spread}). */
   int targetMob = -1;
   /** Id quái đang chạy tới (-1 = đang chạy tới đồ rơi / không có). */
   int goalMob = -1;
   int goalDrop = -1;
   long lastTickAt;
   /** Đang bay vì bị boss đẩy / hất ({@code Zone.knockNoClient}): mỗi tick {@link BossMover#flyKnocked} tới khi chạm đất. */
   boolean knocked;
   float bestDistance = Float.MAX_VALUE;
   long lastProgressAt;

   // ---- đang nhảy lên đánh quái cao hơn (luồng tick khu kiểm tra tầm MỖI TICK, tới tầm là ra chiêu ngay trên không)
   int hopMob = -1;
   int hops;
   /** Lần nhảy 2 (ở đỉnh lần 1), 0 = không. */
   float secondHop;
   boolean hopFired;
   World.OfflineQuery hopQuery;
   World.OfflineReply hopReply;

   /** Quái / đồ rơi không tới được: id -> hết hạn bỏ qua (đồ rơi lưu âm: -id - 1). */
   private final Map<Integer, Long> unreachable = new HashMap<>();

   /**
    * "Phạm vi đánh" như decompile C3541: quái cách gốc ({@code ax}, {@code ay}) ≤ {@code range} px (khoảng cách thẳng);
    * {@code range} ≤ 0 = Không giới hạn.
    */
   static boolean inRange(float x, float y, float ax, float ay, int range) {
      if (range <= 0) {
         return true;
      }
      float dx = x - ax;
      float dy = y - ay;
      return dx * dx + dy * dy <= (float) range * range;
   }

   /**
    * Kỹ năng nào với tới quái ở ({@code mx}, {@code my}) từ ({@code px}, {@code py}) — Y HỆT client tự đánh
    * ({@code AttackReach.inReach}): |dx| ≤ max(30, rangeX), |dy| ≤ rangeY + 100; cận chiến (rangeY ≤ 150) đánh quái thấp
    * hơn chỉ khi lệch ≤ 50. Chặt hơn server (+ lượn + 200 px) -> không "tay ngắn đánh xa". @return bit i = kỹ năng i
    */
   static int reachMask(float px, float py, float mx, float my, int[] rangeX, int[] rangeY) {
      float dx = Math.abs(px - mx);
      float dy = my - py;
      int mask = 0;
      for (int i = 0; i < rangeX.length && i < 31; i++) {
         if (dx <= horizontal(rangeX[i]) && Math.abs(dy) <= verticalLimit(dy, rangeY[i])) {
            mask |= 1 << i;
         }
      }
      return mask;
   }

   static float horizontal(int rangeX) {
      return Math.max(MIN_HORIZONTAL_PX, rangeX);
   }

   /** Lệch dọc tối đa còn đánh được; {@code dy} = quái − mình (âm = quái thấp hơn). */
   static float verticalLimit(float dy, int rangeY) {
      float limit = rangeY + VERTICAL_SLACK_PX;
      return rangeY <= MELEE_MAX_RANGE_Y && dy < 0 ? Math.min(limit, MELEE_BELOW_PX) : limit;
   }

   /** Chỗ đứng cách quái theo chiều ngang: 0,8 × tầm ngang NHỎ nhất (mọi chiêu đều với tới). */
   static float standOff(int[] rangeX) {
      float min = Float.MAX_VALUE;
      for (int r : rangeX) {
         min = Math.min(min, horizontal(r));
      }
      return min == Float.MAX_VALUE ? MIN_HORIZONTAL_PX : min * APPROACH_FACTOR;
   }

   /**
    * Quái cao hơn tầm dọc mọi chiêu: cần nhảy lên thêm bao nhiêu px (+ dư); ≤ 0 = khỏi nhảy, > {@link #MAX_HOP_PX} = nhảy
    * kép cũng không tới.
    */
   static float hopNeeded(float py, float my, int[] rangeY) {
      float best = Float.MAX_VALUE;
      for (int r : rangeY) {
         best = Math.min(best, my - py - (r + VERTICAL_SLACK_PX));
      }
      return best == Float.MAX_VALUE ? 0 : best + HOP_MARGIN_PX;
   }

   /** Bắt đầu nhảy lên đánh {@code mobId} cao {@code height} px (> {@link #SINGLE_HOP_PX}: 2 lần). @return true nếu nhảy */
   boolean startHop(int mobId, float height, World.OfflineQuery q, World.OfflineReply reply) {
      if (mover == null) {
         return false;
      }
      float first = height > SINGLE_HOP_PX ? height / 2 : height;
      if (!mover.hop(first)) {
         return false;
      }
      hops = hopMob == mobId ? hops + 1 : 1;
      hopMob = mobId;
      secondHop = height > SINGLE_HOP_PX ? height - first : 0;
      hopFired = false;
      hopQuery = q;
      hopReply = reply;
      return true;
   }

   void endHop() {
      hopQuery = null;
      hopReply = null;
      secondHop = 0;
   }

   /** Đứng đúng chỗ mà không chiêu nào với tới (quái bay quá cao): bỏ con này {@link #UNREACHABLE_MS}. */
   void markUnreachable(int mobId, long now) {
      unreachable.put(mobId, now + UNREACHABLE_MS);
      hopMob = -1;
      hops = 0;
      endHop();
      stop();
   }

   void goTo(float x, float y, int mob, int drop, long now) {
      if (!walking || goalMob != mob || goalDrop != drop || Math.abs(goalX - x) > 1 || Math.abs(goalY - y) > 1) {
         bestDistance = Float.MAX_VALUE;
         lastProgressAt = now;
      }
      walking = true;
      goalX = x;
      goalY = y;
      goalMob = mob;
      goalDrop = drop;
   }

   /**
    * Bị khống chế / văng (không tự đi được): tính lại "tiến độ" từ bây giờ — không thì lúc đáp đất xa đích hơn trước khi
    * bị đẩy, {@link #checkStuck} coi là kẹt và bỏ mục tiêu {@link #UNREACHABLE_MS} (bot bị boss hất ra rồi không đánh tiếp).
    */
   void resetProgress(long now) {
      bestDistance = Float.MAX_VALUE;
      lastProgressAt = now;
   }

   void stop() {
      walking = false;
      goalMob = -1;
      goalDrop = -1;
   }

   boolean skipMob(int mobId, long now) {
      return skip(mobId, now);
   }

   boolean skipDrop(int dropId, long now) {
      return skip(-dropId - 1, now);
   }

   private boolean skip(int key, long now) {
      Long until = unreachable.get(key);
      if (until == null) {
         return false;
      }
      if (now >= until) {
         unreachable.remove(key);
         return false;
      }
      return true;
   }

   /** Ghi nhận khoảng cách tới đích; kẹt quá lâu -> bỏ đích này. @return true nếu vừa bỏ */
   boolean checkStuck(float distance, long now) {
      if (distance < bestDistance - PROGRESS_PX) {
         bestDistance = distance;
         lastProgressAt = now;
         return false;
      }
      if (now - lastProgressAt < STUCK_MS) {
         return false;
      }
      unreachable.put(goalMob >= 0 ? goalMob : -goalDrop - 1, now + UNREACHABLE_MS);
      if (unreachable.size() > 64) { // dọn mục đã hết hạn
         for (Iterator<Long> it = unreachable.values().iterator(); it.hasNext(); ) {
            if (it.next() <= now) {
               it.remove();
            }
         }
      }
      stop();
      return true;
   }
}
