package com.tlnt.ui.window.character.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class MaskExpandClickListener extends ClickListener {
   public com.tlnt.ui.window.character.panel.MaskPanel.MaskSkillPanel maskSkillPanel;
   public MaskExpandClickListener(MaskPanel.MaskSkillPanel panel) {
      super();
      this.maskSkillPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestDailyTask();
   }
}
