package com.tlnt.world.entity.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector3;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.WorldEntity;

public final class ChatBubbleComponent extends BaseComponent {
   public GameObject projectile;
   public GlyphLayout speed = new GlyphLayout();
   public Vector3 active = new Vector3();
   public String autoFlipEnabled;
   public boolean projectile2;
   public float lastTemplate;
   public float elapsedTime;

   public ChatBubbleComponent(WorldEntity owner) {
      super(owner.getEntity());
      this.projectile = owner;
      this.setRenderOrder(1.0E7F);
   }

   public final void showChatMessage(String message) {
      this.lastTemplate = (float)((int)((float)message.split(" ").length / 200.0F * 60.0F));
      if (this.lastTemplate < 3.0F) {
         this.lastTemplate = 3.0F;
      }

      if (this.lastTemplate > 15.0F) {
         this.lastTemplate = 15.0F;
      }

      String wrappedText;
      if (message != null && !message.isEmpty()) {
         StringBuilder result = new StringBuilder();
         String[] words = message.split("\\s+");
         StringBuilder line = new StringBuilder();
         String[] wordArray;
         int wordCount = (wordArray = words).length;

         for (int i = 0; i < wordCount; i++) {
            String word = wordArray[i];
            int lineLength = line.length() + word.length();
            int spaceLength;
            if (line.length() == 0) {
               spaceLength = 0;
            } else {
               spaceLength = 1;
            }

            if (lineLength + spaceLength > 20) {
               StringBuilder appended = result.append((CharSequence)line);
               appended.append("\n");
               line = new StringBuilder(word);
            } else {
               if (line.length() != 0) {
                  line.append(" ");
               }

               line.append(word);
            }
         }

         if (line.length() != 0) {
            result.append((CharSequence)line);
         }

         wrappedText = result.toString().trim();
      } else {
         wrappedText = "";
      }

      this.autoFlipEnabled = wrappedText;
      GlyphLayout layout = this.speed;
      BitmapFont font = FontFactory.scaleFont(FontManager.PLAIN_FONT, 24.0F);
      layout.setText(font, this.autoFlipEnabled, Color.WHITE, 0.0F, Anchor.BOTTOM_RIGHT.halign, false);
      this.elapsedTime = 0.0F;
      this.projectile2 = true;
   }

   @Override
   public final void update(float delta) {
      if (this.projectile2) {
         this.elapsedTime += delta;
         if (this.elapsedTime >= this.lastTemplate) {
            this.projectile2 = false;
         }

         this.active.set(this.getEntity().getPosition().x, this.getEntity().getPosition().y + this.projectile.getHeight() + 0.5F, 0.0F);
      }
   }

   @Override
   public final void render(RenderContext ctx) {
      if (this.projectile2) {
         float textWidth = this.speed.width;
         float textHeight = this.speed.height;
         textWidth = Math.max(100.0F, textWidth);
         textHeight = Math.max(30.0F, textHeight);
         ctx.worldToHud(this.active);
         ctx.useHudCamera();
         ctx.drawNinePatch(
            NinePatchAsset.obtain("ui/123", 20),
            this.active.x,
            this.active.y - 10.0F,
            textWidth + 20.0F,
            textHeight + 20.0F,
            Anchor.BOTTOM_CENTER
         );
         ctx.drawTexture(TextureAsset.obtain("ui/149"), this.active.x, this.active.y, Anchor.TOP_CENTER);
         ctx.drawText(FontManager.PLAIN_FONT, this.autoFlipEnabled, this.active.x, this.active.y + textHeight / 2.0F, Anchor.CENTER, 24.0F, Color.BLACK);
         ctx.usePreviousCamera();
      }
   }

}
