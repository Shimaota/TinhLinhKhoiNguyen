package com.tlnt.server.handler;

import com.tlnt.server.account.AccountService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 49 sub 0 C→S: xin captcha mới — client gửi khi mở form đăng ký ({@code RegisterDialog.onOpen}) và
 * khi bấm vào ảnh captcha. Phía client method này tên sai là {@code sendCreateCharacter()}.
 */
public final class CaptchaHandler implements PacketHandler {

   private final AccountService accountService;

   public CaptchaHandler(AccountService accountService) {
      this.accountService = accountService;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (message.readByte() == 0 && !session.isAuthenticated()) {
         accountService.requestCaptcha(session);
      }
   }
}
