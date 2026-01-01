package com.tlnt.ui.window.iteminfo.effect;

import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;

public final class SingleEffectLabelPool extends Pool<TextraLabel> {
   public com.tlnt.ui.window.iteminfo.effect.ItemSingleEffectRow row;
   public SingleEffectLabelPool(ItemSingleEffectRow row) {
      super();
      this.row = row;
   }

   @Override
   public TextraLabel newObject() {
      TextraLabel label = new TextraLabel("", com.tlnt.engine.font.FontFactory.createTextraFontWithLineHeight(com.tlnt.core.audio.FontManager.PLAIN_FONT, 24, 3.0F));
      label.setWrap(true);
      label.setAlignment(10);
      return label;
   }
}
