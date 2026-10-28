package com.tlnt.server.admin.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.forge.ElementRules;
import com.tlnt.server.forge.SoulRules;
import com.tlnt.server.forge.UpgradeRules;
import com.tlnt.server.item.CollectionKind;
import com.tlnt.server.item.ItemBuffs;
import com.tlnt.server.item.ItemCollection;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.shop.ShopService;
import com.tlnt.server.mail.Mail;
import com.tlnt.server.mail.MailRepository;
import com.tlnt.server.mail.MailService;
import com.tlnt.server.player.Gender;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.Leveling;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.player.SkillData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.WorldPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tab "Người chơi" của admin panel: danh sách online, tìm, chi tiết đầy đủ (chỉ số, đồ mặc / hành trang / rương / bộ sưu
 * tập với thông tin item như client, hiệu ứng, kỹ năng), và mọi thao tác sửa nhân vật.
 *
 * <h2>Sửa nhân vật an toàn</h2>
 * <ul>
 *   <li>ONLINE: việc sửa xếp vào hàng đợi session của người đó (như 1 gói tin — không tranh với gói họ đang gửi), xong
 *       thì gửi lại kho / tiền / khối chỉ số và lưu nền. Luồng admin chờ kết quả tối đa {@link #WAIT_SECONDS} giây.</li>
 *   <li>OFFLINE: giữ tài khoản ({@code AccountService.adminHold} — chặn đăng nhập), nạp từ SQL (sau lượt lưu còn dở),
 *       sửa, lưu và CHỜ ghi xong rồi mới thả.</li>
 *   <li>Đang giao dịch: không sửa hành trang (túi đang đóng băng, giao dịch chốt trên bản sao).</li>
 * </ul>
 */
final class AdminPlayers {

   static final int WAIT_SECONDS = 5;
   private static final int MAX_ROWS = 100;
   /** Tối đa số món KHÔNG cộng dồn tặng 1 lần (mỗi món 1 ô). */
   private static final int MAX_SINGLE_ITEMS = 50;
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
         .withZone(ZoneId.systemDefault());

   /** Việc làm trên 1 nhân vật; trả về dữ liệu / câu báo. */
   @FunctionalInterface
   interface Op {
      Object apply(PlayerData d, Change c) throws SQLException;
   }

   /** Ghi lại phần đã đổi để gửi lại client (online). */
   static final class Change {
      final boolean online;
      final PlayerSession session;
      boolean bag;
      boolean box;
      boolean body;
      boolean currency;
      /** Đổi nhiều kho (chuyển món giữa túi / rương / đồ mặc / bộ sưu tập) -> gửi lại tất cả. */
      boolean all;

      Change(boolean online, PlayerSession session) {
         this.online = online;
         this.session = session;
      }
   }

   private final AdminApi.Deps d;

   AdminPlayers(AdminApi.Deps deps) {
      this.d = deps;
   }

   // ================================================================== danh sách

   /** Tối đa số dòng "đang online" gửi trình duyệt (cấp cao trước); {@code total} = tổng thật. */
   static final int ONLINE_ROWS = 500;

   Object online(String filter) {
      List<Map<String, Object>> list = new ArrayList<>();
      long now = System.currentTimeMillis();
      String f = filter == null ? "" : filter.trim().toLowerCase(java.util.Locale.ROOT);
      int total = 0;
      for (WorldPlayer p : d.world().onlinePlayers()) {
         PlayerData pd = p.data();
         if (pd.bot) {
            continue; // người chơi giả: tab "Bot người chơi"
         }
         total++;
         if (!f.isEmpty() && !(pd.name + " " + d.mapName(pd.mapId) + " " + p.session().getRemoteIp())
               .toLowerCase(java.util.Locale.ROOT).contains(f)) {
            continue;
         }
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", pd.getId());
         row.put("accountId", pd.getAccountId());
         row.put("name", pd.name);
         row.put("level", pd.level);
         row.put("gender", Gender.name(pd.gender));
         row.put("cls", className(pd.classId));
         row.put("map", d.mapName(pd.mapId));
         row.put("zone", p.zone() == null ? -1 : p.zone().id());
         row.put("ip", p.session().getRemoteIp());
         row.put("session", (now - pd.sessionStartAt) / 1000);
         list.add(row);
      }
      list.sort((a, b) -> Integer.compare((Integer) b.get("level"), (Integer) a.get("level")));
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("total", total);
      out.put("matched", list.size());
      out.put("rows", list.size() > ONLINE_ROWS ? new ArrayList<>(list.subList(0, ONLINE_ROWS)) : list);
      return out;
   }

   List<Map<String, Object>> search(String text) throws SQLException {
      List<Map<String, Object>> list = new ArrayList<>();
      try (Connection c = d.playerDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT p.id, p.name, p.level, p.map_id, p.class_id, p.gender,"
                  + " p.last_login_at, a.id, a.username, a.banned, a.last_login_ip FROM player p"
                  + " JOIN account a ON a.id = p.account_id WHERE p.name LIKE ? OR a.username LIKE ? OR p.id = ?"
                  + " ORDER BY p.level DESC LIMIT " + MAX_ROWS)) {
         String like = AdminApi.like(text.trim());
         ps.setString(1, like);
         ps.setString(2, like);
         ps.setInt(3, AdminApi.parseInt(text.trim(), -1));
         try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               Map<String, Object> row = new LinkedHashMap<>();
               int id = rs.getInt(1);
               row.put("id", id);
               row.put("name", rs.getString(2));
               row.put("level", rs.getInt(3));
               row.put("map", d.mapName(rs.getInt(4)));
               row.put("cls", className(rs.getInt(5)));
               row.put("gender", Gender.name(rs.getInt(6)));
               row.put("lastLogin", AdminAccounts.date(rs.getTimestamp(7)));
               row.put("accountId", rs.getInt(8));
               row.put("username", rs.getString(9));
               row.put("banned", rs.getBoolean(10));
               row.put("ip", rs.getString(11));
               row.put("online", d.world().online(id) != null);
               list.add(row);
            }
         }
      }
      return list;
   }

   // ================================================================== chi tiết

   /** Tài khoản (SQL) + nhân vật đầy đủ (online: RAM trên hàng đợi session; offline: SQL). */
   Object detail(int playerId) throws SQLException {
      Map<String, Object> account = account(playerId);
      Object character = run(playerId, false, (p, c) -> character(p, c.online));
      Map<String, Object> out = AdminApi.ok(null);
      out.put("account", account);
      out.put("player", character);
      return out;
   }

   private Map<String, Object> account(int playerId) throws SQLException {
      try (Connection c = d.playerDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT a.id, a.username, a.email, a.role, a.banned,"
                  + " a.ban_reason, a.secondary_password_hash IS NOT NULL, a.last_login_ip, a.created_at,"
                  + " p.created_at, p.last_login_at, p.last_logout_at FROM player p JOIN account a ON a.id = p.account_id"
                  + " WHERE p.id = ?")) {
         ps.setInt(1, playerId);
         try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
               throw new AdminApi.BadRequest("Không có nhân vật #" + playerId + ".");
            }
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", rs.getInt(1));
            a.put("username", rs.getString(2));
            a.put("email", rs.getString(3));
            a.put("role", rs.getInt(4));
            a.put("banned", rs.getBoolean(5));
            a.put("banReason", rs.getString(6));
            a.put("pw2", rs.getBoolean(7));
            a.put("ip", rs.getString(8));
            a.put("created", AdminAccounts.date(rs.getTimestamp(9)));
            a.put("charCreated", AdminAccounts.date(rs.getTimestamp(10)));
            a.put("lastLogin", AdminAccounts.date(rs.getTimestamp(11)));
            a.put("lastLogout", AdminAccounts.date(rs.getTimestamp(12)));
            return a;
         }
      }
   }

   private Map<String, Object> character(PlayerData p, boolean online) {
      long now = System.currentTimeMillis();
      Map<String, Object> o = new LinkedHashMap<>();
      o.put("id", p.getId());
      o.put("accountId", p.getAccountId());
      o.put("name", p.name);
      o.put("online", online);
      o.put("gender", Gender.name(p.gender));
      o.put("cls", className(p.classId));
      o.put("level", p.level);
      o.put("maxLevel", Leveling.maxLevel());
      o.put("exp", p.exp);
      o.put("expToLevel", Leveling.expToLevelUp(p));
      o.put("gold", p.gold);
      o.put("lockedGold", p.lockedGold);
      o.put("diamond", p.diamond);
      o.put("recharge", p.rechargeTotal);
      o.put("pk", p.pkPoints);
      o.put("map", d.mapName(p.mapId));
      o.put("mapId", p.mapId);
      WorldPlayer wp = online ? d.world().online(p.getId()) : null;
      o.put("zone", wp == null || wp.zone() == null ? -1 : wp.zone().id());
      o.put("ip", wp == null ? "" : wp.session().getRemoteIp());
      o.put("pos", p.x == null ? "" : Math.round(p.x) + ", " + Math.round(p.y));
      GamePack.Title title = p.titleId > 0 ? GameDataCache.title(p.titleId) : null;
      o.put("title", title == null ? "" : title.name());
      o.put("tribe", p.tribe == null ? "" : p.tribe.name());
      o.put("party", p.party == null ? "" : "#" + p.party.id());
      o.put("trading", p.trade != null);
      o.put("playTime", p.playTimeSeconds + (online ? (now - p.sessionStartAt) / 1000 : 0));
      o.put("talentPoints", p.talentPoints);
      o.put("stamina", p.activityPoints);
      o.put("quest", p.mainQuest == null ? "" : "NV " + p.mainQuest.questId() + " · bước " + p.mainQuest.stage()
            + " · tiến độ " + p.mainQuest.progress());
      PlayerStats s = online && p.stats != null ? p.stats : PlayerStats.of(p);
      o.put("hp", p.hp < 0 ? s.maxHp : p.hp);
      o.put("mp", p.mp < 0 ? s.maxMp : p.mp);
      o.put("stats", stats(s));
      List<Map<String, Object>> body = new ArrayList<>();
      for (int slot = 0; slot < p.body.size(); slot++) {
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("slot", slot);
         row.put("slotName", AdminItems.slotName(slot));
         ItemData item = p.body.get(slot);
         row.put("item", item == null ? null : AdminItems.view(item, slot));
         body.add(row);
      }
      o.put("body", body);
      o.put("back", p.back() == null ? null : AdminItems.view(p.back(), -1)); // món đeo lưng đang dùng
      o.put("quick", p.quickSlots);
      o.put("bag", box(p.bag));
      o.put("box", box(p.box));
      List<Map<String, Object>> collections = new ArrayList<>();
      for (CollectionKind kind : CollectionKind.values()) {
         ItemCollection col = kind.of(p);
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("name", kind.displayName);
         row.put("kind", kind.ordinal());
         row.put("itemType", kind.itemTypeId);
         row.put("selected", col == null ? -1 : col.selected);
         List<Map<String, Object>> items = new ArrayList<>();
         if (col != null) {
            for (int k = 0; k < col.items.size(); k++) {
               items.add(AdminItems.view(col.items.get(k), k));
            }
         }
         row.put("items", items);
         collections.add(row);
      }
      o.put("collections", collections);
      List<Map<String, Object>> buffs = new ArrayList<>();
      for (Map.Entry<Integer, Long> e : p.buffs.entrySet()) {
         if (e.getValue() <= now || ItemBuffs.forOption(e.getKey()) == null) {
            continue;
         }
         ItemPack.OptionInfo info = GameDataCache.option(e.getKey());
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("option", e.getKey());
         row.put("item", ItemBuffs.forOption(e.getKey()).itemTemplateId());
         row.put("name", info == null ? "Hiệu ứng #" + e.getKey() : info.name());
         row.put("description", info == null ? "" : AdminItems.clean(info.description()));
         row.put("until", TIME.format(Instant.ofEpochMilli(e.getValue())));
         row.put("secondsLeft", (e.getValue() - now) / 1000);
         buffs.add(row);
      }
      o.put("buffs", buffs);
      List<Map<String, Object>> skills = new ArrayList<>();
      for (SkillData sk : p.skills) {
         var t = GameDataCache.skillTemplate(sk.skillId);
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", sk.skillId);
         row.put("name", t == null ? "Kỹ năng #" + sk.skillId : t.name());
         row.put("level", sk.level);
         row.put("maxLevel", t == null ? sk.level : maxSkillLevel(t));
         skills.add(row);
      }
      o.put("skills", skills);
      return o;
   }

   private static Map<String, Object> box(ItemContainer box) {
      Map<String, Object> out = new LinkedHashMap<>();
      List<Map<String, Object>> items = new ArrayList<>();
      for (int slot = 0; slot < box.size(); slot++) {
         ItemData item = box.get(slot);
         if (item != null) {
            items.add(AdminItems.view(item, slot));
         }
      }
      out.put("size", box.size());
      out.put("items", items);
      return out;
   }

   /** Khối chỉ số như bảng nhân vật (nhãn, giá trị). */
   private static List<Object[]> stats(PlayerStats s) {
      List<Object[]> l = new ArrayList<>();
      l.add(new Object[] {"HP tối đa", s.maxHp});
      l.add(new Object[] {"MP tối đa", s.maxMp});
      l.add(new Object[] {"Tấn công", s.attackPower});
      l.add(new Object[] {"Tấn công lên quái", s.attackVsMob});
      l.add(new Object[] {"Chí mạng", s.critChance});
      l.add(new Object[] {"Sát thương chí mạng %", s.critDamagePercent});
      l.add(new Object[] {"Giáp", s.armor});
      l.add(new Object[] {"Xuyên giáp", s.armorPen});
      l.add(new Object[] {"Né tránh", s.evasion});
      l.add(new Object[] {"Chính xác", s.accuracy});
      l.add(new Object[] {"Tốc chạy", s.moveSpeed});
      l.add(new Object[] {"Hồi HP / 5s", s.hpRegen});
      l.add(new Object[] {"Hồi MP / 5s", s.mpRegen});
      l.add(new Object[] {"Hút HP %", s.hpLeechPercent});
      l.add(new Object[] {"Hút MP %", s.mpLeechPercent});
      l.add(new Object[] {"Hút HP quái %", s.hpLeechVsMobPercent});
      l.add(new Object[] {"Hút MP quái %", s.mpLeechVsMobPercent});
      l.add(new Object[] {"Phản sát thương %", s.reflectDamagePercent});
      l.add(new Object[] {"ST choáng", s.stunDamage});
      l.add(new Object[] {"ST suy yếu", s.weakenDamage});
      l.add(new Object[] {"ST băng giá", s.iceDamage});
      l.add(new Object[] {"ST thiêu đốt", s.burnDamage});
      l.add(new Object[] {"ST sấm sét", s.thunderDamage});
      l.add(new Object[] {"Kháng chí mạng %", s.critResistPercent});
      l.add(new Object[] {"Khuếch đại tấn công %", s.damageAmpPercent});
      l.add(new Object[] {"Hấp thụ sát thương %", s.damageAbsorbPercent});
      l.add(new Object[] {"Kháng hiệu ứng %", s.effectResistPercent});
      l.add(new Object[] {"Kháng sát thương", s.damageResist});
      l.add(new Object[] {"Thưởng vàng %", s.goldBonusPercent});
      l.add(new Object[] {"Thưởng kinh nghiệm %", s.expBonusPercent});
      l.add(new Object[] {"Thể lực tối đa", s.maxStamina});
      return l;
   }

   // ================================================================== thao tác

   Object kick(int playerId) {
      WorldPlayer p = d.world().online(playerId);
      if (p == null || !d.accounts().adminKick(p.data().getAccountId(), "Bạn đã bị quản trị viên mời ra khỏi game.")) {
         throw new AdminApi.BadRequest("Nhân vật không online.");
      }
      return AdminApi.ok("Đã kick " + p.data().name + ".");
   }

   /** Nâng lên cấp {@code target} (từng cấp, cộng điểm tiềm năng như lên cấp thật). */
   Object level(int playerId, int target) throws SQLException {
      return run(playerId, true, (p, c) -> {
         if (target <= p.level || target > Leveling.maxLevel()) {
            throw new AdminApi.BadRequest("Cấp mới phải lớn hơn cấp hiện tại (" + p.level + ") và ≤ "
                  + Leveling.maxLevel() + ".");
         }
         int before = p.level;
         while (p.level < target && Leveling.addExp(p, Math.max(1, Leveling.expToLevelUp(p))) > 0) {
            // lên từng cấp: điểm tiềm năng cộng theo từng cấp
         }
         if (c.online) {
            ItemService.refreshLook(p);
            d.world().broadcastStats(p);
            com.tlnt.server.quest.QuestEvents.fire(c.session, com.tlnt.server.quest.StepKind.REACH_LEVEL, 0);
            com.tlnt.server.achievement.AchievementEvents.add(c.session,
                  com.tlnt.server.achievement.AchievementType.LEVEL, p.level);
            c.session.send(com.tlnt.server.net.Packets.notice("Quản trị viên đã nâng bạn lên cấp " + p.level + "."));
         }
         Log.info("Admin panel: '%s' cap %d -> %d", Log.ascii(p.name), before, p.level);
         return "Đã nâng " + p.name + " từ cấp " + before + " lên " + p.level + ".";
      });
   }

   /** Đặt số vàng / vàng khoá / lam thạch (giá trị tuyệt đối; trường không gửi = giữ). */
   Object currency(int playerId, JsonObject b) throws SQLException {
      return run(playerId, true, (p, c) -> {
         StringBuilder log = new StringBuilder();
         if (b.has("gold")) {
            long v = amount(b, "gold");
            log.append(" vang ").append(p.gold).append("->").append(v);
            p.gold = v;
         }
         if (b.has("lockedGold")) {
            long v = amount(b, "lockedGold");
            log.append(" vang khoa ").append(p.lockedGold).append("->").append(v);
            p.lockedGold = v;
         }
         if (b.has("diamond")) {
            long v = amount(b, "diamond");
            log.append(" lam thach ").append(p.diamond).append("->").append(v);
            p.diamond = v;
         }
         c.currency = true;
         Log.info("Admin panel: '%s'%s", Log.ascii(p.name), log);
         return "Đã cập nhật tiền tệ của " + p.name + ".";
      });
   }

   private static long amount(JsonObject b, String key) {
      long v;
      try {
         v = b.get(key).getAsLong();
      } catch (RuntimeException e) {
         throw new AdminApi.BadRequest("Trường " + key + " phải là số.");
      }
      if (v < 0 || v > ItemService.MAX_GOLD) {
         throw new AdminApi.BadRequest("Giá trị 0-" + ItemService.MAX_GOLD + ".");
      }
      return v;
   }

   /** Thêm item (dạng RewardItem {"i","q","l","s","d"}) vào hành trang / rương. */
   Object itemAdd(int playerId, String where, JsonObject raw) throws SQLException {
      RewardItem reward = RewardItem.fromJson(raw, false);
      if (reward == null || GameDataCache.itemTemplate(reward.templateId()) == null) {
         throw new AdminApi.BadRequest("Không có mẫu item này.");
      }
      if (reward.templateId() == ItemService.GOLD_TEMPLATE_ID || reward.templateId() == ItemService.DIAMOND_TEMPLATE_ID) {
         throw new AdminApi.BadRequest("Vàng / lam thạch: sửa ở mục Tiền tệ.");
      }
      if (reward.quantity() <= 0) {
         throw new AdminApi.BadRequest("Số lượng phải > 0.");
      }
      return run(playerId, true, (p, c) -> {
         ItemContainer box = container(p, where, c, false);
         long now = System.currentTimeMillis();
         ItemData first = reward.create(now);
         String name = first.template() == null ? "#" + reward.templateId() : first.template().name();
         if (first.stackable()) {
            if (reward.quantity() > ItemData.MAX_STACK * 10) {
               throw new AdminApi.BadRequest("Số lượng quá lớn.");
            }
            first.quantity = reward.quantity();
            if (box.add(first) < 0) {
               throw new AdminApi.BadRequest("Không đủ chỗ trống.");
            }
         } else {
            int n = reward.quantity();
            if (n > MAX_SINGLE_ITEMS) {
               throw new AdminApi.BadRequest("Món không cộng dồn: tối đa " + MAX_SINGLE_ITEMS + " món / lần.");
            }
            if (box.countEmpty() < n) {
               throw new AdminApi.BadRequest("Cần " + n + " ô trống, chỉ còn " + box.countEmpty() + ".");
            }
            first.quantity = 1;
            box.add(first);
            for (int k = 1; k < n; k++) {
               box.add(reward.create(now)); // option dạng khoảng: random riêng từng món
            }
         }
         Log.info("Admin panel: them %d x item %d vao %s cua '%s'", reward.quantity(), reward.templateId(), where,
               Log.ascii(p.name));
         return "Đã thêm " + reward.quantity() + " × " + name + " vào " + label(where) + " của " + p.name + ".";
      });
   }

   /** Sửa món ở ô {@code slot} (xem {@link AdminItems#applyEdit}); {@code where} back = đồ đeo lưng. */
   Object itemEdit(int playerId, String where, int slot, JsonObject edit) throws SQLException {
      return run(playerId, true, (p, c) -> {
         if ("back".equals(where)) {
            if (p.back() == null) {
               throw new AdminApi.BadRequest("Không đeo đồ ở lưng.");
            }
            ItemData copy = p.back().copy();
            AdminItems.applyEdit(copy, edit);
            p.backs.items.set(p.backs.selected, copy);
            c.all = true;
            return "Đã sửa đồ đeo lưng.";
         }
         ItemContainer box = container(p, where, c, true);
         ItemData item = box.valid(slot) ? box.get(slot) : null;
         if (item == null) {
            throw new AdminApi.BadRequest("Ô " + slot + " đang trống.");
         }
         ItemData copy = item.copy();
         AdminItems.applyEdit(copy, edit);
         box.set(slot, copy);
         Log.info("Admin panel: sua item o %d (%s) cua '%s'", slot, where, Log.ascii(p.name));
         return "Đã sửa " + (copy.template() == null ? "item" : copy.template().name()) + " ở " + label(where) + ".";
      });
   }

   /** Đọc mới một món riêng từ trạng thái nhân vật để popup cập nhật mà không tải lại toàn bộ hồ sơ. */
   Object itemView(int playerId, String where, int slot) throws SQLException {
      return run(playerId, false, (p, c) -> {
         if (!"body".equals(where)) {
            throw new AdminApi.BadRequest("Chỉ hỗ trợ đọc nhanh trang bị đang mặc.");
         }
         ItemData item = p.body.valid(slot) ? p.body.get(slot) : null;
         if (item == null) {
            throw new AdminApi.BadRequest("Ô trang bị " + slot + " đang trống.");
         }
         Map<String, Object> result = new LinkedHashMap<>();
         result.put("item", AdminItems.view(item, slot));
         return result;
      });
   }

   /** Bùa dùng bởi admin lấy đúng RewardItem đang bán để random chỉ số như lúc mua shop. */
   static List<Map<String, Object>> soulCharmList() {
      List<Map<String, Object>> out = new ArrayList<>();
      for (ItemPack.ItemTemplate template : GameDataCache.item().itemTemplates()) {
         if (template.typeId() != SoulRules.CHARM_TYPE) {
            continue;
         }
         RewardItem goods = ShopService.goods(template.id());
         if (goods == null) {
            continue;
         }
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", template.id());
         row.put("name", template.name());
         row.put("icon", AdminItems.icon(template));
         List<String> lines = new ArrayList<>();
         int[][] min = goods.stats(), max = goods.statsMax();
         for (int i = 0; i < min.length; i++) {
            if (min[i] == null || min[i].length < 2) {
               continue;
            }
            int[] lo = java.util.Arrays.copyOfRange(min[i], 1, min[i].length);
            int[] hi = max[i] == null ? lo : java.util.Arrays.copyOfRange(max[i], 1, max[i].length);
            String a = AdminItems.statText(min[i][0], lo);
            String b = AdminItems.statText(min[i][0], hi);
            lines.add(a.equals(b) ? a : a + " – " + b);
         }
         row.put("lines", lines);
         out.add(row);
      }
      out.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
      return out;
   }

   /** Danh mục đá và preview đủ 10 cấp để popup buff hiển thị rõ chỉ số sẽ áp dụng. */
   static List<Map<String, Object>> elementStoneList() {
      List<Map<String, Object>> out = new ArrayList<>();
      for (int id = 127; id <= 133; id++) {
         ElementRules.Stone stone = ElementRules.stone(id);
         if (stone == null) {
            continue;
         }
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", id);
         row.put("name", itemName(id));
         row.put("icon", AdminItems.icon(id));
         List<Map<String, Object>> levels = new ArrayList<>();
         for (int level = 1; level <= ElementRules.MAX_LEVEL; level++) {
            levels.add(Map.of("level", level, "line", ElementRules.describe(id, level)));
         }
         row.put("levels", levels);
         out.add(row);
      }
      return out;
   }

   /** Tăng cường trực tiếp đồ đang mặc trong admin: roll bùa theo shop, đặt cấp đá hoặc gỡ từng socket. */
   Object itemEnhance(int playerId, int slot, JsonObject b) throws SQLException {
      String action = AdminApi.str(b, "action");
      return run(playerId, true, (p, c) -> {
         ItemContainer body = container(p, "body", c, true);
         ItemData item = body.valid(slot) ? body.get(slot) : null;
         if (item == null || !UpgradeRules.upgradable(item)) {
            throw new AdminApi.BadRequest("Ô này không có trang bị có thể khảm.");
         }
         int open = SoulRules.openSockets(item);
         String message;
         switch (action) {
            case "charm" -> {
               int template = AdminApi.intField(b, "template");
               int socket = b.has("socket") ? AdminApi.intField(b, "socket") : item.soulCharms.size();
               RewardItem goods = ShopService.goods(template);
               if (goods == null || goods.templateId() != template) {
                  throw new AdminApi.BadRequest("Bùa này không có cấu hình bán trong shop.");
               }
               if (socket < 0 || socket > item.soulCharms.size() || socket >= open) {
                  throw new AdminApi.BadRequest("Hãy chọn lỗ bùa trống kế tiếp trên trang bị.");
               }
               boolean replacing = socket < item.soulCharms.size();
               if (!replacing && item.soulCharms.size() >= open) {
                  throw new AdminApi.BadRequest("Trang bị còn " + Math.max(0, open - item.soulCharms.size())
                        + " lỗ bùa; hãy tăng sao để mở thêm lỗ.");
               }
               ItemData charm = goods.create(System.currentTimeMillis());
               if (!SoulRules.isCharm(charm)) {
                  throw new AdminApi.BadRequest("Mẫu shop được chọn không phải bùa linh hồn.");
               }
               if (replacing) {
                  item.soulCharms.set(socket, charm);
               } else {
                  item.soulCharms.add(charm);
               }
               if (charm.locked) {
                  item.locked = true;
               }
               message = "Đã khảm " + itemName(template) + " vào lỗ bùa " + (socket + 1) + " với chỉ số random theo shop: "
                     + String.join(", ", charm.stats.stream().map(s -> AdminItems.statText(s.statTypeId(), s.values())).toList());
            }
            case "element" -> {
               int template = AdminApi.intField(b, "template");
               int level = AdminApi.intField(b, "level");
               int socket = b.has("socket") ? AdminApi.intField(b, "socket") : -1;
               if (!ElementRules.isStone(template) || level < 1 || level > ElementRules.MAX_LEVEL) {
                  throw new AdminApi.BadRequest("Đá nguyên tố hoặc cấp không hợp lệ (1–10).");
               }
               if (socket < 0 || socket >= open || socket > item.elementStones.size()) {
                  throw new AdminApi.BadRequest("Hãy chọn một lỗ nguyên tố đang mở.");
               }
               int existingIndex = ElementRules.indexOf(item, template);
               if (existingIndex >= 0 && existingIndex != socket) {
                  throw new AdminApi.BadRequest(itemName(template) + " đã nằm ở lỗ nguyên tố " + (existingIndex + 1) + ".");
               }
               ItemData.ElementStone current = socket < item.elementStones.size() ? item.elementStones.get(socket) : null;
               if (current == null && ElementRules.used(item) >= open) {
                  throw new AdminApi.BadRequest("Trang bị đã hết lỗ nguyên tố; hãy tăng sao để mở thêm lỗ.");
               }
               if (socket == item.elementStones.size()) {
                  item.elementStones.add(ElementRules.make(template, level));
               } else {
                  item.elementStones.set(socket, ElementRules.make(template, level));
               }
               item.locked = true;
               message = "Đã đặt " + itemName(template) + " cấp " + level + " vào lỗ nguyên tố " + (socket + 1) + " ("
                     + ElementRules.describe(template, level) + ").";
            }
            case "removeCharm" -> {
               int index = AdminApi.intField(b, "index");
               if (index < 0 || index >= item.soulCharms.size()) {
                  throw new AdminApi.BadRequest("Socket bùa không còn tồn tại.");
               }
               ItemData removed = item.soulCharms.remove(index);
               message = "Đã gỡ " + itemName(removed.templateId) + " khỏi trang bị.";
            }
            case "removeStone" -> {
               int index = AdminApi.intField(b, "index");
               if (index < 0 || index >= item.elementStones.size() || item.elementStones.get(index) == null) {
                  throw new AdminApi.BadRequest("Socket đá không còn tồn tại.");
               }
               ItemData.ElementStone removed = item.elementStones.remove(index);
               message = "Đã gỡ " + itemName(removed.templateId()) + " khỏi trang bị.";
            }
            default -> throw new AdminApi.BadRequest("Thao tác tăng cường không hợp lệ.");
         }
         Map<String, Object> result = AdminApi.ok(message);
         result.put("slot", slot);
         result.put("item", AdminItems.view(item, slot));
         return result;
      });
   }

   /** Xoá {@code qty} món ở ô {@code slot} (≤ 0 hoặc ≥ số đang có = xoá cả ô). */
   Object itemRemove(int playerId, String where, int slot, int qty) throws SQLException {
      return run(playerId, true, (p, c) -> {
         if ("back".equals(where)) {
            if (p.back() == null) {
               throw new AdminApi.BadRequest("Không đeo đồ ở lưng.");
            }
            String name = itemName(p.back().templateId);
            p.backs.remove(p.backs.selected);
            ItemService.refreshLook(p);
            if (c.online) {
               d.world().broadcastStats(p);
            }
            c.all = true;
            return "Đã xoá " + name + " (đồ đeo lưng).";
         }
         ItemContainer box = container(p, where, c, true);
         ItemData item = box.valid(slot) ? box.get(slot) : null;
         if (item == null) {
            throw new AdminApi.BadRequest("Ô " + slot + " đang trống.");
         }
         String name = item.template() == null ? "item" : item.template().name();
         if (qty <= 0 || qty >= item.quantity) {
            box.take(slot);
         } else {
            ItemData copy = item.copy();
            copy.quantity -= qty;
            box.set(slot, copy);
         }
         Log.info("Admin panel: xoa item o %d (%s) cua '%s'", slot, where, Log.ascii(p.name));
         return "Đã xoá " + (qty > 0 && qty < item.quantity ? qty + " × " : "") + name + " khỏi " + label(where) + ".";
      });
   }

   /** Hiệu ứng item bật trực tiếp (thời gian {@code minutes}, ≤ 0 = thời gian gốc của item). */
   Object buffAdd(int playerId, int itemTemplateId, double minutes) throws SQLException {
      ItemBuffs.Effect effect = ItemBuffs.forItem(itemTemplateId);
      if (effect == null) {
         throw new AdminApi.BadRequest("Item " + itemTemplateId + " không có hiệu ứng.");
      }
      long ms = minutes > 0 ? Math.round(Math.min(minutes, 60 * 24 * 30) * 60_000) : effect.durationMs();
      return run(playerId, true, (p, c) -> {
         if (c.online) {
            d.items().adminBuff(c.session, p, effect, ms);
         } else {
            ItemBuffs.applyFor(p, effect, System.currentTimeMillis(), ms);
         }
         Log.info("Admin panel: buff item %d %d phut cho '%s'", itemTemplateId, ms / 60_000, Log.ascii(p.name));
         return "Đã bật hiệu ứng " + itemName(itemTemplateId) + " (" + Math.round(ms / 60_000.0) + " phút) cho "
               + p.name + ".";
      });
   }

   Object buffRemove(int playerId, int optionId) throws SQLException {
      return run(playerId, true, (p, c) -> {
         if (c.online) {
            d.items().adminRemoveBuff(c.session, p, optionId);
         } else {
            ItemBuffs.remove(p, java.util.Set.of(optionId));
         }
         return "Đã gỡ hiệu ứng.";
      });
   }

   /** Hiệu ứng item bật được: {item, name, option, optionName, minutes}. */
   static List<Map<String, Object>> buffList() {
      List<Map<String, Object>> list = new ArrayList<>();
      for (ItemBuffs.Effect e : ItemBuffs.itemEffects()) {
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("item", e.itemTemplateId());
         row.put("name", itemName(e.itemTemplateId()));
         ItemPack.OptionInfo info = GameDataCache.option(e.optionId());
         row.put("option", e.optionId());
         row.put("description", info == null ? "" : AdminItems.clean(info.description()));
         row.put("minutes", e.durationMs() / 60_000.0);
         list.add(row);
      }
      list.sort((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")));
      return list;
   }

   /**
    * Tặng đồ qua thư hệ thống. {@code items}: mảng RewardItem {"i","q","l","s","d"} (item 1 = vàng, khoá -> vàng khoá;
    * 2 = lam thạch). Tối đa {@link MailService#MAX_ITEMS} món / thư, nhiều hơn thì chia thư.
    */
   Object gift(int playerId, JsonArray raw, String title, String content) throws SQLException {
      List<ItemData> items = AdminApi.rewards(raw);
      if (items.isEmpty() && title.isBlank()) {
         throw new AdminApi.BadRequest("Thư trống: nhập tiêu đề hoặc thêm ít nhất 1 món.");
      }
      String name = nameOf(playerId);
      List<List<ItemData>> parts = new ArrayList<>();
      for (int i = 0; i < items.size(); i += MailService.MAX_ITEMS) {
         parts.add(new ArrayList<>(items.subList(i, Math.min(items.size(), i + MailService.MAX_ITEMS))));
      }
      if (parts.isEmpty()) {
         parts.add(new ArrayList<>());
      }
      String subject = title.isBlank() ? "Quà từ quản trị viên" : title;
      for (List<ItemData> part : parts) {
         Mail mail = d.mails().systemMail(playerId, name, subject, content, part);
         d.players().runOnWriter(conn -> MailRepository.insert(conn, mail)).thenAccept(ok -> {
            if (ok) {
               d.mails().deliver(mail);
            }
         });
      }
      Log.info("Admin panel: tang %d mon cho #%d '%s'", items.size(), playerId, Log.ascii(name));
      return AdminApi.ok("Đã gửi " + parts.size() + " thư (" + items.size() + " món) vào Hòm thư của " + name + ".");
   }

   // ================================================================== thao tác item như trong game

   /** Tên 4 ô dùng nhanh ({@link PlayerData#quickSlots}). */
   static final String[] QUICK = {"HP", "MP", "Thể lực", "Dự phòng"};

   /**
    * Thao tác trên 1 món như nút trong game. {@code where}: bag / box / body / col (bộ sưu tập, {@code kind} =
    * {@link CollectionKind#ordinal}, {@code slot} = vị trí trong bộ sưu tập).
    * <pre>
    * body: unequip                                   (tháo vào hành trang)
    * bag : use (online) · equip (online) · store (cất rương) · lock / unlock · quick {quick 0-3, -1 = bỏ}
    *       sell (bán như game: giá tạm {@link ItemService#sellPrice} vàng khoá) · collect (cho vào bộ sưu tập)
    * box : take (lấy ra hành trang)
    * col : select (dùng / thôi dùng) · take (lấy ra hành trang)
    * </pre>
    * "use" / "equip" chạy ĐÚNG luồng của game trên hàng đợi người chơi (hộp hỏi, hiệu ứng, nhiệm vụ...) nên chỉ khi online.
    */
   Object itemAction(int playerId, String where, int slot, String action, JsonObject b) throws SQLException {
      return run(playerId, true, (p, c) -> {
         String w = where == null ? "" : where;
         String a = action == null ? "" : action;
         if ((w.equals("bag") || w.equals("box")) && p.trade != null) {
            throw new AdminApi.BadRequest(p.name + " đang giao dịch, thử lại sau.");
         }
         switch (w + "/" + a) {
            case "body/unequip" -> {
               ItemData it = need(p.body, slot);
               int empty = p.bag.firstEmpty();
               if (empty < 0) {
                  throw new AdminApi.BadRequest("Hành trang đã đầy.");
               }
               p.bag.set(empty, p.body.take(slot));
               c.all = true;
               return "Đã tháo " + itemName(it.templateId) + " vào hành trang.";
            }
            case "bag/use", "bag/equip" -> {
               need(p.bag, slot);
               if (!c.online) {
                  throw new AdminApi.BadRequest("Chỉ dùng / mặc hộ được khi nhân vật đang online.");
               }
               if (a.equals("use")) {
                  d.items().use(c.session, slot, Math.max(1, AdminApi.intField(b, "qty")));
               } else {
                  d.items().equip(c.session, slot);
               }
               return "Đã thực hiện như người chơi tự bấm (kết quả hiện trong game).";
            }
            case "bag/store" -> {
               ItemData it = need(p.bag, slot);
               if (p.box.add(it) < 0) {
                  throw new AdminApi.BadRequest("Rương đã đầy.");
               }
               p.bag.take(slot);
               c.all = true;
               return "Đã cất " + itemName(it.templateId) + " vào rương.";
            }
            case "box/take" -> {
               ItemData it = need(p.box, slot);
               if (p.bag.add(it) < 0) {
                  throw new AdminApi.BadRequest("Hành trang đã đầy.");
               }
               p.box.take(slot);
               c.all = true;
               return "Đã lấy " + itemName(it.templateId) + " ra hành trang.";
            }
            case "bag/lock", "bag/unlock" -> {
               ItemData it = need(p.bag, slot).copy();
               it.locked = a.equals("lock");
               p.bag.set(slot, it);
               c.all = true;
               return (it.locked ? "Đã khoá " : "Đã mở khoá ") + itemName(it.templateId) + ".";
            }
            case "bag/quick" -> {
               int q = AdminApi.intField(b, "quick");
               ItemData it = need(p.bag, slot);
               if (q < 0) {
                  for (int i = 0; i < p.quickSlots.length; i++) {
                     if (p.quickSlots[i] == slot) {
                        p.quickSlots[i] = -1;
                     }
                  }
                  c.all = true;
                  return "Đã bỏ phím tắt.";
               }
               if (q >= p.quickSlots.length) {
                  throw new AdminApi.BadRequest("Ô dùng nhanh 0-" + (p.quickSlots.length - 1) + ".");
               }
               ItemPack.ItemType type = it.type();
               if (type == null || type.equippable()) {
                  throw new AdminApi.BadRequest("Trang bị không đặt vào ô dùng nhanh được.");
               }
               p.quickSlots[q] = slot;
               c.all = true;
               return "Đã gán " + itemName(it.templateId) + " vào ô dùng nhanh " + QUICK[q] + ".";
            }
            case "bag/sell" -> {
               ItemData it = need(p.bag, slot);
               long price = ItemService.sellPrice(it);
               p.bag.take(slot);
               synchronized (p) {
                  p.lockedGold = Math.min(ItemService.MAX_GOLD, p.lockedGold + price);
               }
               c.all = true;
               return "Đã bán " + itemName(it.templateId) + (it.quantity > 1 ? " x" + it.quantity : "") + " được "
                     + price + " vàng khoá.";
            }
            case "bag/collect" -> {
               ItemData it = need(p.bag, slot);
               CollectionKind kind = CollectionKind.forItemType(it.typeId());
               if (kind == null) {
                  throw new AdminApi.BadRequest("Món này không thuộc bộ sưu tập nào.");
               }
               ItemCollection col = kind.of(p);
               for (ItemData owned : col.items) {
                  if (owned.templateId == it.templateId) {
                     throw new AdminApi.BadRequest("Đã có món này trong " + kind.displayName.toLowerCase() + ".");
                  }
               }
               if (col.isFull()) {
                  throw new AdminApi.BadRequest(kind.displayName + " đã đầy.");
               }
               p.bag.take(slot);
               it.locked = true;
               col.add(it);
               c.all = true;
               return "Đã cho " + itemName(it.templateId) + " vào " + kind.displayName.toLowerCase() + ".";
            }
            case "back/take" -> {
               if (p.back() == null) {
                  throw new AdminApi.BadRequest("Không đeo đồ ở lưng.");
               }
               int empty = p.bag.firstEmpty();
               if (empty < 0) {
                  throw new AdminApi.BadRequest("Hành trang đã đầy.");
               }
               ItemData it = p.backs.remove(p.backs.selected);
               p.bag.set(empty, it);
               ItemService.refreshLook(p);
               if (c.online) {
                  d.world().broadcastStats(p);
               }
               c.all = true;
               return "Đã tháo " + itemName(it.templateId) + " (đồ đeo lưng) vào hành trang.";
            }
            case "col/select", "col/take" -> {
               int k = AdminApi.intField(b, "kind");
               if (k < 0 || k >= CollectionKind.values().length) {
                  throw new AdminApi.BadRequest("Bộ sưu tập không hợp lệ.");
               }
               CollectionKind kind = CollectionKind.values()[k];
               ItemCollection col = kind.of(p);
               ItemData it = col.get(slot);
               if (it == null) {
                  throw new AdminApi.BadRequest("Không có món ở vị trí " + slot + ".");
               }
               if (a.equals("select")) {
                  col.selected = col.selected == slot ? -1 : slot;
                  c.all = true;
                  return col.selected == slot ? "Đang dùng " + itemName(it.templateId) + "." : "Đã thôi dùng.";
               }
               int empty = p.bag.firstEmpty();
               if (empty < 0) {
                  throw new AdminApi.BadRequest("Hành trang đã đầy.");
               }
               p.bag.set(empty, col.remove(slot));
               c.all = true;
               return "Đã lấy " + itemName(it.templateId) + " ra hành trang.";
            }
            default -> throw new AdminApi.BadRequest("Thao tác không hợp lệ: " + w + "/" + a + ".");
         }
      });
   }

   private static ItemData need(ItemContainer box, int slot) {
      ItemData it = box.valid(slot) ? box.get(slot) : null;
      if (it == null) {
         throw new AdminApi.BadRequest("Ô " + slot + " đang trống.");
      }
      return it;
   }

   // ================================================================== chỉ số nhân vật / vị trí / kỹ năng

   /** Đặt EXP trong cấp, điểm tiềm năng, thể lực, sát khí (trường nào gửi thì đổi). */
   Object editStats(int playerId, JsonObject b) throws SQLException {
      return run(playerId, true, (p, c) -> {
         StringBuilder log = new StringBuilder();
         if (b.has("exp")) {
            long need = Math.max(1, Leveling.expToLevelUp(p));
            long v = b.get("exp").getAsLong();
            if (v < 0 || v >= need) {
               throw new AdminApi.BadRequest("EXP trong cấp 0-" + (need - 1) + " (muốn lên cấp hãy dùng Nâng cấp).");
            }
            p.exp = v;
            log.append(" exp=").append(v);
         }
         if (b.has("talentPoints")) {
            int v = b.get("talentPoints").getAsInt();
            if (v < 0 || v > 100_000) {
               throw new AdminApi.BadRequest("Điểm tiềm năng 0-100.000.");
            }
            synchronized (p) {
               p.talentPoints = v;
            }
            log.append(" tiem nang=").append(v);
         }
         PlayerStats s = c.online && p.stats != null ? p.stats : PlayerStats.of(p);
         if (b.has("stamina")) {
            int v = b.get("stamina").getAsInt();
            if (v < 0 || v > s.maxStamina) {
               throw new AdminApi.BadRequest("Thể lực 0-" + s.maxStamina + ".");
            }
            p.activityPoints = v;
            log.append(" the luc=").append(v);
         }
         if (b.has("pk")) {
            int v = b.get("pk").getAsInt();
            if (v < 0 || v > 127) {
               throw new AdminApi.BadRequest("Sát khí 0-127.");
            }
            p.pkPoints = v;
            log.append(" sat khi=").append(v);
         }
         if (c.online) {
            c.session.send(com.tlnt.server.world.WorldPackets.stamina(p.activityPoints, s.maxStamina));
            ItemService.refreshLook(p);
            d.world().broadcastStats(p);
         }
         Log.info("Admin panel: '%s'%s", Log.ascii(p.name), log);
         return "Đã cập nhật " + p.name + ".";
      });
   }

   /** Đặt cấp kỹ năng {@code skillId} (0 = chưa học; có trong dữ liệu kỹ năng). */
   Object skillLevel(int playerId, int skillId, int level) throws SQLException {
      var template = GameDataCache.skillTemplate(skillId);
      if (template == null) {
         throw new AdminApi.BadRequest("Không có kỹ năng " + skillId + ".");
      }
      int max = maxSkillLevel(template);
      if (level < 0 || level > max) {
         throw new AdminApi.BadRequest("Cấp kỹ năng 0-" + max + ".");
      }
      return run(playerId, true, (p, c) -> {
         SkillData sk = com.tlnt.server.skill.SkillService.find(p, skillId);
         if (sk == null) {
            throw new AdminApi.BadRequest("Nhân vật chưa có kỹ năng này.");
         }
         sk.level = (byte) level;
         if (c.online) {
            c.session.send(com.tlnt.server.world.WorldPackets.learnedSkills(p.skills));
            c.session.send(com.tlnt.server.world.WorldPackets.skillBar(p.skillBar));
            ItemService.refreshLook(p);
            d.world().broadcastStats(p);
         }
         Log.info("Admin panel: '%s' ky nang %d -> cap %d", Log.ascii(p.name), skillId, level);
         return template.name() + " của " + p.name + " đặt cấp " + level + ".";
      });
   }

   static int maxSkillLevel(com.tlnt.server.data.model.SkillPack.Skill template) {
      int max = 0;
      if (template.levels() != null) {
         for (var l : template.levels()) {
            max = Math.max(max, l.level());
         }
      }
      return max;
   }

   /** Dịch chuyển: online -> chuyển map ngay; offline -> đổi vị trí lưu (lần vào sau xuất hiện ở đó). */
   Object teleport(int playerId, int mapId, Float x, Float y) throws SQLException {
      if (d.world().mapTemplate(mapId) == null) {
         throw new AdminApi.BadRequest("Không có map " + mapId + ".");
      }
      WorldPlayer wp = d.world().online(playerId);
      if (wp != null) {
         String error;
         try {
            error = d.world().adminTeleport(wp.session(), mapId, x, y).get(WAIT_SECONDS, TimeUnit.SECONDS);
         } catch (Exception e) {
            throw new AdminApi.BadRequest("Chưa dịch chuyển được: " + e.getMessage());
         }
         if (error != null) {
            throw new AdminApi.BadRequest(error);
         }
         Log.info("Admin panel: dich chuyen '%s' toi map %d", Log.ascii(wp.data().name), mapId);
         return AdminApi.ok("Đã dịch chuyển " + wp.data().name + " tới " + d.mapName(mapId) + ".");
      }
      if (d.world().adminTeleportBlocked(mapId)) {
         throw new AdminApi.BadRequest("Không đặt vị trí trong phó bản / chiến trường / đài đấu.");
      }
      return run(playerId, true, (p, c) -> {
         p.mapId = mapId;
         p.x = x;
         p.y = y;
         return "Đã đặt vị trí " + p.name + " ở " + d.mapName(mapId) + " (áp dụng lần vào game sau).";
      });
   }

   /** Danh sách map cho ô chọn vị trí: {id, tên, cấm dịch chuyển}. */
   Object mapList() {
      List<Object[]> list = new ArrayList<>();
      for (Object[] m : d.world().mapStats()) {
         int id = (Integer) m[0];
         list.add(new Object[] {id, m[1], d.world().adminTeleportBlocked(id)});
      }
      list.sort((a, b) -> Integer.compare((Integer) a[0], (Integer) b[0]));
      return list;
   }

   // ================================================================== nội bộ

   /**
    * Chạy {@code op} trên nhân vật {@code playerId}: online -> hàng đợi session; offline -> SQL (ghi thì giữ tài khoản).
    * {@code write} = sửa (gửi lại client + lưu).
    */
   Object run(int playerId, boolean write, Op op) throws SQLException {
      WorldPlayer wp = d.world().online(playerId);
      if (wp != null) {
         PlayerSession s = wp.session();
         PlayerData pd = wp.data();
         CompletableFuture<Object> f = new CompletableFuture<>();
         s.inbox().execute(() -> {
            try {
               if (s.getPlayer() != pd) {
                  throw new AdminApi.BadRequest("Nhân vật vừa thoát game, thử lại.");
               }
               Change c = new Change(true, s);
               Object result = op.apply(pd, c);
               if (write) {
                  if (c.all) {
                     d.items().adminSyncAll(s, pd);
                  } else if (c.bag || c.box || c.body) {
                     d.items().adminSync(s, pd, c.bag, c.box, c.body);
                  } else if (c.currency) {
                     s.send(com.tlnt.server.item.ItemPackets.currency(pd));
                  }
                  d.players().saveAsync(pd, false);
               }
               f.complete(result);
            } catch (Throwable e) {
               f.completeExceptionally(e);
            }
         }, d.gamePool());
         return wrap(await(f));
      }
      int accountId = accountOf(playerId);
      if (d.accounts().isOnline(accountId)) {
         throw new AdminApi.BadRequest("Tài khoản đang đăng nhập (chưa vào game), thử lại sau ít giây.");
      }
      if (!write) {
         PlayerData pd = d.players().loadOffline(accountId);
         if (pd == null) {
            throw new AdminApi.BadRequest("Không có nhân vật #" + playerId + ".");
         }
         return op.apply(pd, new Change(false, null));
      }
      if (!d.accounts().adminHold(accountId)) {
         throw new AdminApi.BadRequest("Tài khoản vừa đăng nhập / đang được xử lý, thử lại.");
      }
      try {
         PlayerData pd = d.players().loadOffline(accountId);
         if (pd == null) {
            throw new AdminApi.BadRequest("Không có nhân vật #" + playerId + ".");
         }
         Object result = op.apply(pd, new Change(false, null));
         if (!d.players().saveOffline(pd)) {
            throw new AdminApi.BadRequest("Lỗi ghi CSDL, thay đổi chưa được lưu (xem nhật ký).");
         }
         if (d.rankings() != null) {
            d.rankings().update(pd);
         }
         return wrap(result);
      } finally {
         d.accounts().adminRelease(accountId);
      }
   }

   private static Object wrap(Object result) {
      return result instanceof String text ? AdminApi.ok(text) : result;
   }

   private static Object await(CompletableFuture<Object> f) {
      try {
         return f.get(WAIT_SECONDS, TimeUnit.SECONDS);
      } catch (ExecutionException e) {
         Throwable cause = e.getCause();
         if (cause instanceof RuntimeException re) {
            throw re;
         }
         throw new IllegalStateException(cause);
      } catch (TimeoutException e) {
         throw new AdminApi.BadRequest("Hàng đợi của người chơi bận, chưa xong sau " + WAIT_SECONDS
               + " giây (thao tác vẫn có thể được áp dụng sau đó).");
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new AdminApi.BadRequest("Bị ngắt.");
      }
   }

   /** Kho theo tên: bag / box / body; đánh dấu đã đổi. Đang giao dịch thì không đụng hành trang. */
   private static ItemContainer container(PlayerData p, String where, Change c, boolean allowBody) {
      switch (where == null ? "" : where) {
         case "bag" -> {
            if (p.trade != null) {
               throw new AdminApi.BadRequest(p.name + " đang giao dịch, thử lại sau.");
            }
            c.bag = true;
            return p.bag;
         }
         case "box" -> {
            c.box = true;
            return p.box;
         }
         case "body" -> {
            if (!allowBody) {
               throw new AdminApi.BadRequest("Thêm đồ vào hành trang rồi để người chơi tự mặc.");
            }
            c.body = true;
            return p.body;
         }
         default -> throw new AdminApi.BadRequest("Kho phải là bag / box / body.");
      }
   }

   private static String label(String where) {
      return switch (where) {
         case "box" -> "rương";
         case "body" -> "đồ đang mặc";
         default -> "hành trang";
      };
   }

   private int accountOf(int playerId) throws SQLException {
      try (Connection c = d.playerDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT account_id FROM player WHERE id = ?")) {
         ps.setInt(1, playerId);
         try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
               throw new AdminApi.BadRequest("Không có nhân vật #" + playerId + ".");
            }
            return rs.getInt(1);
         }
      }
   }

   private String nameOf(int playerId) throws SQLException {
      WorldPlayer p = d.world().online(playerId);
      if (p != null) {
         return p.data().name;
      }
      try (Connection c = d.playerDb().getConnection();
            PreparedStatement ps = c.prepareStatement("SELECT name FROM player WHERE id = ?")) {
         ps.setInt(1, playerId);
         try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
               throw new AdminApi.BadRequest("Không có nhân vật #" + playerId + ".");
            }
            return rs.getString(1);
         }
      }
   }

   static String className(int classId) {
      GamePack.CharClass c = GameDataCache.charClass(classId);
      return c == null ? "#" + classId : c.name();
   }

   private static String itemName(int templateId) {
      ItemPack.ItemTemplate t = GameDataCache.itemTemplate(templateId);
      return t == null ? "#" + templateId : t.name();
   }
}
