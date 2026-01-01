package com.tlnt.ui.window.chat.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.github.tommyettinger.textra.TypingLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.chat.ChatMessage;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ShapeButton;

public final class ChatMessageRow extends BaseTable {
   public ChatMessageRow.ChannelIconButton channelIconButton;
   public TypingLabel messageLabel;

   public ChatMessageRow(RenderContext renderContext) {
      super(renderContext);
      this.top().left();
      this.channelIconButton = new ChatMessageRow.ChannelIconButton(super.renderContext);
      this.add(this.channelIconButton).size(35.0F).space(10.0F).top();
      this.messageLabel = new TypingLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 2.5F));
      this.messageLabel.setAlignment(10);
      this.messageLabel.setWrap(true);
      this.messageLabel.skipToTheEnd();
      this.add(this.messageLabel).top().space(10.0F).grow().padTop(-3.0F);
      this.messageLabel.setTypingListener(new ChatMessageRowTypingAdapter(this));
   }

   public final ChatMessageRow setMessage(ChatMessage message) {
      this.channelIconButton.setIcon(message.channel.icon);
      String colorTag = "";
      switch (message.channel) {
         case NEARBY:
         case PRIVATE:
         case PARTY:
         case TRIBE:
            colorTag = "[#FFFFFF]";
            break;
         case SYSTEM:
            colorTag = "[#60FFF2]";
            break;
         case WORLD:
            colorTag = "[#FFF600]";
      }

      this.messageLabel.setText(colorTag + message.senderName + ": " + message.text);
      Gdx.app.postRunnable(new InvalidateHierarchyRunnable(this));
      return this;
   }

   public class ChannelIconButton extends ShapeButton {
      public TextureAsset iconTexture;

      public ChannelIconButton(RenderContext renderContext) {
         super(renderContext);
         this.iconTexture = TextureAsset.obtain("ui/187");
      }

      public final ChatMessageRow.ChannelIconButton setIcon(TextureAsset icon) {
         this.iconTexture = icon;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawTextureSized(this.iconTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }
   }
}
