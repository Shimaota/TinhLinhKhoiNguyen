package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class NpcSelectionOptionClickListener extends ClickListener {
   public com.tlnt.ui.window.action.NpcSelectionPopup parentPopup;
   public com.tlnt.ui.window.action.NpcSelectionPopup.OptionButton optionButton;
   public NpcSelectionOptionClickListener(NpcSelectionPopup.OptionButton optionButton, NpcSelectionPopup popup) {
      super();
      this.optionButton = optionButton;
      this.parentPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.optionButton.parentPopup.hide();
      ServerPacketSender.getInstance().sendMoveItem(this.optionButton.npcType, this.optionButton.npcId, this.optionButton.optionIndex);
   }
}
