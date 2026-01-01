package com.tlnt.ui.window.weaponcraft;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class CraftWeaponClickListener extends ClickListener {
   public com.tlnt.ui.window.weaponcraft.WeaponCraftWindow.RecipeDetailPanel recipeDetailPanel;
   public CraftWeaponClickListener(WeaponCraftWindow.RecipeDetailPanel detailPanel) {
      super();
      this.recipeDetailPanel = detailPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendMountAction(this.recipeDetailPanel.weaponCraftWindow.currentRecipeIndex);
   }
}
