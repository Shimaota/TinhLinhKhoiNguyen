package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.player.PlayerStats;
import com.tlnt.ui.window.GamePanel;

public final class CharacterPortraitPanel extends GamePanel {
   public TextureAsset backgroundTexture;
   public PlayerStats stats;

   public CharacterPortraitPanel(RenderContext renderContext) {
      super(renderContext);
      this.backgroundTexture = TextureAsset.obtain("ui/88");
   }

   public final CharacterPortraitPanel setPlayerData(PlayerStats playerStats) {
      this.stats = playerStats;
      return this;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.stats != null) {
         super.renderContext
            .drawTexture(
               TextureAsset.obtain("ui/47"),
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight(),
               Anchor.TOP_CENTER
            );
         super.renderContext.drawTexture(TextureAsset.obtain("ui/4"), this.getX() + this.getWidth() / 2.0F, this.getY(), Anchor.BOTTOM_CENTER);
         super.renderContext
            .drawText(
               FontManager.PLAIN_FONT, this.stats.level + "", this.getX() + this.getWidth() / 2.0F, this.getY() + 25.0F, Anchor.CENTER, 18.0F, Color.WHITE
            );
         super.renderContext
            .drawTextColorName(
               FontManager.PLAIN_FONT,
               this.stats.characterName,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 98.0F,
               Anchor.CENTER,
               24.0F,
               "#8B4E23"
            );
         RenderContext ctx = super.renderContext;
         BitmapFont font = FontManager.PLAIN_FONT;
         String expText = this.stats.currentExp + "/" + this.stats.maxExp;
         float pos = this.getX() + this.getWidth() / 2.0F;
         float expTextY = this.getY() + 75.0F;
         ctx.drawTextColorName(font, expText, pos, expTextY, Anchor.CENTER, 14.0F, "#8B4E23");
         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain("ui/87"), this.getX() + 21.0F, this.getY() + 58.0F, 236.0F, 10.0F, Anchor.BOTTOM_LEFT
            );
         ctx = super.renderContext;
         TextureAsset barTexture = this.backgroundTexture;
         float barX = this.getX() + 23.0F;
         pos = this.getY() + 60.0F;
         Anchor barAnchor = Anchor.BOTTOM_LEFT;
         float barWidth = (float)this.backgroundTexture.getWidth();
         PlayerStats playerStats = this.stats;
         ctx.drawTextureCropped(
            barTexture,
            barX,
            pos,
            barAnchor,
            0,
            0,
            (int)(barWidth * (this.stats.maxExp == 0L ? 0.0F : (float)playerStats.currentExp / (float)playerStats.maxExp)),
            this.backgroundTexture.getHeight()
         );
      }
   }

}
