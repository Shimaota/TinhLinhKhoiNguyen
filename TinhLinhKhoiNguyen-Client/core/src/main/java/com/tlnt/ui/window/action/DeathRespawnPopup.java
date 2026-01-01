package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameMain;
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
import com.tlnt.world.entity.player.PlayerEntity;

public class DeathRespawnPopup extends Popup {
   public DeathRespawnPopup.TitlePanel titlePanel;
   public TextraLabel messageLabel;
   public Table buttonTable;
   public DeathRespawnPopup.StyledButton waitButton;
   public DeathRespawnPopup.StyledButton resurrectButton;
   public DeathRespawnPopup.StyledButton returnToVillageButton;

   public DeathRespawnPopup(RenderContext renderContext) {
      super(renderContext, WindowType.EXHAUSTED_POPUP, ((float)WorldConfig.VIEWPORT_WIDTH / 2.0F), ((float)WorldConfig.VIEWPORT_HEIGHT / 4.0F), 1, 964.0F, 394.0F);
      this.addActor(this.titlePanel = new DeathRespawnPopup.TitlePanel(super.renderContext));
      Table messageTable;
      (messageTable = new Table())
         .add(this.messageLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F)))
         .grow()
         .padBottom(10.0F);
      this.messageLabel.setAlignment(2);
      this.messageLabel.setColor(Color.valueOf("8B4E23"));
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(messageTable)).setSize(754.0F, 110.0F);
      scrollPane.setPosition(137.0F, 186.0F);
      this.addActor(scrollPane);
      this.buttonTable = new Table();
      this.buttonTable.setSize(700.0F, 63.0F);
      this.buttonTable.setPosition(167.0F, 111.0F);
      this.addActor(this.buttonTable);
      Table table = this.buttonTable;
      DeathRespawnPopup.StyledButton button = new DeathRespawnPopup.StyledButton(super.renderContext).setStyle(DeathRespawnPopup.ButtonStyle.NEUTRAL);
      table.add(this.waitButton = button.setText("Chờ")).size(169.0F, 63.0F).space(8.0F);
      table = this.buttonTable;
      button = new DeathRespawnPopup.StyledButton(super.renderContext).setStyle(DeathRespawnPopup.ButtonStyle.CONFIRM);
      table.add(this.resurrectButton = button.setText("Hồi sinh\n[%80](1 cỏ hồi sinh)")).size(169.0F, 63.0F).space(8.0F);
      this.buttonTable
         .add(
            this.returnToVillageButton = new DeathRespawnPopup.StyledButton(super.renderContext)
               .setStyle(DeathRespawnPopup.ButtonStyle.CANCEL)
               .setText("Về làng")
         )
         .size(169.0F, 63.0F)
         .space(8.0F);
      this.resurrectButton.addListener(new WaitRespawnClickListener(this));
      this.returnToVillageButton.addListener(new ReturnToVillageClickListener(this));
   }

   @Override
   public final void onRender(float delta) {
      if (PlayerEntity.getInstance() != null) {
         if (this.isVisible()) {
            if (PlayerEntity.getInstance().isAlive()) {
               this.hide();
               return;
            }
         } else if (PlayerEntity.getInstance().isDead()) {
            GameMain.instance.getLoginScreen().debugScrollPane.showWindow(super.windowType);
         }
      }
   }

   public final DeathRespawnPopup setMessage(String message) {
      this.messageLabel.setText(message);
      return this;
   }

   @Override
   public final void drawBackground() {
      super.renderContext
         .drawTextureSized(TextureAsset.obtain("ui/45"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
   }

   public static enum ButtonStyle {
      CONFIRM(CompositeSprite.obtain("ui/54", "ui/55", "ui/56", false)),
      CANCEL(CompositeSprite.obtain("ui/57", "ui/58", "ui/59", false)),
      NEUTRAL(CompositeSprite.obtain("ui/10", "ui/11", "ui/12", false));

      public CompositeSprite sprite;

      ButtonStyle(CompositeSprite sprite) {
         this.sprite = sprite;
      }

   }

   public class StyledButton extends ShapeButton {
      public DeathRespawnPopup.ButtonStyle style = DeathRespawnPopup.ButtonStyle.CONFIRM;
      public TextraLabel label;

      public StyledButton(RenderContext renderContext) {
         super(renderContext);
         this.add(this.label = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 24, 1.5F)))
            .grow()
            .padTop(0.0F);
         this.label.setAlignment(2);
      }

      public final DeathRespawnPopup.StyledButton setText(String text) {
         this.label.setText(text);
         return this;
      }

      public final DeathRespawnPopup.StyledButton setStyle(DeathRespawnPopup.ButtonStyle style) {
         this.style = style;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext.drawCompositeSprite(this.style.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
         this.drawChildren(batch, parentAlpha);
      }
   }

   public class TitlePanel extends BaseTable {
      public TextraLabel titleLabel;

      public TitlePanel(RenderContext renderContext) {
         super(renderContext);
         this.setSize(279.0F, 99.0F);
         this.setPosition(375.0F, 296.0F);
         this.add(
               this.titleLabel = new TextraLabel(
                  "Kiệt sức", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 42)
               )
            )
            .grow();
         this.titleLabel.setAlignment(2);
         this.titleLabel.setColor(Color.valueOf("8B4E23"));
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawTextureSized(TextureAsset.obtain("ui/47"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }
}
