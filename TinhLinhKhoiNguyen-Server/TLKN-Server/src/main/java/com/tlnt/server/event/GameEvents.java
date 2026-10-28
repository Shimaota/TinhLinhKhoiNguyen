package com.tlnt.server.event;

import com.tlnt.server.util.Log;

import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Công tắc sự kiện toàn server (admin panel tab "Sự kiện" / lệnh console {@code event}). Cờ lưu ở
 * {@code tlnt_server.server_state} (khoá {@link Event#stateKey}), nạp lúc khởi động. Tắt = mọi thứ của sự kiện biến mất
 * (NPC, cây, quái / boss, rơi đồ, cửa hàng...) — mỗi hệ thống đọc {@link #active} hoặc nghe {@link #onChange}.
 *
 * <p>Cấu hình dữ liệu (cây / boss thế giới) gắn sự kiện bằng khoá JSON {@code "event":"trungthu"} = {@link Event#key}.
 */
public final class GameEvents {

   public enum Event {
      MID_AUTUMN("trungthu", "Trung thu");

      /** Mã dùng trong JSON cấu hình ({@code "event":"trungthu"}) và lệnh console. */
      public final String key;
      public final String title;

      Event(String key, String title) {
         this.key = key;
         this.title = title;
      }

      String stateKey() {
         return "event_" + key;
      }

      String scheduleKey() {
         return "event_" + key + "_schedule";
      }

      public static Event of(String key) {
         for (Event e : values()) {
            if (e.key.equalsIgnoreCase(key)) {
               return e;
            }
         }
         return null;
      }
   }

   private static volatile Map<Event, Boolean> state = new EnumMap<>(Event.class);
   /** Lịch tự bật / tắt {bắt đầu, kết thúc} epoch ms (0 = không). Mỗi mốc dùng 1 lần rồi tự xoá. */
   private static volatile Map<Event, long[]> schedule = new EnumMap<>(Event.class);
   private static final List<BiConsumer<Event, Boolean>> LISTENERS = new CopyOnWriteArrayList<>();
   private static volatile EventRepository repository;

   private GameEvents() {
   }

   public static boolean active(Event e) {
      return Boolean.TRUE.equals(state.get(e));
   }

   /** Sự kiện của mã {@code key} trong JSON cấu hình đang bật; key rỗng / không biết = không gắn sự kiện -> true. */
   public static boolean activeKey(String key) {
      if (key == null || key.isBlank()) {
         return true;
      }
      Event e = Event.of(key);
      return e == null || active(e);
   }

   public static boolean midAutumn() {
      return active(Event.MID_AUTUMN);
   }

   /** NPC thuộc sự kiện (mẫu NPC -> sự kiện): ẩn khi sự kiện tắt. 21 Nguyệt linh thần (map 2) = Trung thu. */
   static final Map<Integer, Event> EVENT_NPCS = Map.of(21, Event.MID_AUTUMN);

   public static boolean isEventNpc(int npcTemplateId) {
      return EVENT_NPCS.containsKey(npcTemplateId);
   }

   /** NPC này có hiện / bấm được không. */
   public static boolean npcVisible(int npcTemplateId) {
      Event e = EVENT_NPCS.get(npcTemplateId);
      return e == null || active(e);
   }

   /** Có sự kiện nào có NPC đang tắt (gói map bỏ NPC sự kiện). Chỉ 1 sự kiện -> đúng nghĩa. */
   public static boolean anyEventNpcHidden() {
      for (Event e : EVENT_NPCS.values()) {
         if (!active(e)) {
            return true;
         }
      }
      return false;
   }

   /** Nghe bật / tắt (gọi trên luồng đã đổi cờ — luồng blocking). */
   public static void onChange(BiConsumer<Event, Boolean> listener) {
      LISTENERS.add(listener);
   }

   /** Khởi động: đọc cờ từ SQL. */
   public static void load(EventRepository repo) throws SQLException {
      repository = repo;
      Map<Event, Boolean> next = new EnumMap<>(Event.class);
      for (Event e : Event.values()) {
         next.put(e, repo.enabled(e.stateKey()));
      }
      state = next;
      Map<Event, long[]> sched = new EnumMap<>(Event.class);
      for (Event e : Event.values()) {
         sched.put(e, parseSchedule(repo.get(e.scheduleKey())));
      }
      schedule = sched;
      for (Event e : Event.values()) {
         Log.info("Su kien %s: %s", e.key, active(e) ? "BAT" : "tat");
      }
   }

   /** Bật / tắt (SQL — luồng blocking), báo các hệ thống. @return false nếu đã đúng trạng thái đó */
   public static synchronized boolean set(Event e, boolean on) throws SQLException {
      if (active(e) == on) {
         return false;
      }
      if (repository != null) {
         repository.save(e.stateKey(), on);
      }
      Map<Event, Boolean> next = new EnumMap<>(state);
      next.put(e, on);
      state = next;
      Log.info("Su kien %s: %s", e.key, on ? "BAT" : "TAT");
      for (BiConsumer<Event, Boolean> l : LISTENERS) {
         try {
            l.accept(e, on);
         } catch (RuntimeException ex) {
            Log.warn("Su kien %s: loi khi %s: %s", e.key, on ? "bat" : "tat", ex);
         }
      }
      return true;
   }

   static long[] parseSchedule(String v) {
      long[] r = {0, 0};
      if (v == null || v.isBlank()) {
         return r;
      }
      String[] parts = v.split(",");
      try {
         r[0] = Long.parseLong(parts[0].trim());
         r[1] = parts.length > 1 ? Long.parseLong(parts[1].trim()) : 0;
      } catch (NumberFormatException ex) {
         Log.warn("Lich su kien sai dinh dang: %s", v);
      }
      return r;
   }

   /** Lịch {bắt đầu, kết thúc} (epoch ms, 0 = không). */
   public static long[] scheduleOf(Event e) {
      long[] s = schedule.get(e);
      return s == null ? new long[] {0, 0} : s.clone();
   }

   /** Đặt lịch tự bật lúc {@code start} / tự tắt lúc {@code end} (0 = bỏ mốc đó). SQL — luồng blocking. */
   public static synchronized void setSchedule(Event e, long start, long end) throws SQLException {
      long[] value = {Math.max(0, start), Math.max(0, end)};
      if (repository != null) {
         repository.put(e.scheduleKey(), value[0] + "," + value[1]);
      }
      Map<Event, long[]> next = new EnumMap<>(schedule);
      next.put(e, value);
      schedule = next;
      Log.info("Lich su kien %s: bat %d, tat %d", e.key, value[0], value[1]);
   }

   /**
    * Mỗi 30 giây (luồng blocking): tới mốc bắt đầu -> bật (xoá mốc bắt đầu); tới mốc kết thúc -> tắt (xoá cả lịch). Bật /
    * tắt tay giữa chừng không bị lịch đè lại vì mốc đã dùng thì xoá.
    */
   public static void tickSchedule(long now) {
      for (Event e : Event.values()) {
         long[] s = scheduleOf(e);
         try {
            if (s[1] > 0 && now >= s[1]) {
               setSchedule(e, 0, 0);
               set(e, false);
            } else if (s[0] > 0 && now >= s[0]) {
               setSchedule(e, 0, s[1]);
               set(e, true);
            }
         } catch (SQLException ex) {
            Log.warn("Lich su kien %s loi SQL: %s", e.key, ex.getMessage());
         }
      }
   }

   /** Admin panel: mỗi sự kiện {key, title, active, description, details}. */
   public static List<Map<String, Object>> adminList() {
      List<Map<String, Object>> out = new java.util.ArrayList<>();
      for (Event e : Event.values()) {
         Map<String, Object> row = new java.util.LinkedHashMap<>();
         row.put("key", e.key);
         row.put("title", e.title);
         row.put("active", active(e));
         if (e == Event.MID_AUTUMN) {
            row.put("description", "NPC Nguyệt linh thần (map 2: quà điểm danh, làm bánh, cửa hàng Trung thu), cà rốt +"
                  + " 5 Ngọc thố ở map 1-4, đèn hoa đăng, Tinh anh / Thủ lĩnh rơi nguyên liệu, boss Cổ thần Dạ xoa"
                  + " (world_boss 11, 30 phút). Điểm sự kiện ở bảng xếp hạng tab Sự kiện.");
            row.put("details", RabbitService.adminStatus());
            row.put("stats", EventStats.snapshot());
            row.put("top", com.tlnt.server.ranking.RankingService.topEvent(20));
            row.put("boss", MidAutumn.BOSS_ID);
         }
         long[] s = scheduleOf(e);
         row.put("start", s[0]);
         row.put("end", s[1]);
         out.add(row);
      }
      return out;
   }

   /** Test: đặt cờ trong RAM, không SQL, không listener. */
   static void setForTest(Event e, boolean on) {
      Map<Event, Boolean> next = new EnumMap<>(state);
      next.put(e, on);
      state = next;
   }
}
