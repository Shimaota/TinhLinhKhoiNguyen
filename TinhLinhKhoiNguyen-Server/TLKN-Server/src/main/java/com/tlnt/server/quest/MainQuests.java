package com.tlnt.server.quest;

import com.tlnt.server.item.RewardItem;
import com.tlnt.server.player.PlayerData;

import java.util.List;
import java.util.Map;

import static com.tlnt.server.quest.QuestStep.at;
import static com.tlnt.server.quest.QuestStep.atNpc;
import static com.tlnt.server.quest.QuestStep.collect;
import static com.tlnt.server.quest.QuestStep.collectBoss;
import static com.tlnt.server.quest.QuestStep.collectElite;
import static com.tlnt.server.quest.QuestStep.harvest;
import static com.tlnt.server.quest.QuestStep.harvestFlower;
import static com.tlnt.server.quest.QuestStep.hitBoss;
import static com.tlnt.server.quest.QuestStep.hitWorldBoss;
import static com.tlnt.server.quest.QuestStep.kill;
import static com.tlnt.server.quest.QuestStep.killElite;
import static com.tlnt.server.quest.QuestStep.reach;
import static com.tlnt.server.quest.QuestStep.reachVia;
import static com.tlnt.server.quest.QuestStep.talk;
import static com.tlnt.server.quest.QuestStep.ui;

/**
 * Nội dung nhiệm vụ chính tuyến 1-33 (1-21: thiết kế của người dùng; 22-33: tự thiết kế theo khuôn Linh Mộc / Đầm lầy
 * cho 3 vùng Velmora / Darguun / Frearh — viết thêm nhiệm vụ: thêm 1 khối vào {@link #QUESTS}) + khoá map theo nhiệm vụ. Chỉ viết chữ thường, client tự tô màu bước xong / đang làm / chưa tới.
 */
public final class MainQuests {

   /** {@code npc_template.id}. */
   static final int NPC_BABURU = 1;
   static final int NPC_MORGI = 2;
   static final int NPC_NIMORA = 3;
   static final int NPC_TOORU = 4;
   static final int NPC_BURAN = 7;
   /** Thợ săn Bác Rano (Rừng cổ mộc). */
   static final int NPC_RANO = 9;
   /** Phù thủy Olivar (Rạch cá sấu). */
   static final int NPC_OLIVAR = 8;
   /** Tư tế Buraen (Làng — vào Tế đàn Thiên tích, Đấu trường). */
   static final int NPC_BURAEN = 17;
   static final int NPC_APPLE_TREE = 18;
   static final int NPC_CHEST = 19;
   /** Ngư dân Ông Tataru (Bãi đá ngầm). */
   static final int NPC_TATARU = 13;
   /** Trưởng làng Ông Ribaron (Làng chài Velmora). */
   static final int NPC_RIBARON = 14;
   /** Tù trưởng Bác Kargu (Cao nguyên đá). */
   static final int NPC_KARGU = 15;
   /** Thợ săn Zair (Sông băng Frearh). */
   static final int NPC_ZAIR = 16;
   /** {@code map_template.id}. */
   static final int MAP_CLIFF = 1;
   static final int MAP_VILLAGE = 2;
   static final int MAP_REED_VALLEY = 3;
   static final int MAP_GRASSLAND = 4;
   static final int MAP_FARM = 5;
   static final int MAP_SAYARI = 6;
   static final int MAP_ANCIENT_WOOD = 7;
   static final int MAP_DIVINE_WOOD = 8;
   static final int MAP_AVENNA = 9;
   static final int MAP_CROCODILE_CREEK = 10;
   static final int MAP_MANGROVE = 11;
   /** Vùng Darguun (NV 26-29). */
   static final int MAP_DARGUUN = 12;
   static final int MAP_STONE_PLATEAU = 13;
   static final int MAP_PHONG_THACH = 14;
   /** Vùng Frearh (NV 30-33). */
   static final int MAP_SNOWFIELD = 15;
   static final int MAP_FREARH_RIVER = 16;
   static final int MAP_ICE_MOUNTAIN = 17;
   /** Vùng Velmora (NV 22-25). */
   static final int MAP_VELMORA_COAST = 18;
   static final int MAP_VELMORA_VILLAGE = 19;
   static final int MAP_REEF = 20;
   /** Tầng 1 Tế đàn Thiên tích (boss Tinh linh Bầu trời). */
   static final int MAP_ALTAR_1 = 34;
   /** Rừng Linh Mộc (phó bản tổ đội): map đầu, map boss Hắc thần, map cuối (Tinh linh Thần mộc). */
   static final int MAP_LINH_MOC_1 = 25;
   static final int MAP_LINH_MOC_BOSS = 29;
   static final int MAP_LINH_MOC_LAST = 30;
   /** {@code mob_template.id}. */
   static final int MOB_TUSK_BOAR = 2;
   static final int MOB_BISON = 3;
   static final int MOB_RHINO = 4;
   static final int MOB_WARRIOR_BOAR = 5;
   static final int MOB_SCARECROW = 33;
   static final int MOB_SABERTOOTH = 7;
   static final int MOB_SAYARI_LEOPARD = 8;
   static final int MOB_MAN_EATER = 9;
   static final int MOB_HORNET = 11;
   static final int MOB_SAYARI_GORILLA = 12;
   static final int MOB_SWAMP_CRAB = 13;
   /** Cây ăn thịt của Đầm lầy (mob 14 — khác Cây ăn thịt Sayari mob 9). */
   static final int MOB_SWAMP_MAN_EATER = 14;
   static final int MOB_POISON_FROG = 15;
   static final int MOB_HERMIT_CRAB = 18;
   static final int MOB_MUDSKIPPER = 19;
   static final int MOB_SAWFISH = 20;
   static final int MOB_SEAGULL = 21;
   static final int MOB_VELMORA_TIGERFISH = 22;
   static final int MOB_ANTELOPE = 23;
   static final int MOB_DARGUUN_HYENA = 24;
   static final int MOB_MOUNTAIN_LION = 25;
   static final int MOB_HUNTER_HAWK = 26;
   static final int MOB_EARTH_DRAGON = 27;
   static final int MOB_YAK = 28;
   static final int MOB_SNOW_WOLF = 29;
   static final int MOB_SNOW_LEOPARD = 30;
   static final int MOB_SNOW_OWL = 31;
   static final int MOB_MAMMOTH = 32;
   /** {@code plant_template.id} Nấm hương. */
   static final int PLANT_SHIITAKE = 10;
   /** {@code plant_template.id} Hoa Tử nguyệt — mọi cây hoa (1-6) mọc cùng map 9 / 10 / 11 (mũi tên bước hái hoa). */
   static final int PLANT_FLOWER = 1;
   /** {@code item_template.id} phần thưởng: 281 Kinh nghiệm (cộng EXP, không vào túi), 1 Vàng, 145 Sách tiềm năng. */
   static final int EXP = 281;
   private static final int GOLD = 1;
   private static final int TALENT_BOOK = 145;
   private static final int DIAMOND = 2;
   static final int SUN_STONE = 125;
   static final int MOON_STONE = 126;
   private static final int ORIGIN_STONE = 274;
   /** Túi đá nguyên tố. */
   private static final int ELEMENT_STONE_BAG = 269;
   /** Quả Linh Mộc. */
   private static final int LINH_MOC_FRUIT = 288;
   /** Bùa linh mộc (16.2: rơi riêng khi tham gia hạ boss). */
   static final int LINH_MOC_CHARM = 263;
   /** Ngoại trang Tinh linh Rừng xanh (thưởng NV 17). */
   private static final int FOREST_SPIRIT_COSTUME = 187;
   /** {@code tlnt_data.world_boss.id}: Vệ thần Rừng xanh / Tinh Linh Rừng xanh. */
   static final int WORLD_BOSS_GUARD = 1;
   static final int WORLD_BOSS_SPIRIT = 2;
   /** Boss thế giới các vùng sau: Vệ thần / Tinh Linh Đầm lầy, Biển cả, Phong thạch, Băng giá. */
   static final int WORLD_BOSS_SWAMP_GUARD = 9;
   static final int WORLD_BOSS_SWAMP_SPIRIT = 10;
   static final int WORLD_BOSS_SEA_GUARD = 11;
   static final int WORLD_BOSS_SEA_SPIRIT = 12;
   static final int WORLD_BOSS_STONE_GUARD = 13;
   static final int WORLD_BOSS_STONE_SPIRIT = 14;
   static final int WORLD_BOSS_ICE_GUARD = 15;
   static final int WORLD_BOSS_ICE_SPIRIT = 16;
   /** Vật phẩm rơi riêng từ boss (bước {@link StepKind#COLLECT_BOSS} NV 21 / 24 / 28 / 32 — giống Bùa linh mộc NV 16). */
   static final int U_MOC_RING = 279;
   static final int SEA_SHELL_SHIELD = 165;
   static final int DARGUUN_STORM_EYE = 264;
   static final int ICE_HEART = 278;
   /** Mặt nạ Velmora / Darguun / Frearh (thưởng NV 25 / 29 / 33 — như ngoại trang 187 của NV 17). */
   private static final int VELMORA_MASK = 366;
   private static final int DARGUUN_MASK = 367;
   private static final int FREARH_MASK = 368;
   /** Nanh Hổ răng kiếm — rơi riêng từ Hổ răng kiếm Tinh anh / Thủ lĩnh ở bước 13.1. */
   static final int SABERTOOTH_FANG = 262;
   /** Bùa bảo hiểm Tinh linh (thưởng NV 9). */
   private static final int SPIRIT_CHARM = 310;
   /** Thú cưng Moara (item loại 37 -> char_template_id trong item_appearance) — hộ tống ở bước 9.4. */
   private static final int MOARA = 290;
   /** Túi Khởi nguyên (túi tân thủ). */
   private static final int STARTER_BAG = 298;
   /** Nguyên liệu rèn vũ khí NV 5 (rơi riêng khi hạ quái ở bước 5.2 / 5.3). */
   static final int ROUGH_STONE = 267;
   static final int BEAST_BONE = 266;
   /** {@code tlnt_data.shop.id} cửa hàng dược phẩm (Nimora). */
   private static final int PHARMACY_SHOP = 2;

   private static final String ELITE_HINT = "Tinh anh là những cá thể sở hữu sức mạnh vượt trội, mỗi khi quái bị đánh"
         + " bại chúng có tỉ lệ hồi sinh thành Tinh anh. Các thành viên trong tổ đội có thể giúp hoàn thành nhiệm vụ cùng"
         + " nhau.";
   private static final String BACK_TO_TOORU = "Quay trở lại gặp ông trưởng lão Tooru để báo cáo hoàn thành nhiệm vụ";

