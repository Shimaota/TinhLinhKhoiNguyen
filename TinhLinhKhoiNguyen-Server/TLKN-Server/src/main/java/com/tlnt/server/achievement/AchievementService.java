package com.tlnt.server.achievement;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Thành tựu (op 62; cửa sổ Nhiệm vụ ngày tab "Thành tựu" {@code AchievementPanel}). Danh sách + quà = bảng
 * {@code tlnt_data.achievement} (dump từ game gốc), tiến độ = cột JSON {@code player.achievement} ({@link AchievementState}).
 *
 * <pre>
 * S→C 62/0 xoá danh sách; 62/1 byte n × (int id, str tiêu đề, str mô tả, int mục tiêu, byte m × item quà, int tiến độ,
 *         byte trạng thái 0 đang làm / 1 nhận được / 2 đã nhận) — client CỘNG THÊM vào danh sách; 62/2 int id, int tiến
 *         độ, byte trạng thái (cập nhật 1 mục)
 * C→S 62/0 int id = "Nhận" ({@code sendMountEquip} — tên sai)
 * </pre>
 * Luồng: mọi thay đổi tiến độ chạy trên luồng session của chính người đó ({@link AchievementEvents} chuyển về), nhận quà
 * cũng vậy -> không cần khoá riêng; cho đồ dưới {@code synchronized (player)} trên bản sao túi (thiếu chỗ thì không nhận).
 */
public final class AchievementService {

   /** Số mục tối đa 1 gói 62/1 (client đọc số lượng bằng 1 byte). */
   private static final int PER_PACKET = 100;
   private static final long CLAIM_COOLDOWN_MS = 300;

   private static volatile List<Achievement> all = List.of();

   private final AchievementRepository repository;
   private final PlayerService players;
   private final ItemService items;
   final Executor gamePool;
   /** {@code world_boss.id} tính là Vệ thần / Tinh linh (khoá achievement.guardianBosses / spiritBosses). */
   final Set<Integer> guardianBosses;
   final Set<Integer> spiritBosses;

   public AchievementService(AchievementRepository repository, PlayerService players, ItemService items,
         Executor gamePool, Set<Integer> guardianBosses, Set<Integer> spiritBosses) {
      this.repository = repository;
      this.players = players;
      this.items = items;
      this.gamePool = gamePool;
      this.guardianBosses = Set.copyOf(guardianBosses);
      this.spiritBosses = Set.copyOf(spiritBosses);
      reload();
      AchievementEvents.install(this);
      Log.info("Thanh tuu: %d muc", all.size());
   }

   /** Đọc lại bảng (luồng blocking). Lỗi -> giữ bản cũ. Người đang online thấy bản mới ở lần đăng nhập sau. */
   public void reload() {
      List<Achievement> loaded = repository.loadAll();
      if (loaded != null) {
         all = List.copyOf(loaded);
      }
   }

   static List<Achievement> all() {
      return all;
   }

   // ================================================================== vào game / gửi danh sách

   /**
    * Hook vào game: bù cấp hiện có (nhân vật có từ trước khi có thành tựu) rồi gửi danh sách. Số sao KHÔNG bù từ đồ đang
    * có — chỉ tính lúc nâng cấp thành công ({@code forge/UpgradeService}).
    */
   public void onLogin(PlayerSession session, PlayerData d) {
      d.achievements = d.achievements == null ? new AchievementState() : d.achievements.normalize();
      d.achievements.apply(AchievementType.LEVEL, d.level);
      sendAll(session, d);
   }

   void sendAll(PlayerSession session, PlayerData d) {
      Message clear = new Message(Opcode.ACHIEVEMENT);
      clear.writeByte(0);
      session.send(clear);
      List<Achievement> list = all;
      long now = System.currentTimeMillis();
      for (int from = 0; from < list.size(); from += PER_PACKET) {
         List<Achievement> part = list.subList(from, Math.min(list.size(), from + PER_PACKET));
         Message msg = new Message(Opcode.ACHIEVEMENT);
         msg.writeByte(1);
         msg.writeByte(part.size());
         for (Achievement a : part) {
            long count = d.achievements.get(a.type());
            msg.writeInt(a.id());
            msg.writeString(a.title());
            msg.writeString(a.description());
            msg.writeInt(a.target());
            msg.writeByte(a.rewards().size());
            for (RewardItem r : a.rewards()) {
               ItemPackets.writePreview(msg, r, now);
            }
            msg.writeInt(a.progress(count));
            msg.writeByte(a.status(count, d.achievements.isClaimed(a.id())));
         }
         session.send(msg);
      }
   }

