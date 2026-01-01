package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class QuestPartyTabToggleListener extends ClickListener {
   public com.tlnt.ui.hud.widget.QuestPartyPanel questPartyPanel;
   public QuestPartyTabToggleListener(QuestPartyPanel questPartyPanel) {
      super();
      this.questPartyPanel = questPartyPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.questPartyPanel.partyPanel.setVisible(true);
      this.questPartyPanel.questPanel.setVisible(false);
   }
}
