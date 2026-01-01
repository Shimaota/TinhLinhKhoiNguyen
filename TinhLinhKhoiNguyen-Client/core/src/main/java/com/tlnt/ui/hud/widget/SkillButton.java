package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.skill.LearnedSkill;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.world.entity.player.PlayerEntity;

public final class SkillButton extends ShapeButton {
   public TextureAsset backgroundTexture;
   public TextureAsset iconOverlay;
   public TextureAsset frameOverlay;
   public TextureAsset selectedIndicator;
   public TextureAsset cooldownTexture;
   public LearnedSkill skill;
   public boolean isSelected;

   public SkillButton(RenderContext renderContext) {
      super(renderContext);
      this.backgroundTexture = TextureAsset.obtain("ui/25");
      this.iconOverlay = TextureAsset.obtain("ui/207");
      this.frameOverlay = TextureAsset.obtain("ui/165");
      this.selectedIndicator = TextureAsset.obtain("ui/26");
      this.cooldownTexture = TextureAsset.obtain("ui/164");
      this.addListener(new SkillButtonClickListener(this));
   }

   public final void activateSkill() {
      if (this.isSelected) {
         if (this.skill != null && this.isSelected) {
            PlayerEntity.getInstance().interactionController.interact(false);
            return;
         }
      } else {
         if (this.skill == null || this.isSelected) {
            return;
         }

         ServerPacketSender.getInstance().sendSkillUse(this.skill.skill.id);
      }
   }

   public final void setSkill(LearnedSkill skill, boolean isSelected) {
      this.skill = skill;
      this.isSelected = isSelected;
   }

   public final LearnedSkill getSkill() {
      return this.skill;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
      }

      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.beginStencilMask();
      super.renderContext
         .drawTextureSized(
            this.iconOverlay, this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, this.getWidth(), this.getHeight(), Anchor.CENTER
         );
      super.renderContext.beginMaskedRender();
      if (this.skill != null) {
         int iconIndex;
         if (this.skill.skill.id == 1) {
            if (PlayerEntity.getInstance().POOL2.weaponType == -1) {
               iconIndex = 1;
            } else {
               iconIndex = 0;
            }
         } else {
            if (PlayerEntity.getInstance().POOL2.weaponType == -1) {
               iconIndex = 0;
            } else {
               iconIndex = PlayerEntity.getInstance().POOL2.weaponType;
            }
         }

         if (iconIndex >= 0) {
            super.renderContext
               .drawTextureSized(
                  TextureAsset.obtain(this.skill.skill.iconPaths[iconIndex]),
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth(),
                  this.getHeight(),
                  Anchor.CENTER
               );
         }

         if (this.skill.levelData != null) {
            long now = System.currentTimeMillis();
            long cooldownEnd;
            if ((cooldownEnd = this.skill.lastCastTime + (long)this.skill.levelData.cooldownMillis) > now) {
               float remainingSec = (float)(cooldownEnd - now) / 1000.0F;
               RenderContext ctx = super.renderContext;
               TextureAsset texture = this.cooldownTexture;
               float overlayX = this.getX() + 10.5F;
               float overlayY = this.getY() + 12.0F;
               ctx.drawTextureCropped(
                  texture,
                  overlayX,
                  overlayY,
                  Anchor.BOTTOM_LEFT,
                  0,
                  0,
                  this.cooldownTexture.getWidth(),
                  (int)((float)this.cooldownTexture.getHeight() * ((float)(cooldownEnd - now) / (float)this.skill.levelData.cooldownMillis))
               );
               String cooldownText;
               if (remainingSec >= 1.0F) {
                  cooldownText = String.valueOf((int)remainingSec);
               } else {
                  String format = "%.2f";
                  Object[] formatArgs = new Object[1];
                  formatArgs[0] = remainingSec;
                  cooldownText = String.format(format, formatArgs);
               }

               super.renderContext
                  .drawText(
                     FontManager.DEFAULT_FONT,
                     cooldownText,
                     this.getX() + this.getWidth() / 2.0F,
                     this.getY() + this.getHeight() / 2.0F,
                     Anchor.CENTER,
                     22.0F,
                     Color.WHITE
                  );
            }
         }
      }

      super.renderContext.endStencilMask();
      super.renderContext.drawTextureSized(this.frameOverlay, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      if (this.isSelected) {
         super.renderContext.drawTexture(this.selectedIndicator, this.getX() - 5.0F, this.getY() + 86.0F, Anchor.BOTTOM_LEFT);
      }

      super.renderContext.resetBatchColor();
   }

}