   private static final List<MainQuest> QUESTS = List.of(
         new MainQuest(1, "Bước Chân Đầu Tiên",
               "Bạn vừa đặt chân đến ngôi làng nhỏ tại Eldarah. Hãy làm quen với công việc nơi đây để bắt đầu hành"
                     + " trình của mình.",
               List.of(locked(EXP, 1_000), locked(GOLD, 10_000), locked(TALENT_BOOK, 20)), 1,
               List.of(
                     talk("Nói chuyện với Trưởng làng", NPC_BABURU, MAP_VILLAGE,
                           "Nhà của trưởng làng nằm ở giữa Làng ánh sáng Eldarah, hãy đi theo chỉ dẫn đến gặp trưởng"
                                 + " làng và nói chuyện với ông ấy.",
                           "Bỏ qua",
                           "Phía nông trại sau làng dạo này có chút lộn xộn. Trưởng lão vẫn hay ra đó trông nom.\n"
                                 + "Nhưng ông ấy cũng lớn tuổi rồi, làm một mình không xuể.",
                           "Ta còn phải lo việc trong làng, không tiện qua đó thường xuyên.\n"
                                 + "Con hãy thay ta qua đó để giúp ông ấy một tay nhé."),
                     talk("Nói chuyện với Trưởng lão", NPC_TOORU, MAP_FARM,
                           "Trưởng lão đang trông nom nông trại phía sau làng, hãy đi theo chỉ dẫn đến gặp trưởng lão"
                                 + " và nói chuyện với ông ấy.",
                           "Bỏ qua",
                           "Cậu đến thật đúng lúc!\n"
                                 + "Dạo này người trong làng bận nhiều việc... nông trại lại thiếu người. Ta thì già"
                                 + " rồi, cũng không làm được nhiều việc nặng nữa.\n"
                                 + "Hãy giúp ta thu hoạch cây táo đằng kia nhé!"),
                     at(StepKind.HARVEST_APPLE, "Thu hoạch Cây táo", NPC_APPLE_TREE, MAP_FARM,
                           "Cây táo ở Nông trại và cho ra quả thường xuyên sau mỗi phút, hãy đi theo chỉ dẫn và tới"
                                 + " thu hoạch lấy Quả táo."),
                     talk("Nói chuyện với Trưởng lão", NPC_TOORU, MAP_FARM, BACK_TO_TOORU, "Đóng",
                           "Cảm ơn cậu nhé! Cây táo dạo này ra quả nhiều hơn trước, nếu không có cậu nhắc ta phải"
                                 + " loay hoay đến chiều mới xong.\n"
                                 + "Cậu hãy mang theo vài Quả táo khi ra khỏi làng để phòng khi cần sử dụng.",
                           "Táo đỏ sẽ giúp con hồi lại HP còn táo xanh sẽ giúp con hồi lại MP khi sử dụng.\n"
                                 + "Nhưng nếu cứ làm mọi thứ bằng tay sẽ rất bất tiện.\n"
                                 + "Trong phần cài đặt, con có thể gán phím tắt và cài đặt tự động sử dụng Quả táo"
                                 + " khi cần thiết.",
                           "Khi mọi thứ đã sẵn sàng con hãy ra ngoài đầu làng...\n"
                                 + "Ở đó có những hình nhân bằng rơm, người trong làng dựng lên để luyện tập. Chúng"
                                 + " không phản kháng nên cũng dễ làm quen.\n"
                                 + "Hãy đánh hạ 5 người rơm để làm quen với cách ra đòn và kiểm soát lực đánh nhé."))),
         new MainQuest(2, "Rèn Luyện Khởi Đầu",
               "Bạn bắt đầu làm quen với cuộc sống tại Eldarah. Hãy học cách sử dụng vật phẩm hồi phục, thiết lập phím"
                     + " tắt để chiến đấu hiệu quả và thử sức với những hình nộm bằng rơm.",
               List.of(locked(EXP, 2_000), locked(GOLD, 20_000), locked(TALENT_BOOK, 50), locked(234, 10)), -1,
               List.of(
                     ui(StepKind.BIND_POTIONS, "Gán phím tắt", 2,
                           "Thiết lập phím tắt để có thể nhanh chóng sử dụng vật phẩm hồi HP và MP trong những tình"
                                 + " huống cần thiết."),
                     kill("Đánh hạ 5 người rơm", MOB_SCARECROW, 5, MAP_VILLAGE,
                           "Những hình nhân rơm được sử dụng quanh làng để luyện tập. Dù không phản kháng, chúng vẫn"
                                 + " giúp làm quen với cách ra đòn và kiểm soát lực đánh."),
                     ui(StepKind.USE_APPLE, "Sử dụng Quả táo", 1,
                           "Khi bị thương hoặc suy yếu việc hồi phục là rất quan trọng. Sử dụng Quả táo để hồi phục"
                                 + " lại sức lực khi cần thiết."),
                     talk("Nói chuyện với Trưởng lão", NPC_TOORU, MAP_FARM, BACK_TO_TOORU, "Đóng",
                           "Tốt lắm... có vẻ cậu đã làm quen được với cách chiến đấu, ra đòn và kiểm soát lực đánh.\n"
                                 + "Cậu có thể thiết lập những tính năng tự động để việc luyện tập đạt hiệu quả tốt"
                                 + " hơn.\n"
                                 + "Ngoài ra ta cũng chuẩn bị cho cậu trang bị ở trong chiếc rương kia, hãy lấy ra và"
                                 + " sử dụng nhé."))),
         new MainQuest(3, "Sẵn Sàng Chiến Đấu",
               "Với những trang bị cơ bản sức mạnh của bạn đã được cải thiện, bạn hãy sẵn sàng bước những bước đi đầu"
                     + " tiên. Hãy chuẩn bị tinh thần đối mặt với những thử thách phía trước.",
               List.of(locked(EXP, 3_500), locked(GOLD, 100_000), locked(TALENT_BOOK, 50)), 0,
               List.of(
                     ui(StepKind.AUTO_SETTINGS, "Học cách cài đặt", 1,
                           "Mở Menu cài đặt để tìm hiểu cách cài đặt và tùy chỉnh điều khiển, tối ưu trải nghiệm chiến"
                                 + " đấu."),
                     // Client bật hướng dẫn theo SỐ THỨ TỰ bước (NV 3: bước 3 = lấy đồ khỏi rương
                     // TutorialTreasureChestStep, 4 = mặc Áo, 5 = cộng tiềm năng) -> rương tách 2 bước cho khớp.
                     at(StepKind.OPEN_CHEST, "Mở rương đồ", NPC_CHEST, MAP_FARM,
                           "Rương được đặt tại nông trại, bên trong có chứa những vật phẩm khởi đầu cần thiết mà"
                                 + " Trưởng lão đã chuẩn bị."),
                     at(StepKind.TAKE_FROM_CHEST, "Lấy trang bị trong rương", NPC_CHEST, MAP_FARM,
                           "Trưởng lão đã chuẩn bị sẵn trang bị trong rương, hãy lấy trang bị ra hành trang."),
                     ui(StepKind.EQUIP, "Mặc trang bị", 1,
                           "Trang bị là yếu tố quan trọng trong chiến đấu, hãy sử dụng trang bị vừa lấy từ rương để"
                                 + " gia tăng sức mạnh."),
                     ui(StepKind.ADD_TALENT, "Sử dụng điểm tiềm năng", 1,
                           "Mở bảng kỹ năng nhân vật và sử dụng điểm tiềm năng phân bổ vào các chỉ số mong muốn giúp"
                                 + " tăng sức mạnh của bản thân."),
                     talk("Nói chuyện với Trưởng lão", NPC_TOORU, MAP_FARM, BACK_TO_TOORU, "Đóng",
                           "Tốt lắm! Những thứ cơ bản cậu đã nắm được rồi, nhìn cũng không còn lúng túng như lúc đầu"
                                 + " nữa.\n"
                                 + "Điều này tuy không lớn... nhưng thiếu đi thì khó tiến xa được.",
                           "Bên ngoài làng thường có Heo răng nanh, nếu cậu muốn thử sức hãy tham gia một cuộc đi"
                                 + " săn.\n"
                                 + "Tới Thung lũng cỏ lau và đánh bại 10 Heo răng nanh nhé."))),
         new MainQuest(4, "Săn Bắt Hái Lượm",
               "Đã đến lúc bạn bước vào cuộc săn đầu tiên. Thung lũng Eldarah trù phú có rất nhiều tài nguyên nhưng"
                     + " cũng ẩn chứa không ít hiểm nguy... Hãy cẩn thận!",
               List.of(locked(EXP, 5_000), locked(GOLD, 30_000), locked(TALENT_BOOK, 50), locked(235, 3)), -1,
               List.of(
                     kill("Đánh hạ 10 Heo răng nanh", MOB_TUSK_BOAR, 10, MAP_REED_VALLEY,
                           "Heo răng nanh thường xuất hiện quanh Thung lũng cỏ lau và có thể gây nguy hiểm nếu không"
                                 + " để ý."),
                     at(StepKind.UPGRADE_APPLE_TREE, "Nâng cấp cây táo", NPC_APPLE_TREE, MAP_FARM,
                           "Cây táo trong nông trại có thể được chăm sóc để phát triển tốt hơn. Hãy nâng cấp cây táo"
                                 + " để tăng sản lượng và chất lượng."),
                     talk("Nói chuyện với Trưởng lão", NPC_TOORU, MAP_FARM, BACK_TO_TOORU, "Đóng",
                           "Thế nào? Chuyến đi săn tốt chứ... Trông cậu vẫn ổn, có vẻ cũng không gặp rắc rối lớn.\n"
                                 + "Nhưng ta thấy cậu vẫn thiếu một thứ gì đó thì phải."))),
         new MainQuest(5, "Chế Tạo Công Cụ",
               "Để sinh tồn bạn cần biết cách sử dụng những công cụ cơ bản. Hãy thu thập nguyên liệu và gặp Thợ rèn"
                     + " vũ khí. Hãy đi theo chỉ dẫn để gặp và nói chuyện với ông ấy.",
               List.of(locked(EXP, 10_000), locked(GOLD, 200_000), locked(SUN_STONE, 50), locked(MOON_STONE, 50)), 1,
               List.of(
                     talk("Nói chuyện với Thợ rèn", NPC_MORGI, MAP_VILLAGE,
                           "Nhà của ông Thợ rèn Morgi nằm ở đầu làng, các thế hệ chiến binh của làng đều được ông ấy"
                                 + " rèn vũ khí. Hãy đi theo hướng dẫn.",
                           "Đóng",
                           "Chào cậu!... cậu là người mới đến hả?\n"
                                 + "Ta là thợ rèn Morgi, suốt ngày ta chỉ quanh quẩn bên lò rèn này thôi.",
                           "Nếu cậu cần một món vũ khí phù hợp... hãy tìm giúp ta nguyên liệu vừa tay cậu nhé.\n"
                                 + "Hãy thu thập 5 Đá thô sơ và 1 Xương thú, ta sẽ giúp cậu rèn một món vũ khí phù"
                                 + " hợp."),
                     // 5.2 / 5.3: đang ở bước này hạ quái bất kỳ (trừ người rơm) rơi 1 món RIÊNG (QuestEvents.questDrop)
                     collect("Thu thập 5 Đá thô sơ", ROUGH_STONE, 5, MAP_REED_VALLEY, MOB_TUSK_BOAR,
                           "Đá thô sơ là nguyên liệu cơ bản để chế tạo ra vũ khí. Hãy đánh bại những con quái bất kỳ"
                                 + " để thu thập lấy 5 Đá thô sơ."),
                     collect("Thu thập 1 Xương thú", BEAST_BONE, 1, MAP_REED_VALLEY, MOB_TUSK_BOAR,
                           "Xương thú là nguyên liệu cơ bản để tạo ra tay cầm. Hãy đánh bại những con quái bất kỳ để"
                                 + " thu thập lấy 1 Xương thú."),
                     at(StepKind.CRAFT_WEAPON, "Chế tác vũ khí", NPC_MORGI, MAP_VILLAGE,
                           "Sau khi đã thu thập đủ nguyên liệu, hãy quay lại gặp ông Thợ rèn Morgi nhờ ông ấy chế tác"
                                 + " một vũ khí bất kỳ."),
                     ui(StepKind.EQUIP_WEAPON, "Sử dụng vũ khí", 1,
                           "Mỗi loại vũ khí đều ẩn chứa sức mạnh riêng. Hãy sử dụng nó để kích hoạt và mở khóa toàn"
                                 + " bộ kỹ năng mới."),
                     ui(StepKind.ASSIGN_SKILL, "Gán phím tắt kỹ năng", 1,
                           "Mỗi vũ khí đều mang theo một bộ kỹ năng riêng. Hãy gán kỹ năng đó vào phím tắt để có thể"
                                 + " sử dụng trong chiến đấu."),
                     talk("Nói chuyện với Thợ rèn", NPC_MORGI, MAP_VILLAGE,
                           "Quay trở lại đầu làng gặp ông Thợ rèn Morgi để báo cáo và hoàn thành nhiệm vụ.", "Đóng",
                           "Khá lắm!... Đã biết cách dùng vũ khí rồi đúng không?\n"
                                 + "Mấy thứ này dùng quen tay sẽ khác ngay. Hãy giữ lấy cảm giác đó... rồi tiếp tục rèn"
                                 + " luyện nhé.",
                           "Nếu cậu cần một món vũ khí phù hợp... hãy tìm giúp ta nguyên liệu vừa tay cậu nhé.\n"
                                 + "Hãy thu thập Đá thô sơ và Xương thú, ta sẽ giúp cậu rèn một món vũ khí phù hợp.",
                           "Nếu muốn vũ khí mạnh hơn... Ta có thể giúp cậu nâng cấp chúng lên.\n"
                                 + "Cậu có thể dùng Đá Mặt Trời hoặc Đá Mặt Trăng để làm nguyên liệu. Mỗi loại đá sẽ cho"
                                 + " cậu một ấn ký đặc biệt."))),
         new MainQuest(6, "Ấn Ký Nhật Nguyệt",
               "Tìm hiểu về 2 loại đá cường hóa trang bị. Mỗi lựa chọn sẽ ảnh hưởng đến cách bạn chiến đấu, hãy thử"
                     + " nghiệm và tìm ra hướng phù hợp nhất với bản thân.",
               // TODO(người dùng): thêm quà "ID 298 khoá, option [9,200] [30,5] [32,5], hạn 7 ngày" — tạm bỏ theo yêu
               // cầu 2026-10-05; Túi Khởi nguyên (túi tân thủ) 298 vẫn tặng.
               List.of(locked(EXP, 15_000), locked(GOLD, 55_000), locked(SUN_STONE, 50), locked(DIAMOND, 5),
                     locked(STARTER_BAG, 1)), -1,
               List.of(
                     // Client: bước 1 = TutorialWeaponForgeStep (nâng Áo), bước 2 = TutorialEquipmentUpgradeStep (vũ khí)
                     atNpc(StepKind.UPGRADE_STONE, "Khảm đá Mặt Trời", SUN_STONE, NPC_MORGI, MAP_VILLAGE,
                           "Tới đầu làng gặp ông Thợ rèn Morgi, sử dụng Đá Mặt Trời để nâng cấp trang bị. Sức mạnh"
                                 + " của trang bị sẽ được cải thiện rõ rệt sau mỗi lần cường hóa."),
                     atNpc(StepKind.UPGRADE_STONE, "Khảm đá Mặt Trăng", MOON_STONE, NPC_MORGI, MAP_VILLAGE,
                           "Tới đầu làng gặp ông Thợ rèn Morgi, sử dụng Đá Mặt Trăng để nâng cấp trang bị. Sức mạnh"
                                 + " của trang bị sẽ được cải thiện rõ rệt sau mỗi lần cường hóa."),
                     ui(StepKind.REACH_LEVEL, "Luyện tập đạt cấp 5", 5,
                           "Hãy tiếp tục luyện tập và chiến đấu đạt cấp độ 5. Việc nâng cao cấp độ sẽ giúp trở nên"
                                 + " mạnh mẽ hơn và chuẩn bị cho các thử thách sắp tới."),
                     talk("Nói chuyện với Trưởng lão", NPC_TOORU, MAP_FARM, BACK_TO_TOORU, "Đóng",
                           "Vừa từ chỗ thợ rèn về đó hả!.. Ta thấy cậu đã có trang bị riêng, cũng đã biết sử dụng kỹ"
                                 + " năng rồi nhỉ.\n"
                                 + "Nhìn cũng ra dáng hơn rồi đấy, những thứ cần thiết coi như đã đủ."))),
         new MainQuest(7, "Hành Trang Dược Liệu",
               "Trước khi bước vào những cuộc đi săn, bạn nên chuẩn bị đầy đủ những vật phẩm cần thiết, đặc biệt là"
                     + " những loại dược liệu hỗ trợ chiến đấu.",
               List.of(locked(EXP, 20_000), locked(GOLD, 75_000), locked(SUN_STONE, 50), locked(ORIGIN_STONE, 200)), 0,
               List.of(
                     talk("Nói chuyện với Dược sư", NPC_NIMORA, MAP_VILLAGE,
                           "Hãy tìm Nimora trong làng và trò chuyện với cô ấy để tìm hiểu thêm về các loại dược phẩm"
                                 + " hỗ trợ cho những chuyến đi săn.",
                           "Đóng",
                           "Nếu đã đến đây cậu cũng biết mình cần gì rồi nhỉ?\n"
                                 + "Ở đây ta có một vài loại dược phẩm cơ bản, chúng khá lành tính và có thể hỗ trợ cậu"
                                 + " trong những chuyến đi săn."),
                     // Client: bước 2 = TutorialPharmacyNpcStep (dẫn tới Nimora -> cửa hàng dược phẩm)
                     atNpc(StepKind.BUY_SHOP, "Mua 1 dược phẩm bất kỳ", PHARMACY_SHOP, NPC_NIMORA, MAP_VILLAGE,
                           "Chuẩn bị trước luôn là lợi thế. Hãy mua một dược phẩm bất kỳ từ Nimora để chuẩn bị cho"
                                 + " những chuyến đi sắp tới."),
                     kill("Đánh hạ 25 Bò rừng Eldarah", MOB_BISON, 25, MAP_REED_VALLEY,
                           "Bò rừng thường di chuyển theo bầy ở khu vực Thung lũng cỏ lau và Thảo nguyên mạch gió."
                                 + " Chúng không nhanh nhưng rất lì đòn và khó bị hạ gục."),
                     kill("Đánh hạ 25 Tê giác Eldarah", MOB_RHINO, 25, MAP_GRASSLAND,
                           "Tê giác thường xuất hiện ở Thảo nguyên mạch gió. Chúng không nhanh nhưng rất lì đòn và khó"
                                 + " bị hạ gục."),
                     talk("Nói chuyện với Trưởng lão", NPC_TOORU, MAP_FARM, BACK_TO_TOORU, "Đóng",
                           "Làm tốt lắm!... Cậu đã trưởng thành hơn trước rất nhiều.\n"
                                 + "Ngoài kia có rất nhiều những chiến binh cùng ý chí với cậu.",
                           "Cậu hãy tìm cho mình những người đồng hành, kết bạn với họ và lập tổ đội.\n"
                                 + "Chỉ khi sát cánh cùng nhau, cậu mới có thể vượt qua những thử thách phía trước."))),
         new MainQuest(8, "Sức Mạnh Đồng Đội",
               "Một tổ đội sát cánh cùng bạn có thể giúp bạn vượt qua những thử thách khó khăn nhất. Phối hợp và chiến"
                     + " đấu như một thể thống nhất, đó là chìa khóa của chiến thắng.",
               List.of(locked(EXP, 35_000), locked(GOLD, 100_000), locked(SUN_STONE, 50), locked(ORIGIN_STONE, 500)), -1,
               List.of(
                     ui(StepKind.FRIEND_INVITE, "Gửi lời mời kết bạn với 5 người", 5,
                           "Hãy tìm kiếm những người đồng hành, kết bạn với 5 người chơi và bắt đầu hành trình cùng"
                                 + " nhau."),
                     ui(StepKind.JOIN_PARTY, "Gia nhập 1 tổ đội", 1,
                           "Hãy tìm một tổ đội hoặc tạo tổ đội của riêng mình, sát cánh cùng những chiến binh khác để"
                                 + " vượt qua thử thách sắp tới."),
                     killElite("Đánh bại Tinh anh Heo răng nanh", MOB_TUSK_BOAR, 1, MAP_REED_VALLEY, ELITE_HINT),
                     killElite("Đánh bại Tinh anh Bò rừng Eldarah", MOB_BISON, 1, MAP_REED_VALLEY, ELITE_HINT),
                     talk("Nói chuyện với Trưởng lão", NPC_TOORU, MAP_FARM, BACK_TO_TOORU, "Đóng",
                           "Tốt lắm!... Con đã biết cách chiến đấu sát cánh cùng những người đồng đội.\n"
                                 + "Trong những cuộc đi săn lâu ngày hãy phối hợp cùng họ để gia tăng sức mạnh hiệu quả"
                                 + " nhé.",
                           "Ta nhận được tin từ trưởng làng!... Moara đã đi lạc về hướng Vách núi Eldarah.\n"
                                 + "Trận động đất hôm trước đã khiến Moara bị hoảng loạn, nó rất nhạy cảm với sự thay"
                                 + " đổi của môi trường."))),
         // Thưởng khi bấm "Tiếp tục" ở trang 3 của Baburu = lúc trang 4 hiện ra (rewardPage 2).
         new MainQuest(9, "Dấu Chân Nơi Vách Núi",
               "Moara đã bất ngờ hoảng loạn và chạy thẳng lên Vách núi Eldarah. Không giống như mọi lần, lần này nó"
                     + " dường như đã cảm nhận được một điều gì đó bất thường. Hãy cẩn thận.",
               List.of(locked(EXP, 75_000), locked(GOLD, 130_000), locked(SPIRIT_CHARM, 1)), 2,
               List.of(
                     reach("Đến Vách núi Eldarah", MAP_CLIFF,
                           "Vách núi Eldarah nằm ở phía sau Làng ánh sáng, nơi địa hình hiểm trở. Hãy men theo con"
                                 + " đường phía sau làng để đến đó."),
                     kill("Đánh hạ 50 Heo chiến binh", MOB_WARRIOR_BOAR, 50, MAP_CLIFF,
                           "Heo chiến binh thường xuất hiện quanh khu vực Vách núi Eldarah, chúng rất hiếu chiến và"
                                 + " luôn sẵn sàng lao vào bất kỳ kẻ thù nào."),
                     talk("Nói chuyện với Buran", NPC_BURAN, MAP_CLIFF,
                           "Moara đã chạy về phía Vách núi Eldarah sau trận động đất và Buran đã tìm được nó. Hãy tới"
                                 + " Vách núi Eldarah để giúp cậu ấy.",
                           "Đóng",
                           "Cậu đến đúng lúc đấy! Tớ tìm được Moara nhưng lũ heo chiến binh đã kéo tới đây... Tớ tạm"
                                 + " thời chưa thể về làng.\n"
                                 + "Hãy giúp tớ hộ tống Moara nhé... Tớ sẽ xử lý lũ heo này."),
                     // Moara đi theo suốt bước này (world/Escort), chỉ ở Vách núi + Làng; bỏ xa / sang map khác /
                     // bị hạ gục / thoát game -> lùi về 9.3
                     talk("Hộ tống Moara", NPC_BABURU, MAP_VILLAGE,
                           "Moara đã được tìm thấy, nhưng nó vẫn còn hoảng loạn sau trận động đất. Sinh vật nhỏ bé này"
                                 + " rất nhạy cảm và dễ bị kích động. Hãy đưa Moara về Làng ánh sáng gặp ông Baburu,"
                                 + " đừng bỏ nó lại quá xa.",
                           "Đóng",
                           "Cảm ơn con đã đưa Moara về đây, con đã vất vả rồi.\n"
                                 + "Sức mạnh càng cao thì con càng phải cẩn thận hơn nhé.",
                           "Rất lâu về trước... vùng đất này chỉ là nơi cằn cỗi và đầy hiểm họa, người dân Eldarah đã"
                                 + " có một giao ước với Tinh linh Bầu trời.\n"
                                 + "Cứ mỗi đời thủ lĩnh của bộ lạc đều sẽ phải chọn ra một người để hiến tế.\n"
                                 + "Đổi lại là sự bảo hộ, mưa thuận gió hòa và đất đai trù phú.\n"
                                 + "Từ đó, Eldarah được che chở... mùa màng tốt tươi, muông thú sinh sôi, bộ tộc ngày một"
                                 + " hưng thịnh.",
                           "Nhưng rồi Thủ lĩnh Gorun và con trai ông ấy là Rinn đã đứng lên chống lại khế ước ấy.\n"
                                 + "Họ đã từ chối thực hiện nghi thức hiến tế...\n"
                                 + "Không chấp nhận việc lấy sinh mạng của kẻ vô tội để đổi lấy sự bình yên giả tạo.\n"
                                 + "Một quyết định khiến cả bộ tộc khiếp sợ... nhưng cũng là lần đầu tiên Eldarah được tự"
                                 + " do.",
                           "Một trận chiến đổi lấy tự do nhưng cũng để lại những vết sẹo không thể xóa nhòa.\n"
                                 + "Dù năm ấy chúng ta tạm thời đẩy lùi được cơn thịnh nộ của Bầu trời nhưng những người"
                                 + " chiến binh của bộ tộc đã phải nằm xuống.\n"
                                 + "Tuy nhiên mọi thứ chưa kết thúc. Cơn thịnh nộ ấy chỉ tạm thời bị đẩy lùi. Nó đang chờ"
                                 + " đợi... Ta có linh cảm rằng nó sắp quay trở lại.").escorting(MOARA, MAP_CLIFF, MAP_VILLAGE))),
         // NV 10: thưởng khi bấm "Tiếp tục" ở trang 1 của Baburu (bước 10.3, rewardPage 0).
         new MainQuest(10, "Điềm Báo Từ Bầu Trời",
               "Sau biến cố năm xưa khi giao ước bị phá vỡ, Tinh linh Bầu Trời đã trở lại. Đây không phải một trận chiến"
                     + " mà là lời cảnh báo: cơn thịnh nộ của bầu trời đang dần quay lại.",
               List.of(locked(EXP, 100_000), locked(GOLD, 170_000), locked(ORIGIN_STONE, 500),
                     locked(ELEMENT_STONE_BAG, 20)), 0,
               List.of(
                     // Vào phó bản (map 34) = xong; chỉ đường tới Tư tế Buraen ở Làng (Tế đàn không có cửa dịch chuyển)
                     reachVia("Tới Tế đàn Thiên tích", MAP_ALTAR_1, NPC_BURAEN, MAP_VILLAGE,
                           "Tới gặp ông Tư tế để tới Tế đàn Thiên tích. Nơi Tế đàn năm xưa người dân Eldarah đã đứng"
                                 + " lên chống lại giao ước với Tinh linh Bầu trời."),
                     hitBoss("Chạm trán Tinh linh Bầu trời", MAP_ALTAR_1, 100,
                           "Sau nhiều năm bị phong ấn, dấu hiệu thức tỉnh của Tinh linh Bầu trời lại một lần nữa xuất"
                                 + " hiện tại Tế đàn Thiên tích. Hãy gây sát thương lên Tinh linh Bầu trời 100 lần."),
                     talk("Nói chuyện với Trưởng làng", NPC_BABURU, MAP_VILLAGE,
                           "Quay trở lại gặp ông trưởng làng Baburu để báo cáo và hoàn thành nhiệm vụ.", "Đóng",
                           "Vậy là con đã chạm trán với Tinh Linh Bầu Trời?... Vậy là những gì ta lo sợ đã thành sự"
                                 + " thật.\n"
                                 + "Đây mới chỉ là lời cảnh báo. Ta e sắp tới Eldarah sẽ phải đối mặt với cơn thịnh nộ"
                                 + " chưa từng có từ Bầu trời.",
                           "Từ sau trận chiến lần trước, Làng ánh sáng luôn tổ chức các giải đấu trên Thượng nguồn"
                                 + " Eldarah.\n"
                                 + "Nhằm tìm ra những chiến binh mạnh nhất chiến đấu bảo vệ ngôi làng khỏi thực thể tối"
                                 + " cao ấy.\n"
                                 + "Ta thấy con rất có tiềm năng. Hãy tới gặp ông Tư tế để đăng ký tham gia nhé."))),
         // NV 11: thưởng khi bấm "Nói chuyện" ở bước 11.3 (rewardPage -1).
         new MainQuest(11, "Giải Đấu Bộ Lạc",
               "Giải đấu Bộ lạc diễn ra hàng ngày với các nhóm giải Nhi đồng, Chiến binh, Thủ lĩnh, Ngoại hạng... nhằm"
                     + " chọn ra những chiến binh mạnh nhất bảo vệ Eldarah. Hãy gặp Tư tế Buraen để đăng ký tham gia.",
               List.of(locked(EXP, 150_000), locked(GOLD, 220_000), locked(ORIGIN_STONE, 500)), -1,
               List.of(
                     talk("Nói chuyện với Tư tế", NPC_BURAEN, MAP_VILLAGE,
                           "Tư tế Buraen ở phía cuối làng dưới chân Thượng nguồn Eldarah. Người có thể kết nối với"
                                 + " thế giới thần linh và tổ chức các giải đấu.",
                           "Đóng",
                           "Ta đã nghe Trưởng làng nhắc đến cậu... Cậu đã chạm trán với Tinh linh Bầu Trời rồi hả?\n"
                                 + "Giải đấu tại Thượng nguồn Eldarah không phải trò chơi, đó là nơi chọn ra những chiến"
                                 + " binh có đủ bản lĩnh.\n"
                                 + "Nếu cậu đã sẵn sàng, hãy để tên mình được ghi vào danh sách thi đấu."),
                     // Giải đấu chưa làm: bấm nút "Giải đấu Bộ lạc" (Buraen > Đấu trường) là xong
                     atNpc(StepKind.JOIN_TOURNAMENT, "Tham gia Giải đấu Bộ lạc", 0, NPC_BURAEN, MAP_VILLAGE,
                           "Vào những khung giờ cố định trong ngày, những chiến binh từ khắp nơi tụ họp tại Làng ánh"
                                 + " sáng để tham gia Giải đấu Bộ lạc. Hãy tới gặp Tư tế Buraen để ghi danh."),
                     talk("Nói chuyện với Trưởng làng", NPC_BABURU, MAP_VILLAGE,
                           "Quay lại gặp trưởng làng Baburu để báo cáo và hoàn thành nhiệm vụ.", "Đóng",
                           "Con làm tốt lắm... ta không nhìn nhầm người!\n"
                                 + "Hãy tiếp tục mạnh lên, người dân Eldarah sau này trông cậy vào thế hệ chiến binh các"
                                 + " con."))),
         // NV 12: thưởng khi bấm "Tiếp tục" ở trang 1 của Bác Rano (bước 12.6).
         new MainQuest(12, "Đại Ngàn Sayari",
               "Rừng Sayari nằm ở phía đông lục địa Eldarah, là nơi sinh sống của nhiều loại thực vật dị thường. Hãy rèn"
                     + " luyện bản thân trước khi tiến vào khu rừng này.",
               List.of(locked(EXP, 300_000), locked(GOLD, 300_000), locked(ELEMENT_STONE_BAG, 35)), 0,
               List.of(
                     ui(StepKind.REACH_LEVEL, "Luyện tập đạt cấp 10", 10,
                           "Hãy tiếp tục luyện tập và chiến đấu đạt cấp độ 10. Việc nâng cao cấp độ sẽ giúp trở nên"
                                 + " mạnh mẽ hơn và chuẩn bị cho các thử thách sắp tới."),
                     reach("Đến Đại ngàn Sayari", MAP_SAYARI,
                           "Rừng Sayari nằm ở phía tây bản đồ Eldarah, nơi ánh sáng mặt trời len lỏi qua những tán cổ"
                                 + " thụ khổng lồ, nuôi dưỡng sự sống từ thuở sơ khai."),
                     kill("Đánh hạ 200 Báo rừng Sayari", MOB_SAYARI_LEOPARD, 200, MAP_SAYARI,
                           "Báo rừng Sayari thường xuất hiện ngoài rìa khu vực Đại ngàn Sayari, chúng luôn rình rập và"
                                 + " tấn công bất kỳ kẻ nào xâm nhập lãnh thổ của chúng."),
                     kill("Đánh hạ 200 Hổ răng kiếm", MOB_SABERTOOTH, 200, MAP_SAYARI,
                           "Hổ răng kiếm thường xuất hiện sâu trong khu vực Đại ngàn Sayari tới Rừng cổ mộc. Chúng là"
                                 + " những kẻ săn mồi cổ xưa với cặp nanh dài sắc bén."),
                     kill("Đánh hạ 200 Cây ăn thịt", MOB_MAN_EATER, 200, MAP_SAYARI,
                           "Loài cây biến dị nguy hiểm xuất hiện rải rác trong khu rừng Sayari. Dưới tác động của nguồn"
                                 + " năng lượng linh mộc, chúng không còn là những cái cây vô tri mà đã phát triển bản"
                                 + " năng săn mồi."),
                     talk("Nói chuyện với Thợ săn", NPC_RANO, MAP_ANCIENT_WOOD,
                           "Thợ săn Rano là người bản địa của bộ tộc Sayari, người am hiểu từng lối mòn và thói quen"
                                 + " của từng loại thú nguy hiểm ở nơi đây.",
                           "Đóng",
                           "Chào thợ săn trẻ... Ta khá ấn tượng với nhóc đấy!\n"
                                 + "Nơi đây rất nguy hiểm, không phải ai bước vào khu rừng này cũng còn nguyên vẹn đâu.",
                           "Nhưng nếu là một thợ săn có kỹ năng và kinh nghiệm thì Sayari rất phù hợp, nơi đây có"
                                 + " những tài nguyên cực kỳ phong phú.\n"
                                 + "Cậu hãy đánh hạ những con Hổ răng kiếm, thu thập chiến lợi phẩm riêng cho mình nhé.\n"
                                 + "Chúc cậu may mắn."))),
         // NV 13: thưởng khi bấm "Tiếp tục" ở trang 1 của Bác Rano (bước 13.2).
         new MainQuest(13, "Chiến Lợi Phẩm Đầu Tiên",
               "Hổ răng kiếm là loài săn mồi quen thuộc trong khu rừng Sayari, xuất hiện rải rác khắp nơi. Đánh bại chúng"
                     + " và tìm lấy 1 chiếc răng nanh để làm chiến lợi phẩm cho cuộc đi săn này.",
               List.of(locked(EXP, 350_000), locked(GOLD, 350_000), locked(ELEMENT_STONE_BAG, 35),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     // Nanh rơi RIÊNG khi hạ Hổ răng kiếm Tinh anh / Thủ lĩnh (QuestEvents.questDrop)
                     collectElite("Tìm 1 Nanh Hổ răng kiếm", SABERTOOTH_FANG, 1, MAP_SAYARI, MOB_SABERTOOTH,
                           "Hổ răng kiếm là loài săn mồi hung dữ nổi bật với cặp nanh dài và sắc bén. Hãy đánh bại"
                                 + " những cá thể Tinh anh hoặc Thủ lĩnh của chúng để thu lấy chiến lợi phẩm."),
                     talk("Nói chuyện với Thợ săn", NPC_RANO, MAP_ANCIENT_WOOD,
                           "Quay trở lại gặp bác thợ săn Rano để báo cáo và hoàn thành nhiệm vụ.", "Đóng",
                           "Hmm! Để ta xem nào... Một chiếc Răng nanh còn nguyên vẹn à.\n"
                                 + "Khá lắm thanh niên trẻ! Coi như cậu có chút bản lĩnh.\n"
                                 + "Nhưng có vẻ như cậu chưa đi hết khu rừng này đúng không nhỉ?",
                           "Một người thợ săn luôn phải am hiểu địa hình khu vực... Không chỉ để săn mồi, mà còn để"
                                 + " biết khi nào nên rút lui.\n"
                                 + "Hãy tiếp tục tiến vào sâu bên trong khu rừng để đánh hạ Ong bắp cày và những con Khỉ"
                                 + " đột Sayari hung dữ nhé.\n"
                                 + "Chỉ khi hiểu được khu rừng... cậu mới có thể đi sâu hơn vào Linh mộc."))),
         // NV 14: thưởng khi bấm "Tiếp tục" ở trang 1 của Bác Rano (bước 14.4).
         new MainQuest(14, "Theo Vết Con Mồi",
               "Hãy lần theo dấu vết, dùng mọi kỹ năng của thợ săn để tiếp tục cuộc săn trong khu rừng Sayari. Cuộc săn"
                     + " không chỉ là về sức mạnh, mà còn về sự kiên nhẫn, nhanh nhẹn và khả năng đọc trước bước đi của"
                     + " con mồi.",
               List.of(locked(EXP, 500_000), locked(GOLD, 440_000), locked(ORIGIN_STONE, 500)), 0,
               List.of(
                     kill("Đánh hạ 350 Ong bắp cày", MOB_HORNET, 350, MAP_ANCIENT_WOOD,
                           "Ong bắp cày thường xuất hiện tại Rừng cổ mộc và Rừng thần mộc. Với nọc độc nguy hiểm và tốc"
                                 + " độ tấn công nhanh, chúng có thể khiến con mồi suy yếu chỉ trong thời gian ngắn."),
                     kill("Đánh hạ 350 Khỉ đột Sayari", MOB_SAYARI_GORILLA, 350, MAP_DIVINE_WOOD,
                           "Khỉ đột Sayari thường xuất hiện sâu trong Rừng thần mộc, chúng có sức mạnh cơ bắp thuần túy"
                                 + " và canh giữ lãnh thổ theo bầy đàn."),
                     harvest("Thu hoạch 10 Nấm hương", PLANT_SHIITAKE, 10, MAP_SAYARI,
                           "Nấm hương thường xuất hiện rải rác tại rừng Sayari. Có tác dụng hồi phục mạnh mẽ, được người"
                                 + " dân Sayari coi là món thần dược hồi phục."),
                     talk("Nói chuyện với Thợ săn", NPC_RANO, MAP_ANCIENT_WOOD,
                           "Quay trở lại gặp bác thợ săn Rano để báo cáo và hoàn thành nhiệm vụ.", "Đóng",
                           "Khá lắm! Xem ra cậu đã bắt đầu quen với khu rừng Sayari rồi đấy.\n"
                                 + "Ong bắp cày, Khỉ đột Sayari... tất cả đều là những loài không dễ đối phó.\n"
                                 + "Nhưng điều quan trọng không phải là hạ được bao nhiêu con, mà là hiểu được nơi mình"
                                 + " đang săn.",
                           "Phía sâu hơn trong khu rừng là vùng Linh mộc.\n"
                                 + "Đó là nơi nguy hiểm hơn rất nhiều, và không phải thợ săn nào cũng đủ khả năng đặt chân"
                                 + " tới.\n"
                                 + "Nếu muốn tiếp tục cuộc săn, hãy chuẩn bị thật kỹ. Từ đây, những con mồi cậu gặp sẽ"
                                 + " không còn dễ đối phó như trước nữa."))),
         // NV 15: thưởng khi bấm "Tiếp tục" ở trang 1 của Bác Rano = lúc trang 2 hiện ra (bước 15.4, rewardPage 0).
         new MainQuest(15, "Cấm Địa Linh Mộc",
               "Tìm hiểu nguyên nhân diệt vong của bộ tộc Sayari trong quá khứ. Tiến vào vùng cấm địa của rừng Sayari"
                     + " đồng thời tìm cách phong ấn hai thực thể bóng tối.",
               List.of(locked(EXP, 1_000_000), locked(GOLD, 550_000), locked(LINH_MOC_FRUIT, 25)), 0,
               List.of(
                     // Vào phó bản Rừng Linh Mộc (map 25) = xong; chỉ đường tới Bác Rano (phó bản không có cửa dịch chuyển)
                     reachVia("Cùng tổ đội tới Rừng Linh Mộc", MAP_LINH_MOC_1, NPC_RANO, MAP_ANCIENT_WOOD,
                           "Rừng Linh Mộc là cấm địa của Sayari, nơi linh khí hỗn loạn. Hãy tập hợp tổ đội tới gặp bác"
                                 + " Thợ săn Rano và cùng nhau tiến vào Rừng Linh Mộc."),
                     // Boss quái Hắc thần Linh mộc (map 29) — đếm ở ZoneCombat.recordBossDamage
                     hitBoss("Chạm trán Hắc thần Linh mộc", MAP_LINH_MOC_BOSS, 100,
                           "Hắc thần Linh Mộc là thực thể được sinh ra từ Cội rễ Linh mộc, mang trong mình sức mạnh tăm"
                                 + " tối của khu rừng, là kẻ hủy diệt bộ tộc Sayari. Hãy cẩn thận!"),
                     // Boss Tinh linh Thần mộc (map 30) — đếm ở ZoneCombat.hitBoss
                     hitBoss("Chạm trán Tinh Linh Thần Mộc", MAP_LINH_MOC_LAST, 100,
                           "Tinh linh Thần Mộc được sinh ra từ nguồn năng lượng Linh Mộc. Dưới sự ảnh hưởng của nó,"
                                 + " những kẻ sống sót trở thành tín đồ, canh giữ khu rừng như những cái bóng vô hồn."),
                     talk("Nói chuyện với Thợ săn", NPC_RANO, MAP_ANCIENT_WOOD,
                           "Quay trở lại gặp bác thợ săn Rano để báo cáo và hoàn thành nhiệm vụ.", "Đóng",
                           "Làm tốt lắm thợ săn trẻ! Cậu đã tạm thời phong ấn được hai thực thể đó.\n"
                                 + "Nhưng cậu không thể tiêu diệt hoàn toàn sức mạnh ấy, chúng chỉ đang ngủ... dưới lớp rễ"
                                 + " mục và bóng tối của khu rừng.\n"
                                 + "Sẽ đến lúc chúng lại tìm cách trỗi dậy... Nhưng hiện tại có vẻ như linh khí của khu"
                                 + " rừng đã bắt đầu ổn định lại rồi.",
                           "Tuy nhiên, nguồn sức mạnh hắc ám vẫn đang âm thầm lan rộng khắp khu rừng.\n"
                                 + "Vệ thần Rừng Xanh... Ông ấy là Hộ pháp của Sayari, là thực thể bảo vệ khu rừng khỏi"
                                 + " những kẻ xâm nhập.\n"
                                 + "Nhưng kể từ Đêm Cội Rễ Nứt Vỡ, bị ảnh hưởng bởi sức mạnh Linh mộc Hắc ám, Vệ thần trở"
                                 + " nên mất kiểm soát.\n"
                                 + "Ông ấy không còn phân biệt rõ giữa kẻ xâm nhập và những người cố gắng cứu lấy khu rừng.\n"
                                 + "Bất kỳ ai bước vào đều có thể trở thành mục tiêu."))),
         // NV 16: thưởng khi bấm "Tiếp tục" ở trang 1 của Bác Rano (bước 16.3).
         new MainQuest(16, "Tiếng Gầm Nơi Đại Ngàn",
               "Sức mạnh Linh mộc Hắc ám đã âm thầm lan rộng khắp khu rừng khiến linh khí bị xáo trộn. Vệ thần Rừng Xanh"
                     + " trở nên mất kiểm soát, bất kỳ ai bước vào đều có thể trở thành mục tiêu.",
               List.of(locked(EXP, 2_000_000), locked(GOLD, 680_000), locked(ORIGIN_STONE, 500),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     // Boss thế giới xuất hiện ngẫu nhiên (map 6/7/8) — đếm ở ZoneCombat.recordBossDamage
                     hitWorldBoss("Chạm trán Vệ thần Rừng Xanh", WORLD_BOSS_GUARD, 1000,
                           "Vệ thần Rừng Xanh là thực thể bảo hộ Sayari, sinh mệnh của ông ấy tồn tại cùng với hơi thở"
                                 + " sự sống của khu rừng. Hãy chú ý thông báo từ hệ thống."),
                     // Rơi RIÊNG khi gây sát thương lên Hắc thần / Tinh linh Thần mộc / 2 boss thế giới mà boss chết
                     collectBoss("Tìm 1 Bùa linh mộc", LINH_MOC_CHARM, 1,
                           "Bùa linh mộc là tấm bùa hộ thân của người Sayari, mang theo nguồn năng lượng tái sinh từ khu"
                                 + " rừng. Đánh bại những thực thể tối cao của khu rừng để nhận được vật phẩm này."),
                     talk("Báo cáo với Thợ săn Bác Rano", NPC_RANO, MAP_ANCIENT_WOOD,
                           "Sau khi hạ gục Vệ thần Rừng Xanh và thu thập được Bùa Linh Mộc, hãy quay trở lại gặp bác"
                                 + " Rano để báo cáo tình hình. Có lẽ ông ấy biết cách sử dụng tấm bùa này để tìm ra"
                                 + " nguồn gốc của sức mạnh hắc ám.",
                           "Đóng",
                           "Cậu đã trở về rồi! Ta có thể cảm nhận được linh khí của khu rừng trên người cậu.\n"
                                 + "Vệ thần Rừng Xanh... cuối cùng cậu cũng đã đối mặt với ông ấy.\n"
                                 + "Và cậu đã tìm được Bùa Linh Mộc. Tốt lắm! Thứ này có thể giúp chúng ta tiếp tục điều"
                                 + " tra.",
                           "Nhưng có một điều khiến ta lo lắng...\n"
                                 + "Bùa Linh Mộc vốn chỉ xuất hiện khi sức mạnh sâu bên trong khu rừng bị đánh thức. Việc"
                                 + " nó xuất hiện lần này chứng tỏ nguồn hắc ám vẫn chưa biến mất.\n"
                                 + "Nếu muốn cứu khu rừng Sayari, chúng ta phải tìm được nơi bắt đầu của tất cả chuyện"
                                 + " này."))),
         // NV 17: thưởng khi bấm "Tiếp tục" ở trang 1 của Bác Rano (bước 17.3).
         new MainQuest(17, "Kẻ Thách Thức Rừng Xanh",
               "Tinh Linh Rừng Xanh là thực thể tối cao ngự trị và canh giữ nhịp sống của khu rừng Sayari. Hãy nâng cấp"
                     + " trang bị để tối ưu sức chiến đấu trước khi đối mặt với hắn.",
               List.of(locked(EXP, 5_000_000), locked(DIAMOND, 20), locked(ORIGIN_STONE, 500),
                     locked(TALENT_BOOK, 100),
                     // option 3 +HP 500-1000, 9 +Tấn công lên quái 50-100, 30 +% Hút HP 1-5 (random), hạn 3 ngày
                     new RewardItem(FOREST_SPIRIT_COSTUME, 1, true, new int[][] {{3, 500}, {9, 50}, {30, 1}},
                           new int[][] {{3, 1000}, {9, 100}, {30, 5}}, 3)), 0,
               List.of(
                     ui(StepKind.UPGRADE_STAR, "Nâng cấp trang bị 3 sao", 3,
                           "Sức mạnh hiện tại là chưa đủ. Hãy nâng cấp một trang bị bất kì lên 3 sao để tăng cường khả"
                                 + " năng chiến đấu."),
                     hitWorldBoss("Chạm trán Tinh linh Rừng Xanh", WORLD_BOSS_SPIRIT, 2500,
                           "Tinh linh Rừng Xanh đang dần bị Sức mạnh Linh mộc Hắc ám xâm chiếm. Nếu hắn bị hắc hóa,"
                                 + " toàn bộ khu vực sẽ sụp đổ. Hãy chú ý thông báo từ hệ thống."),
                     talk("Báo cáo với Thợ săn Bác Rano", NPC_RANO, MAP_ANCIENT_WOOD,
                           "Sau khi đối mặt với Tinh Linh Rừng Xanh, hãy quay trở lại gặp bác Rano để báo cáo. Có vẻ như"
                                 + " những biến động trong khu rừng chỉ là một phần của vấn đề lớn hơn đang xảy ra tại"
                                 + " Eldarah.",
                           "Đóng",
                           "Cậu đã trở về rồi! Trận chiến với Tinh Linh Rừng Xanh chắc hẳn không dễ dàng.\n"
                                 + "Nhưng cậu đã làm rất tốt. Ít nhất, giờ chúng ta đã biết sức mạnh hắc ám không chỉ ảnh"
                                 + " hưởng đến những sinh vật trong Rừng Linh Mộc.\n"
                                 + "Ta vừa nhận được tin báo về một nơi khác... Đầm lầy Avenna.",
                           "Đầm lầy Avenna nằm giữa vùng Eldarah, nơi ma thuật cổ xưa đã tích tụ qua nhiều năm.\n"
                                 + "Gần đây, những sinh vật sống tại đó đang biến đổi một cách bất thường. Ngay cả những"
                                 + " loài vốn không quá nguy hiểm cũng trở nên hung dữ.\n"
                                 + "Ta muốn cậu đến đó điều tra.\n"
                                 + "Hãy đến Đầm lầy Avenna và tìm hiểu xem nguồn ma thuật bất thường kia đang gây ra"
                                 + " chuyện gì."))),
         // NV 18: thưởng khi bấm "Tiếp tục" ở trang 1 của Olivar (bước 18.5).
         new MainQuest(18, "Đầm lầy Avenna",
               "Đầm lầy Avenna nằm ở giữa bản đồ Eldarah, nơi đây quái vật đã bị biến dị, chúng sẽ chủ động tấn công bạn"
                     + " vì thế hãy cẩn thận với sức mạnh của chúng.",
               List.of(locked(EXP, 7_500_000), locked(GOLD, 780_000), locked(ORIGIN_STONE, 750),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     reach("Đến Đầm lầy Avenna", MAP_AVENNA,
                           "Đầm lầy Avenna nằm ở giữa bản đồ Eldarah, nơi ma thuật tích tụ dày đặc và sinh vật bị biến"
                                 + " đổi không ngừng."),
                     kill("Đánh bại 500 Cua đầm lầy", MOB_SWAMP_CRAB, 500, MAP_AVENNA,
                           "Cua đầm lầy đã bị biến dị, sức mạnh của nó không thể xem thường. Hãy cẩn thận, nó thường"
                                 + " xuất hiện tại Đầm lầy Avenna."),
                     kill("Đánh bại 500 Cây ăn thịt", MOB_SWAMP_MAN_EATER, 500, MAP_AVENNA,
                           "Loài cây biến dị nguy hiểm xuất hiện rải rác tại Đầm lầy. Dưới tác động của ma thuật tích"
                                 + " tụ, chúng biến đổi và đã phát triển thành bản thể quái dị."),
                     kill("Đánh bại 500 Ếch nấm độc", MOB_POISON_FROG, 500, MAP_MANGROVE,
                           "Ếch nấm độc có sức sát thương khá cao, nó đã biến dị và có nọc độc khiến đối phương bị ảo"
                                 + " giác. Hãy cẩn thận khi phải đối mặt với nó."),
                     talk("Báo cáo với Phù thủy Olivar", NPC_OLIVAR, MAP_CROCODILE_CREEK,
                           "Sau khi tiêu diệt những sinh vật biến dị tại Đầm lầy Avenna, hãy trở về gặp Phù thủy Olivar"
                                 + " để báo cáo những gì đã phát hiện.",
                           "Đóng",
                           "Cậu đã trở về rồi sao?\n"
                                 + "Những sinh vật ở Đầm lầy Avenna quả nhiên đã bị biến dị.\n"
                                 + "Ta có thể cảm nhận được một nguồn ma thuật rất mạnh đang lan ra từ đó.\n"
                                 + "Có vẻ chuyện này nghiêm trọng hơn ta nghĩ.",
                           "Nhưng điều đáng lo nhất là nguồn ma thuật ấy vẫn chưa biến mất.\n"
                                 + "Nếu cứ tiếp tục lan rộng, nhiều khu vực khác cũng sẽ bị ảnh hưởng.\n"
                                 + "Ta cần tìm hiểu xem thứ gì đang gây ra sự biến đổi này.\n"
                                 + "Hãy chuẩn bị, cuộc điều tra thực sự mới chỉ bắt đầu."))),
         // NV 19: thưởng khi bấm "Tiếp tục" ở trang 2 của Olivar (bước 19.3, rewardPage 1).
         new MainQuest(19, "Tinh Chất Ma Thuật",
               "Những bông hoa mọc ở Đầm lầy Avenna đã hấp thụ nguồn ma thuật tràn ra từ lòng đất. Hãy hái chúng mang về"
                     + " cho Phù thủy Olivar, biết đâu có thể chế thành thuốc giúp bạn chống chọi với sức mạnh biến dị nơi"
                     + " đây.",
               List.of(locked(EXP, 10_000_000), locked(GOLD, 1_000_000), locked(ORIGIN_STONE, 1000),
                     locked(TALENT_BOOK, 100)), 1,
               List.of(
                     // Hoa bất kỳ trong 6 loại (cây 1-6, map 9 / 10 / 11) — PlantService đếm số hoa nhận được
                     harvestFlower("Thu thập 10 Hoa ma thuật", 10, MAP_AVENNA, PLANT_FLOWER,
                           "Hoa ma thuật mọc rải rác trong Đầm lầy, thường nở ở những nơi ma thuật tích tụ dày đặc. Đóa"
                                 + " hoa nào tỏa ánh sáng lấp lánh là đóa hoa đã hấp thụ đủ tinh chất."),
                     // Olivar > "Chế tạo Ma thuật" (forge/BrewService) — chỉ lần pha THÀNH CÔNG mới tính
                     atNpc(StepKind.BREW, "Pha chế 1 loại thuốc Ma thuật", 0, NPC_OLIVAR, MAP_CROCODILE_CREEK,
                           "Mang Hoa ma thuật và Bình nước phép đến gặp Olivar. Chỉ có đôi tay khéo léo của bà mới chiết"
                                 + " được tinh chất từ những đóa hoa này."),
                     talk("Báo cáo với Phù thủy Olivar", NPC_OLIVAR, MAP_CROCODILE_CREEK,
                           "Thuốc Ma thuật đã hoàn thành. Hãy mang nó đến cho Olivar để bà kiểm tra thành quả.",
                           "Đóng",
                           "Tốt lắm, thuốc đã hoàn thành rồi.\n"
                                 + "Cậu nhìn xem, ánh sáng trong bình này chính là ma thuật được chắt lọc từ những đóa"
                                 + " hoa.\n"
                                 + "Chúng hấp thụ ma thuật từ lòng đầm lầy, nên ta mới có thể dùng chúng để đo sức mạnh"
                                 + " của nguồn năng lượng kia.",
                           "Nhưng cậu hãy nhìn kỹ đi, ánh sáng trong bình đang dao động bất thường.\n"
                                 + "Nguồn ma thuật dưới đầm lầy không còn thuần khiết nữa, có thứ gì đó đang làm nó vẩn"
                                 + " đục.\n"
                                 + "Ta e rằng những sinh vật cậu gặp mới chỉ là khởi đầu.",
                           "Cậu hãy giữ lấy phần thưởng này, cậu xứng đáng nhận được nó.\n"
                                 + "Nghe này, gần đây dân làng kể có tiếng gầm lạ vang lên từ sâu trong đầm lầy.\n"
                                 + "Hãy chuẩn bị tinh thần, ta nghĩ kẻ canh giữ nơi đó đã bị ảnh hưởng."))),
         // NV 20: thưởng khi bấm "Tiếp tục" ở trang 2 của Olivar (bước 20.2).
         new MainQuest(20, "Tiếng Gầm Nơi Đầm Lầy",
               "Tiếng gầm lạ vang lên từ sâu trong Đầm lầy Avenna. Vệ thần canh giữ nơi đây đã bị ma thuật biến dị và trở"
                     + " nên cuồng bạo. Hãy tìm ra nó và ngăn nó lại trước khi quá muộn.",
               List.of(locked(EXP, 11_000_000), locked(GOLD, 1_100_000), locked(ORIGIN_STONE, 1100),
                     locked(TALENT_BOOK, 100)), 1,
               List.of(
                     // Boss thế giới 9 (map 9 / 10 / 11) — đếm ở ZoneCombat.recordBossDamage
                     hitWorldBoss("Chạm trán Vệ thần Đầm lầy", WORLD_BOSS_SWAMP_GUARD, 2500,
                           "Vệ thần Đầm lầy xuất hiện bất ngờ và vô cùng hung dữ. Hãy theo dõi kênh thế giới để biết khi"
                                 + " nào nó xuất hiện, và đừng đối đầu một mình nếu chưa đủ mạnh."),
                     talk("Báo cáo với Phù thủy Olivar", NPC_OLIVAR, MAP_CROCODILE_CREEK,
                           "Vệ thần đã bị khuất phục. Hãy quay về báo cho Olivar những gì cậu đã thấy.",
                           "Đóng",
                           "Ta nghe thấy tiếng gầm đó đã tắt rồi.\n"
                                 + "Cậu đã khuất phục được Vệ thần Đầm lầy sao? Giỏi lắm!\n"
                                 + "Nó vốn là kẻ canh giữ hiền lành của vùng đất này, chỉ vì ma thuật biến dị mà hóa"
                                 + " cuồng bạo.",
                           "Nhưng ta đã kiểm tra cơ thể nó, trong đó có một thứ năng lượng rất lạ.\n"
                                 + "Một luồng sức mạnh Hắc ám, nó không thuộc về đầm lầy này.\n"
                                 + "Vệ thần chỉ là kẻ bị thao túng, còn nguồn gốc thật sự vẫn nằm sâu hơn nữa.",
                           "Kẻ đứng đầu đầm lầy này là Tinh Linh Đầm lầy.\n"
                                 + "Nếu Hắc ám xâm chiếm hoàn toàn nó thì cả khu vực này sẽ sụp đổ.\n"
                                 + "Trước khi đối mặt với nó, cậu hãy nâng cấp trang bị cho thật mạnh nhé."))),
         // NV 21: thưởng khi bấm "Tiếp tục" ở trang 2 của Olivar (bước 21.4).
         new MainQuest(21, "Kẻ Thách Thức Đầm Lầy",
               "Tinh Linh Đầm lầy là thực thể tối cao ngự trị và canh giữ nhịp sống của khu đầm lầy, nay đang dần bị Hắc"
                     + " ám xâm chiếm. Hãy nâng cấp trang bị để tối ưu sức chiến đấu trước khi đối mặt với hắn.",
               List.of(locked(EXP, 11_000_000), locked(GOLD, 1_100_000), locked(ORIGIN_STONE, 1100),
                     locked(TALENT_BOOK, 100)), 1,
               List.of(
                     ui(StepKind.UPGRADE_STAR, "Nâng cấp trang bị 6 sao", 6,
                           "Sức mạnh hiện tại vẫn chưa đủ. Hãy đến Thợ rèn nâng cấp 1 trang bị bất kỳ lên 6 sao để tăng"
                                 + " cường khả năng chiến đấu."),
                     hitWorldBoss("Chạm trán Tinh Linh Đầm lầy", WORLD_BOSS_SWAMP_SPIRIT, 2500,
                           "Tinh Linh Đầm lầy đang dần bị sức mạnh Hắc ám xâm chiếm. Nếu hắn bị hắc hóa, toàn bộ khu vực"
                                 + " sẽ sụp đổ. Hãy chú ý thông báo từ hệ thống để biết thời điểm hắn xuất hiện."),
                     // Rơi RIÊNG khi gây sát thương lên boss mà boss chết (như Bùa linh mộc 16.2)
                     collectBoss("Tìm 1 Nhẫn u mộc", U_MOC_RING, 1,
                           "Nhẫn u mộc được kết tinh từ ma thuật cổ xưa của đầm lầy, chỉ xuất hiện trên người những thực"
                                 + " thể tối cao. Đánh bại chúng để nhận được vật phẩm này."),
                     talk("Báo cáo với Phù thủy Olivar", NPC_OLIVAR, MAP_CROCODILE_CREEK,
                           "Tinh Linh Đầm lầy đã bị đánh bại. Hãy trở về báo tin cho Olivar.",
                           "Đóng",
                           "Cậu đã làm được rồi sao?\n"
                                 + "Ta cảm nhận được nhịp sống của đầm lầy đang dịu lại, ma thuật cũng đã bớt cuồng"
                                 + " loạn.\n"
                                 + "Cảm ơn cậu, nếu không có cậu thì nơi này đã bị Hắc ám nuốt chửng rồi.",
                           "Nhưng đừng vội mừng, ta đã tìm thấy thứ này trên người Tinh Linh.\n"
                                 + "Một dấu ấn Hắc ám, nó không phải do tự nhiên mà có.\n"
                                 + "Chắc chắn có kẻ đứng sau tất cả chuyện này.",
                           "Ta sẽ nghiên cứu dấu ấn này, còn cậu hãy nghỉ ngơi và củng cố sức mạnh.\n"
                                 + "Cuộc hành trình của cậu vẫn chưa kết thúc đâu.\n"
                                 + "Khi nào ta có manh mối, ta sẽ cho người báo tin."))),
         // ===== NV 22-33: tự thiết kế theo khuôn Rừng Linh Mộc / Đầm lầy (vào vùng + đánh quái -> thu thập -> Vệ thần
         // -> nâng sao + Tinh Linh). Vùng Velmora (map 18-20, quái cấp 45-60, boss thế giới 11 / 12) mở từ NV 22.
         // NV 22: thưởng khi bấm "Tiếp tục" ở trang 1 của Ribaron (bước 22.5).
         new MainQuest(22, "Bờ Biển Velmora",
               "Phía tây Rạch cá sấu là vùng duyên hải Velmora, nơi những ngư dân sống nhờ biển cả. Gần đây biển động"
                     + " bất thường, sinh vật ven bờ trở nên hung hãn. Hãy đến đó tìm hiểu tình hình.",
               List.of(locked(EXP, 12_000_000), locked(GOLD, 1_200_000), locked(ORIGIN_STONE, 1200),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     ui(StepKind.REACH_LEVEL, "Luyện tập đạt cấp 35", 35,
                           "Vùng biển Velmora nguy hiểm hơn Đầm lầy rất nhiều. Hãy luyện tập đạt cấp độ 35 trước khi"
                                 + " lên đường."),
                     reach("Đến Bờ biển Velmora", MAP_VELMORA_COAST,
                           "Bờ biển Velmora nằm ở phía tây Rạch cá sấu, nơi sóng biển vỗ vào những bãi cát trắng trải dài"
                                 + " bất tận."),
                     kill("Đánh bại 500 Ốc mượn hồn", MOB_HERMIT_CRAB, 500, MAP_VELMORA_COAST,
                           "Ốc mượn hồn vốn hiền lành, nhưng nguồn năng lượng lạ từ biển khiến chiếc vỏ của chúng cứng"
                                 + " như đá và trở nên hung hăng với bất kỳ ai tới gần."),
                     kill("Đánh bại 500 Cá thòi lòi", MOB_MUDSKIPPER, 500, MAP_VELMORA_COAST,
                           "Cá thòi lòi bò lên bờ thành từng đàn lớn, chúng nhảy rất nhanh và cắn rất đau. Hãy cẩn thận"
                                 + " khi đi dọc Bờ biển Velmora."),
                     talk("Báo cáo với Trưởng làng Ribaron", NPC_RIBARON, MAP_VELMORA_VILLAGE,
                           "Trưởng làng Ribaron là người đứng đầu Làng chài Velmora. Hãy tới gặp ông để hỏi về những biến"
                                 + " động của biển cả.",
                           "Đóng",
                           "Chàng trai trẻ, cậu từ Eldarah tới sao? Cảm ơn cậu đã dẹp yên bờ biển giúp chúng ta.\n"
                                 + "Mấy tháng nay biển cả cứ cuồn cuộn không ngừng, thuyền đánh cá không dám ra khơi.\n"
                                 + "Ngay cả những con ốc nhỏ bé cũng trở nên hung dữ lạ thường.",
                           "Bà Olivar có gửi thư cho ta, kể về dấu ấn Hắc ám ở Đầm lầy Avenna.\n"
                                 + "Ta e rằng thứ sức mạnh đó đã theo dòng nước chảy ra tận biển.\n"
                                 + "Hãy tới làng chài tìm hiểu thêm, ông Tataru ở Bãi đá ngầm là người hiểu biển cả nhất"
                                 + " nơi đây."))),
         // NV 23: thưởng khi bấm "Tiếp tục" ở trang 1 của Tataru (bước 23.4).
         new MainQuest(23, "Làng Chài Velmora",
               "Làng chài Velmora đang bị bao vây bởi những sinh vật biển biến dị. Hãy giúp dân làng đẩy lùi chúng và tìm"
                     + " gặp ông Tataru, người ngư dân già hiểu rõ biển cả nhất vùng.",
               List.of(locked(EXP, 13_000_000), locked(GOLD, 1_300_000), locked(ORIGIN_STONE, 1300),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     kill("Đánh bại 500 Cá răng đao", MOB_SAWFISH, 500, MAP_VELMORA_VILLAGE,
                           "Cá răng đao có chiếc mõm dài sắc như lưỡi đao, chúng thường phục kích ở vùng nước nông quanh"
                                 + " Làng chài Velmora."),
                     kill("Đánh bại 500 Hải âu", MOB_SEAGULL, 500, MAP_VELMORA_VILLAGE,
                           "Đàn hải âu bị biến dị không còn bắt cá mà lao xuống tấn công cả ngư dân. Hãy xua đuổi chúng"
                                 + " khỏi bầu trời Làng chài Velmora."),
                     killElite("Đánh bại Tinh anh Hải âu", MOB_SEAGULL, 1, MAP_VELMORA_VILLAGE, ELITE_HINT),
                     talk("Nói chuyện với Ngư dân Tataru", NPC_TATARU, MAP_REEF,
                           "Ông Tataru là ngư dân già nhất Velmora, ông sống một mình ở Bãi đá ngầm và đọc được tâm trạng"
                                 + " của biển cả.",
                           "Đóng",
                           "Hừm... Lâu lắm rồi mới có người lặn lội tới tận Bãi đá ngầm này.\n"
                                 + "Ta đã đánh cá ở đây hơn sáu mươi năm, chưa bao giờ thấy biển giận dữ như bây giờ.\n"
                                 + "Cá hổ Velmora từ ngoài khơi kéo vào bờ thành từng đàn.",
                           "Đêm nào ta cũng nghe thấy tiếng gầm vọng lên từ dưới đáy biển.\n"
                                 + "Đó là Vệ thần Biển cả, kẻ canh giữ đại dương từ thuở xa xưa.\n"
                                 + "Nếu ngay cả ông ấy cũng mất kiểm soát, cả vùng duyên hải này sẽ không còn yên bình."))),
         // NV 24: thưởng khi bấm "Tiếp tục" ở trang 1 của Ribaron (bước 24.4).
         new MainQuest(24, "Tiếng Gầm Nơi Biển Cả",
               "Vệ thần Biển cả, kẻ canh giữ đại dương của Velmora, đã bị sức mạnh Hắc ám thao túng. Hãy đối mặt với ông"
                     + " ấy và tìm ra dấu vết của kẻ đứng sau.",
               List.of(locked(EXP, 14_000_000), locked(GOLD, 1_400_000), locked(ORIGIN_STONE, 1400),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     kill("Đánh bại 500 Cá hổ Velmora", MOB_VELMORA_TIGERFISH, 500, MAP_REEF,
                           "Cá hổ Velmora là kẻ săn mồi đáng sợ nhất vùng biển, hàm răng của chúng có thể nghiền nát cả"
                                 + " vỏ thuyền. Chúng tụ tập dày đặc quanh Bãi đá ngầm."),
                     hitWorldBoss("Chạm trán Vệ thần Biển cả", WORLD_BOSS_SEA_GUARD, 2500,
                           "Vệ thần Biển cả trỗi dậy từ đáy đại dương với sức mạnh khủng khiếp. Hãy chú ý thông báo từ hệ"
                                 + " thống và cùng đồng đội đối mặt với ông ấy."),
                     // Rơi RIÊNG khi gây sát thương lên boss mà boss chết (như Bùa linh mộc 16.2)
                     collectBoss("Tìm 1 Khiên vỏ sò", SEA_SHELL_SHIELD, 1,
                           "Khiên vỏ sò được kết từ lớp vỏ cứng rơi ra trên người những thực thể tối cao. Đánh bại chúng"
                                 + " để nhận được vật phẩm này."),
                     talk("Báo cáo với Trưởng làng Ribaron", NPC_RIBARON, MAP_VELMORA_VILLAGE,
                           "Vệ thần Biển cả đã bình tĩnh trở lại. Hãy mang Khiên vỏ sò về cho trưởng làng Ribaron xem.",
                           "Đóng",
                           "Biển đã lặng sóng rồi... Thuyền của dân làng lại có thể ra khơi.\n"
                                 + "Cậu đã làm được điều mà cả làng chài này không dám nghĩ tới.\n"
                                 + "Để ta xem chiếc khiên vỏ sò cậu mang về nào.",
                           "Trên mặt khiên có một vết ấn màu đen, giống hệt dấu ấn mà bà Olivar đã mô tả.\n"
                                 + "Vệ thần chỉ là con rối, kẻ thực sự bị Hắc ám nhắm tới là Tinh Linh Biển cả.\n"
                                 + "Nếu Tinh Linh bị hắc hóa, cả đại dương sẽ nổi giận."))),
         // NV 25: thưởng khi bấm "Tiếp tục" ở trang 2 của Ribaron (bước 25.3).
         new MainQuest(25, "Kẻ Thách Thức Biển Cả",
               "Tinh Linh Biển cả là thực thể tối cao ngự trị đại dương Velmora, nay đang dần bị Hắc ám xâm chiếm. Hãy"
                     + " nâng cấp trang bị để tối ưu sức chiến đấu trước khi đối mặt với hắn.",
               List.of(locked(EXP, 15_000_000), locked(GOLD, 1_500_000), locked(ORIGIN_STONE, 1500),
                     locked(TALENT_BOOK, 100), locked(DIAMOND, 20),
                     // option như Mặt nạ Velmora rơi từ Tinh Linh Biển cả (tlnt_data.world_boss 12), hạn 3 ngày
                     new RewardItem(VELMORA_MASK, 1, true, new int[][] {{44, 3}, {19, 350}, {5, 500}},
                           new int[][] {{44, 5}, {19, 500}, {5, 1000}}, 3)), 1,
               List.of(
                     ui(StepKind.UPGRADE_STAR, "Nâng cấp trang bị 5 sao", 5,
                           "Sóng gió đại dương không dành cho kẻ yếu. Hãy đến Thợ rèn nâng cấp 1 trang bị bất kỳ lên 5"
                                 + " sao để tăng cường khả năng chiến đấu."),
                     hitWorldBoss("Chạm trán Tinh Linh Biển cả", WORLD_BOSS_SEA_SPIRIT, 2500,
                           "Tinh Linh Biển cả đang dần bị sức mạnh Hắc ám xâm chiếm. Nếu hắn bị hắc hóa, cả vùng duyên"
                                 + " hải sẽ chìm trong biển nước. Hãy chú ý thông báo từ hệ thống."),
                     talk("Báo cáo với Trưởng làng Ribaron", NPC_RIBARON, MAP_VELMORA_VILLAGE,
                           "Tinh Linh Biển cả đã bị đánh bại. Hãy trở về báo tin cho trưởng làng Ribaron.",
                           "Đóng",
                           "Cậu trở về rồi! Cả làng đã đứng trên bờ cầu nguyện cho cậu suốt đêm qua.\n"
                                 + "Biển cả đã thực sự yên bình, ta có thể cảm nhận được điều đó qua từng con sóng.",
                           "Nhưng cậu nhìn xem, dấu ấn Hắc ám trên người Tinh Linh lần này còn rõ hơn trước.\n"
                                 + "Sức mạnh đó đang lớn dần lên qua mỗi vùng đất nó đi qua.\n"
                                 + "Ta đã gửi tin cho bà Olivar, bà nói dấu ấn này có liên quan tới đá phong thạch.",
                           "Đá phong thạch chỉ có ở vùng núi Darguun, phía bắc Rừng ngập mặn.\n"
                                 + "Tù trưởng Kargu của bộ tộc Darguun là bạn cũ của ta, cậu hãy tới tìm ông ấy.\n"
                                 + "Đi đường cẩn thận, chàng trai trẻ."))),
         // ===== Vùng Darguun (map 12-14, quái cấp 65-80, boss thế giới 13 / 14) mở từ NV 26, đi qua Rừng ngập mặn.
         // NV 26: thưởng khi bấm "Tiếp tục" ở trang 1 của Kargu (bước 26.5).
         new MainQuest(26, "Bình Nguyên Darguun",
               "Bình nguyên Darguun nằm ở phía bắc Rừng ngập mặn, vùng đất khô cằn với những cơn gió mang theo bụi đá."
                     + " Hãy vượt qua bình nguyên để tìm gặp Tù trưởng Kargu.",
               List.of(locked(EXP, 17_000_000), locked(GOLD, 1_600_000), locked(ORIGIN_STONE, 1600),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     ui(StepKind.REACH_LEVEL, "Luyện tập đạt cấp 40", 40,
                           "Thú dữ vùng Darguun mạnh hơn bất kỳ nơi nào cậu từng đặt chân tới. Hãy luyện tập đạt cấp độ"
                                 + " 40 trước khi lên đường."),
                     reach("Đến Bình nguyên Darguun", MAP_DARGUUN,
                           "Bình nguyên Darguun nằm ở phía bắc Rừng ngập mặn, nơi đất đá khô cằn trải dài tới tận chân"
                                 + " núi."),
                     kill("Đánh bại 500 Linh dương", MOB_ANTELOPE, 500, MAP_DARGUUN,
                           "Linh dương Darguun vốn nhút nhát, nhưng giờ chúng húc ngã bất kỳ ai đi ngang qua bình nguyên"
                                 + " bằng cặp sừng sắc nhọn."),
                     kill("Đánh bại 500 Linh cẩu Darguun", MOB_DARGUUN_HYENA, 500, MAP_DARGUUN,
                           "Linh cẩu Darguun săn mồi theo bầy, tiếng cười rợn người của chúng vang khắp bình nguyên mỗi"
                                 + " khi chiều xuống."),
                     talk("Gặp Tù trưởng Kargu", NPC_KARGU, MAP_STONE_PLATEAU,
                           "Tù trưởng Kargu là người đứng đầu bộ tộc Darguun, sống trên Cao nguyên đá. Hãy tới gặp ông và"
                                 + " chuyển lời của trưởng làng Ribaron.",
                           "Đóng",
                           "Người của Ribaron à? Lão già đánh cá đó vẫn còn nhớ tới ta sao, ha ha!\n"
                                 + "Cậu băng qua được bình nguyên lúc này là không tệ đâu.\n"
                                 + "Thú dữ ở đây đã trở nên điên loạn từ khi những cơn bão đá bắt đầu.",
                           "Bộ tộc Darguun sống dựa vào săn bắn trên cao nguyên này từ bao đời.\n"
                                 + "Nhưng giờ những con Sư tử núi và Chim ưng thợ săn tấn công cả người của ta.\n"
                                 + "Nếu cậu muốn tìm hiểu về đá phong thạch, trước hết hãy giúp bộ tộc ta đã."))),
         // NV 27: thưởng khi bấm "Tiếp tục" ở trang 1 của Kargu (bước 27.4).
         new MainQuest(27, "Cao Nguyên Đá",
               "Cao nguyên đá là lãnh địa săn bắn của bộ tộc Darguun. Hãy chứng tỏ bản lĩnh của mình bằng cách đánh bại"
                     + " những kẻ săn mồi hung dữ nhất nơi đây.",
               List.of(locked(EXP, 18_000_000), locked(GOLD, 1_700_000), locked(ORIGIN_STONE, 1700),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     kill("Đánh bại 500 Sư tử núi", MOB_MOUNTAIN_LION, 500, MAP_STONE_PLATEAU,
                           "Sư tử núi là chúa tể của Cao nguyên đá. Chúng ẩn mình sau những tảng đá lớn và vồ lấy con mồi"
                                 + " chỉ trong chớp mắt."),
                     kill("Đánh bại 200 Chim ưng thợ săn", MOB_HUNTER_HAWK, 200, MAP_STONE_PLATEAU,
                           "Chim ưng thợ săn từng là bạn đồng hành của thợ săn Darguun. Giờ đây chúng lao xuống từ trên"
                                 + " cao, tấn công bất kỳ ai không kịp đề phòng."),
                     killElite("Đánh bại Tinh anh Sư tử núi", MOB_MOUNTAIN_LION, 1, MAP_STONE_PLATEAU, ELITE_HINT),
                     talk("Báo cáo với Tù trưởng Kargu", NPC_KARGU, MAP_STONE_PLATEAU,
                           "Quay trở lại gặp Tù trưởng Kargu để báo cáo và hoàn thành nhiệm vụ.", "Đóng",
                           "Ha! Ta đã thấy cậu hạ con Sư tử đầu đàn từ xa. Một đòn đẹp lắm!\n"
                                 + "Từ hôm nay, cậu là bạn của bộ tộc Darguun.\n"
                                 + "Giờ thì ta sẽ kể cho cậu nghe về những cơn bão đá.",
                           "Trên đỉnh Núi phong thạch là nơi Vệ thần Phong thạch ngự trị.\n"
                                 + "Ông ấy điều khiển gió và đá, giữ cho vùng núi này cân bằng.\n"
                                 + "Nhưng giờ những cơn bão đá ngày càng dữ dội, ta sợ rằng ông ấy đã bị thứ gì đó chi"
                                 + " phối."))),
         // NV 28: thưởng khi bấm "Tiếp tục" ở trang 1 của Kargu (bước 28.4).
         new MainQuest(28, "Tiếng Gầm Núi Phong Thạch",
               "Những cơn bão đá từ Núi phong thạch ngày càng dữ dội. Vệ thần Phong thạch đã mất kiểm soát, hãy lên núi"
                     + " ngăn ông ấy lại trước khi bộ tộc Darguun bị vùi lấp.",
               List.of(locked(EXP, 19_000_000), locked(GOLD, 1_800_000), locked(ORIGIN_STONE, 1800),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     kill("Đánh bại 500 Rồng đất Darguun", MOB_EARTH_DRAGON, 500, MAP_PHONG_THACH,
                           "Rồng đất Darguun có lớp da cứng như đá phong thạch, chúng chiếm cứ các sườn núi và không cho"
                                 + " bất kỳ ai tới gần đỉnh núi."),
                     hitWorldBoss("Chạm trán Vệ thần Phong thạch", WORLD_BOSS_STONE_GUARD, 2500,
                           "Vệ thần Phong thạch xuất hiện cùng những cơn bão đá cuồng nộ. Hãy chú ý thông báo từ hệ"
                                 + " thống và cùng đồng đội đối mặt với ông ấy."),
                     collectBoss("Tìm 1 Mắt bão Darguun", DARGUUN_STORM_EYE, 1,
                           "Mắt bão Darguun là viên đá kết tinh từ sức mạnh của gió và đá, chỉ xuất hiện trên người những"
                                 + " thực thể tối cao. Đánh bại chúng để nhận được vật phẩm này."),
                     talk("Báo cáo với Tù trưởng Kargu", NPC_KARGU, MAP_STONE_PLATEAU,
                           "Bão đá đã lặng. Hãy mang Mắt bão Darguun về cho Tù trưởng Kargu.",
                           "Đóng",
                           "Gió đã ngừng thổi rồi... Lần đầu tiên sau nhiều tháng, trời Darguun lại trong xanh.\n"
                                 + "Cậu thật sự đã khuất phục được Vệ thần Phong thạch.\n"
                                 + "Viên Mắt bão này... bên trong nó có một vệt đen đang cuộn xoáy.",
                           "Dấu ấn Hắc ám, đúng như lời bà Olivar nói.\n"
                                 + "Kẻ đứng đầu vùng núi này là Tinh Linh Phong thạch, sức mạnh của hắn vượt xa Vệ thần.\n"
                                 + "Nếu hắn bị hắc hóa, cả dãy núi sẽ sụp xuống đầu chúng ta."))),
         // NV 29: thưởng khi bấm "Tiếp tục" ở trang 2 của Kargu (bước 29.3).
         new MainQuest(29, "Kẻ Thách Thức Phong Thạch",
               "Tinh Linh Phong thạch là thực thể tối cao ngự trị vùng núi Darguun, nay đang dần bị Hắc ám xâm chiếm. Hãy"
                     + " nâng cấp trang bị để tối ưu sức chiến đấu trước khi đối mặt với hắn.",
               List.of(locked(EXP, 20_000_000), locked(GOLD, 1_900_000), locked(ORIGIN_STONE, 1900),
                     locked(TALENT_BOOK, 100), locked(DIAMOND, 20),
                     // option như Mặt nạ Darguun rơi từ Tinh Linh Phong thạch (world_boss 14), hạn 3 ngày
                     new RewardItem(DARGUUN_MASK, 1, true, new int[][] {{14, 3}, {11, 350}, {35, 50}},
                           new int[][] {{14, 5}, {11, 500}, {35, 100}}, 3)), 1,
               List.of(
                     ui(StepKind.UPGRADE_STAR, "Nâng cấp trang bị 5 sao", 5,
                           "Đá phong thạch cứng hơn bất kỳ loại thép nào. Hãy đến Thợ rèn nâng cấp 1 trang bị bất kỳ lên"
                                 + " 5 sao để tăng cường khả năng chiến đấu."),
                     hitWorldBoss("Chạm trán Tinh Linh Phong thạch", WORLD_BOSS_STONE_SPIRIT, 2500,
                           "Tinh Linh Phong thạch đang dần bị sức mạnh Hắc ám xâm chiếm. Nếu hắn bị hắc hóa, cả dãy núi"
                                 + " Darguun sẽ sụp đổ. Hãy chú ý thông báo từ hệ thống."),
                     talk("Báo cáo với Tù trưởng Kargu", NPC_KARGU, MAP_STONE_PLATEAU,
                           "Tinh Linh Phong thạch đã bị đánh bại. Hãy trở về báo tin cho Tù trưởng Kargu.",
                           "Đóng",
                           "Cả bộ tộc đã nghe thấy tiếng đá vỡ trên đỉnh núi. Cậu đã chiến thắng!\n"
                                 + "Bộ tộc Darguun sẽ ghi nhớ tên cậu trong những bài ca săn bắn.",
                           "Nhưng có một chuyện ta phải nói với cậu.\n"
                                 + "Khi Tinh Linh ngã xuống, dấu ấn Hắc ám trên người hắn bay vút về phía bắc.\n"
                                 + "Phía đó chỉ có Cánh đồng tuyết và vùng băng giá Frearh.",
                           "Người của ta ở Frearh là thợ săn Zair, cậu ấy sống bên Sông băng Frearh.\n"
                                 + "Cái lạnh ở đó có thể giết người chỉ trong một đêm, hãy chuẩn bị thật kỹ.\n"
                                 + "Cầu cho gió núi dẫn đường cho cậu, người bạn của Darguun."))),
         // ===== Vùng Frearh (map 15-17, quái cấp 85-100, boss thế giới 15 / 16) mở từ NV 30, đi qua Núi phong thạch.
         // NV 30: thưởng khi bấm "Tiếp tục" ở trang 1 của Zair (bước 30.5).
         new MainQuest(30, "Cánh Đồng Tuyết",
               "Phía bắc Núi phong thạch là vùng băng giá Frearh, nơi tuyết phủ trắng quanh năm. Dấu ấn Hắc ám đã bay về"
                     + " phía này, hãy lần theo nó và tìm gặp thợ săn Zair.",
               List.of(locked(EXP, 22_000_000), locked(GOLD, 2_000_000), locked(ORIGIN_STONE, 2000),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     ui(StepKind.REACH_LEVEL, "Luyện tập đạt cấp 45", 45,
                           "Cái lạnh và thú dữ của Frearh không tha cho kẻ yếu. Hãy luyện tập đạt cấp độ 45 trước khi lên"
                                 + " đường."),
                     reach("Đến Cánh đồng tuyết", MAP_SNOWFIELD,
                           "Cánh đồng tuyết nằm ở phía bắc Núi phong thạch, nơi gió tuyết gào thét không ngừng ngày đêm."),
                     kill("Đánh bại 500 Bò lông dài", MOB_YAK, 500, MAP_SNOWFIELD,
                           "Bò lông dài sống thành đàn trên Cánh đồng tuyết. Chúng giẫm nát mọi thứ trên đường đi mỗi khi"
                                 + " bị kích động."),
                     kill("Đánh bại 500 Sói tuyết Frearh", MOB_SNOW_WOLF, 500, MAP_SNOWFIELD,
                           "Sói tuyết Frearh hòa lẫn vào màu tuyết trắng, chúng bao vây con mồi trong im lặng rồi tấn"
                                 + " công cùng lúc."),
                     talk("Gặp Thợ săn Zair", NPC_ZAIR, MAP_FREARH_RIVER,
                           "Thợ săn Zair sống bên Sông băng Frearh. Hãy tới gặp cậu ấy và chuyển lời của Tù trưởng Kargu.",
                           "Đóng",
                           "Đứng lại! À... cậu là người mà Tù trưởng Kargu nhắn tới phải không?\n"
                                 + "Xin lỗi, ở nơi này ta không thể tin bất kỳ ai bước ra từ cơn bão tuyết.\n"
                                 + "Cậu đã vượt qua Cánh đồng tuyết, xem ra cũng có bản lĩnh đấy.",
                           "Mấy tuần trước, một luồng sáng đen rơi xuống phía Núi linh băng.\n"
                                 + "Kể từ đó, thú dữ trên sông băng trở nên điên cuồng, ngay cả ta cũng suýt mất mạng.\n"
                                 + "Nếu muốn lần theo dấu ấn đó, cậu phải giúp ta dọn đường dọc Sông băng trước đã."))),
         // NV 31: thưởng khi bấm "Tiếp tục" ở trang 1 của Zair (bước 31.4).
         new MainQuest(31, "Dấu Vết Trên Băng",
               "Thú dữ dọc Sông băng Frearh đã bị nhiễm Hắc ám. Hãy cùng thợ săn Zair dọn đường tới Núi linh băng, nơi"
                     + " luồng sáng đen đã rơi xuống.",
               List.of(locked(EXP, 23_000_000), locked(GOLD, 2_100_000), locked(ORIGIN_STONE, 2100),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     kill("Đánh bại 500 Báo tuyết Frearh", MOB_SNOW_LEOPARD, 500, MAP_FREARH_RIVER,
                           "Báo tuyết Frearh nhanh nhẹn và lặng lẽ như bóng ma, chúng săn mồi dọc hai bờ Sông băng"
                                 + " Frearh."),
                     kill("Đánh bại 500 Cú tuyết Frearh", MOB_SNOW_OWL, 500, MAP_ICE_MOUNTAIN,
                           "Cú tuyết Frearh canh giữ lối lên Núi linh băng. Đôi mắt của chúng phát ra ánh sáng lạnh lẽo"
                                 + " trong đêm tối."),
                     killElite("Đánh bại Tinh anh Báo tuyết Frearh", MOB_SNOW_LEOPARD, 1, MAP_FREARH_RIVER,
                           ELITE_HINT),
                     talk("Báo cáo với Thợ săn Zair", NPC_ZAIR, MAP_FREARH_RIVER,
                           "Quay trở lại gặp thợ săn Zair để báo cáo và hoàn thành nhiệm vụ.", "Đóng",
                           "Đường lên Núi linh băng đã thông rồi. Cậu giỏi hơn ta tưởng nhiều đấy.\n"
                                 + "Ta đã lên tới chân núi và thấy dấu chân của một sinh vật khổng lồ.\n"
                                 + "Những con Voi Mammoth đang bảo vệ một thứ gì đó trên đỉnh núi.",
                           "Đó là Vệ thần Băng giá, kẻ canh giữ cái lạnh vĩnh cửu của Frearh.\n"
                                 + "Luồng sáng đen kia đã rơi trúng nơi ông ấy ngự trị.\n"
                                 + "Hãy cẩn thận, băng giá có thể đóng băng cả dòng máu trong người cậu."))),
         // NV 32: thưởng khi bấm "Tiếp tục" ở trang 1 của Zair (bước 32.4).
         new MainQuest(32, "Tiếng Gầm Nơi Băng Giá",
               "Vệ thần Băng giá đã bị luồng sáng đen thao túng, những cơn bão tuyết ngày càng khủng khiếp. Hãy lên Núi"
                     + " linh băng đối mặt với ông ấy.",
               List.of(locked(EXP, 24_000_000), locked(GOLD, 2_200_000), locked(ORIGIN_STONE, 2200),
                     locked(TALENT_BOOK, 100)), 0,
               List.of(
                     kill("Đánh bại 500 Voi Mammoth", MOB_MAMMOTH, 500, MAP_ICE_MOUNTAIN,
                           "Voi Mammoth là sinh vật cổ xưa nhất của Frearh, cặp ngà khổng lồ của chúng có thể quét bay"
                                 + " mọi kẻ thù. Chúng canh giữ đỉnh Núi linh băng."),
                     hitWorldBoss("Chạm trán Vệ thần Băng giá", WORLD_BOSS_ICE_GUARD, 2500,
                           "Vệ thần Băng giá xuất hiện cùng bão tuyết dữ dội. Hãy chú ý thông báo từ hệ thống và cùng đồng"
                                 + " đội đối mặt với ông ấy."),
                     collectBoss("Tìm 1 Tim băng", ICE_HEART, 1,
                           "Tim băng là khối băng vĩnh cửu kết tinh trong người những thực thể tối cao. Đánh bại chúng để"
                                 + " nhận được vật phẩm này."),
                     talk("Báo cáo với Thợ săn Zair", NPC_ZAIR, MAP_FREARH_RIVER,
                           "Bão tuyết đã ngừng. Hãy mang Tim băng về cho thợ săn Zair.",
                           "Đóng",
                           "Bão tuyết đã ngừng rồi! Lần đầu tiên ta thấy được mặt trời trên sông băng.\n"
                                 + "Để ta xem khối Tim băng này nào...\n"
                                 + "Bên trong nó có một ngọn lửa màu đen đang cháy, lạnh buốt tới tận xương.",
                           "Đây không còn là dấu ấn nữa, đây là một phần sức mạnh của kẻ đứng sau tất cả.\n"
                                 + "Hắn đang hút năng lượng từ các Tinh Linh để hồi sinh bản thân.\n"
                                 + "Tinh Linh Băng giá là mục tiêu tiếp theo, chúng ta phải ngăn hắn lại."))),
         // NV 33: thưởng khi bấm "Tiếp tục" ở trang 2 của Zair (bước 33.3).
         new MainQuest(33, "Kẻ Thách Thức Băng Giá",
               "Tinh Linh Băng giá là thực thể tối cao ngự trị vùng Frearh, và là mục tiêu cuối cùng của sức mạnh Hắc ám."
                     + " Hãy nâng cấp trang bị để tối ưu sức chiến đấu trước khi đối mặt với hắn.",
               List.of(locked(EXP, 25_000_000), locked(GOLD, 2_300_000), locked(ORIGIN_STONE, 2300),
                     locked(TALENT_BOOK, 100), locked(DIAMOND, 20),
                     // option như Mặt nạ Frearh rơi từ Tinh Linh Băng giá (world_boss 16), hạn 3 ngày
                     new RewardItem(FREARH_MASK, 1, true, new int[][] {{7, 35}, {21, 350}, {39, 50}},
                           new int[][] {{7, 75}, {21, 500}, {39, 100}}, 3)), 1,
               List.of(
                     ui(StepKind.UPGRADE_STAR, "Nâng cấp trang bị 5 sao", 5,
                           "Chỉ những chiến binh mạnh nhất mới chịu được cái lạnh của Tinh Linh Băng giá. Hãy đến Thợ rèn"
                                 + " nâng cấp 1 trang bị bất kỳ lên 5 sao để tăng cường khả năng chiến đấu."),
                     hitWorldBoss("Chạm trán Tinh Linh Băng giá", WORLD_BOSS_ICE_SPIRIT, 2500,
                           "Tinh Linh Băng giá đang dần bị sức mạnh Hắc ám xâm chiếm. Nếu hắn bị hắc hóa, cả Eldarah sẽ"
                                 + " chìm trong băng giá vĩnh cửu. Hãy chú ý thông báo từ hệ thống."),
                     talk("Báo cáo với Thợ săn Zair", NPC_ZAIR, MAP_FREARH_RIVER,
                           "Tinh Linh Băng giá đã bị đánh bại. Hãy trở về báo tin cho thợ săn Zair.",
                           "Đóng",
                           "Cậu làm được rồi! Băng trên sông đã bắt đầu tan, Frearh đang thở trở lại.\n"
                                 + "Rừng xanh, Đầm lầy, Biển cả, Phong thạch và giờ là Băng giá... cậu đã cứu cả năm vùng"
                                 + " đất.",
                           "Nhưng khi Tinh Linh ngã xuống, ngọn lửa đen trong người hắn đã thoát ra.\n"
                                 + "Nó không bay về phương nào khác, mà bay thẳng về phía Thượng nguồn Eldarah.\n"
                                 + "Nơi bắt đầu của mọi dòng sông, và cũng là quê hương của cậu.",
                           "Ta sẽ gửi tin cho Tù trưởng Kargu, trưởng làng Ribaron và bà Olivar.\n"
                                 + "Cậu hãy trở về Eldarah nghỉ ngơi và củng cố sức mạnh.\n"
                                 + "Trận chiến thật sự với kẻ đứng sau tất cả... sắp bắt đầu rồi."))));

