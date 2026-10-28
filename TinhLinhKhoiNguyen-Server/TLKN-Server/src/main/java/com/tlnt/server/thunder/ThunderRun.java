package com.tlnt.server.thunder;

/**
 * 1 lượt Lôi vực Sấm sét đang chạy — CHỈ RAM ({@code PlayerData.thunderRun}): thoát game / rời map là hết lượt (lượt đã
 * trừ lúc vào). {@link #deaths} chỉ luồng tick khu sửa; điểm thưởng + Ấn linh chỉ luồng session sửa.
 */
public final class ThunderRun {

   public final long start;
   /** Giờ hết lượt (lệnh test /loivuc het đổi được). */
   public volatile long end;
   public final int mapId;
   /** Số lần bị hạ gục trong lượt (luồng tick khu). */
   public volatile int deaths;
   /** Đã Ấn linh (luồng session). */
   public volatile boolean sealed;
   /** Điểm cống hiến / hoạt động đã nhận từ hạ gục người trong lượt (luồng session). */
   int killTribePoints;
   int killDynamicPoints;
   /** Đã kết thúc (rời map / hết giờ / bị loại). */
   private volatile boolean finished;

   public ThunderRun(long start, int mapId) {
      this(start, start + ThunderRules.RUN_MS, mapId);
   }

   ThunderRun(long start, long end, int mapId) {
      this.start = start;
      this.end = end;
      this.mapId = mapId;
   }

   /** Đã kết thúc hoặc hết giờ. */
   public boolean over() {
      return finished || System.currentTimeMillis() >= end;
   }

   public boolean finished() {
      return finished;
   }

   void finish() {
      finished = true;
   }

   /** Lần bị hạ gục này (đã đếm) là lần bị loại. */
   public boolean eliminated() {
      return deaths > ThunderRules.MAX_DEATHS;
   }
}
