-- tlnt_server: accounts + characters. SCHEMA ONLY (no player data).
-- Generated from a private dump; only default `web_setting` rows are included.

-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Máy chủ: 127.0.0.1
-- Phiên bản máy phục vụ: 10.4.32-MariaDB
-- Phiên bản PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Cơ sở dữ liệu: `tlnt_server`
--

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `account`
--

CREATE TABLE `account` (
  `id` int(11) NOT NULL,
  `username` varchar(20) NOT NULL COMMENT 'không phân biệt hoa thường (collation _ci)',
  `password_hash` varchar(255) NOT NULL COMMENT 'pbkdf2$vòng$salt$hash (PasswordHasher)',
  `email` varchar(128) NOT NULL,
  `secondary_password_hash` varchar(255) DEFAULT NULL COMMENT 'MK cấp 2, NULL = chưa tạo',
  `protection_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '"Tài khoản đang được bảo vệ" (client: isSecondaryPasswordLocked)',
  `banned` tinyint(1) NOT NULL DEFAULT 0,
  `ban_reason` varchar(255) DEFAULT NULL,
  `role` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0 người chơi, 1 GM, 2 quản trị viên (lệnh test chat)',
  `register_ip` varchar(45) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `last_login_at` datetime DEFAULT NULL,
  `last_login_ip` varchar(45) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Đang đổ dữ liệu cho bảng `account`
--


-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `card_charge`
--

CREATE TABLE `card_charge` (
  `id` bigint(20) NOT NULL,
  `account_id` int(11) NOT NULL,
  `request_id` varchar(32) NOT NULL COMMENT 'mã đơn web gửi đối tác (duy nhất)',
  `trans_id` varchar(32) DEFAULT NULL COMMENT 'mã giao dịch phía đối tác',
  `telco` varchar(16) NOT NULL,
  `serial` varchar(32) NOT NULL,
  `code` varchar(32) NOT NULL COMMENT 'mã thẻ (cần cho lệnh check + so callback)',
  `declared_value` int(11) NOT NULL COMMENT 'mệnh giá người chơi chọn',
  `real_value` int(11) DEFAULT NULL COMMENT 'mệnh giá thật đối tác báo',
  `partner_amount` int(11) DEFAULT NULL COMMENT 'tiền đối tác trả cho mình (sau phí)',
  `status` smallint(6) NOT NULL DEFAULT 99 COMMENT '99 chờ, 1 đúng, 2 sai mệnh giá, 3 thẻ lỗi, 4 bảo trì, 100 bị từ chối',
  `message` varchar(255) DEFAULT NULL,
  `diamond` bigint(20) NOT NULL DEFAULT 0 COMMENT 'lam thạch sẽ cộng (web tính khi có kết quả)',
  `credited` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0 chưa, 2 đang cộng, 1 đã cộng',
  `ip` varchar(45) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `credited_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `mail`
--

CREATE TABLE `mail` (
  `id` int(11) NOT NULL COMMENT 'server tự cấp (tăng dần từ MAX(id))',
  `owner_id` int(11) NOT NULL COMMENT 'player.id chủ thư',
  `box` tinyint(4) NOT NULL COMMENT '0 hòm thư đến, 1 thư đã gửi',
  `sender_name` varchar(32) NOT NULL,
  `receiver_name` varchar(32) NOT NULL,
  `subject` varchar(64) NOT NULL,
  `content` varchar(512) NOT NULL,
  `items` text DEFAULT NULL COMMENT 'JSON [item gọn như item_bag], NULL = không đính kèm; item 1 = vàng',
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `claimed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 = đã nhận đồ đính kèm',
  `created_at` bigint(20) NOT NULL COMMENT 'epoch ms, quá 7 ngày tự xoá'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Đang đổ dữ liệu cho bảng `mail`
--


-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `market_listing`
--

CREATE TABLE `market_listing` (
  `id` int(11) NOT NULL COMMENT 'server tự cấp (tăng dần từ MAX(id))',
  `seller_id` int(11) NOT NULL,
  `seller_name` varchar(32) NOT NULL,
  `item` text NOT NULL COMMENT 'JSON item gọn',
  `price` int(11) NOT NULL COMMENT 'vàng không khoá',
  `fee` bigint(20) NOT NULL COMMENT 'phí đã thu lúc đăng bán (không hoàn)',
  `fee_index` tinyint(4) NOT NULL COMMENT 'dòng tlnt_data.market_fee (8h/16h/24h)',
  `created_at` bigint(20) NOT NULL COMMENT 'epoch ms',
  `expire_at` bigint(20) NOT NULL COMMENT 'epoch ms, quá hạn -> trả đồ về thư',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0 đang bán, 1 đã bán, 2 gỡ xuống, 3 hết hạn',
  `buyer_id` int(11) NOT NULL DEFAULT 0,
  `closed_at` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Đang đổ dữ liệu cho bảng `market_listing`
--


-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `player`
--

CREATE TABLE `player` (
  `id` int(11) NOT NULL COMMENT 'playerId gửi cho client',
  `account_id` int(11) NOT NULL,
  `name` varchar(16) NOT NULL COMMENT 'unique không phân biệt hoa thường/dấu -> chống tên giả mạo',
  `gender` tinyint(4) NOT NULL DEFAULT 1 COMMENT 'giới tính: 1 Rinn, 2 Lyria, 3 Bruk (char_template mặc định ghi trong code: player/Gender)',
  `class_id` tinyint(4) NOT NULL DEFAULT 1 COMMENT 'char_class.id = loại vũ khí: 1 Chưa vũ khí, 2 Rìu, 3 Cung tên, 4 Ná thun, 5 Giáo, 6 Roi da (chọn khi cầm vũ khí đầu tiên)',
  `level` smallint(6) NOT NULL DEFAULT 1,
  `exp` bigint(20) NOT NULL DEFAULT 0,
  `pk_points` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'điểm PK 0..20: +1 khi hạ gục người lúc cờ Thử thách; >10 mà bị hạ gục thì -10% EXP, -1 điểm',
  `pk_points_at` bigint(20) NOT NULL DEFAULT 0,
  `spirit_badge_until` bigint(20) NOT NULL DEFAULT 0,
  `hp` bigint(20) NOT NULL DEFAULT -1 COMMENT '-1 = đầy (tính max khi vào map)',
  `mp` bigint(20) NOT NULL DEFAULT -1 COMMENT '-1 = đầy',
  `gold` bigint(20) NOT NULL DEFAULT 0 COMMENT 'vàng KHÔNG khoá (giao dịch, thư, kí gửi)',
  `locked_gold` bigint(20) NOT NULL DEFAULT 0 COMMENT 'vàng khoá (quà phúc lợi), không giao dịch được',
  `diamond` bigint(20) NOT NULL DEFAULT 0 COMMENT 'lam thạch (ô tiền tệ 3): mua shop NPC cur 2, quà item 2',
  `recharge_total` bigint(20) NOT NULL DEFAULT 0 COMMENT 'tổng lam thạch đã nạp (mốc Quà nạp phúc lợi)',
  `welfare` text DEFAULT NULL COMMENT 'phúc lợi: {day,onlineMs,onlineClaimed,week,checkinClaimed,rechargeClaimed}',
  `dynamic_points` int(11) NOT NULL DEFAULT 0 COMMENT 'điểm năng động (client Điểm hoạt động, op 23 sub 9) — kí gửi lượt 6+ trong ngày tốn 15',
  `market_sell_day` int(11) NOT NULL DEFAULT 0 COMMENT 'kí gửi: ngày (epochDay) của market_sell_count',
  `market_sell_count` int(11) NOT NULL DEFAULT 0 COMMENT 'kí gửi: số lượt đăng bán trong ngày',
  `ranking` text DEFAULT NULL COMMENT 'JSON số liệu xếp hạng {"ek": tinh anh đã hạ, "gp": vàng đã nhặt}',
  `giftcodes` text DEFAULT NULL COMMENT 'JSON giftcode đã nhập {"TANTHU":1}',
  `activity_points` int(11) NOT NULL DEFAULT 0 COMMENT 'thể lực hiện tại (opcode 23 sub 8), tạo nhân vật = đầy',
  `title_id` int(11) NOT NULL DEFAULT -1 COMMENT 'title.id đang đeo, -1 = không',
  `talent_points` int(11) NOT NULL DEFAULT 0 COMMENT 'điểm tiềm năng chưa cộng',
  `talents` text DEFAULT NULL COMMENT 'int[] điểm đã cộng theo thứ tự bảng talent',
  `bonus_stats` text DEFAULT NULL COMMENT 'PlayerStats cộng vĩnh viễn riêng (đan dược, sự kiện...), NULL = không có',
  `map_id` int(11) NOT NULL DEFAULT 2,
  `pos_x` float DEFAULT NULL COMMENT 'NULL = điểm xuất hiện mặc định của map',
  `pos_y` float DEFAULT NULL,
  `bag_size` smallint(6) NOT NULL DEFAULT 50,
  `box_size` smallint(6) NOT NULL DEFAULT 25,
  `item_body` mediumtext DEFAULT NULL COMMENT 'd? dang m?c (op22 sub0): JSON thua [{slot=equip_slot,item}]',
  `item_back` mediumtext DEFAULT NULL,
  `item_bag` mediumtext DEFAULT NULL COMMENT 'hÓnh trang (op22 sub1): JSON thua [{slot,item}], ch? ¶ c¾ d?',
  `item_box` mediumtext DEFAULT NULL COMMENT 'ruong (op22 sub2): JSON thua [{slot,item}]',
  `item_costume` mediumtext DEFAULT NULL COMMENT 'ngo?i trang lo?i 19 (op22 sub4): {items:[...],selected}',
  `item_title` mediumtext DEFAULT NULL COMMENT 'danh hi?u lo?i 20 (op22 sub6): {items,selected}',
  `item_mask` mediumtext DEFAULT NULL COMMENT 'm?t n? lo?i 36 (op22 sub8): {items,selected}',
  `item_pet` mediumtext DEFAULT NULL COMMENT 'th· cung lo?i 37 (op22 sub9): {items,selected}',
  `quick_slots` varchar(64) DEFAULT NULL COMMENT 'sub 5: int[4] index trong bag, -1 = trống',
  `skills` text DEFAULT NULL COMMENT 'List<SkillData{skillId, level, lastUsedAt}> (opcode 21 sub 1)',
  `skill_bar` text DEFAULT NULL COMMENT 'int[] skillId trên thanh kỹ năng (opcode 21 sub 2)',
  `settings` text DEFAULT NULL COMMENT 'PlayerSettings (opcode 46), NULL = mặc định client',
  `main_quest` text DEFAULT NULL COMMENT '{questId, stage, progress} (opcode 47)',
  `daily_quest` mediumtext DEFAULT NULL COMMENT 'nhiệm vụ ngày - chưa code, giữ nguyên JSON',
  `achievement` mediumtext DEFAULT NULL COMMENT 'thành tựu - chưa code, giữ nguyên JSON',
  `farm` mediumtext DEFAULT NULL COMMENT 'nông trại (cây/vật nuôi) - chưa code, giữ nguyên JSON',
  `friends` text DEFAULT NULL COMMENT 'bạn bè (op 34): [{id,name,avatar,level}] - tên/cấp/ảnh chụp lúc thêm, cập nhật khi bạn online',
  `play_time_seconds` bigint(20) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `last_login_at` datetime DEFAULT NULL,
  `last_logout_at` datetime DEFAULT NULL,
  `buffs` text DEFAULT NULL,
  `item_daily` text DEFAULT NULL,
  `combos` text DEFAULT NULL,
  `altar_day` int(11) NOT NULL DEFAULT 0 COMMENT 'Te dan: ngay (epochDay gio server) cua altar_used',
  `linhmoc_day` int(11) NOT NULL DEFAULT 0 COMMENT 'Rung Linh Moc: ngay (epochDay gio server) di luot gan nhat',
  `tribe_left_at` bigint(20) NOT NULL DEFAULT 0,
  `event_state` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Đang đổ dữ liệu cho bảng `player`
--


-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `player_name_history`
--

CREATE TABLE `player_name_history` (
  `name` varchar(16) NOT NULL COMMENT 'tên cũ đang được giữ',
  `player_id` int(11) NOT NULL COMMENT 'nhân vật giữ tên (player.id)',
  `changed_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'lúc đổi khỏi tên này'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `server_state`
--

CREATE TABLE `server_state` (
  `k` varchar(64) NOT NULL,
  `v` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `server_state`
--


-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `tribe`
--

CREATE TABLE `tribe` (
  `id` int(11) NOT NULL COMMENT 'server tự cấp (tăng dần từ MAX(id))',
  `name` varchar(32) NOT NULL,
  `name_key` varchar(32) NOT NULL COMMENT 'tên viết thường (chống trùng tên) — sửa name thì sửa cả cột này',
  `slogan` varchar(255) NOT NULL DEFAULT '' COMMENT 'khẩu hiệu',
  `level` int(11) NOT NULL DEFAULT 0 COMMENT 'cấp 0..100 (sức chứa = 10 + cấp)',
  `exp` bigint(20) NOT NULL DEFAULT 0 COMMENT 'điểm cống hiến TRONG cấp hiện tại (cấp N cần 3000 + 250 × (N − 1))',
  `gold` bigint(20) NOT NULL DEFAULT 0 COMMENT 'ngân sách vàng',
  `diamond` bigint(20) NOT NULL DEFAULT 0 COMMENT 'ngân sách lam thạch',
  `emblem_id` int(11) NOT NULL DEFAULT 1 COMMENT 'tlnt_data.tribe_emblem.id (1 = Cờ lệnh mặc định)',
  `emblem_expire_at` bigint(20) NOT NULL DEFAULT -1 COMMENT 'hết hạn biểu tượng (epoch ms), -1 = vĩnh viễn',
  `emblem_lines` varchar(1024) NOT NULL DEFAULT '[]' COMMENT 'JSON chỉ số biểu tượng [[stat_type, giá trị], ...]',
  `skills` varchar(255) NOT NULL DEFAULT '[]' COMMENT 'JSON tribe_skill.id theo thứ tự ô [3, 7]',
  `top_points` int(11) NOT NULL DEFAULT 0 COMMENT 'player.id Công thần (0 = chưa ai)',
  `top_diamond` int(11) NOT NULL DEFAULT 0 COMMENT 'player.id Tài chủ',
  `top_gold` int(11) NOT NULL DEFAULT 0 COMMENT 'player.id Kim chủ',
  `members` mediumtext NOT NULL COMMENT 'JSON [{id, name, level, tpl, role (0 Tù trưởng / 1 Phó / 2 Thành viên), points, gold, diamond, joinedAt}]',
  `applicants` text NOT NULL COMMENT 'JSON đơn xin vào [{id, name, level, tpl, at}]',
  `items` mediumtext NOT NULL COMMENT 'JSON Rương bộ lạc (định dạng item gọn như item_bag)',
  `notices` mediumtext NOT NULL COMMENT 'JSON tab Thông báo (chuỗi, cũ trước, tối đa 50)',
  `created_at` bigint(20) NOT NULL COMMENT 'epoch ms',
  `updated_at` bigint(20) NOT NULL COMMENT 'epoch ms'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Đang đổ dữ liệu cho bảng `tribe`
--


-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `tribe_log`
--

CREATE TABLE `tribe_log` (
  `id` bigint(20) NOT NULL,
  `tribe_id` int(11) NOT NULL,
  `at` bigint(20) NOT NULL COMMENT 'epoch ms',
  `player_id` int(11) NOT NULL,
  `player_name` varchar(32) NOT NULL,
  `kind` varchar(16) NOT NULL COMMENT 'create / donate / skill_open / skill_change / shop / emblem / disband',
  `gold` bigint(20) NOT NULL DEFAULT 0 COMMENT 'vàng: + góp vào / - chi ra',
  `diamond` bigint(20) NOT NULL DEFAULT 0 COMMENT 'lam thạch: + góp vào / - chi ra',
  `detail` varchar(255) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Đang đổ dữ liệu cho bảng `tribe_log`
--


-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `web_post`
--

CREATE TABLE `web_post` (
  `id` int(11) NOT NULL,
  `cat` varchar(20) NOT NULL COMMENT 'tin-tuc | huong-dan | su-kien',
  `prefix` varchar(100) NOT NULL DEFAULT '' COMMENT 'nhãn trước tiêu đề, vd [Phó Bản Cá Nhân]',
  `title` varchar(255) NOT NULL,
  `summary` varchar(500) NOT NULL DEFAULT '' COMMENT 'tóm tắt (thẻ Hoạt động nổi bật, mô tả trang)',
  `body` mediumtext NOT NULL,
  `image` varchar(255) NOT NULL DEFAULT '',
  `featured` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1 = hiện ở Hoạt động nổi bật',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT 'thứ tự trong Hoạt động nổi bật (nhỏ trước)',
  `published` tinyint(4) NOT NULL DEFAULT 1 COMMENT '0 = ẩn',
  `published_at` datetime NOT NULL DEFAULT current_timestamp(),
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Đang đổ dữ liệu cho bảng `web_post`
--


-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `web_rate_limit`
--

CREATE TABLE `web_rate_limit` (
  `k` varchar(96) NOT NULL,
  `hits` int(11) NOT NULL,
  `window_start` int(11) NOT NULL COMMENT 'unix giây',
  `blocked_until` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=ascii COLLATE=ascii_general_ci;

--
-- Đang đổ dữ liệu cho bảng `web_rate_limit`
--


-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `web_setting`
--

CREATE TABLE `web_setting` (
  `k` varchar(64) NOT NULL,
  `v` text NOT NULL COMMENT 'giá trị JSON',
  `note` varchar(255) NOT NULL DEFAULT '' COMMENT 'ghi chú cho người sửa',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Đang đổ dữ liệu cho bảng `web_setting`
--

INSERT INTO `web_setting` (`k`, `v`, `note`, `updated_at`) VALUES
('card.amounts', '[10000,20000,30000,50000,100000,200000,300000,500000,1000000]', 'Mệnh giá nhận', '2026-10-22 00:15:05'),
('card.bonus_percent', '0', '% khuyến mãi lam thạch', '2026-10-22 00:15:05'),
('card.callback_ips', '[]', 'IP đối tác được gọi callback ([] = không lọc)', '2026-10-22 00:15:05'),
('card.domain_post', '\"https://doithe-cua-ban.vn\"', 'Tên miền đối tác gạch thẻ, không có / cuối', '2026-10-22 00:15:05'),
('card.enabled', 'true', 'Bật / tắt nạp thẻ', '2026-10-22 00:15:05'),
('card.max_bad_per_hour', '5', 'Thẻ lỗi tối đa / giờ, quá thì khoá nạp 1 giờ', '2026-10-22 00:15:05'),
('card.max_pending', '5', 'Đơn chờ duyệt tối đa / tài khoản', '2026-10-22 00:15:05'),
('card.max_per_10min', '10', 'Thẻ gửi tối đa / 10 phút / tài khoản', '2026-10-22 00:15:05'),
('card.partner_id', '\"\"', 'Partner ID đối tác', '2026-10-22 00:15:05'),
('card.partner_key', '\"\"', 'Partner key đối tác (BÍ MẬT)', '2026-10-22 00:15:05'),
('card.telcos', '[\"VIETTEL\",\"VINAPHONE\",\"MOBIFONE\",\"VIETNAMOBILE\",\"ZING\",\"GATE\",\"GARENA\",\"VCOIN\"]', 'Loại thẻ nhận', '2026-10-22 00:15:05'),
('card.timeout', '20', 'Giây chờ API đối tác', '2026-10-22 00:15:05'),
('card.vnd_per_diamond', '100', 'Số VNĐ = 1 lam thạch', '2026-10-22 00:15:05'),
('card.wrong_value_percent', '50', '% nhận được khi chọn sai mệnh giá', '2026-10-22 00:15:05'),
('site.base_url', '\"http://localhost:8080\"', 'URL gốc web, không có / cuối', '2026-10-22 00:15:05'),
('site.download', '{\"ios\":\"#\",\"android\":\"#\",\"pc\":\"#\",\"apk\":\"#\"}', 'Link tải client của máy chủ', '2026-10-22 00:15:05'),
('site.email', '\"\"', 'Email hỗ trợ (trống = ẩn)', '2026-10-22 00:15:05'),
('site.fanpage', '\"#\"', 'Link fanpage', '2026-10-22 00:15:05'),
('site.group', '\"#\"', 'Link nhóm cộng đồng', '2026-10-22 00:15:05'),
('site.https', 'false', 'true khi chạy https (cookie Secure + HSTS)', '2026-10-22 00:15:05'),
('site.logo', 'false', 'true = dùng ảnh assets/img/logo-site.png thay logo chữ', '2026-10-22 00:15:05'),
('site.name', '\"Tinh Linh Khởi Nguyên\"', 'Tên game hiện trên web', '2026-10-22 00:18:23'),
('site.notice', '\"\"', 'Dòng lưu ý ở trang đăng nhập / nạp thẻ / chân trang (trống = câu mặc định)', '2026-10-22 00:15:05'),
('site.operator', '\"\"', 'Người / nhóm vận hành (trống = ẩn)', '2026-10-22 00:15:05'),
('site.posts_per_page', '5', 'Số bài mỗi trang ở khung Tin tức trang chủ', '2026-10-22 00:15:05'),
('site.privacy', '\"/dieu-khoan.php#bao-mat\"', 'Link chính sách bảo mật', '2026-10-22 00:15:05'),
('site.proxy_header', '\"HTTP_CF_CONNECTING_IP\"', 'Header chứa IP thật khi trust_proxy', '2026-10-22 00:15:05'),
('site.slogan', '\"Khởi đầu vạn vật\"', 'Khẩu hiệu (thẻ title)', '2026-10-22 00:18:23'),
('site.terms', '\"/dieu-khoan.php\"', 'Link điều khoản', '2026-10-22 00:15:05'),
('site.trust_proxy', 'false', 'true nếu đứng sau Cloudflare / nginx', '2026-10-22 00:15:05'),
('site.video', '\"\"', 'Link nhúng YouTube khu Giới thiệu game (trống = ẩn)', '2026-10-22 00:15:05');

--
-- Chỉ mục cho các bảng đã đổ
--

--
-- Chỉ mục cho bảng `account`
--
ALTER TABLE `account`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_account_username` (`username`);

--
-- Chỉ mục cho bảng `card_charge`
--
ALTER TABLE `card_charge`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_card_charge_request` (`request_id`),
  ADD KEY `idx_card_charge_account` (`account_id`,`id`),
  ADD KEY `idx_card_charge_serial` (`telco`,`serial`),
  ADD KEY `idx_card_charge_credit` (`credited`,`status`);

--
-- Chỉ mục cho bảng `mail`
--
ALTER TABLE `mail`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_mail_owner` (`owner_id`,`box`);

--
-- Chỉ mục cho bảng `market_listing`
--
ALTER TABLE `market_listing`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_market_status` (`status`);

--
-- Chỉ mục cho bảng `player`
--
ALTER TABLE `player`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_player_account` (`account_id`),
  ADD UNIQUE KEY `uk_player_name` (`name`);

--
-- Chỉ mục cho bảng `player_name_history`
--
ALTER TABLE `player_name_history`
  ADD PRIMARY KEY (`name`),
  ADD KEY `idx_player` (`player_id`);

--
-- Chỉ mục cho bảng `server_state`
--
ALTER TABLE `server_state`
  ADD PRIMARY KEY (`k`);

--
-- Chỉ mục cho bảng `tribe`
--
ALTER TABLE `tribe`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_tribe_name` (`name_key`);

--
-- Chỉ mục cho bảng `tribe_log`
--
ALTER TABLE `tribe_log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_tribe_log_tribe` (`tribe_id`,`at`);

--
-- Chỉ mục cho bảng `web_post`
--
ALTER TABLE `web_post`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_web_post_list` (`published`,`cat`,`published_at`),
  ADD KEY `idx_web_post_featured` (`published`,`featured`,`sort`);

--
-- Chỉ mục cho bảng `web_rate_limit`
--
ALTER TABLE `web_rate_limit`
  ADD PRIMARY KEY (`k`);

--
-- Chỉ mục cho bảng `web_setting`
--
ALTER TABLE `web_setting`
  ADD PRIMARY KEY (`k`);

--
-- AUTO_INCREMENT cho các bảng đã đổ
--

--
-- AUTO_INCREMENT cho bảng `account`
--
ALTER TABLE `account`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- AUTO_INCREMENT cho bảng `card_charge`
--
ALTER TABLE `card_charge`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- AUTO_INCREMENT cho bảng `player`
--
ALTER TABLE `player`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'playerId gửi cho client', AUTO_INCREMENT=1;

--
-- AUTO_INCREMENT cho bảng `tribe_log`
--
ALTER TABLE `tribe_log`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- AUTO_INCREMENT cho bảng `web_post`
--
ALTER TABLE `web_post`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1;

--
-- Các ràng buộc cho các bảng đã đổ
--

--
-- Các ràng buộc cho bảng `card_charge`
--
ALTER TABLE `card_charge`
  ADD CONSTRAINT `fk_card_charge_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`);

--
-- Các ràng buộc cho bảng `player`
--
ALTER TABLE `player`
  ADD CONSTRAINT `fk_player_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
