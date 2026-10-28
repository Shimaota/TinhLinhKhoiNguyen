package com.tlnt.server.quest;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Message;
import com.tlnt.server.player.PlayerData;

import java.util.List;

/** Gói S→C nhiệm vụ chính tuyến (op 47 — client {@code handleQuestUpdate}) + mũi tên chỉ đường (op 63). */
public final class QuestPackets {

   public static final int OPCODE = 47;
   public static final int GUIDE_ARROW = 63;

   private QuestPackets() {
   }

   /**
    * Sub 0: cả nhiệm vụ ({@code GameState.tutorialState}) — int id, str tên, str mô tả, byte n bước + n×(str tên bước,
    * str gợi ý, int số lượng), byte n thưởng + n×item, rồi phần tiến độ như sub 1.
    */
   public static Message full(MainQuest quest, PlayerData.MainQuestProgress q, long now) {
      Message msg = new Message(OPCODE);
      msg.writeByte(0);
      msg.writeInt(quest.id());
      msg.writeString(quest.name());
      msg.writeString(quest.description());
      msg.writeByte(quest.steps().size());
      for (QuestStep step : quest.steps()) {
         msg.writeString(step.name());
         msg.writeString(step.hint());
         msg.writeInt(step.count());
      }
      List<RewardItem> rewards = quest.rewards();
      msg.writeByte(rewards.size());
      for (RewardItem reward : rewards) {
         ItemPackets.writeItem(msg, reward.create(now));
      }
      writeProgress(msg, quest, q);
      return msg;
   }

   /** Sub 1: tiến độ — byte bước đang làm, int tiến độ, int id NPC có dấu "!" (0 = không), int map của NPC đó. */
   public static Message progress(MainQuest quest, PlayerData.MainQuestProgress q) {
      Message msg = new Message(OPCODE);
      msg.writeByte(1);
      writeProgress(msg, quest, q);
      return msg;
   }

   /** Sub 2: hiệu ứng hoàn thành nhiệm vụ trên nhân vật. */
   public static Message completeEffect() {
      Message msg = new Message(OPCODE);
      msg.writeByte(2);
      return msg;
   }

   private static void writeProgress(Message msg, MainQuest quest, PlayerData.MainQuestProgress q) {
      QuestStep step = quest.step(q.stage());
      msg.writeByte(q.stage());
      msg.writeInt(q.progress());
      int npc = step == null ? 0 : step.markerNpc();
      boolean marker = npc > 0;
      msg.writeInt(npc);
      msg.writeInt(marker ? step.mapId() : 0);
   }

   /** Op 63 sub 0: tắt mũi tên chỉ đường. */
   public static Message arrowOff() {
      Message msg = new Message(GUIDE_ARROW);
      msg.writeByte(0);
      return msg;
   }

   /**
    * Op 63 sub 1: mũi tên quanh nhân vật chỉ về vùng ({@code x}, {@code y}, {@code w}, {@code h}) pixel (client chia
    * 100 như toạ độ NPC); đứng trong vùng thì mũi tên ẩn.
    */
   public static Message arrow(float x, float y, float w, float h) {
      Message msg = new Message(GUIDE_ARROW);
      msg.writeByte(1);
      msg.writeFloat(x);
      msg.writeFloat(y);
      msg.writeFloat(w);
      msg.writeFloat(h);
      return msg;
   }
}
