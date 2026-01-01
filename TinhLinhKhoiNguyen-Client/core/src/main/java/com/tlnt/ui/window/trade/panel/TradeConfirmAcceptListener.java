package com.tlnt.ui.window.trade.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameState;
import com.tlnt.net.task.ServerPacketSender;

public final class TradeConfirmAcceptListener extends ClickListener {
   public com.tlnt.ui.window.trade.panel.TradeOfferPanel tradeOfferPanel;
   public TradeConfirmAcceptListener(TradeOfferPanel panel) {
      super();
      this.tradeOfferPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendStopFishing();
      GameState.tradeSession.myAccepted = true;
   }
}
