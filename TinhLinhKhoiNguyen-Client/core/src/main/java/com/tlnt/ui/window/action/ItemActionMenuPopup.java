package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.WindowType;

public class ItemActionMenuPopup extends Popup {
   public Pool<ItemActionMenuPopup.ActionButton> buttonPool = new ContextMenuButtonPool(this);
   public Table buttonTable;

   public ItemActionMenuPopup(RenderContext renderContext) {
      super(
         renderContext,
         WindowType.MENU_POPUP,
         (float)WorldConfig.VIEWPORT_WIDTH / 2.0F,
         (float)WorldConfig.VIEWPORT_HEIGHT / 2.0F,
         1,
         538.0F,
         340.0F
      );
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.buttonTable = new Table().top().left())).setSize(468.0F, 253.0F);
      scrollPane.setPosition(38.0F, 44.0F);
      this.addActor(scrollPane);
   }

   @Override
   public final boolean isDismissOnOutsideClick() {
      return true;
   }

   public final void showWithActions(float x, float y, int alignment, ItemActionMenuPopup.ActionType... actionTypes) {
      Array.ArrayIterator iter = this.buttonTable.getChildren().iterator();

      while (iter.hasNext()) {
         Actor child;
         if ((child = (Actor)iter.next()) instanceof ItemActionMenuPopup.ActionButton) {
            this.buttonPool.free((ItemActionMenuPopup.ActionButton)child);
         }
      }

      this.buttonTable.clear();
      ItemActionMenuPopup.ActionType[] types = actionTypes;
      int count = actionTypes.length;

      for (int i = 0; i < count; i++) {
         ItemActionMenuPopup.ActionType actionType;
         if ((actionType = types[i]) != null) {
            this.buttonTable.add(this.buttonPool.obtain().setAction(actionType.actionCallback).setText(actionType.label)).height(58.0F).growX().space(7.0F).row();
         }
      }

      this.setPosition(x, y, 10);
      GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.MENU_POPUP);
   }

   @Override
   public final void drawBackground() {
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/130"),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            Anchor.BOTTOM_LEFT
         );
   }

   @Override
   public final boolean isModal() {
      return false;
   }

   public class ActionButton extends TextButton {

      public final ItemActionMenuPopup buttonSprite = ItemActionMenuPopup.this;
      public ItemActionMenuPopup.ActionCallback action;

      public ActionButton(RenderContext renderContext) {
         super("", renderContext);
         this.addListener(new PopupMenuItemClickListener(this, ItemActionMenuPopup.this));
      }

      public final ItemActionMenuPopup.ActionButton setAction(ItemActionMenuPopup.ActionCallback callback) {
         this.action = callback;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         String leftPath = "ui/124";
         String middlePath = "ui/125";
         String rightPath = "ui/126";
         super.renderContext
            .drawCompositeSprite(
               CompositeSprite.obtain(leftPath, middlePath, rightPath, false),
               this.getX(),
               this.getY(),
               this.getWidth(),
               this.getHeight(),
               Anchor.BOTTOM_LEFT
            );
         super.renderContext.resetBatchColor();
      }

      @Override
      public final void drawLabel() {
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, super.text, this.getX() + 30.0F, this.getY() + this.getHeight() / 2.0F, Anchor.MIDDLE_LEFT, 24.0F, Color.WHITE);
      }
   }

   public abstract static class ActionCallback {
      public Object itemId;

      public final ItemActionMenuPopup.ActionCallback setItemId(Object itemId) {
         this.itemId = itemId;
         return this;
      }

      public abstract void execute();
   }

   public static enum ActionType {
      UNLOCK_ITEM("Thách thức", new UnlockItemContextAction()),
      SKILL_UPGRADE("Giao dịch", new SkillUpgradeContextAction()),
      FORGE_UPGRADE("Kết bạn", new ForgeUpgradeContextAction()),
      ENCHANT_ITEM("Xem thông tin", new TransferToStorageMenuAction()),
      SHOP_SELL("Mời vào bộ lạc", new SellItemMenuAction()),
      SPLIT_ITEM("Nhường trưởng nhóm", new SplitItemContextAction()),
      DESTROY_ITEM("Đuổi khỏi nhóm", new DestroyItemContextAction()),
      USE_ITEM("Mời vào tổ đội", new LeaderboardActionCallback()),
      SHOP_BUY("Phong phó Tù trưởng", new ItemContextMenuAction()),
      TRADE_ITEM("Cắt chức phó Tù trưởng", new ViewPlayerInfoMenuAction()),
      SHOP_ACTION("Nhường Tù trưởng", new DropItemCallbackAction()),
      TRIBE_DONATE("Đuổi khỏi bộ lạc", new TribeDonateActionCallback()),
      STORAGE_DEPOSIT("Xóa bạn bè", new InviteToPartyMenuAction());

      public String label;
      public ItemActionMenuPopup.ActionCallback actionCallback;

      ActionType(String label, ItemActionMenuPopup.ActionCallback callback) {
         this.label = label;
         this.actionCallback = callback;
      }

      public final ItemActionMenuPopup.ActionType withItemId(Object itemId) {
         this.actionCallback.setItemId(itemId);
         return this;
      }

   }
}
