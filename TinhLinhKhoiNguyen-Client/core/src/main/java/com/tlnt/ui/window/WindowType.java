package com.tlnt.ui.window;

public enum WindowType {
   DEFAULT,
   LOGIN_PANEL,
   REGIS_PANEL,
   DOWNLOAD_RESOURCES_PANEL,
   CREATE_PLAYER_PANEL,
   DISCONNECTED_PANEL,
   SERVER_LIST_PANEL,
   CLEAR_DATA_PANEL,
   CONFIRM_POPUP(false, true),
   SMALL_CONFIRM_POPUP(false, false),
   MENU_NPC_POPUP,
   MENU_NPC_POPUP_V2,
   ZONE_POPUP,
   VIP_POPUP,
   GENERIC_POPUP,
   WELFARE,
   TEAM_POPUP,
   CHAT_POPUP,
   SETTING_POPUP,
   SETTING_2_POPUP,
   RANK_POPUP,
   /** Ranking sent separately by the server (op 75). */
   OTHER_RANK_POPUP,
   MAIL_POPUP,
   COMBINE_POPUP,
   TRANSACTION_POPUP,
   /** "Recharge" (op 72). */
   RECHARGE_POPUP,
   /** "World Boss" (op 100 — NEW). */
   WORLD_BOSS_POPUP,
   SHORTCUT_SKILL(false, true),
   INFO_ITEM_POPUP(false, false),
   INPUT_POPUP(false, false),
   EXHAUSTED_POPUP(true, false),
   MENU_POPUP(false, false),
   SHOP_CONSIGNMENT_POPUP,
   CLAN_POPUP,
   BOX_POPUP,
   QUEST_POPUP,
   MINI_MAP_POPUP,
   SHOP_POPUP,
   EVENT_POPUP,
   MESSAGE_POPUP,
   HANDBOOK_POPUP,
   COLLECTION_POPUP,
   CHOOSE_WEAPON_POPUP(true, false),
   INFO_PLAYER_POPUP(false, true),
   INFO_OPTION_INFO_POPUP(false, false),
   REWARD_ITEMS_POPUP(false, false);

   public boolean closeable;
   public boolean backEnabled;

   WindowType(boolean closeable, boolean backEnabled) {
      this.closeable = closeable;
      this.backEnabled = backEnabled;
   }

   WindowType() {
      this(true, true);
   }

}
