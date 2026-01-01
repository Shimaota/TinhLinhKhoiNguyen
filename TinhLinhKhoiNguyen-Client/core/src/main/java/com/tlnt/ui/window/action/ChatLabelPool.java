package com.tlnt.ui.window.action;

import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;

public final class ChatLabelPool extends Pool<TextraLabel> {
   public com.tlnt.ui.window.action.ChatInputPopup chatInputPopup;
   public ChatLabelPool(ChatInputPopup popup) {
      super();
      this.chatInputPopup = popup;
   }

   @Override
   public TextraLabel newObject() {
      TextraLabel label = new TextraLabel("", com.tlnt.engine.font.FontFactory.createTextraFontWithLineHeight(com.tlnt.core.audio.FontManager.DEFAULT_FONT, 22, 2.5F));
      label.setAlignment(10);
      label.setWrap(true);
      return label;
   }
}
