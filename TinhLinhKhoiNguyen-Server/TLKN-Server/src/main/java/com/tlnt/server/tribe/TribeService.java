package com.tlnt.server.tribe;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.item.Appearance;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packet;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.Gender;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.quest.MainQuests;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.shop.Shop;
import com.tlnt.server.shop.TribeShop;
import com.tlnt.server.social.Invites;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;
import com.tlnt.server.world.Zone;

import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToLongFunction;
import java.util.regex.Pattern;

/**
 * Bộ lạc (op 37, cửa sổ {@code TribeWindow}; spec "Spec hệ thống Bộ lạc"). Lane GAME.
 *
 * <h2>Luồng / khoá</h2>
 * Mọi bộ lạc nằm trong RAM ({@link #tribes}, nạp hết lúc khởi động) và CHỈ sửa dưới 1 khoá chung ({@code this}) — thao
 * tác bộ lạc là bấm nút, hiếm, như tổ đội. Thứ tự khoá luôn {@code synchronized (player)} -> {@code this} (góp quỹ, tạo
 * bộ lạc: trừ vàng / lam thạch / Cờ lệnh của người bấm và cộng vào bộ lạc trong cùng 1 lượt ghi
 * {@link PlayerService#saveWith}). Trong khoá KHÔNG lấy khoá nhân vật khác. Mọi lượt ghi DB xếp hàng ngay trong khoá với
 * ảnh chụp lúc đó -> thứ tự ghi = thứ tự trong RAM.
 *
 * <h2>Buff</h2>
 * Thành viên online mang {@link TribeTag} ({@code PlayerData.tribe}): khối chỉ số đọc tên + huy hiệu trên đầu,
 * {@code ItemStats} / {@code ZoneCombat} đọc kỹ năng + biểu tượng ({@link TribeRules}). Vào / rời / bộ lạc đổi kỹ năng /
 * biểu tượng -> thay tag + tính lại chỉ số ngay (không cần đăng nhập lại). Kỹ năng "đứng cạnh" (2, 4): {@link #tick}
 * 5 s / lần đếm người cùng bộ lạc cùng khu trong {@code tribe.nearbyPx} trên luồng tick khu.
 */
public final class TribeService implements TribeShop {

   /** Chu kỳ {@link #tick} (giây): hết hạn biểu tượng / đơn xin vào, ghi điểm cống hiến gom lại, đếm "đứng cạnh". */
   public static final int TICK_SECONDS = 5;
   private static final long ACTION_COOLDOWN_MS = 400;
   private static final long SEARCH_COOLDOWN_MS = 100;
   private static final long INVITE_TTL_MS = 60_000;
   private static final long CONFIRM_TTL_MS = 60_000;
   private static final int MAX_NOTICES = 50;
   private static final long HOUR_MS = 3_600_000L;
   private static final long DAY_MS = 24 * HOUR_MS;
   /** Icon danh hiệu cạnh tên bộ lạc trên đầu (spec mục 3). */
   static final String BADGE_LEADER = "icon/721";
   static final String BADGE_DEPUTY = "icon/722";
   static final String BADGE_TOP_POINTS = "icon/723";
   static final String BADGE_TOP_DIAMOND = "icon/724";
   static final String BADGE_TOP_GOLD = "icon/725";
   /** item_type Biểu tượng Bộ lạc. */
   static final int TYPE_EMBLEM = 32;
   /** stat_type 57 "HSD {0} ngày" trên món biểu tượng = số ngày hiệu lực (chỉ hiện chữ, món trong rương không hết hạn). */
   static final int STAT_EXPIRE_DAYS = 57;
   private static final Pattern NAME = Pattern.compile("^[\\p{L}\\p{N}]+( [\\p{L}\\p{N}]+)*$");
   private static final DateTimeFormatter NOTICE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

   private final TribeConfig cfg;
   private final World world;
   private final PlayerService players;
   private final ItemService items;
   private final Executor gamePool;
   private final Map<Integer, Tribe> tribes = new LinkedHashMap<>();
   /** Người chơi id -> bộ lạc đang ở. */
   private final Map<Integer, Tribe> byMember = new HashMap<>();
   private int nextId;
   /** Lời mời: người được mời -> id BỘ LẠC mời (không phải id người mời). */
   private final Invites invites = new Invites(INVITE_TTL_MS);
   /** Hộp xác nhận đang chờ (Tù trưởng id -> việc). */
   private final Map<Integer, Pending> pending = new HashMap<>();
   /** Tù trưởng id -> món biểu tượng đang chờ "Đồng ý" thay biểu tượng hiện tại (so theo tham chiếu). */
   private final Map<Integer, ItemData> pendingEmblem = new HashMap<>();

   private record Pending(byte kind, int tribeId, int target, long at) {
   }

   TribeService(TribeConfig cfg, TribeRepository repository, World world, PlayerService players,
         ItemService items, Executor gamePool) throws SQLException {
      this.cfg = cfg;
      this.world = world;
      this.players = players;
      this.items = items;
      this.gamePool = gamePool;
      TribeRules.configure(cfg);
      nextId = repository.maxId();
      for (Tribe t : repository.loadAll()) {
         tribes.put(t.id, t);
         for (Tribe.Member m : t.members) {
            byMember.put(m.id, t);
         }
      }
      instance = this;
      Log.info("Bo lac: %d bo lac, %d thanh vien", tribes.size(), byMember.size());
   }

   /** Mở bằng {@link TribeRepository} mặc định. */
   public static TribeService create(TribeConfig cfg, com.tlnt.server.db.Database playerDb, World world,
         PlayerService players, ItemService items, Executor gamePool) throws SQLException {
      return new TribeService(cfg, new TribeRepository(playerDb), world, players, items, gamePool);
   }

   public int shopId() {
      return cfg.shopId();
   }

   // ================================================================== vào / thoát game

   /** Hook vào game (trước khi vào khu): gắn tag + chụp tên / cấp mới, gửi 37/1-4. */
   public void onLogin(PlayerSession session, PlayerData d) {
      d.tribeNearby = 0;
      Tribe t;
      synchronized (this) {
         t = byMember.get(d.getId());
         d.tribe = null;
         if (t != null) {
            Tribe.Member m = t.member(d.getId());
            m.name = d.name;
            m.level = d.level;
            m.tpl = templateId(d);
            d.tribe = tag(t, m);
            session.send(TribePackets.mine(t, this::onlineData));
            session.send(TribePackets.notices(t.notices));
            session.send(TribePackets.warehouse(t.items));
            session.send(TribePackets.skills(t.skills));
         } else {
            session.send(TribePackets.mine(null, id -> null));
         }
      }
      if (t != null) {
         ItemService.refreshLook(d); // chỉ số đã tính trước hook này -> tính lại với buff bộ lạc
      }
   }

   public synchronized void onLogout(PlayerData d) {
      invites.remove(d.getId());
      pending.remove(d.getId());
      pendingEmblem.remove(d.getId());
      Tribe t = byMember.get(d.getId());
      Tribe.Member m = t == null ? null : t.member(d.getId());
      if (m != null) {
         m.level = d.level;
         m.name = d.name;
      }
   }

   // ================================================================== op 37 C→S

