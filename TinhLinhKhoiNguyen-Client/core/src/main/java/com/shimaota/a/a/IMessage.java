package com.shimaota.a.a;

public interface IMessage extends IMessageReader {
   byte[] getBytes();

   void close();

   byte getOpcode();
}
