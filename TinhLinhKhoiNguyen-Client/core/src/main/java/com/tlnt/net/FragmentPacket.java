package com.tlnt.net;

import java.io.Serializable;

public final class FragmentPacket implements Serializable {
   public int packetId;
   public byte[] data;
   public int offset;
   public int totalSize;
}
