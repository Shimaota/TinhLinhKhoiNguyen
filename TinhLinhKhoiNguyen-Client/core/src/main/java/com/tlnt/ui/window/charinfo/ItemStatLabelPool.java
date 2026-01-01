package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;

public final class ItemStatLabelPool extends Pool<TextraLabel> {
   public com.tlnt.ui.window.charinfo.EquipmentPaperdollPanel.ItemDescriptionPanel itemStatDetailsPanel;
   public ItemStatLabelPool(EquipmentPaperdollPanel.ItemDescriptionPanel panel) {
      super();
      this.itemStatDetailsPanel = panel;
   }

   @Override
   public TextraLabel newObject() {
      TextraLabel label = new TextraLabel("", com.tlnt.engine.font.FontFactory.getCachedTextraFont(com.tlnt.core.audio.FontManager.PLAIN_FONT, 16));
      label.setAlignment(10);
      label.setWrap(true);
      return label;
   }
}
