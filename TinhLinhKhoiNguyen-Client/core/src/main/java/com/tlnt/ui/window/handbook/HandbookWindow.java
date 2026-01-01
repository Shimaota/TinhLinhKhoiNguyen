package com.tlnt.ui.window.handbook;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.guide.GuideCategory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.handbook.panel.HandbookChapterPanel;

public final class HandbookWindow extends TabbedWindow {
   public Pool<HandbookChapterPanel> OVERLAY_COLOR = new HandbookChapterPanelPool(this);

   public HandbookWindow(RenderContext ctx) {
      super(ctx, WindowType.HANDBOOK_POPUP);
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

   @Override
   public final void onUpdate() {
      if (GuideCategory.dirty) {
         Array.ArrayIterator panelIter = this.getPanels().iterator();

         while (panelIter.hasNext()) {
            GamePanel panel;
            if ((panel = (GamePanel)panelIter.next()) instanceof HandbookChapterPanel) {
               this.OVERLAY_COLOR.free((HandbookChapterPanel)panel);
            }
         }

         this.clearAllTabs();

         for (GuideCategory category : GuideCategory.getAll()) {
            this.addTab(category.name, category.iconPath, this.OVERLAY_COLOR.obtain().bindChapter(category));
         }

         GuideCategory.dirty = false;
      }
   }
}
