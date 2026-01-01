package com.tlnt.ui.window.tribe;

import com.tlnt.core.GameState;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.ResourceBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.tribe.panel.TribeListEntry;
import com.tlnt.ui.window.tribe.panel.TribeMemberRow;
import com.tlnt.ui.window.tribe.panel.TribeNoticePanel;
import com.tlnt.ui.window.tribe.panel.TribePendingPanel;
import com.tlnt.ui.window.tribe.panel.TribeSkillPanel;
import com.tlnt.ui.window.tribe.panel.TribeWarehousePanel;

public final class TribeWindow extends TabbedWindow {
   public TribeShopPanel OVERLAY_COLOR;

   public TribeWindow(RenderContext renderContext) {
      super(renderContext, WindowType.CLAN_POPUP);
      this.addTabWithCallback("Bộ lạc", "icon/263", new TribeListEntry(super.renderContext), () -> {
         if (GameState.tribeData.myTribe == null) {
            return true;
         } else {
            return false;
         }
      });
      String tabTitle = "Thành viên";
      this.addTabWithCallback(
         tabTitle,
         "icon/264",
         new TribeMemberRow(super.renderContext),
         () -> ((GameState.tribeData.myTribe != null ? 1 : 0) != 0)
      );
      tabTitle = "Chờ duyệt";
      this.addTabWithCallback(tabTitle, "icon/265", new TribePendingPanel(super.renderContext), () -> {
         if (GameState.tribeData.myTribe != null) {
            return true;
         } else {
            return false;
         }
      });
      tabTitle = "Kỹ năng";
      this.addTabWithCallback(
         tabTitle,
         "icon/278",
         new TribeSkillPanel(super.renderContext),
         () -> ((GameState.tribeData.myTribe != null ? 1 : 0) != 0)
      );
      tabTitle = "Rương đồ";
      this.addTabWithCallback(tabTitle, "icon/266", new TribeWarehousePanel(super.renderContext), () -> {
         if (GameState.tribeData.myTribe != null) {
            return true;
         } else {
            return false;
         }
      });
      tabTitle = "Thông báo";
      this.addTabWithCallback(tabTitle, "icon/267", new TribeNoticePanel(super.renderContext), () -> {
         if (GameState.tribeData.myTribe != null) {
            return true;
         } else {
            return false;
         }
      });
      this.addTabWithCallback(
         "Cửa hàng",
         "icon/268",
         this.OVERLAY_COLOR = new TribeShopPanel(this, super.renderContext),
         () -> {
            if (GameState.tribeData.myTribe != null) {
               return true;
            } else {
               return false;
            }
         }
      );
      this.addActor(new ResourceBarPanel(super.renderContext));
   }

   @Override
   public final void onUpdate() {
      if (GameState.shopData != null && GameState.shopData.shopType == 2 && !GameState.shopData.categories.isEmpty()) {
         this.OVERLAY_COLOR.setItemCategory(GameState.shopData.categories.first());
      } else {
         this.OVERLAY_COLOR.setItemCategory(null);
      }
   }

}
