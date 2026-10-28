package com.tlnt.server.bot;

import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.WorldPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 1 người chơi giả đang online. Field thường chỉ luồng session của bot đụng tới ({@code BotService.step}); luồng
 * scheduler chỉ đọc các field volatile + cờ {@link #queued}.
 */
final class Bot {

   enum Kind {
      /** Treo như Luyện tập Offline: cắm 1 khúc bãi (Phạm vi đánh), chiêu mạnh trước, né quái người khác đang đánh. */
      GRIND_OFFLINE("offline", "Treo offline", true),
      /**
       * Như bật nút Tự đánh của client ({@code AutoTargetController}): giữ mục tiêu tới khi chết, chọn quái gần nhất mọi
       * loại hợp cấp, chiêu NGẪU NHIÊN trong các ô bật đang dùng được, thỉnh thoảng tự chạy sang khúc khác.
       */
      GRIND_AUTO("auto", "Auto đánh", true),
      /** Đứng cạnh NPC / điểm xuất hiện, thỉnh thoảng đi vài bước. */
      IDLE("idle", "Đứng chơi", false),
      /** Đi lang thang khắp map (rải rác). */
      WANDER("wander", "Đi dạo", false),
      /** Đứng im cả buổi (treo máy). */
      AFK("afk", "AFK", false),
      /** Cầm cần câu đứng mép nước, quăng / kéo cần bằng đúng FishingService. */
      FISH("fish", "Câu cá", false),
      /** Kéo tới boss thế giới đang sống, đánh tới khi boss còn 20% HP (để người thật kết liễu). */
      BOSS("boss", "Săn boss", true),
      /** Lữ khách: đi tới cửa dịch chuyển sang map khác (map đã mở theo cấp), đổi khu, dạo quanh rồi lại đi tiếp. */
      TRAVEL("travel", "Lữ khách", false),
      /** Đấu tập: 2 bot cùng chỗ thách đấu nhau (HP dừng ở 1 — không ai chết), nghỉ hồi máu rồi đấu lại. */
      SPAR("spar", "Đấu tập", true);

      final String key;
      final String label;
      final boolean fighter;

      Kind(String key, String label, boolean fighter) {
         this.key = key;
         this.label = label;
         this.fighter = fighter;
      }
   }

   final Kind kind;
   final PlayerSession session;
   final PlayerData data;
   final long bornAt;
   final int startLevel;
   volatile long logoutAt;
   /** Mô tả chỗ (admin panel): "Up Sói xám Lv12", "Cạnh Ông Morgi"... */
   final String place;
   /** Loại quái được đánh; rỗng = mọi loại. */
   final int[] templates;
   /** Bot up: cấp quái của bãi (lên quá cao so với bãi thì thoát, bot mới hợp cấp vào thay). */
   final int spotLevel;
   /** Gốc + bán kính đi lại (bot đứng / đi dạo). */
   final float homeX;
   final float homeSpread;
   final BotFactory.Build build;

   // ---- nhịp phản xạ riêng từng bot (không đồng loạt ra chiêu)
   int reactionMin = 250;
   int reactionMax = 600;
   /** {@code OfflineQuery.spread}: 0 không né, 1 hơi né, 2 né hẳn quái người khác đang đánh. */
   int spread = 2;
   /** Chiêu ngẫu nhiên trong các ô đang dùng được (auto đánh client) thay vì chiêu mạnh trước (treo offline). */
   boolean randomSkill;
   /** Lần sau được chạy 1 nhịp (scheduler đọc). */
   volatile long nextStepAt;

   final AtomicBoolean queued = new AtomicBoolean();
   volatile WorldPlayer world;
   /** Khu lúc vào game ({@code BotService.register}). */
   volatile int zoneId = -1;
   volatile boolean leaving;

   // ---- tổ đội: trưởng nhóm giữ danh sách thành viên bot đi cùng
   final List<Bot> followers = new ArrayList<>(0);
   volatile Bot leader;

   // ---- săn boss
   int bossId;
   /** Boss đang săn là người chơi giả (Tinh linh): đánh bằng World.botChaseWorldBoss thay vì offlineSeek tìm quái. */
   boolean charBoss;

   // ---- bị PK (luồng tick khu ghi qua listener -> volatile)
   volatile int hitBy;
   volatile long hitAt;
   int foe;
   boolean fight;
   int pkFights;

   int target = -1;
   boolean seeking;
   long seekAt;
   long hpPotionAt;
   long mpPotionAt;
   long staminaPotionAt;
   long regenAt;
   long reviveAt;
   long pauseUntil;
   long nextChatAt;
   long nextActAt;
   long nextSpendAt;
   long relocateAt;
   long hookPressAt;
   int casts;
   /** Bot câu: đã bước ra mép nước (quay mặt về phía nước). */
   boolean faced;
   int deaths;

   // ---- lữ khách: cửa đang đi tới (vị trí trong mảng cửa của map), hạn tới cửa
   int doorPos = -1;
   float doorX;
   float doorY;
   long doorDeadline;
   long doorWalkAt;
   int trips;

   // ---- đấu tập: bạn tập (cùng khu), người khởi xướng thách đấu
   volatile Bot rival;
   boolean challenger;
   boolean dueling;
   long nextDuelAt;
   int rounds;
   int wins;

   // ---- bot đứng chơi / đi dạo / lữ khách: chào + đi theo 1 người thật một lúc
   volatile int followId;
   volatile long followUntil;

   Bot(Kind kind, PlayerSession session, PlayerData data, long bornAt, long logoutAt, String place, int[] templates,
         int spotLevel, float homeX, float homeSpread, BotFactory.Build build) {
      this.kind = kind;
      this.session = session;
      this.data = data;
      this.bornAt = bornAt;
      this.startLevel = data.level;
      this.logoutAt = logoutAt;
      this.place = place;
      this.templates = templates;
      this.spotLevel = spotLevel;
      this.homeX = homeX;
      this.homeSpread = homeSpread;
      this.build = build;
   }
}
