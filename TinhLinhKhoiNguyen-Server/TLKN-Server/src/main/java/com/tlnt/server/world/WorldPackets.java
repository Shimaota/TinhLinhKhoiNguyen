package com.tlnt.server.world;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.fishing.FishingPackets;
import com.tlnt.server.fishing.FishingService;
import com.tlnt.server.giftcode.GiftcodeService;
import com.tlnt.server.item.Appearance;
import com.tlnt.server.item.CollectionKind;
import com.tlnt.server.item.ItemCollection;
import com.tlnt.server.item.ItemBuffs;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.mail.Mail;
import com.tlnt.server.map.MapPackets;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Packet;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.party.Party;
import com.tlnt.server.tribe.TribeTag;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.Gender;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.player.SkillData;
import com.tlnt.server.ranking.RankingService;
import com.tlnt.server.welfare.WelfareService;

import java.util.ArrayList;
import java.util.List;

/**
 * Gói S→C về nhân vật trong thế giới. Format bám {@code MessageHandler} / {@code ServerPacketHandler}
 * phía client (ghi rõ nhánh ở từng method). Toạ độ gửi bằng pixel, client tự chia 100.
 */
public final class WorldPackets {

   /** Loại thực thể ở byte đầu khối chỉ số: 0 = người chơi (8 = thú cưỡi, 9/10 = đối tượng đặc biệt). */
   private static final int ENTITY_PLAYER = 0;

   private WorldPackets() {
   }

   /** Opcode 2 sub 0: tạo nhân vật của chính mình (client gọi PlayerFactory.obtainMainPlayer). */
   static Message localPlayer(WorldPlayer p) {
      Message msg = new Message(2);
      msg.writeByte(0);
      msg.writeInt(p.id());
      msg.writeFloat(p.x);
      msg.writeFloat(p.y);
      writeStats(msg, p.data(), p.stats(), p.locked, p.duelActiveOpponent());
      return msg;
   }

   /** Opcode 3: người chơi khác xuất hiện trong khu. */
   static Packet playerSpawn(WorldPlayer p) {
      Message msg = new Message(3);
      msg.writeInt(p.id());
      msg.writeFloat(p.x);
      msg.writeFloat(p.y);
      msg.writeBoolean(p.facingLeft);
      writeStats(msg, p.data(), p.stats(), p.locked, p.duelActiveOpponent());
      return msg.toPacket();
   }

   /**
    * Opcode 3 cho ẢNH ẢO (28 Ảo Ảnh Phách): y hệt ngoại hình của {@code p} nhưng id riêng (âm, không trùng người
    * thật), đứng ở ({@code x}, {@code y}) và ở trạng thái khoá -> người khác không chọn làm mục tiêu được.
    */
   static Packet illusionSpawn(WorldPlayer p, int illusionId, float x, float y) {
      Message msg = new Message(3);
      msg.writeInt(illusionId);
      msg.writeFloat(x);
      msg.writeFloat(y);
      msg.writeBoolean(p.facingLeft);
      writeStats(msg, p.data(), p.stats(), true, 0);
      return msg.toPacket();
   }

   /**
    * Op 2 sub 2 (client {@code PlayerEntity.setStealthed}, decompile C3540 m16492): int id, bool tàng hình. Người khác: ẩn
    * hẳn (thân, tên, bóng); chính mình: mờ 20%.
    */
   static Packet stealth(int playerId, boolean on) {
      Message msg = new Message(2);
      msg.writeByte(2);
      msg.writeInt(playerId);
      msg.writeBoolean(on);
      return msg.toPacket();
   }

   /** Loại thực thể 8 ({@code PlayerEntity.isMountClass}): client không liệt kê vào "người chơi gần đây" của tổ đội. */
   private static final int ENTITY_PET = 8;

   /**
    * Opcode 3 cho THÚ CƯNG đi theo {@code owner}: người chơi giả id {@link WorldPlayer#petId}, skeleton của char_template
    * thú cưng (không skin — skeleton quái chỉ có skin mặc định), tên = tên item, đang khoá (không ai chọn làm mục tiêu
    * được, không thanh máu), cấp 1, {@link #PET_HP} HP / MP. Server tự mô phỏng di chuyển ({@link Pet}), client lướt
    * tới vị trí nhận qua op 5 như người chơi khác.
    */
   static Packet petSpawn(WorldPlayer owner) {
      Pet pet = owner.pet;
      return companionSpawn(owner, owner.petId(), pet.templateId, pet.name, pet.x, pet.y, pet.facingLeft, PET_HP);
   }

   /** Op 3 cho sinh vật hộ tống của {@code owner} (như thú cưng, id {@link WorldPlayer#escortId}, {@link #ESCORT_HP}). */
   static Packet escortSpawn(WorldPlayer owner) {
      Escort e = owner.escort;
      return companionSpawn(owner, owner.escortId(), e.templateId, e.name, e.x, e.y, e.facingLeft, ESCORT_HP);
   }

   /**
    * Người chơi giả đi theo {@code owner} (thú cưng / sinh vật hộ tống): skeleton + avatar tra từ char_template ID trong
    * {@code item_appearance}, {@code hp} HP / MP.
    */
   private static Packet companionSpawn(WorldPlayer owner, int id, int templateId, String name, float x, float y,
         boolean facingLeft, long hp) {
      GamePack.CharTemplate template = GameDataCache.charTemplate(templateId);
      PlayerData d = owner.data();
      PlayerStats s = owner.stats();
      Message msg = new Message(3);
      msg.writeInt(id);
      msg.writeFloat(x);
      msg.writeFloat(y);
      msg.writeBoolean(facingLeft);
      msg.writeByte(ENTITY_PET);
      msg.writeString(name);
      msg.writeInt(-1); // tổ đội
      msg.writeInt(-1); // bộ lạc
      msg.writeString("");
      msg.writeString("");
      msg.writeString(template != null && template.avatarIconPath() != null ? template.avatarIconPath() : "");
      msg.writeByte(d.classId);
      msg.writeByte(Gender.charTemplateId(d.gender));
      msg.writeBoolean(false); // thanh máu
      msg.writeByte(0); // hoà bình: không icon trạng thái PK cạnh tên
      msg.writeBoolean(false); // kẻ địch
      msg.writeBoolean(true); // khoá: không chọn làm mục tiêu được
      msg.writeBoolean(false); // thách đấu
      msg.writeString(template != null && template.skeletonPath() != null ? template.skeletonPath()
            : "skeletons/chars/" + templateId);
      msg.writeByte(0); // skin: dùng skin mặc định của skeleton
      msg.writeInt(-1); // Ấn
      msg.writeString(""); // danh hiệu
      msg.writeByte(-1); // vũ khí
      msg.writeShort(1);
      msg.writeLong(0);
      msg.writeLong(PlayerStats.expToNextLevel(1));
      msg.writeInt(0);
      msg.writeByte(0); // tiềm năng
      msg.writeLong(hp);
      msg.writeLong(hp);
      msg.writeLong(hp); // MP
      msg.writeLong(hp);
      long[] rest = {s.attackPower, s.attackVsMob, s.critChance, s.critDamagePercent, s.armor, s.armorPen,
            s.evasion, s.accuracy, s.moveSpeed, s.hpRegen, s.mpRegen, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0};
      for (long value : rest) {
         msg.writeLong(value);
      }
      return msg.toPacket();
   }

