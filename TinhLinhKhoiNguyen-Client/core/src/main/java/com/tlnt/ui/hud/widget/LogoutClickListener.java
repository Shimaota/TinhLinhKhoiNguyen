package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.screen.login.LoginScreen;

public final class LogoutClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.NearbyInteractButton nearbyInteractButton;
   public LogoutClickListener(NearbyInteractButton nearbyInteractButton) {
      super();
      this.nearbyInteractButton = nearbyInteractButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance.getLoginScreen();
      LoginScreen.cycleNearbyTarget();
   }
}
