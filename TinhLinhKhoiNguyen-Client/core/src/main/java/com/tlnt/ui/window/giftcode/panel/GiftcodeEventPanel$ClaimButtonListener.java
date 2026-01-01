package com.tlnt.ui.window.giftcode.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class GiftcodeEventPanel$ClaimButtonListener extends ClickListener {
   public GiftcodeEventPanel.EventRowPanel eventDetailPanel;

   public GiftcodeEventPanel$ClaimButtonListener(GiftcodeEventPanel.EventRowPanel rowPanel) {
      super();
      this.eventDetailPanel = rowPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.eventDetailPanel.eventData != null) {
         if (this.eventDetailPanel.eventData.claimStatus == 1) {
            ServerPacketSender.getInstance().sendNpcInteract(this.eventDetailPanel.eventData.event.id, this.eventDetailPanel.eventData.id);
            return;
         }
      }
   }
}
