package com.tlnt.server.forge;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcContext;
import com.tlnt.server.npc.NpcService;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.QuestService;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.util.List;

/**
 * "Chế Tác Vũ Khí" (op 60, client {@code WeaponCraftWindow}) — bước nhiệm vụ 5.4 rèn vũ khí đầu tiên: Ông Morgi có nút
 * "Rèn vũ khí" khi đang ở bước đó ({@code NpcService}), mở cửa sổ 5 công thức; bấm "Chế tác" (C→S op 60 sub 0, byte vị
 * trí công thức — client gọi {@code sendMountAction}, tên sai) trả {@link #STONES} Đá thô sơ + {@link #BONES} Xương
 * thú, nhận vũ khí khoá với chỉ số ngẫu nhiên trong khoảng. Chỉ rèn được khi đang ở bước đó (1 lần).
 *
 * <p>Luồng session của người rèn; túi sửa dưới {@code synchronized (d)} (giao dịch chốt trên luồng người kia).
 */
public final class WeaponCraftService {

   /** {@code item_template.id} nguyên liệu. */
   static final int STONE = 267;
   static final int BONE = 266;
   static final int STONES = 5;
   static final int BONES = 1;

   /** Dòng chỉ số ({@code stat_type.id}). */
   private static final int HP = 3;
   private static final int MP = 5;
   private static final int ATTACK = 7;
   private static final int CRIT = 11;
   private static final int CRIT_DAMAGE_PERCENT = 14;
   private static final int ARMOR = 15;
   private static final int ARMOR_PEN = 17;
   private static final int EVASION = 19;
   private static final int ACCURACY = 21;
   private static final int MOVE_SPEED_PERCENT = 24;
   private static final int ATTACK_AMPLIFY_PERCENT = 44;

   /** Công thức theo thứ tự hiện trong cửa sổ: Rìu Đá, Giáo Đá, Roi da, Cung tên, Ná thun — {stat, min, max}. */
   static final List<RewardItem> RECIPES = List.of(
         weapon(98, new int[] {ATTACK, 77, 104}, new int[] {HP, 450, 625}, new int[] {ARMOR, 70, 95}),
         weapon(103, new int[] {ATTACK, 68, 92}, new int[] {CRIT, 200, 280}, new int[] {CRIT_DAMAGE_PERCENT, 1, 2}),
         weapon(108, new int[] {ATTACK, 85, 115}, new int[] {EVASION, 200, 280},
               new int[] {MOVE_SPEED_PERCENT, 4, 6}),
         weapon(113, new int[] {ATTACK, 68, 92}, new int[] {ACCURACY, 200, 280}, new int[] {ARMOR_PEN, 70, 95}),
         weapon(118, new int[] {ATTACK, 60, 81}, new int[] {MP, 450, 625},
               new int[] {ATTACK_AMPLIFY_PERCENT, 3, 5}));

   private final ItemService items;

   public WeaponCraftService(ItemService items) {
      this.items = items;
   }

   /** Nút "Rèn vũ khí" ở Morgi: nhớ chỗ NPC (rèn chỉ khi còn đứng gần) rồi mở cửa sổ. */
   public static void open(NpcContext ctx) {
      PlayerData d = ctx.data();
      d.craftMapId = ctx.mapId();
      d.craftX = ctx.spawn().x();
      d.craftY = ctx.spawn().y();
      ctx.send(recipesPacket(System.currentTimeMillis()));
   }

   /** Op 60 sub 0: các công thức (chỉ số hiện "min-max"); client mở cửa sổ khi nhận. */
   static Message recipesPacket(long now) {
      Message msg = new Message(Opcode.WEAPON_CRAFT);
      msg.writeByte(0);
      msg.writeByte(RECIPES.size());
      for (RewardItem recipe : RECIPES) {
         ItemPackets.writePreview(msg, recipe, now);
      }
      return msg;
   }

   /** Op 60 sub 1: đóng cửa sổ (client đọc 1 byte rồi ẩn). */
   static Message closePacket() {
      Message msg = new Message(Opcode.WEAPON_CRAFT);
      msg.writeByte(1);
      msg.writeByte(0);
      return msg;
   }

   /** C→S op 60 sub 0: rèn công thức {@code index}. */
   public void craft(PlayerSession session, int index) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || index < 0 || index >= RECIPES.size()) {
         return;
      }
      if (!QuestService.atStep(d, StepKind.CRAFT_WEAPON)) {
         session.send(Packets.notice("Hiện chưa thể rèn vũ khí."));
         return;
      }
      if (!near(d)) {
         session.send(Packets.notice("Hãy đứng gần thợ rèn."));
         return;
      }
      long now = System.currentTimeMillis();
      ItemData weapon = RECIPES.get(index).create(now);
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể rèn."));
            return;
         }
         ItemContainer bag = d.bag.deepCopy();
         if (ItemService.count(bag, STONE, now) < STONES || ItemService.count(bag, BONE, now) < BONES) {
            session.send(Packets.notice("Cần " + STONES + " " + itemName(STONE) + " và " + BONES + " "
                  + itemName(BONE) + "."));
            return;
         }
         ItemService.remove(bag, STONE, STONES, now);
         ItemService.remove(bag, BONE, BONES, now);
         String error = ItemService.give(d, bag, List.of(weapon), now); // thiếu chỗ thì không đổi gì
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
      }
      items.syncBagAndCurrency(session);
      session.send(closePacket());
      ItemPackets.receivedNotices(List.of(weapon)).forEach(session::send);
      Log.debug("Che tac vu khi: '%s' ren mon %d", d.name, weapon.templateId);
      QuestEvents.fire(session, StepKind.CRAFT_WEAPON, NpcService.NPC_MORGI); // bước nhắm vào Morgi (dấu "!")
   }

   private static boolean near(PlayerData d) {
      Float x = d.x;
      Float y = d.y;
      return d.craftMapId >= 0 && d.mapId == d.craftMapId && x != null && y != null
            && Math.abs(x - d.craftX) <= NpcService.TALK_RANGE_PX && Math.abs(y - d.craftY) <= NpcService.TALK_RANGE_PX;
   }

   private static String itemName(int templateId) {
      ItemPack.ItemTemplate t = GameDataCache.itemTemplate(templateId);
      return t != null ? t.name() : "vật phẩm";
   }

   private static RewardItem weapon(int templateId, int[]... stats) {
      int[][] min = new int[stats.length][];
      int[][] max = new int[stats.length][];
      for (int i = 0; i < stats.length; i++) {
         min[i] = new int[] {stats[i][0], stats[i][1]};
         max[i] = new int[] {stats[i][0], stats[i][2]};
      }
      return new RewardItem(templateId, 1, true, min, max, 0);
   }
}
