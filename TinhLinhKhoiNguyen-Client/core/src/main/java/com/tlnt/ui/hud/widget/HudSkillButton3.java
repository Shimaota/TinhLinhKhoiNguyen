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

public final class HudSkillButton3 extends ShapeButton {
   public DeformableSprite cooldownOverlay;
   public Item skill;

   public HudSkillButton3(RenderContext renderContext) {
      super(renderContext);
      this.setSize(88.0F, 88.0F);
      float posX = (float)(WorldConfig.VIEWPORT_WIDTH - (Gdx.app.getType() != Application.ApplicationType.iOS ? 753 : 863));
      this.setPosition(posX, 25.0F, 20);
      this.addListener(new ConsumableButtonClickListener(this));
      this.cooldownOverlay = new DeformableSprite(TextureAsset.obtain("ui/28").getRegion());
      this.cooldownOverlay.setColor(Color.BLACK);
   }

   public final void activateSkill() {
      if (!(getCooldownRatio() > 0.0F)) {
         ServerPacketSender.getInstance().sendDepositItem(3);
      }
   }

   public final void setSkill(Item skill) {
      this.skill = skill;
   }

   public static void swapSkillFromInventory() {
      ServerPacketSender sender = ServerPacketSender.getInstance();
      sender.sendSwapItems(Settings.pendingQuickSlotItemId, 3);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (!Settings.assigningHpPotionSlot) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain("ui/28"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
            );
         if (this.skill != null && this.skill.template != null) {
            super.renderContext
               .drawTexture(
                  TextureAsset.obtain(this.skill.template.iconPath), this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  this.skill.quantity + "",
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + 10.0F,
                  Anchor.BOTTOM_CENTER,
                  20.0F,
                  Color.WHITE
               );
         }

         super.renderContext.resetBatchColor();
         float cooldownRatio;
         if ((cooldownRatio = getCooldownRatio()) != 0.0F && this.skill != null && this.skill.template != null) {
            super.renderContext
               .drawRadialSprite(this.cooldownOverlay, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT, 360.0F * cooldownRatio);
         }
      }
   }

   public static float getCooldownRatio() {
      long remainingMs;
      if ((remainingMs = GameState.skill1CooldownStart + (long)GameState.skill1CooldownDuration - System.currentTimeMillis()) < 0L) {
         remainingMs = 0L;
      }

      return (float)remainingMs / (float)GameState.skill1CooldownDuration;
   }

}
