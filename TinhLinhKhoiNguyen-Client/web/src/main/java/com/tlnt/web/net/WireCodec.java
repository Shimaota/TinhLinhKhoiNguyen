package com.tlnt.web.net;

/**
 * Encodes and decodes KryoNet binary frames, 100% compatible with Wire.java on the server.
 */
public final class WireCodec {
   public static final int LENGTH_BYTES = 4;
   public static final int ID_REGISTER_TCP = 9 + 2;   // 11 (0x0B)
   public static final int ID_KEEP_ALIVE   = 11 + 2;  // 13 (0x0D)
   public static final int ID_PING         = 13 + 2;  // 15 (0x0F)
   public static final int ID_DATA_PACKET  = 75 + 2;  // 77 (0x4D)

   public static final byte[] KEEP_ALIVE_FRAME = {0, 0, 0, 1, (byte) ID_KEEP_ALIVE};

   public interface FrameHandler {
      void onRegisterTcp(int connectionId);
      void onKeepAlive();
      void onPing(int id, boolean isReply);
      void onDataPacket(byte cmd, byte[] payload);
   }

   private WireCodec() {
   }

   public static byte[] encodeDataPacket(byte cmd, byte[] body) {
      if (body == null) body = new byte[0];
      int len = body.length;
      int varLen = varIntSize(len + 1);
      int objectLength = 1 + 1 + varLen + len;
      byte[] frame = new byte[LENGTH_BYTES + objectLength];
      frame[0] = (byte) (objectLength >>> 24);
      frame[1] = (byte) (objectLength >>> 16);
      frame[2] = (byte) (objectLength >>> 8);
      frame[3] = (byte) objectLength;

      int p = LENGTH_BYTES;
      frame[p++] = (byte) ID_DATA_PACKET;
      frame[p++] = cmd;
      p = writeVarInt(frame, p, len + 1);
      if (len > 0) {
         System.arraycopy(body, 0, frame, p, len);
      }
      return frame;
   }

   public static byte[] encodePing(int id, boolean isReply) {
      int zz = zigZag(id);
      int objectLength = 1 + varIntSize(zz) + 1;
      byte[] frame = new byte[LENGTH_BYTES + objectLength];
      frame[0] = (byte) (objectLength >>> 24);
      frame[1] = (byte) (objectLength >>> 16);
      frame[2] = (byte) (objectLength >>> 8);
      frame[3] = (byte) objectLength;

      int p = LENGTH_BYTES;
      frame[p++] = (byte) ID_PING;
      p = writeVarInt(frame, p, zz);
      frame[p] = (byte) (isReply ? 1 : 0);
      return frame;
   }

   public static void decodeFrames(byte[] data, FrameHandler handler) {
      if (data == null || data.length < LENGTH_BYTES) return;
      int offset = 0;
      while (offset + LENGTH_BYTES <= data.length) {
         int length = ((data[offset] & 0xFF) << 24)
                    | ((data[offset + 1] & 0xFF) << 16)
                    | ((data[offset + 2] & 0xFF) << 8)
                    | (data[offset + 3] & 0xFF);

         if (length <= 0 || offset + LENGTH_BYTES + length > data.length) {
            break;
         }

         offset += LENGTH_BYTES;
         int end = offset + length;
         int p = offset;

         // Read classId (7-bit little-endian varint)
         int classId = 0;
         for (int shift = 0; shift < 35; shift += 7) {
            if (p >= end) break;
            int b = data[p++] & 0xFF;
            classId |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
         }

         if (classId == ID_REGISTER_TCP) {
            int rawZz = 0;
            for (int shift = 0; shift < 35; shift += 7) {
               if (p >= end) break;
               int b = data[p++] & 0xFF;
               rawZz |= (b & 0x7F) << shift;
               if ((b & 0x80) == 0) break;
            }
            handler.onRegisterTcp(unZigZag(rawZz));
         } else if (classId == ID_KEEP_ALIVE) {
            handler.onKeepAlive();
         } else if (classId == ID_PING) {
            int rawZz = 0;
            for (int shift = 0; shift < 35; shift += 7) {
               if (p >= end) break;
               int b = data[p++] & 0xFF;
               rawZz |= (b & 0x7F) << shift;
               if ((b & 0x80) == 0) break;
            }
            boolean isReply = p < end && data[p++] != 0;
            handler.onPing(unZigZag(rawZz), isReply);
         } else if (classId == ID_DATA_PACKET) {
            if (p < end) {
               byte cmd = data[p++];
               int rawLen = 0;
               for (int shift = 0; shift < 35; shift += 7) {
                  if (p >= end) break;
                  int b = data[p++] & 0xFF;
                  rawLen |= (b & 0x7F) << shift;
                  if ((b & 0x80) == 0) break;
               }
               int dataLen = rawLen > 0 ? rawLen - 1 : 0;
               byte[] payload = new byte[dataLen];
               if (dataLen > 0 && p + dataLen <= end) {
                  System.arraycopy(data, p, payload, 0, dataLen);
               }
               handler.onDataPacket(cmd, payload);
            }
         }

         offset = end;
      }
   }

   public static int zigZag(int value) {
      return (value << 1) ^ (value >> 31);
   }

   public static int unZigZag(int value) {
      return (value >>> 1) ^ -(value & 1);
   }

   public static int varIntSize(int value) {
      if ((value >>> 7) == 0) return 1;
      if ((value >>> 14) == 0) return 2;
      if ((value >>> 21) == 0) return 3;
      if ((value >>> 28) == 0) return 4;
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
}
