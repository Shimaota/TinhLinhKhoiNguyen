package com.tlnt.ui.window.talent.panel;

import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.action.QuantityInputDialog;
import com.tlnt.world.entity.player.PlayerEntity;

public final class TalentPointsInputHandler implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.talent.panel.EnterTalentPointsClickListener clickListener;
   public TalentPointsInputHandler(EnterTalentPointsClickListener listener) {
      super();
      this.clickListener = listener;
   }

   @Override
   public final void onInputConfirmed(String input) {
      int points = 0;

      try {
         points = Integer.parseInt(input);
      } catch (Exception e) {
      }

      if (points <= 0) {
         points = 1;
      } else if (points > PlayerEntity.getInstance().POOL2.availableTalentPoints / this.clickListener.skillDetailPanel.talentPanel.selectedPassiveSkill.pointCost
         )
       {
         points = PlayerEntity.getInstance().POOL2.availableTalentPoints / this.clickListener.skillDetailPanel.talentPanel.selectedPassiveSkill.pointCost;
      }

      ServerPacketSender.getInstance().sendAttack(this.clickListener.skillDetailPanel.talentPanel.selectedPassiveSkill.id, points);
   }
}
