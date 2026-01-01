package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class QuestTabClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.QuestPartyPanel questPartyPanel;
   public QuestTabClickListener(QuestPartyPanel questPartyPanel) {
      super();
      this.questPartyPanel = questPartyPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.questPartyPanel.questPanel.setVisible(true);
      this.questPartyPanel.partyPanel.setVisible(false);
   }
}
