package com.tlnt.server.session;

import com.tlnt.server.account.AccountService;
import com.tlnt.server.net.PacketDispatcher;
import com.tlnt.server.net.transport.NetConnection;
import com.tlnt.server.net.transport.NetListener;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Nhận sự kiện từ tầng mạng (Netty hoặc KryoNet) và gắn mỗi kết nối với 1 {@link PlayerSession}.
 *
 * <ul>
 *   <li>Session gắn thẳng vào kết nối ({@link NetConnection#attach}) -> mỗi gói tra session O(1), không qua map.</li>
 *   <li>Map {@link #sessions} chỉ để đếm / duyệt khi tắt server.</li>
 *   <li>Ngắt kết nối: đăng xuất (gỡ khỏi thế giới + lưu nhân vật) chạy trên pool game, xếp SAU các gói đang chờ của
 *       session đó -> không làm chậm luồng mạng và không chạy song song với handler của chính người đó.</li>
 * </ul>
 */
public final class SessionManager implements NetListener {

   private final Map<Integer, PlayerSession> sessions = new ConcurrentHashMap<>();
   private final PacketDispatcher dispatcher;
   private final AccountService accountService;
   private final Executor gamePool;

   public SessionManager(PacketDispatcher dispatcher, AccountService accountService, Executor gamePool) {
      this.dispatcher = dispatcher;
      this.accountService = accountService;
      this.gamePool = gamePool;
   }

   @Override
   public void connected(NetConnection connection) {
      // Quá số kết nối / IP: đóng luôn, KHÔNG gắn session (disconnected thấy session null -> không trừ bộ đếm).
      if (!accountService.guard().onConnect(connection.remoteIp())) {
         connection.close();
         return;
      }
      PlayerSession session = new PlayerSession(connection);
      connection.attach(session);
      sessions.put(connection.id(), session);
      Log.debug("Ket noi moi #%d tu %s, tong %d phien", connection.id(), connection.remoteIp(), sessions.size());
   }

   @Override
   public void received(NetConnection connection, byte opcode, byte[] data) {
      PlayerSession session = (PlayerSession) connection.attachment();
      if (session != null) {
         dispatcher.dispatch(session, opcode, data);
      }
   }

   @Override
   public void disconnected(NetConnection connection) {
      PlayerSession session = (PlayerSession) connection.attachment();
      if (session == null) {
         return;
      }
      accountService.guard().onDisconnect(connection.remoteIp());
      if (session.isOfflineTraining() && session.isAuthenticated()) {
         // Treo offline: nhân vật ở lại thế giới, bot server đánh tiếp. Session giữ trong map (tắt server vẫn lưu được);
         // hết treo -> OfflineTrainingService đăng xuất + forget.
         session.detachConnection();
         Log.debug("Ket noi #%d ngat, nhan vat tiep tuc treo offline", connection.id());
         return;
      }
      sessions.remove(connection.id(), session);
      session.markClosing();
      session.inbox().execute(() -> {
         try {
            accountService.logout(session); // gỡ khỏi thế giới + lưu nhân vật + gỡ khỏi danh sách online
         } catch (Exception e) {
            Log.error("Loi dang xuat session #" + session.getId(), e);
         }
      }, gamePool);
      Log.debug("Ket noi #%d ngat, con %d phien", connection.id(), sessions.size());
   }

   /** Session treo offline đã đăng xuất xong: bỏ khỏi danh sách (kết nối thật đã ngắt từ trước). */
   public void forget(PlayerSession session) {
      sessions.remove(session.getId(), session);
   }

   public int activeCount() {
      return sessions.size();
   }

   /** Bản sao danh sách session đang mở (vd để đăng xuất hết khi tắt server). */
   public List<PlayerSession> snapshot() {
      return new ArrayList<>(sessions.values());
   }
}
