package com.tlnt.ui.hud;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.SnapshotArray;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.Dialog;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.BuffTimerTooltip;
import com.tlnt.ui.window.action.ChatInputPopup;
import com.tlnt.ui.window.action.DeathRespawnPopup;
import com.tlnt.ui.window.action.InvitationConfirmPopup;
import com.tlnt.ui.window.action.ItemActionMenuPopup;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.action.NpcSelectionPopup;
import com.tlnt.ui.window.action.QuantityInputDialog;
import com.tlnt.ui.window.action.RegionInfoPopup;
import com.tlnt.ui.window.character.CharacterWindow;
import com.tlnt.ui.window.chat.ChatWindow;
import com.tlnt.ui.window.chest.ChestWindow;
import com.tlnt.ui.window.controls.ControlsWindow;
import com.tlnt.ui.window.countdown.CountdownPopup;
import com.tlnt.ui.window.dailyquest.DailyActivityWindow;
import com.tlnt.ui.window.forge.ForgeWindow;
import com.tlnt.ui.window.giftcode.GiftcodeEventConfirmDialog;
import com.tlnt.ui.window.giftcode.GiftcodeWindow;
import com.tlnt.ui.window.handbook.HandbookWindow;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;
import com.tlnt.ui.window.mail.MailWindow;
import com.tlnt.ui.window.map.MapWindow;
import com.tlnt.ui.window.market.MarketWindow;
import com.tlnt.ui.window.marketsell.MarketSellWindow;
import com.tlnt.ui.window.party.PartyWindow;
import com.tlnt.ui.window.ranking.RankingWindow;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.ui.window.settings.SettingsWindow;
import com.tlnt.ui.window.shop.ShopWindow;
import com.tlnt.ui.window.skillslot.SkillSlotPopup;
import com.tlnt.ui.window.talent.TalentWindow;
import com.tlnt.ui.window.trade.TradeWindow;
import com.tlnt.ui.window.tribe.TribeWindow;
import com.tlnt.ui.window.wardrobe.WardrobeWindow;
import com.tlnt.ui.window.weaponcraft.WeaponCraftWindow;

public final class WindowManager extends Dialog {
   public WindowManager(RenderContext renderContext) {
      super(renderContext, WindowType.DEFAULT);
      this.addActor(new NpcSelectionPopup(super.renderContext));
      this.addActor(new NpcDialogPopupV2(super.renderContext));
      this.addActor(new RegionInfoPopup(super.renderContext));
      this.addActor(new CharacterWindow(super.renderContext));
      this.addActor(new MarketWindow(super.renderContext));
      this.addActor(new PartyWindow(super.renderContext));
      this.addActor(new GiftcodeWindow(super.renderContext));
      this.addActor(new ChatInputPopup(super.renderContext));
      this.addActor(new ItemInfoPopup(super.renderContext));
      this.addActor(new InvitationConfirmPopup(super.renderContext));
      this.addActor(new GiftcodeEventConfirmDialog(super.renderContext));
      this.addActor(new QuantityInputDialog(super.renderContext));
      this.addActor(new DeathRespawnPopup(super.renderContext));
      this.addActor(new ItemActionMenuPopup(super.renderContext));
      this.addActor(new TribeWindow(super.renderContext));
      this.addActor(new SettingsWindow(super.renderContext));
      this.addActor(new ControlsWindow(super.renderContext));
      this.addActor(new RankingWindow(super.renderContext));
      this.addActor(new com.tlnt.ui.window.ranking.OtherRankWindow(super.renderContext));
      this.addActor(new com.tlnt.ui.window.recharge.RechargeWindow(super.renderContext));
      this.addActor(new com.tlnt.ui.window.worldboss.WorldBossWindow(super.renderContext));
      this.addActor(new MailWindow(super.renderContext));
      this.addActor(new SkillSlotPopup(super.renderContext));
      this.addActor(new ForgeWindow(super.renderContext));
      this.addActor(new ChestWindow(super.renderContext));
      this.addActor(new CountdownPopup(super.renderContext));
      this.addActor(new DailyActivityWindow(super.renderContext));
      this.addActor(new MapWindow(super.renderContext));
      this.addActor(new MarketSellWindow(super.renderContext));
      this.addActor(new TradeWindow(super.renderContext));
      this.addActor(new WardrobeWindow(super.renderContext));
      this.addActor(new BuffTimerTooltip(super.renderContext));
      this.addActor(new ChatWindow(super.renderContext));
      this.addActor(new WeaponCraftWindow(super.renderContext));
      this.addActor(new HandbookWindow(super.renderContext));
      this.addActor(new RewardPopup(super.renderContext));
      this.addActor(new TalentWindow(super.renderContext));
      this.addActor(new ShopWindow(super.renderContext));
      this.setTouchable(Touchable.childrenOnly);
   }

   public final void showDialog(WindowType windowType) {
      SnapshotArray children = new SnapshotArray(this.getChildren());
      Dialog target = null;
      Array.ArrayIterator it = children.iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof Dialog) {
            Dialog dialog;
            if ((dialog = (Dialog)child).windowType == windowType) {
               target = dialog;
            } else if (dialog.isVisible() && windowType.closeable && dialog.windowType.backEnabled) {
               dialog.hide();
            }
         }
      }

      if (target != null) {
         target.toFront();
         target.show();
      }
   }

   public final <T extends Dialog> T findDialog(Class<T> type) {
      for (Actor child : this.getChildren()) {
         if (type.isInstance(child)) {
            return (T)child;
         }
      }

      return null;
   }
}
