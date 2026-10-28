package com.tlnt.server.achievement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admin panel: xem / sửa tiến độ thành tựu của 1 nhân vật (bảng thành tựu đang nạp + {@link AchievementState}). */
public final class AchievementAdmin {

   private AchievementAdmin() {
   }

   /** {types: [{key, count, max}], items: [{id, title, type, target, progress, status, claimed}]}. */
   public static Map<String, Object> view(AchievementState state) {
      AchievementState s = state == null ? new AchievementState() : state;
      Map<String, Object> out = new LinkedHashMap<>();
      List<Map<String, Object>> types = new ArrayList<>();
      for (AchievementType t : AchievementType.values()) {
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("key", t.key);
         row.put("count", s.get(t));
         row.put("max", t.max);
         types.add(row);
      }
      out.put("types", types);
      List<Map<String, Object>> items = new ArrayList<>();
      for (Achievement a : AchievementService.all()) {
         long count = s.get(a.type());
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", a.id());
         row.put("title", a.title());
         row.put("type", a.type().key);
         row.put("target", a.target());
         row.put("progress", a.progress(count));
         row.put("status", a.status(count, s.isClaimed(a.id())));
         items.add(row);
      }
      out.put("items", items);
      return out;
   }

   /** Đặt bộ đếm loại {@code key} = {@code value} (0 = xoá). @return câu lỗi, null = được */
   public static String setCount(AchievementState s, String key, long value) {
      AchievementType t = AchievementType.of(key);
      if (t == null) {
         return "Không có loại thành tựu " + key + ".";
      }
      s.normalize();
      if (value <= 0) {
         s.counts.remove(t.key);
      } else {
         s.counts.put(t.key, value);
      }
      return null;
   }
}
