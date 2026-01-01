package com.tlnt.model.item;

import com.tlnt.engine.render.TextureAsset;

public final class Item {
   public int slotIndex;
   public ItemTemplate template;
   public Item.StarGrade starGrade;
   public byte starCount;
   public int quantity;
   public boolean locked;
   public long createdTime;
   public long expireTime;
   public StatLine[] statLines;
   public Item[] soulCharms;
   public ElementStone[] elementStones;

   public Item() {
   }

   public Item(Item source) {
      this(source, source.quantity);
   }

   public Item(Item source, int quantity) {
      this.slotIndex = source.slotIndex;
      this.template = source.template;
      this.starGrade = source.starGrade;
      this.quantity = quantity;
      this.locked = source.locked;
      this.createdTime = source.createdTime;
      this.expireTime = source.expireTime;
      this.statLines = source.statLines;
   }

   public Item(int slotIndex, int templateId, Item.StarGrade starGrade, int quantity, boolean locked, long createdTime, long expireTime) {
      this.slotIndex = slotIndex;
      this.template = ItemTemplate.getById(templateId);
      this.starGrade = starGrade;
      this.quantity = quantity;
      this.locked = locked;
      this.createdTime = createdTime;
      this.expireTime = expireTime;
   }

   public final void clear() {
      this.template = null;
      this.starGrade = null;
      if (this.statLines != null) {
         StatLine[] lines = this.statLines;
         int lineCount = this.statLines.length;

         for (int i = 0; i < lineCount; i++) {
            StatLine line;
            (line = lines[i]).clear();
         }

         this.statLines = null;
      }

      this.soulCharms = null;
      this.elementStones = null;
   }

   public static enum StarGrade {
      NONE("", TextureAsset.obtain("")),
      STAR_TIER_1("(Nhật ấn)", TextureAsset.obtain("ui/141")),
      STAR_TIER_2("(Nguyệt ấn)", TextureAsset.obtain("ui/142"));

      public String displayName;
      public TextureAsset starIcon;

      public static Item.StarGrade[] getValues() {
         return (Item.StarGrade[])Item.StarGrade.values().clone();
      }

      StarGrade(String displayName, TextureAsset starIcon) {
         this.displayName = displayName;
         this.starIcon = starIcon;
      }

   }
}
