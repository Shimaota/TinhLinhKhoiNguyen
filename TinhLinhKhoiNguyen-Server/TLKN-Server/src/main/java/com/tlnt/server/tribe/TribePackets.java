package com.tlnt.server.tribe;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;

import java.util.List;
import java.util.function.IntFunction;

/**
 * Gói S→C op 37 (client {@code ServerPacketHandler.handlePetUpdate} — tên sai, thật ra là Bộ lạc; đọc 1 bộ lạc bằng
 * {@code readPetFromMessage}).
 *
 * <pre>
 * 37/0 danh sách tab "Bộ lạc": byte n, n × bộ lạc
 * 37/1 bộ lạc của mình: bool có, [bộ lạc]  (false = không thuộc bộ lạc nào -> client ẩn các tab trong)
 * 37/2 tab "Thông báo": byte n, n × str
 * 37/3 Rương bộ lạc: int n, n × item (ô = thứ tự, op 27 sub 14 / 15 gửi lại số ô)
 * 37/4 kỹ năng: int n, n × int tribe_skill.id
 * 37/5 chỉ số biểu tượng đang dùng: int n, n × (int stat_type.id, bool ranges=false, byte n, n × int giá trị)
 * bộ lạc = int id, str tên, str khẩu hiệu, byte sức chứa, byte cấp, int tribe_emblem.id, long hết hạn biểu tượng
 *          (-1 = "vĩnh viễn", client đếm ngược tới mốc này), long quỹ vàng, long quỹ lam thạch, long điểm trong cấp,
 *          long điểm cần, byte n × (int id, str tên, int cấp, byte vai trò 0 Tù trưởng / 1 Phó / 2 Thành viên,
 *          byte char_template, str avatar, long cống hiến), byte m × đơn xin vào
 *          (int id, str tên, int cấp, byte char_template, str avatar)
 * </pre>
 */
final class TribePackets {

   private TribePackets() {
   }

   static Message list(List<Tribe> tribes, IntFunction<PlayerData> online) {
      Message msg = new Message(Opcode.TRIBE);
      msg.writeByte(0);
      msg.writeByte(tribes.size());
      for (Tribe t : tribes) {
         write(msg, t, online, false);
      }
      return msg;
   }

   /** {@code online}: id -> nhân vật đang online (tên / cấp mới nhất), null = dùng bản chụp. */
   static Message mine(Tribe t, IntFunction<PlayerData> online) {
      Message msg = new Message(Opcode.TRIBE);
      msg.writeByte(1);
      msg.writeBoolean(t != null);
      if (t != null) {
         write(msg, t, online, true);
      }
      return msg;
   }

   static Message notices(List<String> notices) {
      Message msg = new Message(Opcode.TRIBE);
      msg.writeByte(2);
      int from = Math.max(0, notices.size() - Byte.MAX_VALUE);
      msg.writeByte(notices.size() - from);
      for (int i = notices.size() - 1; i >= from; i--) { // mới nhất trên cùng
         msg.writeString(notices.get(i));
      }
      return msg;
   }

   static Message warehouse(List<ItemData> items) {
      Message msg = new Message(Opcode.TRIBE);
      msg.writeByte(3);
      msg.writeInt(items.size());
      for (ItemData item : items) {
         ItemPackets.writeItem(msg, item);
      }
      return msg;
   }

   static Message skills(List<Integer> skills) {
      Message msg = new Message(Opcode.TRIBE);
      msg.writeByte(4);
      msg.writeInt(skills.size());
      for (int id : skills) {
         msg.writeInt(id);
      }
      return msg;
   }

   /** Chỉ số gắn với biểu tượng Bộ lạc đang dùng; sub riêng để không đổi cấu trúc gói thông tin cũ. */
   static Message emblemStats(Tribe t) {
      Message msg = new Message(Opcode.TRIBE);
      msg.writeByte(5);
      int[][] lines = t.emblemLines;
      msg.writeInt(lines.length);
      for (int[] line : lines) {
         msg.writeInt(line[0]);
         msg.writeBoolean(false); // false = giá trị đã chốt, cùng định dạng StatLine trong gói item
         msg.writeByte(line.length - 1);
         for (int i = 1; i < line.length; i++) {
            msg.writeInt(line[i]);
         }
      }
      return msg;
   }

   /**
    * 1 bộ lạc. Danh sách tìm kiếm ({@code full} false) chỉ cần Tù trưởng (tên) + số thành viên: người khác gửi tên rỗng,
    * không gửi đơn xin vào — gói nhỏ hơn nhiều.
    */
   private static void write(Message msg, Tribe t, IntFunction<PlayerData> online, boolean full) {
      msg.writeInt(t.id);
      msg.writeString(t.name);
      msg.writeString(t.slogan);
      msg.writeByte(t.maxMembers());
      msg.writeByte(t.level);
      msg.writeInt(t.emblemId);
      msg.writeLong(t.emblemExpireAt);
      msg.writeLong(t.gold);
      msg.writeLong(t.diamond);
      boolean max = t.level >= TribeRules.MAX_LEVEL;
      long need = TribeRules.need(Math.min(TribeRules.MAX_LEVEL, t.level + 1));
      msg.writeLong(max ? need : t.exp);
      msg.writeLong(need);
      int n = Math.min(t.members.size(), Byte.MAX_VALUE);
      msg.writeByte(n);
      for (int i = 0; i < n; i++) {
         Tribe.Member m = t.members.get(i);
         boolean shown = full || m.role == TribeRules.ROLE_LEADER;
         PlayerData d = shown ? online.apply(m.id) : null;
         msg.writeInt(m.id);
         msg.writeString(!shown ? "" : d != null ? d.name : m.name);
         msg.writeInt(d != null ? d.level : m.level);
         msg.writeByte(m.role);
         msg.writeByte(m.tpl);
         msg.writeLong(m.points);
      }
      int a = full ? Math.min(t.applicants.size(), Byte.MAX_VALUE) : 0;
      msg.writeByte(a);
      for (int i = 0; i < a; i++) {
         Tribe.Applicant ap = t.applicants.get(i);
         msg.writeInt(ap.id());
         msg.writeString(ap.name());
         msg.writeInt(ap.level());
         msg.writeByte(ap.tpl());
      }
   }
}
