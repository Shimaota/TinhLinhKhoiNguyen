package com.tlnt.ui.window.action;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.option.OptionInfo;
import com.tlnt.ui.hud.widget.BuffBar;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.WindowType;

public class BuffTimerTooltip extends Popup {
   public TextureAsset backgroundTexture;
   public TextraLabel descriptionLabel;
   public OptionInfo currentOptionInfo;

   public BuffTimerTooltip(RenderContext renderContext) {
      super(renderContext, WindowType.INFO_OPTION_INFO_POPUP, ((float)WorldConfig.VIEWPORT_WIDTH / 2.0F), ((float)WorldConfig.VIEWPORT_HEIGHT / 2.0F), 1, 317.0F, 200.0F);
      this.backgroundTexture = TextureAsset.obtain("ui/130");
      Table contentTable;
      Table table = contentTable = new Table().padBottom(5.0F);
      table.add(
            this.descriptionLabel = new TextraLabel(
               "", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 20, 2.5F)
            )
         )
         .grow();
      this.descriptionLabel.setWrap(true);
      this.descriptionLabel.setAlignment(2);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(contentTable)).setSize(277.0F, 160.0F);
      scrollPane.setPosition(20.0F, 20.0F);
      this.addActor(scrollPane);
   }

   public final void showForBuff(BuffBar.BuffIcon buffIcon, OptionInfo optionInfo) {
      this.currentOptionInfo = optionInfo;
      Vector2 stagePos = new Vector2(0.0F, 0.0F);
      buffIcon.localToStageCoordinates(stagePos);
      float x = stagePos.x;
      float y = stagePos.y - this.getHeight();
      this.setPosition(x, y);
      this.show();
   }

   @Override
   public final void onUpdate(float delta) {
      if (this.currentOptionInfo != null) {
         long elapsedMs = System.currentTimeMillis() - this.currentOptionInfo.startTime;
         long remainingMs;
         if ((remainingMs = (long)this.currentOptionInfo.timeRemaining - elapsedMs) < 0L) {
            remainingMs = 0L;
         }

         int totalSeconds;
         int minutes = (totalSeconds = (int)(remainingMs / 1000L)) / 60;
         int seconds = totalSeconds % 60;
         String format = "(%02d:%02d)";
         Object[] formatArgs = new Object[2];
         formatArgs[0] = minutes;
         formatArgs[1] = seconds;
         String timeText = String.format(format, formatArgs);
         StringBuilder sb;
         (sb = new StringBuilder()).append("[#8B4E23][%130]");
         StringBuilder nameSb = sb.append(this.currentOptionInfo.optionInfoTemplate.name);
         nameSb.append("[%100]\n").append(timeText).append("\n");
         sb.append(this.currentOptionInfo.optionInfoTemplate.description);
         this.descriptionLabel.setText(sb.toString());
      }
   }

   @Override
   public final boolean isDismissOnOutsideClick() {
      return true;
   }

   @Override
   public final boolean isModal() {
      return false;
   }

   @Override
   public final void drawBackground() {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
   }

}
