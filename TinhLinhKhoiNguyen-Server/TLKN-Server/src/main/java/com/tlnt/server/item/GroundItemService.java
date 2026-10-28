package com.tlnt.server.item;

import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Nhặt vật phẩm dưới đất (C→S op 30 sub 0). Luồng session kiểm tra người chơi -> luồng tick khu gỡ vật phẩm
 * ({@link World#pickUp}) -> lại luồng session cho vào túi. Không vào được túi (đầy / đang giao dịch) thì thả lại dưới
 * chân người nhặt (không mất đồ).
 */
public final class GroundItemService {

   /** Túi đầy / đang giao dịch: tạm ngừng "Tự nhặt vật phẩm" bấy lâu. */
   private static final long AUTO_PICKUP_PAUSE_MS = 10_000;

   private final World world;
   private final ItemService items;
   private final Executor gamePool;

   public GroundItemService(World world, ItemService items, Executor gamePool) {
      this.world = world;
      this.items = items;
      this.gamePool = gamePool;
      // "Tự nhặt vật phẩm": khu đã gỡ đồ khỏi đất (luồng tick) -> vào túi trên luồng session như bấm nhặt
      world.setAutoPickup((player, item) -> {
         PlayerSession session = player.session();
         session.inbox().execute(() -> give(session, item), gamePool);
      });
   }

   public void pickUp(PlayerSession session, int dropId) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      String error = d.isDead() ? "Không thể nhặt khi đã bị hạ gục."
            : d.trade != null ? "Đang giao dịch, không thể nhặt."
            : d.fishing != null ? "Đang câu cá, không thể nhặt."
            : null;
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      world.pickUp(session, dropId, (player, item) -> session.inbox().execute(() -> give(session, item), gamePool));
   }

   /**
    * Dòng 48 "+X% Thưởng vàng": vàng (thường / khoá) nhặt từ đất — mọi đồ dưới đất đều do quái / boss rơi ra — được thêm
    * X% (làm tròn xuống). Món khác / X = 0: trả nguyên món. Bản sao, không sửa món gốc.
    */
   static ItemData withGoldBonus(ItemData item, long percent) {
      if (item.templateId != ItemService.GOLD_TEMPLATE_ID || percent <= 0) {
         return item;
      }
      long amount = Math.min(Integer.MAX_VALUE, (long) item.quantity * (100 + percent) / 100);
      ItemData copy = item.copy();
      copy.quantity = (int) amount;
      return copy;
   }

   /** Luồng session. */
   private void give(PlayerSession session, ItemData item) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return; // đã thoát: vật phẩm mất (như rơi ra rồi biến mất)
      }
      long now = System.currentTimeMillis();
      ItemData got = withGoldBonus(item, d.stats == null ? 0 : d.stats.goldBonusPercent);
      String error;
      if (d.trade != null) {
         error = "Đang giao dịch, không thể nhặt.";
      } else {
         synchronized (d) { // cùng khoá với thư / kí gửi (chống nhân bản)
            // túi đầy: chỉ nhặt được món cộng dồn vào chồng sẵn có
            error = ItemService.giveStacking(d, d.bag.deepCopy(), List.of(got), now);
         }
      }
      if (error != null) {
         session.send(Packets.notice(error));
         d.autoPickupPausedUntil = now + AUTO_PICKUP_PAUSE_MS; // không hút lại món vừa thả ra liên tục
         world.dropAtFeet(session, item); // thả lại món GỐC: nhặt lại mới cộng Thưởng vàng (không cộng 2 lần)
         return;
      }
      item = got;
      if (item.templateId == ItemService.GOLD_TEMPLATE_ID) { // Ve chai: vàng nhặt từ đất (quái / boss rơi), cả Thưởng vàng
         com.tlnt.server.ranking.RankingService.goldPicked(d, item.quantity);
      }
      items.syncBagAndCurrency(session);
      session.send(Packets.notice("Bạn vừa nhận được " + ItemPackets.receivedName(item) + "!"));
      Log.debug("'%s' nhat item %d", d.name, item.templateId);
      QuestEvents.fire(session, StepKind.COLLECT, item.templateId); // vật phẩm nhiệm vụ
      QuestEvents.fire(session, StepKind.COLLECT_ELITE, item.templateId); // 13.1 Nanh Hổ răng kiếm (bước loại riêng)
      QuestEvents.fire(session, StepKind.COLLECT_BOSS, item.templateId); // 16.2 Bùa linh mộc (rơi từ boss)
      com.tlnt.server.daily.DailyEvents.obtained(session, List.of(item)); // NV hằng ngày "Nhặt ..." (vàng = item 1)
   }
}
