package com.tlnt.server.net;

import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.io.UncheckedIOException;
import java.util.concurrent.Executor;

/**
 * Bảng opcode -> handler (mảng 256 phần tử, tra O(1)), thay cho switch khổng lồ của {@code MessageHandler} client.
 * Đăng ký tất cả ở {@link com.tlnt.server.handler.Routes}.
 *
 * <h2>Chạy ở đâu ({@link Lane})</h2>
 * <ul>
 *   <li>{@code IO}: chạy NGAY trên luồng mạng — chỉ cho việc cực nhẹ, không khoá, không SQL (ping, di chuyển).</li>
 *   <li>{@code GAME}: xếp vào hàng tuần tự của session, chạy trên pool game (số nhân CPU).</li>
 *   <li>{@code BLOCKING}: như GAME nhưng chạy trên pool riêng cho SQL / băm mật khẩu / vẽ captcha.</li>
 * </ul>
 * GAME và BLOCKING của cùng 1 session vẫn giữ đúng thứ tự gửi (xem {@link SerialExecutor}).
 *
 * <h2>Ai được gửi ({@link Access})</h2>
 * {@code PUBLIC}: cả khi chưa đăng nhập (handshake, login, captcha, ping). {@code AUTHENTICATED}: phải đăng nhập.
 *
 * <h2>Bảo vệ tài khoản (MK cấp 2 — nút "Khóa")</h2>
 * Tài khoản đang bật bảo vệ thì các gói đăng ký bằng {@link #protect} (dùng / bán / vứt / giao dịch / kí gửi / gửi thư /
 * rèn / mua...) bị chặn NGAY trên luồng mạng, trước khi vào hàng đợi: 1 lần đọc cờ volatile + 1 phép dịch bit, không
 * handler nào phải tự kiểm tra. Bot treo offline gọi thẳng service nên không bị chặn.
 */
public final class PacketDispatcher {

   public enum Lane { IO, GAME, BLOCKING }

   public enum Access { PUBLIC, AUTHENTICATED }

   /** Quá số gói đang chờ này thì coi là spam và ngắt kết nối. */
   private static final int MAX_PENDING_PACKETS = 256;
   /**
    * Client thật không bao giờ gửi gói sai định dạng (thiếu byte, chuỗi UTF hỏng) -> quá số này thì coi là client
    * giả / dò lỗi và ngắt kết nối. Gói sai chỉ ghi debug 1 dòng (không stack trace) để không ai spam ngập log được.
    */
   private static final int MAX_MALFORMED_PACKETS = 20;

   private record Route(PacketHandler handler, Lane lane, Access access) {
   }

   /**
    * Luật chặn khi tài khoản đang được bảo vệ cho 1 opcode: byte thứ {@code index} của body (0 = sub, 1 = thao tác sau
    * byte chế độ — lò rèn op 44) nằm trong {@code mask} (256 bit) thì chặn. {@code index} < 0 = chặn cả opcode.
    */
   private record Protection(int index, long[] mask) {
      boolean blocks(byte[] data) {
         if (index < 0) {
            return true;
         }
         if (data == null || data.length <= index) {
            return false;
         }
         int v = data[index] & 0xFF;
         return (mask[v >>> 6] & (1L << (v & 63))) != 0;
      }
   }

   /** Câu báo khi gói bị chặn vì tài khoản đang được bảo vệ. */
   public static final String MSG_PROTECTED =
         "Tài khoản đang được bảo vệ. Vào Cài đặt > MK cấp 2 > Mở khóa để dùng chức năng này.";
   /** Báo tối đa 1 lần / bấy nhiêu ms (client bấm / tự động liên tục). */
   private static final long PROTECTED_NOTICE_INTERVAL_MS = 2_000;

   private final Route[] routes = new Route[256];
   private final Protection[] protections = new Protection[256];
   private final Executor gamePool;
   private final Executor blockingPool;

   public PacketDispatcher(Executor gamePool, Executor blockingPool) {
      this.gamePool = gamePool;
      this.blockingPool = blockingPool;
   }

