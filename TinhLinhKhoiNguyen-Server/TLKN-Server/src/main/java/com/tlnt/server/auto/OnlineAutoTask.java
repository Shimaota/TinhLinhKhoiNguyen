package com.tlnt.server.auto;

import com.tlnt.server.handler.ReviveHandler;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.session.SessionManager;
import com.tlnt.server.thunder.ThunderRules;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.Arena;
import com.tlnt.server.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ô "Thiết lập tự động" (op 46) cho người chơi ĐANG ONLINE: tự dùng thuốc HP / MP / thể lực dưới ngưỡng và tự hồi sinh
 * bằng Cỏ hồi sinh. Client chỉ lưu + gửi các ô này (không tự dùng), game gốc để server làm. Treo offline
 * ({@code offline/OfflineTrainingService}) và bot ({@code bot/BotService}) có nhịp riêng nên bỏ qua ở đây.
 *
 * <p>Server không biết client có đang bật tự đánh (client không gửi) -> ô đã bật là luôn chạy.
 * Nhịp scheduler {@link #INTERVAL_MS}: chỉ ĐỌC trạng thái để lọc (không khoá, có thể cũ 1 nhịp), người cần làm gì thì xếp
 * 1 việc vào hàng đợi session — việc kiểm lại rồi dùng ô thuốc nhanh / hồi sinh như người chơi bấm (cùng code, cùng luật).
 */
public final class OnlineAutoTask implements Runnable {

   public static final long INTERVAL_MS = 500;
   /** Dùng mỗi loại thuốc tối đa 1 lần / bấy nhiêu ms (như treo offline). */
   static final long POTION_INTERVAL_MS = 1_000;
   /** Bị hạ gục bấy nhiêu ms mới tự hồi sinh (người chơi kịp thấy hộp Kiệt sức). */
   static final long REVIVE_DELAY_MS = 3_000;
   /** Ô dùng nhanh HUD (số client gửi ở 27/9): 3 = HP, 4 = MP, 1 = thể lực. */
   static final int QUICK_HP = 3;
   static final int QUICK_MP = 4;
   static final int QUICK_STAMINA = 1;

   /** Trạng thái tự động của 1 session — chỉ luồng session đụng (trừ {@link #queued}, {@link #deadSince}). */
   static final class State {
      final AtomicBoolean queued = new AtomicBoolean();
      long hpAt;
      long mpAt;
      long staminaAt;
      /** Lúc thấy bị hạ gục lần đầu (0 = đang sống). */
      volatile long deadSince;
      /** Đã thử hồi sinh cho lần bị hạ gục này (thiếu cỏ thì không thử / báo lại); sống lại thì nhịp sau xoá. */
      volatile boolean reviveTried;
   }

   private final SessionManager sessions;
   private final World world;
   private final ItemService items;
   private final Executor gamePool;
   private final Map<PlayerSession, State> states = new ConcurrentHashMap<>();

   public OnlineAutoTask(SessionManager sessions, World world, ItemService items, Executor gamePool) {
      this.sessions = sessions;
      this.world = world;
      this.items = items;
      this.gamePool = gamePool;
   }

   @Override
   public void run() {
      long now = System.currentTimeMillis();
      states.keySet().removeIf(s -> s.getPlayer() == null || s.isDetached());
      for (PlayerSession session : sessions.snapshot()) {
         PlayerData d = session.getPlayer();
         if (d == null || !d.inWorld || session.isDetached() || session.isOfflineTraining() || d.bot) {
            continue;
         }
         PlayerSettings s = d.settings;
         if (s == null || !anyEnabled(s)) {
            continue;
         }
         State st = states.computeIfAbsent(session, k -> new State());
         if (!d.isDead()) {
            st.deadSince = 0;
            st.reviveTried = false;
         } else if (st.deadSince == 0) {
            st.deadSince = now;
         }
         if (wants(d, s, st, now) && st.queued.compareAndSet(false, true)) {
            session.inbox().execute(() -> {
               try {
                  step(session, st);
               } catch (Exception ex) {
                  Log.error("Loi tu dong thuoc / hoi sinh session #" + session.getId(), ex);
               } finally {
                  st.queued.set(false);
               }
            }, gamePool);
         }
      }
   }

   static boolean anyEnabled(PlayerSettings s) {
      return s.autoUseHpItems || s.autoUseMpItems || s.autoUseStaminaItems || s.autoRevive;
   }

   /** Có việc cần làm không (lọc nhanh trên luồng scheduler; {@link #step} kiểm lại). */
   static boolean wants(PlayerData d, PlayerSettings s, State st, long now) {
      if (d.isDead()) {
         return reviveDue(d, s, st, now);
      }
      return needHp(d, s, st, now) || needMp(d, s, st, now) || needStamina(d, s, st, now);
   }

   static boolean reviveDue(PlayerData d, PlayerSettings s, State st, long now) {
      return s.autoRevive && !st.reviveTried && st.deadSince > 0 && now - st.deadSince >= REVIVE_DELAY_MS
            && !ThunderRules.isThunderMap(d.mapId) && !Arena.isArenaMap(d.mapId); // 2 nơi này tự hồi sinh theo luật riêng
   }

   static boolean needHp(PlayerData d, PlayerSettings s, State st, long now) {
      PlayerStats stats = d.stats;
      return s.autoUseHpItems && stats != null && stats.maxHp > 0 && d.hp * 100 < stats.maxHp * s.autoHpThresholdPercent
            && now - st.hpAt >= POTION_INTERVAL_MS;
   }

   static boolean needMp(PlayerData d, PlayerSettings s, State st, long now) {
      PlayerStats stats = d.stats;
      return s.autoUseMpItems && stats != null && stats.maxMp > 0 && d.mp * 100 < stats.maxMp * s.autoMpThresholdPercent
            && now - st.mpAt >= POTION_INTERVAL_MS;
   }

   static boolean needStamina(PlayerData d, PlayerSettings s, State st, long now) {
      return s.autoUseStaminaItems && d.activityPoints < PlayerData.STAMINA_PER_SKILL
            && now - st.staminaAt >= POTION_INTERVAL_MS;
   }

   /** Luồng session: kiểm lại rồi dùng ô thuốc nhanh / hồi sinh như người chơi bấm. */
   private void step(PlayerSession session, State st) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || session.isOfflineTraining()) {
         return;
      }
      PlayerSettings s = d.settings;
      if (s == null) {
         return;
      }
      long now = System.currentTimeMillis();
      if (d.isDead()) {
         if (reviveDue(d, s, st, now)) {
            st.reviveTried = true; // thiếu cỏ: báo 1 lần, chờ người chơi bấm "Về làng" / lần bị hạ gục sau
            if (ReviveHandler.reviveWithHerb(session, d, world, items)) {
               Log.debug("Tu hoi sinh: %s", d.name);
            }
         }
         return;
      }
      if (needHp(d, s, st, now)) {
         st.hpAt = now;
         items.useQuickSlot(session, QUICK_HP);
      }
      if (needMp(d, s, st, now)) {
         st.mpAt = now;
         items.useQuickSlot(session, QUICK_MP);
      }
      if (needStamina(d, s, st, now)) {
         st.staminaAt = now;
         items.useQuickSlot(session, QUICK_STAMINA);
      }
   }
}
