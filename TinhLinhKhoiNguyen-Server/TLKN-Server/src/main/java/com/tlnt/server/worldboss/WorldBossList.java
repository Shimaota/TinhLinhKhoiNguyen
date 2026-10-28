package com.tlnt.server.worldboss;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.quest.MainQuests;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.QuestGuide;
import com.tlnt.server.quest.QuestPackets;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Bảng Boss thế giới (op 100 — MỚI, không có trong decompile; nút "Boss thế giới" dưới "Luyện tập Offline" ở cửa sổ Tiện
 * ích = Huy hiệu Tinh Linh, 45/1 -> S→C 100/2 mở bảng). Danh sách = {@link WorldBossService#entries()} (bỏ boss Lôi vực + boss
 * sự kiện đang tắt): hình (mẫu quái / nhân vật + skin), cấp, công, máu hiện tại, trạng thái + giây tới lần xuất hiện, map
 * đang đứng (KHÔNG gửi khu), các map có thể xuất hiện, quà ({@link WorldBossLoot}: số lượng khoảng + % rơi).
 *
 * <p>2 nút (người dùng 2026-10-18): "Dịch chuyển" {@link #TELEPORT_DIAMOND} lam thạch -> tới ĐÚNG khu có boss, cạnh boss
 * ({@code World.teleportToWorldBoss}, không đi được thì hoàn); "Tìm đường" {@link #PATH_GOLD} vàng (khoá trước) -> tự
 * chạy tới MAP có boss qua các cửa ({@link WorldBossPath}). Cả 2 hỏi xác nhận op 32 trước khi trừ tiền.
 *
 * <p>Luồng session (handler lane GAME); máu boss đọc trên luồng khu boss ({@code World.worldBossHp}) rồi quay về luồng
 * session để gửi.
 */
public final class WorldBossList {

   public static final long TELEPORT_DIAMOND = 5;
   public static final long PATH_GOLD = 100_000;
   /** Hạn "Tìm đường": quá bấy lâu chưa tới thì thôi dẫn. */
   static final long PATH_MS = 10 * 60_000L;
   /** Xin danh sách cách nhau tối thiểu (client hỏi lại mỗi 3 s khi đang mở bảng). */
   static final long LIST_GAP_MS = 1000;
   /** Chờ máu boss từ luồng khu tối đa bấy lâu (quá thì gửi máu tối đa). */
   static final long HP_TIMEOUT_MS = 500;

   static final byte STATE_WAITING = 0;
   static final byte STATE_ALIVE = 1;
   static final byte STATE_SPAWNING = 2;

   private static volatile WorldBossList instance;

   private final WorldBossService bosses;
   private final World world;
   private final PlayerService players;
   private final Executor gamePool;

   public WorldBossList(WorldBossService bosses, World world, PlayerService players, Executor gamePool) {
      this.bosses = bosses;
      this.world = world;
      this.players = players;
      this.gamePool = gamePool;
      instance = this;
   }

   /** Cho {@code ConfirmHandler} (op 32 loại 19 / 20). */
   public static WorldBossList instance() {
      return instance;
   }

   // ================================================================== danh sách (C→S 100/0)

   public void list(PlayerSession session) {
      PlayerData d = session.getPlayer();
      long now = System.currentTimeMillis();
      if (d == null || !d.inWorld || now - d.bossListAt < LIST_GAP_MS || !badge(session, d, now, false)) {
         return;
      }
      d.bossListAt = now;
      List<WorldBossService.Entry> entries = bosses.entries();
      List<CompletableFuture<long[]>> hps = new ArrayList<>(entries.size());
      for (WorldBossService.Entry e : entries) {
         hps.add(e.alive() ? world.worldBossHp(e.mapId(), e.zoneId(), e.def().id())
               .completeOnTimeout(null, HP_TIMEOUT_MS, TimeUnit.MILLISECONDS) : CompletableFuture.completedFuture(null));
      }
      CompletableFuture.allOf(hps.toArray(new CompletableFuture[0])).whenComplete((ok, error) -> {
         long[][] hp = new long[entries.size()][];
         for (int i = 0; i < hp.length; i++) {
            hp[i] = hps.get(i).getNow(null);
         }
         session.inbox().execute(() -> {
            if (session.getPlayer() == d) {
               session.send(listPacket(entries, hp, world::mapTemplate, System.currentTimeMillis()));
            }
         }, gamePool);
      });
   }

   /**
    * S→C 100/0: byte n, n × (int id, str tên, byte loại 0 quái / 1 người chơi giả, int mẫu mob_template / char_template,
    * byte n skin + n × str, short cấp, long công, long máu, long máu tối đa, byte trạng thái 0 chờ / 1 sống / 2 đang xuất
    * hiện, int giây tới lần xuất hiện, str map đang đứng (sống) / "", str các map có thể xuất hiện, int phút hồi sinh,
    * byte n quà + n × (item xem trước {@link ItemPackets#writePreview}, int số lượng min, int max, float % rơi)).
    * {@code hp[i]} = {máu, máu tối đa} của boss sống (null = không đọc được -> máu tối đa).
    */
   static Message listPacket(List<WorldBossService.Entry> entries, long[][] hp,
         java.util.function.IntFunction<MapTemplate> maps, long now) {
      Message msg = new Message(Opcode.WORLD_BOSS);
      msg.writeByte(0);
      msg.writeByte(entries.size());
      for (int i = 0; i < entries.size(); i++) {
         WorldBossService.Entry e = entries.get(i);
         WorldBossDef def = e.def();
         msg.writeInt(def.id());
         msg.writeString(def.name());
         boolean ch = def.charSpec() != null;
         msg.writeByte(ch ? 1 : 0);
         msg.writeInt(ch ? def.charSpec().charTemplateId() : def.mobTemplateId());
         String[] skins = ch ? def.charSpec().skins() : new String[0];
         msg.writeByte(skins.length);
         for (String skin : skins) {
            msg.writeString(skin);
         }
         msg.writeShort(def.level());
         msg.writeLong(def.attack());
         long[] h = hp[i];
         msg.writeLong(e.alive() && h != null ? h[0] : def.hp());
         msg.writeLong(e.alive() && h != null ? h[1] : def.hp());
         byte state = e.alive() ? STATE_ALIVE : e.spawning() ? STATE_SPAWNING : STATE_WAITING;
         msg.writeByte(state);
         msg.writeInt(state == STATE_WAITING ? (int) Math.max(0, (e.nextSpawnAt() - now + 999) / 1000) : 0);
         msg.writeString(e.alive() ? e.mapName() : "");
         StringBuilder names = new StringBuilder();
         for (int mapId : def.maps()) {
            MapTemplate m = maps.apply(mapId);
            if (m != null) {
               names.append(names.length() == 0 ? "" : ", ").append(m.name());
            }
         }
         msg.writeString(names.toString());
         msg.writeInt((int) (def.respawnMs() / 60_000));
         List<WorldBossLoot> loot = def.loot().size() > 40 ? def.loot().subList(0, 40) : def.loot();
         msg.writeByte(loot.size());
         for (WorldBossLoot l : loot) {
            ItemPackets.writePreview(msg, l.item(), now);
            msg.writeInt(l.minPiles() * l.minQty());
            msg.writeInt(l.piles() * l.maxQty());
            msg.writeFloat((float) Math.min(100, Math.max(0, l.chance())));
         }
      }
      return msg;
   }

   /** S→C 100/2: mở cửa sổ Boss thế giới (nút "Boss thế giới" ở cửa sổ Tiện ích — {@code badge/SpiritBadgeService}). */
   public static Message openPacket() {
      Message msg = new Message(Opcode.WORLD_BOSS);
      msg.writeByte(2);
      return msg;
   }

   /** S→C 100/1: bật "Tự tìm đường" theo mũi tên op 63 vừa gửi (không payload). */
   static Message autoPathPacket() {
      Message msg = new Message(Opcode.WORLD_BOSS);
      msg.writeByte(1);
      return msg;
   }

   // ================================================================== Dịch chuyển (C→S 100/1)

   public void requestTeleport(PlayerSession session, int id) {
      PlayerData d = session.getPlayer();
      WorldBossService.Entry e = alive(session, d, id);
      if (e == null) {
         return;
      }
      if (d.diamond < TELEPORT_DIAMOND) {
         session.send(Packets.notice("Không đủ " + TELEPORT_DIAMOND + " lam thạch."));
         return;
      }
      d.pendingBossTeleport = id;
      d.pendingBossPath = 0;
      session.send(Packets.confirm(Packets.CONFIRM_BOSS_TELEPORT, "Dịch chuyển tới " + e.def().name() + " tại "
            + e.mapName() + " tốn " + TELEPORT_DIAMOND + " lam thạch?"));
   }

   /** "Đồng ý" (op 32 loại 19): trừ lam thạch rồi dịch chuyển; khu từ chối thì hoàn. Luồng session. */
   public void confirmTeleport(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || d.pendingBossTeleport == 0) {
         return;
      }
      int id = d.pendingBossTeleport;
      d.pendingBossTeleport = 0;
      WorldBossService.Entry e = alive(session, d, id);
      if (e == null) {
         return;
      }
      synchronized (d) { // lam thạch: cùng khoá với shop / thư / kí gửi
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể dùng lam thạch."));
            return;
         }
         if (d.diamond < TELEPORT_DIAMOND) {
            session.send(Packets.notice("Không đủ " + TELEPORT_DIAMOND + " lam thạch."));
            return;
         }
         d.diamond -= TELEPORT_DIAMOND;
      }
      session.send(ItemPackets.currency(d));
      players.saveAsync(d, false);
      Log.debug("'%s' dich chuyen toi boss #%d (-%d lam thach)", Log.ascii(d.name), id, TELEPORT_DIAMOND);
      world.teleportToWorldBoss(session, e.mapId(), e.zoneId(), id,
            () -> session.inbox().execute(() -> refundDiamond(session, d), gamePool));
   }

   private void refundDiamond(PlayerSession session, PlayerData d) {
      if (session.getPlayer() != d) {
         return;
      }
      synchronized (d) {
         d.diamond += TELEPORT_DIAMOND;
      }
      session.send(ItemPackets.currency(d));
      players.saveAsync(d, false);
   }

   // ================================================================== Tìm đường (C→S 100/2)

   public void requestPath(PlayerSession session, int id) {
      PlayerData d = session.getPlayer();
      WorldBossService.Entry e = alive(session, d, id);
      if (e == null || pathError(session, d, e) == null) {
         return;
      }
      if (d.gold + d.lockedGold < PATH_GOLD) {
         session.send(Packets.notice("Không đủ " + money(PATH_GOLD) + " vàng."));
         return;
      }
      d.pendingBossPath = id;
      d.pendingBossTeleport = 0;
      session.send(Packets.confirm(Packets.CONFIRM_BOSS_PATH, "Tự tìm đường tới " + e.mapName() + " (nơi có "
            + e.def().name() + ") tốn " + money(PATH_GOLD) + " vàng?"));
   }

   /** "Đồng ý" (op 32 loại 20): trừ vàng (khoá trước), mũi tên + tự tìm đường tới cửa đầu tiên. Luồng session. */
   public void confirmPath(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || d.pendingBossPath == 0) {
         return;
      }
      int id = d.pendingBossPath;
      d.pendingBossPath = 0;
      WorldBossService.Entry e = alive(session, d, id);
      MapTemplate.Waypoint door = e == null ? null : pathError(session, d, e);
      if (door == null) {
         return;
      }
      synchronized (d) {
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể dùng vàng."));
            return;
         }
         if (d.gold + d.lockedGold < PATH_GOLD) {
            session.send(Packets.notice("Không đủ " + money(PATH_GOLD) + " vàng."));
            return;
         }
         long fromLocked = Math.min(d.lockedGold, PATH_GOLD);
         d.lockedGold -= fromLocked;
         d.gold -= PATH_GOLD - fromLocked;
      }
      session.send(ItemPackets.currency(d));
      d.bossPathUntil = System.currentTimeMillis() + PATH_MS;
      d.bossPathMap = e.mapId();
      session.send(QuestPackets.arrow(door.x(), door.y(), door.w(), door.h()));
      session.send(autoPathPacket());
      session.send(Packets.notice("Đang tự tìm đường tới " + e.mapName() + "."));
      Log.debug("'%s' tim duong toi boss #%d map %d", Log.ascii(d.name), id, e.mapId());
   }

   /** Cửa đầu tiên trên đường tới map boss; null = không đi được (đã báo lý do). */
   private MapTemplate.Waypoint pathError(PlayerSession session, PlayerData d, WorldBossService.Entry e) {
      if (d.mapId == e.mapId()) {
         session.send(Packets.notice("Bạn đang ở " + e.mapName() + "."));
         return null;
      }
      if (world.adminTeleportBlocked(d.mapId)) {
         session.send(Packets.notice("Không thể tìm đường khi đang ở phó bản, chiến trường hoặc đài đấu."));
         return null;
      }
      if (!QuestEvents.canEnter(d, e.mapId())) {
         session.send(Packets.notice(MainQuests.MAP_LOCKED));
         return null;
      }
      MapTemplate.Waypoint door = QuestGuide.firstDoor(d.mapId, e.mapId(), world::mapTemplate);
      if (door == null) {
         session.send(Packets.notice("Không tìm được đường tới " + e.mapName() + "."));
      }
      return door;
   }

   // ================================================================== chung

   /** Boss {@code id} đang sống + người chơi có Huy hiệu; null = không (đã báo lý do). */
   private WorldBossService.Entry alive(PlayerSession session, PlayerData d, int id) {
      if (d == null || !d.inWorld || !badge(session, d, System.currentTimeMillis(), true)) {
         return null;
      }
      WorldBossService.Entry e = bosses.entry(id);
      if (e == null || !e.alive()) {
         session.send(Packets.notice(e == null ? "Không có boss này." : "Boss chưa xuất hiện."));
         return null;
      }
      if (d.isDead()) {
         session.send(Packets.notice("Đã bị hạ gục."));
         return null;
      }
      return e;
   }

   private static boolean badge(PlayerSession session, PlayerData d, long now, boolean tell) {
      if (d.spiritBadgeUntil > now) {
         return true;
      }
      if (tell) {
         session.send(Packets.notice("Cần Huy hiệu Tinh Linh còn hạn."));
      }
      return false;
   }

   private static String money(long v) {
      return String.format(java.util.Locale.ROOT, "%,d", v).replace(',', '.');
   }
}
