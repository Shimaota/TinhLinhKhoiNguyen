package com.tlnt.server.daily;

import com.google.gson.annotations.SerializedName;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Nhiệm vụ hằng ngày của 1 nhân vật (cột JSON {@code player.daily_quest}):
 * <pre>
 * {"day":20736,"week":20734,"tasks":[{"id":"kill","k":"kill","t":8,"g":80,"p":12,"c":false,"n":"Đánh hạ Báo rừng Sayari"}],
 *  "add":1,"swap":2,"pts":14,"got":[1]}
 * </pre>
 * {@code day} / {@code week} = epochDay (giờ server) của hôm nay / thứ Hai tuần này: sang ngày mới -> danh sách NV, lượt
 * nhận thêm / đổi về 0 (dịch vụ giao lại {@link DailyConfig#baseTasks} NV); sang tuần mới -> điểm chăm chỉ + mốc đã nhận về 0.
 * Chỉ luồng session của chính người đó sửa ({@link DailyEvents} chuyển sự kiện về hàng đợi session); {@link #watch} đọc
 * được từ luồng tick khu để lọc nhanh.
 */
public final class DailyState {

   /** 1 nhiệm vụ đã giao. */
   public static final class Task {
      /** {@link DailyConfig.TaskDef#id}. */
      public String id;
      /** {@link DailyKind#key} (giữ riêng để cấu hình đổi vẫn đếm đúng NV đã giao). */
      public String k;
      /** Mục tiêu (mẫu quái / item / boss — theo loại). */
      public int t;
      /** Số cần làm. */
      public int g;
      /** Đã làm. */
      public int p;
      /** Đã nhận thưởng. */
      public boolean c;
      /** Tên hiện ở bảng (đã thay {@code {mob}}). */
      public String n;

      public Task() {
      }

      Task(String id, DailyKind kind, int target, int goal, String name) {
         this.id = id;
         this.k = kind.key;
         this.t = target;
         this.g = Math.max(1, goal);
         this.n = name;
      }

      public DailyKind kind() {
         return DailyKind.of(k);
      }

      public boolean done() {
         return p >= g;
      }
   }

   public int day;
   public long week;
   public List<Task> tasks = new ArrayList<>();
   /** Số lần "Nhận NV mới" hôm nay. */
   @SerializedName("add")
   public int added;
   /** Số lần "Đổi NV" hôm nay. */
   @SerializedName("swap")
   public int swaps;
   /** Điểm chăm chỉ tuần này. */
   @SerializedName("pts")
   public int points;
   /** id mốc chăm chỉ đã nhận tuần này. */
   @SerializedName("got")
   public Set<Integer> claimed = new TreeSet<>();

   /** Bit {@link DailyKind#bit} của các loại đang có NV chưa xong — luồng tick khu đọc để khỏi đẩy sự kiện thừa. */
   transient volatile long watch;

   /**
    * Sang ngày / tuần mới so với {@code today}: xoá NV + lượt (ngày), điểm + mốc (tuần).
    * @return true nếu sang ngày mới (cần giao NV mới)
    */
   boolean roll(LocalDate today) {
      long monday = today.with(DayOfWeek.MONDAY).toEpochDay();
      if (week != monday) {
         week = monday;
         points = 0;
         claimed.clear();
      }
      if (day == today.toEpochDay()) {
         return false;
      }
      day = (int) today.toEpochDay();
      tasks.clear();
      added = 0;
      swaps = 0;
      refreshWatch();
      return true;
   }

   boolean hasTask(String id) {
      for (Task t : tasks) {
         if (id.equals(t.id)) {
            return true;
         }
      }
      return false;
   }

   /** Tính lại {@link #watch} sau khi danh sách / tiến độ đổi. */
   void refreshWatch() {
      long bits = 0;
      for (Task t : tasks) {
         DailyKind kind = t.kind();
         if (kind != null && !t.done()) {
            bits |= kind.bit();
         }
      }
      watch = bits;
   }

   /** JSON cũ thiếu field -> rỗng. */
   public DailyState normalize() {
      if (tasks == null) {
         tasks = new ArrayList<>();
      }
      tasks.removeIf(t -> t == null || t.id == null || t.kind() == null);
      claimed = claimed == null ? new TreeSet<>() : new TreeSet<>(claimed);
      refreshWatch();
      return this;
   }
}