   public void register(int opcode, Lane lane, Access access, PacketHandler handler) {
      int index = opcode & 0xFF;
      if (routes[index] != null) {
         throw new IllegalStateException("Opcode " + opcode + " da dang ky 2 lan");
      }
      routes[index] = new Route(handler, lane, access);
   }

   /**
    * Chặn {@code opcode} khi tài khoản đang được bảo vệ nếu byte thứ {@code index} của body là 1 trong {@code values}
    * (không truyền giá trị = chặn cả opcode). Gọi lúc khởi động ({@link com.tlnt.server.handler.Routes}).
    */
   public void protect(int opcode, int index, int... values) {
      int op = opcode & 0xFF;
      if (values.length == 0) {
         protections[op] = new Protection(-1, null);
         return;
      }
      Protection current = protections[op];
      if (current != null && current.index() != index) {
         throw new IllegalStateException("Opcode " + opcode + " da co luat bao ve theo byte " + current.index());
      }
      long[] mask = current == null ? new long[4] : current.mask();
      for (int v : values) {
         v &= 0xFF;
         mask[v >>> 6] |= 1L << (v & 63);
      }
      protections[op] = new Protection(index, mask);
   }

   /** Gói (opcode, body) có bị chặn khi tài khoản đang được bảo vệ không. */
   public boolean isProtected(int opcode, byte[] data) {
      Protection p = protections[opcode & 0xFF];
      return p != null && p.blocks(data);
   }

   /** Gọi từ luồng mạng cho mỗi gói nhận được. */
   public void dispatch(PlayerSession session, byte opcode, byte[] data) {
      if (session.getState() == PlayerSession.State.CLOSING) {
         return;
      }
      Route route = routes[opcode & 0xFF];
      if (route == null) {
         Log.debug("Chua co handler cho opcode %d, bo qua", opcode);
         return;
      }
      if (route.access() == Access.AUTHENTICATED && !session.isAuthenticated()) {
         Log.debug("Session #%d gui opcode %d khi chua dang nhap, bo qua", session.getId(), opcode);
         return;
      }
      if (session.isProtected() && isProtected(opcode, data)) {
         if (session.allowProtectedNotice(System.currentTimeMillis(), PROTECTED_NOTICE_INTERVAL_MS)) {
            session.send(Packets.notice(MSG_PROTECTED));
         }
         Log.debug("Session #%d: tai khoan dang bao ve, chan opcode %d", session.getId(), opcode);
         return;
      }
      Message message = new Message(opcode, data);
      switch (route.lane()) {
         case IO -> runSafely(session, message, route.handler());
         case GAME -> enqueue(session, message, route.handler(), gamePool);
         case BLOCKING -> enqueue(session, message, route.handler(), blockingPool);
      }
   }

   private void enqueue(PlayerSession session, Message message, PacketHandler handler, Executor pool) {
      if (session.inbox().pending() >= MAX_PENDING_PACKETS) {
         Log.warn("Session #%d (%s) spam goi tin, ngat ket noi", session.getId(), session.getRemoteIp());
         session.close();
         return;
      }
      session.inbox().execute(() -> runSafely(session, message, handler), pool);
   }

   private static void runSafely(PlayerSession session, Message message, PacketHandler handler) {
      if (session.getState() == PlayerSession.State.CLOSING) {
         return;
      }
      try {
         handler.handle(session, message);
      } catch (UncheckedIOException malformed) {
         int count = session.incrementMalformed();
         Log.debug("Session #%d gui goi opcode %d sai dinh dang (%s), lan %d", session.getId(), message.getOpcode(),
               malformed.getCause() != null ? malformed.getCause().getMessage() : malformed.getMessage(), count);
         if (count == MAX_MALFORMED_PACKETS) {
            Log.warn("Session #%d (%s) gui qua nhieu goi sai dinh dang, ngat ket noi", session.getId(),
                  session.getRemoteIp());
            session.close();
         }
      } catch (Exception | LinkageError e) {
         // LinkageError (vd NoSuchMethodError): chạy lẫn file .class cũ/mới -> build lại sạch (mvn clean package).
         // Bắt để luồng pool không chết và lỗi hiện rõ trong log.
         Log.error("Loi xu ly opcode " + message.getOpcode() + " cua session #" + session.getId(), e);
      }
   }
}
