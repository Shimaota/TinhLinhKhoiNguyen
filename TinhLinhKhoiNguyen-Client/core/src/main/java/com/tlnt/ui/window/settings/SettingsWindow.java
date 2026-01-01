package com.tlnt.ui.window.settings;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.TitledPopup;
import com.tlnt.ui.window.settings.panel.AccountProtectionPanel;
import com.tlnt.ui.window.settings.panel.ChangeSecondaryPasswordPanel;
import com.tlnt.ui.window.settings.panel.CreateSecondaryPasswordPanel;
import com.tlnt.ui.window.settings.panel.SettingsMainPanel;

public class SettingsWindow extends TitledPopup {
   public Group optionsTable;
   public SettingsMainPanel OVERLAY_COLOR;
   public AccountProtectionPanel accountProtectionPanel;
   public CreateSecondaryPasswordPanel closeButton;
   public ChangeSecondaryPasswordPanel changeSecondaryPasswordPanel;

   public SettingsWindow(RenderContext ctx) {
      super(ctx, WindowType.SETTING_POPUP, "Cài đặt");
      this.add(this.optionsTable = new Group()).grow();
      this.optionsTable.addActor(this.OVERLAY_COLOR = new SettingsMainPanel(this, super.renderContext));
      this.optionsTable.addActor(this.accountProtectionPanel = new AccountProtectionPanel(this, super.renderContext));
      this.optionsTable.addActor(this.closeButton = new CreateSecondaryPasswordPanel(this, super.renderContext));
      this.optionsTable.addActor(this.changeSecondaryPasswordPanel = new ChangeSecondaryPasswordPanel(this, super.renderContext));
   }

   @Override
   public final void onOpen() {
      super.onOpen();
      this.showPanel(SettingsMainPanel.class);
   }

   public final void showPanel(Class panelClass) {
      for (int i = 0; i < this.optionsTable.getChildren().size; i++) {
         Actor child;
         (child = this.optionsTable.getChild(i)).setVisible(panelClass.isInstance(child));
      }
   }

}