   private static Message update(Achievement a, PlayerData d) {
      long count = d.achievements.get(a.type());
      Message msg = new Message(Opcode.ACHIEVEMENT);
      msg.writeByte(2);
      msg.writeInt(a.id());
      msg.writeInt(a.progress(count));
      msg.writeByte(a.status(count, d.achievements.isClaimed(a.id())));
      return msg;
   }

   // ================================================================== tiến độ (luồng session)

   /** Cộng / nâng bộ đếm {@code type} rồi gửi 62/2 cho mọi mục của chuỗi có tiến độ hiện ra đổi. */
   void onEvent(PlayerSession session, AchievementType type, long amount) {
      PlayerData d = session.getPlayer();
      if (d == null || d.achievements == null) {
         return;
      }
      long before = d.achievements.get(type);
      if (!d.achievements.apply(type, amount)) {
         return;
      }
      long after = d.achievements.get(type);
      for (Achievement a : all) {
         if (a.type() == type && !d.achievements.isClaimed(a.id()) && a.progress(before) != a.progress(after)) {
            session.send(update(a, d));
            if (a.progress(before) < a.target() && a.progress(after) >= a.target()) {
               session.send(Packets.notice("Hoàn thành thành tựu " + a.title() + "! Hãy vào mục Thành tựu nhận thưởng."));
            }
         }
      }
   }

   // ================================================================== nhận quà (C→S 62/0)

   public void claim(PlayerSession session, int id) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || d.achievements == null) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastAchievementClaimAt < CLAIM_COOLDOWN_MS) {
         return;
      }
      d.lastAchievementClaimAt = now;
      Achievement a = find(id);
      if (a == null) {
         return;
      }
      if (a.status(d.achievements.get(a.type()), d.achievements.isClaimed(id)) != Achievement.CLAIMABLE) {
         session.send(update(a, d)); // client bấm nhầm / lệch -> gửi lại trạng thái thật
         return;
      }
      List<ItemData> gifts = new ArrayList<>();
      for (RewardItem r : a.rewards()) {
         gifts.add(r.create(now));
      }
      synchronized (d) {
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể nhận thưởng."));
            return;
         }
         String error = ItemService.give(d, gifts, now);
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
         d.achievements.claimed.add(id);
      }
      players.saveAsync(d, false); // đồ vừa vào túi + cờ "đã nhận" ghi cùng 1 dòng
      items.syncBagAndCurrency(session);
      session.send(update(a, d));
      if (!gifts.isEmpty()) {
         session.send(ItemPackets.rewardPopup(gifts));
      }
      com.tlnt.server.player.ActivityPoints.add(session, d, com.tlnt.server.player.ActivityPoints.ACHIEVEMENT); // hoàn thành (nhận) 1 thành tựu
      Log.debug("Thanh tuu #%d: '%s' nhan thuong", id, d.name);
   }

   private static Achievement find(int id) {
      for (Achievement a : all) {
         if (a.id() == id) {
            return a;
         }
      }
      return null;
   }

   // ================================================================== lệnh test

   private static final String USAGE = "Lệnh: /tt [info] | <loại> N (cộng N vào chuỗi, vd /tt revive 10, /tt level 30)"
         + " | reset (xoá tiến độ + đã nhận) | gui (gửi lại danh sách). Loại: elite leader guardian spirit revive"
         + " market_sell brew activity_chest upgrade_star refine_stone fish beehive pig_harvest level";

   /** Lệnh chat {@code /tt ...} ({@code chat/ChatService}, luồng session). */
   public static void testCommand(PlayerSession session, PlayerData d, String[] args) {
      AchievementService s = AchievementEvents.service();
      if (s == null || d.achievements == null) {
         session.send(Packets.notice("Chưa bật thành tựu."));
         return;
      }
      String sub = args.length > 1 ? args[1] : "info";
      long n = 1;
      if (args.length > 2) {
         try {
            n = Long.parseLong(args[2]);
         } catch (NumberFormatException e) {
            n = 1;
         }
      }
      AchievementType type = AchievementType.of(sub);
      if (type != null) {
         s.onEvent(session, type, n);
         session.send(Packets.notice(type.key + " = " + d.achievements.get(type)));
         return;
      }
      switch (sub) {
         case "reset" -> {
            d.achievements = new AchievementState();
            d.achievements.apply(AchievementType.LEVEL, d.level);
            s.sendAll(session, d);
            session.send(Packets.notice("Đã xoá tiến độ thành tựu."));
         }
         case "gui" -> s.sendAll(session, d);
         case "info" -> session.send(Packets.notice("Thành tựu: " + d.achievements.counts + ", đã nhận "
               + d.achievements.claimed + ". " + USAGE));
         default -> session.send(Packets.notice(USAGE));
      }
   }
}
