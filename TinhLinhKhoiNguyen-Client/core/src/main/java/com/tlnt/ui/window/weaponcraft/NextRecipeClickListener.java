package com.tlnt.ui.window.weaponcraft;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class NextRecipeClickListener extends ClickListener {
   public com.tlnt.ui.window.weaponcraft.WeaponCraftWindow weaponCraftWindow;
   public NextRecipeClickListener(WeaponCraftWindow window) {
      super();
      this.weaponCraftWindow = window;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.weaponCraftWindow.showNextRecipe();
   }
}
