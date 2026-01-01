package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.Settings;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.ShapeButton;

public final class HudSkillButton1 extends ShapeButton {
   public int slotIndex = 1;
   public Item skill;

   public HudSkillButton1(int slotIndex, RenderContext renderContext) {
      super(renderContext);
      this.setSize(114.0F, 118.0F);
      float posX = (float)(WorldConfig.VIEWPORT_WIDTH - (Gdx.app.getType() != Application.ApplicationType.iOS ? 527 : 637));
      this.setPosition(posX, 165.0F, 20);
      this.addListener(new HudSkillDepositClickListener(this));
   }

   public final void setSkill(Item skill) {
      this.skill = skill;
   }

   public final void activateSkill() {
      ServerPacketSender.getInstance().sendSwapItems(Settings.pendingQuickSlotItemId, this.slotIndex);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (!Settings.assigningStaminaFoodSlot) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext
            .drawTextureSized(TextureAsset.obtain("ui/35"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
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
                  this.getY() + 20.0F,
                  Anchor.BOTTOM_CENTER,
                  20.0F,
                  Color.WHITE
               );
         }

         super.renderContext.resetBatchColor();
      }
   }

}
