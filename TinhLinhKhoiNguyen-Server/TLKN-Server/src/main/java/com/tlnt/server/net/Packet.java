package com.tlnt.server.net;

import java.util.Arrays;

/**
 * Gói S→C đã mã hoá xong thành frame (xem {@link com.tlnt.server.net.wire.Wire}). Bất biến, dùng chung được cho
 * nhiều người/nhiều luồng: phát cho cả khu = mã hoá 1 lần, gửi N lần cùng 1 mảng byte.
 *
 * Tạo bằng {@link Message#toPacket()}; gói tĩnh (thông tin map, khoá giải mã...) nên tạo 1 lần rồi giữ lại.
 */
public final class Packet {

   private final byte opcode;
   private final byte[] frame;
   private DataPacket dataPacket;

   Packet(byte opcode, byte[] frame) {
      this.opcode = opcode;
      this.frame = frame;
   }

   public byte opcode() {
      return opcode;
   }

   /** Frame đầy đủ (4 byte độ dài + object). KHÔNG được sửa mảng này. */
   public byte[] frame() {
      return frame;
   }

   /** Dạng object cho tầng KryoNet cũ (net.transport=kryonet). */
   public DataPacket dataPacket() {
      DataPacket p = dataPacket;
      if (p == null) {
         // frame = [len 4][0x4D][cmd][varint len+1][body]: bỏ phần đầu để lấy body.
         int headerEnd = 4 + 2;
         while ((frame[headerEnd] & 0x80) != 0) {
            headerEnd++;
         }
         headerEnd++;
         p = new DataPacket(opcode, Arrays.copyOfRange(frame, headerEnd, frame.length));
         dataPacket = p; // tạo trùng giữa 2 luồng cũng vô hại
      }
      return p;
   }
}
