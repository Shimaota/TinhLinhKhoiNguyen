package com.tlnt.ui.window.controls.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ObjectSet;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.mob.MobTemplate;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.world.entity.player.PlayerEntity;

/**
 * "Offline Training" button at the bottom of the auto-attack Settings tab (decompiled C3764: shown only while the Spirit Badge is active). Click ->
 * op 46 (all Settings currently shown) + op 77/0 with the selected mob types (the "All" box = send empty); the server asks for confirmation,
 * and the idle session follows exactly this table (skill slots, Avoid elite mobs, Do not attack bosses, Auto loot, potions, Auto revive).
 */
public final class OfflineTrainingButton extends TextButton {
   private final CompositeSprite buttonSprite = CompositeSprite.obtain("ui/10", "ui/11", "ui/12", false);

   public OfflineTrainingButton(RenderContext ctx) {
      super("Luyện tập Offline", ctx);
      this.addListener(new ClickListener() {
         @Override
         public void clicked(InputEvent event, float x, float y) {
            // Snapshot the Settings table that is open: resend the settings (op 46) FIRST, the server uses them for the whole idle session
            ServerPacketSender.getInstance().sendGameSettings();
            ServerPacketSender.getInstance().sendOfflineTraining(selectedMobTemplates());
         }
      });
   }

   static int[] selectedMobTemplates() {
      PlayerEntity player = PlayerEntity.getInstance();
      if (player == null || player.autoTargetController.speed) {
         return new int[0];
      }
      ObjectSet<MobTemplate> active = player.autoTargetController.active;
      int[] ids = new int[active.size];
      int i = 0;
      for (MobTemplate template : active) {
         ids[i++] = template.id;
      }
      return ids;
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.resetBatchColor();
   }

   @Override
   public final void drawLabel() {
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            super.text,
            this.getX() + this.getWidth() / 2.05F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            22.0F,
            Color.WHITE
         );
   }
}
