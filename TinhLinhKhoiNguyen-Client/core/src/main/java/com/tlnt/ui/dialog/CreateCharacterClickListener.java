package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class CreateCharacterClickListener extends ClickListener {
   public com.tlnt.ui.dialog.CharacterCreateDialog characterCreateDialog;
   public CreateCharacterClickListener(CharacterCreateDialog characterCreateDialog) {
      super();
      this.characterCreateDialog = characterCreateDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.characterCreateDialog.submitCharacterCreation();
   }
}
