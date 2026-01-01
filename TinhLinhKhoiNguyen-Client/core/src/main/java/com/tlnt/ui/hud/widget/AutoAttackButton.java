package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.entity.player.component.AutoTargetController;

public final class AutoAttackButton extends ShapeButton {
   public AutoAttackButton(RenderContext renderContext) {
      super(renderContext);
      this.setSize(89.0F, 89.0F);
      float posX = (float)(WorldConfig.VIEWPORT_WIDTH - (Gdx.app.getType() != Application.ApplicationType.iOS ? 40 : 150));
      this.setPosition(posX, 25.0F, 20);
      this.addListener(new AutoAttackClickListener(this));
   }

   public static void toggleAutoAttack() {
      AutoTargetController controller = PlayerEntity.getInstance().autoTargetController;
      int projectile;
      if (!PlayerEntity.getInstance().autoTargetController.projectile) {
         projectile = 1;
      } else {
         projectile = 0;
      }

      controller.projectile = (projectile != 0);
      if (controller.projectile) {
         com.tlnt.core.Settings.autoAttackAnchor.set(PlayerEntity.getInstance().getPosition()); // decompiled C3632: "Attack range" anchor
      }
      if (controller.projectile && PlayerEntity.getInstance().offscreenArrow != null) {
         PlayerEntity.getInstance().offscreenArrow.setAutoPathing(false); // decompiled C3632: auto-attack replaces auto-pathing
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
      }

      if (PlayerEntity.getInstance().autoTargetController != null) {
         RenderContext ctx = super.renderContext;
         int iconId;
         if (PlayerEntity.getInstance().autoTargetController.projectile) {
            iconId = 41;
         } else {
            iconId = 40;
         }

         ctx.drawTextureSized(TextureAsset.obtain("ui/" + iconId), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      }

      super.renderContext.resetBatchColor();
   }
}
