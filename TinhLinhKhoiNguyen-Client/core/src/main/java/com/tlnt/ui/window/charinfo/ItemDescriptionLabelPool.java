package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;

public final class ItemDescriptionLabelPool extends Pool<TextraLabel> {
   public com.tlnt.ui.window.charinfo.InspectEquipmentPanel.ItemDescriptionPanel descriptionPanel;
   public ItemDescriptionLabelPool(InspectEquipmentPanel.ItemDescriptionPanel panel) {
      super();
      this.descriptionPanel = panel;
   }

   @Override
   public TextraLabel newObject() {
      TextraLabel label = new TextraLabel("", com.tlnt.engine.font.FontFactory.getCachedTextraFont(com.tlnt.core.audio.FontManager.PLAIN_FONT, 16));
      label.setAlignment(10);
      label.setWrap(true);
      return label;
   }
}
