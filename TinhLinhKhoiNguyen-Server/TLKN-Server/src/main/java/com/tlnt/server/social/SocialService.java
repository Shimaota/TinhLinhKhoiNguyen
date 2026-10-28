package com.tlnt.server.social;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.item.Appearance;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;
import com.tlnt.server.world.WorldPlayer;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Menu người chơi ({@code ItemActionMenuPopup}, tên client sai: đây là menu khi bấm ảnh đại diện 1 người / 1 dòng bạn
 * bè): xem thông tin, kết bạn (lời mời chỉ trong RAM, đồng ý thì 2 bên thành bạn; danh sách bạn
 * lưu SQL), xoá bạn, thách đấu; + đổi trạng thái PK. Giao dịch ở {@code trade/TradeService}.
 * Lane GAME, luồng session của người gửi.
 */
public final class SocialService {

   /** Tối đa số bạn bè. */
   static final int MAX_FRIENDS = 50;
   /** Tối đa số lời mời kết bạn đang chờ (quá thì bỏ lời cũ nhất). */
   static final int MAX_FRIEND_INVITES = 20;
   private static final long INSPECT_COOLDOWN_MS = 1_000;
   private static final long INVITE_COOLDOWN_MS = 3_000;
   /** Khoảng cách tối thiểu giữa 2 lần gửi lời thách đấu. */
   static final long DUEL_INVITE_COOLDOWN_MS = 15_000;
   /** Khoảng cách tối đa (pixel) để thách đấu. */
   private static final float DUEL_RANGE_PX = 600f;
   /** Thời gian 1 trận thách đấu (sau đếm ngược). */
   private static final long DUEL_DURATION_MS = 3 * 60_000;

   private final World world;
   private final Executor gamePool;
   private final Invites duelInvites = new Invites(20_000);
   /**
    * Người mời id -> những người đã đồng ý lời mời kết bạn của họ, chờ áp vào danh sách của người mời trên luồng của
    * họ ({@link #applyAccepted}). Chỉ RAM.
    */
   private final Map<Integer, Queue<FriendEntry>> accepted = new ConcurrentHashMap<>();

   public SocialService(World world, Executor gamePool) {
      this.world = world;
      this.gamePool = gamePool;
   }

   // ================================================================== xem thông tin (op 2 sub 0)

