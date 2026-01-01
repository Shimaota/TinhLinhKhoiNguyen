package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class HudSkillDepositClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.HudSkillButton1 hudSkillButton;
   public HudSkillDepositClickListener(HudSkillButton1 hudSkillButton) {
      super();
      this.hudSkillButton = hudSkillButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendDepositItem(this.hudSkillButton.slotIndex);
   }
}
