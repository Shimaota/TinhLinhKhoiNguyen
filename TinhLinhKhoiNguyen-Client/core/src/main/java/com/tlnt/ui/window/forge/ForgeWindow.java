package com.tlnt.ui.window.forge;

import com.tlnt.core.GameState;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.forge.panel.ForgeBrewPanel;
import com.tlnt.ui.window.forge.panel.ForgeCraftPanel;
import com.tlnt.ui.window.forge.panel.ForgeElementPanel;
import com.tlnt.ui.window.forge.panel.ForgeRefinePanel;
import com.tlnt.ui.window.forge.panel.ForgeSoulPanel;
import com.tlnt.ui.window.forge.panel.ForgeTransmutePanel;
import com.tlnt.ui.window.forge.panel.ForgeUpgradePanel;

public class ForgeWindow extends TabbedWindow {
   public ForgeUpgradePanel OVERLAY_COLOR;

   public ForgeWindow(RenderContext renderContext) {
      super(renderContext, WindowType.COMBINE_POPUP);
      this.addTabWithCallback(
         "Nâng cấp", "icon/276", this.OVERLAY_COLOR = new ForgeUpgradePanel(super.renderContext), () -> {
            if (GameState.forgeData.activeMode == 0) {
               return true;
            } else {
               return false;
            }
         }
      );
      this.addTabWithCallback("Linh hồn", "icon/276", new ForgeSoulPanel(super.renderContext), () -> {
         if (GameState.forgeData.activeMode == 1) {
            return true;
         } else {
            return false;
         }
      });
      String tabTitle = "Nguyên tố";
      this.addTabWithCallback(
         tabTitle,
         "icon/276",
         new ForgeElementPanel(super.renderContext),
         () -> ((GameState.forgeData.activeMode == 2
               ? 1
               : 0) != 0)
      );
      tabTitle = "Pha chế";
      this.addTabWithCallback(tabTitle, "icon/276", new ForgeBrewPanel(super.renderContext), () -> {
         if (GameState.forgeData.activeMode == 3) {
            return true;
         } else {
            return false;
         }
      });
      tabTitle = "Chế tác";
      this.addTabWithCallback(tabTitle, "icon/276", new ForgeCraftPanel(super.renderContext), () -> {
         if (GameState.forgeData.activeMode == 4) {
            return true;
         } else {
            return false;
         }
      });
      this.addTabWithCallback(
         "Luyện đá", "icon/276", new ForgeRefinePanel(super.renderContext), () -> {
            return ((GameState.forgeData.activeMode == 5 ? 1 : 0) != 0);
         }
      );
      this.addTabWithCallback(
         "Chuyển hóa", "icon/0", new ForgeTransmutePanel(super.renderContext), () -> GameState.forgeData.activeMode == 6
      );
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

   @Override
   public final void onClose() {
   }

}
