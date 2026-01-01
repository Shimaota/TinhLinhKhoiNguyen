package com.tlnt.world.label;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

public final class GuildNameLabel extends FloatingOverlay {
   public PlayerEntity playerEntity;
   public GlyphLayout glyphLayout;
   public float fontSize;

   public GuildNameLabel(PlayerEntity playerEntity) {
      super(new Vector2(0.0F, 30.0F));
      this.playerEntity = playerEntity;
      this.glyphLayout = new GlyphLayout();
   }

   @Override
   public final void refresh() {
      int visibleFlag;
      if (this.playerEntity.POOL2.guildId != -1) {
         visibleFlag = 1;
      } else {
         visibleFlag = 0;
      }

      this.setVisible((visibleFlag != 0));
      this.fontSize = 28.0F;
      this.glyphLayout.setText(FontFactory.scaleFont(FontManager.DEFAULT_FONT, this.fontSize), this.playerEntity.POOL2.guildName);
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      String badge = this.playerEntity.POOL2.guildBadgeTexture;
      if (badge != null && !badge.isEmpty()) {
         ctx.drawTextureScaled(TextureAsset.obtain(badge), x - this.glyphLayout.width / 2.0F - 25.0F, y + 5.0F, Anchor.CENTER, 1.0F, 1.0F);
      }
      String colorName = this.playerEntity.POOL2.guildId == PlayerEntity.getInstance().POOL2.guildId
         ? "#00FF90"
         : "#FFF600";
      ctx.drawTextColorName(FontManager.DEFAULT_FONT, this.playerEntity.POOL2.guildName, x, y + 10.0F, Anchor.CENTER, this.fontSize, colorName);
   }

   @Override
   public final void release() {
      super.release();
      this.playerEntity = null;
   }

}
