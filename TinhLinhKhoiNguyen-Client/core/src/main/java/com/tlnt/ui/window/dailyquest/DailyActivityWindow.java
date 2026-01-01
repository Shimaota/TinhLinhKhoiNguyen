package com.tlnt.ui.window.dailyquest;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.dailyquest.panel.AchievementPanel;
import com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel;
import com.tlnt.ui.window.dailyquest.panel.MainQuestPanel;

public final class DailyActivityWindow extends TabbedWindow {

   public DailyActivityWindow(RenderContext ctx) {
      super(ctx, WindowType.QUEST_POPUP);
      String tabTitle = "Chính tuyến";
      this.addTab(tabTitle, "icon/269", new MainQuestPanel(super.renderContext));
      tabTitle = "Hàng ngày";
      this.addTabFull(
         tabTitle, "icon/738", new DailyQuestPanel(super.renderContext), null, new DailyQuestTabCondition(this)
      );
      this.addTabFull(
         "Thành tựu",
         "icon/256",
         new AchievementPanel(super.renderContext),
         null,
         new AchievementTabCondition(this)
      );
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

}
