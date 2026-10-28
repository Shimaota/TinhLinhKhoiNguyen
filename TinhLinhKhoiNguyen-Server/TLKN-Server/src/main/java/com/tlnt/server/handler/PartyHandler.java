package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.party.PartyService;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 31 C→S: tổ đội (cửa sổ TEAM_POPUP + menu người chơi). Tên method client đều sai:
 *
 * <pre>
 * sub | method client           | ý nghĩa thật (nút)                                | payload
 * 0   | sendOpenStorage         | "Tạo tổ đội"                                      | —
 * 1   | sendRequestLeaderboard  | "Giải tán" (trưởng nhóm)                          | —
 * 2   | sendLeaderboardAction   | "Mời tổ đội" / "Mời vào tổ đội"                   | int playerId
 * 3   | sendRequestGuild        | "Rời nhóm"                                        | —
 * 4   | sendRequestParty        | mở tab / "Làm mới" tổ đội lân cận -> S→C 31/1     | —
 * 5   | sendPartyAction         | "Xin vào"                                         | int partyId
 * 6   | sendPartyInvite         | "Đồng ý" yêu cầu xin vào (trưởng nhóm)            | int playerId
 * 7   | sendRequestFriends      | ô tick "Tự động duyệt" (đảo trạng thái)           | —
 * 8   | sendFriendAction        | "Nhường trưởng nhóm"                              | int playerId
 * 9   | sendGuildAction         | "Đuổi khỏi nhóm"                                  | int playerId
 * </pre>
 */
public final class PartyHandler implements PacketHandler {

   private final PartyService parties;

   public PartyHandler(PartyService parties) {
      this.parties = parties;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> parties.create(session);
         case 1 -> parties.disband(session);
         case 2 -> parties.invite(session, message.readInt());
         case 3 -> parties.leave(session);
         case 4 -> parties.sendNearby(session);
         case 5 -> parties.requestJoin(session, message.readInt());
         case 6 -> parties.approve(session, message.readInt());
         case 7 -> parties.toggleAutoApprove(session);
         case 8 -> parties.transferLeader(session, message.readInt());
         case 9 -> parties.kick(session, message.readInt());
         default -> {
         }
      }
   }
}
