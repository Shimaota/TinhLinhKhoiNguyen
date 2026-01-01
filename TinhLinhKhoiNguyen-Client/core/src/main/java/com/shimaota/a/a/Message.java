package com.shimaota.a.a;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class Message implements IMessageBuffer {
   public byte opcode;
   public ByteArrayOutputStream outputStream;
   public DataOutputStream dataOutputStream;
   public ByteArrayInputStream inputStream;
   public DataInputStream dataInputStream;
   public long createTime;

   public Message(int opcode) {
      this((byte)opcode);
   }

   public Message(byte opcode) {
      this.opcode = opcode;
      this.outputStream = new ByteArrayOutputStream();
      this.dataOutputStream = new DataOutputStream(this.outputStream);
      this.createTime = System.currentTimeMillis();
   }

   public Message(byte opcode, byte[] data) {
      this.opcode = opcode;
      this.inputStream = new ByteArrayInputStream(data);
      this.dataInputStream = new DataInputStream(this.inputStream);
   }

   @Override
   public final byte getOpcode() {
      return this.opcode;
   }

   @Override
   public final byte[] getBytes() {
      return this.outputStream.toByteArray();
   }

   @Override
   public final void close() {
      try {
         if (this.inputStream != null) {
            this.inputStream.close();
         }

         if (this.outputStream != null) {
            this.outputStream.close();
         }

         if (this.dataInputStream != null) {
            this.dataInputStream.close();
         }

         if (this.dataOutputStream != null) {
            this.dataOutputStream.close();
         }
      } catch (Exception e) {
      }
   }

   @Override
   public final boolean readBoolean() {
      try {
         return this.dataInputStream.readBoolean();
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public final byte readByte() {
      try {
         return this.dataInputStream.readByte();
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public final short readShort() {
      try {
         return this.dataInputStream.readShort();
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public final int readInt() {
      try {
         return this.dataInputStream.readInt();
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public final long readLong() {
      try {
         return this.dataInputStream.readLong();
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public final float readFloat() {
      try {
         return this.dataInputStream.readFloat();
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public final String readString() {
      try {
         return this.dataInputStream.readUTF();
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   public final void writeBoolean(boolean value) {
      try {
         this.dataOutputStream.writeBoolean(value);
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   public final void writeByte(int value) {
      try {
         this.dataOutputStream.writeByte(value);
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   public final void writeFloat(float value) {
      try {
         this.dataOutputStream.writeFloat(value);
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   public final void writeInt(int value) {
      try {
         this.dataOutputStream.writeInt(value);
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   public final void writeLong(long value) {
      try {
         this.dataOutputStream.writeLong(value);
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   public final void writeString(String value) {
      try {
         this.dataOutputStream.writeUTF(value);
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public final byte[] readBytes() {
      try {
         byte[] bytes = new byte[this.dataInputStream.readInt()];
         this.dataInputStream.read(bytes);
         return bytes;
      } catch (java.io.IOException e) {
         throw new RuntimeException(e);
      }
   }
}
