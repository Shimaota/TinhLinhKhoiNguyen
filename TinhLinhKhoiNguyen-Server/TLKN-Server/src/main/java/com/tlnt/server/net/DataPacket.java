package com.tlnt.server.net;

/**
 * Phải khớp 100% với {@code com.tlnt.net.DataPacket} phía client: cùng tên field,
 * cùng thứ tự khai báo ({@code cmd} trước {@code data}), vì Kryo FieldSerializer
 * mặc định dựa vào thứ tự đó để suy ra wire format. Đừng đổi nếu không sửa cả client.
 *
 * Chỉ tầng KryoNet dự phòng dùng class này; tầng Netty mã hoá tay đúng định dạng này (xem {@code Wire}).
 */
public class DataPacket {

   public byte cmd;
   public byte[] data;

   /** Constructor rỗng bắt buộc để Kryo có thể deserialize. */
   public DataPacket() {
   }

   public DataPacket(byte cmd, byte[] data) {
      this.cmd = cmd;
      this.data = data;
   }
}
