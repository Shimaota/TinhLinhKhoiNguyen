package com.tlnt.server.plant;

import com.tlnt.server.gather.GatherConfig;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Hái cây (op 54). Lane GAME (luồng session): kiểm tra người chơi rồi chuyển sang luồng tick khu
 * ({@link World#harvestPlant}) — cây là trạng thái của khu; hái xong đồ được cho vào túi trên lại luồng session.
 *
 * <h2>Luồng</h2>
 * <ol>
 *   <li>Người chơi bấm cây (client tự đi tới cách ≤ 1 m rồi gửi C→S op 54 sub 0, int id thực thể cây).</li>
 *   <li>Session: còn sống, không giao dịch / câu cá, hành trang còn chỗ.</li>
 *   <li>Khu: cây còn, loại cây có cấu hình ({@link PlantConfig}), không ai đang hái, đứng trong {@link #REACH_PX} ->
 *       GIỮ cây, thanh "Đang thu hoạch..." (client khoá di chuyển). Người khác bấm vào cây đang có người hái bị từ chối.</li>
 *   <li>Hết thời gian: còn trong khu + còn sống + không đi xa -> cây biến mất (cả khu thấy động tác hái), hẹn mọc lại
 *       ở chỗ ngẫu nhiên; đồ vào túi (op 22), thông báo.</li>
 * </ol>
 */
public final class PlantService {

   /** Đứng cách cây tối đa (pixel, mỗi trục); client đi tới ~100 px mới gửi. */
   static final float REACH_PX = 220f;
   /** Lệnh test /qua: số món mỗi loại. */
   private static final int TEST_QUANTITY = 5;

   private final PlantRepository repository;
   private final World world;
   private final ItemService items;
   private final Executor gamePool;

   public PlantService(PlantRepository repository, World world, ItemService items, Executor gamePool) {
      this.repository = repository;
      this.world = world;
      this.items = items;
      this.gamePool = gamePool;
      reload();
   }

   /** Đọc lại {@code plant_template.harvest} (luồng blocking). Lỗi / trống thì giữ bản cũ (mặc định). */
   public void reload() {
      PlantConfig loaded = repository.load();
      if (loaded != null) {
         PlantConfig.set(loaded);
      }
   }

   /** C→S op 54 sub 0. */
   public void harvest(PlayerSession session, int plantId) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      String error = d.isDead() ? "Không thể thu hoạch khi đã bị hạ gục."
            : d.trade != null ? "Đang giao dịch, không thể thu hoạch."
            : d.fishing != null ? "Đang câu cá, không thể thu hoạch."
            : bagFull(d) ? "Cần ít nhất 1 ô trong hành trang"
            : null;
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      world.harvestPlant(session, plantId, REACH_PX,
            (player, entry) -> session.inbox().execute(() -> reward(session, entry), gamePool));
   }

   /** Luồng session: cho đồ của cây vào túi. */
   private void reward(PlayerSession session, PlantConfig.Entry entry) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      List<ItemData> got = new ArrayList<>();
      for (RewardItem r : entry.items()) {
         got.add(r.create(now));
      }
      String error;
      if (d.trade != null) {
         error = "Đang giao dịch";
      } else {
         synchronized (d) { // cùng khoá với thư / kí gửi (chống nhân bản)
            error = ItemService.give(d, got, now);
         }
      }
      if (error != null) {
         session.send(Packets.notice(error + ", không nhận được " + names(got) + "."));
         return;
      }
      items.syncBagAndCurrency(session);
      session.send(Packets.notice("Bạn vừa nhận được " + names(got) + "!"));
      Log.debug("'%s' hai cay %d", d.name, entry.templateId());
      QuestEvents.fire(session, StepKind.HARVEST_PLANT, entry.templateId()); // vd NV 14.3 hái Nấm hương
      com.tlnt.server.event.MidAutumnService.plantHarvested(session, d, entry.templateId()); // Cà rốt: +1 điểm
      int flowers = 0;
      for (ItemData item : got) {
         if (com.tlnt.server.forge.BrewService.RECIPES.containsKey(item.templateId)) {
            flowers += item.quantity;
         }
      }
      if (flowers > 0) {
         QuestEvents.fire(session, StepKind.HARVEST_FLOWER, 0, flowers); // NV 19.1 hái Hoa ma thuật
      }
      com.tlnt.server.daily.DailyEvents.obtained(session, got); // NV hằng ngày "Nhặt Quả mọng / Nấm hương / Cỏ hồi sinh"
      com.tlnt.server.player.ActivityPoints.add(session, d, com.tlnt.server.player.ActivityPoints.PLANT_HARVEST);
   }

   /**
    * Hết ô trống và không chồng được món hái nào (chưa biết cây nào ở luồng này nên xét mọi món hái được). Lúc nhận
    * đồ vẫn kiểm tra lại thật.
    */
   private static boolean bagFull(PlayerData d) {
      if (d.bag.firstEmpty() >= 0) {
         return false;
      }
      long now = System.currentTimeMillis();
      Set<Integer> ids = harvestItemIds();
      for (int slot = 0; slot < d.bag.size(); slot++) {
         ItemData item = d.bag.get(slot);
         if (item != null && ids.contains(item.templateId) && !item.isExpired(now)) {
            return false;
         }
      }
      return true;
   }

   private static Set<Integer> harvestItemIds() {
      Set<Integer> ids = new LinkedHashSet<>();
      for (PlantConfig.Entry e : PlantConfig.current().byTemplate().values()) {
         for (RewardItem r : e.items()) {
            ids.add(r.templateId());
         }
      }
      return ids;
   }

   private static String names(List<ItemData> got) {
      StringBuilder sb = new StringBuilder();
      for (ItemData item : got) {
         if (sb.length() > 0) {
            sb.append(", ");
         }
         sb.append(com.tlnt.server.item.ItemPackets.receivedName(item));
      }
      return sb.toString();
   }

   // ================================================================== lệnh test

   /** /qua: {@value #TEST_QUANTITY} món mỗi loại hái / săn được (quả mọng, cỏ hồi sinh, nấm hương, mật ong, dừa). */
   public void giveTestItems(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      List<ItemData> got = new ArrayList<>();
      for (PlantConfig.Entry e : PlantConfig.current().byTemplate().values()) {
         for (RewardItem r : e.items()) {
            ItemData item = r.create(now);
            item.quantity = TEST_QUANTITY;
            got.add(item);
         }
      }
      for (GatherConfig.Entry e : GatherConfig.current().byMob().values()) { // mật ong, quả dừa
         for (RewardItem r : e.items()) {
            ItemData item = r.create(now);
            item.quantity = TEST_QUANTITY;
            got.add(item);
         }
      }
      String error;
      synchronized (d) {
         error = d.trade != null ? "Đang giao dịch." : ItemService.give(d, got, now);
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      items.syncBagAndCurrency(session);
      session.send(Packets.notice("Bạn vừa nhận được " + names(got) + "!"));
   }
}
