package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.WindowType;

public class InvitationConfirmPopup extends Popup {
   public InvitationConfirmPopup.TitlePanel titlePanel;
   public TextraLabel messageLabel;
   public Table buttonTable;
   public InvitationConfirmPopup.StyledButton acceptButton;
   public InvitationConfirmPopup.StyledButton refuseButton;
   public byte invitationType;

   public InvitationConfirmPopup(RenderContext renderContext) {
      super(renderContext, WindowType.CONFIRM_POPUP, ((float)WorldConfig.VIEWPORT_WIDTH / 2.0F), ((float)WorldConfig.VIEWPORT_HEIGHT / 4.0F), 1, 1009.0F, 379.0F);
      this.addActor(this.titlePanel = new InvitationConfirmPopup.TitlePanel(super.renderContext));
      Table messageTable;
      Table table = messageTable = new Table();
      table.add(
            this.messageLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F))
         )
         .grow()
         .padBottom(10.0F);
      this.messageLabel.setAlignment(2);
      this.messageLabel.setWrap(true);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(messageTable)).setSize(780.0F, 100.0F);
      scrollPane.setPosition(155.0F, 186.0F);
      this.addActor(scrollPane);
      this.buttonTable = new Table();
      this.buttonTable.setSize(780.0F, 63.0F);
      this.buttonTable.setPosition(155.0F, 111.0F);
      this.addActor(this.buttonTable);
      this.buttonTable
         .add(
            this.acceptButton = new InvitationConfirmPopup.StyledButton(super.renderContext, "Đồng ý")
               .setStyle(InvitationConfirmPopup.ButtonStyle.ACCEPT)
         )
         .size(169.0F, 63.0F)
         .space(8.0F);
      this.buttonTable
         .add(
            this.refuseButton = new InvitationConfirmPopup.StyledButton(super.renderContext, "Từ chối")
               .setStyle(InvitationConfirmPopup.ButtonStyle.REFUSE)
         )
         .size(169.0F, 63.0F)
         .space(8.0F);
      this.acceptButton.addListener(new InvitationAcceptClickListener(this));
      this.refuseButton.addListener(new DeclineInviteClickListener(this));
   }

   @Override
   public final boolean isDismissOnOutsideClick() {
      return true;
   }

   public final void setInvitationData(byte invitationType, String title, String message) {
      this.invitationType = invitationType;
      this.titlePanel.setTitle(title);
      this.messageLabel.setText("[#8B4E23]" + message);
   }

   @Override
   public final void drawBackground() {
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/45"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
   }

   public static enum ButtonStyle {
      ACCEPT(CompositeSprite.obtain("ui/54", "ui/55", "ui/56", false)),
      REFUSE(CompositeSprite.obtain("ui/57", "ui/58", "ui/59", false));

      public CompositeSprite sprite;

      ButtonStyle(CompositeSprite sprite) {
         this.sprite = sprite;
      }

   }

   public class StyledButton extends ShapeButton {
      public InvitationConfirmPopup.ButtonStyle style = InvitationConfirmPopup.ButtonStyle.ACCEPT;
      public String text;
      public TextraLabel label;

      public StyledButton(RenderContext renderContext, String text) {
         super(renderContext);
         this.text = text;
         this.add(
               this.label = new TextraLabel(
                  this.text, FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 24, 2.0F)
               )
            )
            .grow();
         this.label.setAlignment(2);
      }

      public final InvitationConfirmPopup.StyledButton setStyle(InvitationConfirmPopup.ButtonStyle style) {
         this.style = style;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext.drawCompositeSprite(this.style.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }
   }

   public class TitlePanel extends BaseTable {
      public TextraLabel titleLabel;

      public TitlePanel(RenderContext renderContext) {
         super(renderContext);
         this.setSize(279.0F, 99.0F);
         this.setPosition(415.0F, 300.0F);
         this.add(this.titleLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 42))).grow();
         this.titleLabel.setAlignment(2);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain("ui/47"),
               this.getX(),
               this.getY(),
               this.getWidth(),
               this.getHeight(),
               Anchor.BOTTOM_LEFT
            );
         super.draw(batch, parentAlpha);
      }

      public void setTitle(String title) {
         this.titleLabel.setText("[#8B4E23]" + title);
      }
   }
}
