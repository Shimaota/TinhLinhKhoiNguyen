package com.tlnt.server.net;

import com.tlnt.server.net.wire.Wire;

import java.io.EOFException;
import java.io.UTFDataFormatException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * Mirror của {@code com.girlkun.a.a.Message} phía client: opcode byte + body. Body mã hoá GIỐNG HỆT
 * {@link java.io.DataOutputStream}/{@link java.io.DataInputStream} (big-endian, String = modified UTF-8 có 2 byte
 * độ dài như {@code writeUTF}, byte[] = int độ dài + dữ liệu) — client đọc/ghi bằng đúng 2 class đó.
 *
 * Khác bản cũ: đọc/ghi thẳng trên mảng byte, không tạo ByteArray*Stream/Data*Stream cho mỗi gói (mỗi giây có
 * hàng trăm nghìn gói khi đông người). Đọc quá cuối gói -> {@link UncheckedIOException} bọc {@link EOFException},
 * handler không cần tự kiểm tra độ dài (dispatcher bắt lỗi và bỏ gói).
 *
 * 1 Message chỉ dùng cho 1 chiều: tạo bằng {@link #Message(int)} để GHI, bằng {@link #Message(byte, byte[])} để ĐỌC.
 * Không thread-safe (không cần: 1 gói chỉ 1 luồng xử lý).
 */
public final class Message {

   private static final byte[] EMPTY = new byte[0];

   private final byte opcode;
   private byte[] buf;
   private int pos;
   /** Đọc: số byte hợp lệ trong {@link #buf}. Ghi: không dùng. */
   private final int limit;

   /** Tạo message để GHI (server -> client). */
   public Message(int opcode) {
      this.opcode = (byte) opcode;
      this.buf = new byte[64];
      this.limit = -1;
   }

   /** Tạo message để ĐỌC (client -> server). Không copy {@code data}. */
   public Message(byte opcode, byte[] data) {
      this.opcode = opcode;
      this.buf = data == null ? EMPTY : data;
      this.limit = this.buf.length;
   }

   public byte getOpcode() {
      return opcode;
   }

   /** Body đã ghi (copy). */
   public byte[] getBytes() {
      return Arrays.copyOf(buf, pos);
   }

   /** Số byte body đã ghi. */
   public int length() {
      return pos;
   }

   /** Đóng gói thành frame gửi được (mã hoá 1 lần, gửi cho bao nhiêu người cũng được). */
   public Packet toPacket() {
      return new Packet(opcode, Wire.encodeDataPacket(opcode, buf, 0, pos));
   }

   /** Số byte còn lại chưa đọc. */
   public int remaining() {
      return limit - pos;
   }

   // ------------------------------------------------------------------ đọc

   private void need(int n) {
      if (pos + n > limit) {
         throw new UncheckedIOException(new EOFException("Goi opcode " + opcode + " thieu du lieu"));
      }
   }

   public boolean readBoolean() {
      need(1);
      return buf[pos++] != 0;
   }

   public byte readByte() {
      need(1);
      return buf[pos++];
   }

   public short readShort() {
      need(2);
      short v = (short) (((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF));
      pos += 2;
      return v;
   }

   public int readInt() {
      need(4);
      int v = ((buf[pos] & 0xFF) << 24) | ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8)
            | (buf[pos + 3] & 0xFF);
      pos += 4;
      return v;
   }

   public long readLong() {
      long hi = readInt() & 0xFFFFFFFFL;
      long lo = readInt() & 0xFFFFFFFFL;
      return (hi << 32) | lo;
   }

   public float readFloat() {
      return Float.intBitsToFloat(readInt());
   }

   /** Giống {@link java.io.DataInputStream#readUTF}. */
   public String readString() {
      int utfLength = readShort() & 0xFFFF;
      need(utfLength);
      int end = pos + utfLength;
      char[] chars = new char[utfLength];
      int count = 0;
      while (pos < end) {
         int c = buf[pos] & 0xFF;
         switch (c >> 4) {
            case 0, 1, 2, 3, 4, 5, 6, 7 -> {
               pos++;
               chars[count++] = (char) c;
            }
            case 12, 13 -> {
               if (pos + 2 > end) {
                  throw malformed();
               }
               int c2 = buf[pos + 1];
               if ((c2 & 0xC0) != 0x80) {
                  throw malformed();
               }
               chars[count++] = (char) (((c & 0x1F) << 6) | (c2 & 0x3F));
               pos += 2;
            }
            case 14 -> {
               if (pos + 3 > end) {
                  throw malformed();
               }
               int c2 = buf[pos + 1];
               int c3 = buf[pos + 2];
               if ((c2 & 0xC0) != 0x80 || (c3 & 0xC0) != 0x80) {
                  throw malformed();
               }
               chars[count++] = (char) (((c & 0x0F) << 12) | ((c2 & 0x3F) << 6) | (c3 & 0x3F));
               pos += 3;
            }
            default -> throw malformed();
         }
      }
      return new String(chars, 0, count);
   }

   private UncheckedIOException malformed() {
      return new UncheckedIOException(new UTFDataFormatException("Chuoi UTF sai o goi opcode " + opcode));
   }

   /** int độ dài + dữ liệu. */
   public byte[] readBytes() {
      int length = readInt();
      if (length < 0) {
         throw new UncheckedIOException(new EOFException("Do dai mang am o goi opcode " + opcode));
      }
      need(length);
      byte[] out = Arrays.copyOfRange(buf, pos, pos + length);
      pos += length;
      return out;
   }

   // ------------------------------------------------------------------ ghi

   private void ensure(int n) {
      if (pos + n > buf.length) {
         buf = Arrays.copyOf(buf, Math.max(buf.length * 2, pos + n));
      }
   }

   public void writeBoolean(boolean value) {
      ensure(1);
      buf[pos++] = (byte) (value ? 1 : 0);
   }

   public void writeByte(int value) {
      ensure(1);
      buf[pos++] = (byte) value;
   }

   public void writeShort(int value) {
      ensure(2);
      buf[pos++] = (byte) (value >>> 8);
      buf[pos++] = (byte) value;
   }

   public void writeInt(int value) {
      ensure(4);
      buf[pos++] = (byte) (value >>> 24);
      buf[pos++] = (byte) (value >>> 16);
      buf[pos++] = (byte) (value >>> 8);
      buf[pos++] = (byte) value;
   }

   public void writeLong(long value) {
      writeInt((int) (value >>> 32));
      writeInt((int) value);
   }

   public void writeFloat(float value) {
      writeInt(Float.floatToIntBits(value));
   }

   /** Giống {@link java.io.DataOutputStream#writeUTF}: tối đa 65535 byte sau mã hoá. */
   public void writeString(String value) {
      int length = value.length();
      int utfLength = 0;
      for (int i = 0; i < length; i++) {
         char c = value.charAt(i);
         utfLength += (c >= 0x0001 && c <= 0x007F) ? 1 : (c > 0x07FF ? 3 : 2);
      }
      if (utfLength > 65535) {
         throw new UncheckedIOException(new UTFDataFormatException("Chuoi qua dai: " + utfLength + " byte"));
      }
      ensure(2 + utfLength);
      buf[pos++] = (byte) (utfLength >>> 8);
      buf[pos++] = (byte) utfLength;
      for (int i = 0; i < length; i++) {
         char c = value.charAt(i);
         if (c >= 0x0001 && c <= 0x007F) {
            buf[pos++] = (byte) c;
         } else if (c > 0x07FF) {
            buf[pos++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
            buf[pos++] = (byte) (0x80 | ((c >> 6) & 0x3F));
            buf[pos++] = (byte) (0x80 | (c & 0x3F));
         } else {
            buf[pos++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
            buf[pos++] = (byte) (0x80 | (c & 0x3F));
         }
      }
   }

   /** Ghi nguyên các byte (không có độ dài phía trước) — dùng để nối phần gói đã encode sẵn. */
   public void writeRaw(byte[] value) {
      ensure(value.length);
      System.arraycopy(value, 0, buf, pos, value.length);
      pos += value.length;
   }

   /** int độ dài + dữ liệu. */
   public void writeBytes(byte[] value) {
      writeInt(value.length);
      writeRaw(value);
   }
}
