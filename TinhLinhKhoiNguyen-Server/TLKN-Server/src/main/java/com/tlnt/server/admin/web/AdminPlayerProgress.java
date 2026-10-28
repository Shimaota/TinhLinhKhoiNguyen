package com.tlnt.server.admin.web;

import com.tlnt.server.achievement.AchievementAdmin;
import com.tlnt.server.achievement.AchievementState;
import com.tlnt.server.daily.DailyState;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.MainQuest;
import com.tlnt.server.quest.MainQuests;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.util.Log;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admin actions and views for a character's quests and achievements. */
final class AdminPlayerProgress {

   private final AdminApi.Deps d;
   private final AdminPlayers players;

   AdminPlayerProgress(AdminApi.Deps deps, AdminPlayers players) {
      this.d = deps;
      this.players = players;
   }

   Object view(int playerId) throws SQLException {
      return players.run(playerId, false, (p, c) -> {
         Map<String, Object> out = AdminApi.ok(null);
         out.put("quest", quest(p));
         out.put("daily", daily(p));
         out.put("achievements", AchievementAdmin.view(p.achievements));
         return out;
      });
   }

   private static Map<String, Object> quest(PlayerData p) {
      Map<String, Object> q = new LinkedHashMap<>();
      PlayerData.MainQuestProgress mq = p.mainQuest;
      int id = MainQuests.currentQuest(mq);
      MainQuest quest = MainQuests.get(id);
      q.put("questId", id);
      q.put("stage", mq == null ? 0 : mq.stage());
      q.put("progress", mq == null ? 0 : mq.progress());
      q.put("name", quest.name());
      List<Object[]> steps = new ArrayList<>();
      for (int k = 0; k < quest.steps().size(); k++) {
         var st = quest.steps().get(k);
         steps.add(new Object[] {k, st.name(), st.count()});
      }
      q.put("steps", steps);
      List<Object[]> all = new ArrayList<>();
      for (int i = 1; i <= MainQuests.count(); i++) {
         all.add(new Object[] {i, MainQuests.get(i).name()});
      }
      q.put("quests", all);
      return q;
   }

   private static Map<String, Object> daily(PlayerData p) {
      Map<String, Object> m = new LinkedHashMap<>();
      DailyState s = p.dailyQuest;
      m.put("day", s == null ? 0 : s.day);
      m.put("dayText", s == null || s.day <= 0 ? "chưa giao" : LocalDate.ofEpochDay(s.day).toString());
      m.put("points", s == null ? 0 : s.points);
      m.put("added", s == null ? 0 : s.added);
      m.put("swaps", s == null ? 0 : s.swaps);
      m.put("claimed", s == null ? List.of() : s.claimed);
      List<Map<String, Object>> tasks = new ArrayList<>();
      if (s != null) {
         for (int i = 0; i < s.tasks.size(); i++) {
            DailyState.Task t = s.tasks.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", i);
            row.put("name", t.n);
            row.put("kind", t.k);
            row.put("progress", t.p);
            row.put("goal", t.g);
            row.put("claimed", t.c);
            tasks.add(row);
         }
      }
      m.put("tasks", tasks);
      return m;
   }

   Object action(int playerId, String action, com.google.gson.JsonObject b) throws SQLException {
      return players.run(playerId, true, (p, c) -> {
         String msg;
         String column;
         switch (action) {
            case "quest/set" -> {
               int q = AdminApi.intField(b, "questId");
               if (q < 1 || q > MainQuests.count() + 1) {
                  throw new AdminApi.BadRequest("Nhiệm vụ 1-" + (MainQuests.count() + 1) + ".");
               }
               MainQuest quest = MainQuests.get(q);
               int stage = Math.max(0, Math.min(quest.steps().size() - 1, AdminApi.intField(b, "stage")));
               int progress = Math.max(0, AdminApi.intField(b, "progress"));
               if (c.online && QuestEvents.service() != null && progress == 0) {
                  QuestEvents.service().jump(c.session, q, stage + 1);
               } else {
                  p.mainQuest = new PlayerData.MainQuestProgress(q, stage, progress);
                  p.questDialog = null;
               }
               column = "main_quest";
               msg = "Đặt nhiệm vụ " + q + " bước " + (stage + 1) + " tiến độ " + progress + ".";
            }
            case "daily/reset" -> {
               p.dailyQuest = new DailyState();
               column = "daily_quest";
               msg = "Đã xoá nhiệm vụ hằng ngày — lần mở bảng tới giao 10 nhiệm vụ mới.";
            }
            case "daily/task" -> {
               DailyState s = p.dailyQuest;
               int i = AdminApi.intField(b, "index");
               if (s == null || i < 0 || i >= s.tasks.size()) {
                  throw new AdminApi.BadRequest("Không có nhiệm vụ " + i + ".");
               }
               DailyState.Task t = s.tasks.get(i);
               t.p = Math.max(0, Math.min(t.g, AdminApi.intField(b, "progress")));
               t.c = AdminApi.bool(b, "claimed");
               column = "daily_quest";
               msg = "Đã sửa nhiệm vụ \"" + t.n + "\".";
            }
            case "daily/points" -> {
               DailyState s = p.dailyQuest == null ? new DailyState() : p.dailyQuest;
               s.points = Math.max(0, AdminApi.intField(b, "points"));
               s.added = Math.max(0, AdminApi.intField(b, "added"));
               s.swaps = Math.max(0, AdminApi.intField(b, "swaps"));
               p.dailyQuest = s;
               column = "daily_quest";
               msg = "Đã sửa điểm chăm chỉ / lượt nhận / đổi.";
            }
            case "achievement/count" -> {
               if (p.achievements == null) {
                  p.achievements = new AchievementState();
               }
               String err = AchievementAdmin.setCount(p.achievements, AdminApi.str(b, "type"),
                     Math.max(0, b.has("value") ? b.get("value").getAsLong() : 0));
               if (err != null) {
                  throw new AdminApi.BadRequest(err);
               }
               column = "achievement";
               msg = "Đã đặt bộ đếm thành tựu.";
            }
            case "achievement/claim" -> {
               if (p.achievements == null) {
                  p.achievements = new AchievementState();
               }
               int id = AdminApi.intField(b, "achievementId");
               if (AdminApi.bool(b, "claimed")) {
                  p.achievements.claimed.add(id);
               } else {
                  p.achievements.claimed.remove(id);
               }
               column = "achievement";
               msg = AdminApi.bool(b, "claimed") ? "Đã đánh dấu đã nhận quà." : "Đã cho nhận lại quà thành tựu.";
            }
            default -> throw new AdminApi.BadRequest("Thao tác không hợp lệ: " + action + ".");
         }
         c.all = true;
         if (c.online) {
            resend(c, p, column);
         }
         Log.info("Admin panel: '%s' %s", Log.ascii(p.name), action);
         return msg;
      });
   }

   private void resend(AdminPlayers.Change c, PlayerData p, String column) {
      switch (column) {
         case "achievement" -> {
            if (d.achievements() != null) {
               d.achievements().onLogin(c.session, p);
            }
         }
         case "daily_quest" -> {
            if (d.dailyQuests() != null) {
               d.dailyQuests().onLogin(c.session, p);
            }
         }
         case "main_quest" -> {
            if (QuestEvents.service() != null) {
               QuestEvents.service().sendAll(c.session, p);
            }
         }
         default -> {
         }
      }
      com.tlnt.server.item.ItemService.refreshLook(p);
      d.world().broadcastStats(p);
   }
}
