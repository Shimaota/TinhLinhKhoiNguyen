package com.tlnt.server.world;

import com.tlnt.server.net.Packet;
import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Vật phẩm rơi dưới đất của 1 khu (op 30 — client {@code handleMapItemUpdate}). CHỈ luồng tick của khu đụng tới.
 *
 * <ul>
 *   <li>Rơi: op 30 sub 1 cả khu; client tự cho rơi xuống đất (trọng lực), server đặt sẵn ở mặt đất bên dưới.</li>
 *   <li>Người làm rơi (vd người phá tổ ong) được ƯU TIÊN nhặt trong {@link #OWNER_MS}; sau đó ai nhặt trước được.</li>
 *   <li>Không ai nhặt sau {@link #LIFETIME_MS} -> biến mất (op 30 sub 2).</li>
 *   <li>Nhặt: op 30 sub 3 cả khu (vật phẩm bay vào người nhặt), đồ vào túi trên luồng session của người đó.</li>
 *   <li>Đồ RIÊNG ({@link #addPersonal} — vật phẩm nhiệm vụ): chỉ gửi cho chủ, chỉ chủ nhặt được, không nằm trong gói
 *   cả khu ({@link #snapshot}); chủ vào lại khu thì gửi riêng ({@link #personalOf}).</li>
 *   <li>Đồ CHỈ CHỦ NHẶT ({@link #addOwnerOnly} — vd Nanh Hổ răng kiếm bước 13.1): cả khu thấy, chỉ chủ nhặt được suốt
 *   {@link #LIFETIME_MS}, không ai nhặt thì biến mất.</li>
 * </ul>
 */
final class ZoneDrops {

   static final long OWNER_MS = 15_000;
   static final long LIFETIME_MS = 60_000;
   private static final long CHECK_INTERVAL_MS = 1_000;

   static final class Drop {
      final int id;
      final ItemData item;
      final float x;
      final float y;
      final int ownerId;
      final long ownerUntil;
      final long expireAt;
      /** Lúc rơi xuống đất (tự nhặt chỉ hút món đã nằm đủ lâu). */
      final long droppedAt;
      /** Đồ riêng của {@code ownerId}: người khác không thấy, không nhặt được. */
      final boolean personal;
      /** Cả khu thấy nhưng chỉ {@code ownerId} nhặt được (tới lúc biến mất). */
      final boolean ownerOnly;

      Drop(int id, ItemData item, float x, float y, int ownerId, long now) {
         this(id, item, x, y, ownerId, now, false, false);
      }

      Drop(int id, ItemData item, float x, float y, int ownerId, long now, boolean personal, boolean ownerOnly) {
         this(id, item, x, y, ownerId, now, personal, ownerOnly, LIFETIME_MS);
      }

      Drop(int id, ItemData item, float x, float y, int ownerId, long now, boolean personal, boolean ownerOnly,
            long lifetimeMs) {
         this.id = id;
         this.item = item;
         this.x = x;
         this.y = y;
         this.ownerId = ownerId;
         this.ownerUntil = ownerId > 0 ? now + OWNER_MS : 0;
         this.expireAt = now + lifetimeMs;
         this.droppedAt = now;
         this.personal = personal;
         this.ownerOnly = ownerOnly;
      }

      /** Người khác chủ còn phải chờ bao lâu (ms), 0 = nhặt được. */
      long waitFor(int playerId, long now) {
         if (personal || ownerOnly) {
            return playerId == ownerId ? 0 : Long.MAX_VALUE;
         }
         return playerId == ownerId ? 0 : Math.max(0, ownerUntil - now);
      }
   }

   private final Map<Integer, Drop> drops = new LinkedHashMap<>();
   private int nextId = 1;
   private long nextCheckAt;
   private Packet snapshot;

   Drop add(ItemData item, float x, float y, int ownerId, long now, Consumer<Packet> broadcast) {
      Drop drop = new Drop(nextId++, item, x, y, ownerId, now);
      drops.put(drop.id, drop);
      snapshot = null;
      broadcast.accept(WorldPackets.groundItemAdd(drop.id, item.templateId, x, y).toPacket());
      return drop;
   }

   /** Đồ riêng của {@code ownerId} (vật phẩm nhiệm vụ): op 30 sub 1 chỉ gửi qua {@code toOwner}. */
   Drop addPersonal(ItemData item, float x, float y, int ownerId, long now, Consumer<Packet> toOwner) {
      Drop drop = new Drop(nextId++, item, x, y, ownerId, now, true, false);
      drops.put(drop.id, drop);
      toOwner.accept(WorldPackets.groundItemAdd(drop.id, item.templateId, x, y).toPacket());
      return drop;
   }

   /** Đồ cả khu thấy nhưng chỉ {@code ownerId} nhặt được: op 30 sub 1 cả khu. */
   Drop addOwnerOnly(ItemData item, float x, float y, int ownerId, long now, Consumer<Packet> broadcast) {
      return addOwnerOnly(item, x, y, ownerId, now, LIFETIME_MS, broadcast);
   }

   /** Như trên, nằm dưới đất {@code lifetimeMs} (quà boss thế giới: lâu hơn — người có công có thể đang hồi sinh). */
   Drop addOwnerOnly(ItemData item, float x, float y, int ownerId, long now, long lifetimeMs,
         Consumer<Packet> broadcast) {
      Drop drop = new Drop(nextId++, item, x, y, ownerId, now, false, true, lifetimeMs);
      drops.put(drop.id, drop);
      snapshot = null;
      broadcast.accept(WorldPackets.groundItemAdd(drop.id, item.templateId, x, y).toPacket());
      return drop;
   }

   /**
    * Món {@code playerId} nhặt được ngay (hết ưu tiên của người khác / đồ riêng của họ) trong ô ±{@code range} quanh
    * (x, y), đã nằm dưới đất ít nhất {@code minAgeMs}.
    */
   List<Drop> pickableNear(int playerId, float x, float y, float range, long minAgeMs, long now) {
      List<Drop> out = null;
      for (Drop d : drops.values()) {
         if (Math.abs(d.x - x) <= range && Math.abs(d.y - y) <= range && now - d.droppedAt >= minAgeMs
               && d.waitFor(playerId, now) == 0) {
            if (out == null) {
               out = new ArrayList<>(4);
            }
            out.add(d);
         }
      }
      return out == null ? List.of() : out;
   }

   /** Đồ riêng của {@code playerId} đang nằm dưới đất (gửi lại khi họ vào khu). */
   List<Drop> personalOf(int playerId) {
      List<Drop> out = new ArrayList<>();
      for (Drop d : drops.values()) {
         if (d.personal && d.ownerId == playerId) {
            out.add(d);
         }
      }
      return out;
   }

   Drop get(int id) {
      return drops.get(id);
   }

   /** Gỡ (đã nhặt) — người gọi tự phát gói. */
   void remove(Drop drop) {
      if (drops.remove(drop.id) != null && !drop.personal) {
         snapshot = null;
      }
   }

   int size() {
      return drops.size();
   }

   /** Gỡ vật phẩm hết hạn, phát op 30 sub 2. */
   void expire(long now, Consumer<Packet> broadcast) {
      if (drops.isEmpty() || now < nextCheckAt) {
         return;
      }
      nextCheckAt = now + CHECK_INTERVAL_MS;
      List<Drop> gone = new ArrayList<>();
      for (Drop d : drops.values()) {
         if (now >= d.expireAt) {
            gone.add(d);
         }
      }
      for (Drop d : gone) {
         drops.remove(d.id);
         snapshot = null;
         broadcast.accept(WorldPackets.groundItemRemove(d.id).toPacket());
      }
   }

   /** Op 30 sub 0 cho người vào khu (client xoá hết vật phẩm cũ khi đổi map). */
   Packet snapshot() {
      Packet packet = snapshot;
      if (packet == null) {
         List<Drop> shared = new ArrayList<>(drops.size());
         for (Drop d : drops.values()) {
            if (!d.personal) {
               shared.add(d);
            }
         }
         packet = WorldPackets.groundItems(shared).toPacket();
         snapshot = packet;
      }
      return packet;
   }
}
