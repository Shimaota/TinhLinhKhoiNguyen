package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.Settings;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.chat.ChatMessage;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.GameTextField;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.WindowType;

public final class ChatInputPopup extends Popup {
   public final Pool<TextraLabel> labelPool;
   public NinePatchAsset backgroundPatch;
   public ChatInputPopup.InputBar inputBar;
   public ChatInputPopup.SendButton sendButton;

   public ChatInputPopup(RenderContext renderContext) {
      super(renderContext, WindowType.CHAT_POPUP, ((float)WorldConfig.VIEWPORT_WIDTH / 2.0F) - 50.0F, ((float)WorldConfig.VIEWPORT_HEIGHT / 4.0F), 1, 714.0F, 288.0F);
      this.labelPool = new ChatLabelPool(this);
      this.backgroundPatch = NinePatchAsset.obtain("ui/13", 20);
      this.addActor(new ChatInputPopup.ChatHistoryPanel(super.renderContext));
      this.addActor(this.inputBar = new ChatInputPopup.InputBar(super.renderContext));
      this.addActor(this.sendButton = new ChatInputPopup.SendButton(super.renderContext));
      this.sendButton.addListener(new OpenInventoryClickListener(this));
   }

   @Override
   public final void drawBackground() {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
   }

   @Override
   public final void onUpdate(float delta) {
      if (this.getStage() != null && this.getStage().getKeyboardFocus() != this.inputBar.textField) {
         this.getStage().setKeyboardFocus(this.inputBar.textField);
      }
   }

   @Override
   public final boolean isModal() {
      return false;
   }

   @Override
   public final void onRender(float delta) {
      if (GameMain.instance.getLoginScreen().debugScrollPane.isHudActive()) {
         InputHandler.getInstance();
         if (InputHandler.isKeyJustPressed(Settings.keybindings[11]) && !this.isVisible()) {
            this.show();
         }
      }
   }

   public class ChatHistoryPanel extends GamePanel {
      public ScrollPane scrollPane;
      public Table messageTable;

      public ChatHistoryPanel(RenderContext renderContext) {
         super(renderContext);
         this.setSize(625.0F, 183.0F);
         this.setPosition(15.0F, 93.0F);
         this.add(this.scrollPane = ScrollPaneFactory.createScrollPane(this.messageTable = new Table().top().left().padBottom(10.0F))).grow();
      }

      @Override
      public final void refresh() {
         if (GameState.chatManager.hudDirty) {
            GameState.chatManager.hudDirty = false;
            Array.ArrayIterator iter = this.messageTable.getChildren().iterator();

            while (iter.hasNext()) {
               Actor child;
               if ((child = (Actor)iter.next()) instanceof TextraLabel) {
                  ChatInputPopup.this.labelPool.free((TextraLabel)child);
               }
            }

            this.messageTable.clear();

            for (ChatMessage message : GameState.chatManager.hudMessages) {
               TextraLabel label = ChatInputPopup.this.labelPool.obtain();
               switch (message.channel) {
                  case NEARBY:
                     label.setText("[#FFFFFF]" + message.channel.label + " " + message.senderName + ": " + message.text);
                     break;
                  case WORLD:
                     label.setText("[#FFF600]" + message.channel.label + " " + message.senderName + ": " + message.text);
                     break;
                  case SYSTEM:
                     label.setText("[#60FFF2]" + message.channel.label + " " + message.senderName + ": " + message.text);
               }

               label.skipToTheEnd();
               this.messageTable.add(label).growX().minHeight(30.0F).space(5.0F).row();
            }

            this.scrollPane.scrollTo(0.0F, 0.0F, 0.0F, 0.0F);
         }
      }

      @Override
      public final void onUpdate(float delta) {
      }
   }

   public class InputBar extends GamePanel {
      public CompositeSprite background;
      public ChatInputPopup.TextField textField;
      public ChatInputPopup.ToggleButton toggleButton;

      public InputBar(RenderContext renderContext) {
         super(renderContext);
         this.background = CompositeSprite.obtain(
            "ui/22", "ui/23", "ui/24", false
         );
         this.setSize(701.0F, 83.0F);
         this.setPosition(7.0F, 8.0F);
         this.addActor(this.textField = ChatInputPopup.this.new TextField("", super.renderContext));
         this.addActor(this.toggleButton = ChatInputPopup.this.new ToggleButton(super.renderContext));
         this.textField.addListener(new ChatKeyboardInputListener(this));
         this.toggleButton.addListener(new ChatInputSubmitListener(this));
      }

      public void sendChatMessage() {
         String text = this.textField.getText();
         this.textField.setText("");
         if (!text.trim().isEmpty()) {
            ServerPacketSender.getInstance().sendChat(text);
            ChatInputPopup.this.hide();
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawCompositeSprite(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }

   public class SendButton extends ShapeButton {

      public SendButton(RenderContext renderContext) {
         super(renderContext);
         this.setSize(43.0F, 31.0F);
         this.setPosition(653.0F, 249.0F);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain("ui/42"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
            );
         super.renderContext.resetBatchColor();
      }
   }

   public class TextField extends GameTextField {
      public NinePatchAsset backgroundPatch;

      public TextField(String text, RenderContext renderContext) {
         super(text, renderContext, FontFactory.scaleFont(FontManager.TEXT_FIELD_FONT, 22.0F));
         this.backgroundPatch = NinePatchAsset.obtain("ui/21", 20);
         this.setSize(606.0F, 53.0F);
         this.setPosition(17.0F, 15.0F);
      }

      @Override
      public final void drawBackground(float x, float y, float width, float height, boolean focused, boolean disabled, float parentAlpha) {
         super.renderContext.drawNinePatch(this.backgroundPatch, x, y, width, height, Anchor.BOTTOM_LEFT);
      }

      @Override
      public final void drawText(Batch batch, BitmapFont font, float x, float y) {
         super.drawText(batch, font, x, y - 5.0F);
      }
   }

   public class ToggleButton extends ShapeButton {

      public ToggleButton(RenderContext renderContext) {
         super(renderContext);
         this.setSize(50.0F, 52.0F);
         this.setPosition(633.0F, 15.0F);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain("ui/20"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
            );
         super.renderContext.resetBatchColor();
      }
   }
}
