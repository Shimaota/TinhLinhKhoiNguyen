package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.player.FactionStatus;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.util.GameUtils;

public final class PopupMenu extends GamePanel {
   public NinePatchAsset background;
   public boolean isOpen;
   public long lastToggleTime;

   public PopupMenu(RenderContext renderContext) {
      super(renderContext);
      this.background = NinePatchAsset.obtain("ui/13", 20);
      this.setSize(70.0F, (float)(FactionStatus.FactionType.values().length * 70));
      float posX = Gdx.app.getType() != Application.ApplicationType.iOS ? 455.0F : 480.0F;
      this.setPosition(posX, (float)WorldConfig.VIEWPORT_HEIGHT + this.getHeight(), 10);
      PopupMenu menu = this;
      FactionStatus.FactionType[] factionTypes;
      int count = (factionTypes = FactionStatus.FactionType.values()).length;

      for (int i = 0; i < count; i++) {
         FactionStatus.FactionType factionType = factionTypes[i];
         menu.add(menu.new PopupActionButton(factionType, menu.renderContext)).grow().uniform().row();
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isOpen) {
         if (GameUtils.hasElapsed(this.lastToggleTime, 5000)) {
            this.toggle();
         }
      }

      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

   public final void toggle() {
      this.clearActions();
      if (this.isOpen) {
         this.addAction(Actions.moveTo(this.getX(), (float)WorldConfig.VIEWPORT_HEIGHT, 0.5F, Interpolation.swingIn));
      } else {
         this.addAction(Actions.moveTo(this.getX(), (float)WorldConfig.VIEWPORT_HEIGHT - this.getHeight() - 90.0F, 0.5F, Interpolation.swingOut));
      }

      this.lastToggleTime = System.currentTimeMillis();
      int open;
      if (!this.isOpen) {
         open = 1;
      } else {
         open = 0;
      }

      this.isOpen = (open != 0);
   }

   public class PopupActionButton extends ShapeButton {

      public final PopupMenu popupMenu = PopupMenu.this;
      public TextureAsset actionIcon;
      public FactionStatus.FactionType actionType;

      public PopupActionButton(FactionStatus.FactionType actionType, RenderContext renderContext) {
         super(renderContext);
         this.actionType = actionType;
         this.actionIcon = TextureAsset.obtain(this.actionType.displayName);
         this.addListener(new PopupMenuItemClickListener(this, PopupMenu.this));
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext.drawTextureSized(this.actionIcon, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }
   }
}
