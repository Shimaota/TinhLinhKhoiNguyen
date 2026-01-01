package com.tlnt.ui.window.character;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.character.panel.CharacterBagTab;
import com.tlnt.ui.window.character.panel.CharacterStatsTab;
import com.tlnt.ui.window.talent.panel.TalentPanel;

public class CharacterWindow extends TabbedWindow {

   public CharacterWindow(RenderContext renderContext) {
      super(renderContext, WindowType.GENERIC_POPUP);
      this.addTab("Nhân vật", "icon/252", new CharacterStatsTab(super.renderContext));
      this.addTab("Hành trang", "icon/255", new CharacterBagTab(super.renderContext));
      String talentTabTitle = "Kỹ năng";
      this.addTab(talentTabTitle, "icon/278", new TalentPanel(super.renderContext));
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

}
