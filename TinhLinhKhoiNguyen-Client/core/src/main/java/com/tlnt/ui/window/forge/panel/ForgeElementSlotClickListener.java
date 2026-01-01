package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.RemoveInlayAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ForgeElementSlotClickListener extends ClickListener {
   public com.tlnt.ui.window.forge.panel.ForgeElementPanel.RecipePanel recipePanel;
   public ForgeElementSlotClickListener(ForgeElementPanel.RecipePanel panel) {
      super();
      this.recipePanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ForgeElementPanel.ElementSlotButton anchor = this.recipePanel.elementSlot2;
      Item item = this.recipePanel.elementSlot2.getItem();
      RemoveInlayAction defaultAction = ItemActionRegistry.DETACH_INLAY_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
      actions[0] = ItemActionRegistry.DETACH_INLAY_ACTION;
      actions[1] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
