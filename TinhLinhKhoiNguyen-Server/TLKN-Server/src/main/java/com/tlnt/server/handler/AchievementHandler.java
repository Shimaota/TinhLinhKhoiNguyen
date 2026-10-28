package com.tlnt.server.handler;

import com.tlnt.server.achievement.AchievementService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 62 C→S: thành tựu. Sub 0 ({@code sendMountEquip} — tên sai; nút "Nhận" {@code AchievementClaimListener} ở tab
 * "Thành tựu"): int id thành tựu. Danh sách server tự gửi lúc vào game (mở tab không gửi gì). Logic:
 * {@link AchievementService}.
 */
public final class AchievementHandler implements PacketHandler {

   private final AchievementService achievements;

   public AchievementHandler(AchievementService achievements) {
      this.achievements = achievements;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() != null && message.readByte() == 0) {
         achievements.claim(session, message.readInt());
      }
   }
}
