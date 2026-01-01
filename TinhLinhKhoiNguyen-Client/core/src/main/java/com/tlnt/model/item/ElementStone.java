package com.tlnt.model.item;

public final class ElementStone {
   public ItemTemplate stoneTemplate;
   public byte enhanceLevel;
   public StatLine statLine;

   public ElementStone(int templateId, byte enhanceLevel) {
      this.stoneTemplate = ItemTemplate.getById(templateId);
      this.enhanceLevel = enhanceLevel;
   }
}
