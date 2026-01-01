package com.tlnt.world.label;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.player.FactionStatus;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

public final class PlayerNameLabel extends FloatingOverlay {
   public PlayerEntity playerEntity;
   public GlyphLayout glyphLayout;
   public float fontSize;

   static final Color TEAMMATE_COLOR = Color.valueOf("FFF600");

   /** Teammate outside the camera view: yellow name in the left / right edge column of the HUD (this frame). */
   private void offscreen(boolean left, float width) {
      com.tlnt.core.GameMain game = com.tlnt.core.GameMain.instance;
      com.tlnt.screen.login.LoginScreen screen = game == null ? null : game.getLoginScreen();
      if (screen != null && screen.debugScrollPane != null && screen.debugScrollPane.POOL2 != null) {
         screen.debugScrollPane.POOL2.addOffscreenTeammate(this.playerEntity.getCharacterName(), left, width);
      }
   }

   public PlayerNameLabel(PlayerEntity playerEntity) {
      super(new Vector2(0.0F, 26.0F));
      this.playerEntity = playerEntity;
      this.glyphLayout = new GlyphLayout();
   }

   @Override
   public final void refresh() {
      this.fontSize = this.playerEntity.isLocalPlayer() ? 26.0F : 24.0F;
      this.glyphLayout.setText(FontFactory.scaleFont(FontManager.DEFAULT_FONT, this.fontSize), this.playerEntity.getCharacterName());
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      if (this.playerEntity.POOL2.factionStatus.faction != FactionStatus.FactionType.NEUTRAL) {
         ctx.drawTextureScaled(
            TextureAsset.obtain(this.playerEntity.POOL2.factionStatus.faction.displayName),
            x - this.glyphLayout.width / 2.0F - 30.0F,
            y - 5.0F,
            Anchor.CENTER,
            0.75F,
            0.75F
         );
      }
      // decompiled C3489: red faction (FACTION_B) = red name; others white, IN THE SAME PARTY as the local player = yellow #FFF600 (+ off-screen
      // teammates show their names at the left / right HUD edges — C3617); in a duel (isInParty — wrong client name) = red
      Color nameColor;
      boolean teammate = false;
      if (this.playerEntity.POOL2.factionStatus.faction == FactionStatus.FactionType.FACTION_B) {
         nameColor = Color.RED;
      } else {
         PlayerEntity me = PlayerEntity.getInstance();
         int myParty = me == null || me.POOL2 == null ? -1 : me.POOL2.partyId;
         if (myParty != -1 && this.playerEntity.POOL2.partyId == myParty && !this.playerEntity.isLocalPlayer()) {
            nameColor = TEAMMATE_COLOR;
            teammate = true;
         } else {
            nameColor = Color.WHITE;
         }
      }
      if (this.playerEntity.POOL2.isInParty) {
         nameColor = Color.RED;
      }
      if (teammate) {
         com.badlogic.gdx.math.Rectangle view = ctx.viewBounds;
         float w = this.glyphLayout.width;
         if (x - w / 2.0F < view.x - w) {
            offscreen(true, w);
         } else if (x + w / 2.0F > view.x + view.width + w) {
            offscreen(false, w);
         }
      }

      ctx.drawText(FontManager.DEFAULT_FONT, this.playerEntity.getCharacterName(), x, y, Anchor.CENTER, this.fontSize, nameColor);
   }

   @Override
   public final void release() {
      super.release();
      this.playerEntity = null;
   }
}
