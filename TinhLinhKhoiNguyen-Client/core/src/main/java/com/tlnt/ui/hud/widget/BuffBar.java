package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.DeformableSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.option.OptionInfo;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;

public final class BuffBar extends GamePanel {
   public Pool<BuffBar.BuffIcon> buffIconPool = new BuffIconPool(this);
   public Table buffIconTable;

   public BuffBar(RenderContext renderContext) {
      super(renderContext);
      this.setSize(223.0F, 109.0F);
      if (Gdx.app.getType() != Application.ApplicationType.iOS) {
         this.setPosition(535.0F, (float)(WorldConfig.VIEWPORT_HEIGHT - 10), 10);
      } else {
         this.setPosition(563.0F, (float)(WorldConfig.VIEWPORT_HEIGHT - 30), 10);
      }
      ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.buffIconTable = new Table().top().left());
      this.add(scrollPane).grow();
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator it = this.buffIconTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof BuffBar.BuffIcon) {
            this.buffIconPool.free((BuffBar.BuffIcon)child);
         }
      }

      this.buffIconTable.clear();

      for (OptionInfo buff : GameState.activeBuffs) {
         this.buffIconTable.add(this.buffIconPool.obtain().setBuff(buff)).size(52.0F).space(5.0F);
         if (this.buffIconTable.getChildren().size % 4 == 0) {
            this.buffIconTable.row();
         }
      }
   }

   public class BuffIcon extends ShapeButton {
      public NinePatchAsset backgroundPatch;
      public DeformableSprite cooldownSprite;
      public OptionInfo buffInfo;

      public BuffIcon(RenderContext renderContext) {
         super(renderContext);
         String patchPath = "ui/184";
         this.backgroundPatch = NinePatchAsset.obtain(patchPath, 20);
         this.cooldownSprite = new DeformableSprite(TextureAsset.obtain("ui/185").getRegion());
         this.addListener(new BuffIconClickListener(this, BuffBar.this));
      }

      public final BuffBar.BuffIcon setBuff(OptionInfo buffInfo) {
         this.buffInfo = buffInfo;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextureFit(
               TextureAsset.obtain(this.buffInfo.optionInfoTemplate.icon),
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               this.getWidth(),
               this.getHeight(),
               Anchor.CENTER
            );
         if (this.buffInfo.timeRemaining != -1) {
            int remainingMs;
            if ((remainingMs = (int)(this.buffInfo.startTime + (long)this.buffInfo.timeRemaining - System.currentTimeMillis())) < 0) {
               remainingMs = 0;
            }

            float sweepAngle;
            if ((sweepAngle = (float)remainingMs / (float)this.buffInfo.timeRemaining * 360.0F) > 0.0F) {
               super.renderContext
                  .drawRadialSprite(
                     this.cooldownSprite,
                     this.getX() + this.getWidth() / 2.0F,
                     this.getY() + this.getHeight() / 2.0F,
                     this.getWidth(),
                     this.getHeight(),
                     Anchor.CENTER,
                     sweepAngle
                  );
            }
         }

         super.renderContext.resetBatchColor();
      }
   }
}
