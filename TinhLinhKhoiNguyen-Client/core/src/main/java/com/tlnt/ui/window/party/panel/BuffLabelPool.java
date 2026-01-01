package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;

public final class BuffLabelPool extends Pool<TextraLabel> {
   public com.tlnt.ui.window.party.panel.PartyBuffInfoPanel partyBuffInfoPanel;
   public BuffLabelPool(PartyBuffInfoPanel partyBuffInfoPanel) {
      super();
      this.partyBuffInfoPanel = partyBuffInfoPanel;
   }

   @Override
   public TextraLabel newObject() {
      TextraLabel label = new TextraLabel("", com.tlnt.engine.font.FontFactory.createTextraFontWithLineHeight(com.tlnt.core.audio.FontManager.PLAIN_FONT, 22, 3.0F));
      label.setAlignment(10);
      return label;
   }
}
