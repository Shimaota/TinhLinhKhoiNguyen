package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameState;
import com.tlnt.core.Settings;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.DeformableSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.ShapeButton;

public final class MpPotionSlotButton extends ShapeButton {
   public DeformableSprite cooldownSprite;
   public Item item;

   public MpPotionSlotButton(RenderContext renderContext) {
      super(renderContext);
      this.setSize(88.0F, 88.0F);
      float posX = (float)(WorldConfig.VIEWPORT_WIDTH - (Gdx.app.getType() != Application.ApplicationType.iOS ? 645 : 755));
      this.setPosition(posX, 25.0F, 20);
      this.addListener(new MpPotionSlotButtonClickListener(this));
      this.cooldownSprite = new DeformableSprite(TextureAsset.obtain("ui/27").getRegion());
      this.cooldownSprite.setColor(Color.BLACK);
   }

   public final void usePotion() {
      if (!(getCooldownRatio() > 0.0F)) {
         ServerPacketSender.getInstance().sendDepositItem(4);
      }
   }

   public final void setItem(Item item) {
      this.item = item;
   }

   public final Item getItem() {
      return this.item;
   }

   public static void swapItemToSlot() {
      ServerPacketSender.getInstance().sendSwapItems(Settings.pendingQuickSlotItemId, 4);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (!Settings.assigningMpPotionSlot) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext
            .drawTextureSized(TextureAsset.obtain("ui/27"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (this.item != null && this.item.template != null) {
            super.renderContext
               .drawTexture(
                  TextureAsset.obtain(this.item.template.iconPath), this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  this.item.quantity + "",
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + 10.0F,
                  Anchor.BOTTOM_CENTER,
                  20.0F,
                  Color.WHITE
               );
         }

         super.renderContext.resetBatchColor();
         float cooldownRatio;
         if ((cooldownRatio = getCooldownRatio()) != 0.0F && this.item != null && this.item.template != null) {
            super.renderContext
               .drawRadialSprite(this.cooldownSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT, 360.0F * cooldownRatio);
         }
      }
   }

   public static float getCooldownRatio() {
      long remainingMs;
      if ((remainingMs = GameState.skill2CooldownStart + (long)GameState.skill2CooldownDuration - System.currentTimeMillis()) < 0L) {
         remainingMs = 0L;
      }

      return (float)remainingMs / (float)GameState.skill2CooldownDuration;
   }

}
