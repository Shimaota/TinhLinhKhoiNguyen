package com.tlnt.server.world;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Số khu của từng lượt Tế đàn Thiên tích đang chạy: 1 lượt = 1 số khu (nhỏ nhất còn trống), dùng chung cả 7 map ->
 * vào map 1 khu 5 thì map 2 cũng khu 5. Chỉ trong RAM (server khởi động lại: lượt còn giờ nhận số mới, quái mới).
 */
final class AltarZones {

   private record Run(int zoneNo, long runId, long until) {
   }

   /** playerId -> lượt. Chỉ sửa dưới khoá {@code this}. */
   private final Map<Integer, Run> runs = new HashMap<>();

   /** Số khu của lượt {@code runId} (giữ tới {@code until}); lượt mới thì cấp số mới. */
   synchronized int zoneOf(int playerId, long runId, long until) {
      Run run = runs.get(playerId);
      if (run != null && run.runId == runId) {
         return run.zoneNo;
      }
      long now = System.currentTimeMillis();
      runs.values().removeIf(r -> r.until <= now); // lượt hết giờ mà chủ chưa vào lại (duyệt hết: 1 lần / lượt)
      runs.remove(playerId);
      BitSet used = new BitSet();
      for (Run r : runs.values()) {
         used.set(r.zoneNo);
      }
      int zoneNo = used.nextClearBit(0);
      runs.put(playerId, new Run(zoneNo, runId, until));
      return zoneNo;
   }

   synchronized void release(int playerId) {
      runs.remove(playerId);
   }
}
