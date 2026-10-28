package com.tlnt.server.net.wire;

/**
 * Định dạng byte trên đường truyền, GIỐNG HỆT KryoNet + Kryo 2.x mà client dùng (client không đổi được):
 *
 * <pre>
 * frame      = int32 big-endian độ dài phần sau | object
 * object     = varint(classId + 2) | các field
 *
 * DataPacket   (id 75 -> 0x4D): byte cmd | varint(len + 1) (0 = null) | len byte dữ liệu
 * RegisterTCP  (id  9 -> 0x0B): varint zigzag connectionID           (server gửi ngay khi client kết nối)
 * KeepAlive    (id 11 -> 0x0D): (không có field)                     (gửi khi 8s chưa ghi gì)
 * Ping         (id 13 -> 0x0F): varint zigzag id | boolean isReply
 * </pre>
 *
 * Mã hoá tay thay vì gọi Kryo: Kryo của KryoNet dùng CHUNG 1 khoá {@code synchronized} cho mọi kết nối (nút cổ
 * chai khi 10.000 người), còn ở đây mỗi gói chỉ là vài phép ghi mảng. Test {@code WireTest} so từng byte với
 * Kryo thật trong kryonet-client-compat để đảm bảo không lệch.
 */
public final class Wire {

   public static final int LENGTH_BYTES = 4;
   public static final int ID_REGISTER_TCP = 9 + 2;
   public static final int ID_KEEP_ALIVE = 11 + 2;
   public static final int ID_PING = 13 + 2;
   public static final int ID_DATA_PACKET = 75 + 2;

   /** Frame KeepAlive dựng sẵn. */
   public static final byte[] KEEP_ALIVE_FRAME = {0, 0, 0, 1, (byte) ID_KEEP_ALIVE};

   private Wire() {
   }

   /** Frame DataPacket(cmd, body[off..off+len)). */
   public static byte[] encodeDataPacket(byte cmd, byte[] body, int off, int len) {
      int varLen = varIntSize(len + 1);
      int objectLength = 1 + 1 + varLen + len;
      byte[] frame = new byte[LENGTH_BYTES + objectLength];
      int p = writeLength(frame, objectLength);
      frame[p++] = (byte) ID_DATA_PACKET;
      frame[p++] = cmd;
      p = writeVarInt(frame, p, len + 1);
      System.arraycopy(body, off, frame, p, len);
      return frame;
   }

   public static byte[] encodeRegisterTcp(int connectionId) {
      int zz = zigZag(connectionId);
      int objectLength = 1 + varIntSize(zz);
      byte[] frame = new byte[LENGTH_BYTES + objectLength];
      int p = writeLength(frame, objectLength);
      frame[p++] = (byte) ID_REGISTER_TCP;
      writeVarInt(frame, p, zz);
      return frame;
   }

   public static byte[] encodePing(int id, boolean isReply) {
      int zz = zigZag(id);
      int objectLength = 1 + varIntSize(zz) + 1;
      byte[] frame = new byte[LENGTH_BYTES + objectLength];
      int p = writeLength(frame, objectLength);
      frame[p++] = (byte) ID_PING;
      p = writeVarInt(frame, p, zz);
      frame[p] = (byte) (isReply ? 1 : 0);
      return frame;
   }

   // ------------------------------------------------------------------ varint (Kryo: 7 bit/byte, little-endian)

   public static int zigZag(int value) {
      return (value << 1) ^ (value >> 31);
   }

   public static int unZigZag(int value) {
      return (value >>> 1) ^ -(value & 1);
   }

   public static int varIntSize(int value) {
      if ((value >>> 7) == 0) {
         return 1;
      }
      if ((value >>> 14) == 0) {
         return 2;
      }
      if ((value >>> 21) == 0) {
         return 3;
      }
      if ((value >>> 28) == 0) {
         return 4;
      }
      return 5;
   }

   private static int writeVarInt(byte[] out, int p, int value) {
      while ((value & ~0x7F) != 0) {
         out[p++] = (byte) ((value & 0x7F) | 0x80);
         value >>>= 7;
      }
      out[p++] = (byte) value;
      return p;
   }

   private static int writeLength(byte[] frame, int length) {
      frame[0] = (byte) (length >>> 24);
      frame[1] = (byte) (length >>> 16);
      frame[2] = (byte) (length >>> 8);
      frame[3] = (byte) length;
      return LENGTH_BYTES;
   }
}
