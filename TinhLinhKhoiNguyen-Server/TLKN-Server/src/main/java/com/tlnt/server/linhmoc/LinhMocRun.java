package com.tlnt.server.linhmoc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1 lượt Rừng Linh Mộc của 1 nhóm — CHỈ RAM ({@code LinhMocService}; tắt máy chủ là mất lượt, lượt đã trừ vẫn tính).
 *
 * <p>Quyền vào theo {@link #memberIds} (chụp lúc mở), KHÔNG theo tổ đội: rời / giải tán / chuyển trưởng nhóm không ảnh
 * hưởng, người mới vào tổ đội không vào được. {@link #mobLevel} / {@link #unit} khoá tới hết lượt (lên cấp, thoát /
 * vào lại không tính lại).
 *
 * <p>Luồng: tạo trên luồng session của trưởng nhóm; luồng tick khu đọc ({@link #active}, {@link #end}); dọn map / kết
 * thúc qua các method {@code synchronized}.
 */
public final class LinhMocRun {

   public enum State { RUNNING, SUCCESS, FAILED }

   /** Mã lượt (tăng dần, duy nhất trong 1 lần chạy máy chủ) — khoá khu riêng của lượt ở từng map. */
   public final long id;
   public final Set<Integer> memberIds;
   /** Tên thành viên lúc mở (gửi thư quà cả khi họ đã thoát game). */
   public final java.util.Map<Integer, String> names;
   public final int mobLevel;
   public final double unit;
   public final long start;
   /** Giờ hết lượt (epoch ms). Lệnh test {@code /linhmoc het} rút ngắn. */
   public volatile long end;
   /** Số khu hiển thị (giống nhau ở mọi map của lượt). */
   public final int zoneNo;
   private volatile State state = State.RUNNING;
   /** Map đã diệt sạch quái. Dưới khoá {@code this}. */
   private final Set<Integer> cleared = new HashSet<>();

   public LinhMocRun(long id, Set<Integer> memberIds, int mobLevel, double unit, long start, long end, int zoneNo) {
      this(id, memberIds, java.util.Map.of(), mobLevel, unit, start, end, zoneNo);
   }

   public LinhMocRun(long id, Set<Integer> memberIds, java.util.Map<Integer, String> names, int mobLevel, double unit,
         long start, long end, int zoneNo) {
      this.names = java.util.Map.copyOf(names);
      this.id = id;
      this.memberIds = Set.copyOf(memberIds);
      this.mobLevel = mobLevel;
      this.unit = unit;
      this.start = start;
      this.end = end;
      this.zoneNo = zoneNo;
   }

   /** Còn vào được: đang chạy và chưa hết giờ. */
   public boolean active(long now) {
      return state == State.RUNNING && now < end;
   }

   public State state() {
      return state;
   }

   /** Chuyển sang kết thúc (1 lần); false = đã kết thúc rồi. */
   public synchronized boolean finish(State result) {
      if (state != State.RUNNING || result == State.RUNNING) {
         return false;
      }
      state = result;
      return true;
   }

   /** Đánh dấu map đã diệt sạch; false = đã đánh dấu rồi. */
   public synchronized boolean markCleared(int mapId) {
      return cleared.add(mapId);
   }

   public synchronized boolean isCleared(int mapId) {
      return cleared.contains(mapId);
   }

   public synchronized int clearedCount() {
      return cleared.size();
   }

   /** Đã diệt sạch quái mọi map trong {@code maps}. */
   public synchronized boolean allCleared(List<Integer> maps) {
      return cleared.containsAll(maps);
   }
}
