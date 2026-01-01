package com.tlnt.ui.window.trade.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class TradeEnterCoinsListener extends ClickListener {
   public com.tlnt.ui.window.trade.panel.TradeOfferPanel tradeOfferPanel;

   public TradeEnterCoinsListener(TradeOfferPanel panel) {
      super();
      this.tradeOfferPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (!GameState.tradeSession.myOfferLocked && !GameState.tradeSession.myAccepted) {
         GameMain.instance
            .getLoginScreen()
            .debugScrollPane
            .findDialog(QuantityInputDialog.class)
            .openWithPrompt(
               "Nhập số vàng", new TextField.TextFieldFilter.DigitsOnlyFilter(), "", new TradeGoldAmountInputCallback(this)
            );
      }
   }

}
