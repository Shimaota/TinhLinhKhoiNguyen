package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class CreateCharacterClickListener2 extends ClickListener {
   public com.tlnt.ui.dialog.CharacterCreateDialog characterCreateDialog;
   public CreateCharacterClickListener2(CharacterCreateDialog characterCreateDialog) {
      super();
      this.characterCreateDialog = characterCreateDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().disconnect();
   }
}
