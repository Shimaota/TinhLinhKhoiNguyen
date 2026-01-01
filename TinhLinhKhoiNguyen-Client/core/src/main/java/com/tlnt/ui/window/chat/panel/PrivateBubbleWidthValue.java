package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;

public final class PrivateBubbleWidthValue extends Value {
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel.MessageBubble messageBubble;
   public PrivateBubbleWidthValue(PrivateChatPanel.MessageBubble messageBubble) {
      super();
      this.messageBubble = messageBubble;
   }

   @Override
   public final float get(Actor context) {
      GlyphLayout layout;
      (layout = new GlyphLayout())
         .setText(
            FontFactory.scaleFont(FontManager.DEFAULT_FONT, 22.0F),
            this.messageBubble.messageLabel.storedText,
            Color.WHITE,
            480.0F,
            8,
            true
         );
      return Math.max(100.0F, layout.width);
   }
}
