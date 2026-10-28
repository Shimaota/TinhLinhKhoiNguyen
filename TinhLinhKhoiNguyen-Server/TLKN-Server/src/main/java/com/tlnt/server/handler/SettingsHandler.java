package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.QuestService;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

/**
 * Opcode 46 C→S ({@code ServerPacketSender.sendGameSettings}): client gửi LẠI CẢ BỘ cài đặt mỗi khi người chơi
 * đổi phím tắt (ShortcutAssignInputHandler) hoặc bật/tắt 1 tuỳ chọn tự động / ẩn người khác.
 * Thứ tự: 6 ô tự đánh, 8 cờ tự động, ngưỡng HP, ngưỡng MP (byte), 16 phím (int), ẩn người chơi khác.
 * Không có 4 cờ túi đồ (chỉ server gửi xuống) -> giữ nguyên giá trị cũ.
 *
 * Thay thế cả object {@link PlayerData#settings} (không sửa từng field) để lượt lưu trên luồng khác luôn chụp
 * được 1 bộ cài đặt trọn vẹn. Lưu SQL khi thoát / tắt server như dữ liệu nhân vật khác.
 */
public final class SettingsHandler implements PacketHandler {

   /** Mã phím libGDX nằm trong 0..255 ({@code Input.Keys.MAX_KEYCODE}). */
   private static final int MAX_KEYCODE = 255;

   @Override
   public void handle(PlayerSession session, Message message) {
      PlayerData player = session.getPlayer();
      if (player == null) {
         return;
      }

      PlayerSettings next = new PlayerSettings();
      for (int i = 0; i < PlayerSettings.AUTO_SKILL_SLOTS; i++) {
         next.autoSkillSlotEnabled[i] = message.readBoolean();
      }
      next.autoIgnorePlayersAndBosses = message.readBoolean();
      next.autoAvoidEliteMobs = message.readBoolean();
      next.autoRevive = message.readBoolean();
      next.autoPickupItems = message.readBoolean();
      next.autoUseStaminaItems = message.readBoolean();
      next.autoUseHpItems = message.readBoolean();
      next.autoUseMpItems = message.readBoolean();
      next.autoHarvestApples = message.readBoolean();
      next.autoHpThresholdPercent = clampPercent(message.readByte());
      next.autoMpThresholdPercent = clampPercent(message.readByte());
      next.keybindings = new int[PlayerSettings.KEYBINDINGS];
      for (int i = 0; i < PlayerSettings.KEYBINDINGS; i++) {
         int keycode = message.readInt();
         if (keycode < 0 || keycode > MAX_KEYCODE) {
            Log.debug("Nhan vat #%d gui phim tat khong hop le (vi tri %d = %d), bo qua", player.getId(), i, keycode);
            return;
         }
         next.keybindings[i] = keycode;
      }
      next.hideOtherPlayers = message.readBoolean();
      // "Phạm vi đánh": client libGDX nối int CUỐI gói (decompile đặt sau 2 byte %); client cũ không gửi -> không giới hạn
      next.autoAttackRange = message.remaining() >= 4
            ? Math.max(0, Math.min(PlayerSettings.MAX_ATTACK_RANGE, message.readInt())) : 0;

      PlayerSettings previous = player.settings;
      if (previous != null) {
         next.inventoryFlags = previous.inventoryFlags.clone();
      }
      player.settings = next;
      if (QuestService.tutorialSettings(next)) {
         QuestEvents.fire(session, StepKind.AUTO_SETTINGS, 0);
      }
   }

   private static byte clampPercent(byte value) {
      return (byte) Math.max(0, Math.min(100, value));
   }
}
