package com.tlnt.ui.window.action;

public final class ItemActionRegistry {
   public static UseItemAction2 PICKUP_EQUIP_ACTION = new UseItemAction2("Trang bị");
   public static EquipItemAction2 USE_ITEM_ACTION;
   public static SplitStackItemAction SPLIT_STACK_ACTION;
   public static UseItemAction LOCK_ACTION = new UseItemAction("Khóa");
   public static CraftItemAction CRAFT_ITEM_ACTION;
   public static BindShortcutAction BIND_SHORTCUT_ACTION = new BindShortcutAction("Phím tắt");
   public static RemoveItemAction SELL_ACTION = new RemoveItemAction("Bán");
   public static OpenItemTooltipAction CLOSE_TOOLTIP_ACTION = new OpenItemTooltipAction("Đóng");
   public static DropItemActionHandler DROP_ITEM_HANDLER;
   public static ReturnToCharacterSelectAction RETURN_TO_CHAR_SELECT_ACTION;
   public static BuyItemAction BUY_ACTION = new BuyItemAction("Mua");
   public static BuyMultipleItemAction BUY_MULTIPLE_ACTION;
   public static EquipItemAction LIST_FOR_SALE_ACTION = new EquipItemAction("Rao bán");
   public static AddMailAttachmentAction ADD_MAIL_ATTACHMENT_ACTION = new AddMailAttachmentAction("Chọn");
   public static DeselectItemAction DESELECT_ACTION = new DeselectItemAction("Bỏ chọn");
   public static UsePvpItemAction USE_ITEM_QUANTITY_ACTION;
   public static DonateToTribeActionHandler DONATE_TO_TRIBE_HANDLER;
   public static RemoveInlayAction DETACH_INLAY_ACTION = new RemoveInlayAction("Gỡ khảm");
   public static UseItemActionHandler USE_ITEM_HANDLER;
   public static UnequipItemAction UNEQUIP_ACTION;
   public static DestroyInventoryItemAction DESTROY_ITEM_ACTION = new DestroyInventoryItemAction("Trang bị");
   public static RemoveSkillAction REMOVE_SKILL_ACTION;
   public static CompositeItemAction COMPOSITE_ITEM_ACTION;
   public static UnequipPetAction UNEQUIP_PET_ACTION;
   public static EquipCostumeAction EQUIP_COSTUME_ACTION = new EquipCostumeAction("Trang bị");
   public static EquipBackwearAction EQUIP_BACKWEAR_ACTION = new EquipBackwearAction("Trang bị");
   public static StoreItemAction STORE_ITEM_ACTION;
   public static DropItemAction SELECT_CHAR_ACTION = new DropItemAction("Dùng");
   public static DiscardItemCancelAction DISCARD_CANCEL_ACTION;
   public static EquipPetAction EQUIP_PET_ACTION;
   public static DestroyItemAction DESTROY_ITEM_ACTION_2 = new DestroyItemAction("Tháo");
   public static GuildDepositGoldItemAction DEPOSIT_GUILD_GOLD_ACTION;
   public static GuildDepositAllGoldAction WITHDRAW_GUILD_GOLD_ACTION = new GuildDepositAllGoldAction(
      "Tháo"
   );
   public static EquipToSecondSlotAction EQUIP_SECOND_SLOT_ACTION;
   public static SendItemToStorageAction SEND_TO_STORAGE_ACTION;
   public static AutoPickupFilterActionConfig SET_AUTO_PICKUP_ACTION = new AutoPickupFilterActionConfig("Dùng");
   public static DetachSpiritualStoneAction DETACH_SPIRITUAL_STONE_ACTION = new DetachSpiritualStoneAction(
      "Tháo"
   );
   public static ForgeEquipActionHandler FORGE_EQUIP_HANDLER = new ForgeEquipActionHandler("Mua");
   public static BuyManyAction BUY_MANY_ACTION;
   public static AddTradeItemAction ADD_TRADE_ITEM_ACTION = new AddTradeItemAction("Chọn");
   public static RemoveTradeItemAction REMOVE_TRADE_ITEM_ACTION;
   public static UseItemAction3 USE_ACTION_3 = new UseItemAction3("Sử dụng");
   public static EquipToThirdSlotAction EQUIP_THIRD_SLOT_ACTION = new EquipToThirdSlotAction("Phân phát");

   static {
      USE_ITEM_ACTION = new EquipItemAction2("Tháo ra");
      SPLIT_STACK_ACTION = new SplitStackItemAction("Sử dụng");
      CRAFT_ITEM_ACTION = new CraftItemAction("Dùng nhiều");
      new DiscardItemAction("Vứt bỏ");
      DROP_ITEM_HANDLER = new DropItemActionHandler("Chọn");
      RETURN_TO_CHAR_SELECT_ACTION = new ReturnToCharacterSelectAction("Lấy ra");
      BUY_MULTIPLE_ACTION = new BuyMultipleItemAction("Gỡ xuống");
      USE_ITEM_QUANTITY_ACTION = new UsePvpItemAction("Chọn");
      DONATE_TO_TRIBE_HANDLER = new DonateToTribeActionHandler("Lấy ra");
      USE_ITEM_HANDLER = new UseItemActionHandler("Cất vào");
      UNEQUIP_ACTION = new UnequipItemAction("Lấy ra");
      REMOVE_SKILL_ACTION = new RemoveSkillAction("Lấy ra");
      COMPOSITE_ITEM_ACTION = new CompositeItemAction("Trang bị");
      UNEQUIP_PET_ACTION = new UnequipPetAction("Lấy ra");
      STORE_ITEM_ACTION = new StoreItemAction("Lấy ra");
      DISCARD_CANCEL_ACTION = new DiscardItemCancelAction("Tháo");
      EQUIP_PET_ACTION = new EquipPetAction("Dùng");
      DEPOSIT_GUILD_GOLD_ACTION = new GuildDepositGoldItemAction("Dùng");
      EQUIP_SECOND_SLOT_ACTION = new EquipToSecondSlotAction("Trang bị");
      SEND_TO_STORAGE_ACTION = new SendItemToStorageAction("Lấy ra");
      BUY_MANY_ACTION = new BuyManyAction("Mua nhiều");
      REMOVE_TRADE_ITEM_ACTION = new RemoveTradeItemAction("Lấy ra");
   }

}
