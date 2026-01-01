package com.tlnt.ui.window.weaponcraft;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.ShapeButton;

public final class CraftWeaponButton extends ShapeButton {
   public com.tlnt.ui.window.weaponcraft.WeaponCraftWindow.RecipeDetailPanel recipeDetailPanel;
   public CompositeSprite iconSprite;

   public CraftWeaponButton(WeaponCraftWindow.RecipeDetailPanel detailPanel, RenderContext ctx) {
      super(ctx);
      this.recipeDetailPanel = detailPanel;
      String leftPath = "ui/10";
      String middlePath = "ui/11";
      this.iconSprite = CompositeSprite.obtain(leftPath, middlePath, "ui/12", false);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawCompositeSprite(this.iconSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Chế tác",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            24.0F,
            Color.WHITE
         );
      super.renderContext.resetBatchColor();
   }

}
