package com.tlnt.server.net;

/**
 * Opcode dùng chung 2 chiều (client và server dùng cùng số, ý nghĩa xem comment từng dòng). Tên đặt theo
 * chức năng THẬT — nhiều tên method phía client (decompile) bị sai nghĩa, ví dụ
 * {@code ServerPacketSender.sendCreateCharacter()} thực ra là xin captcha (opcode 49).
 * Bảng đầy đủ + format từng gói: {@code TLNT-Server/docs/PROTOCOL.md}.
 */
public final class Opcode {

   /** C→S: handshake (version client + version dữ liệu). */
   public static final int HANDSHAKE = 0;
   /** 2 chiều: tài khoản. C→S sub = {@link com.tlnt.server.handler.AuthHandler}; S→C sub 0 = trạng thái MK cấp 2. */
   public static final int ACCOUNT = 1;
   /**
    * 2 chiều: nhân vật. C→S sub 0 xem thông tin 1 người, sub 1 bật/tắt khoá nhân vật (nút MountButton).
    * S→C sub 0 tạo nhân vật của mình, sub 5 thông tin người được xem, sub 6 cờ khoá nhân vật của 1 người.
    */
   public static final int PLAYER = 2;
   /** C→S sub 0: vị trí nhân vật mình (x*100, y*100 = pixel). S→C: đích di chuyển của người khác. */
   public static final int MOVE = 5;
   /** S→C: kỹ năng. Sub 1 đã học, sub 2 thanh kỹ năng, sub 3 kỹ năng đang chọn. */
   public static final int SKILL = 21;
   /**
    * 2 chiều: túi đồ. S→C sub 0 đồ mặc, 1 hành trang, 2 rương, 4/6/8/9 ngoại trang/danh hiệu/mặt nạ/thú cưng, 5 ô dùng
    * nhanh. C→S sub 0 sắp xếp hành trang, sub 1 sắp xếp rương.
    */
   public static final int INVENTORY = 22;
   /** S→C: chỉ số. Sub 0 cả khối chỉ số 1 người, 3 HP/MP, 8 thể lực. */
   public static final int STATS = 23;
   /** C→S: thao tác item (sub xem {@code ItemHandler}). */
   public static final int ITEM = 27;
   /** 2 chiều: bộ sưu tập ngoại trang / danh hiệu / mặt nạ / thú cưng (xem {@code CollectionKind}). */
   public static final int COSTUME = 48;
   public static final int TITLE = 50;
   public static final int MASK = 64;
   public static final int PET = 66;
   /** Bộ sưu tập đeo lưng (C→S sub 0 ẩn / 1 dùng-tháo / 2 mở rộng; S→C sub 0 chỉ số / 1 đang dùng). */
   public static final int BACKWEAR = 78;
   /** S→C sub 0: mở cửa sổ rương. */
   public static final int BOX = 53;
   /** C→S: đi vào cửa dịch chuyển (byte = vị trí cửa trong danh sách opcode 6). */
   public static final int WAYPOINT = 11;
   /** C→S sub 0 xin danh sách khu, 1 đổi khu, 2 dịch chuyển qua bản đồ thế giới, 3 nút thoát map. */
   public static final int ZONE = 25;
   /**
    * 2 chiều: chat. C→S sub 0 chat khu (str nội dung; sub 1-4 kênh khác chưa làm). S→C sub 0 chat khu (int người nói,
    * str nội dung): bong bóng trên đầu + khung chat "gần đây".
    */
   public static final int CHAT = 26;
   /** S→C: sub 0 thông báo nổi + chat hệ thống, sub 1 chạy chữ. */
   public static final int NOTICE = 14;
   /** 2 chiều: ping, server echo timestamp. */
   public static final int PING = 29;
   /**
    * S→C: hộp hỏi "Đồng ý / Từ chối" (byte loại, bool false, str "", str nội dung — GiftcodeEventConfirmDialog).
    * C→S: bấm "Đồng ý" -> byte loại ({@code sendEmote}, tên sai). "Từ chối" không gửi gì.
    */
   public static final int CONFIRM = 32;
   /**
    * 2 chiều: bị hạ gục / hồi sinh. C→S sub 0 hồi sinh bằng cỏ hồi sinh, sub 1 về làng. S→C sub 0 chữ hộp "Kiệt sức",
    * sub 1 hiệu ứng hồi sinh 1 người.
    */
   public static final int REVIVE = 28;
   /** 2 chiều: tổ đội (sub xem {@code PartyHandler}). S→C sub 0 thông tin tổ đội (-1 = không có), sub 1 tổ đội lân cận. */
   public static final int PARTY = 31;
   /** 2 chiều: bạn bè. C→S sub 0 xin danh sách, 1 xin lời mời, 2 kết bạn, 5 xoá. S→C sub 0 danh sách, 1 lời mời. */
   public static final int FRIEND = 34;
   /**
    * C→S sub 0: thách đấu (int playerId). S→C sub 0: cờ đang thách đấu 1 người (int id, bool, [int đối thủ]), sub 3:
    * đếm ngược thách đấu.
    */
   public static final int DUEL = 41;
   /** 2 chiều sub 0: trạng thái PK. C→S byte trạng thái muốn đổi; S→C int playerId, byte trạng thái. */
   public static final int PK = 42;
   /** 2 chiều: giao dịch (sub xem {@code TradeHandler} / {@code TradePackets}). */
   public static final int TRADE = 56;
   /**
    * Câu cá. C→S sub 0 = bấm nút câu ({@code sendRequestHarvest} — tên sai): quăng cần / kéo cần. S→C
    * ({@code handleGatherAction} — tên sai): 0 quăng cần, 1 câu được, 2 thanh kéo cần, 3 thôi câu.
    */
   public static final int FISHING = 57;
   /**
    * Cây hái được. C→S sub 0 = bấm hái (int id thực thể cây; {@code sendGuildVaultAction} — tên sai, gọi từ
    * {@code MoveToPlantCallback}). S→C ({@code handlePlantInteraction}): 0 danh sách, 1 cây mọc, 2 thanh thu hoạch,
    * 3 đã hái (cây biến mất), 4 huỷ thanh thu hoạch.
    */
   public static final int PLANT = 54;
   /**
    * Vật phẩm dưới đất. C→S sub 0 = nhặt (int id; {@code sendQuestAction} — tên sai, từ {@code MoveToItemCallback}).
    * S→C ({@code handleMapItemUpdate}): 0 danh sách, 1 rơi, 2 biến mất, 3 có người nhặt.
    */
   public static final int GROUND_ITEM = 30;
   /** S→C sub 0: thanh hiệu ứng (buff) trên HUD — byte n, n × (int option_info.id, int ms còn lại). */
   public static final int BUFFS = 51;
   /**
    * 2 chiều: NPC. C→S sub 0 bấm NPC (byte id thực thể, int npc_template.id), sub 1 bấm nút hộp thoại (byte trang, int id
    * thực thể, byte nút). S→C sub 0 NPC trên map, sub 1 hộp thoại, sub 2 chữ nổi trên đầu NPC.
    */
   public static final int NPC = 24;
   /**
    * 2 chiều: phúc lợi (quà online / điểm danh / quà nạp — client đặt tên giftcode event). C→S sub 0 nhận mốc (byte
    * tab, byte mốc), sub 1 nhập giftcode (chưa làm). S→C sub 0 cả danh sách (1 lần / lần vào game), sub 1 cập nhật 1 mốc.
    */
   public static final int WELFARE = 33;
   /**
    * 2 chiều: kí gửi (chợ người chơi, cửa sổ "Ký gửi" / "Cửa hàng"). C→S sub 0 mở cửa sổ, 1 rao bán, 2 mua, 3 gỡ
    * xuống, 4 "Rao bán" lại. S→C sub 0 xoá danh sách, 1 danh sách, 2 bỏ chọn món đang rao.
    */
   public static final int MARKET = 36;
   /**
    * Bộ lạc. C→S sub 0..16 ({@code sendMarketSearch}, {@code sendMarketBuy}... — tên sai, xem {@code TribeHandler}); S→C
    * {@code handlePetUpdate} (tên sai) sub 0..4 ({@code tribe/TribePackets}).
    */
   public static final int TRIBE = 37;
   /** Thành tựu: C→S sub 0 int id "Nhận" ({@code sendMountEquip} — tên sai); S→C 0 xoá / 1 danh sách / 2 cập nhật 1 mục. */
   public static final int ACHIEVEMENT = 62;
   /** S→C: quà hiện ở các mục Cẩm nang (client {@code handleSkillSlots} — tên sai nghĩa) — {@code handbook/HandbookService}. */
   public static final int HANDBOOK = 61;
   /**
    * 2 chiều: hòm thư. C→S sub 0 đọc, 1 gửi, 3 nhận 1 thư, 4 xoá (byte hộp, byte n, n×int id), 5 nhận tất cả.
    * S→C sub 0 hòm thư đến, sub 1 thư đã gửi.
    */
   public static final int MAIL = 40;
   /**
    * 2 chiều: cửa hàng NPC. S→C sub 0 mở shop (tab + món + giá). C→S sub 0 mua (int shop, int tab, int ô, int số lần).
    */
   public static final int SHOP = 55;
   /** S→C: lò rèn ({@code ForgeWindow}). Sub 0 mở (byte chế độ = tab). Logic C→S chưa làm. */
   public static final int FORGE = 44;
   /**
    * 2 chiều: Huy hiệu Tinh Linh ({@code badge/SpiritBadgeService}). S→C 45/0 bool còn hạn + long ms còn lại, 45/1 danh
    * sách nút Tiện ích. C→S 45/0 bấm nút Tiện ích, 45/1 byte nút.
    */
   public static final int UTILITY = 45;
   /** S→C: "Chế Tác Vũ Khí" ({@code WeaponCraftWindow}). Sub 0 mở (byte n công thức, n×item). */
   public static final int WEAPON_CRAFT = 60;
   /** 2 chiều: bảng xếp hạng. C→S sub 0 xin 1 bảng (int id). S→C sub 0 danh sách bảng, sub 1 1 bảng. */
   public static final int RANKING = 39;
   /** Top tuần (tab "Top tuần" cửa sổ Cửa hàng): C→S sub 0 xin bảng; S→C sub 0 danh sách + quà, sub 1 bảng. */
   public static final int WEEKLY_TOP = 73;
   /** S→C bảng xếp hạng riêng (client mới {@code OtherRankWindow}, decompile C3873 m18111). */
   public static final int OTHER_RANK = 75;
   /**
    * 2 chiều: cửa sổ "Nạp tiền" (client mới). C→S sub 0 int id gói; S→C sub 0 danh sách gói, sub 1 str link mở trình
    * duyệt ({@code recharge/RechargeService}).
    */
   public static final int RECHARGE = 72;
   /** S→C vật thể trôi (đèn hoa đăng — client {@code FloatAwayEffect}, decompile C3524). */
   public static final int FLOAT_AWAY = 80;
   /** S→C vật trang trí do server đặt (client {@code MapPropEntity}, decompile C3531): 0 danh sách, 1 thêm, 2 gỡ. */
   public static final int MAP_PROP = 81;
   /** S→C vị trí nhiều người 1 gói (client mới, decompile C3873 m18084): byte n, n × (int id, float x, float y). */
   public static final int MOVE_BATCH = 82;
   /** S→C sub 0: bảng "nhận thưởng" hiện các món vừa nhận (int n, n×item). */
   public static final int REWARD_POPUP = 65;
   /**
    * S→C: dòng thông báo có đồng hồ đếm ngược bên phải nút "Thoát" ({@code AnnouncementBar}, client
    * {@code handleChatMessage} — tên sai). Sub 0 thêm / cập nhật (int id, str chữ, int ms đã trôi, int ms tổng) -> client
    * vẽ "chữ 12m:5s" tự đếm; sub 1 xoá (int id).
    */
   public static final int ANNOUNCEMENT = 67;
   /**
    * 2 chiều: nhiệm vụ hằng ngày (tab "Hàng ngày"). C→S 0 mở, 1 nhận mốc chăm chỉ, 2 đổi NV, 3 nhận thưởng NV, 4 nhận
    * NV mới ({@code sendRequestMount}, {@code sendPet*} — tên sai). S→C ({@code handleAuctionUpdate}) 0 mốc + điểm, 1 mốc
    * đã nhận, 2 điểm, 3 danh sách NV, 4 tiến độ 1 NV — {@code daily/DailyQuestService}.
    */
   public static final int DAILY_QUEST = 69;
   /** 2 chiều: nông trại. C→S 0 bấm con vật (thu hoạch heo). S→C 0 / 1 con vật, 2 cây táo, 3 / 5 thanh thu hoạch, 4 nhặt. */
   public static final int FARM = 52;
   /** 2 chiều: gói combo (tab "Gói combo" cửa sổ ShopWindow). C→S 0 mở, 1 mua. S→C 0 danh sách, 1 đã mua. */
   public static final int COMBO = 68;
   /** 2 chiều: vòng quay may mắn (tab "Vòng quay"). C→S 0 mở, 1 quay (byte 0 = 1 lần, 1 = 10 lần). S→C 0 ô, 1 ô trúng. */
   public static final int LUCKY_WHEEL = 71;
   /** C→S: Luyện tập Offline (nút trong Thiết lập tự động, decompile C3764): sub 0 byte n, n×int mob_template (0 = mọi loại). */
   public static final int OFFLINE_TRAINING = 77;
   /**
    * 2 chiều (MỚI, không có trong decompile): bảng Boss thế giới (nút "Boss thế giới" dưới "Luyện tập Offline" ở cửa sổ Tiện ích
    * = Huy hiệu Tinh Linh). C→S 0 xin danh sách, 1 int id dịch chuyển (lam thạch), 2 int id tự tìm đường (vàng). S→C 0
    * danh sách, 1 bật tự tìm đường, 2 mở bảng — {@code worldboss/WorldBossList}, docs/PROTOCOL.md.
    */
   public static final int WORLD_BOSS = 100;
   /**
    * 2 chiều: hộp nhập chữ. S→C (byte kênh, str lời nhắc, str chữ sẵn) mở hộp; C→S (byte kênh, str chữ) khi bấm đồng ý.
    * Kênh 1 chat thế giới (Tù và), 2 đổi tên (Thẻ đổi tên) — {@code Packets.INPUT_*}.
    */
   public static final int INPUT = 70;
   /** C→S: tạo nhân vật (name, giới tính 1..3). S→C: mở bảng tạo nhân vật (không payload). */
   public static final int CREATE_CHARACTER = 38;
   /** S→C: danh sách máy chủ. */
   public static final int SERVER_LIST = 43;
   /** 2 chiều: cài đặt tự động + 16 phím tắt + ẩn người khác (C→S khi người chơi đổi, S→C lúc vào game). */
   public static final int SETTINGS = 46;
   /** C→S sub 0: xin captcha mới. S→C sub 0: ảnh captcha PNG. */
   public static final int CAPTCHA = 49;
   /** S→C: khoá AES giải mã tài nguyên (ảnh, atlas skeleton) client tải về. */
   public static final int DECRYPT_KEYS = 59;

   private Opcode() {
   }
}
