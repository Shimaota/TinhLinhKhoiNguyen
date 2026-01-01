package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.tribe.panel.info.TribeInfoPanel;

public final class TribeNoticePanel extends GamePanel {
   public TribeNoticePanel(RenderContext renderContext) {
      super(renderContext);
      TribeInfoPanel infoPanel;
      (infoPanel = new TribeInfoPanel(super.renderContext)).setSize(616.0F, 610.0F);
      this.addActor(infoPanel);
      TribeNoticePanel.ContentPanel contentPanel;
      (contentPanel = new TribeNoticePanel.ContentPanel(super.renderContext)).setSize(610.0F, 610.0F);
      contentPanel.setPosition(637.0F, 0.0F);
      this.addActor(contentPanel);
   }

   public class ContentPanel extends GamePanel {
      public NinePatchAsset backgroundPatch;
      public TextraLabel noticeTextLabel;

      public ContentPanel(RenderContext renderContext) {
         super(renderContext);
         this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
         Table contentTable;
         Table table = contentTable = new Table();
         table.add(
               this.noticeTextLabel = new TextraLabel(
                  "", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 2.0F)
               )
            )
            .grow();
         this.noticeTextLabel.setAlignment(10);
         this.noticeTextLabel.setWrap(true);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(contentTable)).setSize(572.0F, 512.0F);
         scrollPane.setPosition(19.0F, 49.0F);
         this.addActor(scrollPane);
      }

      @Override
      public final void refresh() {
         StringBuilder sb = new StringBuilder();

         for (String notice : GameState.tribeData.notices) {
            StringBuilder appended = sb.append(notice);
            appended.append("\n");
         }

         this.noticeTextLabel.setText(sb.toString());
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Thông báo Bộ lạc",
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 576.0F,
               Anchor.BOTTOM_CENTER,
               30.0F,
               Color.WHITE
            );
         super.draw(batch, parentAlpha);
      }
   }
}
