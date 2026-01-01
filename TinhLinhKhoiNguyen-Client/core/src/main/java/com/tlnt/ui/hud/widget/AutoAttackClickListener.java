package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class AutoAttackClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.AutoAttackButton autoAttackButton;
   public AutoAttackClickListener(AutoAttackButton autoAttackButton) {
      super();
      this.autoAttackButton = autoAttackButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      AutoAttackButton.toggleAutoAttack();
   }
}
