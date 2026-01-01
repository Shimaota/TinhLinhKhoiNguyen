package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ComboPackBuyClickListener extends ClickListener {
   private static final long PENDING_RETRY_MS = 2000L;
   public com.tlnt.ui.window.shop.panel.ComboPackPanel.ComboPackRow comboPackRow;
   public ComboPackBuyClickListener(ComboPackPanel.ComboPackRow row) {
      super();
      this.comboPackRow = row;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      event.stop();
      event.handle();
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      if (popup.shouldBlockShopClick()) {
         return;
      }
      if (this.comboPackRow.packData != null && !this.comboPackRow.packData.purchased) {
         com.tlnt.model.combopack.ComboPack pack = this.comboPackRow.packData;
         long now = System.currentTimeMillis();
         if (pack.purchasePending && now - pack.purchaseRequestedAt < PENDING_RETRY_MS) {
            return;
         }
         pack.purchasePending = true;
         pack.purchaseRequestedAt = now;
         ServerPacketSender.getInstance().sendSocialAction(pack.catalog.id, pack.index);
      }
   }
}
