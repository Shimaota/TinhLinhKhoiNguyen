package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class NpcDialogChoiceClickListener extends ClickListener {
   public com.tlnt.ui.window.action.NpcDialogPopupV2.NpcChoiceButton dialogOptionButton;
   public com.tlnt.ui.window.action.NpcDialogPopupV2 dialogPopup;
   public NpcDialogChoiceClickListener(NpcDialogPopupV2.NpcChoiceButton choiceButton, NpcDialogPopupV2 popup) {
      super();
      this.dialogOptionButton = choiceButton;
      this.dialogPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.dialogOptionButton.buttonSprite.hide();
      ServerPacketSender.getInstance()
         .sendMoveItem(this.dialogOptionButton.buttonSprite.npcType, this.dialogOptionButton.buttonSprite.npcId, this.dialogOptionButton.choiceIndex);
   }
}
