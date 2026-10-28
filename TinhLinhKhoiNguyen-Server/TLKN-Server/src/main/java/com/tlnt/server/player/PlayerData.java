package com.tlnt.server.player;

import com.tlnt.server.item.Appearance;
import com.tlnt.server.item.ItemCollection;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.party.Party;
import com.tlnt.server.social.FriendEntry;
import com.tlnt.server.trade.Trade;

import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu 1 nhân vật nằm trong RAM suốt thời gian online (login -> load SQL -> cache ->
 * thoát/tắt server -> ghi SQL). Chỉ lưu NGUỒN; chỉ số chiến đấu cuối (HP, tấn công...) tính lại từ nguồn
 * ({@link PlayerStats#of}): chỉ số gốc nhân vật.
 *
 * Map sang DB: {@link PlayerRepository}. Tên field bám theo tên phía client để dễ đối chiếu.
 */
public final class PlayerData {

   /** hp/mp = -1: chưa tính, lần vào map đầu tiên sẽ đặt bằng max. */
   public static final long FULL = -1;

   // ---------------------------------------------------------------- định danh
   private int id;
   private final int accountId;
   public String name;
   /**
    * Người chơi giả ({@code bot/BotService}): không tài khoản, KHÔNG lưu DB ({@link PlayerService#saveAsync} bỏ qua),
    * không tính nhiệm vụ / thành tựu / NV hằng ngày / xếp hạng. Mọi luật còn lại (đánh, EXP, đồ rơi) là của người thật.
    */
   public transient boolean bot;
   /** Giới tính / nhân vật gốc: 1 Rinn, 2 Lyria, 3 Bruk ({@link Gender}; char_template mặc định ghi cứng ở đó). */
   public int gender;
   /**
    * Class = loại vũ khí ({@code item/WeaponClass}): 1 Chưa vũ khí, 2 Rìu, 3 Cung tên, 4 Ná thun, 5 Giáo, 6 Roi da.
    * Chọn 1 lần duy nhất khi trang bị vũ khí đầu tiên.
    */
   public int classId;

   // ---------------------------------------------------------------- tiến trình
   public int level;
   public long exp;
   /**
    * Điểm sát khí (0..{@code social/PkRules.MAX_PK_POINTS}): +1 mỗi lần hạ gục người khác khi đang cờ Thử thách (ngoài
    * Lôi vực / thách đấu); ≥ 20 giảm 50%, ≥ 50 giảm 75% mọi chỉ số chiến đấu ({@code PkRules.applyPenalty}); mỗi 60 phút
    * giảm 1 ({@code social/PkPoints}). Ghi dưới {@code synchronized (this)}.
    */
   public int pkPoints;
   /** Mốc tính giảm sát khí (ms): lần hạ gục bằng cờ Thử thách / lần giảm gần nhất (cột pk_points_at). */
   public long pkPointsAt;
   /** Huy hiệu Tinh Linh còn hạn tới (epoch ms, 0 = chưa có — cột spirit_badge_until, {@code badge/SpiritBadgeService}). */
   public long spiritBadgeUntil;
   /** Client đang hiện nút Tiện ích (đã gửi op 45/0 bật) — để báo tắt đúng 1 lần khi hết hạn. Chỉ luồng session. */
   public transient boolean spiritBadgeShown;
   /**
    * HP / MP hiện tại. Nhiều luồng sửa (luồng session: bình máu, kỹ năng; luồng tick khu: bị người khác đánh, hút máu)
    * -> GHI luôn trong {@code synchronized (this)} (các method dưới hoặc khối synchronized), đọc thì không khoá. HP ≤ 0
    * = đã bị hạ gục (chờ hồi sinh, op 28).
    */
   public volatile long hp;
   public volatile long mp;
   /**
    * Vàng KHÔNG khoá (ô tiền tệ 1 của client): giao dịch, gửi thư (phí), kí gửi (mua / phí) đều dùng loại này.
    * Chỉ luồng session sửa (giao dịch chốt trên luồng người kia nhưng lúc đó túi + vàng đang đóng băng).
    */
   public long gold;
   /** Vàng khoá (ô tiền tệ 2): phần thưởng phúc lợi (item 1 khoá); không giao dịch / gửi / kí gửi được. */
   public long lockedGold;
   /**
    * Lam thạch (ô tiền tệ 3, op 22 sub 3): mua shop NPC ({@code cur} 2), quà item 2. Nạp tiền (làm sau) cộng vào đây +
    * {@link #rechargeTotal}. Chỉ luồng session sửa (trừ dưới {@code synchronized (this)}).
    */
   public long diamond;
   /** Tổng lam thạch đã nạp (mốc "Quà nạp" ở phúc lợi). Logic nạp làm sau — hiện chỉ lưu. */
   public long rechargeTotal;
   /**
    * Điểm năng động (client "Điểm hoạt động" ở bảng nhân vật, op 23 sub 9) — KHÁC thể lực {@link #activityPoints}.
    * Kí gửi món thứ 6 trở lên (đang treo) tốn điểm này; nguồn cộng: {@link ActivityPoints}. Sửa dưới khoá {@code this}.
    */
   public int dynamicPoints;
   /** Kí gửi (luật cũ: lượt đăng bán trong ngày) — KHÔNG còn dùng (2026-10-10 đổi sang đếm món đang treo), giữ cột DB. */
   public int marketSellDay;
   public int marketSellCount;
   /** Số liệu bảng xếp hạng không có cột riêng (cột JSON {@code ranking}). */
   public RankStats rankStats = new RankStats();
   /**
    * Giftcode đã nhập: mã (chữ hoa) -> số lần (cột JSON {@code giftcodes}). Lưu cùng dòng nhân vật -> túi đồ + lượt
    * nhập luôn được ghi cùng lúc. Chỉ luồng session.
    */
   public java.util.Map<String, Integer> giftcodes = new java.util.HashMap<>();
   /** Gói combo đã mua: "idDanhMục:vịTríGói" -> số lần (cột JSON {@code combos}, {@code combo/ComboService}). */
   public java.util.Map<String, Integer> comboBought = new java.util.HashMap<>();
   /** Chống bấm liên tục: lần mua combo / quay vòng quay gần nhất. */
   public transient long lastComboBuyAt;
   public transient long lastWheelSpinAt;
   /** Chống dò giftcode: lần nhập gần nhất + số lần nhập sai liên tiếp. */
   public transient long lastGiftcodeAt;
   public transient int giftcodeFails;
   /** Lần xin bảng xếp hạng gần nhất (chống spam). */
   public transient long lastRankingAt;
   /** Lần xin bảng Top tuần (op 73) gần nhất — chặn gói dồn dập. */
   public transient long lastWeeklyTopAt;
   /** Tiến độ phúc lợi: online hôm nay, điểm danh tuần, mốc đã nhận (cột welfare, JSON). */
   public com.tlnt.server.welfare.WelfareState welfare;
   /**
    * Hòm thư (op 40): nạp từ SQL lúc vào game ({@code mail/MailService}), từ đó RAM là bản chuẩn của CHÍNH chủ, mọi
    * thay đổi ghi SQL qua db-writer. Không lưu trong cột player.
    */
   public transient com.tlnt.server.mail.Mailbox mailbox;
   /** Lần gửi thư / thao tác kí gửi gần nhất (chống spam). Chỉ luồng session. */
   public transient long lastMailAt;
   public transient long lastMarketAt;
   /**
    * Thể lực hiện tại (opcode 23 sub 8); tối đa = PlayerStats.maxStamina. Mỗi lần ra chiêu / đánh -{@link #STAMINA_PER_SKILL}
    * ({@link #spendStamina}), bị hạ gục -{@link #STAMINA_DEATH_LOSS} ({@link #damageHp}); 0 = không đánh được. Ghi dưới khoá
    * {@code this} (luồng session + luồng tick khu cùng sửa).
    */
   public int activityPoints;
   public static final int STAMINA_PER_SKILL = 1;
   public static final int STAMINA_DEATH_LOSS = 50;
   /** Lần báo "Thể lực đã cạn" gần nhất (chống spam khi client tự đánh). Chỉ luồng session. */
   public transient long staminaNoticeAt;
   /**
    * Vũ khí đang chờ xác nhận "lần đầu dùng vũ khí" (opcode 32): ô túi + mẫu item lúc hỏi; -1 = không chờ.
    * KHÔNG lưu DB.
    */
   public transient int pendingWeaponSlot = -1;
   public transient int pendingWeaponTemplateId = -1;
   /** Ô túi của Bùa tẩy điểm đang chờ xác nhận (op 32 loại 14); -1 = không chờ. KHÔNG lưu DB. */
   public transient int pendingResetSlot = -1;
   /** Bộ sưu tập đang chờ xác nhận mở thêm 1 ô (op 32) + số ô lúc hỏi; null = không chờ. KHÔNG lưu DB. */
   public transient com.tlnt.server.item.CollectionKind pendingExpandKind;
   public transient int pendingExpandCapacity;
   /**
    * Trạng thái PK ({@code social/PkRules}, FactionStatus client). KHÔNG lưu DB — mỗi lần vào game về hoà bình. Chỉ luồng
    * tick khu ghi (cùng luồng dựng gói xuất hiện).
    */
   public transient byte faction;
   /** Lúc vào trạng thái PK hiện tại (ms, 0 = chưa đổi lần nào) — chống đổi liên tục. Chỉ luồng tick khu. */
   public transient long factionChangedAt;
   /** {@code title.id} đang đeo, -1 = không. */
   public int titleId = -1;
   public int talentPoints;
   /** Điểm đã cộng vào từng tiềm năng, index theo thứ tự bảng {@code talent}. */
   public int[] talents = new int[0];
   // ---------------------------------------------------------------- vị trí
   public int mapId;
   /** null = dùng điểm xuất hiện mặc định của map. */
   public Float x;
   public Float y;

   // ---------------------------------------------------------------- item (opcode 22, logic ở item/ItemService)
   /** Số ô mặc định hành trang / rương khi tạo nhân vật. */
   public static final int DEFAULT_BAG_SIZE = 50;
   public static final int DEFAULT_BOX_SIZE = 25;
   /** Số ô đồ mặc = số equip_slot (0..9) bảng đồ client vẽ ({@code EquipmentPaperdollPanel}). */
   public static final int BODY_SLOTS = 10;

   /** sub 1: hành trang (cột item_bag). */
   public ItemContainer bag = new ItemContainer(DEFAULT_BAG_SIZE);
   /** sub 2: rương (cột item_box). */
   public ItemContainer box = new ItemContainer(DEFAULT_BOX_SIZE);
   /** sub 0: đồ đang mặc, ô = item_type.equip_slot (cột item_body). */
   public ItemContainer body = new ItemContainer(BODY_SLOTS);
   /** sub 4 / 6 / 8 / 9: ngoại trang, danh hiệu, mặt nạ, thú cưng (+ món đang dùng). */
   public ItemCollection costumes = new ItemCollection();
   public ItemCollection titles = new ItemCollection();
   public ItemCollection masks = new ItemCollection();
   public ItemCollection pets = new ItemCollection();
   /**
    * Bộ sưu tập đeo lưng (loại 48, tab "Đeo lưng"): chỉ số = tổng cả bộ như 4 bộ sưu tập kia, món đang dùng
    * ({@link #back()}) quyết định skin trên lưng. Cột {@code item_back}.
    */
   public ItemCollection backs = new ItemCollection();

   /** Món đeo lưng đang dùng (null = không đeo). */
   public ItemData back() {
      return backs.getSelected();
   }
   /** sub 5: 4 ô dùng nhanh [HP, MP, thể lực, dự phòng], giá trị = ô trong {@link #bag}, -1 = trống. */
   public int[] quickSlots = {-1, -1, -1, -1};

   /**
    * Chỉ số cuối + ngoại hình, TÍNH SẴN trên luồng session (vào game / mặc-tháo đồ) rồi công bố volatile; luồng
    * tick khu chỉ đọc (gửi gói xuất hiện / chỉ số). Không lưu DB.
    */
   public transient volatile PlayerStats stats;
   /** Đã vào map xong lần đầu (Zone.join gửi xong trạng thái ban đầu) -> từ đây chỉ luồng session đụng túi đồ. */
   public transient volatile boolean inWorld;
   /**
    * Hạn sớm nhất trong mọi kho (epoch ms), {@link Long#MAX_VALUE} = không có món nào có hạn. Luồng session tính,
    * {@code ItemExpiryTask} chỉ đọc để biết lúc nào cần dọn (không phải duyệt túi đồ của 10.000 người).
    */
   public transient volatile long nextExpireAt = Long.MAX_VALUE;
   /**
    * Buff tốc chạy của kỹ năng (16 Phá Tốc Nỗ): +% tốc chạy tới {@link #moveSpeedBuffUntil} (epoch ms). Chỉ luồng
    * session đọc/ghi (cộng vào chỉ số trong {@link PlayerStats#of}). Không lưu DB — thoát game là mất.
    */
   public transient int moveSpeedBuffPercent;
   public transient long moveSpeedBuffUntil;
   /**
    * Hiệu ứng từ vật phẩm đang còn (vd Nấm hương): {@code option_info.id} -> hết hạn (epoch ms). Lưu cột JSON
    * {@code player.buffs}. Sửa kiểu copy-on-write (thay cả map) trên luồng session; luồng scheduler chỉ đọc
    * ({@code item/ItemBuffs}).
    */
   public volatile java.util.Map<Integer, Long> buffs = java.util.Map.of();
   /**
    * Phụ kiện có lượt đang đeo + bộ đếm lượt ({@code item/GearCharges}): luồng session dựng lại khi mặc / tháo, mọi
    * luồng trừ lượt. Không lưu DB (lượt nằm trên dòng chỉ số 58 / 59 của món đồ).
    */
   public transient volatile com.tlnt.server.item.GearCharges.State gear = com.tlnt.server.item.GearCharges.NONE;
   /** Bất tử tới (epoch ms) — Bùa linh mộc sau khi hồi sinh: mọi sát thương nhận vào = 0. Luồng tick khu ghi / đọc. */
   public transient volatile long invulnerableUntil;
   /** Bị đóng băng tới (epoch ms): không ra chiêu được (client tự khoá di chuyển qua op 35). */
   public transient volatile long frozenUntil;
   /** Choáng (chỉ số Sát thương choáng váng của người đánh) tới: như đóng băng (không ra chiêu, client khoá di chuyển). */
   public transient volatile long stunnedUntil;
   /** Suy yếu tới: chậm 20% ({@link PlayerStats#of}) + nhận thêm 20% sát thương mọi nguồn. Luồng tick khu ghi. */
   public transient volatile long weakenedUntil;
   /** Suy yếu từ skill boss 36: giảm % sức tấn công tới thời điểm này. */
   public transient volatile int bossWeakenAttackPercent;
   public transient volatile long bossWeakenAttackUntil;
   /** Đang bị thiêu đốt tới (không dính lần 2 khi còn cháy). Luồng tick khu ghi / đọc. */
   public transient volatile long burningUntil;
   /** Kỹ năng giảm sát thương nhận vào (4 Trụ Tái Tạo / 10 Lá Chắn Tiễn): % (100 = không mất máu) tới lúc này. */
   public transient volatile int skillShieldPercent;
   public transient volatile long skillShieldUntil;
   /**
    * Trái tim đại dương: đang được tính "gần nước" (+tốc chạy / tấn công trong {@code ItemStats}). Luồng session ghi
    * (theo yêu cầu của luồng tick khu), {@link #waterPosted} = giá trị đã xin gần nhất (-1 chưa xin) chống xin lặp.
    */
   public transient volatile boolean nearWater;
   public transient volatile int waterPosted = -1;
   /** Item hiệu ứng (loại 11) đang chờ hỏi "Bạn đang sử dụng ... có muốn dùng ..." (op 32): ô túi + mẫu; -1 = không. */
   public transient int pendingBuffSlot = -1;
   public transient int pendingBuffTemplateId = -1;
   /** Số món "Dùng nhiều" đang chờ xác nhận ở hộp hỏi đè hiệu ứng. */
   public transient int pendingBuffQuantity = 1;
   /** Đang chờ "Đồng ý" bán: ô túi, mẫu item, số lượng lúc hỏi ({@code ItemService.confirmSell}). */
   public transient int pendingSellSlot = -1;
   public transient int pendingSellTemplateId = -1;
   public transient int pendingSellQuantity;
   /** Lựa chọn đang đặt trong lò rèn tab Nâng cấp ({@code forge/UpgradeService}); không lưu. Chỉ luồng session. */
   public transient com.tlnt.server.forge.UpgradeService.State forgeUpgrade;
   /** Lựa chọn tab Luyện đá ({@code forge/RefineService}); không lưu. Chỉ luồng session. */
   public transient com.tlnt.server.forge.RefineService.State forgeRefine;
   /** Lựa chọn tab Linh hồn — khảm bùa ({@code forge/SoulService}); không lưu. Chỉ luồng session. */
   public transient com.tlnt.server.forge.SoulService.State forgeSoul;
   /** Lựa chọn đang đặt ở tab Chuyển hóa (RAM). */
   public transient com.tlnt.server.forge.TransmuteService.State forgeTransmute;
   /** Lựa chọn tab Nguyên tố — khảm đá nguyên tố ({@code forge/ElementService}); không lưu. Chỉ luồng session. */
   public transient com.tlnt.server.forge.ElementService.State forgeElement;
   /** Lựa chọn tab Pha chế (Olivar), RAM. */
   public transient com.tlnt.server.forge.BrewService.State forgeBrew;
   /** Lò rèn tab Chế tác (Rèn linh thạch): ô đã chọn — RAM. */
   public transient com.tlnt.server.forge.CraftService.State forgeCraft;
   /** Lần chat gần nhất (chống spam, {@code chat/ChatService}). */
   public transient long lastChatAt;
   /** Lần bấm "Sắp xếp" hành trang / rương gần nhất (chống spam). */
   public transient long lastBagSortAt;
   public transient long lastBoxSortAt;
   public transient volatile Appearance appearance;

   // ---------------------------------------------------------------- kỹ năng / cài đặt / nhiệm vụ
   public List<SkillData> skills = new ArrayList<>();
   /** Thanh kỹ năng: danh sách skillId. */
   public int[] skillBar = new int[0];
   /** Cài đặt tự động + phím tắt (opcode 46). Tạo nhân vật = PlayerSettings.defaults(); client đổi -> thay cả object. */
   public volatile PlayerSettings settings;
   /**
    * Nhiệm vụ chính tuyến (opcode 47, {@code quest/QuestService}): nhiệm vụ đang làm + bước + tiến độ; null = chưa có
    * (vào game -> nhiệm vụ 1). Chỉ luồng session ghi (bản bất biến, gán cả bản); luồng tick khu đọc để khoá map / chỉ
    * đường.
    */
   public volatile MainQuestProgress mainQuest;
   /** Hội thoại nhiệm vụ đang mở (bước nào) — nút "Tiếp tục" sang trang kế của đúng bước đó. Chỉ luồng session. */
   public transient com.tlnt.server.quest.QuestService.Dialog questDialog;
   /** Thợ rèn đã mở "Chế Tác Vũ Khí" (map + vị trí, {@code forge/WeaponCraftService}) — rèn chỉ khi còn đứng gần. */
   /** "Tự nhặt vật phẩm" tạm dừng tới lúc này (túi vừa đầy) — luồng session ghi, luồng tick khu đọc. */
   public transient volatile long autoPickupPausedUntil;
   public transient int craftMapId = -1;
   public transient float craftX;
   public transient float craftY;
   /**
    * Nhiệm vụ hằng ngày + điểm chăm chỉ tuần (cột JSON {@code daily_quest}, {@code daily/DailyQuestService}). Chỉ luồng
    * session sửa; luồng tick khu chỉ đọc {@code watch}.
    */
   public com.tlnt.server.daily.DailyState dailyQuest = new com.tlnt.server.daily.DailyState();
   /** NV hằng ngày đang hỏi "Đổi NV" (op 32 loại 15) — chính đối tượng trong danh sách. */
   public transient Object dailySwapTask;
   /** Lần bấm nút bảng NV hằng ngày gần nhất (chống spam). */
   public transient long lastDailyClickAt;
   /** Đang thu hoạch heo tới lúc này (thanh "Đang thu hoạch..." — {@code farm/PigService}). */
   public transient long pigHarvestUntil;
   /** Heo đang chờ xác nhận "Lớn nhanh" (op 32 loại 17), 0 = không. */
   public transient int pendingGrowPig;
   /** Bảng Boss thế giới (op 100): boss đang hỏi xác nhận dịch chuyển / tìm đường (0 = không). Chỉ luồng session. */
   public transient int pendingBossTeleport;
   public transient int pendingBossPath;
   /** Lần xin danh sách boss gần nhất (chống spam op 100/0). Chỉ luồng session. */
   public transient long bossListAt;
   /**
    * "Tìm đường" tới boss: map đích (0 = không) + hạn (epoch ms). Đặt ở luồng session, đọc / xoá ở luồng tick khu lúc vào
    * map ({@code QuestEvents.joinedMap}) — volatile.
    */
   public transient volatile int bossPathMap;
   public transient volatile long bossPathUntil;
   /** Hồi chiêu thuốc HP / MP (op 27 — 2 ô thuốc ở HUD): lúc dùng được lần kế (ms). */
   public transient long hpPotionReadyAt;
   public transient long mpPotionReadyAt;
   /** Thành tựu: bộ đếm + đã nhận (cột JSON {@code achievement}, {@code achievement/AchievementService}). Chỉ luồng session. */
   public com.tlnt.server.achievement.AchievementState achievements = new com.tlnt.server.achievement.AchievementState();
   /** Lần bấm "Nhận" thành tựu gần nhất (chống spam). */
   public transient long lastAchievementClaimAt;
   /** Nông trại: cây táo (cột JSON {@code farm}, {@code farm/AppleTreeService}). */
   public com.tlnt.server.farm.FarmState farm = new com.tlnt.server.farm.FarmState();
   /** Sự kiện (Trung thu: ngày điểm danh) — cột JSON {@code event_state}. */
   public com.tlnt.server.event.EventState eventState = new com.tlnt.server.event.EventState();
   /** Tế đàn Thiên tích: ngày (epochDay giờ server) đi lượt gần nhất (cột altar_day) — 1 lượt / ngày. */
   public int altarDay;
   /** Lượt Tế đàn đang chạy — CHỈ RAM ({@code altar/AltarService}), tắt máy chủ là mất; start 0 = không có. */
   public transient volatile com.tlnt.server.altar.AltarRun altarRun = new com.tlnt.server.altar.AltarRun();
   /**
    * Rừng Linh Mộc: ngày (epochDay giờ server) đi lượt gần nhất (cột linhmoc_day, như altar_day) — 1 lượt / ngày: trùng
    * hôm nay = hết lượt. Ghi dưới khoá {@code this} ({@code linhmoc/LinhMocService} — cả từ luồng session trưởng nhóm).
    */
   public int linhMocDay;
   /**
    * Lượt Lôi vực Sấm sét đang chạy — CHỈ RAM ({@code thunder/ThunderService}); null = không có. Rời map / thoát game là
    * hết lượt. Lượt đã vào + Lệnh bài đã dùng trong ngày đếm ở {@link #itemDaily}.
    */
   public transient volatile com.tlnt.server.thunder.ThunderRun thunderRun;

   /**
    * Đang trong phó bản (hàm chung cho mọi chỗ chặn hành động): đứng ở map Rừng Linh Mộc — chỉ vào được khi lượt đang
    * chạy, hết lượt thì bị đưa ra ngoài, đăng nhập lại luôn ở ngoài. Dùng để khoá trang bị ({@code item/ItemService}).
    */
   public boolean isInDungeon() {
      return inWorld && com.tlnt.server.linhmoc.LinhMocRules.isDungeonMap(mapId);
   }

   // ---------------------------------------------------------------- xã hội (social/, trade/)
   /** Danh sách bạn bè (cột friends), thứ tự = thứ tự gửi client (client xoá bạn theo vị trí). Chỉ luồng session. */
   public List<FriendEntry> friends = new ArrayList<>();
   /**
    * Lời mời kết bạn đang chờ (tab "Lời mời"), thứ tự = thứ tự gửi client (client trả lời theo vị trí). CHỈ trong RAM —
    * thoát game là mất. Chỉ luồng session (người mời xếp việc vào hàng đợi của người nhận).
    */
   public transient List<FriendEntry> friendInvites = new ArrayList<>();
   /**
    * Giao dịch đang mở, null = không. Khác null thì mọi thao tác túi đồ / vàng của chính người này bị chặn
    * ({@code ItemService}) -> lúc chốt, luồng của 1 trong 2 người sửa được túi của cả hai mà không ai sửa song song.
    */
   public transient volatile Trade trade;
   /**
    * Lần câu cá đang diễn ra ({@code fishing/FishingService}), null = không câu. Khác null thì chặn giao dịch, thao tác
    * vật phẩm, thư, kí gửi, shop. Chỉ luồng session ghi.
    */
   public transient volatile com.tlnt.server.fishing.Fishing fishing;
   /** Lần quăng cần / kéo được cá gần nhất (chống bấm liên tục). Chỉ luồng session. */
   public transient long lastFishAt;
   /** Hộp thoại NPC đang mở (op 24 sub 1), null = không. Chỉ luồng session. */
   public transient com.tlnt.server.npc.NpcContext.OpenDialog npcDialog;
   /**
    * Đang có sinh vật hộ tống đi theo ({@code world/Escort}) — luồng tick khu ghi, luồng session đọc (nút "Nói chuyện"
    * của bước hộ tống chỉ hiện khi còn đi cùng).
    */
   public transient volatile boolean escorting;
   /**
    * Rương đồ mở gần nhất (bấm NPC Rương đồ): map + vị trí NPC. Cất / lấy / sắp xếp rương chỉ khi còn đứng gần đó
    * ({@code ItemService.nearBox}); -1 = chưa mở. Chỉ luồng session.
    */
   public transient int boxMapId = -1;
   /** Shop NPC vừa mở ({@code shop/ShopService}) + map / vị trí NPC: mua chỉ khi còn đứng gần. -1 = chưa mở. */
   public transient int shopId = -1;
   /**
    * Các tab của shop đó mà nhân vật này thấy (đã lọc class / giới tính, {@code Shop.viewFor}) lúc mở: chỉ số tab / ô
    * client gửi khi mua tính theo danh sách này (nạp lại shop hay đổi class giữa chừng cũng không lệch).
    */
   public transient java.util.List<com.tlnt.server.shop.Shop.Tab> shopTabs = java.util.List.of();
   public transient int shopMapId = -1;
   public transient float shopX;
   public transient float shopY;
   public transient long lastShopBuyAt;
   public transient float boxX;
   public transient float boxY;
   /** Tổ đội đang ở, null = không ({@code party/PartyService} ghi dưới khoá của nó). Không lưu DB. */
   public transient volatile Party party;
   /**
    * Dòng Ngũ Linh Liên Vũ được cộng chỉ số ({@link PlayerStats#of}): tổ đội đủ 5 loại vũ khí -> cả 5 bit, không thì 0.
    */
   public transient volatile int partyBonusMask;
   /** Lần mời giao dịch / thách đấu / xem thông tin gần nhất (chống spam). Chỉ luồng session. */
   public transient long lastInviteAt;
   public transient long lastInspectAt;
   /** Lần gửi lời thách đấu gần nhất (cách nhau tối thiểu 15 giây — {@code SocialService}). Chỉ luồng session. */
   public transient long lastDuelInviteAt;
   /** Lần thao tác tổ đội gần nhất (chống spam, {@code party/PartyService}). */
   public transient volatile long lastPartyActionAt;
   /**
    * Bộ lạc đang ở ({@code tribe/TribeService} thay cả bản dưới khoá của nó), null = không. Khối chỉ số đọc tên / huy
    * hiệu, {@link PlayerStats#of} + luồng tick khu đọc buff. Không lưu DB (bộ lạc giữ danh sách thành viên).
    */
   public transient volatile com.tlnt.server.tribe.TribeTag tribe;
   /** Số người cùng bộ lạc đứng cạnh (kỹ năng bộ lạc 2 / 4) — luồng tick khu ghi 5 s / lần. */
   public transient volatile int tribeNearby;
   /** Lúc rời / bị đuổi khỏi bộ lạc gần nhất (epoch ms, cột tribe_left_at) — chờ {@code tribe.rejoinHours} mới vào lại. */
   public volatile long tribeLeftAt;
   /** Chống bấm liên tục ở cửa sổ Bộ lạc. */
   public transient volatile long lastTribeActionAt;
   public transient volatile long lastTribeSearchAt;

   // ---------------------------------------------------------------- thống kê
   public long playTimeSeconds;
   /** Thời điểm vào game của phiên hiện tại (không lưu DB). */
   public transient long sessionStartAt;

   public record MainQuestProgress(int questId, int stage, int progress) {
   }

   /**
    * Cột JSON {@code ranking} ({@code {"ek":..,"gp":..}}): "Top chiến binh" = tinh anh / thủ lĩnh đã hạ, "Top ve chai" =
    * tổng vàng đã nhặt (logic hạ tinh anh / nhặt vàng làm sau — chỉ cần cộng vào đây).
    */
   /**
    * Số món đã dùng TRONG NGÀY của item giới hạn theo ngày (quả mọng: 10 quả / ngày) — cột JSON {@code item_daily}
    * ({@code {"day":epochDay,"used":{"147":3}}}); sang ngày mới thì đếm lại. Chỉ luồng session.
    */
   public DailyItemUse itemDaily = new DailyItemUse();

   public static final class DailyItemUse {
      public long day;
      public java.util.HashMap<Integer, Integer> used = new java.util.HashMap<>();

      /** Số đã dùng hôm nay ({@code today} = LocalDate.toEpochDay); qua ngày thì xoá sổ. */
      public int usedToday(int templateId, long today) {
         if (day != today) {
            day = today;
            used.clear();
         }
         return used.getOrDefault(templateId, 0);
      }
   }

   /**
    * Số liệu xếp hạng (cột JSON {@code ranking}, {@code ranking/RankingService}). Tổng từ trước tới nay: điểm Chiến binh
    * (Tinh anh 5 / Thủ lĩnh 10), vàng nhặt (Ve chai). Top tuần: bộ đếm của tuần {@link #week} + giữ lại tuần trước
    * ({@link #prevWeek}) để lượt trả thưởng đầu tuần vẫn đọc được kể cả khi người chơi đã sang tuần mới. Cường hóa không
    * lưu ở đây (tính từ đồ đang mặc). Sửa từ luồng session (khoá: this), đọc từ luồng tính bảng.
    */
   public static final class RankStats {
      /** Điểm Chiến binh (JSON cũ "ek" = số tinh anh, chưa từng được cộng). */
      @com.google.gson.annotations.SerializedName(value = "wp", alternate = "ek")
      public long warriorPoints;
      @com.google.gson.annotations.SerializedName("gp")
      public long goldPicked;
      /** Điểm sự kiện (bảng "Sự kiện" op 39) — Trung thu: bánh, cà rốt, đèn hoa đăng, boss ({@code event/}). */
      @com.google.gson.annotations.SerializedName("ep")
      public long eventPoints;
      /** Mã tuần ({@code ranking/WeekClock}) của 3 bộ đếm tuần; 0 = chưa có. */
      @com.google.gson.annotations.SerializedName("w")
      public int week;
      @com.google.gson.annotations.SerializedName("wr")
      public long weekRecharge;
      @com.google.gson.annotations.SerializedName("ww")
      public long weekWarrior;
      @com.google.gson.annotations.SerializedName("wg")
      public long weekGold;
      @com.google.gson.annotations.SerializedName("pw")
      public int prevWeek;
      @com.google.gson.annotations.SerializedName("pr")
      public long prevRecharge;
      @com.google.gson.annotations.SerializedName("pww")
      public long prevWarrior;
      @com.google.gson.annotations.SerializedName("pg")
      public long prevGold;

      /** Sang tuần {@code now}: bộ đếm tuần đang giữ thành "tuần trước" (nếu đúng là tuần liền trước), rồi về 0. */
      public synchronized void roll(int now) {
         if (week == now) {
            return;
         }
         if (week == now - 1) {
            prevWeek = week;
            prevRecharge = weekRecharge;
            prevWarrior = weekWarrior;
            prevGold = weekGold;
         } else if (prevWeek != now - 1) {
            prevWeek = 0;
            prevRecharge = prevWarrior = prevGold = 0;
         }
         week = now;
         weekRecharge = weekWarrior = weekGold = 0;
      }

      public synchronized void addWarrior(long points, int now) {
         roll(now);
         warriorPoints += points;
         weekWarrior += points;
      }

      public synchronized void addGold(long amount, int now) {
         roll(now);
         goldPicked += amount;
         weekGold += amount;
      }

      public synchronized void addEvent(long points) {
         eventPoints += points;
      }

      public synchronized void addRecharge(long amount, int now) {
         roll(now);
         weekRecharge += amount;
      }
   }

   // ---------------------------------------------------------------- HP / MP (khoá: this)

   public boolean isDead() {
      return hp <= 0;
   }

   /**
    * Trừ {@code amount} HP nhưng không xuống dưới {@code floor} (0 = có thể chết; 1 = thách đấu, thua không chết).
    * Đã chết thì không trừ nữa. @return HP còn lại
    */
   public synchronized long damageHp(long amount, long floor) {
      if (hp <= 0 || amount <= 0) {
         return hp;
      }
      hp = Math.max(Math.min(floor, hp), hp - amount);
      if (hp <= 0) { // vừa bị hạ gục (mọi nguồn: quái, người, ong đốt...)
         activityPoints = Math.max(0, activityPoints - STAMINA_DEATH_LOSS);
      }
      return hp;
   }

   /** Trừ {@code amount} thể lực nếu đủ. @return false = thể lực đã cạn (không được đánh) */
   public synchronized boolean spendStamina(int amount) {
      if (activityPoints < amount) {
         return false;
      }
      activityPoints -= amount;
      return true;
   }

   /** Hồi HP (không vượt {@code max}); đã chết thì KHÔNG hồi (chỉ hồi sinh mới sống lại). @return HP sau khi hồi */
   public synchronized long healHp(long amount, long max) {
      if (hp > 0 && amount > 0) {
         hp = Math.min(max, hp + amount);
      }
      return hp;
   }

   /** Hồi MP (không vượt {@code max}), đã chết thì không hồi. */
   public synchronized long healMp(long amount, long max) {
      if (hp > 0 && amount > 0) {
         mp = Math.min(max, mp + amount);
      }
      return mp;
   }

   /** Trừ MP nếu đủ. @return true nếu đã trừ */
   public synchronized boolean spendMp(long amount) {
      if (mp < amount) {
         return false;
      }
      mp -= amount;
      return true;
   }

   /** Đặt thẳng HP / MP (hồi sinh, lệnh test, kẹp theo tối đa mới). */
   public synchronized void setHpMp(long newHp, long newMp) {
      hp = newHp;
      mp = newMp;
   }

   public PlayerData(int id, int accountId) {
      this.id = id;
      this.accountId = accountId;
   }

   public int getId() {
      return id;
   }

   void assignId(int id) {
      this.id = id;
   }

   public int getAccountId() {
      return accountId;
   }
}