   /**
    * "Xem thông tin" (từ ảnh đại diện hoặc danh sách bạn bè — người đó có thể ở map khác). Túi đồ của người bị xem chỉ
    * luồng session của họ được đọc -> dựng gói trên hàng đợi của họ rồi gửi cho người xem.
    */
   public void inspect(PlayerSession session, int targetId) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastInspectAt < INSPECT_COOLDOWN_MS) {
         return;
      }
      d.lastInspectAt = now;
      WorldPlayer target = world.online(targetId);
      if (target == null || !target.data().inWorld) {
         session.send(Packets.notice("Người chơi không trực tuyến."));
         return;
      }
      PlayerSession targetSession = target.session();
      targetSession.inbox().execute(() -> {
         PlayerData td = targetSession.getPlayer();
         if (td == target.data()) {
            session.send(WorldPackets.inspect(td, target.locked()));
            if (td != d) {
               targetSession.send(Packets.notice(d.name + " đang xem thông tin của bạn."));
            }
         }
      }, gamePool);
   }

   // ================================================================== bạn bè (op 34)

   /** Sub 0: mở tab "Danh sách bạn bè". Người đang online: cập nhật tên / cấp / ảnh đại diện đã lưu. */
   public void sendFriends(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null) {
         return;
      }
      applyAccepted(session, d);
      session.send(list(0, d.friends));
   }

   /** Sub 1: mở tab "Lời mời" (cũng tự gửi mỗi khi có lời mời mới -> client hiện chấm đỏ). */
   public void sendFriendInvites(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d != null) {
         session.send(list(1, d.friendInvites));
      }
   }

   /** S→C 34 sub 0 / 1: int n, n×(int id, str tên, str avatar, short cấp, bool online). */
   private Message list(int sub, List<FriendEntry> entries) {
      Message msg = new Message(34);
      msg.writeByte(sub);
      msg.writeInt(entries.size());
      for (FriendEntry entry : entries) {
         WorldPlayer online = world.online(entry.id);
         if (online != null) {
            refresh(entry, online.data());
         }
         msg.writeInt(entry.id);
         msg.writeString(entry.name != null ? entry.name : "");
         msg.writeString(entry.avatar != null ? entry.avatar : "");
         msg.writeShort(entry.level);
         msg.writeBoolean(online != null);
      }
      return msg;
   }

   /**
    * Sub 2 "Kết bạn": gửi lời mời. Lời mời nằm trong RAM của người nhận ({@link PlayerData#friendInvites}), thêm trên
    * luồng session của HỌ (xếp vào hàng đợi), rồi đẩy tab "Lời mời" cho họ.
    */
   public void addFriend(PlayerSession session, int targetId) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || targetId == d.getId()) {
         return;
      }
      FriendEntry existing = find(d.friends, targetId);
      if (existing != null) {
         session.send(Packets.notice(existing.name + " đã có trong danh sách bạn bè."));
         return;
      }
      if (d.friends.size() >= MAX_FRIENDS) {
         session.send(Packets.notice("Danh sách bạn bè đã đầy (tối đa " + MAX_FRIENDS + ")."));
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastInviteAt < INVITE_COOLDOWN_MS) {
         session.send(Packets.notice("Thao tác quá nhanh."));
         return;
      }
      WorldPlayer target = world.online(targetId);
      if (target == null) {
         session.send(Packets.notice("Người chơi không tồn tại hoặc offline."));
         return;
      }
      d.lastInviteAt = now;
      FriendEntry me = new FriendEntry(d.getId(), null, null, 0);
      refresh(me, d);
      PlayerSession targetSession = target.session();
      // Lời mời của người nhận chỉ luồng session của họ được đọc -> kiểm tra trùng ở đó, rồi báo người gửi.
      targetSession.inbox().execute(() -> receiveInvite(targetSession, target.data(), me, session), gamePool);
   }

   /**
    * Luồng session người nhận: thêm lời mời (giới hạn số lời mời). Người gửi đã có lời mời đang chờ -> báo "đã gửi
    * rồi", không tính bước nhiệm vụ gửi lời mời (gửi 1 người nhiều lần chỉ tính 1).
    */
   private void receiveInvite(PlayerSession session, PlayerData d, FriendEntry from, PlayerSession sender) {
      if (session.getPlayer() != d) {
         return;
      }
      if (find(d.friends, from.id) != null) { // mình đã có họ trong danh sách -> kết bạn ngược lại luôn
         QuestEvents.fireFor(from.id, StepKind.FRIEND_INVITE, 0);
         acceptFrom(session, d, from);
         return;
      }
      if (find(d.friendInvites, from.id) != null) {
         sender.send(Packets.notice("Bạn đã gửi lời mời kết bạn với người này rồi."));
         return;
      }
      sender.send(Packets.notice("Gửi lời mời kết bạn thành công!"));
      QuestEvents.fireFor(from.id, StepKind.FRIEND_INVITE, 0);
      if (d.friendInvites.size() >= MAX_FRIEND_INVITES) {
         d.friendInvites.remove(0);
      }
      d.friendInvites.add(from);
      sendFriendInvites(session); // không báo: lời mời đã hiện trong tab "Lời mời"
   }

   /** Sub 3 "Đồng ý" ở tab "Lời mời" (int VỊ TRÍ). Cả 2 thành bạn của nhau. */
   public void acceptFriendInvite(PlayerSession session, int index) {
      PlayerData d = session.getPlayer();
      if (d == null || index < 0 || index >= d.friendInvites.size()) {
         return;
      }
      FriendEntry from = d.friendInvites.get(index);
      if (world.online(from.id) == null) { // người mời đã thoát: không kết bạn 1 chiều, bỏ lời mời
         d.friendInvites.remove(index);
         sendFriendInvites(session);
         session.send(Packets.notice("Người chơi không tồn tại hoặc đang offline."));
         return;
      }
      if (find(d.friends, from.id) == null && d.friends.size() >= MAX_FRIENDS) {
         session.send(Packets.notice("Danh sách bạn bè đã đầy (tối đa " + MAX_FRIENDS + ")."));
         return;
      }
      d.friendInvites.remove(index);
      acceptFrom(session, d, from);
      sendFriendInvites(session);
   }

   /** Sub 4 "Từ chối" ở tab "Lời mời" (int VỊ TRÍ). */
   public void declineFriendInvite(PlayerSession session, int index) {
      PlayerData d = session.getPlayer();
      if (d == null || index < 0 || index >= d.friendInvites.size()) {
         return;
      }
      FriendEntry from = d.friendInvites.remove(index);
      WorldPlayer inviter = world.online(from.id);
      if (inviter != null) {
         inviter.send(Packets.notice(d.name + " đã từ chối lời mời kết bạn."));
      }
      sendFriendInvites(session);
   }

   /**
    * Luồng session của {@code d}: thêm {@code from} vào bạn của mình. Người mời PHẢI đang online (đã kiểm tra ở
    * {@link #acceptFriendInvite}; offline thì báo và không thêm). Phía người mời đi qua {@link #accepted} (RAM) rồi áp
    * trên luồng CỦA HỌ ngay; lỡ họ thoát đúng lúc đó thì {@link #onLogout} áp nốt trước khi lưu -> không bao giờ còn
    * mục treo trong RAM hay bạn bè 1 chiều.
    */
   private void acceptFrom(PlayerSession session, PlayerData d, FriendEntry from) {
      WorldPlayer inviter = world.online(from.id);
      if (find(d.friends, from.id) == null) {
         if (inviter != null) {
            refresh(from, inviter.data());
         }
         d.friends.add(from);
      }
      session.send(Packets.notice("Đã kết bạn với " + from.name + "."));
      sendFriends(session);

      FriendEntry me = new FriendEntry(d.getId(), null, null, 0);
      refresh(me, d);
      accepted.computeIfAbsent(from.id, id -> new ConcurrentLinkedQueue<>()).add(me);
      if (inviter != null) {
         PlayerSession inviterSession = inviter.session();
         String myName = d.name;
         inviterSession.inbox().execute(() -> {
            if (inviterSession.getPlayer() == inviter.data()) {
               inviterSession.send(Packets.notice(myName + " vừa đồng ý kết bạn!"));
               sendFriends(inviterSession); // áp lời đồng ý rồi gửi danh sách mới
            }
         }, gamePool);
      }
   }

   /** Luồng session của {@code d}: thêm những người đã đồng ý lời mời của mình (lúc mình offline hoặc vừa xong). */
   private void applyAccepted(PlayerSession session, PlayerData d) {
      Queue<FriendEntry> queue = accepted.remove(d.getId());
      if (queue == null) {
         return;
      }
      FriendEntry entry;
      while ((entry = queue.poll()) != null) {
         if (find(d.friends, entry.id) != null) {
            continue;
         }
         if (d.friends.size() >= MAX_FRIENDS) {
            session.send(Packets.notice("Danh sách bạn bè đã đầy, không thêm được " + entry.name + "."));
            continue;
         }
         d.friends.add(entry);
      }
   }

   /** Sub 5 "Xoá bạn bè": client gửi VỊ TRÍ trong danh sách đã nhận (không phải id). Chỉ xoá phía mình. */
   public void removeFriend(PlayerSession session, int index) {
      PlayerData d = session.getPlayer();
      if (d == null || index < 0 || index >= d.friends.size()) {
         return;
      }
      FriendEntry removed = d.friends.remove(index);
      session.send(Packets.notice("Đã xoá " + removed.name + " khỏi danh sách bạn bè."));
      sendFriends(session);
   }

   private static FriendEntry find(List<FriendEntry> entries, int id) {
      for (FriendEntry entry : entries) {
         if (entry.id == id) {
            return entry;
         }
      }
      return null;
   }

   /** Chép tên / cấp / ảnh đại diện hiện tại (đọc không khoá dữ liệu người khác: chỉ field đơn, lệch chút không sao). */
   private static void refresh(FriendEntry entry, PlayerData other) {
      entry.name = other.name;
      entry.level = other.level;
      Appearance look = other.appearance;
      GamePack.CharTemplate template = look == null ? null : GameDataCache.charTemplate(look.templateId());
      if (template != null) {
         entry.avatar = template.avatarIconPath();
      }
   }

   // ================================================================== trạng thái PK (op 42 sub 0)

   /** Menu biểu tượng PK: byte = trạng thái muốn đổi ({@link PkRules}). */
   public void setFaction(PlayerSession session, int faction) {
      world.setFaction(session, faction);
   }

   // ================================================================== thách đấu (op 41 sub 0 + op 32 loại 3)

   /** "Thách thức" trong menu người chơi: gửi lời mời thách đấu. */
   public void challenge(PlayerSession session, int targetId) {
      WorldPlayer me = world.online(session);
      if (me == null || targetId == me.id()) {
         return;
      }
      PlayerData d = me.data();
      long now = System.currentTimeMillis();
      if (now - d.lastInviteAt < INVITE_COOLDOWN_MS) {
         me.send(Packets.notice("Thao tác quá nhanh."));
         return;
      }
      long wait = d.lastDuelInviteAt + DUEL_INVITE_COOLDOWN_MS - now;
      if (wait > 0) {
         me.send(Packets.notice("Hãy chờ " + ((wait + 999) / 1000) + " giây nữa mới gửi được lời thách đấu mới."));
         return;
      }
      WorldPlayer target = world.online(targetId);
      String error = checkDuelPair(me, target);
      if (error != null) {
         me.send(Packets.notice(error));
         return;
      }
      d.lastInviteAt = now;
      d.lastDuelInviteAt = now;
      duelInvites.put(targetId, me.id(), now);
      target.send(Packets.invitation(Packets.CONFIRM_DUEL, "Thách đấu", d.name + " muốn thách đấu với bạn."));
      me.send(Packets.notice("Đã gửi lời thách đấu tới " + target.data().name + "."));
   }

   /** Người được thách bấm "Đồng ý". */
   public void acceptDuel(PlayerSession session) {
      WorldPlayer me = world.online(session);
      if (me == null) {
         return;
      }
      int fromId = duelInvites.take(me.id(), System.currentTimeMillis());
      WorldPlayer challenger = fromId == 0 ? null : world.online(fromId);
      if (fromId == 0) {
         me.send(Packets.notice("Lời thách đấu đã hết hạn."));
         return;
      }
      String error = checkDuelPair(me, challenger);
      if (error != null) {
         me.send(Packets.notice(error));
         return;
      }
      world.startDuel(challenger, me, DUEL_RANGE_PX, DUEL_DURATION_MS);
   }

   /** Hook đăng xuất: bỏ lời thách đấu gửi tới người này. */
   public void onLogout(PlayerData d) {
      duelInvites.remove(d.getId());
      // Lời đồng ý kết bạn tới đúng lúc thoát: áp vào danh sách TRƯỚC khi chụp dữ liệu lưu (hook chạy trước lượt lưu).
      Queue<FriendEntry> queue = accepted.remove(d.getId());
      if (queue != null) {
         for (FriendEntry entry : queue) {
            if (find(d.friends, entry.id) == null && d.friends.size() < MAX_FRIENDS) {
               d.friends.add(entry);
            }
         }
      }
   }

   private static String checkDuelPair(WorldPlayer me, WorldPlayer other) {
      if (other == null) {
         return "Người chơi không trực tuyến.";
      }
      if (me.zone() == null || me.zone() != other.zone()) {
         return "Đối phương không ở cùng khu vực.";
      }
      if (me.data().hp <= 0 || other.data().hp <= 0) {
         return "Không thể thách đấu khi đã tử vong.";
      }
      if (Math.abs(me.x() - other.x()) > DUEL_RANGE_PX || Math.abs(me.y() - other.y()) > DUEL_RANGE_PX) {
         return "Khoảng cách quá xa, không thể thách đấu.";
      }
      return null;
   }
}
