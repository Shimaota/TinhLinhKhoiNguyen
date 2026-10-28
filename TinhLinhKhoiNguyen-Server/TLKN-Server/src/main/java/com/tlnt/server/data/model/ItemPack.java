package com.tlnt.server.data.model;

import java.util.List;

/**
 * Nội dung file DataGame {@code item} (xem {@code GameDataLoader.loadItemData} phía client).
 */
public record ItemPack(
      int version,
      List<ItemType> itemTypes,
      List<ItemTemplate> itemTemplates,
      List<StatType> statTypes,
      List<OptionInfo> options) {

   public record ItemType(int id, String name, int equipSlot, boolean equippable) {
   }

   public record ItemTemplate(int id, int typeId, int classId, String name, String description,
         String iconPath, String previewImagePath, int requiredLevel, String emblemPath) {
   }

   public record StatType(int id, String formatPattern) {
   }

   public record OptionInfo(int id, String name, String icon, String description) {
   }
}
