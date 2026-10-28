package com.tlnt.server.net;

/**
 * Các gói S→C dùng ở nhiều nơi. Format mỗi gói phải khớp đúng nhánh đọc tương ứng trong
 * {@code MessageHandler.dispatchMessage} phía client (ghi rõ ở từng method).
 */
public final class Packets {

   private Packets() {
   }

   /** Opcode 14 sub 0: chữ nổi giữa màn hình + dòng "Hệ thống" trong chat. */
   public static Message notice(String text) {
      Message msg = new Message(Opcode.NOTICE);
      msg.writeByte(0);
      msg.writeString(text);
      return msg;
   }

   /**
    * Opcode 14 sub 1: chữ CHẠY ngang trên đầu màn hình ({@code messageTicker}) + dòng "Hệ thống" trong chat — thông báo
    * toàn server (boss thế giới xuất hiện / bị hạ).
    */
   public static Message ticker(String text) {
      Message msg = new Message(Opcode.NOTICE);
      msg.writeByte(1);
      msg.writeString(text);
      return msg;
   }

   /** Opcode 1 sub 0: hasSecondaryPassword, isSecondaryPasswordLocked (tài khoản đang bật bảo vệ). */
   public static Message accountState(boolean hasSecondaryPassword, boolean protectionEnabled) {
      Message msg = new Message(Opcode.ACCOUNT);
      msg.writeByte(0);
      msg.writeBoolean(hasSecondaryPassword);
      msg.writeBoolean(protectionEnabled);
      return msg;
   }

   /** Loại hộp hỏi opcode 32 ({@link #confirm} / {@link #invitation}); client gửi lại đúng số này khi bấm "Đồng ý". */
   public static final byte CONFIRM_FIRST_WEAPON = 1;
   /** Lời mời giao dịch (op 56 sub 0 của người mời). */
   public static final byte CONFIRM_TRADE = 2;
   /** Lời mời thách đấu (op 41 sub 0 của người mời). */
   public static final byte CONFIRM_DUEL = 3;
   /** Lời mời vào tổ đội (op 31 sub 2 của người mời). */
   public static final byte CONFIRM_PARTY = 4;
   /** Mở thêm 1 ô bộ sưu tập (op 48/50/64/66 sub 2). */
   public static final byte CONFIRM_EXPAND_COLLECTION = 5;
   /** Có người xin vào tổ đội (gửi trưởng nhóm khi tắt tự duyệt — op 31 sub 5 của người xin). */
   public static final byte CONFIRM_PARTY_REQUEST = 6;
   /** Dùng item hiệu ứng (loại 11) khi đang có hiệu ứng cùng nhóm — đè lên (op 27 sub 2). */
   public static final byte CONFIRM_USE_BUFF = 7;
   /** Bán vật phẩm (nút "Bán" ở menu item túi — client không hỏi) -> {@code ItemService.confirmSell}. */
   public static final byte CONFIRM_SELL = 8;
   /** Lời mời vào bộ lạc (op 37 sub 11 của Tù trưởng / Phó). */
   public static final byte CONFIRM_TRIBE_INVITE = 9;
   /** Tù trưởng xác nhận nhường chức (op 37 sub 13). */
   public static final byte CONFIRM_TRIBE_TRANSFER = 10;
   /** Giải tán bộ lạc: hỏi lần 1 (op 37 sub 6) rồi lần 2. */
   public static final byte CONFIRM_TRIBE_DISBAND = 11;
   public static final byte CONFIRM_TRIBE_DISBAND_FINAL = 12;
   /** Dùng biểu tượng từ Rương bộ lạc khi đang có biểu tượng khác còn hạn (thay thế). */
   public static final byte CONFIRM_TRIBE_EMBLEM = 13;
   /** Dùng Bùa tẩy điểm (item 166) — đặt lại tiềm năng + kỹ năng. */
   public static final byte CONFIRM_RESET_POINTS = 14;
   /** Nhiệm vụ hằng ngày: "Đổi NV" / "Nhận NV mới" tốn 1 Vé nhiệm vụ. */
   public static final byte CONFIRM_DAILY_SWAP = 15;
   public static final byte CONFIRM_DAILY_ADD = 16;
   /** "Lớn nhanh" heo con ở nông trại (tốn lam thạch — farm/PigService). */
   public static final byte CONFIRM_PIG_GROW = 17;
   /** "Luyện tập Offline" (Huy hiệu Tinh Linh — offline/OfflineTrainingService). */
   public static final byte CONFIRM_OFFLINE_TRAINING = 18;
   /** Bảng Boss thế giới (op 100): dịch chuyển tới khu có boss (lam thạch) / tự tìm đường tới map boss (vàng). */
   public static final byte CONFIRM_BOSS_TELEPORT = 19;
   public static final byte CONFIRM_BOSS_PATH = 20;

