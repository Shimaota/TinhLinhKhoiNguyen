package com.tlnt.server.fishing;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.item.Appearance;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Câu cá (op 57). Lane GAME: mọi bước chạy trên hàng đợi session của người câu (túi đồ chỉ luồng session sửa).
 *
 * <h2>Luồng</h2>
 * <ol>
 *   <li>Mặc Cần câu ({@link FishingConfig#rodItem}) vào ô vũ khí -> server gửi op 2 sub 4 = chế độ HUD câu cá: client
 *       hiện nút tròn "nắm tay", ẩn thanh kỹ năng ({@code ItemService.sync}).</li>
 *   <li>Bấm nút (C→S op 57 sub 0, {@code sendRequestHarvest} — tên sai) khi đang rảnh -> quăng cần: phải cầm cần câu,
 *       đứng gần vùng nước của map (cột {@code map_template.waters}), còn sống, không khoá nhân vật, không giao dịch,
 *       có Mồi câu. TỐN 1 MỒI ngay lúc quăng (câu được / hụt / sổng đều mất). Phát op 57/0 cho cả khu.</li>
 *   <li>Sau {@link #BITE_MIN_MS}..{@link #BITE_MAX_MS} cá cắn -> gửi thanh kéo cần (op 57/2) cho riêng người câu.
 *       Bấm nút TRƯỚC lúc đó = giật sớm: mất mồi, thôi câu.</li>
 *   <li>Bấm lúc kim ở vùng VÀNG hoặc XANH -> kéo được, quay bảng {@link FishingConfig#loot} theo vùng (xanh hẹp hơn
 *       -> tỉ lệ đồ giá trị cao hơn); ngoài 2 vùng = câu hụt. Không bấm trong {@link #HOOK_WINDOW_MS} = tự huỷ, cá
 *       sổng, mất mồi. Kéo được -> món KHOÁ vào túi, phát op 57/1 (icon bay trên đầu). Túi đầy = cá sổng.</li>
 *   <li>Đi khỏi chỗ quăng cần, đổi khu / map, bị hạ gục, khoá nhân vật, tháo cần: thôi câu (op 57/3), mất mồi.</li>
 * </ol>
 * Đang câu ({@link PlayerData#fishing} khác null): chặn giao dịch, bán / dùng / mặc / vứt vật phẩm, gửi / nhận thư,
 * kí gửi, mua shop.
 *
 * <p>Hẹn giờ: luồng scheduler chỉ xếp {@link #tick} vào hàng đợi session mỗi {@link #TICK_MS} (chỉ người đang câu).
 */
public final class FishingService {

   /** Đứng cách vùng nước tối đa bấy nhiêu pixel (mỗi phía của hộp) mới quăng cần được. */
   static final float WATER_REACH_PX = 200f;
   /** Đi xa chỗ quăng cần hơn bấy nhiêu pixel = thôi câu. */
   static final float MOVE_TOLERANCE_PX = 40f;
   /** Cá cắn câu sau khoảng (ms) random này kể từ lúc quăng. */
   static final int BITE_MIN_MS = 3_000;
   static final int BITE_MAX_MS = 9_000;
   /** Thời gian được bấm kéo cần kể từ lúc hiện thanh. */
   static final long HOOK_WINDOW_MS = 5_000;
   /** Bấm lại trong khoảng này sau khi quăng thì bỏ qua (bấm 2 lần liền / chờ anim quăng), không tính giật sớm. */
   static final long CAST_GRACE_MS = 1_200;
   /** Nghỉ tối thiểu giữa 2 lần quăng cần. */
   static final long CAST_COOLDOWN_MS = 1_000;
   /** Bù ping: kim ở vùng tốt trong khoảng này trước lúc gói bấm tới server vẫn tính. */
   static final long LAG_TOLERANCE_MS = 250;
   /** Ms mỗi lượt kim chạy hết thanh (random trong khoảng). */
   static final int MIN_PERIOD_MS = 1_300;
   static final int MAX_PERIOD_MS = 2_000;
   /** Bề rộng vùng vàng / xanh (tỉ lệ bề rộng thanh, random trong khoảng). */
   static final float YELLOW_MIN = 0.20f;
   static final float YELLOW_MAX = 0.30f;
   static final float GREEN_MIN = 0.06f;
   static final float GREEN_MAX = 0.10f;
   static final long TICK_MS = 300;

   private static volatile FishingConfig config = FishingConfig.DEFAULT;

   private final FishingRepository repository;
   private final World world;
   private final ItemService items;
   private final ScheduledExecutorService scheduler;
   private final Executor gamePool;

   public FishingService(FishingRepository repository, World world, ItemService items,
         ScheduledExecutorService scheduler, Executor gamePool) {
      this.repository = repository;
      this.world = world;
      this.items = items;
      this.scheduler = scheduler;
      this.gamePool = gamePool;
      reload();
   }

   /** Đọc lại bảng {@code fishing} (luồng blocking). Lỗi thì giữ bản cũ. */
   public void reload() {
      FishingConfig loaded = repository.load();
      if (loaded != null) {
         config = loaded;
      }
   }

   /** Cấu hình đang dùng (cần câu, mồi, bảng cá). */
   public static FishingConfig config() {
      return config;
   }

   /** Tầm từ chỗ đứng tới mặt nước được câu (px). */
   public static float waterReach() {
      return WATER_REACH_PX;
   }

   /** Món ở ô vũ khí có phải cần câu không (quyết định chế độ HUD câu cá). */
   public static boolean isRod(ItemData weapon) {
      return weapon != null && weapon.templateId == config.rodItem();
   }

   /** Đang cầm cần câu. */
   public static boolean holdingRod(PlayerData d) {
      return isRod(d.body.get(Appearance.SLOT_WEAPON));
   }

   // ================================================================== op 57 sub 0: bấm nút câu

   /** Rảnh -> quăng cần; đang chờ cá -> giật sớm; cá đã cắn -> kéo cần. */
   public void press(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      Fishing f = d.fishing;
      if (f != null && problem(session, d, f) != null) {
         stop(session, d, null); // đã rời chỗ câu -> bỏ lần cũ (mồi đã mất), bấm lần này = quăng cần mới
         f = null;
      }
      if (f == null) {
         cast(session, d, now);
      } else if (f.phase == Fishing.Phase.WAITING) {
         if (now - f.castAt >= CAST_GRACE_MS) {
            stop(session, d, "Cá chưa cắn câu, bạn giật cần quá sớm! Mất 1 mồi câu.");
         }
      } else {
         pull(session, d, f, now);
      }
   }

   private void cast(PlayerSession session, PlayerData d, long now) {
      WorldPlayer p = world.online(session);
      if (p == null || p.zone() == null || now - d.lastFishAt < CAST_COOLDOWN_MS) {
         return;
      }
      FishingConfig cfg = config;
      String error = d.isDead() ? "Không thể câu cá khi đã bị hạ gục."
            : p.locked() ? "Hãy tắt khoá nhân vật để câu cá."
            : d.trade != null ? "Đang giao dịch, không thể câu cá."
            : !holdingRod(d) ? "Hãy trang bị " + itemName(cfg.rodItem()) + " để câu cá."
            : !p.zone().map().template().nearWater(p.x(), p.y(), WATER_REACH_PX) ? "Hãy đứng gần mặt nước để câu cá."
            : ItemService.count(d.bag, cfg.baitItem(), now) <= 0 ? "Bạn không có " + itemName(cfg.baitItem()) + "."
            : null;
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      ItemService.remove(d.bag, cfg.baitItem(), 1, now); // mồi mất ngay lúc quăng
      items.syncBagAndCurrency(session);
      long biteAt = now + ThreadLocalRandom.current().nextInt(BITE_MIN_MS, BITE_MAX_MS + 1);
      Fishing f = new Fishing(p.zone(), p.x(), p.y(), now, biteAt);
      d.fishing = f;
      d.lastFishAt = now;
      world.broadcastAround(session, FishingPackets.cast(d.getId()));
      scheduleTick(session, f);
      Log.debug("'%s' quang can cau o map %d", d.name, p.zone().map().template().id());
   }

   /** Cá đã cắn, người chơi bấm kéo cần. */
   private void pull(PlayerSession session, PlayerData d, Fishing f, long now) {
      Fishing.Hit hit = f.judge(now, LAG_TOLERANCE_MS);
      if (hit == Fishing.Hit.MISS) {
         if (ThreadLocalRandom.current().nextInt(100) < JUNK_CHANCE_PERCENT && giveJunk(session, d, now)) {
            return; // câu xịt nhưng kéo lên được rác (Giày da cũ)
         }
         stop(session, d, "Câu hụt! Cá đã sổng mất, mất 1 mồi câu.");
         return;
      }
      RewardItem loot = config.roll(hit == Fishing.Hit.GREEN);
      if (loot == null) {
         stop(session, d, "Cá đã sổng mất! Mất 1 mồi câu.");
         return;
      }
      ItemData item = loot.create(now);
      String error = ItemService.give(d, List.of(item), now);
      if (error != null) {
         stop(session, d, "Hành trang đã đầy, cá đã sổng mất!");
         return;
      }
      d.fishing = null;
      d.lastFishAt = now;
      world.broadcastAround(session, FishingPackets.caught(d.getId(), item.templateId));
      items.syncBagAndCurrency(session);
      session.send(Packets.notice((hit == Fishing.Hit.GREEN ? "Kéo cần hoàn hảo! " : "") + "Bạn câu được "
            + com.tlnt.server.item.ItemPackets.receivedName(item) + "."));
      Log.debug("'%s' cau duoc item %d x%d (%s)", d.name, item.templateId, item.quantity, hit);
      com.tlnt.server.achievement.AchievementEvents.add(session, com.tlnt.server.achievement.AchievementType.FISH, 1); // thành tựu: câu được cá (câu hụt kéo rác không tính)
      com.tlnt.server.player.ActivityPoints.add(session, d, com.tlnt.server.player.ActivityPoints.FISH);
   }

   /** Câu xịt (kéo ngoài vùng vàng / xanh): {@value #JUNK_CHANCE_PERCENT}% kéo lên 1 món rác {@link #JUNK_ITEM}. */
   static final int JUNK_ITEM = 265; // Giày da cũ
   static final int JUNK_CHANCE_PERCENT = 20;

   /** @return false = túi đầy (không nhận được, coi như câu hụt bình thường) */
   private boolean giveJunk(PlayerSession session, PlayerData d, long now) {
      ItemData item = new RewardItem(JUNK_ITEM, 1, true, new int[0][], 0).create(now); // khoá như mọi món câu được
      if (ItemService.give(d, List.of(item), now) != null) {
         return false;
      }
      d.fishing = null;
      d.lastFishAt = now;
      world.broadcastAround(session, FishingPackets.caught(d.getId(), item.templateId));
      items.syncBagAndCurrency(session);
      session.send(Packets.notice("Câu hụt! Bạn kéo lên được " + itemName(item.templateId) + "."));
      return true;
   }

   // ================================================================== hẹn giờ

   private void scheduleTick(PlayerSession session, Fishing f) {
      scheduler.schedule(() -> session.inbox().execute(() -> tick(session, f), gamePool), TICK_MS,
            TimeUnit.MILLISECONDS);
   }

   /** Mỗi {@link #TICK_MS} trên luồng session: kiểm tra còn câu được không, tới giờ cá cắn / cá sổng. */
   private void tick(PlayerSession session, Fishing f) {
      PlayerData d = session.getPlayer();
      if (d == null || d.fishing != f) {
         return; // đã xong / thôi / thoát game: dừng hẹn giờ
      }
      String problem = problem(session, d, f);
      if (problem != null) {
         stop(session, d, problem);
         return;
      }
      long now = System.currentTimeMillis();
      if (f.phase == Fishing.Phase.WAITING && now >= f.biteAt) {
         f.hook(now);
         session.send(FishingPackets.bar(f));
      } else if (f.phase == Fishing.Phase.HOOKED && now >= f.hookDeadline) {
         stop(session, d, "Quá lâu không kéo cần, cá đã sổng mất! Mất 1 mồi câu.");
         return;
      }
      scheduleTick(session, f);
   }

   /** Lý do không câu tiếp được (câu báo), null = vẫn ổn. */
   private String problem(PlayerSession session, PlayerData d, Fishing f) {
      WorldPlayer p = world.online(session);
      if (p == null || p.zone() != f.zone) {
         return "Đã rời khu vực, thôi câu cá.";
      }
      if (d.isDead()) {
         return "Đã bị hạ gục, thôi câu cá.";
      }
      if (p.locked() || d.trade != null || !holdingRod(d)) {
         return "Đã thôi câu cá.";
      }
      if (Math.abs(p.x() - f.x) > MOVE_TOLERANCE_PX || Math.abs(p.y() - f.y) > MOVE_TOLERANCE_PX) {
         return "Bạn đã di chuyển, thôi câu cá. Mất 1 mồi câu.";
      }
      return null;
   }

   /** Thôi câu: phát op 57/3 (về đứng yên, ẩn thanh kéo cần), báo {@code notice} nếu có. Luồng session. */
   private void stop(PlayerSession session, PlayerData d, String notice) {
      if (d.fishing == null) {
         return;
      }
      d.fishing = null;
      world.broadcastAround(session, FishingPackets.stop(d.getId()));
      if (notice != null) {
         session.send(Packets.notice(notice));
      }
   }

   private static String itemName(int templateId) {
      ItemPack.ItemTemplate template = GameDataCache.itemTemplate(templateId);
      return template != null ? template.name() : "vật phẩm #" + templateId;
   }
}
