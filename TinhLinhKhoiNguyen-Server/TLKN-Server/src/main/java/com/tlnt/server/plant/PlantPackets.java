package com.tlnt.server.plant;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;

/**
 * Gói cây hái được. S→C op 54 = client {@code ServerPacketHandler.handlePlantInteraction}; toạ độ pixel (client tự
 * chia 100). Id thực thể cây do khu đặt ({@code world/ZonePlants}), C→S op 54 sub 0 gửi lại id này khi bấm hái.
 */
public final class PlantPackets {

   private PlantPackets() {
   }

   /** Sub 0: thêm nhiều cây (client KHÔNG xoá cây cũ — chỉ gửi ngay sau op 6, lúc client vừa xoá hết). */
   public static Message list(int count, int[] ids, float[] xs, float[] ys, int[] templateIds) {
      Message msg = new Message(Opcode.PLANT);
      msg.writeByte(0);
      msg.writeInt(count);
      for (int i = 0; i < count; i++) {
         msg.writeInt(ids[i]);
         msg.writeFloat(xs[i]);
         msg.writeFloat(ys[i]);
         msg.writeInt(templateIds[i]);
      }
      return msg;
   }

   /** Sub 1: 1 cây vừa mọc. */
   public static Message spawn(int id, float x, float y, int templateId) {
      Message msg = new Message(Opcode.PLANT);
      msg.writeByte(1);
      msg.writeInt(id);
      msg.writeFloat(x);
      msg.writeFloat(y);
      msg.writeInt(templateId);
      return msg;
   }

   /**
    * Sub 2 (chỉ người hái): thanh "Đang thu hoạch..." dưới chân ({@code HarvestProgressBar}); client KHOÁ di chuyển
    * tới khi thanh chạy hết hoặc nhận sub 4.
    */
   public static Message progress(int elapsedMs, int durationMs) {
      Message msg = new Message(Opcode.PLANT);
      msg.writeByte(2);
      msg.writeInt(elapsedMs);
      msg.writeInt(durationMs);
      return msg;
   }

   /** Sub 3 (cả khu): người {@code playerId} quay về phía cây, động tác PICK; cây biến mất. */
   public static Message picked(int playerId, int plantId) {
      Message msg = new Message(Opcode.PLANT);
      msg.writeByte(3);
      msg.writeInt(playerId);
      msg.writeInt(plantId);
      return msg;
   }

   /** Sub 4 (chỉ người hái): huỷ thanh thu hoạch (được đi lại). */
   public static Message stop() {
      Message msg = new Message(Opcode.PLANT);
      msg.writeByte(4);
      return msg;
   }
}
