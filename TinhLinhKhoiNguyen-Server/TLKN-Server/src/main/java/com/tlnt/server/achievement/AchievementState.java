package com.tlnt.server.achievement;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tiến độ thành tựu của 1 nhân vật (cột JSON {@code player.achievement}: {@code {"c":{"revive":126},"got":[19,20]}}).
 * Chỉ luồng session của chính người đó sửa ({@link AchievementEvents} chuyển sự kiện về hàng đợi session).
 */
public final class AchievementState {

   /** {@link AchievementType#key} -> bộ đếm (chuỗi max: giá trị cao nhất). */
   @SerializedName("c")
   public Map<String, Long> counts = new HashMap<>();
   /** id thành tựu đã nhận quà. */
   @SerializedName("got")
   public Set<Integer> claimed = new TreeSet<>();

   public long get(AchievementType type) {
      return counts == null ? 0 : counts.getOrDefault(type.key, 0L);
   }

   /** Cộng dồn (chuỗi đếm) hoặc giữ giá trị lớn hơn (chuỗi max). @return có đổi */
   boolean apply(AchievementType type, long value) {
      if (value <= 0) {
         return false;
      }
      long old = get(type);
      long now = type.max ? Math.max(old, value) : old + value;
      if (now == old) {
         return false;
      }
      counts.put(type.key, now);
      return true;
   }

   boolean isClaimed(int id) {
      return claimed != null && claimed.contains(id);
   }

   public boolean isEmpty() {
      return (counts == null || counts.isEmpty()) && (claimed == null || claimed.isEmpty());
   }

   /** JSON cũ thiếu field -> rỗng. */
   public AchievementState normalize() {
      if (counts == null) {
         counts = new HashMap<>();
      }
      claimed = claimed == null ? new TreeSet<>() : new TreeSet<>(claimed);
      return this;
   }
}
