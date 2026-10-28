package com.tlnt.server.world;

import java.util.HashMap;
import java.util.Map;

/**
 * Tổng sát thương từng người chơi đã gây lên 1 boss (Rừng Linh Mộc: quà boss chỉ cho người gây nhiều nhất). CHỈ luồng
 * tick của khu đụng tới.
 */
final class DamageLedger {

   private final Map<Integer, Long> byPlayer = new HashMap<>();
   /** Đã phát quà (boss chết có nhiều đường — đòn, chảy máu, ảnh ảo, lệnh test — chỉ phát 1 lần). */
   boolean rewarded;

   /** Tên người đã đánh (boss thế giới: thông báo / thư khi người gây nhiều nhất đã rời khu). */
   private final Map<Integer, String> names = new HashMap<>();

   void add(int playerId, long damage) {
      if (damage > 0) {
         byPlayer.merge(playerId, damage, Long::sum);
      }
   }

   /** Như {@link #add(int, long)} + nhớ tên. */
   void add(int playerId, String name, long damage) {
      if (damage > 0) {
         byPlayer.merge(playerId, damage, Long::sum);
         if (name != null) {
            names.putIfAbsent(playerId, name);
         }
      }
   }

   /** Mọi người đã gây sát thương (> 0). */
   java.util.Set<Integer> players() {
      return byPlayer.keySet();
   }

   /** Tên đã nhớ của {@code playerId} ("" = không biết). */
   String name(int playerId) {
      return names.getOrDefault(playerId, "");
   }

   /** Chép toàn bộ sổ (sát thương + tên) của {@code other} — boss bị admin dời sang khu khác. */
   void copyFrom(DamageLedger other) {
      byPlayer.putAll(other.byPlayer);
      names.putAll(other.names);
   }

   /** Admin: đặt lại sát thương của {@code playerId} ({@code damage} ≤ 0 = xoá khỏi sổ). */
   void set(int playerId, long damage) {
      if (damage <= 0) {
         byPlayer.remove(playerId);
      } else {
         byPlayer.put(playerId, damage);
      }
   }

   /** Bảng xếp hạng: [id, sát thương] giảm dần (bằng nhau: id nhỏ trước). */
   java.util.List<long[]> ranking() {
      java.util.List<long[]> out = new java.util.ArrayList<>(byPlayer.size());
      byPlayer.forEach((id, d) -> out.add(new long[] {id, d}));
      out.sort((a, b) -> a[1] != b[1] ? Long.compare(b[1], a[1]) : Long.compare(a[0], b[0]));
      return out;
   }

   long of(int playerId) {
      return byPlayer.getOrDefault(playerId, 0L);
   }

   /** Người gây nhiều sát thương nhất (bằng nhau: id nhỏ hơn); 0 = chưa ai đánh. */
   int top() {
      return topAmong(id -> true);
   }

   /** Như {@link #top} nhưng chỉ xét người thoả {@code eligible} (vd còn ở khu); 0 = không ai. */
   int topAmong(java.util.function.IntPredicate eligible) {
      int best = 0;
      long bestDamage = 0;
      for (Map.Entry<Integer, Long> e : byPlayer.entrySet()) {
         if (!eligible.test(e.getKey())) {
            continue;
         }
         long d = e.getValue();
         if (d > bestDamage || d == bestDamage && d > 0 && e.getKey() < best) {
            best = e.getKey();
            bestDamage = d;
         }
      }
      return best;
   }
}
