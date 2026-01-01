package com.tlnt.ui.window.ranking.panel;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.action.ItemActionMenuPopup;

public final class RankRowClickListener extends ClickListener {
   public com.tlnt.ui.window.ranking.panel.RankCategoryPanel panel;
   public com.tlnt.ui.window.ranking.panel.RankCategoryPanel.RankEntryRow row;
   public RankRowClickListener(RankCategoryPanel.RankEntryRow row, RankCategoryPanel panel) {
      super();
      this.row = row;
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.row.entry != null) {
         Vector2 stagePos = this.row.localToStageCoordinates(new Vector2(x, y));
         ItemActionMenuPopup menu = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemActionMenuPopup.class);
         float menuX = stagePos.x;
         float menuY = stagePos.y;
         ItemActionMenuPopup.ActionType[] actions = new ItemActionMenuPopup.ActionType[2];
         actions[0] = ItemActionMenuPopup.ActionType.ENCHANT_ITEM.withItemId(this.row.entry.playerId);
         actions[1] = ItemActionMenuPopup.ActionType.FORGE_UPGRADE.withItemId(this.row.entry.playerId);
         menu.showWithActions(menuX, menuY, 10, actions);
      }
   }
}
