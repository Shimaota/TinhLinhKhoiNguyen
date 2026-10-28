package com.tlnt.server.world;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packet;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.session.PlayerSession;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Người chơi đang ở trong thế giới: gắn {@link PlayerData} (lưu DB) với trạng thái chỉ có lúc chạy
 * (khu hiện tại, vị trí đã kiểm tra, hướng mặt...). Khu và vị trí KHÔNG lưu khi thoát; riêng map + toạ độ
 * được chép vào {@link PlayerData} để lần sau vào lại đúng chỗ.
 *
 * Mọi field thường chỉ đọc/ghi trên luồng tick của khu đang chứa người chơi. Luồng mạng chỉ đụng tới
 * {@link #reportPosition} (atomic) và các cờ volatile.
 */
public final class WorldPlayer {

   /**
    * 28 Ảo Ảnh Phách: id ảnh ảo đang chạy (0 = không) — trong lúc đó chủ TÀNG HÌNH (op 2 sub 2: người khác không thấy, bản
    * thân mờ 20%). Tự gây đòn đánh -> hiện hình + ảnh ảo biến mất ({@code ZoneCombat.breakStealth}). Luồng tick khu.
    */
   int illusionId;

   /** Chưa có vị trí mới chờ xử lý. */
   private static final long NO_POSITION = Long.MIN_VALUE;

   private final PlayerSession session;
   private final PlayerData data;

   /** Khu đang chứa; null khi đang chuyển khu/map. */
   volatile Zone zone;
   /** Đã thoát game — lệnh vào khu còn đang xếp hàng phải huỷ. */
   volatile boolean removed;
   /** Đòn vừa tính của người này là chí mạng ({@code ZoneCombat.critical}) — gói trúng đòn đọc ngay sau. Chỉ luồng tick khu. */
   boolean lastCrit;
   /** Kỹ năng của đòn đang tính ({@code ZoneCombat.using}) — chiêu cuối không chí mạng. Chỉ luồng tick khu. */
   int hitSkillId;
   /** Client đã có nhân vật của mình (opcode 2 sub 0 đã gửi) -> lần đổi map sau chỉ cần dịch chuyển. */
   boolean spawnedOnClient;

   float x;
   float y;
   boolean facingLeft;
   long lastMoveAt;
   /**
    * "Ngân sách" di chuyển còn lại (px) — chống hack tốc độ ({@link Zone} applyMovement): hồi theo thời gian, đi tới đâu
    * trừ tới đó, đầy tối đa 1 lần lướt / nhảy. Vào khu = đầy.
    */
   float moveBudgetX = Zone.BURST_X_PX;
   float moveBudgetUp = Zone.BURST_UP_PX;
   float moveBudgetDown = Zone.BURST_DOWN_PX;
   /** Bị server đẩy / hất tung tới lúc này (+ thời gian rơi): bỏ kiểm tra ngân sách di chuyển ({@code Zone.spendMoveBudget}). */
   long knockedUntil;
   /** Lần đổi khu/dịch chuyển gần nhất (chống bấm liên tục). */
   long lastTransferAt;
   /**
    * Chế độ khoá nhân vật / ngắm cảnh (nút MountButton, opcode 2 sub 1): nhân vật đứng yên, HUD ẩn, camera tự
    * do; người khác không chọn được mục tiêu là mình. Không lưu DB — vào game lại luôn tắt.
    */
   boolean locked;
   /** Mốc "đứng im" toàn server cho cáo trộm ({@link World#foxCandidates}) — luồng scheduler ghi. */
   volatile float stillX = Float.NaN;
   volatile float stillY;
   volatile long stillSince;
   volatile Zone stillZone;
   /** Lần bật/tắt {@link #locked} gần nhất (chống spam nút). */
   long lastLockToggleAt;
   /** Đối thủ thách đấu đang đấu (0 = không) + id trận ({@link World#startDuel}). Chỉ luồng tick khu. */
   int duelOpponentId;
   long duelId;
   /** Cây đang hái (id thực thể, -1 = không) + số lượt hái (việc hẹn giờ của lượt cũ tự bỏ). Chỉ luồng tick khu. */
   int harvestPlantId = -1;
   long harvestSeq;
   /** Đã hết đếm ngược ("Bắt đầu") -> 2 người đánh được nhau. */
   boolean duelActive;
   /** Khu đấu Giải đấu Bộ lạc: người đánh trúng mình gần nhất + lúc đó (rơi đài thì người này hạ gục). Chỉ luồng tick. */
   int arenaHitBy;
   long arenaHitAt;
   /** Lần gần nhất kéo lại chỗ cũ vì đang choáng / đóng băng mà client vẫn gửi di chuyển. Chỉ luồng tick. */
   long lastPinnedAt;

   /** Id đối thủ nếu đang đấu thật (sau đếm ngược), 0 = không — ghi vào khối chỉ số + op 41 sub 0. */
   int duelActiveOpponent() {
      return duelActive ? duelOpponentId : 0;
   }

   /** Đang đấu thật với {@code other}. */
   boolean dueling(WorldPlayer other) {
      return duelActive && other.duelActive && duelOpponentId == other.id() && other.duelOpponentId == id();
   }

   /** Id thực thể thú cưng: rất âm, không trùng người thật (id DB > 0) hay ảnh ảo (-1, -2...). */
   private static final int PET_ID_BASE = Integer.MIN_VALUE / 2;
   /** Thú cưng đang hiện ở client (null = không có). Chỉ luồng tick khu. */
   Pet pet;

   int petId() {
      return PET_ID_BASE - id();
   }

   /** Id thực thể sinh vật hộ tống: âm, ngoài vùng id thú cưng của client (≤ PET_ID_BASE) -> client nội suy op 5. */
   private static final int ESCORT_ID_BASE = Integer.MIN_VALUE / 4;
   /** Sinh vật đang hộ tống (bước nhiệm vụ hộ tống), null = không có. Đi theo người qua các khu. Chỉ luồng tick khu. */
   Escort escort;

   int escortId() {
      return ESCORT_ID_BASE - id();
   }

   /** Số lần hồi sinh trong phiên — mỗi lượt sống là 1 "mục tiêu mới" với Nanh Hổ răng kiếm. Chỉ luồng tick khu. */
   int lives;
   /** Nanh Hổ răng kiếm: mục tiêu đã đánh ({@link ZoneCombat} key), giữ tối đa {@link #MAX_FIRST_HITS} gần nhất. */
   private java.util.LinkedHashSet<Long> firstHits;
   private static final int MAX_FIRST_HITS = 128;

   /** Ghi nhận đánh mục tiêu {@code key}. @return true nếu là đòn ĐẦU TIÊN lên mục tiêu đó. Chỉ luồng tick khu. */
   boolean firstHit(long key) {
      if (firstHits == null) {
         firstHits = new java.util.LinkedHashSet<>();
      }
      if (!firstHits.add(key)) {
         return false;
      }
      if (firstHits.size() > MAX_FIRST_HITS) {
         java.util.Iterator<Long> it = firstHits.iterator();
         it.next();
         it.remove();
      }
      return true;
   }

   /** Vị trí trong danh sách người vừa di chuyển của lượt phát hiện tại (-1 = không di chuyển). Chỉ Zone dùng. */
   int moveIndex = -1;
   /** Đã đi từ lần phát vị trí trước (đang trong danh sách chờ phát của khu). Chỉ Zone dùng. */
   boolean moveDirty;
   /** Luyện tập Offline: server tự chạy nhân vật ({@link OfflineWalker}); null = người chơi tự điều khiển. Chỉ luồng tick khu. */
   OfflineWalker offlineWalker;
   /**
    * Gói "xuất hiện" (opcode 3) mã hoá sẵn cho người khác vào khu; null = phải dựng lại. Mọi thay đổi có trong gói
    * (vị trí, hướng, cờ khoá, chỉ số) phải gọi {@link #invalidateSpawnPacket()}.
    */
   private Packet spawnPacket;

   /** Vị trí client báo gần nhất, đóng gói 2 float vào 1 long để cập nhật không cần khoá. */
   private final AtomicLong pendingPosition = new AtomicLong(NO_POSITION);

   WorldPlayer(PlayerSession session, PlayerData data) {
      this.session = session;
      this.data = data;
   }

   public int id() {
      return data.getId();
   }

   public PlayerSession session() {
      return session;
   }

   public PlayerData data() {
      return data;
   }

   /** Chỉ số tính sẵn ({@link PlayerData#stats}, đổi khi mặc/tháo đồ — xem ItemService). */
   public PlayerStats stats() {
      return data.stats;
   }

   /** Gói opcode 3 của mình (dựng 1 lần, dùng lại tới khi có thay đổi). Chỉ gọi trên luồng tick của khu. */
   Packet spawnPacket() {
      Packet packet = spawnPacket;
      if (packet == null) {
         packet = WorldPackets.playerSpawn(this);
         spawnPacket = packet;
      }
      return packet;
   }

   void invalidateSpawnPacket() {
      spawnPacket = null;
   }

   public Zone zone() {
      return zone;
   }

   public float x() {
      return x;
   }

   public float y() {
      return y;
   }

   public boolean facingLeft() {
      return facingLeft;
   }

   /** Đang ở chế độ khoá nhân vật / ngắm cảnh (đọc từ luồng khác chỉ để hiển thị). */
   public boolean locked() {
      return locked;
   }

   public void send(Message message) {
      session.send(message);
   }

   public void send(Packet packet) {
      session.send(packet);
   }

   void sendFrames(byte[] frames, int offset, int length) {
      session.sendFrames(frames, offset, length);
   }

   // ------------------------------------------------------------------ vị trí từ client (luồng mạng)

   /** Client gửi vị trí mỗi frame; chỉ giữ lại vị trí mới nhất, tick của khu sẽ lấy ra xử lý. */
   void reportPosition(float px, float py) {
      // +0f đổi -0f thành 0f để không bao giờ đóng gói ra đúng giá trị NO_POSITION.
      long packed = ((long) Float.floatToRawIntBits(px + 0f) << 32) | (Float.floatToRawIntBits(py + 0f) & 0xFFFFFFFFL);
      pendingPosition.set(packed);
   }

   /** @return true nếu có vị trí mới; kết quả ghi vào {@code out[0..1]}. */
   boolean takePendingPosition(float[] out) {
      long packed = pendingPosition.getAndSet(NO_POSITION);
      if (packed == NO_POSITION) {
         return false;
      }
      out[0] = Float.intBitsToFloat((int) (packed >>> 32));
      out[1] = Float.intBitsToFloat((int) packed);
      return true;
   }

   void clearPendingPosition() {
      pendingPosition.set(NO_POSITION);
   }
}
