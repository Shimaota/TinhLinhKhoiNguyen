package com.tlnt.server.farm;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Nuôi heo ở Nông trại (map 5 — khu riêng từng người): dùng item Heo con (235) -> thả 1 con heo ngay chỗ đứng, lớn sau
 * {@link #GROW_MS} (16 giờ = {@code tlnt_data.animal_type} #1, client tự hiện "Heo con (xh ym)" / "Heo trưởng thành"),
 * nuôi tối đa {@link #MAX_PIGS} con. Bấm heo đã lớn -> thanh "Đang thu hoạch..." {@link #HARVEST_MS} -> 1 Thịt heo
 * (149, khoá), con heo biến mất (animation chết). Heo lưu trong cột JSON {@code player.farm} ({@link FarmState#pigs}).
 *
 * <pre>
 * C→S 52/0 int id con vật ({@code sendHarvestAction}) — bấm vào con vật
 * S→C 52/0 byte n × con vật; 52/1 1 con vật: int id, float x × 100... (client chia 100), int loại, int ms còn tới lúc lớn
 *     52/3 int id, int ms đã chạy, int tổng ms — thanh "Đang thu hoạch..."; 52/4 int id người, int id con vật — nhặt +
 *     con vật chết (client tự gỡ); 52/5 tắt thanh
 * </pre>
 * Luồng: thả / bấm trên luồng session; xong thanh thu hoạch = hẹn giờ scheduler -> lại hàng đợi session.
 */
public final class PigService {

   public static final int PIGLET_ITEM = 235;
   public static final int PORK_ITEM = 149;
   public static final int PIG_TYPE = 1;
   public static final long GROW_MS = 16 * 60 * 60_000L;
   public static final int MAX_PIGS = 5;
   public static final int HARVEST_MS = 1_500;
   /** "Lớn nhanh": giá lam thạch / con. */
   public static final long GROW_FAST_DIAMOND = 5;

   private static volatile PigService instance;

   private final ItemService items;
   private final PlayerService players;
   private final ScheduledExecutorService scheduler;
   private final Executor gamePool;

   public PigService(ItemService items, PlayerService players, ScheduledExecutorService scheduler, Executor gamePool) {
      this.items = items;
      this.players = players;
      this.scheduler = scheduler;
      this.gamePool = gamePool;
      instance = this;
   }

   public static PigService instance() {
      return instance;
   }

   // ================================================================== thả heo (dùng item 235)

   /**
    * Kiểm tra + thả 1 con heo ở chỗ đứng (luồng session, người gọi đã kiểm tra món là Heo con và sẽ trừ 1 món nếu trả
    * về null). @return câu báo lỗi, null = đã thả
    */
   public String release(PlayerSession session, PlayerData d) {
      Float x = d.x;
      Float y = d.y;
      if (d.mapId != AppleTreeService.FARM_MAP_ID || x == null || y == null) {
         return "Chỉ có thể thả động vật ở doanh trại";
      }
      FarmState farm = d.farm;
      List<FarmState.Pig> list = new ArrayList<>(farm.pigs());
      if (list.size() >= MAX_PIGS) {
         return "Chỉ nuôi được tối đa " + MAX_PIGS + " con heo.";
      }
      long now = System.currentTimeMillis();
      FarmState.Pig pig = new FarmState.Pig(farm.nextPigId++, PIG_TYPE, now, x, y);
      list.add(pig);
      farm.pigs = List.copyOf(list);
      session.send(addPacket(pig, now));
      Log.debug("'%s' tha heo #%d (%d / %d con)", d.name, pig.id(), list.size(), MAX_PIGS);
      return null;
   }

   // ================================================================== thu hoạch (C→S 52/0)

   public void harvest(PlayerSession session, int pigId) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || d.mapId != AppleTreeService.FARM_MAP_ID) {
         return;
      }
      long now = System.currentTimeMillis();
      FarmState.Pig pig = find(d.farm, pigId);
      if (pig == null) {
         return;
      }
      if (!pig.mature(now)) {
         session.send(Packets.notice("Heo của bạn chưa trưởng thành"));
         return;
      }
      if (now < d.pigHarvestUntil) {
         return; // đang thu hoạch con khác
      }
      d.pigHarvestUntil = now + HARVEST_MS;
      session.send(progressPacket(pigId, HARVEST_MS));
      scheduler.schedule(() -> session.inbox().execute(() -> finish(session, d, pigId), gamePool), HARVEST_MS,
            TimeUnit.MILLISECONDS);
   }

   /** Hết thanh thu hoạch (luồng session): còn ở nông trại + heo còn đó -> 1 Thịt heo, gỡ heo, lưu. */
   private void finish(PlayerSession session, PlayerData d, int pigId) {
      if (session.getPlayer() != d || !d.inWorld || d.mapId != AppleTreeService.FARM_MAP_ID) {
         return;
      }
      FarmState farm = d.farm;
      FarmState.Pig pig = find(farm, pigId);
      long now = System.currentTimeMillis();
      if (pig == null || !pig.mature(now)) {
         session.send(stopPacket());
         return;
      }
      ItemData pork = ItemData.of(PORK_ITEM, 1);
      pork.locked = true; // đồ nông trại luôn khoá (như táo)
      pork.createdTime = now;
      String error;
      synchronized (d) { // túi: cùng khoá với thư / kí gửi / shop
         error = d.trade != null ? "Đang giao dịch, không thể thu hoạch." : ItemService.give(d, List.of(pork), now);
         if (error == null) {
            List<FarmState.Pig> list = new ArrayList<>(farm.pigs());
            list.removeIf(p -> p.id() == pigId);
            farm.pigs = List.copyOf(list);
         }
      }
      if (error != null) {
         session.send(stopPacket());
         session.send(Packets.notice(error));
         return;
      }
      players.saveAsync(d, false);
      items.syncBagAndCurrency(session);
      session.send(pickPacket(d.getId(), pigId)); // nhặt + heo chết (client tự gỡ)
      ItemPackets.receivedNotices(List.of(pork)).forEach(session::send);
      com.tlnt.server.achievement.AchievementEvents.add(session,
            com.tlnt.server.achievement.AchievementType.PIG_HARVEST, 1); // thành tựu "Thu hoạch N Heo trưởng thành"
      com.tlnt.server.daily.DailyEvents.add(session, com.tlnt.server.daily.DailyKind.PIG, 0, 1); // NV hằng ngày
      com.tlnt.server.player.ActivityPoints.add(session, session.getPlayer(), com.tlnt.server.player.ActivityPoints.PIG_HARVEST);
      Log.debug("'%s' thu hoach heo #%d", d.name, pigId);
   }

   // ================================================================== lớn nhanh (C→S 52/1 + xác nhận op 32 loại 17)

   /** Bấm "Lớn nhanh" trên heo đang lớn: hỏi xác nhận. Luồng session. */
   public void askGrowFast(PlayerSession session, int pigId) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || d.mapId != AppleTreeService.FARM_MAP_ID) {
         return;
      }
      FarmState.Pig pig = find(d.farm, pigId);
      if (pig == null) {
         return;
      }
      if (pig.mature(System.currentTimeMillis())) {
         session.send(Packets.notice("Heo đã trưởng thành."));
         return;
      }
      d.pendingGrowPig = pigId;
      session.send(Packets.confirm(Packets.CONFIRM_PIG_GROW,
            "Bạn có muốn heo con lớn nhanh tốn " + GROW_FAST_DIAMOND + " lam thạch không?"));
   }

   /** "Đồng ý": trừ lam thạch, heo trưởng thành ngay (gửi lại con heo — client thay con cũ), lưu ngay. Luồng session. */
   public void confirmGrowFast(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || d.pendingGrowPig == 0) {
         return;
      }
      int pigId = d.pendingGrowPig;
      d.pendingGrowPig = 0;
      long now = System.currentTimeMillis();
      FarmState farm = d.farm;
      FarmState.Pig grown;
      synchronized (d) { // lam thạch: cùng khoá với shop / thư / kí gửi
         FarmState.Pig pig = find(farm, pigId);
         if (pig == null || pig.mature(now)) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể dùng lam thạch."));
            return;
         }
         if (d.diamond < GROW_FAST_DIAMOND) {
            session.send(Packets.notice("Không đủ " + GROW_FAST_DIAMOND + " lam thạch."));
            return;
         }
         d.diamond -= GROW_FAST_DIAMOND;
         grown = new FarmState.Pig(pig.id(), pig.type(), now - GROW_MS, pig.x(), pig.y());
         List<FarmState.Pig> list = new ArrayList<>(farm.pigs());
         list.replaceAll(p -> p.id() == pigId ? grown : p);
         farm.pigs = List.copyOf(list);
      }
      players.saveAsync(d, false);
      session.send(ItemPackets.currency(d));
      if (d.mapId == AppleTreeService.FARM_MAP_ID) {
         session.send(addPacket(grown, now)); // client mới: cùng id -> thay con cũ
      }
      session.send(Packets.notice("Heo con đã trưởng thành."));
      Log.debug("'%s' lon nhanh heo #%d (-%d lam thach)", d.name, pigId, GROW_FAST_DIAMOND);
   }

   private static FarmState.Pig find(FarmState farm, int id) {
      for (FarmState.Pig p : farm.pigs()) {
         if (p.id() == id) {
            return p;
         }
      }
      return null;
   }

   // ================================================================== gói S→C (op 52)

   /** Sub 0: mọi con heo (vào khu nông trại). null = không có con nào. */
   public static Message listPacket(FarmState farm, long now) {
      List<FarmState.Pig> list = farm.pigs();
      if (list.isEmpty()) {
         return null;
      }
      Message msg = new Message(FarmPackets.OPCODE);
      msg.writeByte(0);
      msg.writeByte(list.size());
      for (FarmState.Pig p : list) {
         writePig(msg, p, now);
      }
      return msg;
   }

   static Message addPacket(FarmState.Pig pig, long now) {
      Message msg = new Message(FarmPackets.OPCODE);
      msg.writeByte(1);
      writePig(msg, pig, now);
      return msg;
   }

   private static void writePig(Message msg, FarmState.Pig p, long now) {
      msg.writeInt(p.id());
      msg.writeFloat(p.x());
      msg.writeFloat(p.y());
      msg.writeInt(p.type());
      msg.writeInt(p.remainingMs(now));
   }

   static Message progressPacket(int pigId, int ms) {
      Message msg = new Message(FarmPackets.OPCODE);
      msg.writeByte(3);
      msg.writeInt(pigId);
      msg.writeInt(0);
      msg.writeInt(ms);
      return msg;
   }

   static Message pickPacket(int playerId, int pigId) {
      Message msg = new Message(FarmPackets.OPCODE);
      msg.writeByte(4);
      msg.writeInt(playerId);
      msg.writeInt(pigId);
      return msg;
   }

   static Message stopPacket() {
      Message msg = new Message(FarmPackets.OPCODE);
      msg.writeByte(5);
      return msg;
   }
}
