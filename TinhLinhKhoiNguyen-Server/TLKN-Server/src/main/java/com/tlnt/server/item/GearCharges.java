package com.tlnt.server.item;

import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phụ kiện có LƯỢT / ĐỘ BỀN (đeo ở ô phụ kiện, mô tả item "Khi ... đủ N lần ... sẽ bị vỡ và hết hiệu lực"). Số còn lại
 * nằm trên chính món đồ = dòng chỉ số 58 "Sức chống chịu {0}" / 59 "Hiệu lực {0}" (client tự hiện), thiếu dòng thì coi
 * như đầy ({@link Kind#full}).
 *
 * <pre>
 * item | dòng | đầy      | tác dụng (đang đeo)
 * 165  | 58   | 1.000.000| Khiên vỏ sò: giảm 20% sát thương nhận vào, phần chặn được trừ vào độ bền
 * 262  | 59   | 500      | Nanh Hổ răng kiếm: đòn đầu tiên lên 1 mục tiêu chắc chắn chí mạng + 20% sát thương chí mạng
 * 263  | 59   | 100      | Bùa linh mộc: hồi sinh -> bất tử 3 s + 10% Khuếch đại tấn công 15 s (option 25)
 * 264  | 59   | 500      | Mắt bão Darguun: dùng kỹ năng (trừ đánh thường) -> +2000 Né, +20% tốc chạy 5 s (option 26)
 * 278  | 59   | 500      | Tim băng: 5% đòn đánh đóng băng mục tiêu (người bị đóng băng: option 8)
 * 279  | 59   | 500      | Nhẫn u mộc: 5% đòn đánh rễ cây trói mục tiêu (người bị trói: option 21)
 * 295  | -    | -        | Trái tim đại dương (không có lượt): đứng gần nước +100 tốc chạy, +5% Tấn công
 * </pre>
 * Mỗi lần kích hoạt -1 lượt (khiên: - số sát thương đã chặn). Về 0 = VỠ: món đồ biến mất khỏi người.
 *
 * <p>Luồng: đồ mặc chỉ luồng session sửa, nhưng lượt bị trừ trên luồng tick khu (đòn đánh). Nên lúc mặc / tháo đồ luồng
 * session dựng {@link State} (bộ đếm {@link AtomicLong} cho từng món, công bố volatile {@link PlayerData#gear}); luồng
 * nào cũng trừ được ({@link #use}). Số trên bộ đếm được chép ngược vào dòng chỉ số ở luồng session: mỗi 5 s nếu có đổi
 * ({@link #flush}, gọi từ {@code ItemBuffs}), ngay khi vỡ, và trước khi chụp lưu DB ({@link #writeBack}).
 */
public final class GearCharges {

   public static final int STAT_DURABILITY = 58;
   public static final int STAT_CHARGES = 59;

   public enum Kind {
      SHIELD(165, STAT_DURABILITY, 1_000_000),
      FANG(262, STAT_CHARGES, 500),
      CHARM(263, STAT_CHARGES, 100),
      STORM(264, STAT_CHARGES, 500),
      ICE(278, STAT_CHARGES, 500),
      ROOT(279, STAT_CHARGES, 500);

      public final int templateId;
      public final int statId;
      public final long full;

      Kind(int templateId, int statId, long full) {
         this.templateId = templateId;
         this.statId = statId;
         this.full = full;
      }

      /** Phụ kiện có lượt của mẫu item; null = không phải. */
      public static Kind of(int templateId) {
         for (Kind k : VALUES) {
            if (k.templateId == templateId) {
               return k;
            }
         }
         return null;
      }

      private static final Kind[] VALUES = values();
   }

   /** Trái tim đại dương (phụ kiện ô 9, không có lượt). */
   public static final int OCEAN_HEART = 295;
   /** Trái tim đại dương: đứng cách vùng nước của map không quá bấy nhiêu px (= tầm câu cá). */
   public static final float OCEAN_REACH_PX = 200f;
   public static final int OCEAN_MOVE_SPEED = 100;
   public static final int OCEAN_ATTACK_PERCENT = 5;
   /** Khiên vỏ sò: % sát thương nhận vào bị chặn. */
   public static final int SHIELD_PERCENT = 20;
   /** Nanh Hổ răng kiếm: % sát thương chí mạng cộng thêm của đòn đầu tiên. */
   public static final int FANG_CRIT_BONUS = 20;
   /** Tim băng / Nhẫn u mộc: % mỗi đòn trúng. */
   public static final int ICE_CHANCE = 5;
   public static final int ROOT_CHANCE = 5;
   /** Thời gian đóng băng / trói (như hiệu ứng kỹ năng mặc định). */
   public static final int ICE_MS = 2_000;
   public static final int ROOT_MS = 3_000;
   /** Bùa linh mộc: bất tử sau khi hồi sinh. */
   public static final long CHARM_INVULNERABLE_MS = 3_000;

   /** 1 món đang đeo + số lượt còn lại (ghi từ mọi luồng). */
   static final class Slot {
      final ItemData item;
      final AtomicLong left;

      Slot(ItemData item, long left) {
         this.item = item;
         this.left = new AtomicLong(left);
      }
   }

   /** Phụ kiện có lượt đang đeo — bất biến trừ bộ đếm / cờ. */
   public static final class State {
      final Slot[] slots;
      /** Đang đeo Trái tim đại dương. */
      public final boolean oceanHeart;
      /** Có lượt bị trừ chưa chép vào dòng chỉ số / chưa gửi lại bảng đồ mặc. */
      final AtomicBoolean dirty = new AtomicBoolean();
      /** Đã xếp việc gỡ món vỡ vào hàng đợi session (chỉ xếp 1 lần). */
      final AtomicBoolean brokenPosted = new AtomicBoolean();

      State(Slot[] slots, boolean oceanHeart) {
         this.slots = slots;
         this.oceanHeart = oceanHeart;
      }

      /** Đang đeo món {@code kind} còn lượt. */
      public boolean has(Kind kind) {
         Slot s = slots[kind.ordinal()];
         return s != null && s.left.get() > 0;
      }

      /** Số lượt còn của món {@code kind} (0 = không đeo / đã vỡ). */
      public long left(Kind kind) {
         Slot s = slots[kind.ordinal()];
         return s == null ? 0 : Math.max(0, s.left.get());
      }

      public boolean dirty() {
         return dirty.get();
      }

      /** Trừ tối đa {@code amount}. @return số đã trừ thật (0 = không đeo / đã vỡ) */
      long consume(Kind kind, long amount) {
         Slot s = slots[kind.ordinal()];
         if (s == null || amount <= 0) {
            return 0;
         }
         while (true) {
            long cur = s.left.get();
            if (cur <= 0) {
               return 0;
            }
            long used = Math.min(cur, amount);
            if (s.left.compareAndSet(cur, cur - used)) {
               dirty.set(true);
               return used;
            }
         }
      }

      boolean anyBroken() {
         for (Slot s : slots) {
            if (s != null && s.left.get() <= 0) {
               return true;
            }
         }
         return false;
      }
   }

   public static final State NONE = new State(new Slot[Kind.VALUES.length], false);

   private static volatile World world;
   private static volatile Executor gamePool;

   private GearCharges() {
   }

   /** Gọi 1 lần lúc khởi động ({@code GameServer}). Chưa gọi (test) thì món vỡ chỉ được gỡ ở lần {@link #flush} sau. */
   public static void install(World w, Executor pool) {
      world = w;
      gamePool = pool;
   }

   /** Xếp {@code task} vào hàng đợi session (từ bất kỳ luồng nào). Chưa {@link #install} thì bỏ. */
   public static void post(PlayerSession session, Runnable task) {
      Executor pool = gamePool;
      if (pool != null && session != null) {
         session.inbox().execute(task, pool);
      }
   }

   public static World world() {
      return world;
   }

   // ------------------------------------------------------------------ trừ lượt (mọi luồng)

   /** Trừ lượt trên luồng tick khu. Về 0 -> xếp việc gỡ món vỡ. @return số đã trừ */
   public static long use(WorldPlayer player, Kind kind, long amount) {
      return use(player.session(), player.data(), kind, amount);
   }

   /** Trừ lượt trên luồng session ({@code CombatService}). */
   public static long use(PlayerSession session, PlayerData d, Kind kind, long amount) {
      State state = d.gear;
      long used = state.consume(kind, amount);
      if (used > 0 && state.left(kind) <= 0 && state.brokenPosted.compareAndSet(false, true)) {
         post(session, () -> flush(session));
      }
      return used;
   }

   // ------------------------------------------------------------------ luồng session

   /**
    * Dựng lại {@link PlayerData#gear} từ đồ đang mặc (sau khi mặc / tháo / vào game). Món vẫn đeo giữ nguyên bộ đếm cũ
    * (cùng object) -> lượt đang bị trừ trên luồng khu không mất. Luồng session.
    */
   public static void rebuild(PlayerData d) {
      State old = d.gear != null ? d.gear : NONE;
      writeBack(old);
      Slot[] slots = new Slot[Kind.VALUES.length];
      boolean ocean = false;
      for (int i = 0; i < d.body.size(); i++) {
         ItemData item = d.body.get(i);
         if (item == null) {
            continue;
         }
         ocean |= item.templateId == OCEAN_HEART;
         Kind kind = Kind.of(item.templateId);
         if (kind == null) {
            continue;
         }
         ensureStat(item);
         Slot prev = old.slots[kind.ordinal()];
         slots[kind.ordinal()] = prev != null && prev.item == item ? prev : new Slot(item, current(item, kind));
      }
      State next = new State(slots, ocean);
      if (old.dirty.get()) {
         next.dirty.set(true);
      }
      if (!ocean) {
         d.nearWater = false;
         d.waterPosted = -1;
      }
      d.gear = next;
   }

   /** Chép số lượt còn vào dòng chỉ số của từng món (trước khi lưu DB / gửi client). Luồng session. */
   public static void writeBack(State state) {
      if (state == null) {
         return;
      }
      for (Slot s : state.slots) {
         if (s != null) {
            setStat(s.item, Kind.of(s.item.templateId), Math.max(0, s.left.get()));
         }
      }
   }

   /** {@link #writeBack} cho nhân vật (gọi lúc chụp lưu DB). */
   public static void writeBack(PlayerData d) {
      writeBack(d.gear);
   }

   /**
    * Vào game: gỡ món đã vỡ (hết lượt) khỏi đồ mặc (không báo — chưa gửi gì cho client). @return true nếu có gỡ
    */
   public static boolean purgeBroken(PlayerData d) {
      boolean removed = false;
      for (int i = 0; i < d.body.size(); i++) {
         ItemData item = d.body.get(i);
         Kind kind = item == null ? null : Kind.of(item.templateId);
         if (kind != null && current(item, kind) <= 0) {
            d.body.take(i);
            removed = true;
         }
      }
      rebuild(d);
      return removed;
   }

   /**
    * Luồng session: chép lượt vào món đồ; món về 0 thì vỡ — gỡ khỏi người, báo, tính lại chỉ số; còn không thì chỉ gửi lại
    * bảng đồ mặc (client hiện số lượt mới). Không có gì đổi thì không làm gì.
    */
   public static void flush(PlayerSession session) {
      PlayerData d = session.getPlayer();
      State state = d == null ? null : d.gear;
      if (state == null || (!state.dirty.get() && !state.anyBroken())) {
         return;
      }
      state.dirty.set(false);
      writeBack(state);
      List<String> broken = new ArrayList<>();
      for (Slot s : state.slots) {
         if (s == null || s.left.get() > 0) {
            continue;
         }
         for (int i = 0; i < d.body.size(); i++) {
            if (d.body.get(i) == s.item) {
               d.body.take(i);
               broken.add(name(s.item));
               break;
            }
         }
      }
      if (broken.isEmpty()) {
         session.send(ItemPackets.container(ItemPackets.SUB_BODY, d.body));
         return;
      }
      rebuild(d);
      session.send(ItemPackets.container(ItemPackets.SUB_BODY, d.body));
      for (String name : broken) {
         session.send(Packets.notice(name + " đã bị vỡ và hết hiệu lực."));
      }
      ItemService.refreshLook(d);
      long now = System.currentTimeMillis();
      session.send(ItemBuffs.packet(d, now)); // icon Khiên vỏ sò
      World w = world;
      if (w != null) {
         w.broadcastStats(d);
      }
   }

   // ------------------------------------------------------------------ dòng chỉ số

   /** Món có lượt mà chưa có dòng 58 / 59 -> thêm dòng đầy (đồ rơi / thưởng tạo bằng {@code ItemData.of}). */
   public static void ensureStat(ItemData item) {
      Kind kind = item == null ? null : Kind.of(item.templateId);
      if (kind != null && find(item, kind.statId) < 0) {
         item.stats.add(new ItemData.StatValue(kind.statId, new int[] {(int) kind.full}));
      }
   }

   /** Số lượt đang ghi trên món đồ (thiếu dòng = đầy). */
   static long current(ItemData item, Kind kind) {
      int idx = find(item, kind.statId);
      if (idx < 0) {
         return kind.full;
      }
      int[] values = item.stats.get(idx).values();
      return values == null || values.length == 0 ? kind.full : values[0];
   }

   private static void setStat(ItemData item, Kind kind, long value) {
      if (kind == null) {
         return;
      }
      int idx = find(item, kind.statId);
      ItemData.StatValue line = new ItemData.StatValue(kind.statId, new int[] {(int) Math.min(Integer.MAX_VALUE, value)});
      if (idx < 0) {
         item.stats.add(line);
      } else {
         item.stats.set(idx, line); // thay dòng (không sửa mảng cũ: bản sao item dùng chung mảng)
      }
   }

   private static int find(ItemData item, int statId) {
      for (int i = 0; i < item.stats.size(); i++) {
         if (item.stats.get(i).statTypeId() == statId) {
            return i;
         }
      }
      return -1;
   }

   private static String name(ItemData item) {
      var template = item.template();
      return template != null ? template.name() : "Vật phẩm";
   }
}
