package com.tlnt.server.world;

import com.tlnt.server.combat.MobRules;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.MapPack;
import com.tlnt.server.gather.GatherConfig;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Trạng thái quái của 1 khu: HP từng con + lúc hồi sinh. Danh sách quái ({@link MapTemplate#mobInstances()}) dùng
 * chung cả map, mỗi khu có HP riêng. CHỈ luồng tick của khu đụng tới (như mọi trạng thái của {@link Zone}).
 *
 * Mảng thô thay vì object mỗi con: map vài trăm quái × nhiều khu vẫn nhẹ, duyệt hồi sinh chỉ khi có con đang chết.
 *
 * <p><b>Quái săn bắt</b> ({@link GatherConfig} — tổ ong, quả dừa): KHÔNG nằm trong danh sách của map; {@link SpawnDirector}
 * (toàn server) chọn khu + giành suất rồi giao cho khu mọc ({@link #spawnGather}) ở 1 điểm ngẫu nhiên trong
 * {@code spots} của map. Id thực thể tăng dần từ {@link #GATHER_ID_BASE} (mỗi lần mọc 1 id mới — client thêm bằng
 * op 20 sub 4). Mỗi đòn trúng chỉ trừ 1 HP; bị phá thì biến mất, suất toàn server giữ {@code respawn} giây rồi con
 * mới mọc ở map / khu / điểm ngẫu nhiên.
 */
final class ZoneMobs {

   static final int GATHER_ID_BASE = 50_000;

   /** 1 con quái săn bắt đang đứng. */
   static final class Gather {
      final MapTemplate.MobInstance mob;
      final GatherConfig.Entry entry;
      long hp;
      /** Admin đặt thêm: ngoài suất toàn server, phá / xoá xong là hết. */
      boolean admin;

      Gather(MapTemplate.MobInstance mob, GatherConfig.Entry entry) {
         this.mob = mob;
         this.entry = entry;
         this.hp = entry.hp();
      }
   }

   /** Quái của khu (bản riêng: con hồi sinh thành Tinh anh / Thủ lĩnh được thay bằng bản có biến thể + HP mới). */
   private final List<MapTemplate.MobInstance> mobs;
   /** Quái gốc (bản của khu: map + chỗ mọc thêm {@link #addManual}) — hồi sinh thường thì trở lại đúng con này. */
   private final List<MapTemplate.MobInstance> base;
   /* Mảng theo id quái THƯỜNG: chỉ dài thêm khi {@link #addManual} (boss thế giới). */
   private long[] hp;
   /** Epoch ms hồi sinh; chỉ có nghĩa khi {@code hp[i] <= 0}. */
   private long[] respawnAt;
   private int deadCount;
   /** Người chơi quái đang đánh trả (id, 0 = không ai) — quái THƯỜNG, không có ở quái săn bắt. */
   private int[] aggro;
   /** Danh sách thù từng quái (cuối = người đánh gần đây nhất), cấp phát khi cần. */
   private List<Integer>[] threats;
   /** Epoch ms đòn kế tiếp của quái đang đánh trả. */
   private long[] nextAttackAt;
   /** Số lần con quái ở chỗ này đã chết — mỗi lượt sống = 1 mục tiêu mới (Nanh Hổ răng kiếm "đòn đầu tiên"). */
   private int[] deaths;
   /**
    * Chỗ do code mọc ({@link #addManual} — boss thế giới): không bao giờ tự hồi sinh; chết rồi thì không gửi cho người mới
    * vào khu nữa (client không dời được quái cũ -> lần xuất hiện sau là 1 id mới).
    */
   private boolean[] manual;
   /**
    * Quái đang đánh vì người đứng GẦN (chủ động tấn công — {@link #provokeNearby}), chưa ai đánh nó: người đi xa khỏi vùng
    * quái thì quái quên luôn ({@code ZoneCombat.mobAttacks}). Ai đánh trúng nó ({@link #provoke}) -> thành quái bị đánh
    * thường (nhớ tới khi chết).
    */
   private boolean[] nearbyAggro;
   /**
    * Siêu quái (Tinh anh / Thủ lĩnh) ĐANG ẨN: id những người đã ở trong khu lúc nó xuất hiện (thấy đúng biến thể). Người
    * vào khu sau thấy nó như quái THƯỜNG (gói của con gốc — chống tool dò siêu quái) tới khi có người đánh trúng nó
    * ({@link #revealIfHidden}). null = không ẩn.
    */
   private java.util.Set<Integer>[] witnesses;
   /** Gửi gói hiện đúng siêu quái cho những người trong khu KHÔNG thuộc tập đã thấy (Zone gắn lúc tạo). */
   interface Revealer {
      void reveal(java.util.Set<Integer> witnesses, Packet packet);
   }

   Revealer revealer;
   private int aggroCount;
   /** Gói op 20 sub 0 cho người vào khu; null = có thay đổi, dựng lại khi cần. */
   private List<Packet> snapshot;

   private final int mapId;
   private final List<Gather> gathers = new ArrayList<>();
   /** Khoá sổ đếm từng khu trong {@link SpawnQuota} (= khu chứa). */
   private final Object quotaKey;
   /** mob_template.id -> điểm mọc lần trước (lần sau tránh trùng chỗ). */
   private final Map<Integer, Integer> lastSpot = new HashMap<>();
   private int nextGatherId = GATHER_ID_BASE;

   ZoneMobs(List<MapTemplate.MobInstance> mobs) {
      this(mobs, 0, null);
   }

   /** Quái chết không hồi sinh (phó bản Tế đàn Thiên tích). */
   private final boolean noRespawn;

   ZoneMobs(List<MapTemplate.MobInstance> mobs, int mapId, Object quotaKey) {
      this(mobs, mapId, quotaKey, false);
   }

   ZoneMobs(List<MapTemplate.MobInstance> mobs, int mapId, Object quotaKey, boolean noRespawn) {
      this.noRespawn = noRespawn;
      this.quotaKey = quotaKey != null ? quotaKey : this;
      this.base = new ArrayList<>(mobs);
      this.mobs = new ArrayList<>(mobs);
      this.mapId = mapId;
      this.hp = new long[mobs.size()];
      this.respawnAt = new long[mobs.size()];
      this.aggro = new int[mobs.size()];
      @SuppressWarnings("unchecked")
      List<Integer>[] lists = new List[mobs.size()];
      this.threats = lists;
      this.nextAttackAt = new long[mobs.size()];
      this.deaths = new int[mobs.size()];
      this.manual = new boolean[mobs.size()];
      this.nearbyAggro = new boolean[mobs.size()];
      @SuppressWarnings("unchecked")
      java.util.Set<Integer>[] seen = new java.util.Set[mobs.size()];
      this.witnesses = seen;
      for (int i = 0; i < hp.length; i++) {
         hp[i] = mobs.get(i).spawn().hp();
      }
   }

   /** Số quái THƯỜNG của khu; id thực thể = 0 .. size-1. Duyệt mọi quái (cả săn bắt): {@link #slots} / {@link #idAt}. */
   int size() {
      return hp.length;
   }

   /** Số chỗ để duyệt mọi quái đang có (quái thường + quái săn bắt). */
   int slots() {
      return hp.length + gathers.size();
   }

   /** Id thực thể của chỗ {@code k} (0 .. {@link #slots}-1). */
   int idAt(int k) {
      return k < hp.length ? k : gathers.get(k - hp.length).mob.id();
   }

   /** HP hiện tại (0 = đang chết / id ngoài danh sách). */
   long hp(int mobId) {
      if (mobId >= GATHER_ID_BASE) {
         Gather g = gather(mobId);
         return g == null ? 0 : g.hp;
      }
      return mobId >= 0 && mobId < hp.length ? hp[mobId] : 0;
   }

   /** Số lần quái ở chỗ {@code mobId} đã chết (quái săn bắt / id ngoài danh sách: 0). */
   int deaths(int mobId) {
      return mobId >= 0 && mobId < deaths.length ? deaths[mobId] : 0;
   }

   /** Quái còn sống theo id thực thể; null nếu không có / đang chết. */
   MapTemplate.MobInstance alive(int mobId) {
      if (mobId >= GATHER_ID_BASE) {
         Gather g = gather(mobId);
         return g == null ? null : g.mob;
      }
      return mobId >= 0 && mobId < hp.length && hp[mobId] > 0 ? mobs.get(mobId) : null;
   }

   /** Quái thường ở chỗ {@code mobId} kể cả đang chết (vị trí / mẫu); null = id ngoài danh sách. */
   MapTemplate.MobInstance instance(int mobId) {
      return mobId >= 0 && mobId < hp.length ? mobs.get(mobId) : null;
   }

   /** Quái săn bắt còn đứng theo id, null = không phải / đã bị phá. */
   Gather gather(int mobId) {
      for (Gather g : gathers) {
         if (g.mob.id() == mobId) {
            return g;
         }
      }
      return null;
   }

   boolean isGather(int mobId) {
      return mobId >= GATHER_ID_BASE;
   }

   Iterable<Gather> gathers() {
      return gathers;
   }

   /**
    * Trừ máu con {@code mobId} (phải còn sống — gọi {@link #alive} trước). Quái thường chết thì hẹn hồi sinh sau
    * {@code respawn} giây của điểm sinh. Quái săn bắt: mỗi lần gọi trừ đúng 1 HP; hết thì gỡ khỏi khu + giữ suất.
    *
    * @return HP còn lại (0 = vừa chết)
    */
   long damage(int mobId, long amount, long now) {
      if (mobId >= GATHER_ID_BASE) {
         Gather g = gather(mobId);
         if (g == null) {
            return 0;
         }
         g.hp = Math.max(0, g.hp - 1);
         snapshot = null;
         if (g.hp == 0) {
            gathers.remove(g);
            if (!g.admin) {
               SpawnQuota.mob(g.entry.mobId()).killed(quotaKey, now + g.entry.respawnSec() * 1000L);
            }
         }
         return g.hp;
      }
      if (amount > 0) {
         revealIfHidden(mobId);
      }
      long left = Math.max(0, hp[mobId] - Math.max(0, amount));
      hp[mobId] = left;
      if (left == 0) {
         witnesses[mobId] = null;
         effects.remove(mobId); // lượt sống sau sạch hiệu ứng
         deaths[mobId]++;
         respawnAt[mobId] = noRespawn || manual[mobId] ? Long.MAX_VALUE
               : now + MobRules.respawnMs(mobs.get(mobId).spawn().respawn()); // mặc định 3 s
         deadCount++;
         calm(mobId);
      }
      snapshot = null;
      return left;
   }

   // ------------------------------------------------------------------ đánh trả

   /**
    * {@code playerId} vừa đánh quái {@code mobId} (còn sống): đưa lên ĐẦU danh sách thù (người đánh gần đây nhất bị cắn
    * trước — {@link #pickTarget}). Quái chỉ QUÊN danh sách khi nó chết ({@link #calm} trong {@link #damage}). Quái đang
    * rảnh thì khoá người này, đòn đầu sau {@link MobRules#MOB_FIRST_ATTACK_MS}; đang đánh thì giữ nhịp.
    */
   void provoke(int mobId, int playerId, long now) {
      if (mobId < 0 || mobId >= hp.length || hp[mobId] <= 0 || playerId == 0) {
         return;
      }
      List<Integer> list = threats[mobId];
      if (list == null) {
         list = threats[mobId] = new ArrayList<>(2);
      }
      list.remove(Integer.valueOf(playerId));
      list.add(playerId); // cuối danh sách = mới nhất
      nearbyAggro[mobId] = false; // bị đánh thật: nhớ tới khi chết
      if (aggro[mobId] == 0) {
         aggro[mobId] = playerId;
         nextAttackAt[mobId] = now + MobRules.MOB_FIRST_ATTACK_MS;
         aggroCount++;
      }
   }

   /**
    * Quái đang rảnh {@code mobId} thấy {@code playerId} đứng gần (map quái chủ động tấn công — {@link Regions}): khoá
    * người này như bị đánh nhưng đánh dấu {@link #nearbyAggro} (đi xa là quên).
    */
   void provokeNearby(int mobId, int playerId, long now) {
      if (mobId < 0 || mobId >= hp.length || aggro[mobId] != 0) {
         return;
      }
      provoke(mobId, playerId, now);
      if (aggro[mobId] == playerId) {
         nearbyAggro[mobId] = true;
      }
   }

   /** Quái {@code mobId} đang đánh vì người đứng gần (chưa ai đánh nó). */
   boolean nearbyAggro(int mobId) {
      return mobId >= 0 && mobId < nearbyAggro.length && nearbyAggro[mobId];
   }

   /**
    * Tới nhịp đánh: người ĐÁNH GẦN ĐÂY NHẤT mà {@code valid} (còn trong khu, còn sống, trong vùng quái) -> mục tiêu hiện
    * tại ({@link #aggroTarget}). Người không hợp lệ CHỈ bị bỏ qua lần này (quay lại vùng quái là bị cắn tiếp) — danh sách
    * chỉ xoá khi quái chết. 0 = lúc này không ai cắn được (quái vẫn nhớ).
    */
   int pickTarget(int mobId, java.util.function.IntPredicate valid) {
      List<Integer> list = mobId >= 0 && mobId < threats.length ? threats[mobId] : null;
      if (list != null) {
         for (int i = list.size() - 1; i >= 0; i--) {
            int playerId = list.get(i);
            if (valid.test(playerId)) {
               aggro[mobId] = playerId;
               return playerId;
            }
         }
      }
      return 0;
   }

   /** Số người trong danh sách thù của quái {@code mobId}. */
   int attackerCount(int mobId) {
      List<Integer> list = mobId >= 0 && mobId < threats.length ? threats[mobId] : null;
      return list == null ? 0 : list.size();
   }

   /** Quái thôi đánh hẳn (chết / không còn ai để cắn). */
   void calm(int mobId) {
      if (mobId >= 0 && mobId < nearbyAggro.length) {
         nearbyAggro[mobId] = false;
      }
      if (mobId >= 0 && mobId < aggro.length && aggro[mobId] != 0) {
         aggro[mobId] = 0;
         aggroCount--;
      }
      if (mobId >= 0 && mobId < threats.length && threats[mobId] != null) {
         threats[mobId].clear();
      }
   }

   // ------------------------------------------------------------------ hiệu ứng từ chỉ số người đánh

   /** Hiệu ứng đang dính trên 1 quái thường (epoch ms; 0 = không). Xoá khi quái chết. */
   static final class Effects {
      /** Choáng / đóng băng: không đánh trả. */
      long disabledUntil;
      long weakenedUntil;
      long burningUntil;
   }

   /** Quái thường đang có hiệu ứng (ít con cùng lúc -> map). */
   private final Map<Integer, Effects> effects = new HashMap<>();

   /** Hiệu ứng của quái thường {@code mobId} (tạo nếu chưa có); null = quái săn bắt / id lạ. */
   Effects effects(int mobId) {
      if (mobId < 0 || mobId >= hp.length) {
         return null;
      }
      return effects.computeIfAbsent(mobId, k -> new Effects());
   }

   /** Đang choáng / đóng băng (không đánh trả). */
   boolean disabled(int mobId, long now) {
      Effects e = effects.get(mobId);
      return e != null && now < e.disabledUntil;
   }

   /** Lúc hết choáng / đóng băng (0 = không). */
   long disabledUntil(int mobId) {
      Effects e = effects.get(mobId);
      return e == null ? 0 : e.disabledUntil;
   }

   /** Đang suy yếu (nhận thêm 20% sát thương). */
   boolean weakened(int mobId, long now) {
      Effects e = effects.get(mobId);
      return e != null && now < e.weakenedUntil;
   }

   /** Mọi quái thường của khu đã chết (map có quái). Phó bản: đã diệt hết -> mở đường sang map sau. */
   boolean allDead() {
      return hp.length > 0 && deadCount == hp.length;
   }

   /** Số quái thường còn sống. */
   int aliveCount() {
      return hp.length - deadCount;
   }

   /** Có quái nào đang đánh trả không (khu không có thì khỏi duyệt). */
   boolean anyAggro() {
      return aggroCount > 0;
   }

   /** Id người quái {@code mobId} đang đánh, 0 = không ai. */
   int aggroTarget(int mobId) {
      return mobId >= 0 && mobId < aggro.length ? aggro[mobId] : 0;
   }

   long nextAttackAt(int mobId) {
      return nextAttackAt[mobId];
   }

   void scheduleNextAttack(int mobId, long at) {
      nextAttackAt[mobId] = at;
   }

   /** Như {@link #respawnDue(long, Consumer, java.util.Set)} không ai trong khu (test). */
   void respawnDue(long now, Consumer<Packet> broadcast) {
      respawnDue(now, broadcast, java.util.Set.of());
   }

   /**
    * Hồi sinh những con đã tới giờ, phát op 20 sub 2 qua {@code broadcast} (cả khu). Con hồi sinh thành siêu quái: người
    * đang trong khu ({@code present}) thấy đúng; người vào sau thấy quái thường tới khi nó bị đánh ({@link #witnesses}).
    */
   void respawnDue(long now, Consumer<Packet> broadcast, java.util.Set<Integer> present) {
      if (deadCount == 0 || noRespawn) {
         return;
      }
      for (int i = 0; i < hp.length; i++) {
         if (hp[i] <= 0 && now >= respawnAt[i]) {
            int templateId = base.get(i).spawn().id();
            // giới hạn tính THEO LOẠI QUÁI: đếm tinh anh / thủ lĩnh cùng loại đang sống (chỉ khi có con tới giờ hồi sinh)
            MapTemplate.MobInstance mob = reroll(base.get(i),
                  ThreadLocalRandom.current().nextInt(MobRules.ROLL_RANGE),
                  countAlive(templateId, MobRules.VARIANT_ELITE), countAlive(templateId, MobRules.VARIANT_BOSS));
            mobs.set(i, mob);
            hp[i] = mob.spawn().hp();
            deadCount--;
            witnesses[i] = mob.spawn().variant() != MobRules.VARIANT_NORMAL ? new java.util.HashSet<>(present) : null;
            snapshot = null;
            broadcast.accept(WorldPackets.mobRespawn(mob).toPacket());
         }
      }
   }

   /**
    * Quái {@code mobId} vừa hồi sinh / xuất hiện chưa quá {@code ms} (mốc = {@link #respawnAt}, giữ nguyên sau khi sống
    * lại). Client chỉ khởi tạo quái ở khung hình kế (MobEntity.onAwake) — gói trúng đòn tới ngay cùng khung làm client
    * NPE ({@code wanderAI} null) -> bot chờ một chút mới đánh, như người thật cần thấy quái trước.
    */
   boolean fresh(int mobId, long now, long ms) {
      return mobId >= 0 && mobId < hp.length && hp[mobId] > 0 && now - respawnAt[mobId] < ms;
   }

   /**
    * Mọc thêm 1 quái THƯỜNG (đánh / đánh trả / EXP như mọi quái) ở (x, y) — boss thế giới: id mới = cuối danh sách, phát
    * op 20 sub 4 cả khu qua {@code broadcast}. Chỗ này không tự hồi sinh ({@link #isManual}). @return id thực thể
    */
   int addManual(MapTemplate.MobSpawn spawn, float x, float y, Consumer<Packet> broadcast) {
      int id = hp.length;
      int n = id + 1;
      hp = Arrays.copyOf(hp, n);
      respawnAt = Arrays.copyOf(respawnAt, n);
      aggro = Arrays.copyOf(aggro, n);
      threats = Arrays.copyOf(threats, n);
      nextAttackAt = Arrays.copyOf(nextAttackAt, n);
      deaths = Arrays.copyOf(deaths, n);
      manual = Arrays.copyOf(manual, n);
      nearbyAggro = Arrays.copyOf(nearbyAggro, n);
      witnesses = Arrays.copyOf(witnesses, n);
      MapTemplate.MobInstance mob = new MapTemplate.MobInstance(id, spawn, x, y);
      mobs.add(mob);
      base.add(mob);
      hp[id] = spawn.hp();
      respawnAt[id] = System.currentTimeMillis(); // mốc "vừa xuất hiện" ({@link #fresh})
      manual[id] = true;
      snapshot = null;
      broadcast.accept(WorldPackets.mobAdd(mob, spawn.hp()).toPacket());
      return id;
   }

   /** Chỗ {@code mobId} do code mọc ({@link #addManual}), không tự hồi sinh. */
   boolean isManual(int mobId) {
      return mobId >= 0 && mobId < manual.length && manual[mobId];
   }

   /**
    * Lệnh test: con {@code mobId} (đang sống, quái thường của map) thành biến thể {@code variant} ngay (0 = về con gốc), HP
    * đầy theo biến thể, KHÔNG xét giới hạn mỗi loại. null = không đổi được (đang chết / loại quái không có biến thể đó).
    */
   MapTemplate.MobInstance testSetVariant(int mobId, int variant) {
      if (mobId < 0 || mobId >= hp.length || hp[mobId] <= 0) {
         return null;
      }
      MapTemplate.MobInstance original = base.get(mobId);
      MapTemplate.MobSpawn spawn = original.spawn();
      MapTemplate.MobInstance mob;
      if (variant == MobRules.VARIANT_NORMAL) {
         mob = original;
      } else if (spawn.variant() != MobRules.VARIANT_NORMAL || variantCount(spawn.id()) <= variant) {
         return null;
      } else {
         mob = new MapTemplate.MobInstance(original.id(), spawn.withVariant(variant, MobRules.variantHp(spawn.hp(),
               variant)), original.x(), original.y());
      }
      mobs.set(mobId, mob);
      hp[mobId] = mob.spawn().hp();
      witnesses[mobId] = null; // lệnh test: hiện luôn
      snapshot = null;
      return mob;
   }

   /** CHỈ cho test: chỗ {@code mobId} thành {@code elite} đang ẩn, {@code seen} = người đã thấy lúc xuất hiện. */
   void hideForTest(int mobId, MapTemplate.MobInstance elite, java.util.Set<Integer> seen) {
      mobs.set(mobId, elite);
      hp[mobId] = elite.spawn().hp();
      witnesses[mobId] = new java.util.HashSet<>(seen);
      snapshot = null;
   }

   /** Siêu quái {@code mobId} đang ẩn với người vào khu sau. */
   boolean hidden(int mobId) {
      return mobId >= 0 && mobId < witnesses.length && witnesses[mobId] != null;
   }

   /**
    * Siêu quái đang ẩn bị đánh trúng: hiện đúng (op 20 sub 2 với HP thật) cho người trong khu chưa thấy, từ giờ người vào
    * khu cũng thấy đúng.
    */
   void revealIfHidden(int mobId) {
      if (!hidden(mobId)) {
         return;
      }
      java.util.Set<Integer> seen = witnesses[mobId];
      witnesses[mobId] = null;
      snapshot = null;
      if (revealer != null && hp[mobId] > 0) {
         revealer.reveal(seen, WorldPackets.mobRespawn(mobs.get(mobId), hp[mobId]).toPacket());
      }
   }

   /** Người {@code playerId} vừa vào khu: siêu quái đang ẩn mà họ đã thấy lúc xuất hiện -> gửi lại đúng (quay lại khu). */
   void sendWitnessed(int playerId, Consumer<Packet> send) {
      for (int i = 0; i < witnesses.length; i++) {
         if (witnesses[i] != null && hp[i] > 0 && witnesses[i].contains(playerId)) {
            send.accept(WorldPackets.mobRespawn(mobs.get(i), hp[i]).toPacket());
         }
      }
   }

   /** Số quái loại {@code templateId} còn sống có biến thể {@code variant} (tinh anh / thủ lĩnh) trong khu. */
   private int countAlive(int templateId, int variant) {
      int n = 0;
      for (int i = 0; i < hp.length; i++) {
         MapTemplate.MobSpawn spawn = mobs.get(i).spawn();
         if (hp[i] > 0 && spawn.variant() == variant && spawn.id() == templateId) {
            n++;
         }
      }
      return n;
   }

   /**
    * Con hồi sinh: {@link MobRules#rollVariant} ({@code roll} 0 .. {@link MobRules#ROLL_RANGE} - 1) — quái thường của
    * loại có đủ biến thể Tinh anh / Thủ lĩnh trong {@code mob_variant} (không phải người rơm, quái phó bản, boss) có thể
    * thành Tinh anh / Thủ lĩnh, HP nhân theo {@link MobRules#variantHp}; loại quái này trong khu đã đủ {@link MobRules#MAX_ELITE_PER_ZONE}
    * tinh anh / {@link MobRules#MAX_BOSS_PER_ZONE} thủ lĩnh đang sống ({@code elites} / {@code bosses}) thì về quái thường.
    */
   static MapTemplate.MobInstance reroll(MapTemplate.MobInstance original, int roll, int elites, int bosses) {
      MapTemplate.MobSpawn spawn = original.spawn();
      int variant = MobRules.rollVariant(roll);
      if (variant == MobRules.VARIANT_NORMAL || spawn.variant() != MobRules.VARIANT_NORMAL
            || variantCount(spawn.id()) <= variant
            || variant == MobRules.VARIANT_ELITE && elites >= MobRules.MAX_ELITE_PER_ZONE
            || variant == MobRules.VARIANT_BOSS && bosses >= MobRules.MAX_BOSS_PER_ZONE) {
         return original;
      }
      return new MapTemplate.MobInstance(original.id(), spawn.withVariant(variant, MobRules.variantHp(spawn.hp(),
            variant)), original.x(), original.y());
   }

   private static int variantCount(int mobTemplateId) {
      MapPack.Mob template = GameDataCache.mob(mobTemplateId);
      return template == null || template.variants() == null ? 0 : template.variants().size();
   }

   /**
    * Mọc 1 con quái săn bắt (suất đã được {@link SpawnDirector} giành) ở 1 điểm ngẫu nhiên của map, phát op 20 sub 4.
    * @return false nếu map không có điểm mọc (người gọi trả suất)
    */
   boolean spawnGather(GatherConfig.Entry entry, Consumer<Packet> broadcast) {
      List<float[]> spots = entry.spots(mapId);
      if (spots.isEmpty()) {
         return false;
      }
      float[] spot = spots.get(pickSpot(entry.mobId(), spots.size()));
      MapTemplate.MobInstance mob = new MapTemplate.MobInstance(nextGatherId++,
            MapTemplate.MobSpawn.of(entry.mobId(), spot[0], spot[1], entry.level(), entry.hp()), spot[0], spot[1]);
      gathers.add(new Gather(mob, entry));
      snapshot = null;
      broadcast.accept(WorldPackets.mobAdd(mob, entry.hp()).toPacket());
      return true;
   }

   /**
    * Admin xoá quái săn bắt {@code mobId} (op 20 sub 3 HP 0 -> client cho biến mất): như bị phá, KHÔNG rơi đồ; suất toàn
    * server giữ {@code respawn} giây rồi mọc chỗ khác (con admin đặt thì hết). @return false = không còn
    */
   boolean adminRemoveGather(int mobId, long now, Consumer<Packet> broadcast) {
      Gather g = gather(mobId);
      if (g == null) {
         return false;
      }
      gathers.remove(g);
      if (!g.admin) {
         SpawnQuota.mob(g.entry.mobId()).killed(quotaKey, now + g.entry.respawnSec() * 1000L);
      }
      snapshot = null;
      broadcast.accept(WorldPackets.mobDamaged(mobId, g.hp, 0).toPacket());
      return true;
   }

   /**
    * Admin đặt thêm 1 quái săn bắt ở ({@code x}, {@code y}) — null = 1 điểm mọc ngẫu nhiên của map trong cấu hình. Ngoài
    * suất toàn server. @return id thực thể, -1 = map không có điểm mọc mà không nhập toạ độ
    */
   int adminSpawnGather(GatherConfig.Entry entry, Float x, Float y, Consumer<Packet> broadcast) {
      float[] spot;
      if (x != null && y != null) {
         spot = new float[] {x, y};
      } else {
         List<float[]> spots = entry.spots(mapId);
         if (spots.isEmpty()) {
            return -1;
         }
         spot = spots.get(pickSpot(entry.mobId(), spots.size()));
      }
      MapTemplate.MobInstance mob = new MapTemplate.MobInstance(nextGatherId++,
            MapTemplate.MobSpawn.of(entry.mobId(), spot[0], spot[1], entry.level(), entry.hp()), spot[0], spot[1]);
      Gather g = new Gather(mob, entry);
      g.admin = true;
      gathers.add(g);
      snapshot = null;
      broadcast.accept(WorldPackets.mobAdd(mob, entry.hp()).toPacket());
      return mob.id();
   }

   /** Khu đóng: trả suất toàn server của mọi quái săn bắt đang đứng. */
   void releaseGathers() {
      for (Gather g : gathers) {
         if (!g.admin) {
            SpawnQuota.mob(g.entry.mobId()).release(quotaKey);
         }
      }
      gathers.clear();
      snapshot = null;
   }

   int countGathers(int mobId) {
      int n = 0;
      for (Gather g : gathers) {
         if (g.entry.mobId() == mobId && !g.admin) {
            n++;
         }
      }
      return n;
   }

   /** Điểm ngẫu nhiên, khác điểm lần trước của khu này (nếu có hơn 1 điểm). */
   private int pickSpot(int mobId, int count) {
      int last = lastSpot.getOrDefault(mobId, -1);
      int i = ThreadLocalRandom.current().nextInt(count);
      if (count > 1 && i == last) {
         i = (i + 1 + ThreadLocalRandom.current().nextInt(count - 1)) % count;
      }
      lastSpot.put(mobId, i);
      return i;
   }

   /** Toàn bộ quái của khu (con đang chết gửi HP 0 -> client ẩn). Mã hoá lại chỉ khi có thay đổi. */
   List<Packet> snapshot() {
      List<Packet> packets = snapshot;
      if (packets == null) {
         packets = new ArrayList<>();
         List<MapTemplate.MobInstance> list = mobs;
         long[] hps = hp;
         if (deadCount > 0) { // boss thế giới đã chết: không gửi nữa (mỗi lần xuất hiện 1 id mới)
            list = new ArrayList<>(mobs.size());
            hps = new long[mobs.size()];
            for (int i = 0; i < hp.length; i++) {
               if (!manual[i] || hp[i] > 0) {
                  hps[list.size()] = hp[i];
                  list.add(mobs.get(i));
               }
            }
         }
         boolean anyHidden = false;
         for (java.util.Set<Integer> w : witnesses) {
            anyHidden |= w != null;
         }
         if (anyHidden) { // siêu quái đang ẩn: gửi như con gốc (quái thường, đầy HP thường)
            List<MapTemplate.MobInstance> shown = new ArrayList<>(list.size());
            long[] shownHp = new long[list.size()];
            for (int k = 0; k < list.size(); k++) {
               MapTemplate.MobInstance m = list.get(k);
               int id = m.id();
               if (id >= 0 && id < witnesses.length && witnesses[id] != null && hps[k] > 0) {
                  MapTemplate.MobInstance original = base.get(id);
                  shown.add(original);
                  shownHp[k] = original.spawn().hp();
               } else {
                  shown.add(m);
                  shownHp[k] = hps[k];
               }
            }
            list = shown;
            hps = shownHp;
         }
         for (Message msg : WorldPackets.mobList(list, hps)) {
            packets.add(msg.toPacket());
         }
         for (Gather g : gathers) {
            packets.add(WorldPackets.mobAdd(g.mob, g.hp).toPacket());
         }
         packets = List.copyOf(packets);
         snapshot = packets;
      }
      return packets;
   }
}
