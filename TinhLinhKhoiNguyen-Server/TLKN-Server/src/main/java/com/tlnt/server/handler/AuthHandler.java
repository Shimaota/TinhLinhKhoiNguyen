package com.tlnt.server.handler;

import com.tlnt.server.account.AccountService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

/**
 * Opcode 1 C→S. Sub-opcode theo chức năng THẬT (tên method phía client bị decompile đặt sai):
 *
 * <pre>
 * sub | client gọi                                  | chức năng           | payload
 * 0   | LoginDialog -> sendLogin                     | đăng nhập           | username, password
 * 1   | CreateSecondaryPasswordPanel -> sendRegister | tạo MK cấp 2        | mật khẩu hiện tại, MK2, nhập lại MK2
 * 2   | ChangeSecondaryPasswordPanel -> sendGuestLogin | đổi MK cấp 2      | MK2 hiện tại, MK2 mới, nhập lại
 * 3   | RegisterDialog -> sendSocialLogin            | đăng ký tài khoản   | username, password, confirm, email, captcha
 * 4   | LockAccountListener -> sendForgotPassword    | bật bảo vệ          | (không có)
 * 5   | SecondaryPasswordInputCallback -> sendVerifyCode | tắt bảo vệ      | MK2
 * 6   | DeleteSecondaryPasswordListener -> sendDeleteSecondaryPassword | xoá MK cấp 2 | MK2 (decompile C3760: không payload -> từ chối)
 * </pre>
 *
 * Sub 0 và 3 dùng được khi chưa đăng nhập; các sub còn lại {@link AccountService} tự bỏ qua nếu chưa đăng nhập.
 */
public final class AuthHandler implements PacketHandler {

   private static final byte SUB_LOGIN = 0;
   private static final byte SUB_CREATE_SECONDARY_PASSWORD = 1;
   private static final byte SUB_CHANGE_SECONDARY_PASSWORD = 2;
   private static final byte SUB_REGISTER = 3;
   private static final byte SUB_ENABLE_PROTECTION = 4;
   private static final byte SUB_DISABLE_PROTECTION = 5;
   private static final byte SUB_DELETE_SECONDARY_PASSWORD = 6;

   private final AccountService accountService;

   public AuthHandler(AccountService accountService) {
      this.accountService = accountService;
   }

   @Override
   public void handle(PlayerSession session, Message message) throws Exception {
      byte sub = message.readByte();
      switch (sub) {
         case SUB_LOGIN -> accountService.login(session, message.readString(), message.readString());
         case SUB_REGISTER -> accountService.register(session, message.readString(), message.readString(),
               message.readString(), message.readString(), message.readString());
         case SUB_CREATE_SECONDARY_PASSWORD -> accountService.createSecondaryPassword(session, message.readString(),
               message.readString(), message.readString());
         case SUB_CHANGE_SECONDARY_PASSWORD -> accountService.changeSecondaryPassword(session, message.readString(),
               message.readString(), message.readString());
         case SUB_ENABLE_PROTECTION -> accountService.enableProtection(session);
         case SUB_DISABLE_PROTECTION -> accountService.disableProtection(session, message.readString());
         case SUB_DELETE_SECONDARY_PASSWORD -> accountService.deleteSecondaryPassword(session,
               message.remaining() > 0 ? message.readString() : null);
         default -> Log.debug("Session #%d gui opcode 1 sub %d khong ho tro", session.getId(), sub);
      }
   }
}
