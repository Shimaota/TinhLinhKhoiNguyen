package com.tlnt.net;

import com.shimaota.a.a.IMessage;
import java.io.Serializable;
import java.util.Arrays;

public class DataPacket implements Serializable {
   public byte opcode;
   public byte[] payload;

   public DataPacket() {
   }

   public DataPacket(IMessage message) {
      opcode = message.getOpcode();
      payload = message.getBytes();
   }

   @Override
   public String toString() {
      return "DataNetwork{cmd=" + opcode + ", data=" + Arrays.toString(payload) + "}";
   }
}
