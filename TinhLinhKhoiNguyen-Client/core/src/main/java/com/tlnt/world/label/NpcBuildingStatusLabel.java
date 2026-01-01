package com.tlnt.world.label;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.farm.AnimalBarnState;
import com.tlnt.util.TimeFormatter;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

public final class NpcBuildingStatusLabel extends FloatingOverlay {
   public NpcEntity playerEntity;

   public NpcBuildingStatusLabel(NpcEntity npcEntity) {
      super(new Vector2(0.0F, 52.0F));
      this.playerEntity = npcEntity;
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      if (this.playerEntity.POOL2.template.id == 18) {
         super.size.y = 70.0F;
         AnimalBarnState barnState = GameState.buildingStatus;
         String statusText;
         if (GameState.buildingStatus.upgrading) {
            long remainingMs;
            if ((remainingMs = barnState.upgradeStartTime + barnState.upgradeDurationMs - System.currentTimeMillis()) < 0L) {
               remainingMs = 0L;
            }

            statusText = "Đang nâng cấp (" + TimeFormatter.formatDuration(remainingMs) + ")";
         } else {
            int harvestCount;
            long elapsedMs;
            if ((harvestCount = (int)((elapsedMs = System.currentTimeMillis() - barnState.productionStartTime) / (long)barnState.productionIntervalMs)) > barnState.maxHarvestCount) {
               harvestCount = barnState.maxHarvestCount;
            }

            if (harvestCount == barnState.maxHarvestCount) {
               statusText = "Có thể thu hoạch (" + harvestCount + "/" + barnState.maxHarvestCount + ")";
            } else {
               long nextHarvestMs = (long)barnState.productionIntervalMs - elapsedMs % (long)barnState.productionIntervalMs;
               statusText = "Có thể thu hoạch (" + harvestCount + "/" + barnState.maxHarvestCount + ") " + TimeFormatter.formatDuration(nextHarvestMs);
            }
         }

         ctx.drawTextColorName(
            FontManager.DEFAULT_FONT, statusText, x, y + super.size.y / 2.0F, Anchor.TOP_CENTER, 30.0F, "#fff600"
         );
         ctx.drawText(
            FontManager.DEFAULT_FONT,
            this.playerEntity.getTemplateName() + " cấp " + barnState.level,
            x,
            y - super.size.y / 2.0F,
            Anchor.BOTTOM_CENTER,
            26.0F,
            Color.WHITE
         );
      } else {
         boolean noOverheadText = this.playerEntity.POOL2.template.overheadText.isEmpty();
         super.size.y = noOverheadText ? 26.0F : 70.0F;
         if (noOverheadText) {
            ctx.drawText(FontManager.DEFAULT_FONT, this.playerEntity.getTemplateName(), x, y, Anchor.CENTER, 26.0F, Color.WHITE);
         } else {
            ctx.drawText(
               FontManager.DEFAULT_FONT,
               this.playerEntity.POOL2.template.overheadText,
               x,
               y + super.size.y / 2.0F,
               Anchor.TOP_CENTER,
               32.0F,
               Color.YELLOW
            );
            ctx.drawText(
               FontManager.DEFAULT_FONT, this.playerEntity.getTemplateName(), x, y - super.size.y / 2.0F, Anchor.BOTTOM_CENTER, 26.0F, Color.WHITE
            );
         }
      }
   }

   @Override
   public final void release() {
      super.release();
      this.playerEntity = null;
   }

}
