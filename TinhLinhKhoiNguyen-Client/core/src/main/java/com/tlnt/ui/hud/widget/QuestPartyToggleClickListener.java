package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class QuestPartyToggleClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.QuestPartyPanel questPartyPanel;
   public com.tlnt.ui.hud.widget.QuestPartyToggleButton toggleButton;
   public QuestPartyToggleClickListener(QuestPartyToggleButton toggleButton, QuestPartyPanel questPartyPanel) {
      super();
      this.toggleButton = toggleButton;
      this.questPartyPanel = questPartyPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      boolean checked = this.toggleButton.isChecked();
      this.questPartyPanel.clearActions();
      if (checked) {
         this.questPartyPanel.addAction(Actions.moveTo(-this.questPartyPanel.getWidth(), this.questPartyPanel.getY(), 0.5F, Interpolation.swingIn));
      } else {
         this.questPartyPanel.addAction(
            Actions.moveTo(this.questPartyPanel.openX, this.questPartyPanel.getY(), 0.5F, Interpolation.swingOut)
         );
      }
   }
}
