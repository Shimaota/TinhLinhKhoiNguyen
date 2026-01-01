package com.tlnt.ui.window.giftcode;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.giftcode.GiftcodeEvent;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.giftcode.panel.GiftcodeEventPanel;
import com.tlnt.ui.window.giftcode.panel.GiftcodeRedeemPanel;

public final class GiftcodeWindow extends TabbedWindow {
   public final Array<GiftcodeEventPanel> OVERLAY_COLOR = new Array<>();
   public GiftcodeRedeemPanel contentGroup = new GiftcodeRedeemPanel(this.renderContext);

   public GiftcodeWindow(RenderContext ctx) {
      super(ctx, WindowType.WELFARE);
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

   @Override
   public final void onOpen() {
      super.onOpen();
      if (this.getTabCount() < GameState.giftcodeData.events.size + 1) {
         int missingTabs = GameState.giftcodeData.events.size - this.getTabCount();

         for (int i = 0; i < missingTabs; i++) {
            GiftcodeEventPanel eventPanel = new GiftcodeEventPanel(super.renderContext);
            this.addTabFull("", "", eventPanel, null, new GiftcodeEventTabChecker(this, eventPanel));
            this.OVERLAY_COLOR.add(eventPanel);
         }

         this.addTab("Giftcode", "icon/262", this.contentGroup);
      }

      for (int j = 0; j < GameState.giftcodeData.events.size; j++) {
         GiftcodeEvent event = GameState.giftcodeData.events.get(j);
         this.updateTabLabel(j, event.title, event.iconPath);
         this.OVERLAY_COLOR.get(j).setEventList(event);
      }
   }

}
