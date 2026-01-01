package com.tlnt.ui.window.charinfo;

import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.player.PlayerStats;
import com.tlnt.ui.window.GamePanel;

public abstract class CharacterStatRow extends GamePanel {
   public PlayerStats entityStats;
   public String statLabelText;
   public TextraLabel nameLabel;
   public TextraLabel valueLabel;
   public String cachedValue;

   public CharacterStatRow(RenderContext renderContext, String label) {
      super(renderContext);
      this.statLabelText = "-" + label;
      this.add(
            this.nameLabel = new TextraLabel(
               "[#8B4E23]" + this.statLabelText, FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 24)
            )
         )
         .grow();
      String valuePrefix = "[#8B4E23]";
      this.add(this.valueLabel = new TextraLabel(valuePrefix, FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 24))).grow();
      this.nameLabel.setAlignment(10);
      this.valueLabel.setAlignment(18);
   }

   public final CharacterStatRow setEntityStats(PlayerStats stats) {
      this.entityStats = stats;
      return this;
   }

   @Override
   public final void onUpdate(float delta) {
      String value;
      if (this.entityStats != null && !(value = this.getStatValue(this.entityStats)).equals(this.cachedValue)) {
         this.cachedValue = value;
         this.valueLabel.setText("[#8B4E23]" + this.cachedValue);
      }
   }

   public abstract String getStatValue(PlayerStats stats);

}
