package com.tlnt.server.net.transport.kryonet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.tlnt.server.net.DataPacket;
import com.tlnt.server.net.Packet;
import com.tlnt.server.net.transport.NetConnection;
import com.tlnt.server.net.transport.NetListener;
import com.tlnt.server.net.transport.NetServer;
import com.tlnt.server.util.Log;

import java.net.InetSocketAddress;

/**
 * Tầng mạng KryoNet cũ — CHỈ để dự phòng ({@code net.transport=kryonet}) nếu tầng Netty có vấn đề trên máy nào đó.
 * Không dùng cho đông người: 1 luồng I/O, mỗi lần select quét toàn bộ kết nối, Kryo dùng chung 1 khoá, 128 KB bộ
 * đệm cố định / kết nối, client chậm làm luồng gửi quay vòng chờ.
 *
 * Đăng ký Kryo y hệt client ({@code NetworkClient.connectAndSetup()}): DataPacket = 75, byte[] = 74.
 */
public final class KryoNetServer implements NetServer {

   private final NetListener listener;
   private final Server server;

   public KryoNetServer(NetListener listener, int writeBufferBytes, int objectBufferBytes) {
      this.listener = listener;
      this.server = new Server(writeBufferBytes, objectBufferBytes) {
         @Override
         protected Connection newConnection() {
            return new KryoNetConnection();
         }
      };
      server.getKryo().register(DataPacket.class, 75);
      server.getKryo().register(byte[].class, 74);
      server.addListener(new Listener() {
         @Override
         public void connected(Connection c) {
            listener.connected((KryoNetConnection) c);
         }

         @Override
         public void received(Connection c, Object object) {
            if (object instanceof DataPacket packet) {
               listener.received((KryoNetConnection) c, packet.cmd, packet.data);
            }
         }

         @Override
         public void disconnected(Connection c) {
            listener.disconnected((KryoNetConnection) c);
         }
      });
   }

   @Override
   public void start(int port) throws Exception {
      server.bind(port);
      server.start();
      Log.info("Tang mang KryoNet (du phong) dang nghe cong %d", port);
   }

   @Override
   public void stop() {
      server.stop();
   }

   @Override
   public int connectionCount() {
      return server.getConnections().length;
   }

   @Override
   public String name() {
      return "kryonet";
   }

   private static final class KryoNetConnection extends Connection implements NetConnection {

      private volatile Object attachment;
      private volatile String remoteIp;

      @Override
      public int id() {
         return getID();
      }

      @Override
      public String remoteIp() {
         String ip = remoteIp;
         if (ip == null) {
            InetSocketAddress address = getRemoteAddressTCP();
            ip = address != null ? address.getAddress().getHostAddress() : "?";
            remoteIp = ip;
         }
         return ip;
      }

      @Override
      public void send(Packet packet) {
         if (isConnected()) {
            sendTCP(packet.dataPacket());
         }
      }

      @Override
      public void sendFrames(byte[] frames, int offset, int length) {
         // Tầng cũ không gửi được byte thô: tách từng frame DataPacket rồi gửi object (chậm, chỉ để dự phòng).
         int end = offset + length;
         int p = offset;
         while (p + 4 <= end && isConnected()) {
            int objectLength = ((frames[p] & 0xFF) << 24) | ((frames[p + 1] & 0xFF) << 16)
                  | ((frames[p + 2] & 0xFF) << 8) | (frames[p + 3] & 0xFF);
            int frameEnd = p + 4 + objectLength;
            int q = p + 4 + 1; // bỏ class id 0x4D
            byte cmd = frames[q++];
            int lengthPlusOne = 0;
            for (int shift = 0; ; shift += 7) {
               int b = frames[q++];
               lengthPlusOne |= (b & 0x7F) << shift;
               if ((b & 0x80) == 0) {
                  break;
               }
            }
            byte[] data = lengthPlusOne == 0 ? null : java.util.Arrays.copyOfRange(frames, q, q + lengthPlusOne - 1);
            sendTCP(new DataPacket(cmd, data));
            p = frameEnd;
         }
      }

      @Override
      public Object attachment() {
         return attachment;
      }

      @Override
      public void attach(Object attachment) {
         this.attachment = attachment;
      }
   }
}
