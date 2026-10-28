package com.tlnt.server.handler;

import com.tlnt.server.daily.DailyQuestService;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.net.Packets;
import com.tlnt.server.party.PartyService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.social.SocialService;
import com.tlnt.server.trade.TradeService;
import com.tlnt.server.tribe.TribeService;

/**
 * Opcode 32 C→S: bấm "Đồng ý" ở hộp hỏi / lời mời server gửi ({@code Packets.confirm} / {@code Packets.invitation});
 * client gửi lại byte loại ({@code GiftcodeConfirmAcceptListener} / {@code InvitationAcceptClickListener} ->
 * {@code sendEmote}, tên sai). "Từ chối" không gửi gì.
 *
 * <pre>
 * loại | hộp hỏi                                  | xử lý
 * 1    | lần đầu dùng vũ khí (chọn class)         | ItemService.confirmFirstWeapon
 * 2    | lời mời giao dịch                        | TradeService.acceptInvite
 * 3    | lời mời thách đấu                        | SocialService.acceptDuel
 * 4    | lời mời vào tổ đội                       | PartyService.acceptInvite
 * 5    | mở thêm 1 ô bộ sưu tập (10 triệu vàng)   | ItemService.confirmExpandCollection
 * 6    | có người xin vào tổ đội (trưởng nhóm)    | PartyService.acceptRequest
 * 7    | dùng item hiệu ứng đè hiệu ứng cùng nhóm | ItemService.confirmUseBuff
 * 8    | bán vật phẩm (nút "Bán" túi đồ)          | ItemService.confirmSell
 * 9    | lời mời vào bộ lạc                       | TribeService.acceptInvite
 * 10   | Tù trưởng nhường chức                    | TribeService.confirmTransfer
 * 11/12| giải tán bộ lạc: hỏi lần 1 / lần cuối    | TribeService.confirmDisband
 * 13   | dùng biểu tượng thay biểu tượng còn hạn  | TribeService.confirmEmblem
 * 14   | dùng Bùa tẩy điểm                        | UsableItems.confirmReset
 * 15   | "Đổi NV" hằng ngày (tốn 1 Vé nhiệm vụ)   | DailyQuestService.confirmSwap
 * 16   | "Nhận NV mới" hằng ngày (1 Vé nhiệm vụ)  | DailyQuestService.confirmAdd
 * 17   | "Lớn nhanh" heo con (5 lam thạch)        | PigService.confirmGrowFast
 * 18   | "Luyện tập Offline"                      | OfflineTrainingService.confirm
 * 19   | bảng Boss: "Dịch chuyển" (5 lam thạch)   | WorldBossList.confirmTeleport
 * 20   | bảng Boss: "Tìm đường" (100.000 vàng)    | WorldBossList.confirmPath
 * </pre>
 */
public final class ConfirmHandler implements PacketHandler {

   private final ItemService items;
   private final TradeService trades;
   private final SocialService social;
   private final PartyService parties;
   private final TribeService tribes;
   private final DailyQuestService daily;

   public ConfirmHandler(ItemService items, TradeService trades, SocialService social, PartyService parties,
         TribeService tribes, DailyQuestService daily) {
      this.items = items;
      this.trades = trades;
      this.social = social;
      this.parties = parties;
      this.tribes = tribes;
      this.daily = daily;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case Packets.CONFIRM_FIRST_WEAPON -> items.confirmFirstWeapon(session);
         case Packets.CONFIRM_TRADE -> trades.acceptInvite(session);
         case Packets.CONFIRM_DUEL -> social.acceptDuel(session);
         case Packets.CONFIRM_PARTY -> parties.acceptInvite(session);
         case Packets.CONFIRM_EXPAND_COLLECTION -> items.confirmExpandCollection(session);
         case Packets.CONFIRM_PARTY_REQUEST -> parties.acceptRequest(session);
         case Packets.CONFIRM_USE_BUFF -> items.confirmUseBuff(session);
         case Packets.CONFIRM_SELL -> items.confirmSell(session);
         case Packets.CONFIRM_TRIBE_INVITE -> tribes.acceptInvite(session);
         case Packets.CONFIRM_TRIBE_TRANSFER -> tribes.confirmTransfer(session);
         case Packets.CONFIRM_TRIBE_DISBAND -> tribes.confirmDisband(session, false);
         case Packets.CONFIRM_TRIBE_DISBAND_FINAL -> tribes.confirmDisband(session, true);
         case Packets.CONFIRM_TRIBE_EMBLEM -> tribes.confirmEmblem(session);
         case Packets.CONFIRM_RESET_POINTS -> items.usables().confirmReset(session);
         case Packets.CONFIRM_DAILY_SWAP -> daily.confirmSwap(session);
         case Packets.CONFIRM_DAILY_ADD -> daily.confirmAdd(session);
         case Packets.CONFIRM_OFFLINE_TRAINING -> {
            com.tlnt.server.offline.OfflineTrainingService offline = com.tlnt.server.offline.OfflineTrainingService.instance();
            if (offline != null) {
               offline.confirm(session);
            }
         }
         case Packets.CONFIRM_PIG_GROW -> {
            com.tlnt.server.farm.PigService pigs = com.tlnt.server.farm.PigService.instance();
            if (pigs != null) {
               pigs.confirmGrowFast(session);
            }
         }
         case Packets.CONFIRM_BOSS_TELEPORT -> {
            com.tlnt.server.worldboss.WorldBossList bosses = com.tlnt.server.worldboss.WorldBossList.instance();
            if (bosses != null) {
               bosses.confirmTeleport(session);
            }
         }
         case Packets.CONFIRM_BOSS_PATH -> {
            com.tlnt.server.worldboss.WorldBossList bosses = com.tlnt.server.worldboss.WorldBossList.instance();
            if (bosses != null) {
               bosses.confirmPath(session);
            }
         }
         default -> {
         }
      }
   }
}