   /**
    * Map mở theo nhiệm vụ: đi được map khi đang làm nhiệm vụ ≥ số này. Map 2 / 5 luôn đi được; map không có trong bảng
    * = {@link #UNLISTED_MAP_QUEST}.
    */
   private static final Map<Integer, Integer> MAP_QUEST = Map.ofEntries(
         Map.entry(MAP_VILLAGE, 1), Map.entry(MAP_FARM, 1),
         Map.entry(MAP_REED_VALLEY, 4),
         Map.entry(4, 5),                                   // Thảo nguyên mạch gió
         Map.entry(MAP_CLIFF, 9),                           // Vách núi Eldarah
         Map.entry(6, 11), Map.entry(7, 11), Map.entry(8, 11), // Đại ngàn Sayari, Rừng cổ mộc, Rừng thần mộc
         Map.entry(9, 16), Map.entry(10, 16), Map.entry(11, 16), // Đầm lầy Avenna, Rạch cá sấu, Rừng ngập mặn
         Map.entry(18, 22), Map.entry(19, 22), Map.entry(20, 22), // Bờ biển / Làng chài Velmora, Bãi đá ngầm
         Map.entry(12, 26), Map.entry(13, 26), Map.entry(14, 26), // Bình nguyên Darguun, Cao nguyên đá, Núi phong thạch
         Map.entry(15, 30), Map.entry(16, 30), Map.entry(17, 30), // Cánh đồng tuyết, Sông băng Frearh, Núi linh băng
         // Tế đàn Thiên tích (vào qua Tư tế Buraen — AltarService.join kiểm tra): từ nhiệm vụ 10
         Map.entry(34, 10), Map.entry(35, 10), Map.entry(36, 10), Map.entry(37, 10), Map.entry(38, 10),
         Map.entry(39, 10), Map.entry(40, 10),
         // Rừng Linh Mộc (phó bản qua Bác Rano): từ nhiệm vụ 15
         Map.entry(25, 15), Map.entry(26, 15), Map.entry(27, 15), Map.entry(28, 15), Map.entry(29, 15),
         Map.entry(30, 15));
   /** Map chưa xếp vào chuỗi nhiệm vụ (22, 31...): KHOÁ hẳn tới khi thêm vào {@link #MAP_QUEST}. */
   static final int UNLISTED_MAP_QUEST = Integer.MAX_VALUE;

