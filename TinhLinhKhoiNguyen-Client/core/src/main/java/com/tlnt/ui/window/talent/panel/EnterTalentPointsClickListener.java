package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class EnterTalentPointsClickListener extends ClickListener {
   public final com.tlnt.ui.window.talent.panel.TalentPanel.SkillDetailPanel skillDetailPanel;

   public EnterTalentPointsClickListener(TalentPanel.SkillDetailPanel detailPanel) {
      this.skillDetailPanel = detailPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.skillDetailPanel.talentPanel.selectedPassiveSkill != null) {
         QuantityInputDialog inputDialog = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(QuantityInputDialog.class);
         inputDialog.openWithPrompt(
            "Nhập số điểm", new TextField.TextFieldFilter.DigitsOnlyFilter(), "", new TalentPointsInputHandler(this)
         );
      }
   }

}
