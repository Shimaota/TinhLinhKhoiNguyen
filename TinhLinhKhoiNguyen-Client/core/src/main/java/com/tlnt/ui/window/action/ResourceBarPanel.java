package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.window.GamePanel;
import java.text.MessageFormat;

public final class ResourceBarPanel extends GamePanel {
   public ResourceBarPanel.ResourceBarCell leftResourceBar;
   public ResourceBarPanel.ResourceBarCell rightResourceBar;

   public ResourceBarPanel(RenderContext renderContext) {
      super(renderContext);
      this.setSize(981.0F, 78.0F);
      this.setPosition(313.0F, -41.0F);
      this.center();
      this.add(this.leftResourceBar = new ResourceBarPanel.ResourceBarCell("ui/147", super.renderContext))
         .size(315.0F, 78.0F)
         .space(18.0F);
      this.add(this.rightResourceBar = new ResourceBarPanel.ResourceBarCell("ui/146", super.renderContext))
         .size(315.0F, 78.0F)
         .space(18.0F);
   }

   @Override
   public final void refresh() {
      if (GameState.tribeData.myTribe != null) {
         this.leftResourceBar.setAmount(GameState.tribeData.myTribe.fund);
         this.rightResourceBar.setAmount(GameState.tribeData.myTribe.resourcePoints);
      }
   }

   public class ResourceBarCell extends BaseTable {
      public TextureAsset bgTexture;
      public TextureAsset iconTexture;
      public String valueText;

      public ResourceBarCell(String iconPath, RenderContext renderContext) {
         super(renderContext);
         this.bgTexture = TextureAsset.obtain("ui/89");
         this.iconTexture = TextureAsset.obtain(iconPath);
      }

      public final void setAmount(long amount) {
         String pattern = "{0}";
         Object[] formatArgs = new Object[1];
         formatArgs[0] = amount;
         this.valueText = MessageFormat.format(pattern, formatArgs);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawTextureSized(this.bgTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.drawTexture(this.iconTexture, this.getX() + 40.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT, this.valueText, this.getX() + this.getWidth() - 23.0F, this.getY() + 32.0F, Anchor.BOTTOM_RIGHT, 22.0F, Color.WHITE
            );
      }
   }
}