   public static final String MAP_LOCKED = "Bạn phải làm xong nhiệm vụ mới có thể đến khu vực này";

   private MainQuests() {
   }

   /** Số nhiệm vụ đã viết (id 1..count). */
   public static int count() {
      return QUESTS.size();
   }

   /** Nhiệm vụ {@code id}; chưa viết thì 1 nhiệm vụ "đang cập nhật" (không xong được, không thưởng). */
   public static MainQuest get(int id) {
      if (id >= 1 && id <= QUESTS.size()) {
         return QUESTS.get(id - 1);
      }
      return new MainQuest(id, "Nhiệm vụ đang cập nhật", "Nhiệm vụ tiếp theo sẽ sớm ra mắt.", List.of(), -1,
            List.of(ui(StepKind.WAIT, "Chờ nhiệm vụ mới", 1, "Hãy quay lại sau khi máy chủ cập nhật nhiệm vụ mới.")));
   }

   /** Nhiệm vụ đang làm (null = chưa nhận = nhiệm vụ 1). */
   public static int currentQuest(PlayerData.MainQuestProgress q) {
      return q == null ? 1 : q.questId();
   }

   /** Đi được tới map {@code mapId} chưa. */
   public static boolean canEnter(PlayerData.MainQuestProgress q, int mapId) {
      return currentQuest(q) >= MAP_QUEST.getOrDefault(mapId, UNLISTED_MAP_QUEST);
   }

   /** Nhiệm vụ phải đang làm (≥) để đi được map {@code mapId}; {@link Integer#MAX_VALUE} = khoá hẳn. */
   public static int requiredQuest(int mapId) {
      return MAP_QUEST.getOrDefault(mapId, UNLISTED_MAP_QUEST);
   }

   /** Map có trong chuỗi nhiệm vụ (người chơi tới được khi đủ nhiệm vụ) — map ngoài chuỗi khoá hẳn. */
   public static boolean listed(int mapId) {
      return MAP_QUEST.containsKey(mapId);
   }

   /** Map Nông trại (cây táo riêng). */
   public static boolean isFarm(int mapId) {
      return mapId == MAP_FARM;
   }

   /** Rương đồ (NPC 19) mở được từ bước "Mở rương đồ" của nhiệm vụ 3 trở đi. */
   public static boolean chestUnlocked(PlayerData.MainQuestProgress q) {
      int quest = currentQuest(q);
      return quest > 3 || (quest == 3 && q.stage() >= 1);
   }

   private static RewardItem locked(int templateId, int quantity) {
      return new RewardItem(templateId, quantity, true, new int[0][], 0);
   }
}
