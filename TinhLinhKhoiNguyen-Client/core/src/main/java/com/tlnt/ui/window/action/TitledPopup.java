package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.WindowType;

public abstract class TitledPopup extends Popup {
   public static final Color OVERLAY_COLOR = new Color(0.0F, 0.0F, 0.0F, 0.5F);
   public TitledPopup.TitleBar optionsTable;
   public String accountProtectionPanel;
   public TitledPopup.CloseButton closeButton;

   public TitledPopup(RenderContext renderContext, WindowType windowType, String title) {
      super(renderContext, windowType, (float)WorldConfig.VIEWPORT_WIDTH / 2.1F, (float)WorldConfig.VIEWPORT_HEIGHT / 2.0F, 1, 1009.0F, 643.0F);
      this.accountProtectionPanel = title;
      this.addActor(this.optionsTable = new TitledPopup.TitleBar(super.renderContext));
      this.addActor(this.closeButton = new TitledPopup.CloseButton(super.renderContext));
   }

   @Override
   public void onOpen() {
      this.closeButton.toFront();
      AudioManager.playSound("menu_open", null);
   }

   @Override
   public final void onClose() {
      AudioManager.playSound("menu_close", null);
   }

   @Override
   public final void show() {
      this.clearActions();
      super.show();
      this.addAction(
         Actions.sequence(
            Actions.moveToAligned(super.originX, -this.getHeight(), super.positionAlignment),
            Actions.moveToAligned(super.originX, super.originY, super.positionAlignment, 0.3F, Interpolation.smoother)
         )
      );
   }

   @Override
   public final void hide() {
      this.clearActions();
      MoveToAction moveIn = Actions.moveToAligned(super.originX, super.originY, super.positionAlignment);
      MoveToAction moveOut = Actions.moveToAligned(super.originX, -this.getHeight(), super.positionAlignment, 0.3F, Interpolation.smoother);
      this.addAction(Actions.sequence(moveIn, moveOut, Actions.visible(false)));
   }

   @Override
   public void drawBackground() {
      super.renderContext.drawLine(0.0F, 0.0F, (float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT, OVERLAY_COLOR);
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/44"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
   }

   public class CloseButton extends ShapeButton {

      public final TitledPopup titledPopup = TitledPopup.this;

      public CloseButton(RenderContext renderContext) {
         super(renderContext);
         this.setSize(66.0F, 67.0F);
         this.setPosition(931.0F, 565.0F);
         this.addListener(new TitledPopupCloseListener(this, TitledPopup.this));
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawTextureSized(TextureAsset.obtain("ui/65"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }
   }

   public class TitleBar extends BaseTable {

      public TitleBar(RenderContext renderContext) {
         super(renderContext);
         this.setSize(279.0F, 99.0F);
         this.setPosition(400.0F, 554.0F);
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
         super.renderContext
            .drawTextColorName(
               FontManager.PLAIN_FONT,
               TitledPopup.this.accountProtectionPanel,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 45.0F,
               Anchor.BOTTOM_CENTER,
               42.0F,
               "#8B4E23"
            );
      }
   }
}
