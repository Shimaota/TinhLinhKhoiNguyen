package com.tlnt.ui.window.character.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class PetExpandClickListener extends ClickListener {
   public com.tlnt.ui.window.character.panel.PetPanel.PetSkillPanel petSkillPanel;
   public PetExpandClickListener(PetPanel.PetSkillPanel panel) {
      super();
      this.petSkillPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestGuildVault();
   }
}
