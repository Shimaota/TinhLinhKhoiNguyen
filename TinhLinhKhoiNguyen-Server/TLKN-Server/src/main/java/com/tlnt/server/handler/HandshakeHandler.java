package com.tlnt.server.handler;

import com.tlnt.server.data.DataVersionService;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ServerEntry;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packet;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.net.Packets;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.util.List;

/**
 * Opcode 0 (client -> server, luôn là packet đầu tiên sau khi connect — xem
 * {@code NetworkClient.connectAndSetup()} / {@code ServerPacketSender.sendHandshake()}).
 *
 * Thứ tự field client gửi: clientVersion, platform, locale, gameDataVersion,
 * mapDataVersion, itemDataVersion, skillDataVersion.
 *
 * Server so version client báo với version thật ({@link DataVersionService}) rồi
 * đẩy URL cập nhật (opcode 15-18) cho từng phần lệch version, cộng thêm opcode 19
 * để client tự kiểm tra/tải phần tài nguyên lớn (ảnh/skeleton/âm thanh) qua HTTP —
 * server không đọc/giữ nội dung các file đó, chỉ trỏ URL.
 */
public final class HandshakeHandler implements PacketHandler {

   private final DataVersionService dataVersionService;
   private final String httpBaseUrl;
   /** Gói opcode 59 dựng sẵn; null nếu chưa cấu hình khoá (client chỉ đọc được tài nguyên không mã hoá). */
   private final Packet decryptKeys;

   public HandshakeHandler(DataVersionService dataVersionService, String httpBaseUrl, byte[] textureKey,
         byte[] atlasKey) {
      this.dataVersionService = dataVersionService;
      this.httpBaseUrl = httpBaseUrl;
      if (textureKey != null && atlasKey != null) {
         this.decryptKeys = Packets.decryptKeys(textureKey, atlasKey).toPacket();
      } else {
         this.decryptKeys = null;
         Log.warn("Chua cau hinh assets.textureDecryptKey/atlasDecryptKey -> client khong giai ma duoc tai nguyen ma hoa");
      }
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      String clientVersion = message.readString();
      String platform = message.readString();
      String locale = message.readString();
      int gameDataVersion = message.readInt();
      int mapDataVersion = message.readInt();
      int itemDataVersion = message.readInt();
      int skillDataVersion = message.readInt();

      session.setReportedDataVersions(gameDataVersion, mapDataVersion, itemDataVersion, skillDataVersion);

      Log.debug("Handshake #%d: client=%s platform=%s locale=%s data(game=%d,map=%d,item=%d,skill=%d)",
            session.getId(), clientVersion, platform, locale,
            gameDataVersion, mapDataVersion, itemDataVersion, skillDataVersion);

      // Khoá giải mã phải tới trước khi client bắt đầu load ảnh/skeleton đã tải về.
      if (decryptKeys != null) {
         session.send(decryptKeys);
      }

      sendUpdateIfStale(session, 15, DataVersionService.GAME, gameDataVersion);
      sendUpdateIfStale(session, 16, DataVersionService.MAP, mapDataVersion);
      sendUpdateIfStale(session, 17, DataVersionService.ITEM, itemDataVersion);
      sendUpdateIfStale(session, 18, DataVersionService.SKILL, skillDataVersion);

      // Luôn gửi để client tự đối chiếu version tài nguyên lớn (ver_X/info.txt) của nó.
      Message checkResources = new Message(19);
      checkResources.writeString(httpBaseUrl);
      checkResources.writeString(httpBaseUrl + "/allversion.txt");
      session.send(checkResources);

      sendServerList(session);
   }

   /**
    * Opcode 43: danh sách máy chủ lấy từ cache (nạp từ bảng SQL {@code server_list}), đúng format
    * {@code MessageHandler case 43}: byte count, rồi mỗi server: name (String), host (String), port (int).
    */
   private void sendServerList(PlayerSession session) {
      List<ServerEntry> servers = GameDataCache.servers();
      int count = Math.min(servers.size(), Byte.MAX_VALUE); // client đọc count bằng readByte()

      Message serverList = new Message(43);
      serverList.writeByte(count);
      for (int i = 0; i < count; i++) {
         ServerEntry server = servers.get(i);
         serverList.writeString(server.name());
         serverList.writeString(server.host());
         serverList.writeInt(server.port());
      }
      session.send(serverList);
   }

   private void sendUpdateIfStale(PlayerSession session, int opcode, String dataKey, int clientVersion) {
      int serverVersion = dataVersionService.getVersion(dataKey);
      if (clientVersion == serverVersion) {
         return;
      }
      Log.debug("Connection #%d lech version '%s' (client=%d, server=%d) -> gui URL cap nhat",
            session.getId(), dataKey, clientVersion, serverVersion);
      Message update = new Message(opcode);
      update.writeString(httpBaseUrl + "/" + dataKey);
      session.send(update);
   }
}