   /** Sub 0: tab "Bộ lạc" (mở tab + gõ ô tìm kiếm mỗi phím) -> danh sách bộ lạc khớp tên. */
   public void search(PlayerSession session, String query) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastTribeSearchAt < SEARCH_COOLDOWN_MS) {
         return;
      }
      d.lastTribeSearchAt = now;
      String key = nameKey(query == null ? "" : query);
      Message msg;
      synchronized (this) {
         List<Tribe> found = new ArrayList<>();
         for (Tribe t : tribes.values()) {
            if (key.isEmpty() || nameKey(t.name).contains(key)) {
               found.add(t);
            }
         }
         found.sort(Comparator.comparingInt((Tribe t) -> -t.level).thenComparingInt(t -> -t.members.size())
               .thenComparingInt(t -> t.id));
         msg = TribePackets.list(found.subList(0, Math.min(found.size(), Math.min(cfg.listMax(), Byte.MAX_VALUE))),
               this::onlineData);
      }
      session.send(msg);
   }

   /** Sub 1 "Tạo bộ lạc" (tên): trừ 1 Cờ lệnh Bộ lạc, người tạo thành Tù trưởng. */
   public void create(PlayerSession session, String rawName) {
      PlayerData d = actor(session);
      if (d == null) {
         return;
      }
      String name = cleanName(rawName);
      String error = name.isEmpty() || name.length() > cfg.nameMax() || !NAME.matcher(name).matches()
            ? "Tên bộ lạc dài 1-" + cfg.nameMax() + " ký tự, chỉ gồm chữ (có dấu), số và khoảng trắng." : busy(d);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      long now = System.currentTimeMillis();
      Tribe t;
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (ItemService.count(d.bag, cfg.createItem(), now) < 1) {
            session.send(Packets.notice("Cần 1 " + itemName(cfg.createItem()) + " để tạo bộ lạc (mua ở Tư tế)."));
            return;
         }
         synchronized (this) {
            if (byMember.containsKey(d.getId())) {
               session.send(Packets.notice("Bạn đã ở trong bộ lạc."));
               return;
            }
            String key = nameKey(name);
            for (Tribe other : tribes.values()) {
               if (nameKey(other.name).equals(key)) {
                  session.send(Packets.notice("Tên bộ lạc đã có người sử dụng."));
                  return;
               }
            }
            ItemContainer bag = d.bag.deepCopy();
            ItemService.remove(bag, cfg.createItem(), 1, now);
            d.bag = bag;
            t = new Tribe();
            t.id = ++nextId;
            t.name = name;
            t.createdAt = now;
            t.members.add(new Tribe.Member(d.getId(), d.name, d.level, templateId(d), TribeRules.ROLE_LEADER, now));
            tribes.put(t.id, t);
            byMember.put(d.getId(), t);
            removeApplications(d.getId());
            notice(t, d.name + " đã lập bộ lạc " + t.name + ".");
            TribeRepository.Row row = TribeRepository.snapshot(t);
            TribeRepository.LogEntry log = log(t, d, "create", 0, 0, "Tạo bộ lạc, -1 " + itemName(cfg.createItem()));
            players.saveWith(d, conn -> {
               TribeRepository.save(conn, row);
               TribeRepository.log(conn, log);
            });
            d.tribe = tag(t, t.member(d.getId()));
            d.tribeNearby = 0;
            pushAll(t);
         }
      }
      items.syncBagAndCurrency(session);
      refreshStats(d);
      session.send(Packets.notice("Đã tạo bộ lạc " + name + "!"));
      Log.debug("Bo lac #%d '%s' duoc tao boi '%s'", t.id, name, d.name);
   }

   /** Sub 2 "Xin vào" (int id bộ lạc): đơn vào tab "Chờ duyệt", Tù trưởng / Phó duyệt. */
   public synchronized void apply(PlayerSession session, int tribeId) {
      PlayerData d = actor(session);
      if (d == null) {
         return;
      }
      Tribe t = tribes.get(tribeId);
      String error = byMember.containsKey(d.getId()) ? "Bạn đã ở trong bộ lạc."
            : t == null ? "Bộ lạc không còn tồn tại."
            : joinBlocked(d, System.currentTimeMillis());
      if (error == null && t.full()) {
         error = "Bộ lạc đã đủ " + t.maxMembers() + " thành viên.";
      }
      if (error == null && t.applicant(d.getId()) != null) {
         error = "Đã gửi đơn, hãy chờ Tù trưởng duyệt.";
      }
      if (error == null && applications(d.getId()) >= cfg.maxApplicationsPerPlayer()) {
         error = "Chỉ được xin vào tối đa " + cfg.maxApplicationsPerPlayer() + " bộ lạc cùng lúc.";
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      while (t.applicants.size() >= cfg.maxApplicants()) {
         t.applicants.remove(0);
      }
      t.applicants.add(new Tribe.Applicant(d.getId(), d.name, d.level, templateId(d), System.currentTimeMillis()));
      save(t);
      session.send(Packets.notice("Đã gửi đơn xin vào bộ lạc " + t.name + "."));
      Packet info = TribePackets.mine(t, this::onlineData).toPacket();
      Packet note = Packets.notice(d.name + " (cấp " + d.level + ") xin vào bộ lạc.").toPacket();
      for (Tribe.Member m : t.members) {
         WorldPlayer p = world.online(m.id);
         if (p != null) {
            p.send(info);
            if (m.role != TribeRules.ROLE_MEMBER) {
               p.send(note);
            }
         }
      }
   }

   /** Sub 3 "Đồng ý" ở tab "Chờ duyệt" (Tù trưởng / Phó). */
   public synchronized void approve(PlayerSession session, int playerId) {
      PlayerData d = actor(session);
      Tribe t = officerTribe(session, d);
      if (t == null) {
         return;
      }
      long now = System.currentTimeMillis();
      Tribe.Applicant a = t.applicant(playerId);
      if (a == null || now - a.at() > cfg.applyHours() * HOUR_MS) {
         if (a != null) {
            t.removeApplicant(playerId);
            save(t);
         }
         session.send(Packets.notice("Đơn xin vào đã hết hạn."));
         pushInfo(t);
         return;
      }
      if (byMember.containsKey(playerId)) {
         t.removeApplicant(playerId);
         save(t);
         session.send(Packets.notice(a.name() + " đã vào bộ lạc khác."));
         pushInfo(t);
         return;
      }
      if (t.full()) {
         session.send(Packets.notice("Bộ lạc đã đủ " + t.maxMembers() + " thành viên."));
         return;
      }
      PlayerData target = onlineData(playerId);
      String blocked = target == null ? null : joinBlocked(target, now);
      if (blocked != null) {
         session.send(Packets.notice(a.name() + ": " + blocked));
         return;
      }
      join(t, playerId, target != null ? target.name : a.name(), target != null ? target.level : a.level(),
            target != null ? templateId(target) : a.tpl(), now);
   }

   /** Sub 4 "Từ chối" ở tab "Chờ duyệt". */
   public synchronized void reject(PlayerSession session, int playerId) {
      PlayerData d = actor(session);
      Tribe t = officerTribe(session, d);
      if (t == null || !t.removeApplicant(playerId)) {
         return;
      }
      save(t);
      pushInfo(t);
      send(playerId, Packets.notice("Bộ lạc " + t.name + " đã từ chối đơn xin vào của bạn."));
   }

   /** Sub 5 "Rời đi" (Tù trưởng không rời được, chỉ giải tán). */
   public synchronized void leave(PlayerSession session) {
      PlayerData d = actor(session);
      Tribe t = d == null ? null : byMember.get(d.getId());
      if (t == null) {
         return;
      }
      Tribe.Member m = t.member(d.getId());
      if (m.role == TribeRules.ROLE_LEADER) {
         session.send(Packets.notice("Tù trưởng không thể rời bộ lạc, chỉ có thể giải tán hoặc nhường chức."));
         return;
      }
      long now = System.currentTimeMillis();
      removeMember(t, m, now, true);
      notice(t, d.name + " đã rời bộ lạc.");
      save(t);
      pushInfo(t);
      pushNotices(t);
      session.send(Packets.notice("Bạn đã rời bộ lạc " + t.name + ". " + cfg.rejoinHours()
            + " giờ sau mới vào được bộ lạc khác."));
   }

   /** Sub 6 "Giải tán" (Tù trưởng): hỏi 2 lần ({@link Packets#CONFIRM_TRIBE_DISBAND} rồi ..._FINAL). */
   public synchronized void disband(PlayerSession session) {
      PlayerData d = actor(session);
      Tribe t = leaderTribe(session, d, "Chỉ Tù trưởng được giải tán bộ lạc.");
      if (t == null) {
         return;
      }
      pending.put(d.getId(), new Pending(Packets.CONFIRM_TRIBE_DISBAND, t.id, 0, System.currentTimeMillis()));
      session.send(Packets.confirm(Packets.CONFIRM_TRIBE_DISBAND, "Giải tán bộ lạc " + t.name
            + "? Mọi thành viên sẽ rời bộ lạc và mất buff."));
   }

   /** Op 32 "Đồng ý" ở hộp hỏi giải tán: lần 1 -> hỏi lại; lần 2 ({@code finalStep}) -> giải tán thật. */
   public synchronized void confirmDisband(PlayerSession session, boolean finalStep) {
      PlayerData d = session.getPlayer();
      byte kind = finalStep ? Packets.CONFIRM_TRIBE_DISBAND_FINAL : Packets.CONFIRM_TRIBE_DISBAND;
      Pending p = d == null ? null : takePending(d.getId(), kind);
      Tribe t = p == null ? null : leaderTribe(session, d, "Chỉ Tù trưởng được giải tán bộ lạc.");
      if (t == null || t.id != p.tribeId()) {
         return;
      }
      if (!finalStep) {
         pending.put(d.getId(), new Pending(Packets.CONFIRM_TRIBE_DISBAND_FINAL, t.id, 0, System.currentTimeMillis()));
         session.send(Packets.confirm(Packets.CONFIRM_TRIBE_DISBAND_FINAL, "Xác nhận lần cuối: giải tán " + t.name
               + ", ngân sách còn lại (" + format(t.gold) + " vàng, " + format(t.diamond)
               + " lam thạch) và rương bộ lạc sẽ mất hết."));
         return;
      }
      long now = System.currentTimeMillis();
      tribes.remove(t.id);
      Packet gone = TribePackets.mine(null, id -> null).toPacket();
      Packet note = Packets.notice("Bộ lạc " + t.name + " đã giải tán.").toPacket();
      for (Tribe.Member m : t.members) {
         byMember.remove(m.id);
         WorldPlayer wp = world.online(m.id);
         if (wp != null) {
            wp.data().tribe = null;
            wp.data().tribeNearby = 0;
            wp.send(gone);
            wp.send(note);
            refreshStats(wp.data());
         }
      }
      TribeRepository.LogEntry log = log(t, d, "disband", -t.gold, -t.diamond, "Giải tán, " + t.items.size()
            + " ô rương mất");
      int id = t.id;
      players.runOnWriter(conn -> {
         TribeRepository.delete(conn, id);
         TribeRepository.log(conn, log);
      });
      Log.debug("Bo lac #%d '%s' giai tan boi '%s' (%d vang, %d lam thach)", t.id, t.name, d.name, t.gold,
            t.diamond);
   }

   /** Sub 7 "Góp vàng" / sub 8 "Góp L.Thạch" (số nhập): trừ người góp, cộng quỹ + đóng góp cá nhân. */
   public void donate(PlayerSession session, int amount, boolean diamond) {
      PlayerData d = actor(session);
      if (d == null) {
         return;
      }
      String unit = diamond ? "lam thạch" : "vàng";
      if (amount <= 0) {
         session.send(Packets.notice("Số " + unit + " không hợp lệ."));
         return;
      }
      String error = busy(d);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if ((diamond ? d.diamond : d.gold) < amount) {
            session.send(Packets.notice("Không đủ " + format(amount) + " " + unit + "."));
            return;
         }
         synchronized (this) {
            Tribe t = byMember.get(d.getId());
            if (t == null) {
               session.send(Packets.notice("Bạn chưa ở trong bộ lạc."));
               return;
            }
            Tribe.Member m = t.member(d.getId());
            if (diamond) {
               d.diamond -= amount;
               t.diamond += amount;
               m.diamond += amount;
            } else {
               d.gold -= amount;
               t.gold += amount;
               m.gold += amount;
            }
            notice(t, d.name + " đã góp " + format(amount) + " " + unit + " vào ngân sách.");
            boolean titles = updateTops(t);
            TribeRepository.Row row = TribeRepository.snapshot(t);
            TribeRepository.LogEntry log = log(t, d, "donate", diamond ? 0 : amount, diamond ? amount : 0, "");
            players.saveWith(d, conn -> {
               TribeRepository.save(conn, row);
               TribeRepository.log(conn, log);
            });
            t.dirty = false;
            pushInfo(t);
            pushNotices(t);
            if (titles) {
               retag(t);
            }
         }
      }
      items.syncBagAndCurrency(session);
      session.send(Packets.notice("Đã góp " + format(amount) + " " + unit + " cho bộ lạc."));
   }

   /** Sub 9 "Mở kỹ năng" (Tù trưởng): ô trống + {@code tribe.openSkillGold} vàng quỹ -> 1 kỹ năng ngẫu nhiên chưa có. */
   public synchronized void openSkill(PlayerSession session) {
      PlayerData d = actor(session);
      Tribe t = leaderTribe(session, d, "Chỉ Tù trưởng được mở kỹ năng.");
      if (t == null) {
         return;
      }
      int slots = TribeRules.skillSlots(t.level);
      String error = t.skills.size() >= slots
            ? (slots == 0 ? "Bộ lạc cấp 1 mới có ô kỹ năng đầu tiên."
               : slots >= TribeRules.MAX_SKILLS ? "Bộ lạc đã mở đủ " + TribeRules.MAX_SKILLS + " kỹ năng."
               : "Hết ô kỹ năng, bộ lạc cấp " + nextSlotLevel(t.level) + " có thêm ô.")
            : t.gold < cfg.openSkillGold() ? "Ngân sách không đủ " + format(cfg.openSkillGold()) + " vàng."
            : null;
      List<Integer> free = error == null ? freeSkills(t) : List.of();
      if (error == null && free.isEmpty()) {
         error = "Bộ lạc đã có mọi kỹ năng.";
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      int skill = free.get(ThreadLocalRandom.current().nextInt(free.size()));
      t.gold -= cfg.openSkillGold();
      t.skills.add(skill);
      String name = skillName(skill);
      notice(t, d.name + " đã mở kỹ năng " + name + ".");
      saveWithLog(t, log(t, d, "skill_open", -cfg.openSkillGold(), 0, name));
      pushInfo(t);
      pushNotices(t);
      pushSkills(t);
      retag(t);
      session.send(Packets.notice("Đã mở kỹ năng " + name + "."));
   }

   /** Sub 10 "Đổi kỹ năng" (int tribe_skill.id đang chọn): {@code tribe.changeSkillGold} vàng -> kỹ năng khác chưa có. */
   public synchronized void changeSkill(PlayerSession session, int skillId) {
      PlayerData d = actor(session);
      Tribe t = leaderTribe(session, d, "Chỉ Tù trưởng được đổi kỹ năng.");
      if (t == null) {
         return;
      }
      int slot = t.skills.indexOf(skillId);
      List<Integer> free = freeSkills(t);
      String error = slot < 0 ? "Bộ lạc không có kỹ năng này."
            : t.gold < cfg.changeSkillGold() ? "Ngân sách không đủ " + format(cfg.changeSkillGold()) + " vàng."
            : free.isEmpty() ? "Bộ lạc đã có mọi kỹ năng, không còn kỹ năng khác để đổi."
            : null;
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      int skill = free.get(ThreadLocalRandom.current().nextInt(free.size()));
      t.gold -= cfg.changeSkillGold();
      t.skills.set(slot, skill);
      String text = skillName(skillId) + " -> " + skillName(skill);
      notice(t, d.name + " đã đổi kỹ năng " + text + ".");
      saveWithLog(t, log(t, d, "skill_change", -cfg.changeSkillGold(), 0, text));
      pushInfo(t);
      pushNotices(t);
      pushSkills(t);
      retag(t);
      session.send(Packets.notice("Đã đổi kỹ năng " + text + "."));
   }

   /** Sub 11 "Mời vào bộ lạc" (menu chân dung người khác — Tù trưởng / Phó). */
   public synchronized void invite(PlayerSession session, int targetId) {
      PlayerData d = actor(session);
      Tribe t = officerTribe(session, d);
      if (t == null || targetId == d.getId()) {
         return;
      }
      PlayerData target = onlineData(targetId);
      String error = target == null ? "Người chơi không trực tuyến."
            : byMember.containsKey(targetId) ? target.name + " đã ở trong bộ lạc khác."
            : t.full() ? "Bộ lạc đã đủ " + t.maxMembers() + " thành viên."
            : joinBlocked(target, System.currentTimeMillis());
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      invites.put(targetId, t.id, System.currentTimeMillis());
      send(targetId, Packets.invitation(Packets.CONFIRM_TRIBE_INVITE, "Bộ lạc",
            d.name + " mời bạn vào bộ lạc " + t.name + "."));
      session.send(Packets.notice("Đã mời " + target.name + " vào bộ lạc."));
   }

   /** Op 32 "Đồng ý" lời mời ({@link Packets#CONFIRM_TRIBE_INVITE}). */
   public synchronized void acceptInvite(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      Tribe t = tribes.get(invites.take(d.getId(), now));
      String error = t == null ? "Lời mời vào bộ lạc đã hết hạn."
            : byMember.containsKey(d.getId()) ? "Bạn đã ở trong bộ lạc."
            : t.full() ? "Bộ lạc đã đủ " + t.maxMembers() + " thành viên."
            : joinBlocked(d, now);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      join(t, d.getId(), d.name, d.level, templateId(d), now);
   }

   /** Sub 12 "Phong phó Tù trưởng" (Tù trưởng, người được phong đang là Thành viên). */
   public synchronized void appointDeputy(PlayerSession session, int playerId) {
      PlayerData d = actor(session);
      Tribe t = leaderTribe(session, d, "Chỉ Tù trưởng được bổ nhiệm.");
      Tribe.Member m = t == null ? null : t.member(playerId);
      if (m == null || m.role != TribeRules.ROLE_MEMBER) {
         return;
      }
      if (t.count(TribeRules.ROLE_DEPUTY) >= cfg.maxDeputies()) {
         session.send(Packets.notice("Bộ lạc chỉ có tối đa " + cfg.maxDeputies() + " Phó tù trưởng."));
         return;
      }
      m.role = TribeRules.ROLE_DEPUTY;
      notice(t, m.name + " được bổ nhiệm làm Phó tù trưởng.");
      save(t);
      pushInfo(t);
      pushNotices(t);
      retag(t);
   }

   /** Sub 15 "Cắt chức phó Tù trưởng". */
   public synchronized void demoteDeputy(PlayerSession session, int playerId) {
      PlayerData d = actor(session);
      Tribe t = leaderTribe(session, d, "Chỉ Tù trưởng được cắt chức.");
      Tribe.Member m = t == null ? null : t.member(playerId);
      if (m == null || m.role != TribeRules.ROLE_DEPUTY) {
         return;
      }
      m.role = TribeRules.ROLE_MEMBER;
      notice(t, m.name + " bị cắt chức Phó tù trưởng.");
      save(t);
      pushInfo(t);
      pushNotices(t);
      retag(t);
   }

   /** Sub 13 "Nhường Tù trưởng": hỏi lại ({@link Packets#CONFIRM_TRIBE_TRANSFER}). */
   public synchronized void transferLeader(PlayerSession session, int playerId) {
      PlayerData d = actor(session);
      Tribe t = leaderTribe(session, d, "Bạn không phải Tù trưởng.");
      Tribe.Member m = t == null || playerId == d.getId() ? null : t.member(playerId);
      if (m == null) {
         return;
      }
      pending.put(d.getId(), new Pending(Packets.CONFIRM_TRIBE_TRANSFER, t.id, playerId, System.currentTimeMillis()));
      session.send(Packets.confirm(Packets.CONFIRM_TRIBE_TRANSFER, "Nhường chức Tù trưởng cho " + m.name
            + "? Bạn sẽ trở thành Thành viên."));
   }

   /** Op 32 "Đồng ý" nhường Tù trưởng. */
   public synchronized void confirmTransfer(PlayerSession session) {
      PlayerData d = session.getPlayer();
      Pending p = d == null ? null : takePending(d.getId(), Packets.CONFIRM_TRIBE_TRANSFER);
      Tribe t = p == null ? null : leaderTribe(session, d, "Bạn không phải Tù trưởng.");
      Tribe.Member target = t == null || t.id != p.tribeId() ? null : t.member(p.target());
      if (target == null) {
         if (p != null) {
            session.send(Packets.notice("Người này không còn ở trong bộ lạc."));
         }
         return;
      }
      Tribe.Member me = t.member(d.getId());
      me.role = TribeRules.ROLE_MEMBER;
      target.role = TribeRules.ROLE_LEADER;
      t.members.remove(target);
      t.members.add(0, target);
      notice(t, d.name + " đã nhường chức Tù trưởng cho " + target.name + ".");
      save(t);
      pushInfo(t);
      pushNotices(t);
      retag(t);
   }

   /** Sub 14 "Đuổi khỏi bộ lạc" (Tù trưởng; Phó chỉ đuổi được Thành viên). */
   public synchronized void kick(PlayerSession session, int playerId) {
      PlayerData d = actor(session);
      Tribe t = officerTribe(session, d);
      Tribe.Member m = t == null || playerId == d.getId() ? null : t.member(playerId);
      if (m == null) {
         return;
      }
      int myRole = t.member(d.getId()).role;
      if (m.role == TribeRules.ROLE_LEADER || (myRole == TribeRules.ROLE_DEPUTY && m.role != TribeRules.ROLE_MEMBER)) {
         session.send(Packets.notice("Bạn không thể đuổi " + m.name + "."));
         return;
      }
      long now = System.currentTimeMillis();
      boolean online = onlineData(playerId) != null;
      removeMember(t, m, now, true);
      notice(t, m.name + " đã bị đuổi khỏi bộ lạc.");
      TribeRepository.Row row = TribeRepository.snapshot(t);
      t.dirty = false;
      players.runOnWriter(conn -> {
         TribeRepository.save(conn, row);
         if (!online) { // online: mốc rời nằm trong RAM, lưu cùng nhân vật
            TribeRepository.setLeftAt(conn, playerId, now);
         }
      });
      pushInfo(t);
      pushNotices(t);
      send(playerId, Packets.notice("Bạn đã bị đuổi khỏi bộ lạc " + t.name + "."));
   }

   /** Sub 16 "Khẩu hiệu" (Tù trưởng). */
   public synchronized void setSlogan(PlayerSession session, String raw) {
      PlayerData d = actor(session);
      Tribe t = leaderTribe(session, d, "Chỉ Tù trưởng được đổi khẩu hiệu.");
      if (t == null) {
         return;
      }
      String slogan = cleanText(raw);
      if (slogan.length() > cfg.sloganMax()) {
         session.send(Packets.notice("Khẩu hiệu tối đa " + cfg.sloganMax() + " ký tự."));
         return;
      }
      t.slogan = slogan;
      save(t);
      pushInfo(t);
      session.send(Packets.notice("Đã đổi khẩu hiệu."));
   }

   // ================================================================== rương bộ lạc (op 27 sub 14 / 15)

   /**
    * Op 27 sub 14 "Sử dụng" ở Rương bộ lạc (Tù trưởng): biểu tượng -> kích hoạt, hạn = dòng 57 "HSD {0} ngày" của món
    * (không có thì {@code tribe.emblemDays}). Đang có biểu tượng khác còn hạn -> hỏi trước (op 32 loại
    * {@link Packets#CONFIRM_TRIBE_EMBLEM}): biểu tượng mới THAY biểu tượng cũ, hạn tính lại từ lúc dùng.
    */
   public synchronized void useWarehouseItem(PlayerSession session, int slot) {
      PlayerData d = actor(session);
      Tribe t = leaderTribe(session, d, "Chỉ Tù trưởng được dùng vật phẩm trong rương bộ lạc.");
      ItemData item = t == null ? null : emblemItem(session, t, slot);
      if (item == null) {
         return;
      }
      long now = System.currentTimeMillis();
      if (t.emblemId != TribeRules.DEFAULT_EMBLEM && t.emblemExpireAt > now) {
         pendingEmblem.put(d.getId(), item);
         session.send(Packets.confirm(Packets.CONFIRM_TRIBE_EMBLEM, "Sử dụng biểu tượng bộ lạc sẽ thay thế biểu tượng"
               + " hiện tại (" + emblemName(t.emblemId) + ", còn " + remaining(t.emblemExpireAt - now) + "). Dùng "
               + itemName(item.templateId) + "?"));
         return;
      }
      activateEmblem(session, d, t, item, now);
   }

   /** Op 32 "Đồng ý" thay biểu tượng hiện tại. */
   public synchronized void confirmEmblem(PlayerSession session) {
      PlayerData d = session.getPlayer();
      ItemData item = d == null ? null : pendingEmblem.remove(d.getId());
      Tribe t = item == null ? null : leaderTribe(session, d, "Chỉ Tù trưởng được dùng vật phẩm trong rương bộ lạc.");
      int slot = t == null ? -1 : indexOf(t.items, item);
      if (slot < 0 || emblemItem(session, t, slot) == null) {
         return;
      }
      activateEmblem(session, d, t, item, System.currentTimeMillis());
   }

   /** Món biểu tượng dùng được ở ô {@code slot}, không thì báo + null. */
   private ItemData emblemItem(PlayerSession session, Tribe t, int slot) {
      ItemData item = slot >= 0 && slot < t.items.size() ? t.items.get(slot) : null;
      if (item == null || item.isExpired(System.currentTimeMillis())) {
         session.send(Packets.notice("Vật phẩm không còn trong rương."));
         session.send(TribePackets.warehouse(t.items));
         return null;
      }
      if (item.typeId() != TYPE_EMBLEM || emblemOf(item) == null) {
         session.send(Packets.notice("Vật phẩm này chưa dùng được."));
         return null;
      }
      return item;
   }

   private void activateEmblem(PlayerSession session, PlayerData d, Tribe t, ItemData item, long now) {
      GamePack.TribeEmblem emblem = emblemOf(item);
      if (item.quantity > 1) {
         item.quantity--;
      } else {
         t.items.remove(indexOf(t.items, item));
      }
      boolean permanent = emblem.id() == TribeRules.DEFAULT_EMBLEM;
      int days = emblemDays(item);
      t.emblemId = emblem.id();
      t.emblemExpireAt = permanent ? -1 : now + days * DAY_MS;
      t.emblemLines = permanent ? new int[0][] : lines(item);
      String name = itemName(item.templateId);
      notice(t, d.name + " đã dùng " + name + (permanent ? "." : " (" + days + " ngày)."));
      saveWithLog(t, log(t, d, "emblem", 0, 0, name));
      pushInfo(t);
      pushNotices(t);
      pushWarehouse(t);
      retag(t);
      session.send(Packets.notice("Đã kích hoạt " + name + "."));
   }

   /** Op 27 sub 15 "Phân phát" — chưa làm (client không có chỗ chọn người nhận). */
   public void distribute(PlayerSession session, int slot) {
      session.send(Packets.notice("Tính năng phân phát vật phẩm chưa mở."));
   }

   /**
    * Cửa hàng Bộ lạc ({@code ShopService} chuyển sang khi shopId = {@code tribe.shopId}): Tù trưởng mua bằng NGÂN SÁCH
    * (lam thạch / vàng quỹ), hàng vào Rương bộ lạc.
    */
   @Override
   public synchronized void buy(PlayerSession session, int currency, long cost, List<ItemData> bought, String label) {
      PlayerData d = session.getPlayer();
      Tribe t = leaderTribe(session, d, "Chỉ Tù trưởng được mua ở cửa hàng bộ lạc (trả bằng ngân sách).");
      if (t == null) {
         return;
      }
      boolean diamond = currency == Shop.CUR_DIAMOND;
      if (!diamond && currency != Shop.CUR_GOLD && currency != Shop.CUR_LOCKED_GOLD) {
         session.send(Packets.notice("Vật phẩm không bán cho bộ lạc."));
         return;
      }
      if ((diamond ? t.diamond : t.gold) < cost) {
         session.send(Packets.notice("Ngân sách không đủ " + format(cost) + (diamond ? " lam thạch." : " vàng.")));
         return;
      }
      if (t.items.size() + bought.size() > cfg.warehouseSlots()) {
         session.send(Packets.notice("Rương bộ lạc đã đầy (" + cfg.warehouseSlots() + " ô)."));
         return;
      }
      if (diamond) {
         t.diamond -= cost;
      } else {
         t.gold -= cost;
      }
      t.items.addAll(bought);
      notice(t, d.name + " đã mua " + label + " cho bộ lạc.");
      saveWithLog(t, log(t, d, "shop", diamond ? 0 : -cost, diamond ? -cost : 0, label));
      pushInfo(t);
      pushNotices(t);
      pushWarehouse(t);
      session.send(Packets.notice("Đã mua " + label + ", vật phẩm ở Rương bộ lạc."));
   }

   /** Bộ lạc của người này có không (mở Cửa hàng Bộ lạc). */
   /**
    * Admin panel sửa bộ lạc {@code id}: trường nào khác null thì đổi. Tên kiểm như lúc tạo (không trùng), khẩu hiệu tối đa
    * {@code tribe.sloganMax}, cấp 0-{@link TribeRules#MAX_LEVEL}. Ghi DB + gửi lại cho thành viên online (tag / buff theo cấp).
    * @return câu báo lỗi, null = xong
    */
   public synchronized String adminUpdate(int id, String name, String slogan, Integer level, Long exp, Long gold,
         Long diamond) {
      Tribe t = tribes.get(id);
      if (t == null) {
         return "Không có bộ lạc #" + id + ".";
      }
      if (name != null) {
         String n = cleanName(name);
         if (n.isEmpty() || n.length() > cfg.nameMax() || !NAME.matcher(n).matches()) {
            return "Tên bộ lạc dài 1-" + cfg.nameMax() + " ký tự, chỉ gồm chữ (có dấu), số và khoảng trắng.";
         }
         for (Tribe other : tribes.values()) {
            if (other != t && nameKey(other.name).equals(nameKey(n))) {
               return "Tên bộ lạc đã có người sử dụng.";
            }
         }
         t.name = n;
      }
      if (slogan != null) {
         String s = cleanText(slogan);
         if (s.length() > cfg.sloganMax()) {
            return "Khẩu hiệu tối đa " + cfg.sloganMax() + " ký tự.";
         }
         t.slogan = s;
      }
      if (level != null) {
         t.level = Math.max(0, Math.min(TribeRules.MAX_LEVEL, level));
      }
      if (exp != null) {
         t.exp = Math.max(0, exp);
      }
      if (gold != null) {
         t.gold = Math.max(0, gold);
      }
      if (diamond != null) {
         t.diamond = Math.max(0, diamond);
      }
      notice(t, "Quản trị viên đã cập nhật thông tin bộ lạc.");
      save(t);
      retag(t);
      pushAll(t);
      Log.info("Admin panel: sua bo lac #%d", id);
      return null;
   }

   /** Admin đuổi thành viên (không tính thời gian chờ vào lại). Không đuổi được Tù trưởng. @return lỗi, null = xong */
   public synchronized String adminKick(int id, int playerId) {
      Tribe t = tribes.get(id);
      Tribe.Member m = t == null ? null : t.member(playerId);
      if (m == null) {
         return "Không có thành viên này.";
      }
      if (m.role == TribeRules.ROLE_LEADER) {
         return "Không đuổi được Tù trưởng — chuyển Tù trưởng cho người khác trước.";
      }
      boolean online = onlineData(playerId) != null;
      long now = System.currentTimeMillis();
      removeMember(t, m, now, false);
      notice(t, m.name + " đã bị quản trị viên đưa ra khỏi bộ lạc.");
      TribeRepository.Row row = TribeRepository.snapshot(t);
      t.dirty = false;
      players.runOnWriter(conn -> TribeRepository.save(conn, row));
      pushInfo(t);
      pushNotices(t);
      if (online) {
         send(playerId, Packets.notice("Bạn đã được quản trị viên đưa ra khỏi bộ lạc " + t.name + "."));
      }
      Log.info("Admin panel: dua #%d ra khoi bo lac #%d", playerId, id);
      return null;
   }

   /**
    * Admin đổi chức vụ: {@code role} 0 Tù trưởng (người cũ thành thành viên, Tù trưởng luôn đứng đầu danh sách), 1 Phó,
    * 2 Thành viên. @return lỗi, null = xong
    */
   public synchronized String adminSetRole(int id, int playerId, int role) {
      Tribe t = tribes.get(id);
      Tribe.Member m = t == null ? null : t.member(playerId);
      if (m == null) {
         return "Không có thành viên này.";
      }
      if (role < TribeRules.ROLE_LEADER || role > TribeRules.ROLE_MEMBER) {
         return "Chức vụ không hợp lệ.";
      }
      if (m.role == TribeRules.ROLE_LEADER && role != TribeRules.ROLE_LEADER) {
         return "Muốn đổi Tù trưởng hãy chọn người mới làm Tù trưởng.";
      }
      if (role == TribeRules.ROLE_LEADER) {
         for (Tribe.Member other : t.members) {
            if (other.role == TribeRules.ROLE_LEADER) {
               other.role = TribeRules.ROLE_MEMBER;
            }
         }
         t.members.remove(m);
         t.members.add(0, m);
      }
      m.role = role;
      notice(t, m.name + " được quản trị viên đặt làm " + (role == TribeRules.ROLE_LEADER ? "Tù trưởng"
            : role == TribeRules.ROLE_DEPUTY ? "Phó tù trưởng" : "thành viên") + ".");
      save(t);
      retag(t);
      pushInfo(t);
      pushNotices(t);
      Log.info("Admin panel: bo lac #%d: #%d -> chuc vu %d", id, playerId, role);
      return null;
   }

   /** Admin panel: bản chụp mọi bộ lạc (cả thành viên). */
   public synchronized java.util.List<Map<String, Object>> adminList() {
      java.util.List<Map<String, Object>> out = new ArrayList<>();
      for (Tribe t : tribes.values()) {
         Map<String, Object> row = new java.util.LinkedHashMap<>();
         row.put("id", t.id);
         row.put("name", t.name);
         row.put("slogan", t.slogan);
         row.put("level", t.level);
         row.put("exp", t.exp);
         row.put("need", TribeRules.need(t.level + 1));
         row.put("gold", t.gold);
         row.put("diamond", t.diamond);
         row.put("maxMembers", TribeRules.maxMembers(t.level));
         row.put("applicants", t.applicants.size());
         row.put("warehouse", t.items.size());
         row.put("createdAt", t.createdAt);
         java.util.List<Map<String, Object>> members = new ArrayList<>();
         for (Tribe.Member m : t.members) {
            Map<String, Object> mm = new java.util.LinkedHashMap<>();
            mm.put("id", m.id);
            mm.put("name", m.name);
            mm.put("level", m.level);
            mm.put("role", m.role == TribeRules.ROLE_LEADER ? "Tù trưởng" : m.role == TribeRules.ROLE_DEPUTY ? "Phó" : "Thành viên");
            mm.put("roleId", m.role);
            mm.put("points", m.points);
            mm.put("gold", m.gold);
            mm.put("diamond", m.diamond);
            mm.put("joinedAt", m.joinedAt);
            members.add(mm);
         }
         row.put("members", members);
         out.add(row);
      }
      return out;
   }

   public synchronized boolean isMember(int playerId) {
      return byMember.containsKey(playerId);
   }

   // ================================================================== điểm cống hiến

   /**
    * Cộng điểm cống hiến cho người chơi (vào điểm cá nhân + điểm bộ lạc), đủ thì bộ lạc tự lên cấp. Hoạt động hằng ngày
    * gọi hàm này (danh sách hoạt động làm sau). Không thuộc bộ lạc thì bỏ qua. Ghi DB gom theo {@link #tick}.
    */
   public synchronized void addPoints(int playerId, long points) {
      Tribe t = byMember.get(playerId);
      if (t == null || points <= 0) {
         return;
      }
      t.member(playerId).points += points;
      int before = t.level;
      addExp(t, points);
      boolean titles = updateTops(t);
      t.dirty = true;
      if (t.level != before) {
         notice(t, "Bộ lạc đã lên cấp " + t.level + "! Sức chứa " + t.maxMembers() + " thành viên"
               + (TribeRules.skillSlots(t.level) > TribeRules.skillSlots(before)
                  ? ", thêm 1 ô kỹ năng." : "."));
         save(t);
         pushNotices(t);
      }
      pushInfo(t);
      if (titles) {
         retag(t);
      }
   }

   /**
    * Chat bộ lạc (op 26 sub 3, {@code chat/ChatService} đã lọc chữ + chống spam): S→C 26/3 (int id người gửi, str tên,
    * str nội dung) mã hoá 1 lần, gửi mọi thành viên đang online kể cả người gửi. Chưa có bộ lạc -> báo.
    */
   public static void chat(PlayerSession session, PlayerData d, String text) {
      TribeService s = instance;
      if (s != null) {
         s.say(session, d, text);
      }
   }

   private synchronized void say(PlayerSession session, PlayerData d, String text) {
      Tribe t = byMember.get(d.getId());
      if (t == null) {
         session.send(Packets.notice("Bạn chưa có bộ lạc."));
         return;
      }
      Message msg = new Message(com.tlnt.server.net.Opcode.CHAT);
      msg.writeByte(3);
      msg.writeInt(d.getId());
      msg.writeString(d.name);
      msg.writeString(text);
      broadcast(t, msg.toPacket());
      Log.debug("[chat bo lac #%d] '%s': %s", t.id, d.name, text);
   }

   /**
    * Thưởng điểm cống hiến từ 1 hoạt động ({@code reason}, vd "tham gia Rừng Linh Mộc") + báo người nhận. Không thuộc
    * bộ lạc / chưa bật bộ lạc thì bỏ qua. Gọi từ luồng nào cũng được (không giữ khoá nhân vật).
    */
   public static void reward(int playerId, long points, String reason) {
      TribeService s = instance;
      if (s == null || points <= 0 || !s.isMember(playerId)) {
         return;
      }
      s.addPoints(playerId, points);
      s.send(playerId, Packets.notice("+" + points + " điểm cống hiến bộ lạc (" + reason + ")."));
   }

   /** Cộng điểm vào bộ lạc, lên cấp khi đủ (tối đa {@link TribeRules#MAX_LEVEL}). */
   static void addExp(Tribe t, long points) {
      if (t.level >= TribeRules.MAX_LEVEL) {
         t.exp = 0;
         return;
      }
      t.exp += points;
      while (t.level < TribeRules.MAX_LEVEL && t.exp >= TribeRules.need(t.level + 1)) {
         t.exp -= TribeRules.need(t.level + 1);
         t.level++;
      }
      if (t.level >= TribeRules.MAX_LEVEL) {
         t.exp = 0;
      }
   }

   // ================================================================== định kỳ

   /** Scheduler mỗi {@value #TICK_SECONDS} s: hết hạn biểu tượng / đơn, ghi điểm gom, đếm "đứng cạnh". */
   public void tick() {
      long now = System.currentTimeMillis();
      Map<Zone, List<WorldPlayer>> nearbyWork = new HashMap<>();
      synchronized (this) {
         for (Tribe t : tribes.values()) {
            if (t.emblemExpireAt > 0 && now >= t.emblemExpireAt) {
               String old = emblemName(t.emblemId);
               t.emblemId = TribeRules.DEFAULT_EMBLEM;
               t.emblemExpireAt = -1;
               t.emblemLines = new int[0][];
               notice(t, "Biểu tượng " + old + " đã hết hạn, bộ lạc dùng lại biểu tượng Cờ lệnh.");
               t.dirty = true;
               pushInfo(t);
               pushNotices(t);
               retag(t);
            }
            if (t.applicants.removeIf(a -> now - a.at() > cfg.applyHours() * HOUR_MS)) {
               t.dirty = true;
               pushInfo(t);
            }
            if (t.dirty) {
               save(t);
            }
            if (t.hasSkill(TribeRules.SKILL_UNITY) || t.hasSkill(TribeRules.SKILL_SCHOLAR)) {
               for (Tribe.Member m : t.members) {
                  WorldPlayer p = world.online(m.id);
                  Zone zone = p == null ? null : p.zone();
                  if (zone != null) {
                     nearbyWork.computeIfAbsent(zone, z -> new ArrayList<>()).add(p);
                  }
               }
            }
         }
         pending.values().removeIf(p -> now - p.at() > CONFIRM_TTL_MS);
      }
      float range = cfg.nearbyPx();
      for (Map.Entry<Zone, List<WorldPlayer>> e : nearbyWork.entrySet()) {
         Zone zone = e.getKey();
         List<WorldPlayer> list = e.getValue();
         zone.submit(() -> countNearby(zone, list, range * range));
      }
   }

   /** Luồng tick khu: số người cùng bộ lạc đứng cạnh từng người trong {@code list}; đổi thì tính lại chỉ số. */
   private void countNearby(Zone zone, List<WorldPlayer> list, float rangeSq) {
      for (WorldPlayer p : list) {
         if (p.zone() != zone) {
            continue;
         }
         TribeTag tag = p.data().tribe;
         int n = 0;
         if (tag != null) {
            for (WorldPlayer q : list) {
               TribeTag other = q.data().tribe;
               if (q != p && q.zone() == zone && other != null && other.id() == tag.id()) {
                  float dx = q.x() - p.x();
                  float dy = q.y() - p.y();
                  if (dx * dx + dy * dy <= rangeSq) {
                     n++;
                  }
               }
            }
         }
         PlayerData d = p.data();
         if (d.tribeNearby != n) {
            d.tribeNearby = n;
            refreshStats(d);
         }
      }
   }

   // ================================================================== nội bộ (đang giữ khoá)

   private void join(Tribe t, int playerId, String name, int level, int tpl, long now) {
      removeApplications(playerId);
      t.members.add(new Tribe.Member(playerId, name, level, tpl, TribeRules.ROLE_MEMBER, now));
      byMember.put(playerId, t);
      notice(t, name + " đã gia nhập bộ lạc.");
      save(t);
      WorldPlayer p = world.online(playerId);
      if (p != null) {
         PlayerData d = p.data();
         d.tribe = tag(t, t.member(playerId));
         d.tribeNearby = 0;
         refreshStats(d);
         p.send(TribePackets.notices(t.notices));
         p.send(TribePackets.warehouse(t.items));
         p.send(TribePackets.skills(t.skills));
         p.send(Packets.notice("Bạn đã gia nhập bộ lạc " + t.name + "!"));
      }
      pushInfo(t);
      pushNotices(t);
      Log.debug("Bo lac #%d: '%s' gia nhap", t.id, name);
   }

   /** Gỡ thành viên (rời / bị đuổi): mất buff ngay, đóng góp cá nhân mất, ghi mốc rời (chờ vào lại). */
   private void removeMember(Tribe t, Tribe.Member m, long now, boolean cooldown) {
      t.members.remove(m);
      byMember.remove(m.id);
      if (t.topPoints == m.id) {
         t.topPoints = 0;
      }
      if (t.topDiamond == m.id) {
         t.topDiamond = 0;
      }
      if (t.topGold == m.id) {
         t.topGold = 0;
      }
      boolean titles = updateTops(t);
      WorldPlayer p = world.online(m.id);
      if (p != null) {
         PlayerData d = p.data();
         if (cooldown) {
            d.tribeLeftAt = now;
         }
         d.tribe = null;
         d.tribeNearby = 0;
         p.send(TribePackets.mine(null, id -> null));
         refreshStats(d);
      }
      if (titles) {
         retag(t);
      }
   }

   /**
    * Người giữ danh hiệu Công thần / Tài chủ / Kim chủ: người đang giữ chỉ mất khi có người VƯỢT hẳn (bằng điểm thì
    * người cũ giữ). Số 0 không có danh hiệu. @return có đổi
    */
   static boolean updateTops(Tribe t) {
      int points = top(t, t.topPoints, m -> m.points);
      int diamond = top(t, t.topDiamond, m -> m.diamond);
      int gold = top(t, t.topGold, m -> m.gold);
      boolean changed = points != t.topPoints || diamond != t.topDiamond || gold != t.topGold;
      t.topPoints = points;
      t.topDiamond = diamond;
      t.topGold = gold;
      return changed;
   }

   private static int top(Tribe t, int holder, ToLongFunction<Tribe.Member> value) {
      Tribe.Member current = holder == 0 ? null : t.member(holder);
      long best = current == null ? 0 : value.applyAsLong(current);
      int bestId = current == null || best <= 0 ? 0 : holder;
      for (Tribe.Member m : t.members) {
         long v = value.applyAsLong(m);
         if (v > best) {
            best = v;
            bestId = m.id;
         }
      }
      return bestId;
   }

   /** Huy hiệu cạnh tên bộ lạc: chức vụ > Tài chủ > Kim chủ > Công thần; thành viên thường không có huy hiệu. */
   static String badge(Tribe t, Tribe.Member m) {
      if (m.role == TribeRules.ROLE_LEADER) {
         return BADGE_LEADER;
      }
      if (m.role == TribeRules.ROLE_DEPUTY) {
         return BADGE_DEPUTY;
      }
      if (t.topDiamond == m.id) {
         return BADGE_TOP_DIAMOND;
      }
      if (t.topGold == m.id) {
         return BADGE_TOP_GOLD;
      }
      if (t.topPoints == m.id) {
         return BADGE_TOP_POINTS;
      }
      return "";
   }

   private TribeTag tag(Tribe t, Tribe.Member m) {
      return new TribeTag(t.id, t.name, badge(t, m), t.skillMask(), t.emblemLines);
   }

   /** Tính lại tag mọi thành viên online; buff / tên / huy hiệu đổi thì tính lại chỉ số + phát khối chỉ số. */
   private void retag(Tribe t) {
      for (Tribe.Member m : t.members) {
         PlayerData d = onlineData(m.id);
         if (d == null) {
            continue;
         }
         TribeTag old = d.tribe;
         TribeTag now = tag(t, m);
         d.tribe = now;
         if (!now.sameLook(old) || old.skillMask() != now.skillMask()
               || !Arrays.deepEquals(old.emblemLines(), now.emblemLines())) {
            refreshStats(d);
         }
      }
   }

   /** Tính lại chỉ số + phát khối chỉ số (tên / huy hiệu bộ lạc) trên luồng session của người đó. */
   private void refreshStats(PlayerData d) {
      WorldPlayer p = world.online(d.getId());
      if (p == null) {
         return;
      }
      PlayerSession s = p.session();
      s.inbox().execute(() -> {
         if (s.getPlayer() == d && d.inWorld) {
            ItemService.refreshLook(d);
            world.broadcastStats(d);
         }
      }, gamePool);
   }

   private void pushAll(Tribe t) {
      pushInfo(t);
      pushNotices(t);
      pushSkills(t);
      pushWarehouse(t);
   }

   private void pushSkills(Tribe t) {
      broadcast(t, TribePackets.skills(t.skills).toPacket());
   }

   private void pushWarehouse(Tribe t) {
      broadcast(t, TribePackets.warehouse(t.items).toPacket());
   }

   private void pushInfo(Tribe t) {
      broadcast(t, TribePackets.mine(t, this::onlineData).toPacket());
      broadcast(t, TribePackets.emblemStats(t).toPacket());
   }

   private void pushNotices(Tribe t) {
      broadcast(t, TribePackets.notices(t.notices).toPacket());
   }

   private void broadcast(Tribe t, Packet packet) {
      for (Tribe.Member m : t.members) {
         WorldPlayer p = world.online(m.id);
         if (p != null) {
            p.send(packet);
         }
      }
   }

   private void notice(Tribe t, String text) {
      t.notices.add("[" + LocalDateTime.now().format(NOTICE_TIME) + "] " + text);
      while (t.notices.size() > MAX_NOTICES) {
         t.notices.remove(0);
      }
   }

   private void save(Tribe t) {
      TribeRepository.Row row = TribeRepository.snapshot(t);
      t.dirty = false;
      players.runOnWriter(conn -> TribeRepository.save(conn, row));
   }

   private void saveWithLog(Tribe t, TribeRepository.LogEntry log) {
      TribeRepository.Row row = TribeRepository.snapshot(t);
      t.dirty = false;
      players.runOnWriter(conn -> {
         TribeRepository.save(conn, row);
         TribeRepository.log(conn, log);
      });
   }

   private static TribeRepository.LogEntry log(Tribe t, PlayerData d, String kind, long gold, long diamond,
         String detail) {
      return new TribeRepository.LogEntry(t.id, System.currentTimeMillis(), d.getId(), d.name, kind, gold, diamond,
            detail);
   }

   /** Bỏ mọi đơn xin vào của người này (vừa vào / lập bộ lạc). */
   private void removeApplications(int playerId) {
      for (Tribe t : tribes.values()) {
         if (t.removeApplicant(playerId)) {
            save(t);
            pushInfo(t);
         }
      }
   }

   private int applications(int playerId) {
      int n = 0;
      for (Tribe t : tribes.values()) {
         if (t.applicant(playerId) != null) {
            n++;
         }
      }
      return n;
   }

   private Pending takePending(int playerId, byte kind) {
      Pending p = pending.remove(playerId);
      return p != null && p.kind() == kind && System.currentTimeMillis() - p.at() <= CONFIRM_TTL_MS ? p : null;
   }

   /** Bộ lạc của {@code d} nếu là Tù trưởng / Phó, không thì báo lỗi + null. */
   private Tribe officerTribe(PlayerSession session, PlayerData d) {
      Tribe t = d == null ? null : byMember.get(d.getId());
      if (t == null) {
         return null;
      }
      if (t.member(d.getId()).role == TribeRules.ROLE_MEMBER) {
         session.send(Packets.notice("Chỉ Tù trưởng hoặc Phó tù trưởng được làm việc này."));
         return null;
      }
      return t;
   }

   private Tribe leaderTribe(PlayerSession session, PlayerData d, String error) {
      Tribe t = d == null ? null : byMember.get(d.getId());
      if (t == null) {
         return null;
      }
      if (t.member(d.getId()).role != TribeRules.ROLE_LEADER) {
         session.send(Packets.notice(error));
         return null;
      }
      return t;
   }

   /** Lý do {@code d} chưa được vào bộ lạc (nhiệm vụ, chờ sau khi rời), null = được. */
   private String joinBlocked(PlayerData d, long now) {
      if (MainQuests.currentQuest(d.mainQuest) <= cfg.joinAfterQuest()) {
         return "Cần hoàn thành nhiệm vụ chính tuyến " + cfg.joinAfterQuest() + " mới vào được bộ lạc.";
      }
      long wait = d.tribeLeftAt + cfg.rejoinHours() * HOUR_MS - now;
      if (d.tribeLeftAt > 0 && wait > 0) {
         long minutes = (wait + 59_999) / 60_000;
         return "Vừa rời bộ lạc, còn " + (minutes / 60 > 0 ? minutes / 60 + " giờ " : "") + minutes % 60
               + " phút nữa mới vào được bộ lạc khác.";
      }
      return null;
   }

   private List<Integer> freeSkills(Tribe t) {
      List<Integer> free = new ArrayList<>();
      for (GamePack.TribeSkill s : GameDataCache.game().tribeSkills()) {
         if (!t.hasSkill(s.id())) {
            free.add(s.id());
         }
      }
      return free;
   }

   private static int nextSlotLevel(int level) {
      return (level / 5 + 1) * 5;
   }

   private PlayerData onlineData(int playerId) {
      WorldPlayer p = world.online(playerId);
      return p == null ? null : p.data();
   }

   private void send(int playerId, Message msg) {
      WorldPlayer p = world.online(playerId);
      if (p != null) {
         p.send(msg);
      }
   }

   // ================================================================== tiện ích

   /** Người gửi gói (đã vào map), chống bấm liên tục. */
   private static PlayerData actor(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastTribeActionAt < ACTION_COOLDOWN_MS) {
         session.send(Packets.notice("Thao tác quá nhanh."));
         return null;
      }
      d.lastTribeActionAt = now;
      return d;
   }

   private static String busy(PlayerData d) {
      return d.trade != null ? "Đang giao dịch, không thể thao tác."
            : d.fishing != null ? "Đang câu cá, không thể thao tác." : null;
   }

   /** Khoá so tên (không phân biệt hoa thường, dạng NFC). */
   static String nameKey(String name) {
      return Normalizer.normalize(name.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
   }

   static String cleanName(String raw) {
      return cleanText(raw).replaceAll(" {2,}", " ");
   }

   private static String cleanText(String raw) {
      if (raw == null) {
         return "";
      }
      StringBuilder out = new StringBuilder(raw.length());
      for (int i = 0; i < raw.length(); i++) {
         char c = raw.charAt(i);
         out.append(Character.isISOControl(c) ? ' ' : c);
      }
      return Normalizer.normalize(out.toString().trim(), Normalizer.Form.NFC);
   }

   private static int templateId(PlayerData d) {
      Appearance look = d.appearance;
      return look != null ? look.templateId() : Gender.charTemplateId(d.gender);
   }

   /** Dòng chỉ số của món biểu tượng (giá trị đầu mỗi dòng). */
   private static int[][] lines(ItemData item) {
      List<int[]> out = new ArrayList<>();
      for (ItemData.StatValue s : item.stats) {
         if (s.statTypeId() != STAT_EXPIRE_DAYS && s.values() != null && s.values().length > 0) {
            out.add(new int[] {s.statTypeId(), s.values()[0]});
         }
      }
      return out.toArray(new int[0][]);
   }

   /** Hạn dùng (ngày) của món biểu tượng: dòng 57 "HSD {0} ngày", không có thì {@code tribe.emblemDays}. */
   private int emblemDays(ItemData item) {
      for (ItemData.StatValue s : item.stats) {
         if (s.statTypeId() == STAT_EXPIRE_DAYS && s.values() != null && s.values().length > 0 && s.values()[0] > 0) {
            return s.values()[0];
         }
      }
      return cfg.emblemDays();
   }

   private static int indexOf(List<ItemData> items, ItemData item) {
      for (int i = 0; i < items.size(); i++) {
         if (items.get(i) == item) {
            return i;
         }
      }
      return -1;
   }

   /** "6 ngày 3 giờ" / "5 giờ 12 phút". */
   static String remaining(long ms) {
      long minutes = Math.max(1, ms / 60_000);
      long days = minutes / 1440;
      long hours = minutes % 1440 / 60;
      return days > 0 ? days + " ngày " + hours + " giờ" : hours + " giờ " + minutes % 60 + " phút";
   }

   /** Biểu tượng ứng với món (cùng icon: item "icon\566" = tribe_emblem "icon/566"). */
   static GamePack.TribeEmblem emblemOf(ItemData item) {
      String icon = item.template() == null ? "" : item.template().iconPath().replace('\\', '/');
      for (GamePack.TribeEmblem e : GameDataCache.game().tribeEmblems()) {
         if (e.framePaths().length > 0 && e.framePaths()[0].equals(icon)) {
            return e;
         }
      }
      return null;
   }

   private static String emblemName(int emblemId) {
      for (GamePack.TribeEmblem e : GameDataCache.game().tribeEmblems()) {
         if (e.id() == emblemId) {
            return e.name();
         }
      }
      return "";
   }

   private static String skillName(int skillId) {
      for (GamePack.TribeSkill s : GameDataCache.game().tribeSkills()) {
         if (s.id() == skillId) {
            return s.name();
         }
      }
      return "kỹ năng " + skillId;
   }

   private static String itemName(int templateId) {
      var t = GameDataCache.itemTemplate(templateId);
      return t != null ? t.name() : "vật phẩm";
   }

   static String format(long value) {
      return String.format(Locale.US, "%,d", value).replace(',', '.');
   }

   // ================================================================== lệnh test

   private static volatile TribeService instance;

   private static final String USAGE = "Lệnh: /bl [info] | xp N (cộng N điểm cống hiến) | quy (+100 triệu vàng,"
         + " +1000 lam thạch vào ngân sách) | cap N (đặt cấp bộ lạc) | het (biểu tượng hết hạn sau 10 giây)";

   /** Lệnh chat {@code /bl ...} ({@code chat/ChatService}, luồng session). */
   public static void testCommand(PlayerSession session, PlayerData d, String[] args) {
      TribeService s = instance;
      if (s == null) {
         session.send(Packets.notice("Chưa bật bộ lạc."));
         return;
      }
      s.test(session, d, args);
   }

   private void test(PlayerSession session, PlayerData d, String[] args) {
      String sub = args.length > 1 ? args[1] : "info";
      long n = args.length > 2 ? parse(args[2]) : 0;
      if ("xp".equals(sub)) {
         addPoints(d.getId(), Math.max(1, n));
      }
      synchronized (this) {
         Tribe t = byMember.get(d.getId());
         if (t == null) {
            session.send(Packets.notice("Bạn chưa ở trong bộ lạc. " + USAGE));
            return;
         }
         switch (sub) {
            case "xp" -> {
            }
            case "quy" -> {
               t.gold += 100_000_000L;
               t.diamond += 1_000;
               save(t);
               pushInfo(t);
            }
            case "cap" -> {
               t.level = (int) Math.max(0, Math.min(TribeRules.MAX_LEVEL, n));
               t.exp = 0;
               save(t);
               pushInfo(t);
            }
            case "het" -> {
               if (t.emblemExpireAt > 0) {
                  t.emblemExpireAt = System.currentTimeMillis() + 10_000;
                  pushInfo(t);
               }
            }
            case "info" -> {
            }
            default -> {
               session.send(Packets.notice(USAGE));
               return;
            }
         }
         TribeTag tag = d.tribe;
         session.send(Packets.notice("Bộ lạc " + t.name + " cấp " + t.level + " (" + t.exp + "/"
               + TribeRules.need(t.level + 1) + "), quỹ " + format(t.gold) + " vàng / " + format(t.diamond)
               + " lam thạch, kỹ năng " + t.skills + " (ô " + TribeRules.skillSlots(t.level) + "), biểu tượng "
               + emblemName(t.emblemId) + ", đứng cạnh " + d.tribeNearby + ", huy hiệu "
               + (tag != null ? tag.badge() : "-") + "."));
      }
   }

   private static long parse(String s) {
      try {
         return Long.parseLong(s);
      } catch (NumberFormatException e) {
         return 0;
      }
   }
}
