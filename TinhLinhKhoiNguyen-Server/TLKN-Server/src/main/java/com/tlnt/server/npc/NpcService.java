package com.tlnt.server.npc;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.MapPack;
import com.tlnt.server.forge.BrewService;
import com.tlnt.server.forge.CraftService;
import com.tlnt.server.forge.ForgePackets;
import com.tlnt.server.forge.RefineService;
import com.tlnt.server.forge.SoulService;
import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.forge.WeaponCraftService;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.QuestService;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.shop.ShopService;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bấm NPC (op 24 C→S). Client tự chạy tới cạnh NPC rồi mới gửi; server kiểm tra lại: NPC có thật trên map đang đứng
 * (đúng id thực thể + mẫu), đứng gần, còn sống. Việc của từng loại NPC = {@link NpcAction} đăng ký theo
 * {@code npc_template.id} ({@link #registerDefaults}); NPC chưa đăng ký thì chào 1 câu mặc định.
 *
 * <p>Luồng: lane GAME, luồng session của người bấm (đọc/ghi dữ liệu của chính họ). Vị trí người chơi do luồng tick khu
 * ghi — đọc không khoá (lệch vài frame không sao).
 */
public final class NpcService {

   /** Khoảng cách tối đa (pixel, mỗi trục) từ người chơi tới NPC để tương tác (client đứng cách ~100 px). */
   public static final float TALK_RANGE_PX = 400f;

   /** {@code npc_template.id}. */
   public static final int NPC_BABURU = 1;
   public static final int NPC_MORGI = 2;
   public static final int NPC_NIMORA = 3;
   public static final int NPC_TOORU = 4;
   public static final int NPC_SELAMI = 5;
   public static final int NPC_LYRINA = 6;
   public static final int NPC_BURAN = 7;
   public static final int NPC_BURAEN = 17;
   public static final int NPC_CHEST = 19;
   public static final int NPC_TARAE = 20;
   /** {@code tlnt_data.shop.id} của các shop ở Làng ánh sáng Eldarah. */
   public static final int SHOP_GROCERY = 1;
   public static final int SHOP_PHARMACY = 2;
   public static final int SHOP_BLACKSMITH = 4;
   /** Tarae (Ngoại ô làng) "Đổi cá". */
   public static final int SHOP_FISH = 5;
   /** Olivar (Rạch cá sấu) "Cửa hàng". */
   public static final int SHOP_OLIVAR = 6;

   /** Trang hộp thoại Ông Morgi. */
   private static final int MORGI_MAIN = 0;
   private static final int MORGI_CRAFT = 1;
   private static final int MORGI_ENHANCE = 2;
   /** Như MORGI_CRAFT nhưng có nút {@link #CRAFT_BUTTON} đầu tiên (đang ở bước nhiệm vụ rèn vũ khí). */
   private static final int MORGI_CRAFT_QUEST = 4;
   /** Tên nút mở "Chế Tác Vũ Khí" — hướng dẫn client {@code TutorialWeaponCraftStep} tìm đúng chữ này. */
   private static final String CRAFT_BUTTON = "Rèn vũ khí";

   private final World world;
   private final ShopService shops;
   private final Map<Integer, NpcAction> actions = new HashMap<>();

   public NpcService(World world, ShopService shops) {
      this.world = world;
      this.shops = shops;
      registerDefaults();
   }

   /** Đăng ký việc của 1 loại NPC (gọi lúc khởi động). */
   public void register(int npcTemplateId, NpcAction action) {
      actions.put(npcTemplateId, action);
   }

   /** NPC đã code. Thêm NPC mới: 1 dòng ở đây. */
   private void registerDefaults() {
      register(NPC_CHEST, NpcService::openChest);
      register(NPC_TOORU, ctx -> ctx.say(0, "Ồ!..Chào buổi sáng\n"
            + "Trưởng làng nhờ cậu tới giúp ta mấy công việc ở nông trại đó hả?", "Đóng"));
      register(NPC_SELAMI, shopKeeper("Ồ! Lại là con hả...\n"
            + "- Đi đường chắc mệt rồi đúng không?... Ghé vào uống ngụm nước, mua chút đồ rồi hãy đi tiếp nha!",
            "Cửa hàng tạp hóa", SHOP_GROCERY));
      register(NPC_NIMORA, shopKeeper("Người làng thì không cần lời khách sáo. Vào đây uống ngụm nước lá ấm để ta xem"
            + " vết thương cho cậu nhé!\n"
            + "- Ta có đủ dược phẩm, Thuốc hồi phục, Thuốc sức mạnh, Thuốc cứng cáp và Thuốc trí tuệ.",
            "Cửa hàng dược phẩm", SHOP_PHARMACY));
      register(NPC_BABURU, ctx -> ctx.say(0, "Chào mừng con đã đến Làng ánh sáng Eldarah... nếu muốn con có thể ở lại"
            + " làng\n"
            + "- Ta là Trưởng làng Baburu. Nếu có chuyện gì con cứ đến gặp ta", "Đóng"));
      register(NPC_LYRINA, ctx -> ctx.say(0, "Cậu đi đâu vậy? ...Sáng giờ tớ thấy cậu cứ chạy tới chạy lui.\n"
            + "- Có muốn ăn chút thịt gà nướng không?", "Đóng"));
      register(NPC_BURAN, ctx -> ctx.say(0, "Cậu đi đâu vậy? Ngoài này nhiều thú dữ, hãy trở về trước khi trời tối"
            + " nhé!\n"
            + "- Đừng đi sâu quá lúc này, trời tối nhanh lắm", "Đóng"));
      register(NPC_TARAE, shopKeeper("Cậu là người Làng ánh sáng hả? Bình thường ta sống ở Ngoại ô làng nên ít khi gặp"
            + " người lạ lắm\n"
            + "- Ta là Tarae... Thỉnh thoảng ta sẽ vào vùng hoang dã săn bắt hoặc tìm vài thứ hữu ích mang về Làng để"
            + " trao đổi lấy lương thực",
            "Đổi cá", SHOP_FISH));
      // NPC 8 Olivar (Rạch cá sấu): "Chế tạo Ma thuật" = lò tab Pha chế (forge/BrewService), "Cửa hàng" = shop 6
      register(BrewService.NPC_OLIVAR, new NpcAction() {
         @Override
         public void onTalk(NpcContext ctx) {
            ctx.say(0, "Ma thuật không phải hàng hóa... nhưng chúng ta vẫn có thể trao đổi!", "Chế tạo Ma thuật",
                  "Cửa hàng", "Đóng");
         }

         @Override
         public void onChoice(NpcContext ctx, int page, int option) {
            switch (option) {
               case 0 -> {
                  BrewService.open(ctx.data(), ctx.mapId(), ctx.spawn().x(), ctx.spawn().y()); // client xoá hết ô
                  ctx.send(ForgePackets.open(ForgePackets.MODE_BREW));
                  ctx.send(BrewService.statePacket(ctx.data())); // mô tả chi phí / tỉ lệ
               }
               case 1 -> shops.open(ctx, SHOP_OLIVAR);
               default -> {
               }
            }
         }
      });
      // NPC_BURAEN (17): altar/AltarService (Đấu trường -> Tế đàn Thiên tích, cửa hàng tư thế) — đăng ký ở GameServer.
      register(NPC_MORGI, new NpcAction() {
         @Override
         public void onTalk(NpcContext ctx) {
            String text = "Lò lửa của ta chưa từng nguội.. Búa của ta đã rèn vũ khí cho bao thế hệ chiến binh"
                  + " của làng.\n"
                  + "- Thế nào... cậu có muốn rèn một vũ khí mới không?";
            // Hướng dẫn tân thủ client chỉ tay theo CHỮ nút: NV 5.4 "Chế tác" -> "Rèn vũ khí"
            // (TutorialWeaponCraftStep); NV 6.1 / 6.2 "Cường hóa trang bị" -> "Nâng cấp trang bị"
            // (TutorialWeaponForgeStep / TutorialEquipmentUpgradeStep). Đổi chữ nút = hướng dẫn mất.
            ctx.say(MORGI_MAIN, text, "Chế tác", "Cường hóa trang bị", "Cửa hàng thợ rèn", "Đóng");
         }

         @Override
         public void onChoice(NpcContext ctx, int page, int option) {
            if (page == MORGI_CRAFT_QUEST) { // trang Chế tác có thêm nút "Rèn vũ khí" ở đầu
               if (option == 0) {
                  WeaponCraftService.open(ctx);
                  return;
               }
               page = MORGI_CRAFT;
               option--;
            }
            switch (page) {
               case MORGI_MAIN -> {
                  switch (option) {
                     case 0 -> {
                        String text = "Chào mừng bạn đến lò rèn của ta!\n"
                              + "Ta có thể giúp cậu rèn những phôi trang bị và chế tác các vật liệu cường hóa giúp"
                              + " trang bị của cậu trở nên mạnh mẽ hơn.";
                        if (QuestService.atStep(ctx.data(), StepKind.CRAFT_WEAPON)) {
                           ctx.say(MORGI_CRAFT_QUEST, text, CRAFT_BUTTON, "Rèn linh thạch", "Tinh luyện đá",
                                 "Chuyển hóa trang bị", "Đóng");
                        } else {
                           ctx.say(MORGI_CRAFT, text, "Rèn linh thạch", "Tinh luyện đá", "Chuyển hóa trang bị",
                                 "Đóng");
                        }
                     }
                     case 1 -> ctx.say(MORGI_ENHANCE, "Ta thấy trang bị của cậu có vẻ cũ rồi đấy. Nếu cậu cần, ta có"
                           + " thể giúp cậu nâng cấp nó.\n"
                           + "- Ta không chỉ rèn thép... ta rèn cả ý chí của chiến binh",
                           "Nâng cấp trang bị", "Khảm bùa linh hồn", "Khảm đa nguyên tố", "Đóng");
                     case 2 -> shops.open(ctx, SHOP_BLACKSMITH);
                     default -> {
                     }
                  }
               }
               case MORGI_CRAFT -> {
                  switch (option) {
                     // "Rèn linh thạch" = lò rèn tab Chế tác. Cửa sổ "Chế Tác Vũ Khí" (op 60) chỉ ở bước nhiệm vụ 5.4
                     // (nút "Rèn vũ khí" — forge/WeaponCraftService).
                     case 0 -> {
                        CraftService.open(ctx.data(), ctx.mapId(), ctx.spawn().x(), ctx.spawn().y()); // client xoá hết ô
                        ctx.send(ForgePackets.open(ForgePackets.MODE_CRAFT));
                        ctx.send(CraftService.statePacket(ctx.data())); // mô tả chi phí / tỉ lệ
                     }
                     case 1 -> {
                        RefineService.open(ctx.data()); // client xoá hết ô khi mở -> server cũng xoá
                        ctx.send(ForgePackets.open(ForgePackets.MODE_REFINE));
                        ctx.send(RefineService.statePacket(ctx.data())); // mô tả + nút gạt mặt trời
                     }
                     case 2 -> { // tab "Chuyển hóa" (client mới, chế độ 6)
                        com.tlnt.server.forge.TransmuteService.open(ctx.data(), ctx.mapId(), ctx.spawn().x(),
                              ctx.spawn().y());
                        ctx.send(ForgePackets.open(ForgePackets.MODE_TRANSMUTE));
                        ctx.send(com.tlnt.server.forge.TransmuteService.statePacket(ctx.data())); // mô tả phí / tỉ lệ
                     }
                     default -> {
                     }
                  }
               }
               case MORGI_ENHANCE -> {
                  switch (option) {
                     case 0 -> {
                        // client xoá hết ô khi mở -> server cũng xoá lựa chọn; nhớ chỗ thợ rèn (đi xa = thôi cường hóa)
                        UpgradeService.open(ctx.data(), ctx.mapId(), ctx.spawn().x(), ctx.spawn().y());
                        ctx.send(ForgePackets.open(ForgePackets.MODE_UPGRADE));
                        ctx.send(UpgradeService.statePacket(ctx.data())); // mô tả 4 dòng (số 0)
                     }
                     case 1 -> {
                        SoulService.open(ctx.data(), ctx.mapId(), ctx.spawn().x(), ctx.spawn().y());
                        ctx.send(ForgePackets.open(ForgePackets.MODE_SOUL));
                        ctx.send(SoulService.statePacket(ctx.data())); // mô tả phí / tỉ lệ
                     }
                     case 2 -> {
                        com.tlnt.server.forge.ElementService.open(ctx.data(), ctx.mapId(), ctx.spawn().x(),
                              ctx.spawn().y());
                        ctx.send(ForgePackets.open(ForgePackets.MODE_ELEMENT));
                        ctx.send(com.tlnt.server.forge.ElementService.statePacket(ctx.data())); // mô tả
                     }
                     default -> {
                     }
                  }
               }
               default -> {
               }
            }
         }
      });
   }

   /** NPC bán hàng: hộp thoại 2 nút [tên cửa hàng, "Đóng"]; nút 0 mở shop {@code shopId}. */
   private NpcAction shopKeeper(String greeting, String shopButton, int shopId) {
      return new NpcAction() {
         @Override
         public void onTalk(NpcContext ctx) {
            ctx.say(0, greeting, shopButton, "Đóng");
         }

         @Override
         public void onChoice(NpcContext ctx, int page, int option) {
            if (page == 0 && option == 0) {
               shops.open(ctx, shopId);
            }
         }
      };
   }

   // ================================================================== gói C→S

   /** Sub 0: bấm NPC — byte id thực thể (vị trí trong danh sách NPC của map), int npc_template.id. */
   public void talk(PlayerSession session, int npcIndex, int templateId) {
      NpcContext ctx = context(session, npcIndex);
      if (ctx == null || ctx.template().id() != templateId) {
         return;
      }
      ctx.data().npcDialog = null; // bấm NPC mới = bỏ hộp thoại cũ
      ctx.data().questDialog = null;
      // Đang ở bước nhiệm vụ nói chuyện với NPC này: thêm nút "Nói chuyện" đầu menu (hộp thoại đầu tiên NPC mở).
      ctx.questButton = QuestEvents.service() != null && QuestService.canTalk(ctx.data(), ctx.mapId(), templateId);
      NpcAction action = actions.get(templateId);
      if (action != null) {
         action.onTalk(ctx);
      } else {
         ctx.say(0, "Chào " + ctx.data().name + "!", "Đóng");
      }
   }

   /**
    * Sub 1: bấm nút hộp thoại — byte trang, int npc_template.id (client gửi lại id của hộp thoại), byte nút. Phải đúng
    * hộp thoại server vừa mở; NPC lấy theo vị trí đã lưu lúc mở.
    */
   public void choose(PlayerSession session, int page, int templateId, int option) {
      PlayerData d = session.getPlayer();
      NpcContext.OpenDialog open = d == null ? null : d.npcDialog;
      if (open == null) {
         return;
      }
      d.npcDialog = null; // dùng 1 lần: bấm lại / gói giả không chạy 2 lần
      if (open.templateId() != templateId || open.page() != page || option < 0 || option >= open.choices()) {
         return;
      }
      NpcContext ctx = context(session, open.npcIndex());
      if (ctx == null || ctx.mapId() != open.mapId()) {
         return;
      }
      QuestService quests = QuestEvents.service();
      if (page >= QuestService.DIALOG_PAGE) { // trang hội thoại nhiệm vụ
         if (quests != null) {
            quests.choose(ctx, page, option);
         }
         return;
      }
      if (open.questButton()) {
         if (option == 0) {
            if (quests != null) {
               quests.talk(ctx);
            }
            return;
         }
         option--; // nút của NPC lùi 1 vì "Nói chuyện" đứng đầu
      }
      NpcAction action = actions.get(ctx.template().id());
      if (action != null) {
         action.onChoice(ctx, page, option);
      }
   }

   /** NPC {@code npcIndex} trên map người chơi đang đứng, null nếu không có / đứng xa / đã bị hạ gục. */
   private NpcContext context(PlayerSession session, int npcIndex) {
      WorldPlayer player = world.online(session);
      if (player == null || player.zone() == null || !player.data().inWorld || player.data().isDead()) {
         return null;
      }
      MapTemplate map = player.zone().map().template();
      List<MapTemplate.NpcSpawn> npcs = map.npcs();
      if (npcIndex < 0 || npcIndex >= npcs.size()) {
         return null;
      }
      MapTemplate.NpcSpawn spawn = npcs.get(npcIndex);
      MapPack.Npc template = GameDataCache.npc(spawn.id());
      if (template == null || !near(player, spawn.x(), spawn.y())) {
         return null;
      }
      return new NpcContext(session, player, map.id(), npcIndex, spawn, template);
   }

   /** Người chơi đứng trong {@link #TALK_RANGE_PX} quanh ({@code x}, {@code y}). */
   public static boolean near(WorldPlayer player, float x, float y) {
      return Math.abs(player.x() - x) <= TALK_RANGE_PX && Math.abs(player.y() - y) <= TALK_RANGE_PX;
   }

   // ================================================================== NPC đã code

   /**
    * Rương đồ: ghi nhớ rương vừa mở (map + vị trí) để cất / lấy / sắp xếp chỉ được khi còn đứng gần nó
    * ({@code ItemService.nearBox}), gửi lại nội dung rương rồi mở cửa sổ (op 53 sub 0).
    */
   private static void openChest(NpcContext ctx) {
      PlayerData d = ctx.data();
      if (!QuestEvents.chestUnlocked(d)) { // mở từ bước "Mở rương đồ" (nhiệm vụ 3)
         ctx.send(Packets.notice("Bạn chưa thể mở rương"));
         return;
      }
      d.boxMapId = ctx.mapId();
      d.boxX = ctx.spawn().x();
      d.boxY = ctx.spawn().y();
      ctx.send(ItemPackets.container(ItemPackets.SUB_BOX, d.box));
      ctx.send(ItemPackets.openBox());
      QuestEvents.fire(ctx.session(), StepKind.OPEN_CHEST, NPC_CHEST);
   }
}
