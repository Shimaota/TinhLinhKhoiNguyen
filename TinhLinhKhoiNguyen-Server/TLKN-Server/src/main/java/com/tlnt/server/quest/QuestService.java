package com.tlnt.server.quest;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcContext;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.Leveling;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.player.PlayerSettings;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Nhiệm vụ chính tuyến (op 47). Trạng thái = {@link PlayerData#mainQuest} (nhiệm vụ, bước, tiến độ — cột JSON
 * {@code main_quest}); nội dung ở {@link MainQuests}. Client không gửi gói nhiệm vụ nào: server tự nghe sự kiện của các
 * hệ thống khác ({@link QuestEvents}) rồi gửi tiến độ (op 47 sub 1) + mũi tên chỉ đường (op 63).
 *
 * <p>Bước {@link StepKind#TALK}: bấm đúng NPC thì menu NPC có thêm nút "Nói chuyện" ĐẦU tiên ({@code NpcService});
 * bấm -> hộp thoại các trang (trang {@link #DIALOG_PAGE}+i) và bước xong ngay. Bước cuối của nhiệm vụ là TALK: nhận
 * thưởng lúc bấm "Nói chuyện" ({@code rewardPage} -1) hoặc lúc bấm "Tiếp tục" ở trang {@code rewardPage}; đóng sớm thì
 * vẫn ở bước đó, nói chuyện lại được.
 *
 * <p>Mọi thay đổi chạy trên luồng session của người chơi (sự kiện từ luồng tick khu được chuyển về hàng đợi session).
 */
public final class QuestService {

   /** Trang hộp thoại NPC dành cho hội thoại nhiệm vụ: {@code DIALOG_PAGE + i} (trang NPC thường dùng 0..99). */
   public static final int DIALOG_PAGE = 100;
   public static final String TALK_BUTTON = "Nói chuyện";
   /** Ngưỡng % HP / MP tự dùng táo mà hướng dẫn cài đặt yêu cầu. */
   static final int TUTORIAL_THRESHOLD = 10;

   /** Hội thoại nhiệm vụ đang mở: của bước nào (nhiệm vụ đã sang bước kế vẫn đọc tiếp đúng các trang này). */
   public record Dialog(int questId, int stage) {
   }

   private final World world;
   private final ItemService items;
   private final PlayerService players;
   private final Executor gamePool;

   public QuestService(World world, ItemService items, PlayerService players, Executor gamePool) {
      this.world = world;
      this.items = items;
      this.players = players;
      this.gamePool = gamePool;
   }

   Executor gamePool() {
      return gamePool;
   }

   WorldPlayer online(int playerId) {
      return world.online(playerId);
   }

   /** Tiến độ hiện tại; chưa có thì nhận nhiệm vụ 1. */
   static PlayerData.MainQuestProgress progress(PlayerData d) {
      PlayerData.MainQuestProgress q = d.mainQuest;
      if (q == null) {
         q = new PlayerData.MainQuestProgress(1, 0, 0);
         d.mainQuest = q;
      }
      return q;
   }

   /** Đang ở bước loại {@code kind} không (nút / cửa sổ chỉ mở khi đang làm bước đó). */
   public static boolean atStep(PlayerData d, StepKind kind) {
      QuestStep step = currentStep(d.mainQuest);
      return step != null && step.kind() == kind;
   }

   static QuestStep currentStep(PlayerData.MainQuestProgress q) {
      return q == null ? null : MainQuests.get(q.questId()).step(q.stage());
   }

   // ================================================================== gửi client

   /** Cả nhiệm vụ + mũi tên (vào game, đổi nhiệm vụ). */
   public void sendAll(PlayerSession session, PlayerData d) {
      PlayerData.MainQuestProgress q = progress(d);
      MainQuest quest = MainQuests.get(q.questId());
      session.send(QuestPackets.full(quest, q, System.currentTimeMillis()));
      sendArrow(session, d, quest.step(q.stage()));
   }

   private void sendProgress(PlayerSession session, PlayerData d, PlayerData.MainQuestProgress q) {
      MainQuest quest = MainQuests.get(q.questId());
      session.send(QuestPackets.progress(quest, q));
      sendArrow(session, d, quest.step(q.stage()));
   }

   /** Mũi tên theo map nhân vật đang đứng. */
   private void sendArrow(PlayerSession session, PlayerData d, QuestStep step) {
      if (d.bossPathMap > 0) {
         return; // đang "Tìm đường" tới boss thế giới (worldboss/WorldBossPath): giữ mũi tên boss
      }
      session.send(QuestGuide.arrow(step, d.mapId, world::mapTemplate));
   }

   // ================================================================== sự kiện (luồng session)

   /**
    * Sự kiện {@code kind} với đối tượng {@code target} (id NPC / quái, 0 = không có), {@code amount} lần. Không đúng
    * bước đang làm thì bỏ qua.
    */
   public void onEvent(PlayerSession session, StepKind kind, int target, int amount) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      PlayerData.MainQuestProgress q = progress(d);
      QuestStep step = currentStep(q);
      if (step == null || step.kind() != kind || kind == StepKind.TALK || kind == StepKind.WAIT
            || (step.target() != 0 && target != step.target())) {
         return;
      }
      int state = stateValue(d, step, System.currentTimeMillis());
      int value = step.accumulates() ? q.progress() + amount
            : kind == StepKind.UPGRADE_STAR ? Math.max(q.progress(), amount) // số sao vừa nâng THÀNH CÔNG
            : state >= 0 ? state : step.count();
      setProgress(session, d, q, step, value);
   }

   /**
    * Bước tính từ trạng thái đang có (không cộng dồn sự kiện): tiến độ hiện tại; -1 = bước tính theo sự kiện. Sự kiện
    * của các bước này chỉ là "trạng thái vừa đổi, tính lại".
    */
   static int stateValue(PlayerData d, QuestStep step, long now) {
      return switch (step.kind()) {
         case BIND_POTIONS -> boundPotions(d);
         case COLLECT, COLLECT_ELITE, COLLECT_BOSS ->
               (int) Math.min(Integer.MAX_VALUE, ItemService.count(d.bag, step.target(), now));
         case REACH_LEVEL -> d.level;
         case JOIN_PARTY -> d.party != null ? 1 : 0;
         case AUTO_SETTINGS -> d.settings != null && tutorialSettings(d.settings) ? 1 : 0;
         case REACH_MAP -> d.mapId == step.target() ? 1 : 0;
         default -> -1;
      };
   }

   private void setProgress(PlayerSession session, PlayerData d, PlayerData.MainQuestProgress q, QuestStep step,
         int value) {
      value = Math.max(0, Math.min(step.count(), value));
      if (value >= step.count()) {
         advance(session, d, q);
         return;
      }
      if (value != q.progress()) {
         PlayerData.MainQuestProgress next = new PlayerData.MainQuestProgress(q.questId(), q.stage(), value);
         d.mainQuest = next;
         sendProgress(session, d, next);
      }
   }

   /** Xong bước đang làm (không phải bước cuối) -> bước kế. */
   private void advance(PlayerSession session, PlayerData d, PlayerData.MainQuestProgress q) {
      MainQuest quest = MainQuests.get(q.questId());
      if (quest.lastStage(q.stage())) {
         return; // bước cuối = nói chuyện nhận thưởng ({@link #finishQuest})
      }
      PlayerData.MainQuestProgress next = new PlayerData.MainQuestProgress(q.questId(), q.stage() + 1, 0);
      d.mainQuest = next;
      session.send(QuestPackets.completeEffect()); // hiệu ứng hoàn thành mỗi bước (cả nhiệm vụ: finishQuest)
      Log.debug("Nhiem vu: '%s' xong buoc %d.%d", d.name, q.questId(), q.stage() + 1);
      enterStep(session, d, next);
      if (d.mainQuest == next) { // enterStep không đổi gì (đổi thì đã tự gửi)
         sendProgress(session, d, next);
      }
   }

   /**
    * Vừa sang bước {@code q}: bước tính theo trạng thái sẵn có ({@link #stateValue} — đã gán ô thuốc, đã cài đặt, đã
    * có đồ trong túi, đã đủ cấp, đang trong tổ đội) thì tính ngay.
    *
    * Có đổi thì tự gửi tiến độ ({@link PlayerData#mainQuest} thành bản mới).
    */
   private void enterStep(PlayerSession session, PlayerData d, PlayerData.MainQuestProgress q) {
      QuestStep step = currentStep(q);
      if (step != null && step.escortItem() > 0) {
         world.startEscort(session, step.escortItem(), step.escortMaps()); // sinh vật hộ tống hiện sau lưng
      } else if (d.escorting) {
         world.stopEscort(session); // xong / nhảy khỏi bước hộ tống
      }
      int value = step == null ? -1 : stateValue(d, step, System.currentTimeMillis());
      if (value > 0) {
         setProgress(session, d, q, step, value);
      }
   }

   /** Ô thuốc HP có món hồi HP (loại 21 / 24) + ô thuốc MP có món hồi MP (22 / 25). */
   static int boundPotions(PlayerData d) {
      return (quickHas(d, 0, 21, 24) ? 1 : 0) + (quickHas(d, 1, 22, 25) ? 1 : 0);
   }

   private static boolean quickHas(PlayerData d, int index, int typeA, int typeB) {
      int slot = d.quickSlots != null && index < d.quickSlots.length ? d.quickSlots[index] : -1;
      ItemData item = slot >= 0 ? d.bag.get(slot) : null;
      int type = item == null || item.type() == null ? -1 : item.type().id();
      return type == typeA || type == typeB;
   }

   /**
    * Cài đặt đúng như hướng dẫn {@code TutorialAutoPlaySettingsStep} của client: bật 6 ô tự đánh, "Không đánh Người
    * và Boss", tự nhặt đồ, tự dùng thể lực / HP / MP, ngưỡng táo đỏ / táo xanh 10%.
    */
   public static boolean tutorialSettings(PlayerSettings s) {
      for (boolean slot : s.autoSkillSlotEnabled) {
         if (!slot) {
            return false;
         }
      }
      return s.autoIgnorePlayersAndBosses && s.autoPickupItems && s.autoUseStaminaItems && s.autoUseHpItems
            && s.autoUseMpItems && s.autoHpThresholdPercent == TUTORIAL_THRESHOLD
            && s.autoMpThresholdPercent == TUTORIAL_THRESHOLD;
   }

   // ================================================================== nói chuyện với NPC

   /** Bước đang làm là nói chuyện với NPC mẫu {@code npcTemplateId} ở map {@code mapId} -> hiện nút "Nói chuyện". */
   public static boolean canTalk(PlayerData d, int mapId, int npcTemplateId) {
      QuestStep step = currentStep(d.mainQuest == null ? new PlayerData.MainQuestProgress(1, 0, 0) : d.mainQuest);
      return step != null && step.kind() == StepKind.TALK && step.target() == npcTemplateId
            && (step.mapId() <= 0 || step.mapId() == mapId) && (step.escortItem() <= 0 || d.escorting);
   }

   /**
    * Hộ tống thất bại ({@code world/Escort}: bỏ quá xa, bị hạ gục, thoát game): lùi về bước trước (vd 9.4 -> 9.3, nói
    * chuyện lại để nhận lại sinh vật). Không còn ở bước hộ tống thì bỏ qua.
    */
   void escortFailed(PlayerSession session, String reason) {
      PlayerData d = session.getPlayer();
      if (d == null) {
         return;
      }
      PlayerData.MainQuestProgress q = progress(d);
      QuestStep step = currentStep(q);
      if (step == null || step.escortItem() <= 0) {
         return;
      }
      MainQuest quest = MainQuests.get(q.questId());
      int back = Math.max(0, q.stage() - 1);
      d.mainQuest = new PlayerData.MainQuestProgress(q.questId(), back, 0);
      d.questDialog = null;
      session.send(Packets.notice("Hộ tống thất bại! " + reason + " Hãy làm lại: " + quest.step(back).name() + "."));
      Log.debug("Nhiem vu: '%s' ho tong that bai, ve buoc %d.%d", d.name, q.questId(), back + 1);
      sendAll(session, d);
      enterStep(session, d, d.mainQuest);
   }

   /** Bấm "Nói chuyện": bước thường xong ngay; bước cuối nhận thưởng (nếu nhận lúc mở). Rồi hiện trang 1. */
   public void talk(NpcContext ctx) {
      PlayerData d = ctx.data();
      if (!canTalk(d, ctx.mapId(), ctx.template().id())) {
         return;
      }
      PlayerData.MainQuestProgress q = progress(d);
      MainQuest quest = MainQuests.get(q.questId());
      QuestStep step = quest.step(q.stage());
      if (quest.lastStage(q.stage())) {
         if (quest.rewardPage() < 0 && !finishQuest(ctx.session(), d, quest)) {
            return;
         }
      } else {
         advance(ctx.session(), d, q);
      }
      d.questDialog = new Dialog(q.questId(), q.stage());
      showPage(ctx, step, 0);
   }

   /** Nút ở trang hội thoại nhiệm vụ: "Tiếp tục" (nút 0, chưa phải trang cuối) sang trang kế; nút khác = đóng. */
   public void choose(NpcContext ctx, int page, int option) {
      PlayerData d = ctx.data();
      Dialog dialog = d.questDialog;
      MainQuest quest = dialog == null ? null : MainQuests.get(dialog.questId());
      QuestStep step = quest == null ? null : quest.step(dialog.stage());
      int index = page - DIALOG_PAGE;
      if (step == null || step.target() != ctx.template().id() || index < 0 || index >= step.dialog().size() - 1
            || option != 0) {
         d.questDialog = null;
         return;
      }
      if (quest.lastStage(dialog.stage()) && quest.rewardPage() == index) {
         PlayerData.MainQuestProgress q = progress(d);
         if (q.questId() != dialog.questId() || q.stage() != dialog.stage()
               || !finishQuest(ctx.session(), d, quest)) {
            d.questDialog = null;
            return;
         }
      }
      showPage(ctx, step, index + 1);
   }

   private static void showPage(NpcContext ctx, QuestStep step, int index) {
      String text = step.dialog().get(index);
      if (index == step.dialog().size() - 1) {
         ctx.say(DIALOG_PAGE + index, text, "Đóng");
      } else {
         ctx.say(DIALOG_PAGE + index, text, "Tiếp tục", step.skipLabel());
      }
   }

   // ================================================================== thưởng

   /**
    * Trả thưởng + sang nhiệm vụ kế. Đồ (khoá) vào túi trên bản sao — thiếu chỗ thì không nhận, vẫn ở bước cuối.
    * Item 281 "Kinh nghiệm" = cộng EXP. @return false nếu chưa nhận được
    */
   private boolean finishQuest(PlayerSession session, PlayerData d, MainQuest quest) {
      long now = System.currentTimeMillis();
      List<ItemData> shown = new ArrayList<>();
      List<ItemData> toBag = new ArrayList<>();
      long exp = 0;
      for (RewardItem reward : quest.rewards()) {
         ItemData item = reward.create(now);
         shown.add(item);
         if (item.templateId == MainQuests.EXP) {
            exp += item.quantity;
         } else {
            toBag.add(item);
         }
      }
      synchronized (d) {
         if (session.getPlayer() != d) {
            return false;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể nhận thưởng."));
            return false;
         }
         String error = ItemService.give(d, toBag, now); // bản sao túi: thiếu chỗ thì không đổi gì
         if (error != null) {
            session.send(Packets.notice(error));
            return false;
         }
         d.mainQuest = new PlayerData.MainQuestProgress(quest.id() + 1, 0, 0);
      }
      gainExp(session, d, exp);
      com.tlnt.server.player.ActivityPoints.add(session, d, com.tlnt.server.player.ActivityPoints.MAIN_QUEST); // hoàn thành 1 nhiệm vụ chính tuyến
      items.syncBagAndCurrency(session);
      session.send(QuestPackets.completeEffect());
      if (!shown.isEmpty()) {
         session.send(ItemPackets.rewardPopup(shown));
      }
      Log.debug("Nhiem vu: '%s' hoan thanh nhiem vu %d", d.name, quest.id());
      sendAll(session, d);
      enterStep(session, d, d.mainQuest);
      players.saveAsync(d, false); // thưởng + nhiệm vụ mới cùng 1 dòng player -> ghi ngay
      return true;
   }

   /** Cộng EXP thưởng (chữ "+N"); lên cấp -> hiệu ứng lên cấp + khối chỉ số cho cả khu. */
   private void gainExp(PlayerSession session, PlayerData d, long exp) {
      if (exp <= 0) {
         return;
      }
      int gained = Leveling.addExp(d, exp);
      session.send(ItemPackets.expGain(exp));
      if (gained > 0) {
         ItemService.refreshLook(d); // chỉ số gốc theo cấp
         world.broadcastAround(session, ItemPackets.levelUp(d.getId(), d.level, d.exp,
               PlayerStats.expToNextLevel(d.level)).toPacket());
         world.broadcastStats(d);
         com.tlnt.server.achievement.AchievementEvents.add(session,
               com.tlnt.server.achievement.AchievementType.LEVEL, d.level); // thành tựu "Đạt cấp độ N"
      }
   }

   // ================================================================== lệnh test

   /** {@code /nvinfo}: nhiệm vụ, bước, tiến độ, mục tiêu đang làm. */
   public void testInfo(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null) {
         return;
      }
      PlayerData.MainQuestProgress q = progress(d);
      MainQuest quest = MainQuests.get(q.questId());
      QuestStep step = quest.step(q.stage());
      session.send(Packets.notice("NV " + q.questId() + " \"" + quest.name() + "\" bước " + (q.stage() + 1) + "/"
            + quest.steps().size() + (step == null ? "" : ": " + step.name() + " [" + step.kind() + "] tiến độ "
            + q.progress() + "/" + step.count() + ", mục tiêu " + step.target() + ", map " + step.mapId())));
   }

   /**
    * {@code /nvc [n]}: cộng {@code amount} vào tiến độ bước đang làm (đủ thì sang bước kế). Bước nói chuyện không có tiến
    * độ — dùng {@link #testComplete}.
    */
   public void testAdd(PlayerSession session, int amount) {
      PlayerData d = session.getPlayer();
      if (d == null) {
         return;
      }
      PlayerData.MainQuestProgress q = progress(d);
      QuestStep step = currentStep(q);
      if (step == null || step.kind() == StepKind.TALK || step.kind() == StepKind.WAIT) {
         session.send(Packets.notice("Bước này không có tiến độ — dùng /nvxong."));
         return;
      }
      setProgress(session, d, q, step, q.progress() + amount);
      testInfo(session);
   }

   /** {@code /nvxong}: xong bước đang làm; bước cuối (nói chuyện) = nhận thưởng + sang nhiệm vụ kế. */
   public void testComplete(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null) {
         return;
      }
      PlayerData.MainQuestProgress q = progress(d);
      MainQuest quest = MainQuests.get(q.questId());
      QuestStep step = quest.step(q.stage());
      if (step == null || step.kind() == StepKind.WAIT) {
         session.send(Packets.notice("Nhiệm vụ này chưa có nội dung."));
         return;
      }
      d.questDialog = null;
      if (quest.lastStage(q.stage())) {
         finishQuest(session, d, quest);
      } else {
         advance(session, d, q);
      }
      testInfo(session);
   }

   /** {@code /nv <nhiệm vụ> [bước]}: nhảy tới nhiệm vụ / bước (bước tính từ 1). */
   public void jump(PlayerSession session, int questId, int stepNumber) {
      PlayerData d = session.getPlayer();
      if (d == null || questId < 1) {
         return;
      }
      MainQuest quest = MainQuests.get(questId);
      int stage = Math.max(0, Math.min(quest.steps().size() - 1, stepNumber - 1));
      d.mainQuest = new PlayerData.MainQuestProgress(questId, stage, 0);
      d.questDialog = null;
      sendAll(session, d);
      enterStep(session, d, d.mainQuest);
      session.send(Packets.notice("Nhiệm vụ " + questId + " bước " + (stage + 1) + ": " + quest.step(stage).name()));
   }
}