   /**
    * Op 3 cho BOSS Tế đàn ({@link AltarBoss}): người chơi giả loại 8, có thanh máu, cờ Thử thách (byte 2 — client mọi cờ
    * đều chọn làm mục tiêu được: {@code AutoTargetController.canTargetPlayer}), không khoá, skeleton + skin + cấp vũ khí
    * + chỉ số theo {@code AltarRules.BossSpec}. Cùng thứ tự trường với {@link #writeStats}.
    */
   static Packet bossSpawn(AltarBoss boss) {
      com.tlnt.server.altar.AltarRules.BossSpec spec = boss.spec;
      GamePack.CharTemplate template = GameDataCache.charTemplate(spec.charTemplateId());
      PlayerStats s = boss.stats;
      Message msg = new Message(3);
      msg.writeInt(boss.id);
      msg.writeFloat(boss.x);
      msg.writeFloat(boss.y);
      msg.writeBoolean(true);
      msg.writeByte(ENTITY_PET);
      msg.writeString(spec.name());
      msg.writeInt(-1); // tổ đội
      msg.writeInt(-1); // bộ lạc
      msg.writeString("");
      msg.writeString("");
      msg.writeString(template != null ? template.avatarIconPath() : "");
      msg.writeByte(spec.classId());
      msg.writeByte(spec.charTemplateId());
      msg.writeBoolean(true); // thanh máu
      msg.writeByte(com.tlnt.server.social.PkRules.CHALLENGE);
      msg.writeBoolean(false); // kẻ địch
      msg.writeBoolean(false); // khoá
      msg.writeBoolean(false); // thách đấu
      msg.writeString(template != null ? template.skeletonPath() : "skeletons/chars/" + spec.charTemplateId());
      msg.writeByte(spec.skins().length);
      for (String skin : spec.skins()) {
         msg.writeString(skin);
      }
      msg.writeInt(-1); // Ấn
      msg.writeString(""); // danh hiệu
      msg.writeByte(spec.weaponLevel());
      msg.writeShort(spec.level());
      msg.writeLong(0);
      msg.writeLong(PlayerStats.expToNextLevel(spec.level()));
      msg.writeInt(0);
      msg.writeByte(0); // tiềm năng
      msg.writeLong(boss.hp);
      msg.writeLong(spec.hp());
      msg.writeLong(spec.mp());
      msg.writeLong(spec.mp());
      long[] rest = {s.attackPower, s.attackVsMob, s.critChance, s.critDamagePercent, s.armor, s.armorPen,
            s.evasion, s.accuracy, s.moveSpeed, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
      for (long value : rest) {
         msg.writeLong(value);
      }
      return msg.toPacket();
   }

   /** Op 21 sub 0 chiêu boss không payload (34 = {@code stick/skill3} dậm đất, rung màn hình). */
   static Message bossSkill(int bossId, int skillId) {
      return skillAction(bossId, skillId);
   }

   /** Op 21 sub 0 chiêu có 1 bool (37 {@code stick/skill6}: true = dựng khiên nước, false = khiên vỡ). */
   static Message bossToggle(int bossId, int skillId, boolean on) {
      Message msg = skillAction(bossId, skillId);
      msg.writeBoolean(on);
      return msg;
   }

   /** Op 21 sub 0 skill 33 ({@code stick/skill2}): byte hướng -1 trái / 1 phải. */
   static Message bossFacing(int bossId, int skillId, int dir) {
      Message msg = skillAction(bossId, skillId);
      msg.writeByte(dir < 0 ? -1 : 1);
      return msg;
   }

   /** Op 21 sub 0 skill 40 ({@code stick/skill9}): bool false + byte 0 bắt đầu (lặp skill9_2) / 1 kết thúc (skill9_3). */
   static Message bossPhase(int bossId, int skillId, int phase) {
      Message msg = skillAction(bossId, skillId);
      msg.writeBoolean(false);
      msg.writeByte(phase);
      return msg;
   }

   /** Op 21 sub 0 skill 40: bool true + mục tiêu = 1 lông vũ bay từ boss tới người đó. */
   static Message bossFeather(int bossId, int skillId, int targetId, long hpLeft, long damage, boolean crit) {
      Message msg = skillAction(bossId, skillId);
      msg.writeBoolean(true);
      writeTargetHit(msg, TARGET_PLAYER, targetId, hpLeft, damage, crit);
      return msg;
   }

   /** HP / MP thú cưng (cấp 1). */
   static final long PET_HP = 20_000L;
   /** HP / MP sinh vật hộ tống (Moara NV 9.4). */
   static final long ESCORT_HP = 25_000L;

   /** Opcode 26 sub 0: chat khu — int người nói, str nội dung (client hiện bong bóng + thêm vào chat "gần đây"). */
   static Packet chat(int senderId, String text) {
      Message msg = new Message(26);
      msg.writeByte(0);
      msg.writeInt(senderId);
      msg.writeString(text);
      return msg.toPacket();
   }

   /**
    * Opcode 26 sub 1: chat thế giới — int người gửi, str tên, str nội dung (client thêm vào tab "Thế giới" + chữ chạy
    * trên đầu màn hình). Mã hoá 1 lần, gửi mọi người.
    */
   public static Packet worldChat(int senderId, String senderName, String text) {
      Message msg = new Message(26);
      msg.writeByte(1);
      msg.writeInt(senderId);
      msg.writeString(senderName);
      msg.writeString(text);
      return msg.toPacket();
   }

   /** Opcode 23 sub 0 ({@code handlePlayerStats}): id + khối chỉ số — client đọc lại skeleton/skin/danh hiệu. */
   static Packet playerStats(WorldPlayer p) {
      Message msg = new Message(23);
      msg.writeByte(0);
      msg.writeInt(p.id());
      writeStats(msg, p.data(), p.stats(), p.locked, p.duelActiveOpponent());
      return msg.toPacket();
   }

   /** Opcode 4: người chơi rời khu (client xoá entity + hiệu ứng biến mất). */
   static Packet playerLeave(int playerId) {
      Message msg = new Message(4);
      msg.writeInt(playerId);
      return msg.toPacket();
   }

   /** Op 2 sub 7: đổi tên hiển thị (int id, str tên mới). */
   static Packet rename(int playerId, String name) {
      Message msg = new Message(2);
      msg.writeByte(7);
      msg.writeInt(playerId);
      msg.writeString(name);
      return msg.toPacket();
   }

   /** Op 2 sub 1: chỉ đổi skin (int id, byte n, n × str skin) — ẩn / hiện trang phục, mặt nạ, đeo lưng. */
   static Packet skins(int playerId, String[] skins) {
      Message msg = new Message(2);
      msg.writeByte(1);
      msg.writeInt(playerId);
      msg.writeByte(skins.length);
      for (String s : skins) {
         msg.writeString(s);
      }
      return msg.toPacket();
   }

   /** Op 12 sub 1: dịch chuyển tức thì 1 người (người khác thấy nhảy tới chỗ mới, không đi tới). */
   static Packet teleportOther(int playerId, float x, float y) {
      Message msg = new Message(12);
      msg.writeByte(1);
      msg.writeInt(playerId);
      msg.writeFloat(x);
      msg.writeFloat(y);
      return msg.toPacket();
   }

   /** Op 82: vị trí mới của {@code n} người từ {@code from} (client bỏ qua chính mình). */
   static Packet moveBatch(List<WorldPlayer> moved, int from, int n) {
      Message msg = new Message(com.tlnt.server.net.Opcode.MOVE_BATCH);
      msg.writeByte(n);
      for (int i = from; i < from + n; i++) {
         WorldPlayer p = moved.get(i);
         msg.writeInt(p.id());
         msg.writeFloat(p.x);
         msg.writeFloat(p.y);
      }
      return msg.toPacket();
   }

   /** Opcode 5 sub 0: đích di chuyển của người chơi khác (client tự đi tới). */
   static Packet playerMove(WorldPlayer p) {
      return entityMove(p.id(), p.x, p.y);
   }

   /** Opcode 5 sub 0 cho thực thể {@code id} bất kỳ (người chơi / sinh vật hộ tống). */
   static Packet entityMove(int id, float x, float y) {
      Message msg = new Message(5);
      msg.writeInt(id);
      msg.writeByte(0);
      msg.writeFloat(x);
      msg.writeFloat(y);
      return msg.toPacket();
   }

   /**
    * Opcode 2 sub 6 ({@code ServerPacketHandler.handlePlayerUpdate}): playerId + cờ khoá nhân vật
    * ({@code PlayerStats.isAlive} phía client, tên sai). Encode 1 lần rồi phát cho cả khu.
    */
   static Packet lockState(int playerId, boolean locked) {
      Message msg = new Message(2);
      msg.writeByte(6);
      msg.writeInt(playerId);
      msg.writeBoolean(locked);
      return msg.toPacket();
   }

   /** Opcode 42 sub 0 ({@code MessageHandler} case 42): trạng thái PK của 1 người (byte = {@code PkRules}). */
   static Message faction(int playerId, byte faction) {
      Message msg = new Message(42);
      msg.writeByte(0);
      msg.writeInt(playerId);
      msg.writeByte(faction);
      return msg;
   }

   /**
    * Opcode 41 sub 3: đếm ngược thách đấu (skeleton {@code ui/cooldown} trên HUD) — 3 / 2 / 1 = animation "3" / "2" /
    * "1", 0 = "Start".
    */
   static Message duelCountdown(int step) {
      Message msg = new Message(41);
      msg.writeByte(3);
      msg.writeByte(step);
      return msg;
   }

   /**
    * Opcode 2 sub 5 ({@code handlePlayerUpdate}): "Xem thông tin" 1 người — client mở {@code INFO_PLAYER_POPUP}:
    * khối chỉ số, đồ đang mặc (int n + n×item), rồi 4 bộ sưu tập ngoại trang / danh hiệu / mặt nạ / thú cưng, mỗi bộ
    * = int n + n×item, int món đang dùng (-1 = không), int m + m×(int statType, byte k + k×int) tổng chỉ số.
    * CHỈ gọi trên luồng session của người bị xem (đọc túi đồ của họ).
    */
   public static Message inspect(PlayerData d, boolean locked) {
      Message msg = new Message(2);
      msg.writeByte(5);
      writeStats(msg, d, d.stats != null ? d.stats : PlayerStats.of(d), locked, 0);
      List<ItemData> body = d.body.asList();
      msg.writeInt(body.size());
      for (ItemData item : body) {
         ItemPackets.writeItem(msg, item);
      }
      for (CollectionKind kind : CollectionKind.values()) {
         ItemPackets.writeCollectionForInspect(msg, kind.of(d));
      }
      return msg;
   }

   /**
    * Opcode 2 sub 3 ({@code handlePlayerUpdate}): người chơi mất máu KHÔNG kèm animation người đánh (chảy máu) —
    * int id, long sát thương, long HP còn (0 = chết).
    */
   static Packet playerDamaged(int playerId, long damage, long hpLeft) {
      Message msg = new Message(2);
      msg.writeByte(3);
      msg.writeInt(playerId);
      msg.writeLong(damage);
      msg.writeLong(hpLeft);
      return msg.toPacket();
   }

   /** Opcode 2 sub 8: đồng bộ HP nhân vật giả được hồi máu, không tạo số sát thương giả trên client. */
   static Packet playerHp(int playerId, long hp) {
      Message msg = new Message(2);
      msg.writeByte(8);
      msg.writeInt(playerId);
      msg.writeLong(Math.max(0, hp));
      return msg.toPacket();
   }

   /**
    * Opcode 41 sub 0 (client {@code isInParty / partyLeaderId}): bật = ĐANG THÁCH ĐẤU, tên người đó màu ĐỎ (decompile C3489,
    * {@code PlayerNameLabel}; cùng tổ đội = vàng theo partyId trong khối chỉ số, không qua gói này).
    * Số int đi kèm chỉ có tác dụng với nhân vật CỦA CHÍNH client: = id đối thủ thách đấu -> được chọn đối thủ làm mục
    * tiêu ({@code AutoTargetController.canTargetPlayer}); 0 = không ai (không có id 0) -> chỉ tô vàng. Dùng cho:
    * <ul>
    *   <li>thách đấu: phát cả khu, {@code opponentId} = đối thủ;</li>
    *   <li>(bỏ 2026-10-18) tổ đội: trước gửi RIÊNG cho đồng đội để tô vàng, {@code opponentId} = đối thủ nếu đang
    *       thách đấu, không thì 0 ({@link Zone#sendPartyFlags}).</li>
    * </ul>
    * Client tự tắt cờ khi người đó chết, và đọc lại từ khối chỉ số (op 3 / op 23 sub 0) -> gửi lại sau các gói đó.
    */
   static Packet duelFlag(int playerId, boolean on, int opponentId) {
      Message msg = new Message(41);
      msg.writeByte(0);
      msg.writeInt(playerId);
      msg.writeBoolean(on);
      if (on) {
         msg.writeInt(opponentId);
      }
      return msg.toPacket();
   }

   /** Dòng thứ 2 của hộp "Kiệt sức" (dưới dòng "Bạn đã bị X hạ gục"). */
   static final String DEATH_HINT = "Sử dụng 1 Cỏ hồi sinh để phục hồi hoặc quay trở về làng";

   /** Opcode 28 sub 0: chữ trong hộp "Kiệt sức" (client tự mở khi HP của mình ≤ 0): {@code text} + dòng {@link #DEATH_HINT}. */
   public static Message deathMessage(String text) {
      return deathMessage(text, DEATH_HINT);
   }

   /** Như trên, dòng 2 là {@code hint} (Lôi vực Sấm sét: "Tự động hồi sinh sau 5 giây"). */
   public static Message deathMessage(String text, String hint) {
      Message msg = new Message(28);
      msg.writeByte(0);
      msg.writeString(text + "\n[%75]" + hint); // dòng 2 chữ nhỏ (mã cỡ chữ Textra, như nút "Hồi sinh")
      return msg;
   }

   /** Opcode 28 sub 1: 1 người hồi sinh — client về tư thế đứng + hiệu ứng "revive". */
   static Packet revived(int playerId) {
      Message msg = new Message(28);
      msg.writeByte(1);
      msg.writeInt(playerId);
      return msg.toPacket();
   }

   /** Opcode 10: chuyển client từ màn tải map sang màn chơi. */
   /** Op 67 sub 0: dòng chữ + đồng hồ đếm ngược cạnh nút Thoát (client tự đếm từ {@code elapsedMs} tới {@code totalMs}). */
   static Message announcement(int id, String text, long elapsedMs, long totalMs) {
      Message msg = new Message(com.tlnt.server.net.Opcode.ANNOUNCEMENT);
      msg.writeByte(0);
      msg.writeInt(id);
      msg.writeString(text);
      msg.writeInt((int) Math.max(0, Math.min(Integer.MAX_VALUE, elapsedMs)));
      msg.writeInt((int) Math.max(0, Math.min(Integer.MAX_VALUE, totalMs)));
      return msg;
   }

   /** Op 67 sub 1: xoá dòng đếm ngược {@code id}. */
   static Message announcementRemove(int id) {
      Message msg = new Message(com.tlnt.server.net.Opcode.ANNOUNCEMENT);
      msg.writeByte(1);
      msg.writeInt(id);
      return msg;
   }

   static Message showWorld() {
      return new Message(10);
   }

   /**
    * Opcode 12 sub 2 ({@code PlayerMovementController.applyServerVelocity}): đặt vận tốc Box2D cho nhân vật của CHÍNH người
    * nhận ({@code vx}, {@code vy} m/s — client chia 100 nên gửi ×100; dương = phải / lên) và khoá điều khiển {@code ms}.
    * Dùng để hất tung / đẩy lùi; client tự rơi theo trọng lực rồi báo vị trí như thường.
    */
   static Packet knockback(float vx, float vy, int ms) {
      Message msg = new Message(12);
      msg.writeByte(2);
      msg.writeFloat(vx * 100f);
      msg.writeFloat(vy * 100f);
      msg.writeInt(ms);
      return msg.toPacket();
   }

   /** Opcode 12 sub 0: dịch chuyển nhân vật của chính mình (đổi map/khu, sửa vị trí sai). */
   static Message teleportSelf(float x, float y) {
      Message msg = new Message(12);
      msg.writeByte(0);
      msg.writeFloat(x);
      msg.writeFloat(y);
      return msg;
   }

   /** Opcode 25 sub 0: danh sách khu, client mở ZONE_POPUP. */
   static Message zoneList(List<GameMap.ZoneInfo> zones) {
      Message msg = new Message(25);
      msg.writeByte(0);
      msg.writeInt(zones.size());
      for (GameMap.ZoneInfo zone : zones) {
         msg.writeInt(zone.id());
         msg.writeByte(Math.min(zone.players(), Byte.MAX_VALUE));
         msg.writeByte(Math.min(zone.maxPlayers(), Byte.MAX_VALUE));
         msg.writeByte(Math.min(zone.parties(), Byte.MAX_VALUE)); // số tổ đội đang ở khu (cả nhóm của bot)
         msg.writeByte(loadLevel(zone));
      }
      return msg;
   }

   /** Màu khu trong popup (RegionInfoPopup.RegionType): 0 vắng, 1 đông, 2 đầy. */
   private static int loadLevel(GameMap.ZoneInfo zone) {
      if (zone.players() >= zone.maxPlayers()) {
         return 2;
      }
      return zone.players() * 2 >= zone.maxPlayers() ? 1 : 0;
   }

   // ------------------------------------------------------------------ trạng thái ban đầu khi vào game

   /**
    * Thể lực (opcode 23 sub 8) + item (22 sub 0/1/2/4/5/6/8/9 + chỉ số cộng thêm 48/50/64/66 sub 0) + kỹ năng (21)
    * + cài đặt (46). Gửi 1 lần sau khi client tạo nhân vật.
    */
   static void sendInitialState(WorldPlayer p) {
      PlayerData d = p.data();
      p.send(stamina(d.activityPoints, p.stats().maxStamina));
      p.send(dynamicPoints(d.dynamicPoints));
      p.send(ItemPackets.container(ItemPackets.SUB_BODY, d.body));
      if (FishingService.holdingRod(d)) { // HUD client mặc định chế độ thường
         p.send(FishingPackets.hudMode(true));
      }
      p.send(ItemPackets.container(ItemPackets.SUB_BAG, d.bag));
      p.send(ItemPackets.container(ItemPackets.SUB_BOX, d.box));
      for (CollectionKind kind : CollectionKind.values()) {
         ItemCollection collection = kind.of(d);
         p.send(ItemPackets.collection(kind, collection));
         p.send(ItemPackets.collectionBonus(kind, collection));
      }
      p.send(ItemPackets.quickSlots(d.quickSlots));
      p.send(ItemPackets.currency(d));
      if (!d.buffs.isEmpty()) { // hiệu ứng vật phẩm còn hạn (Nấm hương...) — thanh hiệu ứng HUD
         p.send(ItemBuffs.packet(d, System.currentTimeMillis()));
      }
      if (d.mailbox != null) { // hòm thư nạp lúc vào game (mail/MailService.onLogin)
         p.send(d.mailbox.packet(Mail.INBOX));
         p.send(d.mailbox.packet(Mail.SENT));
      }
      if (d.welfare != null) { // client chỉ cộng thêm tab -> đúng 1 lần mỗi lần vào game (nhánh này chạy 1 lần)
         p.send(WelfareService.fullPacket(d));
      }
      p.send(GiftcodeService.listPacket(d));
      p.send(RankingService.categoriesPacket());
      p.send(RankingService.weeklyEventsPacket(System.currentTimeMillis())); // tab "Top tuần" (op 73/0)
      p.send(learnedSkills(d.skills));
      p.send(skillBar(d.skillBar));
      int first = firstSkill(d.skillBar);
      if (first >= 0 && GameDataCache.skillTemplate(first) != null) {
         p.send(selectedSkill(first));
      }
      if (d.settings != null && d.settings.keybindings != null) {
         p.send(settings(d.settings));
      }
   }

   /** Opcode 23 sub 8: thể lực hiện tại / tối đa (thanh thứ 3 dưới HP, MP trên HUD). */
   public static Message stamina(int current, int max) {
      Message msg = new Message(23);
      msg.writeByte(8);
      msg.writeInt(current);
      msg.writeInt(max);
      return msg;
   }

   /** Opcode 23 sub 9: điểm năng động ("Điểm hoạt động" ở bảng nhân vật — khác thể lực sub 8). */
   public static Message dynamicPoints(int value) {
      Message msg = new Message(23);
      msg.writeByte(9);
      msg.writeInt(value);
      return msg;
   }

   // ------------------------------------------------------------------ kỹ năng (opcode 21)

   /**
    * Opcode 21 sub 1: kỹ năng đã học — byte số lượng, mỗi cái int skillId + byte level + long số ms đã trôi
    * qua từ lần dùng cuối (client: {@code new LearnedSkill(id, level, now - elapsed)}). Bỏ qua skill mà
    * dữ liệu tĩnh không có (client sẽ NPE khi tra template).
    */
   public static Message learnedSkills(List<SkillData> skills) {
      Message msg = new Message(21);
      msg.writeByte(1);
      int count = 0;
      if (skills != null) {
         for (SkillData skill : skills) {
            if (isKnownSkill(skill) && count < Byte.MAX_VALUE) {
               count++;
            }
         }
      }
      msg.writeByte(count);
      long now = System.currentTimeMillis();
      int written = 0;
      if (skills != null) {
         for (SkillData skill : skills) {
            if (written >= count || !isKnownSkill(skill)) {
               continue;
            }
            msg.writeInt(skill.skillId);
            msg.writeByte(skill.level);
            msg.writeLong(skill.lastUsedAt <= 0 ? now : Math.max(0, now - skill.lastUsedAt));
            written++;
         }
      }
      return msg;
   }

   private static boolean isKnownSkill(SkillData skill) {
      return skill != null && GameDataCache.skillTemplate(skill.skillId) != null;
   }

   /** Opcode 21 sub 2: thanh kỹ năng — byte số ô + int skillId theo vị trí ô (-1 = ô trống; client tra trong danh sách đã học). */
   public static Message skillBar(int[] skillIds) {
      Message msg = new Message(21);
      msg.writeByte(2);
      int length = skillIds == null ? 0 : Math.min(skillIds.length, Byte.MAX_VALUE);
      msg.writeByte(length);
      for (int i = 0; i < length; i++) {
         msg.writeInt(skillIds[i]);
      }
      return msg;
   }

   /** Opcode 21 sub 3: kỹ năng đang chọn để đánh. */
   /** Kỹ năng ở ô đầu tiên có kỹ năng, -1 nếu thanh trống. */
   private static int firstSkill(int[] bar) {
      if (bar != null) {
         for (int id : bar) {
            if (id >= 0) {
               return id;
            }
         }
      }
      return -1;
   }

   public static Message selectedSkill(int skillId) {
      Message msg = new Message(21);
      msg.writeByte(3);
      msg.writeInt(skillId);
      return msg;
   }

   /** Opcode 46: đúng thứ tự MessageHandler case 46. */
   public static Message settings(PlayerSettings s) {
      Message msg = new Message(46);
      for (int i = 0; i < PlayerSettings.AUTO_SKILL_SLOTS; i++) {
         msg.writeBoolean(i < s.autoSkillSlotEnabled.length && s.autoSkillSlotEnabled[i]);
      }
      msg.writeBoolean(s.autoIgnorePlayersAndBosses);
      msg.writeBoolean(s.autoAvoidEliteMobs);
      msg.writeBoolean(s.autoRevive);
      msg.writeBoolean(s.autoPickupItems);
      msg.writeBoolean(s.autoUseStaminaItems);
      msg.writeBoolean(s.autoUseHpItems);
      msg.writeBoolean(s.autoUseMpItems);
      msg.writeBoolean(s.autoHarvestApples);
      msg.writeByte(s.autoHpThresholdPercent);
      msg.writeByte(s.autoMpThresholdPercent);
      for (int i = 0; i < PlayerSettings.INVENTORY_FLAGS; i++) { // 5 cờ ẩn: ngoại trang, danh hiệu, mặt nạ, thú cưng, đeo lưng
         msg.writeBoolean(i < s.inventoryFlags.length && s.inventoryFlags[i]);
      }
      for (int i = 0; i < PlayerSettings.KEYBINDINGS; i++) {
         msg.writeInt(i < s.keybindings.length ? s.keybindings[i] : 0);
      }
      msg.writeBoolean(s.hideOtherPlayers);
      msg.writeInt(s.autoAttackRange); // "Phạm vi đánh" — cuối gói (client libGDX đọc nếu còn byte)
      return msg;
   }

   // ------------------------------------------------------------------ khối dùng chung

   /** Đúng thứ tự {@code ServerPacketHandler.readPlayerStats}. */
   static void writeStats(Message msg, PlayerData d, PlayerStats s, boolean locked, int duelOpponentId) {
      // Ngoại hình (áo, vũ khí, ngoại trang, mặt nạ, đeo lưng, danh hiệu) tính sẵn ở item/Appearance;
      // templateId chọn char_template ngoại trang nếu có, nếu không thì là template gốc theo giới tính.
      Appearance look = d.appearance != null ? d.appearance : Appearance.of(d);
      GamePack.CharTemplate template = GameDataCache.charTemplate(look.templateId());
      GamePack.Title title = d.titleId >= 0 ? GameDataCache.title(d.titleId) : null;
      String avatarIconPath = template != null && template.avatarIconPath() != null ? template.avatarIconPath() : "";
      String skeletonPath = template != null && template.skeletonPath() != null ? template.skeletonPath() : "";

      msg.writeByte(ENTITY_PLAYER);
      msg.writeString(d.name);
      Party party = d.party;
      msg.writeInt(party != null ? party.id() : -1); // tổ đội (client: cùng tổ đội thì PK phe A không đánh nhau)
      TribeTag tribe = d.tribe; // client: cùng guildId thì tên bộ lạc xanh + cờ Bộ lạc không nhắm nhau
      msg.writeInt(tribe != null ? tribe.id() : -1);
      msg.writeString(tribe != null ? tribe.name() : "");
      msg.writeString(tribe != null ? tribe.badge() : ""); // icon danh hiệu cạnh tên; rỗng với thành viên thường
      msg.writeString(avatarIconPath);
      msg.writeByte(d.classId);
      msg.writeByte(Gender.charTemplateId(Gender.isValid(d.gender) ? d.gender : Gender.RINN)); // jobData nhân vật gốc; ngoại hình gửi riêng
      // Thanh máu trên đầu: client hiện/ẩn đúng theo cờ này (PlayerHealthBar). Map thường ẩn; sau này bật
      // khi vào PK / đấu trường rồi gửi lại khối chỉ số (opcode 23 sub 0).
      msg.writeBoolean(false);
      msg.writeByte(d.faction);
      msg.writeBoolean(false); // là kẻ địch
      // Client đặt tên field này là isAlive nhưng thực chất = "đang bị khoá" (true: không di chuyển được,
      // ẩn HUD, camera tự do) -> bật/tắt bằng nút MountButton (opcode 2 sub 1).
      msg.writeBoolean(locked);
      // Client đặt tên isInParty / partyLeaderId nhưng thật ra = ĐANG THÁCH ĐẤU + id đối thủ (tên ĐỎ, được chọn đối
      // thủ làm mục tiêu — AutoTargetController.canTargetPlayer; chết thì client tự tắt).
      msg.writeBoolean(duelOpponentId != 0);
      if (duelOpponentId != 0) {
         msg.writeInt(duelOpponentId);
      }
      msg.writeString(skeletonPath);
      // Toàn bộ hình nhân vật nằm trong skin có tên (skin thân "skin/lv1" luôn có) -> sai tên là nhân vật vô hình.
      msg.writeByte(look.skins().length);
      for (String skin : look.skins()) {
         msg.writeString(skin);
      }
      msg.writeInt(title != null ? title.id() : -1); // Ấn (bảng title), khác danh hiệu item loại 20
      msg.writeString(look.titleKey()); // skeleton hiệu ứng danh hiệu trên đầu ("" = không có)
      // Cấp vũ khí đang cầm (1..5, từ skin "axe/lvN"...), client ghép "lv" + giá trị làm skin đạn; -1 = tay không.
      msg.writeByte(look.weaponLevel());
      msg.writeShort(d.level);
      msg.writeLong(d.exp);
      msg.writeLong(PlayerStats.expToNextLevel(d.level));
      msg.writeInt(d.talentPoints);
      writeInts(msg, d.talents);

      msg.writeLong(d.hp);
      msg.writeLong(s.maxHp);
      msg.writeLong(d.mp);
      msg.writeLong(s.maxMp);
      long[] rest = {s.attackPower, s.attackVsMob, s.critChance, s.critDamagePercent, s.armor, s.armorPen,
            s.evasion, s.accuracy, s.moveSpeed, s.hpRegen, s.mpRegen, s.hpLeechPercent, s.mpLeechPercent,
            s.hpLeechVsMobPercent, s.mpLeechVsMobPercent, s.reflectDamagePercent, s.stunDamage, s.weakenDamage,
            s.iceDamage, s.burnDamage, s.thunderDamage, s.critResistPercent, s.damageAmpPercent,
            s.damageAbsorbPercent, s.effectResistPercent, s.damageResist, s.goldBonusPercent, s.expBonusPercent};
      for (long value : rest) {
         msg.writeLong(value);
      }
   }

   /** Mảng int có độ dài 1 byte phía trước. */
   private static void writeInts(Message msg, int[] values) {
      int length = values == null ? 0 : Math.min(values.length, Byte.MAX_VALUE);
      msg.writeByte(length);
      for (int i = 0; i < length; i++) {
         msg.writeInt(values[i]);
      }
   }

   // ------------------------------------------------------------------ quái / chiến đấu

   /** Loại mục tiêu ở op 21 sub 0, cả 2 chiều (client gửi: 0 quái, 1 người chơi, -1 không có mục tiêu). */
   public static final int TARGET_MOB = 0;
   /** Loại mục tiêu người chơi (op 21 sub 0 C→S / S→C). */
   public static final int TARGET_PLAYER = 1;

   /**
    * Op 20 sub 0 (client {@code handleMobUpdate} + {@code readMobStats}): byte n (≤127, nhiều hơn thì tách gói —
    * client chỉ nối thêm), n × (int id, float x, float y, int mob_template.id, byte biến thể, int cấp, long hp,
    * long maxHp). hp ≤ 0 = đang chết, client ẩn. Client tự cho quái đi lang thang quanh điểm sinh
    * ({@code MobWanderAI}) -> server không gửi vị trí.
    */
   public static List<Message> mobList(List<MapTemplate.MobInstance> mobs, long[] hp) {
      List<Message> out = new ArrayList<>();
      for (int from = 0; from < mobs.size(); from += MapPackets.MAX_PER_PACKET) {
         int to = Math.min(mobs.size(), from + MapPackets.MAX_PER_PACKET);
         Message msg = new Message(20);
         msg.writeByte(0);
         msg.writeByte(to - from);
         for (int i = from; i < to; i++) {
            MapTemplate.MobInstance mob = mobs.get(i);
            msg.writeInt(mob.id());
            msg.writeFloat(mob.x());
            msg.writeFloat(mob.y());
            writeMobStats(msg, mob, hp[i]);
         }
         out.add(msg);
      }
      return out;
   }

   /** Op 20 sub 2: quái hồi sinh — client hiện lại ở điểm sinh + hiệu ứng xuất hiện. */
   public static Message mobRespawn(MapTemplate.MobInstance mob) {
      return mobRespawn(mob, mob.spawn().hp());
   }

   /** Như trên với HP hiện tại {@code hp} (hiện đúng siêu quái đang ẩn — client đọc lại chỉ số con quái đã có). */
   static Message mobRespawn(MapTemplate.MobInstance mob, long hp) {
      Message msg = new Message(20);
      msg.writeByte(2);
      msg.writeInt(mob.id());
      writeMobStats(msg, mob, hp);
      return msg;
   }

   /**
    * Op 20 sub 1 (client {@code handleMobUpdate} case 1): quái đánh người — int id quái, int id người, long HP người
    * còn, long sát thương, bool hồi máu. Client tự chọn animation: đứng gần (&lt; 1,5 đơn vị) đánh cận chiến
    * ({@code MOB_ATTACK_2}), xa hơn đánh tầm xa ({@code MOB_ATTACK_1}); sát thương 0 = chữ "né". Phát cả khu.
    */
   static Message mobAttack(int mobId, int playerId, long hpLeft, long damage) {
      Message msg = new Message(20);
      msg.writeByte(1);
      msg.writeInt(mobId);
      msg.writeInt(playerId);
      msg.writeLong(hpLeft);
      msg.writeLong(damage);
      msg.writeBoolean(false);
      return msg;
   }

   /**
    * Op 20 sub 5 (client {@code MobSkill3Anim}): quái quay theo {@code dir} (âm = trái) rồi chơi {@code skill3} (rung màn
    * hình mạnh ở 50%). Chỉ hình ảnh — sát thương gửi riêng ({@link #playerDamaged}). Boss Hắc thần Linh mộc.
    */
   static Message mobSkill3(int mobId, int dir) {
      Message msg = new Message(20);
      msg.writeByte(5);
      msg.writeInt(mobId);
      msg.writeByte(dir < 0 ? -1 : 1);
      return msg;
   }

   /** Op 20 sub 6 (client {@code MobSkill4Anim}): quái chơi {@code skill4} (rung nhẹ ở 50%). Chỉ hình ảnh. */
   static Message mobSkill4(int mobId) {
      Message msg = new Message(20);
      msg.writeByte(6);
      msg.writeInt(mobId);
      return msg;
   }

   /**
    * Op 20 sub 4: thêm 1 quái MỚI tại (x, y) (quái mọc ngẫu nhiên — tổ ong / quả dừa; mỗi lần mọc 1 id mới vì client
    * không dời được quái cũ sang chỗ khác).
    */
   static Message mobAdd(MapTemplate.MobInstance mob, long hp) {
      Message msg = new Message(20);
      msg.writeByte(4);
      msg.writeInt(mob.id());
      msg.writeFloat(mob.x());
      msg.writeFloat(mob.y());
      writeMobStats(msg, mob, hp);
      return msg;
   }

   // ---- vật phẩm dưới đất: op 30 (client handleMapItemUpdate), toạ độ pixel (client chia 100)

   /** Op 30 sub 0: danh sách vật phẩm của khu — int n, n × (int id, int item_template.id, float x, float y). */
   static Message groundItems(java.util.Collection<ZoneDrops.Drop> drops) {
      Message msg = new Message(Opcode.GROUND_ITEM);
      msg.writeByte(0);
      msg.writeInt(drops.size());
      for (ZoneDrops.Drop d : drops) {
         msg.writeInt(d.id);
         msg.writeInt(d.item.templateId);
         msg.writeFloat(d.x);
         msg.writeFloat(d.y);
      }
      return msg;
   }

   /** Op 30 sub 1: 1 vật phẩm vừa rơi. */
   static Message groundItemAdd(int id, int templateId, float x, float y) {
      Message msg = new Message(Opcode.GROUND_ITEM);
      msg.writeByte(1);
      msg.writeInt(id);
      msg.writeInt(templateId);
      msg.writeFloat(x);
      msg.writeFloat(y);
      return msg;
   }

   /** Op 30 sub 2: vật phẩm biến mất (hết hạn). */
   static Message groundItemRemove(int id) {
      Message msg = new Message(Opcode.GROUND_ITEM);
      msg.writeByte(2);
      msg.writeInt(id);
      return msg;
   }

   /** Op 30 sub 3: người {@code playerId} nhặt — vật phẩm bay vào người rồi client tự gỡ, âm "pick". */
   static Message groundItemPicked(int playerId, int id) {
      Message msg = new Message(Opcode.GROUND_ITEM);
      msg.writeByte(3);
      msg.writeInt(playerId);
      msg.writeInt(id);
      return msg;
   }

   /** Khối {@code readMobStats}: int mob_template.id, byte biến thể, int cấp, long hp, long maxHp. */
   private static void writeMobStats(Message msg, MapTemplate.MobInstance mob, long hp) {
      MapTemplate.MobSpawn spawn = mob.spawn();
      msg.writeInt(spawn.id());
      msg.writeByte(spawn.variant());
      msg.writeInt(spawn.level());
      msg.writeLong(hp);
      msg.writeLong(spawn.hp());
   }

   /**
    * Op 21 sub 0, đòn 1 mục tiêu (client {@code handleCombatAction}, nhánh theo skillId: 1 = Đánh Thường tay không
    * -> animation MELEE_NORMAL_ATTACK): int người đánh, int skillId, byte loại mục tiêu, int id mục tiêu, long HP còn,
    * long sát thương, bool hồi máu. Client tự chơi animation của người đánh (kể cả chính mình) + trừ máu; HP ≤ 0 thì
    * quái chơi animation chết rồi tự ẩn ({@code MobDieAnim}).
    */
   public static Message singleTargetHit(int attackerId, int skillId, int targetKind, int targetId, long hpLeft,
         long damage) {
      return singleTargetHit(attackerId, skillId, targetKind, targetId, hpLeft, damage, false);
   }

   /** Như trên, {@code crit} = đòn chí mạng (client hiện chữ chí mạng). */
   public static Message singleTargetHit(int attackerId, int skillId, int targetKind, int targetId, long hpLeft,
         long damage, boolean crit) {
      Message msg = skillAction(attackerId, skillId);
      writeTargetHit(msg, targetKind, targetId, hpLeft, damage, crit);
      return msg;
   }

   /**
    * Op 21 sub 0, kỹ năng 2 giai đoạn — nhánh chỉ có 1 bool:
    * <ul>
    *   <li>4 Trụ Tái Tạo: true = vào thế (lặp {@code axe/skill2_2}), false = kết thúc ({@code skill2_3}).</li>
    *   <li>7 Thịnh Nộ Trảm (và 13 / 19 / 25 / 31): true = gồng (lặp {@code skill5_2}); kết thúc là
    *       {@link #chargedHit}.</li>
    * </ul>
    * Client lặp animation giai đoạn đầu mãi tới khi nhận gói kết thúc -> server LUÔN phải gửi gói kết thúc.
    */
   public static Message skillPhase(int attackerId, int skillId, boolean start) {
      Message msg = skillAction(attackerId, skillId);
      msg.writeBoolean(start);
      return msg;
   }

   /** Op 21 sub 0, kết thúc chiêu gồng (bool false) + đòn 1 mục tiêu như {@link #singleTargetHit}. */
   public static Message chargedHit(int attackerId, int skillId, int targetKind, int targetId, long hpLeft,
         long damage, boolean crit) {
      Message msg = skillAction(attackerId, skillId);
      msg.writeBoolean(false);
      writeTargetHit(msg, targetKind, targetId, hpLeft, damage, crit);
      return msg;
   }

   /**
    * Op 21 sub 0, đòn diện rộng (5 Cuồng Phong Trảm): byte n quái × (int id, long HP còn, long sát thương, bool hồi
    * máu), byte m người chơi × (như trên). Client hiện hiệu ứng trúng đòn lúc BẮT ĐẦU animation.
    */
   static Message areaHit(int attackerId, int skillId, HitList mobs, HitList players) {
      Message msg = skillAction(attackerId, skillId);
      writeHits(msg, mobs);
      writeHits(msg, players);
      return msg;
   }

   /** byte n × (int id, long HP còn, long sát thương, bool hồi máu). */
   private static void writeHits(Message msg, HitList hits) {
      msg.writeByte(hits.count);
      for (int i = 0; i < hits.count; i++) {
         msg.writeInt(hits.ids[i]);
         msg.writeLong(hits.hpLeft[i]);
         msg.writeLong(hits.damage[i]);
         msg.writeBoolean(hits.crit[i]); // chí mạng
      }
   }

   /**
    * Op 21 sub 0, 1 mục tiêu chính + mục tiêu phụ (11 Tán Diệt Tiễn, 17 Xung Kích Nỗ): khối 1 mục tiêu như
    * {@link #singleTargetHit}, rồi byte n quái phụ × (int id, long HP còn, long sát thương, bool hồi máu), byte m người
    * chơi phụ × (như trên).
    */
   static Message splashHit(int attackerId, int skillId, int mainKind, int mainId, long mainHpLeft, long mainDamage,
         boolean mainCrit, HitList mobs, HitList players) {
      Message msg = skillAction(attackerId, skillId);
      writeTargetHit(msg, mainKind, mainId, mainHpLeft, mainDamage, mainCrit);
      writeHits(msg, mobs);
      writeHits(msg, players);
      return msg;
   }

   /**
    * Op 21 sub 0 nhánh 28 (Ảo Ảnh Phách): float thời gian lướt (giây), float tốc độ (đơn vị thế giới / giây = px / 100).
    * Client ({@code DashImpulseController}) tự đẩy nhân vật của mình về phía đang nhìn, tốc độ giảm dần về 0 -> quãng
    * lướt ≈ tốc độ × thời gian / 2.
    */
   public static Message dash(int attackerId, int skillId, float seconds, float speed) {
      Message msg = skillAction(attackerId, skillId);
      msg.writeFloat(seconds);
      msg.writeFloat(speed);
      return msg;
   }

   /** Op 35 bản cho quái: có thời gian = int id, bool bật, int ms; tức thì (sét) = int id. */
   /** Op 35 bản NGƯỜI CHƠI (sub chẵn = sub quái − 1) — dùng cho boss Tế đàn (người chơi giả). */
   static Message playerStatus(MobStatus status, int playerId, int durationMs) {
      Message msg = new Message(35);
      msg.writeByte(status.sub - 1);
      msg.writeInt(playerId);
      if (status.timed) {
         msg.writeBoolean(true);
         msg.writeInt(durationMs);
      }
      return msg;
   }

   /** Op 35 bản người chơi, bool false = tắt hiệu ứng có thời gian ({@code stopRoot / stopIceSpike}...). */
   static Message playerStatusOff(MobStatus status, int playerId) {
      Message msg = new Message(35);
      msg.writeByte(status.sub - 1);
      msg.writeInt(playerId);
      msg.writeBoolean(false);
      msg.writeInt(0);
      return msg;
   }

   public static Message mobStatus(MobStatus status, int mobId, int durationMs) {
      Message msg = new Message(35);
      msg.writeByte(status.sub);
      msg.writeInt(mobId);
      if (status.timed) {
         msg.writeBoolean(true);
         msg.writeInt(durationMs);
      }
      return msg;
   }

   /**
    * Op 80 sub 0 (client {@code FloatAwayEffect}, decompile C3873 m18077 / C3524): vật thể trôi — int id, int mẫu
    * {@code map_effect}, float x, y, mốc x biến mất (pixel, client chia 100), float tốc (đơn vị thế giới / s = px / 100,
    * KHÔNG chia). Trôi sang phải + nhấp nhô, qua mốc thì client tự xoá. Phát cho cả khu.
    */
   /**
    * Op 81 sub 1 (client {@code MapPropEntity}, decompile C3873 m18092 / C3531): 1 vật trang trí — int id, str ảnh, float
    * x, y (pixel, client chia 100; ảnh vẽ giữa-dưới tại điểm này). Vd củ cà rốt ném cho Ngọc thố.
    */
   static Packet mapPropAdd(int id, String texture, float x, float y) {
      Message msg = new Message(Opcode.MAP_PROP);
      msg.writeByte(1);
      msg.writeInt(id);
      msg.writeString(texture);
      msg.writeFloat(x);
      msg.writeFloat(y);
      return msg.toPacket();
   }

   /** Op 81 sub 0: danh sách vật trang trí cho người vào khu — int n, n × (int id, str ảnh, float x, y). */
   static Message mapPropList(java.util.List<Object[]> props) {
      Message msg = new Message(Opcode.MAP_PROP);
      msg.writeByte(0);
      msg.writeInt(props.size());
      for (Object[] p : props) {
         msg.writeInt((Integer) p[0]);
         msg.writeString((String) p[1]);
         msg.writeFloat((Float) p[2]);
         msg.writeFloat((Float) p[3]);
      }
      return msg;
   }

   /** Op 81 sub 2: gỡ 1 vật trang trí. */
   static Packet mapPropRemove(int id) {
      Message msg = new Message(Opcode.MAP_PROP);
      msg.writeByte(2);
      msg.writeInt(id);
      return msg.toPacket();
   }

   static Packet floatAway(int id, int effectId, float x, float y, float endX, float speed) {
      Message msg = new Message(Opcode.FLOAT_AWAY);
      msg.writeByte(0);
      msg.writeInt(id);
      msg.writeInt(effectId);
      msg.writeFloat(x);
      msg.writeFloat(y);
      msg.writeFloat(endX);
      msg.writeFloat(speed);
      return msg.toPacket();
   }


   private static Message skillAction(int attackerId, int skillId) {
      Message msg = new Message(21);
      msg.writeByte(0);
      msg.writeInt(attackerId);
      msg.writeInt(skillId);
      return msg;
   }

   private static void writeTargetHit(Message msg, int targetKind, int targetId, long hpLeft, long damage) {
      writeTargetHit(msg, targetKind, targetId, hpLeft, damage, false);
   }

   /**
    * Khối 1 mục tiêu: byte loại, int id, long HP còn, long sát thương, bool CHÍ MẠNG (client đặt tên sai {@code isHeal}:
    * true = chữ font chí mạng {@code crit/crit} to ×1,5 — {@code FloatingCombatText} kiểu "HEAL" thật ra là chí mạng).
    */
   private static void writeTargetHit(Message msg, int targetKind, int targetId, long hpLeft, long damage,
         boolean crit) {
      msg.writeByte(targetKind);
      msg.writeInt(targetId);
      msg.writeLong(hpLeft);
      msg.writeLong(damage);
      msg.writeBoolean(crit);
   }

   /**
    * Op 20 sub 3: quái mất máu KHÔNG kèm animation người đánh (chảy máu...): int id, long sát thương, long HP còn.
    * Client hiện số + animation trúng đòn / chết.
    */
   public static Message mobDamaged(int mobId, long damage, long hpLeft) {
      Message msg = new Message(20);
      msg.writeByte(3);
      msg.writeInt(mobId);
      msg.writeLong(damage);
      msg.writeLong(hpLeft);
      return msg;
   }

   /** Op 21 sub 4: bắt đầu hồi chiêu ở client ({@code LearnedSkill.lastCastTime = now - elapsed}). */
   public static Message skillCooldown(int skillId, long elapsedMillis) {
      Message msg = new Message(21);
      msg.writeByte(4);
      msg.writeInt(skillId);
      msg.writeLong(elapsedMillis);
      return msg;
   }
}
