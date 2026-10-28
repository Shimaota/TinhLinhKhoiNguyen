package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.tribe.TribeService;
import com.tlnt.server.util.Log;

/**
 * Opcode 37 C→S: Bộ lạc (cửa sổ {@code TribeWindow}). Tên method client đều SAI nghĩa (Market / Auction / Pet...) — nghĩa
 * thật dò từ nút gọi nó:
 *
 * <pre>
 * sub | method client      | nút (nơi bấm)                                        | payload
 * 0   | sendMarketSearch   | mở tab "Bộ lạc" + gõ ô tìm kiếm (TribeListEntry)     | str từ khoá
 * 1   | sendMarketBuy      | "Tạo bộ lạc" (CreateTribeInputCallback)              | str tên
 * 2   | sendMarketSell     | "Xin vào" (JoinTribeClickListener)                   | int id bộ lạc
 * 3   | sendMarketCancel   | "Đồng ý" tab "Chờ duyệt" (AcceptApplicantClickListener) | int id người xin
 * 4   | sendMarketAction   | "Từ chối" tab "Chờ duyệt"                            | int id người xin
 * 5   | sendRequestMarket  | "Rời đi" (LeaveTribeButtonListener)                  | —
 * 6   | sendRequestAuction | "Giải tán" (DisbandTribeButtonListener)              | —
 * 7   | sendAuctionBid     | "Góp vàng" (DonateGoldInputCallback)                 | int vàng
 * 8   | sendAuctionAction  | "Góp L.Thạch" (DonateSpiritStoneInputCallback)       | int lam thạch
 * 9   | sendRequestPet     | "Mở kỹ năng" (TribeSkillActionClickListener)         | —
 * 10  | sendPetAction      | "Đổi kỹ năng" (LearnSkillClickListener)              | int tribe_skill.id đang chọn
 * 11  | sendPetFeed        | "Mời vào bộ lạc" (menu chân dung, SellItemMenuAction) | int id người chơi
 * 12  | sendPetEquip       | "Phong phó Tù trưởng" (menu thành viên)              | int id thành viên
 * 13  | sendPetSkill       | "Nhường Tù trưởng" (menu thành viên)                 | int id thành viên
 * 14  | sendPetRename      | "Đuổi khỏi bộ lạc" (menu thành viên)                 | int id thành viên
 * 15  | sendPetRelease     | "Cắt chức phó Tù trưởng" (menu thành viên)           | int id thành viên
 * 16  | sendSetTitle       | "Khẩu hiệu" (SetMottoInputCallback)                  | str khẩu hiệu
 * </pre>
 * Rương bộ lạc ("Sử dụng" / "Phân phát") đi qua op 27 sub 14 / 15 ({@link ItemHandler}). Logic: {@link TribeService}.
 */
public final class TribeHandler implements PacketHandler {

   private final TribeService tribes;

   public TribeHandler(TribeService tribes) {
      this.tribes = tribes;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      byte sub = message.readByte();
      switch (sub) {
         case 0 -> tribes.search(session, message.readString());
         case 1 -> tribes.create(session, message.readString());
         case 2 -> tribes.apply(session, message.readInt());
         case 3 -> tribes.approve(session, message.readInt());
         case 4 -> tribes.reject(session, message.readInt());
         case 5 -> tribes.leave(session);
         case 6 -> tribes.disband(session);
         case 7 -> tribes.donate(session, message.readInt(), false);
         case 8 -> tribes.donate(session, message.readInt(), true);
         case 9 -> tribes.openSkill(session);
         case 10 -> tribes.changeSkill(session, message.readInt());
         case 11 -> tribes.invite(session, message.readInt());
         case 12 -> tribes.appointDeputy(session, message.readInt());
         case 13 -> tribes.transferLeader(session, message.readInt());
         case 14 -> tribes.kick(session, message.readInt());
         case 15 -> tribes.demoteDeputy(session, message.readInt());
         case 16 -> tribes.setSlogan(session, message.readString());
         default -> Log.debug("Session #%d gui opcode 37 sub %d chua ho tro", session.getId(), sub);
      }
   }
}
