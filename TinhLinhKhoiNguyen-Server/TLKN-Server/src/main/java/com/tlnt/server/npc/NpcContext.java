package com.tlnt.server.npc;

import com.tlnt.server.data.model.MapPack;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Message;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.WorldPlayer;

/**
 * 1 lần tương tác với NPC: người chơi, NPC (vị trí {@code index} trong danh sách NPC của map = id thực thể client),
 * mẫu NPC. Chạy trên luồng session của người chơi.
 */
public final class NpcContext {

   /**
    * Trạng thái hộp thoại đang mở (chỉ luồng session): nút bấm gửi về phải khớp. {@code questButton}: nút 0 là "Nói
    * chuyện" của nhiệm vụ, các nút của NPC lùi 1. {@code templateId}: id client gửi kèm nút bấm (= id ở {@link #say}).
    */
   public record OpenDialog(int mapId, int npcIndex, int templateId, int page, int choices, boolean questButton) {
   }

   private final PlayerSession session;
   private final WorldPlayer player;
   private final int mapId;
   private final int npcIndex;
   private final MapTemplate.NpcSpawn spawn;
   private final MapPack.Npc template;
   /** Hộp thoại kế tiếp ({@link #say}) thêm nút "Nói chuyện" đầu tiên (đang ở bước nhiệm vụ nói chuyện với NPC này). */
   boolean questButton;

   NpcContext(PlayerSession session, WorldPlayer player, int mapId, int npcIndex, MapTemplate.NpcSpawn spawn,
         MapPack.Npc template) {
      this.session = session;
      this.player = player;
      this.mapId = mapId;
      this.npcIndex = npcIndex;
      this.spawn = spawn;
      this.template = template;
   }

   public PlayerSession session() {
      return session;
   }

   public WorldPlayer player() {
      return player;
   }

   public PlayerData data() {
      return player.data();
   }

   public int mapId() {
      return mapId;
   }

   public MapTemplate.NpcSpawn spawn() {
      return spawn;
   }

   public MapPack.Npc template() {
      return template;
   }

   /**
    * Mở hộp thoại lớn ({@code NpcDialogPopupV2}: chân dung {@code npc_template.avatar_icon}, tên, chữ chạy, các nút).
    * {@code page} = số trang do NPC tự đặt (0..127), nút bấm gửi về kèm trang này ({@link NpcAction#onChoice}).
    * Bấm nút nào client cũng tự đóng hộp thoại. Chữ hỗ trợ xuống dòng {@code \n} + markup màu Textra.
    *
    * <p>Op 24 sub 1: byte trang (client gọi npcType), int npc_template.id, byte 1 (hộp thoại lớn), str tên, str chân
    * dung, str nội dung, byte n + n×str nút. Int là MẪU NPC (không phải id thực thể): hướng dẫn tân thủ client so
    * {@code NpcDialogPopupV2.npcId} với mẫu (Morgi 2, Nimora 3, Cây táo 18 — {@code screen/login/tutorial/}).
    */
   public void say(int page, String text, String... choices) {
      boolean withQuest = questButton;
      questButton = false;
      if (withQuest) {
         String[] all = new String[choices.length + 1];
         all[0] = QuestService.TALK_BUTTON;
         System.arraycopy(choices, 0, all, 1, choices.length);
         choices = all;
      }
      Message msg = new Message(24);
      msg.writeByte(1);
      msg.writeByte(page);
      msg.writeInt(template.id());
      msg.writeByte(1);
      msg.writeString(template.name());
      msg.writeString(template.portraitPath());
      msg.writeString(text);
      msg.writeByte(choices.length);
      for (String choice : choices) {
         msg.writeString(choice);
      }
      data().npcDialog = new OpenDialog(mapId, npcIndex, template.id(), page, choices.length, withQuest);
      session.send(msg);
   }

   /** Chữ nổi trên đầu NPC (op 24 sub 2) — chỉ người này thấy. */
   public void bubble(String text) {
      Message msg = new Message(24);
      msg.writeByte(2);
      msg.writeInt(npcIndex);
      msg.writeString(text);
      session.send(msg);
   }

   public void send(Message msg) {
      session.send(msg);
   }
}
