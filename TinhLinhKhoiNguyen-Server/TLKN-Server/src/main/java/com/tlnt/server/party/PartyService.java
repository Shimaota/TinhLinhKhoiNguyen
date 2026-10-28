package com.tlnt.server.party;

import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.item.Appearance;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.WeaponClass;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packet;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.Gender;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.social.Invites;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Tổ đội (op 31, cửa sổ "Tổ đội"): tạo, mời, xin vào / duyệt / tự duyệt, rời, đuổi, nhường trưởng nhóm, giải tán;
 * tối đa {@value #MAX_MEMBERS} người. Chỉ trong RAM (thoát game = rời tổ đội).
 *
 * <p>Luồng: gói tới trên luồng session của người gửi; mọi trạng thái tổ đội sửa dưới 1 khoá chung ({@code this}) —
 * thao tác tổ đội hiếm (bấm nút) nên 1 khoá đủ nhẹ và không bao giờ lệch giữa các thành viên. Trong khoá chỉ đọc field
 * đơn của nhân vật (tên, cấp, class) và gửi gói (không chặn). Chỉ số nhân vật (Ngũ Linh Liên Vũ, id tổ đội trong khối
 * chỉ số) tính lại trên luồng session của TỪNG thành viên (xếp vào hàng đợi của họ).
 *
 * <h2>Ngũ Linh Liên Vũ</h2>
 * Bảng bên trái: mỗi dòng sáng = tổ đội ĐANG CÓ người dùng loại vũ khí đó, mờ = còn THIẾU (chỉ để xem).
 * Cộng chỉ số CHỈ KHI đủ cả 5 loại (Giáo, Rìu, Roi da, Cung tên, Ná thun — mỗi loại ít nhất 1 người): mọi thành viên
 * nhận cả 5 dòng: +5% Tấn công lên quái, +5% Hút MP quái, +5% Hút HP quái, +5% Thưởng kinh nghiệm, +5% Thưởng vàng —
 * cộng trong {@code PlayerStats.of} ({@link #applyBonus}).
 */
public final class PartyService {

   public static final int MAX_MEMBERS = 5;
   /** % cộng mỗi dòng Ngũ Linh Liên Vũ. */
   public static final int FIVE_ELEMENTS_PERCENT = 5;
   static final int MAX_REQUESTS = 20;
   private static final long ACTION_COOLDOWN_MS = 500;
   private static final long INVITE_TTL_MS = 20_000;
   /** Hộp hỏi "xin vào tổ đội" ở trưởng nhóm: đứng chờ tới khi bấm, yêu cầu vẫn nằm ở tab "Chờ duyệt". */
   private static final long REQUEST_POPUP_TTL_MS = 120_000;
   private static final int ROLE_LEADER = 0;
   private static final int ROLE_MEMBER = 1;
   /** Vũ khí cần đủ cho Ngũ Linh Liên Vũ. */
   private static final int[] FIVE_CLASSES = {WeaponClass.SPEAR, WeaponClass.AXE, WeaponClass.WHIP, WeaponClass.BOW,
         WeaponClass.SLINGSHOT};
   /** Tên vũ khí + hiệu ứng từng dòng bảng bên trái (cùng thứ tự {@link #FIVE_CLASSES}). */
   private static final String[] FIVE_WEAPONS = {"Giáo", "Rìu", "Roi da", "Cung tên", "Ná thun"};
   private static final String[] FIVE_EFFECTS = {"+5% Tấn công lên quái", "+5% Hút MP quái", "+5% Hút HP quái",
         "+5% Thưởng kinh nghiệm", "+5% Thưởng vàng"};
   /** Dòng đang bật: tên vũ khí trắng, hiệu ứng vàng. Dòng chưa bật: cả dòng trắng mờ (markup màu TextraLabel). */
   private static final String ACTIVE_WEAPON_COLOR = "[#FFFFFF]";
   private static final String ACTIVE_EFFECT_COLOR = "[#FFF600]";
   private static final String INACTIVE_COLOR = "[#FFFFFF73]";

   private final World world;
   private final Executor gamePool;
   /** Dưới khoá {@code this}. */
   private final Map<Integer, Party> parties = new HashMap<>();
   private int nextId = 1;
   private final Invites invites = new Invites(INVITE_TTL_MS);
   /**
    * Trưởng nhóm id -> người xin vào MỚI NHẤT (hộp hỏi op 32 loại {@link Packets#CONFIRM_PARTY_REQUEST}). Client chỉ
    * có 1 hộp lời mời, yêu cầu mới đè nội dung hộp cũ -> server cũng chỉ nhớ người mới nhất; "Đồng ý" không gửi kèm
    * id nên lấy từ đây.
    */
   private final Invites requestPopups = new Invites(REQUEST_POPUP_TTL_MS);

   public PartyService(World world, Executor gamePool) {
      instance = this;
      this.world = world;
      this.gamePool = gamePool;
   }

   // ================================================================== tạo / giải tán / rời

   /** Tế đàn Thiên tích đi solo: không tạo / vào tổ đội khi đang ở trong đó. */
   static final String NO_PARTY_IN_ALTAR = "Không thể tổ đội trong Tế đàn Thiên tích.";

   /**
    * Map đi 1 mình (Tế đàn): câu báo khi tạo / vào tổ đội; null = được tổ đội. Lôi vực Sấm sét vẫn tổ đội bình thường
    * (cờ Bộ lạc: đồng đội khác bộ lạc vẫn đánh nhau — {@code PkRules.canAttack}).
    */
   static String soloMap(int mapId) {
      return AltarRules.isAltarMap(mapId) ? NO_PARTY_IN_ALTAR : null;
   }

   /**
    * Khoá tổ đội của Giải đấu Bộ lạc ({@code tournament/TournamentService} đặt lúc khởi động): câu báo khi người này không
    * được thay đổi tổ đội lúc này, null = được. {@code leaving} = rời / bị đuổi / giải tán (Giải Anh hùng chỉ cấm vào tổ
    * đội; Hợp chiến khoá cả vào lẫn ra từ 20:15).
    */
   public interface Lock {
      String check(int playerId, boolean leaving);
   }

   public static volatile Lock tournamentLock = (playerId, leaving) -> null;

   /**
    * Bot người chơi không có client nên không thấy hộp mời / hộp "xin vào" ({@code bot/BotService} đăng ký): được gọi
    * NGOÀI khoá, trên luồng của người bấm; bot tự quyết rồi trả lời qua {@link #acceptInvite} / {@link #approve} trên hàng
    * đợi của nó như người thật bấm nút.
    */
   public interface BotHooks {
      /** Người {@code fromId} mời bot {@code botId} vào tổ đội. */
      void invited(int botId, int fromId);

      /** Người {@code requesterId} xin vào tổ đội có trưởng nhóm là bot {@code leaderBotId}. */
      void joinRequested(int leaderBotId, int requesterId);
   }

   public static volatile BotHooks botHooks;

   private static String locked(int playerId, boolean leaving) {
      return tournamentLock.check(playerId, leaving);
   }

   /** Câu báo nếu có thành viên nào của {@code party} đang bị khoá. */
   private static String lockedParty(Party party, boolean leaving) {
      for (Party.Member m : party.members) {
         String error = locked(m.playerId(), leaving);
         if (error != null) {
            return error;
         }
      }
      return null;
   }

   /** Giải Anh hùng: tự rời tổ đội khi tham gia (không qua chống bấm nhanh). */
   public synchronized void forceLeave(PlayerData d, String text) {
      Party party = d.party;
      if (party != null) {
         removeMember(party, d, text, " đã rời tổ đội.");
      }
   }

   /** Sub 0 "Tạo tổ đội": người tạo là trưởng nhóm. */
   public synchronized void create(PlayerSession session) {
      WorldPlayer me = actor(session);
      if (me == null) {
         return;
      }
      PlayerData d = me.data();
      if (d.party != null) {
         me.send(Packets.notice("Bạn đã ở trong tổ đội."));
         return;
      }
      if (soloMap(d.mapId) != null) {
         me.send(Packets.notice(soloMap(d.mapId)));
         return;
      }
      if (locked(d.getId(), false) != null) {
         me.send(Packets.notice(locked(d.getId(), false)));
         return;
      }
      long now = System.currentTimeMillis();
      Party party = new Party(nextId++, d.name, now);
      parties.put(party.id(), party);
      party.members.add(new Party.Member(d.getId(), now));
      d.party = party;
      me.send(Packets.notice("Đã tạo tổ đội."));
      QuestEvents.fireFor(d.getId(), StepKind.JOIN_PARTY, 0);
      changed(party, List.of(d), List.of(d));
   }

   /** Sub 1 "Giải tán" (trưởng nhóm). */
   public synchronized void disband(PlayerSession session) {
      WorldPlayer me = actor(session);
      Party party = me == null ? null : me.data().party;
      if (party == null) {
         return;
      }
      if (!party.isLeader(me.id())) {
         me.send(Packets.notice("Chỉ trưởng nhóm được giải tán tổ đội."));
         return;
      }
      if (lockedParty(party, true) != null) {
         me.send(Packets.notice(lockedParty(party, true)));
         return;
      }
      List<PlayerData> gone = new ArrayList<>();
      for (Party.Member m : party.members) {
         PlayerData md = data(m.playerId());
         if (md != null) {
            gone.add(md);
         }
      }
      party.members.clear();
      parties.remove(party.id());
      for (PlayerData md : gone) {
         leftParty(md, party, "Tổ đội đã giải tán.");
      }
   }

   /** Sub 3 "Rời nhóm". Trưởng nhóm rời -> người vào sớm nhất lên làm trưởng nhóm. */
   public synchronized void leave(PlayerSession session) {
      WorldPlayer me = actor(session);
      Party party = me == null ? null : me.data().party;
      if (party != null && locked(me.id(), true) != null) {
         me.send(Packets.notice(locked(me.id(), true)));
         return;
      }
      if (party != null) {
         removeMember(party, me.data(), "Bạn đã rời tổ đội.", " đã rời tổ đội.");
      }
   }

   /** Hook đăng xuất: rời tổ đội + bỏ lời mời / hộp "xin vào" gửi tới người này. */
   public synchronized void onLogout(PlayerData d) {
      invites.remove(d.getId());
      requestPopups.remove(d.getId());
      Party party = d.party;
      if (party != null) {
         removeMember(party, d, null, " đã rời game.");
      }
   }

   // ================================================================== mời

   /** Sub 2 "Mời vào tổ đội" (tab Lân cận / menu người chơi): hộp mời op 32 loại {@link Packets#CONFIRM_PARTY}. */
   public synchronized void invite(PlayerSession session, int targetId) {
      WorldPlayer me = actor(session);
      if (me == null || targetId == me.id()) {
         return;
      }
      Party party = me.data().party;
      WorldPlayer target = world.online(targetId);
      String error = party == null ? "Bạn chưa có tổ đội."
            : party.full() ? "Tổ đội đã đủ " + MAX_MEMBERS + " người."
            : target == null ? "Người chơi không trực tuyến."
            : target.data().party != null ? target.data().name + " đã ở trong tổ đội khác."
            : soloMap(me.data().mapId) != null ? soloMap(me.data().mapId)
            : soloMap(target.data().mapId) != null ? soloMap(target.data().mapId)
            : lockedParty(party, false) != null ? lockedParty(party, false)
            : locked(targetId, false) != null ? target.data().name + " đang tham gia giải đấu, không thể vào tổ đội."
            : null;
      if (error != null) {
         me.send(Packets.notice(error));
         return;
      }
      invites.put(targetId, me.id(), System.currentTimeMillis());
      target.send(Packets.invitation(Packets.CONFIRM_PARTY, "Tổ đội",
            me.data().name + " mời bạn vào tổ đội."));
      me.send(Packets.notice("Đã mời " + target.data().name + " vào tổ đội."));
      BotHooks hooks = botHooks;
      if (hooks != null && target.data().bot && !me.data().bot) { // bot mời bot: BotService.formParty tự bấm Đồng ý
         int from = me.id();
         gamePool.execute(() -> hooks.invited(targetId, from));
      }
   }

   /** Op 32 loại {@link Packets#CONFIRM_PARTY}: đồng ý lời mời -> vào tổ đội của người mời (nếu còn chỗ). */
   public synchronized void acceptInvite(PlayerSession session) {
      WorldPlayer me = actor(session);
      if (me == null) {
         return;
      }
      int fromId = invites.take(me.id(), System.currentTimeMillis());
      PlayerData inviter = fromId == 0 ? null : data(fromId);
      Party party = inviter == null ? null : inviter.party;
      if (party == null) {
         me.send(Packets.notice("Lời mời tổ đội đã hết hạn."));
         return;
      }
      join(party, me.data());
   }

   // ================================================================== xin vào / duyệt

   /**
    * Sub 4: tab "Tổ đội" khi chưa có nhóm -> danh sách tổ đội có người ở cùng khu. Tìm tổ đội qua người trong khu
    * (luồng tick khu, tối đa 30 người) rồi quay về luồng session dựng gói dưới khoá — KHÔNG duyệt mọi tổ đội cả server
    * trong khoá chung (trước: chặn mọi thao tác tổ đội + đăng xuất trong lúc quét).
    */
   public void sendNearby(PlayerSession session) {
      WorldPlayer me = actor(session);
      if (me == null) {
         return;
      }
      world.partiesInZone(session, found -> session.inbox().execute(() -> sendNearby(me, found), gamePool));
   }

   private synchronized void sendNearby(WorldPlayer me, List<Party> found) {
      if (world.online(me.id()) != me || !me.data().inWorld) { // thoát game trong lúc chuyển luồng
         return;
      }
      List<Party> near = new ArrayList<>();
      for (Party party : found) {
         if (near.size() >= Byte.MAX_VALUE) {
            break;
         }
         if (parties.get(party.id()) == party) { // giải tán trong lúc chuyển luồng thì bỏ
            near.add(party);
         }
      }
      Message msg = new Message(31);
      msg.writeByte(1);
      msg.writeByte(near.size());
      for (Party party : near) {
         PlayerData leader = data(party.leaderId());
         msg.writeInt(party.id());
         msg.writeString(party.name);
         msg.writeInt(templateId(leader));
         msg.writeLong(party.createdAt);
         msg.writeBoolean(party.full() || me.data().party != null);
      }
      me.send(msg);
   }

   /** Sub 5 "Xin vào": tự duyệt thì vào luôn, không thì vào tab "Chờ duyệt" của nhóm. */
   public synchronized void requestJoin(PlayerSession session, int partyId) {
      WorldPlayer me = actor(session);
      if (me == null) {
         return;
      }
      PlayerData d = me.data();
      Party party = parties.get(partyId);
      String error = d.party != null ? "Bạn đã ở trong tổ đội."
            : soloMap(d.mapId) != null ? soloMap(d.mapId)
            : locked(d.getId(), false) != null ? locked(d.getId(), false)
            : party == null ? "Tổ đội không còn tồn tại."
            : party.full() ? "Tổ đội đã đủ " + MAX_MEMBERS + " người."
            : null;
      if (error != null) {
         me.send(Packets.notice(error));
         return;
      }
      if (party.autoApprove) {
         join(party, d);
         return;
      }
      if (party.requests.stream().anyMatch(r -> r.playerId() == d.getId())) {
         me.send(Packets.notice("Đã gửi yêu cầu, hãy chờ trưởng nhóm duyệt."));
         return;
      }
      if (party.requests.size() >= MAX_REQUESTS) {
         party.requests.remove(0);
      }
      party.requests.add(new Party.Request(d.getId(), d.name, d.level, templateId(d)));
      me.send(Packets.notice("Đã gửi yêu cầu vào tổ đội."));
      PlayerData leader = data(party.leaderId());
      if (leader != null) {
         requestPopups.put(leader.getId(), d.getId(), System.currentTimeMillis());
         send(leader, Packets.invitation(Packets.CONFIRM_PARTY_REQUEST, "Tổ đội",
               d.name + " (cấp " + d.level + ") xin vào tổ đội của bạn."));
         BotHooks hooks = botHooks;
         if (hooks != null && leader.bot && !d.bot) {
            int leaderId = leader.getId();
            int requesterId = d.getId();
            gamePool.execute(() -> hooks.joinRequested(leaderId, requesterId));
         }
      }
      changed(party, List.of(), List.of());
   }

   /**
    * Op 32 loại {@link Packets#CONFIRM_PARTY_REQUEST}: trưởng nhóm bấm "Đồng ý" ở hộp "xin vào tổ đội" = duyệt người
    * xin mới nhất (như nút "Đồng ý" ở tab "Chờ duyệt"). "Từ chối" không gửi gì — yêu cầu vẫn ở tab "Chờ duyệt".
    */
   public synchronized void acceptRequest(PlayerSession session) {
      WorldPlayer me = actor(session);
      if (me == null) {
         return;
      }
      int requesterId = requestPopups.take(me.id(), System.currentTimeMillis());
      if (requesterId == 0) {
         me.send(Packets.notice("Yêu cầu xin vào tổ đội đã hết hạn."));
         return;
      }
      approve(session, requesterId); // kiểm tra lại: còn là trưởng nhóm, yêu cầu còn chờ, người xin còn online
   }

   /** Sub 6 "Đồng ý" ở tab "Chờ duyệt" (trưởng nhóm). */
   public synchronized void approve(PlayerSession session, int playerId) {
      WorldPlayer me = actor(session);
      Party party = me == null ? null : me.data().party;
      if (party == null) {
         return;
      }
      if (!party.isLeader(me.id())) {
         me.send(Packets.notice("Chỉ trưởng nhóm được duyệt thành viên."));
         return;
      }
      boolean had = party.removeRequest(playerId);
      PlayerData requester = data(playerId);
      if (!had || requester == null) {
         me.send(Packets.notice("Người chơi không còn trực tuyến."));
         changed(party, List.of(), List.of());
         return;
      }
      join(party, requester);
   }

   /** Sub 7: bật / tắt tự duyệt (trưởng nhóm). Bật -> duyệt luôn các yêu cầu đang chờ (tới khi đủ người). */
   public synchronized void toggleAutoApprove(PlayerSession session) {
      WorldPlayer me = actor(session);
      Party party = me == null ? null : me.data().party;
      if (party == null) {
         return;
      }
      if (!party.isLeader(me.id())) {
         changed(party, List.of(), List.of()); // client đã tự đổi ô tick -> gửi lại trạng thái thật
         return;
      }
      party.autoApprove = !party.autoApprove;
      if (party.autoApprove) {
         for (Party.Request r : new ArrayList<>(party.requests)) {
            PlayerData requester = data(r.playerId());
            party.removeRequest(r.playerId());
            if (requester != null && requester.party == null && !party.full()) {
               join(party, requester);
            }
         }
      }
      changed(party, List.of(), List.of());
   }

   // ================================================================== quản lý thành viên

   /** Sub 8 "Nhường trưởng nhóm". */
   public synchronized void transferLeader(PlayerSession session, int playerId) {
      WorldPlayer me = actor(session);
      Party party = me == null ? null : me.data().party;
      if (party == null || playerId == me.id()) {
         return;
      }
      Party.Member target = party.member(playerId);
      if (!party.isLeader(me.id()) || target == null) {
         me.send(Packets.notice(target == null ? "Người này không ở trong tổ đội." : "Bạn không phải trưởng nhóm."));
         return;
      }
      party.members.remove(target);
      party.members.add(0, target);
      PlayerData leader = data(playerId);
      broadcast(party, Packets.notice((leader != null ? leader.name : "Thành viên") + " đã trở thành trưởng nhóm.")
            .toPacket());
      changed(party, List.of(), List.of());
   }

   /** Sub 9 "Đuổi khỏi nhóm" (trưởng nhóm). */
   public synchronized void kick(PlayerSession session, int playerId) {
      WorldPlayer me = actor(session);
      Party party = me == null ? null : me.data().party;
      if (party == null || playerId == me.id()) {
         return;
      }
      if (!party.isLeader(me.id())) {
         me.send(Packets.notice("Chỉ trưởng nhóm được đuổi thành viên."));
         return;
      }
      PlayerData target = data(playerId);
      if (party.member(playerId) == null || target == null) {
         return;
      }
      if (locked(playerId, true) != null) {
         me.send(Packets.notice(locked(playerId, true)));
         return;
      }
      removeMember(party, target, "Bạn đã bị đuổi khỏi tổ đội.", " đã bị đuổi khỏi tổ đội.");
   }

   // ================================================================== nội bộ (đang giữ khoá)

   private void join(Party party, PlayerData d) {
      if (d.party != null) {
         send(d, Packets.notice("Bạn đã ở trong tổ đội."));
         return;
      }
      if (soloMap(d.mapId) != null) { // nhận lời mời / được duyệt khi đang ở Tế đàn / Lôi vực
         send(d, Packets.notice(soloMap(d.mapId)));
         return;
      }
      String lock = locked(d.getId(), false) != null ? locked(d.getId(), false) : lockedParty(party, false);
      if (lock != null) { // giải đấu: người vào / tổ đội đang bị khoá
         send(d, Packets.notice(lock));
         return;
      }
      if (party.full() || !parties.containsKey(party.id())) {
         send(d, Packets.notice(party.full() ? "Tổ đội đã đủ " + MAX_MEMBERS + " người." : "Tổ đội không còn tồn tại."));
         return;
      }
      party.removeRequest(d.getId());
      party.members.add(new Party.Member(d.getId(), System.currentTimeMillis()));
      d.party = party;
      // Cả nhóm (kể cả người mới, không chỉ trưởng nhóm) — mọi đường vào: nhận lời mời, được duyệt, tự duyệt.
      broadcast(party, Packets.notice(d.name + " vừa gia nhập tổ đội!").toPacket());
      QuestEvents.fireFor(d.getId(), StepKind.JOIN_PARTY, 0); // có thể đang ở luồng người khác (trưởng nhóm duyệt)
      changed(party, List.of(d), List.of(d));
   }

   /** Gỡ {@code d} khỏi {@code party}: trưởng nhóm đi -> người kế tiếp lên; hết người -> xoá tổ đội. */
   private void removeMember(Party party, PlayerData d, String selfText, String othersSuffix) {
      Party.Member m = party.member(d.getId());
      if (m == null) {
         return;
      }
      boolean wasLeader = party.isLeader(d.getId());
      party.members.remove(m);
      leftParty(d, party, selfText);
      if (party.members.isEmpty()) {
         parties.remove(party.id());
         return;
      }
      broadcast(party, Packets.notice(d.name + othersSuffix).toPacket());
      if (wasLeader) {
         PlayerData leader = data(party.leaderId());
         if (leader != null) {
            broadcast(party, Packets.notice(leader.name + " đã trở thành trưởng nhóm.").toPacket());
         }
      }
      changed(party, List.of(), List.of());
   }

   /** {@code d} không còn ở tổ đội: đóng cửa sổ tổ đội (op 31 sub 0 id -1), tắt hiệu ứng, cập nhật khối chỉ số. */
   private void leftParty(PlayerData d, Party party, String text) {
      if (d.party == party) {
         d.party = null;
      }
      d.partyBonusMask = 0;
      Message msg = new Message(31);
      msg.writeByte(0);
      msg.writeInt(-1);
      send(d, msg);
      if (text != null) {
         send(d, Packets.notice(text));
      }
      refreshStats(d);
   }

   /**
    * Tổ đội vừa đổi: tính lại Ngũ Linh Liên Vũ, gửi thông tin tổ đội cho mọi thành viên (mã hoá 1 lần), tính lại chỉ
    * số người có đổi (vào / ra, bật / tắt hiệu ứng). {@code newcomers} nhận gói 2 lần (client lần đầu tạo tổ đội bỏ
    * qua cờ tự duyệt).
    */
   private void changed(Party party, List<PlayerData> refresh, List<PlayerData> newcomers) {
      party.bonusMask = bonusMask(party); // hiển thị: class nào đang có
      int mask = party.bonusMask == ALL_CLASSES ? ALL_CLASSES : 0; // cộng chỉ số: phải đủ cả 5 loại
      Packet packet = partyInfo(party).toPacket();
      for (Party.Member m : party.members) {
         WorldPlayer p = world.online(m.playerId());
         if (p == null) {
            continue;
         }
         PlayerData md = p.data();
         p.send(packet);
         if (newcomers.contains(md)) {
            p.send(packet);
         }
         if (md.partyBonusMask != mask || refresh.contains(md)) {
            md.partyBonusMask = mask;
            refreshStats(md);
         }
      }
   }

   /** Đủ cả 5 loại vũ khí. */
   private static final int ALL_CLASSES = (1 << 5) - 1;

   /** Bit i bật nếu có thành viên (đang online) dùng vũ khí {@code FIVE_CLASSES[i]}. */
   private int bonusMask(Party party) {
      int mask = 0;
      for (Party.Member m : party.members) {
         PlayerData md = data(m.playerId());
         if (md == null) {
            continue;
         }
         for (int i = 0; i < FIVE_CLASSES.length; i++) {
            if (md.classId == FIVE_CLASSES[i]) {
               mask |= 1 << i;
            }
         }
      }
      return mask;
   }

   /** Cộng các dòng Ngũ Linh Liên Vũ đang bật ({@code mask}) vào chỉ số (gọi trong {@code PlayerStats.of}). */
   public static void applyBonus(com.tlnt.server.player.PlayerStats s, int mask) {
      int p = FIVE_ELEMENTS_PERCENT;
      if ((mask & 1) != 0) { // Giáo
         s.attackVsMob += s.attackVsMob * p / 100;
      }
      if ((mask & 1 << 1) != 0) { // Rìu
         s.mpLeechVsMobPercent += p;
      }
      if ((mask & 1 << 2) != 0) { // Roi da
         s.hpLeechVsMobPercent += p;
      }
      if ((mask & 1 << 3) != 0) { // Cung tên
         s.expBonusPercent += p;
      }
      if ((mask & 1 << 4) != 0) { // Ná thun
         s.goldBonusPercent += p;
      }
   }

   /** 1 dòng bảng hiệu ứng: "-Rìu +5% Hút MP quái" — bật: tên trắng + hiệu ứng vàng; chưa bật: trắng mờ. */
   static String bonusLine(int index, boolean active) {
      return active
            ? ACTIVE_WEAPON_COLOR + "-" + FIVE_WEAPONS[index] + " " + ACTIVE_EFFECT_COLOR + FIVE_EFFECTS[index]
            : INACTIVE_COLOR + "-" + FIVE_WEAPONS[index] + " " + FIVE_EFFECTS[index];
   }

   /**
    * Op 31 sub 0: int id, str tên, bool tự duyệt, int char_template trưởng nhóm, long lúc tạo,
    * byte n thành viên × (int id, str tên, short cấp, byte char_template, byte class, byte vai trò 0 trưởng nhóm / 1
    * thành viên, long lúc vào), byte n yêu cầu × (int id, str tên, short cấp, int char_template),
    * byte n hiệu ứng × (bool bật, str chữ).
    */
   private Message partyInfo(Party party) {
      Message msg = new Message(31);
      msg.writeByte(0);
      msg.writeInt(party.id());
      msg.writeString(party.name);
      msg.writeBoolean(party.autoApprove);
      PlayerData leader = data(party.leaderId());
      msg.writeInt(templateId(leader));
      msg.writeLong(party.createdAt);
      List<Party.Member> shown = new ArrayList<>();
      for (Party.Member m : party.members) {
         if (data(m.playerId()) != null) {
            shown.add(m);
         }
      }
      msg.writeByte(shown.size());
      for (Party.Member m : shown) {
         PlayerData md = data(m.playerId());
         msg.writeInt(md.getId());
         msg.writeString(md.name);
         msg.writeShort(md.level);
         msg.writeByte(templateId(md));
         msg.writeByte(md.classId);
         msg.writeByte(party.isLeader(md.getId()) ? ROLE_LEADER : ROLE_MEMBER);
         msg.writeLong(m.joinedAt());
      }
      msg.writeByte(party.requests.size());
      for (Party.Request r : party.requests) {
         msg.writeInt(r.playerId());
         msg.writeString(r.name());
         msg.writeShort(r.level());
         msg.writeInt(r.templateId());
      }
      msg.writeByte(FIVE_CLASSES.length);
      for (int i = 0; i < FIVE_CLASSES.length; i++) {
         boolean active = (party.bonusMask & 1 << i) != 0;
         msg.writeBoolean(active); // client: bật = font đậm (DEFAULT_FONT), tắt = font thường
         msg.writeString(bonusLine(i, active));
      }
      return msg;
   }

   /** Tính lại chỉ số (Ngũ Linh Liên Vũ) + phát khối chỉ số (id tổ đội) trên luồng session của người đó. */
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

   private static volatile PartyService instance;

   /**
    * Op 26 sub 2 (tab "Tổ đội" cửa sổ chat, client {@code sendGuildChat} — tên sai): gửi tới mọi thành viên online
    * S→C 26/2 (int người gửi, str tên, str nội dung). Không có tổ đội -> báo. Mọi luồng.
    */
   public static void chat(com.tlnt.server.session.PlayerSession session, PlayerData d, String text) {
      PartyService s = instance;
      if (s != null) {
         s.partyChat(session, d, text);
      }
   }

   /** Id thành viên tổ đội của {@code d} (trưởng nhóm đầu), rỗng = không có tổ đội. Mọi luồng (khoá ngắn). */
   public static List<Integer> memberIdsOf(PlayerData d) {
      PartyService s = instance;
      if (s == null || d.party == null) {
         return List.of();
      }
      Snapshot snap = s.snapshot(d);
      return snap == null ? List.of() : snap.memberIds();
   }

   private synchronized void partyChat(com.tlnt.server.session.PlayerSession session, PlayerData d, String text) {
      Party party = d.party;
      if (party == null) {
         session.send(Packets.notice("Bạn chưa có tổ đội."));
         return;
      }
      Message msg = new Message(com.tlnt.server.net.Opcode.CHAT);
      msg.writeByte(2);
      msg.writeInt(d.getId());
      msg.writeString(d.name);
      msg.writeString(text);
      broadcast(party, msg.toPacket());
      com.tlnt.server.util.Log.debug("[chat to doi #%d] '%s': %s", party.id(), d.name, text);
   }

   private void broadcast(Party party, Packet packet) {
      for (Party.Member m : party.members) {
         WorldPlayer p = world.online(m.playerId());
         if (p != null) {
            p.send(packet);
         }
      }
   }

   private void send(PlayerData d, Message msg) {
      WorldPlayer p = world.online(d.getId());
      if (p != null) {
         p.send(msg);
      }
   }

   private PlayerData data(int playerId) {
      WorldPlayer p = world.online(playerId);
      return p == null ? null : p.data();
   }

   /** Người gửi gói (đã vào map), chống bấm liên tục. */
   private WorldPlayer actor(PlayerSession session) {
      WorldPlayer me = world.online(session);
      if (me == null || !me.data().inWorld) {
         return null;
      }
      long now = System.currentTimeMillis();
      if (now - me.data().lastPartyActionAt < ACTION_COOLDOWN_MS) {
         me.send(Packets.notice("Thao tác quá nhanh."));
         return null;
      }
      me.data().lastPartyActionAt = now;
      return me;
   }

   private static int templateId(PlayerData d) {
      if (d == null) {
         return Gender.charTemplateId(Gender.RINN);
      }
      Appearance look = d.appearance;
      return look != null ? look.templateId() : Gender.charTemplateId(d.gender);
   }

   /** Ảnh chụp 1 tổ đội: trưởng nhóm + id mọi thành viên (trưởng nhóm đứng đầu). */
   public record Snapshot(int leaderId, List<Integer> memberIds) {
   }

   /** Tổ đội của {@code d} lúc này (phó bản Rừng Linh Mộc chụp danh sách lúc mở); null = không có tổ đội. */
   public synchronized Snapshot snapshot(PlayerData d) {
      Party party = d.party;
      if (party == null || party.member(d.getId()) == null) {
         return null;
      }
      List<Integer> ids = new ArrayList<>(party.members.size());
      for (Party.Member m : party.members) {
         ids.add(m.playerId());
      }
      return new Snapshot(party.leaderId(), List.copyOf(ids));
   }

   /** Số tổ đội (thống kê). */
   public synchronized int count() {
      return parties.size();
   }
}
