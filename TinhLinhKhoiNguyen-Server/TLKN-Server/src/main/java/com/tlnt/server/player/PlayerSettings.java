package com.tlnt.server.player;

/**
 * Cài đặt tự động / phím tắt đồng bộ qua opcode 46 (client gửi {@code sendGameSettings}, server trả lại
 * theo {@code MessageHandler case 46}). Lưu JSON ở cột {@code settings}. Tạo nhân vật = {@link #defaults()};
 * mỗi lần người chơi đổi phím/bật tắt tự động client gửi lại cả bộ -> {@code SettingsHandler} thay thế.
 */
public final class PlayerSettings {

   public static final int AUTO_SKILL_SLOTS = 6;
   public static final int KEYBINDINGS = 16;
   /** 5 cờ "Ẩn" trong {@link #inventoryFlags} (checkbox ở panel Trang phục / Danh hiệu / Mặt nạ / Thú cưng / Đeo lưng). */
   public static final int INVENTORY_FLAGS = 5;
   public static final int HIDE_COSTUME = 0;
   public static final int HIDE_TITLE = 1;
   public static final int HIDE_MASK = 2;
   public static final int HIDE_PET = 3;
   /** Cờ thứ 5 — client decompile đọc 5 cờ ở op 46 (bản client cũ chỉ 4: thiếu cờ này). */
   public static final int HIDE_BACK = 4;

   /**
    * Phím mặc định khi tạo nhân vật (mã phím libGDX {@code Input.Keys}). Vị trí trong mảng = dòng trên bảng phím tắt
    * (KeyBindingPanel), 2 cột trái/phải xen kẽ.
    */
   private static final int[] DEFAULT_KEYBINDINGS = {
         19, // 0  Di chuyển lên          Up
         21, // 1  Di chuyển sang trái    Left
         20, // 2  Di chuyển xuống        Down
         22, // 3  Di chuyển sang phải    Right
         8,  // 4  Ô đánh số 1            1
         66, // 5  Đánh cơ bản            Enter
         9,  // 6  Ô đánh số 2            2
         61, // 7  Đổi mục tiêu           Tab
         10, // 8  Ô đánh số 3            3
         48, // 9  Bật/Tắt Tự động đánh   T
         11, // 10 Ô đánh số 4            4
         46, // 11 Chat                   R
         12, // 12 Ô đánh số 5            5
         42, // 13 Vật phẩm hồi HP        N
         13, // 14 Ô đánh số 6            6
         41, // 15 Vật phẩm hồi MP        M
   };

   public boolean[] autoSkillSlotEnabled = new boolean[AUTO_SKILL_SLOTS];
   public boolean autoIgnorePlayersAndBosses;
   public boolean autoAvoidEliteMobs;
   public boolean autoRevive;
   public boolean autoPickupItems;
   public boolean autoUseStaminaItems;
   public boolean autoUseHpItems;
   public boolean autoUseMpItems;
   public boolean autoHarvestApples;
   public byte autoHpThresholdPercent;
   public byte autoMpThresholdPercent;
   /** "Phạm vi đánh" (decompile C3418.f12249): px quanh chỗ bật tự đánh / bắt đầu treo offline, 0 = Không giới hạn. */
   public int autoAttackRange;
   /** Phạm vi lớn nhất client có (ô chọn 500 … 2500). */
   public static final int MAX_ATTACK_RANGE = 2_500;
   /**
    * 5 cờ "Ẩn ngoại trang / danh hiệu / mặt nạ / thú cưng / đeo lưng" (index {@link #HIDE_COSTUME}...). Client KHÔNG gửi
    * trong opcode 46 mà bật/tắt qua opcode 48/50/64/66/78 sub 0 ({@code CollectionHandler}); server gửi xuống trong
    * opcode 46. JSON cũ chỉ có 4 cờ -> {@link #copy()} / {@link #normalize()} nới ra 5.
    */
   public boolean[] inventoryFlags = new boolean[INVENTORY_FLAGS];
   /** Keycode libGDX cho 16 phím tắt (thứ tự xem {@link #DEFAULT_KEYBINDINGS}). */
   public int[] keybindings;
   public boolean hideOtherPlayers;

   /** Bản sao sâu (mảng mới) — sửa bản sao rồi thay {@code PlayerData.settings} để luồng lưu luôn thấy 1 bộ trọn vẹn. */
   public PlayerSettings copy() {
      PlayerSettings c = new PlayerSettings();
      c.autoSkillSlotEnabled = autoSkillSlotEnabled.clone();
      c.autoIgnorePlayersAndBosses = autoIgnorePlayersAndBosses;
      c.autoAvoidEliteMobs = autoAvoidEliteMobs;
      c.autoRevive = autoRevive;
      c.autoPickupItems = autoPickupItems;
      c.autoUseStaminaItems = autoUseStaminaItems;
      c.autoUseHpItems = autoUseHpItems;
      c.autoUseMpItems = autoUseMpItems;
      c.autoHarvestApples = autoHarvestApples;
      c.autoHpThresholdPercent = autoHpThresholdPercent;
      c.autoMpThresholdPercent = autoMpThresholdPercent;
      c.autoAttackRange = autoAttackRange;
      c.inventoryFlags = inventoryFlags.length == INVENTORY_FLAGS ? inventoryFlags.clone()
            : java.util.Arrays.copyOf(inventoryFlags, INVENTORY_FLAGS);
      c.keybindings = keybindings != null ? keybindings.clone() : null;
      c.hideOtherPlayers = hideOtherPlayers;
      return c;
   }

   /** Sửa JSON cũ (mảng cờ ẩn 4 phần tử) sau khi đọc DB. */
   public PlayerSettings normalize() {
      if (inventoryFlags == null) {
         inventoryFlags = new boolean[INVENTORY_FLAGS];
      } else if (inventoryFlags.length != INVENTORY_FLAGS) {
         inventoryFlags = java.util.Arrays.copyOf(inventoryFlags, INVENTORY_FLAGS);
      }
      if (autoSkillSlotEnabled == null || autoSkillSlotEnabled.length != AUTO_SKILL_SLOTS) {
         autoSkillSlotEnabled = autoSkillSlotEnabled == null ? new boolean[AUTO_SKILL_SLOTS]
               : java.util.Arrays.copyOf(autoSkillSlotEnabled, AUTO_SKILL_SLOTS);
      }
      return this;
   }

   /** Cài đặt của nhân vật mới: phím tắt mặc định, mọi tuỳ chọn tự động tắt. */
   public static PlayerSettings defaults() {
      PlayerSettings settings = new PlayerSettings();
      settings.keybindings = DEFAULT_KEYBINDINGS.clone();
      return settings;
   }
}
