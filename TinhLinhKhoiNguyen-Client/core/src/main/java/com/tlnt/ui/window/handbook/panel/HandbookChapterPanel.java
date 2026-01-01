package com.tlnt.ui.window.handbook.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.guide.GuideCategory;
import com.tlnt.model.guide.GuideTopic;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.util.JsonUiBuilder;

public final class HandbookChapterPanel extends GamePanel {
   public HandbookChapterPanel.PageListPanel pageListPanel = new HandbookChapterPanel.PageListPanel(this.renderContext);
   public HandbookChapterPanel.PageContentPanel pageContentPanel;
   public GuideCategory chapter;

   public HandbookChapterPanel(RenderContext ctx) {
      super(ctx);
      this.pageListPanel.setSize(488.0F, 610.0F);
      this.addActor(this.pageListPanel);
      this.pageContentPanel = new HandbookChapterPanel.PageContentPanel(super.renderContext);
      this.pageContentPanel.setSize(742.0F, 610.0F);
      this.pageContentPanel.setPosition(505.0F, 0.0F);
      this.addActor(this.pageContentPanel);
   }

   public final HandbookChapterPanel bindChapter(GuideCategory category) {
      this.chapter = category;
      this.pageListPanel.refreshPageList(this.chapter);
      return this;
   }

   public class PageContentPanel extends BaseTable {
      public NinePatchAsset backgroundPatch;
      public JsonUiBuilder uiBuilder;
      public Table contentTable;
      public GuideTopic currentPage;

      public PageContentPanel(RenderContext ctx) {
         super(ctx);
         this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
         this.uiBuilder = new JsonUiBuilder();
         ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.contentTable = new Table());
         this.add(scrollPane).grow().pad(10.0F);
      }

      public final void displayPage(GuideTopic topic) {
         if (topic != null) {
            this.currentPage = topic;
            this.contentTable.clear();
            if (topic.contentTable == null) {
               this.uiBuilder.setItems(this.currentPage.items);
               topic.contentTable = this.uiBuilder.buildLayout(super.renderContext, topic.layoutJson);
            }

            this.contentTable.add(topic.contentTable).grow();
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }

   public class PageListPanel extends BaseTable {

      public final HandbookChapterPanel handbookChapterPanel = HandbookChapterPanel.this;
      public Pool<HandbookChapterPanel.PageListPanel.PageButton> pageButtonPool = new HandbookPageButtonPool(this);
      public NinePatchAsset backgroundPatch;
      public Table pageListTable;

      public PageListPanel(RenderContext ctx) {
         super(ctx);
         this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.pageListTable = new Table().top().left())).setSize(450.0F, 537.0F);
         scrollPane.setPosition(19.0F, 20.0F);
         this.addActor(scrollPane);
      }

      public final void refreshPageList(GuideCategory category) {

         for (int i = this.pageListTable.getChildren().size - 1; i >= 0; i--) {
            Actor child;
            if ((child = this.pageListTable.getChildren().get(i)) instanceof HandbookChapterPanel.PageListPanel.PageButton) {
               this.pageButtonPool.free((HandbookChapterPanel.PageListPanel.PageButton)child);
            }
         }

         this.pageListTable.clear();

         for (GuideTopic topic : category.topics) {
            HandbookChapterPanel.PageListPanel.PageButton pageButton = this.pageButtonPool.obtain().bindPage(topic);
            this.pageListTable.add(pageButton).size(450.0F, 58.0F).space(10.0F).row();
            if (pageButton.isChecked()) {
               HandbookChapterPanel.this.pageContentPanel.displayPage(topic);
            }
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               HandbookChapterPanel.this.chapter.name,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 576.0F,
               Anchor.BOTTOM_CENTER,
               30.0F,
               Color.WHITE
            );
         super.draw(batch, parentAlpha);
      }

      public class PageButton extends ShapeButton {

         public final PageListPanel pageListPanel = PageListPanel.this;
         public NinePatchAsset backgroundPatch;
         public GuideTopic page;

         public PageButton(RenderContext ctx) {
            super(ctx);
            String bgPath = "ui/99";
            this.backgroundPatch = NinePatchAsset.obtain(bgPath, 20);
            this.addListener(new HandbookPageButtonClickListener(this, PageListPanel.this));
         }

         public final HandbookChapterPanel.PageListPanel.PageButton bindPage(GuideTopic topic) {
            this.page = topic;
            return this;
         }

         @Override
         public final void draw(Batch batch, float parentAlpha) {
            if (this.isPressed()) {
               super.renderContext.setBatchColor(Color.GRAY);
            } else if (this.isChecked()) {
               super.renderContext.setBatchColor(Color.LIGHT_GRAY);
            }

            super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT, this.page.name, this.getX() + 15.0F, this.getY() + this.getHeight() / 2.0F, Anchor.MIDDLE_LEFT, 22.0F, Color.WHITE
               );
            super.renderContext.resetBatchColor();
         }
      }
   }
}
