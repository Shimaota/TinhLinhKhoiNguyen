package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.social.SocialService;

/**
 * Opcode 34 C→S: bạn bè (tab "Bạn bè" trong cửa sổ tổ đội, menu người chơi). Tên method client đều sai:
 *
 * <pre>
 * sub | method client      | ý nghĩa thật                                   | payload
 * 0   | sendRequestMap     | mở tab danh sách bạn bè -> S→C 34/0            | —
 * 1   | sendRequestSkills  | mở tab "Lời mời" -> S→C 34/1                  | —
 * 2   | sendSkillAction    | "Kết bạn" = gửi lời mời (RAM, không lưu SQL)   | int playerId
 * 3   | sendLearnSkill     | "Đồng ý" lời mời -> 2 bên thành bạn           | int VỊ TRÍ trong tab lời mời
 * 4   | sendUpgradeSkill   | "Từ chối" lời mời                              | int VỊ TRÍ trong tab lời mời
 * 5   | sendResetSkills    | "Xóa bạn bè"                                   | int VỊ TRÍ trong danh sách (không phải id)
 * </pre>
 */
public final class FriendHandler implements PacketHandler {

   private final SocialService social;

   public FriendHandler(SocialService social) {
      this.social = social;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> social.sendFriends(session);
         case 1 -> social.sendFriendInvites(session);
         case 2 -> social.addFriend(session, message.readInt());
         case 3 -> social.acceptFriendInvite(session, message.readInt());
         case 4 -> social.declineFriendInvite(session, message.readInt());
         case 5 -> social.removeFriend(session, message.readInt());
         default -> {
         }
      }
   }
}