   /** Kênh hộp nhập chữ op 70 ({@link #inputDialog}); client gửi lại đúng số này kèm chữ đã nhập. */
   public static final byte INPUT_WORLD_CHAT = 1;
   public static final byte INPUT_RENAME = 2;

   /**
    * Opcode 70 ({@code handleShowDialog}): hộp nhập chữ (byte kênh, str lời nhắc, str chữ sẵn). "Đồng ý" -> client gửi
    * op 70 + byte kênh + str chữ ({@code ChatDialogCallback -> sendChannelChat}).
    */
   public static Message inputDialog(byte channel, String prompt, String initialText) {
      Message msg = new Message(Opcode.INPUT);
      msg.writeByte(channel);
      msg.writeString(prompt);
      msg.writeString(initialText);
      return msg;
   }

   /**
    * Opcode 32 nhánh lời mời: hộp lớn {@code InvitationConfirmPopup} (tiêu đề + nội dung + "Đồng ý / Từ chối").
    * Đồng ý -> client gửi opcode 32 + byte {@code type}; "Từ chối" không gửi gì (lời mời tự hết hạn).
    */
   public static Message invitation(byte type, String title, String text) {
      Message msg = new Message(Opcode.CONFIRM);
      msg.writeByte(type);
      msg.writeBoolean(true);
      msg.writeString(title);
      msg.writeString(text);
      return msg;
   }

   /**
    * Opcode 53 sub 2 (decompile C3873 m18132): mở cửa sổ Thiết lập ({@code SETTING_2_POPUP}) ở tab tự đánh — nơi có nút
    * "Luyện tập Offline" (Huy hiệu Tinh Linh).
    */
   public static Message openAutoSettings() {
      Message msg = new Message(Opcode.BOX);
      msg.writeByte(2);
      return msg;
   }

   /**
    * Opcode 32 ({@code MessageHandler} case 32, nhánh không phải lời mời): hộp nhỏ "Đồng ý / Từ chối"
    * ({@code GiftcodeEventConfirmDialog}). Đồng ý -> client gửi opcode 32 + byte {@code type}.
    */
   public static Message confirm(byte type, String text) {
      Message msg = new Message(Opcode.CONFIRM);
      msg.writeByte(type);
      msg.writeBoolean(false); // false = hộp xác nhận (true = lời mời, cần thêm tên người mời)
      msg.writeString("");
      msg.writeString(text);
      return msg;
   }

   /** Opcode 38: client mở {@code CREATE_PLAYER_PANEL}. */
   public static Message openCreateCharacter() {
      return new Message(Opcode.CREATE_CHARACTER);
   }

   /**
    * Opcode 59 ({@code ServerPacketHandler.handleDecryptKeys}): byte len + khoá ảnh
    * ({@code GameState.textureDecryptKey}), rồi byte len + khoá atlas ({@code GameState.atlasDecryptKey}).
    */
   public static Message decryptKeys(byte[] textureKey, byte[] atlasKey) {
      Message msg = new Message(Opcode.DECRYPT_KEYS);
      msg.writeByte(textureKey.length);
      for (byte b : textureKey) {
         msg.writeByte(b);
      }
      msg.writeByte(atlasKey.length);
      for (byte b : atlasKey) {
         msg.writeByte(b);
      }
      return msg;
   }

   /** Opcode 49 sub 0: ảnh captcha, client decode bằng {@code new Pixmap(bytes)} (PNG). */
   public static Message captcha(byte[] png) {
      Message msg = new Message(Opcode.CAPTCHA);
      msg.writeByte(0);
      msg.writeBytes(png);
      return msg;
   }
}
