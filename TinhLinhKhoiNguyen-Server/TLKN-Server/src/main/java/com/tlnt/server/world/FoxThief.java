package com.tlnt.server.world;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Trạng thái 1 con mini boss Tiểu Tinh linh Cáo ({@code worldboss/FoxService}): sống qua các lần đổi map / khu (vàng đã
 * trộm, mỗi người bị trộm bao nhiêu, đã báo 1 triệu chưa). Boss trên khu = {@link AltarBoss} có {@link AltarBoss#thief}
 * khác null — AI ở {@code AltarBoss.thiefTick}. CHỈ luồng tick của khu đang chứa con cáo đụng tới (đổi khu = chuyển qua
 * hàng đợi khu mới).
 *
 * <pre>
 * LANG_THANG -> TÌM người đứng quanh 1 chỗ (khu hiện tại; rảnh {@link #SEEK_AFTER_MS} thì sang khu / map có người đứng im — {@code FoxService}) ≥ {@link #STILL_MS} (đánh quái tại chỗ vẫn tính) -> TIẾP CẬN (người đó đi thì
 * đuổi theo) -> TRỘM mỗi {@link #STEAL_INTERVAL_MS} 1% vàng khoá (mỗi người tối đa {@link #PER_PLAYER_MAX}, đủ thì đổi
 * mục tiêu) -> BỊ ĐÁNH = bị phát hiện -> CHẠY trốn; {@link #ESCAPE_MS} không hạ được -> sang map + khu khác, hồi đầy HP
 * -> LANG_THANG. Đủ {@link #TOTAL_MAX}: báo thế giới 1 lần, thôi trộm, bỏ sang map khác; bị đánh mà
 * {@link #RICH_ESCAPE_MS} không hạ được thì lại đổi map. Mỗi đòn lên cáo tối đa {@link #MAX_DAMAGE} HP.
 * </pre>
 */
public final class FoxThief {

   public static final long STEAL_INTERVAL_MS = 10_000;
   /** Trộm mỗi lần: 1% vàng khoá hiện có của mục tiêu (tối thiểu 1). */
   public static final int STEAL_PERCENT = 1;
   public static final long PER_PLAYER_MAX = 100_000;
   /** Mỗi lần trộm tối đa bấy nhiêu. */
   public static final long PER_STEAL_MAX = 10_000;
   /** Rảnh (khu hiện tại không có ai để trộm) bấy lâu thì đi tìm mục tiêu ở khu / map khác. */
   public static final long SEEK_AFTER_MS = 15_000;
   public static final long TOTAL_MAX = 1_000_000;
   public static final long ESCAPE_MS = 5 * 60_000;
   public static final long RICH_ESCAPE_MS = 2 * 60_000;
   public static final long MAX_DAMAGE = 10;
   /** Đứng quanh 1 chỗ (lệch ≤ {@link #STILL_RADIUS_PX}) bấy lâu = "đứng im" (treo máy / đánh quái tại chỗ). */
   public static final long STILL_MS = 10_000;
   public static final float STILL_RADIUS_PX = 300f;
   /** Đứng cách mục tiêu bấy nhiêu thì trộm được. */
   public static final float STEAL_X_PX = 250f;
   public static final float STEAL_Y_PX = 200f;
   /** Đang trốn: người trong tầm này thì chạy ra xa. */
   public static final float FLEE_TRIGGER_PX = 1_200f;
   /** Số đống vàng rơi khi chết. */
   public static final int GOLD_PILES = 10;
   /** Chạy trốn liền bấy lâu thì đứng thở {@link #REST_MS} (người cận chiến đuổi kịp). */
   public static final long RUN_MS = 4_000;
   public static final long REST_MS = 2_000;
   static final String TIRED_CHAT = "Hộc hộc... mệt quá!";
   /** Vừa nhắm được người để trộm (bắt đầu tới tìm). */
   static final String TARGET_CHAT = "Hí hí... ta hửi thấy mùi xiền...!";
   /** Mỗi lần trộm được. */
   static final String STEAL_CHAT = "Ngon luôn! Cứ tiền là ta thích...!";
   /** Lần đầu bị đánh ở khu này (bị phát hiện). */
   static final String DETECTED_CHAT = "A! Bị phát hiện rồi, chạy thôi!";

   static final String ANNOUNCE = "Tiểu Tinh linh Cáo đã lấy trộm 1 triệu vàng của dân làng các dũng sĩ hãy truy lùng nó.";

   /** Số thứ tự con cáo (0..4) — để dịch vụ biết con nào. */
   public final int slot;
   /** Tổng vàng đã trộm (rơi ra khi chết). */
   long total;
   /** id người chơi -> đã trộm của người đó. */
   final Map<Integer, Long> stolenFrom = new HashMap<>();
   /** Đã báo thế giới mốc 1 triệu. */
   boolean announced;
   /** Lần đầu bị đánh ở khu hiện tại (0 = chưa bị phát hiện). */
   long detectedAt;
   /** Đủ 1 triệu: phải bỏ sang map khác ngay (1 lần). */
   boolean leaveNow;
   /** Người đang nhắm (0 = không ai) + lúc trộm lần kế. */
   int targetId;
   long nextStealAt;
   /** Bắt đầu rảnh lúc này (0 = đang bận: có mục tiêu / đang trốn). */
   long idleSince;
   /** id người chơi -> {x, y, lúc bắt đầu đứng ở đó} (chỉ người trong khu hiện tại). */
   final Map<Integer, float[]> anchors = new HashMap<>();
   private final Map<Integer, Long> anchorSince = new HashMap<>();

   public FoxThief(int slot) {
      this.slot = slot;
   }

   public long total() {
      return total;
   }

   /** Đủ 1 triệu: thôi trộm. */
   boolean full() {
      return total >= TOTAL_MAX;
   }

   /** Đã trộm của {@code playerId} đủ {@link #PER_PLAYER_MAX} chưa. */
   public boolean done(int playerId) {
      return stolenFrom.getOrDefault(playerId, 0L) >= PER_PLAYER_MAX;
   }

   /**
    * Số vàng trộm 1 lần từ người có {@code lockedGold} vàng khoá, đã trộm của người đó {@code takenFromPlayer}: 1%
    * (tối thiểu 1), không quá phần còn lại của 100k / người và 1 triệu tổng.
    */
   long stealAmount(long lockedGold, long takenFromPlayer) {
      if (lockedGold <= 0) {
         return 0;
      }
      long amount = Math.min(PER_STEAL_MAX, Math.max(1, lockedGold * STEAL_PERCENT / 100));
      amount = Math.min(amount, PER_PLAYER_MAX - takenFromPlayer);
      amount = Math.min(amount, TOTAL_MAX - total);
      return Math.max(0, Math.min(amount, lockedGold));
   }

   /** Ghi đã trộm {@code amount} của {@code playerId}. @return true nếu VỪA chạm mốc 1 triệu */
   boolean record(int playerId, long amount) {
      boolean before = full();
      total += amount;
      stolenFrom.merge(playerId, amount, Long::sum);
      return !before && full();
   }

   /** Bị đánh: lần đầu ở khu này thì bắt đầu đếm giờ bỏ trốn. */
   void hit(long now) {
      if (detectedAt == 0) {
         detectedAt = now;
      }
      targetId = 0;
   }

   /** Tới lúc bỏ sang map khác: đủ 1 triệu (ngay) hoặc bị phát hiện quá {@link #ESCAPE_MS} / {@link #RICH_ESCAPE_MS}. */
   boolean relocateDue(long now) {
      if (leaveNow) {
         return true;
      }
      return detectedAt > 0 && now - detectedAt >= (full() ? RICH_ESCAPE_MS : ESCAPE_MS);
   }

   /** Vừa sang khu mới: quên chuyện ở khu cũ (vàng đã trộm giữ nguyên). */
   void moved() {
      detectedAt = 0;
      leaveNow = false;
      targetId = 0;
      nextStealAt = 0;
      idleSince = 0;
      anchors.clear();
      anchorSince.clear();
   }

   /**
    * Cập nhật chỗ đứng của 1 người (gọi mỗi tick cho mọi người trong khu): đi xa chỗ cũ quá {@link #STILL_RADIUS_PX} thì
    * đặt lại mốc. @return đã đứng quanh 1 chỗ ≥ {@link #STILL_MS}
    */
   boolean track(int playerId, float x, float y, long now) {
      float[] a = anchors.get(playerId);
      if (a == null || Math.abs(x - a[0]) > STILL_RADIUS_PX || Math.abs(y - a[1]) > STILL_RADIUS_PX) {
         anchors.put(playerId, new float[] {x, y});
         anchorSince.put(playerId, now);
         return false;
      }
      return now - anchorSince.getOrDefault(playerId, now) >= STILL_MS;
   }

   /** Bỏ mốc của người không còn trong khu. */
   void forgetExcept(java.util.Set<Integer> present) {
      for (Iterator<Integer> it = anchors.keySet().iterator(); it.hasNext();) {
         int id = it.next();
         if (!present.contains(id)) {
            it.remove();
            anchorSince.remove(id);
         }
      }
   }

   /** Chia {@link #total} thành {@link #GOLD_PILES} đống (đống đầu nhận phần dư); không có vàng -> rỗng. */
   public long[] piles() {
      if (total <= 0) {
         return new long[0];
      }
      int n = (int) Math.min(GOLD_PILES, total);
      long[] out = new long[n];
      long each = total / n;
      for (int i = 0; i < n; i++) {
         out[i] = each;
      }
      out[0] += total - each * n;
      return out;
   }
}
