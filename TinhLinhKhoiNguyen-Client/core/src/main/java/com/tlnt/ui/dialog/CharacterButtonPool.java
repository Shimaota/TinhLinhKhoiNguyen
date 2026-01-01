package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;

public final class CharacterButtonPool extends Pool<CharacterCreateDialog.ClassSelectButton> {
   public com.tlnt.ui.dialog.CharacterCreateDialog ownerDialog;
   public ButtonGroup<Button> buttonGroup;

   public CharacterButtonPool(CharacterCreateDialog ownerDialog) {
      super();
      this.ownerDialog = ownerDialog;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final CharacterCreateDialog.ClassSelectButton newObject() {
      CharacterCreateDialog.ClassSelectButton button = this.ownerDialog.new ClassSelectButton(this.ownerDialog.renderContext);
      this.buttonGroup.add(button);
      return button;
   }
}
