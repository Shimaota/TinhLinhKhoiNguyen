package com.tlnt.server.quest;

/**
 * Loại bước nhiệm vụ = cách tính tiến độ. Sự kiện tới từ các hệ thống khác qua {@link QuestEvents}; {@code target} của
 * bước phải khớp (0 = không cần khớp).
 */
public enum StepKind {
   /** Bấm NPC {@code target} -> nút "Nói chuyện" đầu menu -> hội thoại; bước xong ngay khi mở hội thoại. */
   TALK,
   /** Thu hoạch Cây táo thành công ({@code farm/AppleTreeService}). */
   HARVEST_APPLE,
   /** Hạ {@code count} quái mẫu {@code target} — người kết liễu + đồng đội cùng khu đều được tính. */
   KILL,
   /** Hạ quái mẫu {@code target} là Tinh anh / Thủ lĩnh — người kết liễu + đồng đội cùng khu đều được tính. */
   KILL_ELITE,
   /**
    * Có đủ {@code count} món {@code target} trong hành trang (tiến độ = số đang có). Lúc đang ở bước này, hạ quái bất kỳ
    * (trừ người rơm) rơi 1 món RIÊNG cho người đó ({@link QuestEvents#questDrop}).
    */
   COLLECT,
   /** Gán ô thuốc HP + ô thuốc MP (tiến độ = số ô đã gán, tính lại từ ô dùng nhanh). */
   BIND_POTIONS,
   /** Dùng 1 Quả táo (bấm dùng, kể cả khi HP / MP đã đầy). */
   USE_APPLE,
   /** Cài đặt tự động đúng như hướng dẫn client {@code TutorialAutoPlaySettingsStep}. */
   AUTO_SETTINGS,
   /** Mở được Rương đồ (NPC 19). */
   OPEN_CHEST,
   /** Lấy 1 món ra khỏi Rương đồ (NPC 19). */
   TAKE_FROM_CHEST,
   /** Mặc 1 trang bị. */
   EQUIP,
   /** Cầm 1 vũ khí. */
   EQUIP_WEAPON,
   /** Cộng điểm tiềm năng. */
   ADD_TALENT,
   /** Gán kỹ năng vào ô kỹ năng. */
   ASSIGN_SKILL,
   /** Rèn vũ khí ở cửa sổ "Chế Tác Vũ Khí" (op 60, {@code forge/WeaponCraftService}). */
   CRAFT_WEAPON,
   /** Bấm nâng cấp trang bị bằng đá {@code target} (125 Mặt trời / 126 Mặt trăng) — thành công hay thất bại đều tính. */
   UPGRADE_STONE,
   /** Đạt cấp {@code count} (tiến độ = cấp hiện tại). */
   REACH_LEVEL,
   /** Mua 1 món ở shop {@code target} ({@code tlnt_data.shop.id}). */
   BUY_SHOP,
   /** Gửi lời mời kết bạn {@code count} lần. */
   FRIEND_INVITE,
   /** Đang ở trong 1 tổ đội (tạo hoặc gia nhập). */
   JOIN_PARTY,
   /** Bắt đầu nâng cấp Cây táo. */
   UPGRADE_APPLE_TREE,
   /** Vào tới map {@code target}. */
   REACH_MAP,
   /** Gây sát thương lên boss Tế đàn ở map {@code target} {@code count} lần (đòn hụt không tính). */
   HIT_BOSS,
   /** Bấm nút "Giải đấu Bộ lạc" ở Tư tế Buraen (giải đấu chưa có — bấm là xong). */
   JOIN_TOURNAMENT,
   /**
    * Có đủ {@code count} món {@code target} trong hành trang; lúc đang ở bước này, hạ Tinh anh / Thủ lĩnh của quái
    * {@code arrowMob} rơi 1 món RIÊNG ({@link QuestEvents#questDrop}).
    */
   COLLECT_ELITE,
   /** Thu hoạch (hái) {@code count} cây mẫu {@code target} ({@code plant_template.id}) thành công. */
   HARVEST_PLANT,
   /**
    * Gây sát thương lên boss thế giới {@code target} ({@code tlnt_data.world_boss.id}) {@code count} lần (đòn hụt không
    * tính) — boss xuất hiện ngẫu nhiên nên không chỉ đường.
    */
   HIT_WORLD_BOSS,
   /**
    * Có đủ {@code count} món {@code target} trong hành trang; lúc đang ở bước này, GÂY SÁT THƯƠNG lên boss (Hắc thần Linh
    * mộc, Tinh linh Thần mộc, boss thế giới) mà boss chết thì rơi 1 món RIÊNG ({@link QuestEvents#bossDrop}) — chỉ người
    * đó thấy / nhặt. Hạ quái thường không rơi.
    */
   COLLECT_BOSS,
   /**
    * NÂNG CẤP THÀNH CÔNG 1 trang bị lên {@code count} sao (tiến độ = số sao cao nhất vừa nâng thành công trong lúc làm
    * bước này; đồ đã có sẵn sao trong túi / rương KHÔNG tính — {@code forge/UpgradeService} báo khi thành công).
    */
   UPGRADE_STAR,
   /**
    * Hái {@code count} Hoa bất kỳ (cây cho hoa pha chế — {@code forge/BrewService.RECIPES}, tiến độ = số hoa nhận được);
    * mũi tên chỉ tới map {@code mapId}, đứng ở map có cây hoa ({@code arrowMob} = 1 cây hoa) thì tắt.
    */
   HARVEST_FLOWER,
   /** Pha chế THÀNH CÔNG 1 nước ma thuật ở Phù thủy Olivar ({@code forge/BrewService}). */
   BREW,
   /** Nhiệm vụ chưa có (chờ cập nhật) — không bao giờ xong. */
   WAIT
}
