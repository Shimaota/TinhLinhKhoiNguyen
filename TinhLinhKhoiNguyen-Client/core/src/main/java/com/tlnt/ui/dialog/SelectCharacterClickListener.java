package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class SelectCharacterClickListener extends ClickListener {
   public com.tlnt.ui.dialog.CharacterCreateDialog.ClassSelectButton classSelectButton;
   public com.tlnt.ui.dialog.CharacterCreateDialog characterCreateDialog;
   public SelectCharacterClickListener(CharacterCreateDialog.ClassSelectButton classSelectButton, CharacterCreateDialog characterCreateDialog) {
      super();
      this.classSelectButton = classSelectButton;
      this.characterCreateDialog = characterCreateDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.classSelectButton.characterClass != null) {
         this.classSelectButton.dialog.selectCharacterClass(this.classSelectButton.characterClass);
      }
   }
}
