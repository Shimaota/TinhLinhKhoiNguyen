package com.tlnt.ui.window.chat.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.chat.ChatMessage;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.GameTextField;
import com.tlnt.ui.widget.ShapeButton;

public final class ChatInputBar extends BaseTable {
   public CompositeSprite backgroundSprite;
   public NinePatchAsset inputBorder;
   public ChatMessage.ChatChannel chatChannel;
   public ChatInputBar.ChatTextField textField;
   public ChatInputBar.SendButton sendButton;
   public ChatInputBar.SubmitListener submitListener;

   public ChatInputBar(RenderContext renderContext, ChatMessage.ChatChannel chatChannel, String hintText, ChatInputBar.SubmitListener submitListener) {
      super(renderContext);
      String startPath = "ui/22";
      String middlePath = "ui/23";
      String endPath = "ui/24";
      this.backgroundSprite = CompositeSprite.obtain(startPath, middlePath, endPath, false);
      startPath = "ui/21";
      this.inputBorder = NinePatchAsset.obtain(startPath, 20);
      this.chatChannel = chatChannel;
      this.submitListener = submitListener;
      this.addActor(this.textField = new ChatInputBar.ChatTextField("", super.renderContext));
      this.addActor(this.sendButton = new ChatInputBar.SendButton(super.renderContext));
      this.textField.setMessageText(hintText);
      this.textField.setTextFieldListener(new EnterKeySubmitListener(this));
      this.sendButton.addListener(new SendButtonClickListener(this));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawCompositeSprite(this.backgroundSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.drawNinePatch(this.inputBorder, this.getX() + 17.0F, this.getY() + 15.0F, 1152.0F, 53.0F, Anchor.BOTTOM_LEFT);
      super.renderContext.drawTextureSized(this.chatChannel.icon, this.getX() + 25.0F, this.getY() + 24.0F, 35.0F, 35.0F, Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

   public void submitMessage() {
      String text;
      if (!(text = this.textField.getText()).isEmpty()) {
         if (this.submitListener != null) {
            if (this.submitListener.onSubmit(text)) {
               this.textField.setText("");
            }
         }
      }
   }

   public class ChatTextField extends GameTextField {
      public ChatTextField(String text, RenderContext renderContext) {
         super(text, renderContext, FontFactory.scaleFont(FontManager.TEXT_FIELD_FONT, 22.0F));
         this.setSize(1085.0F, 53.0F);
         this.setPosition(70.0F, 15.0F);
      }

      @Override
      public final void drawBackground(float x, float y, float width, float height, boolean focused, boolean disabled, float parentAlpha) {
      }

      @Override
      public final void drawText(Batch batch, BitmapFont font, float x, float y) {
         super.drawText(batch, font, x, y - 5.0F);
      }
   }

   public class SendButton extends ShapeButton {
      public TextureAsset iconTexture;

      public SendButton(RenderContext renderContext) {
         super(renderContext);
         this.iconTexture = TextureAsset.obtain("ui/20");
         this.setSize(50.0F, 52.0F);
         this.setPosition(1179.0F, 15.0F);
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

   public interface SubmitListener {
      boolean onSubmit(String message);
   }
}
