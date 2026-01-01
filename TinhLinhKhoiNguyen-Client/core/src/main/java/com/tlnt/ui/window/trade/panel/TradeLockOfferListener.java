package com.tlnt.ui.window.trade.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;

public final class TradeLockOfferListener extends ClickListener {
   public com.tlnt.ui.window.trade.panel.TradeOfferPanel tradeOfferPanel;
   public TradeLockOfferListener(TradeOfferPanel panel) {
      super();
      this.tradeOfferPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (!GameState.tradeSession.myOfferLocked) {
         int[][] offerEntries = new int[GameState.tradeSession.myOfferItems.size][2];

         for (int i = 0; i < offerEntries.length; i++) {
            Item item = GameState.tradeSession.myOfferItems.get(i);
            int[] entry = offerEntries[i];
            entry[0] = item.slotIndex;
            entry = offerEntries[i];
            entry[1] = item.quantity;
         }

         ServerPacketSender.getInstance().sendFishingResult(GameState.tradeSession.myOfferCoins, offerEntries);
         GameState.tradeSession.myOfferLocked = true;
         GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
      }
   }
}
