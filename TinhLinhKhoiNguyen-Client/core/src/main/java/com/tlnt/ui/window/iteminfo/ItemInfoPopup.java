package com.tlnt.ui.window.iteminfo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.TaskScheduler;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.consignment.ConsignmentItem;
import com.tlnt.model.item.ElementStone;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.StatLine;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.Popup;
import com.tlnt.ui.window.iteminfo.effect.ItemEffectListRow;
import com.tlnt.ui.window.iteminfo.effect.ItemSingleEffectRow;
import com.tlnt.ui.window.iteminfo.effect.SectionDividerLabel;
import com.tlnt.util.GameUtils;
import com.tlnt.util.TimeFormatter;
import java.util.concurrent.TimeUnit;

public class ItemInfoPopup extends Popup {
   public final Pool<ItemInfoPopup.ActionButton> actionButtonPool = new ActionButtonPool(this);
   public final Pool<ItemEffectListRow> ingredientRowPool = new IngredientRowPool(this);
   public final Pool<ItemSingleEffectRow> materialRowPool = new MaterialRowPool(this);
   public Actor sourceButton;
   public long lastDismissedAt;
   public long lastClickTime;
   public int confirmDelayMs;
   public boolean pendingConfirmation;
   public ItemSlotButton itemSlot;
   public TextraLabel itemNameLabel;
   public TextraLabel itemTypeLabel;
   public TextraLabel descriptionLabel;
   public SectionDividerLabel soulCharmSectionHeader;
   public Table soulCharmTable;
   public SectionDividerLabel elementalStoneSectionHeader;
   public Table elementalStoneTable;
   public Table scrollContentTable;
   public Table actionButtonsTable;

   public ItemInfoPopup(RenderContext renderContext) {
      super(
         renderContext,
         WindowType.INFO_ITEM_POPUP,
         (float)WorldConfig.VIEWPORT_WIDTH / 2.0F,
         (float)WorldConfig.VIEWPORT_HEIGHT / 2.0F,
         1,
         689.0F,
         501.0F
      );
      this.confirmDelayMs = 200;
      this.itemSlot = new ItemSlotButton(super.renderContext);
      this.itemSlot.setSize(104.0F, 104.0F);
      this.itemSlot.setPosition(67.0F, 336.0F);
      this.addActor(this.itemSlot);
      Table labelTable;
      (labelTable = new Table())
         .add(this.itemNameLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 26)))
         .grow();
      this.itemNameLabel.setAlignment(10);
      this.itemNameLabel.setColor(Color.valueOf("8B4E23"));
      labelTable.setSize(453.0F, 41.0F);
      labelTable.setPosition(183.0F, 413.0F);
      this.addActor(labelTable);
      (labelTable = new Table())
         .add(this.itemTypeLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 24)))
         .grow();
      this.itemTypeLabel.setAlignment(10);
      this.itemTypeLabel.setColor(Color.valueOf("8B4E23"));
      labelTable.setSize(453.0F, 41.0F);
      labelTable.setPosition(183.0F, 375.0F);
      this.addActor(labelTable);
      ScrollPane descScrollPane = ScrollPaneFactory.createScrollPane(this.scrollContentTable = new Table().top().left());
      this.scrollContentTable
         .add(this.descriptionLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 24, 2.5F)))
         .growX()
         .row();
      this.descriptionLabel.setWrap(true);
      this.descriptionLabel.setAlignment(10);
      descScrollPane.setSize(562.0F, 236.0F);
      descScrollPane.setPosition(67.0F, 90.0F);
      this.addActor(descScrollPane);
      this.soulCharmSectionHeader = new SectionDividerLabel(super.renderContext, "Bùa linh hồn");
      this.soulCharmTable = new Table().top().left().padLeft(10.0F).padTop(-20.0F);
      this.scrollContentTable.add(this.soulCharmTable).growX().row();
      this.elementalStoneSectionHeader = new SectionDividerLabel(super.renderContext, "Đá nguyên tố");
      this.elementalStoneTable = new Table().top().left().padLeft(10.0F);
      this.scrollContentTable.add(this.elementalStoneTable).growX().row();
      ScrollPane actionScrollPane;
      (actionScrollPane = ScrollPaneFactory.createScrollPane(this.actionButtonsTable = new Table().right())).setSize(562.0F, 50.0F);
      actionScrollPane.setPosition(67.0F, 30.0F);
      this.addActor(actionScrollPane);
   }

   public final Item getCurrentItem() {
      return this.itemSlot.getItem();
   }

   public final boolean shouldBlockShopClick() {
      return this.isVisible() || (this.lastDismissedAt > 0L && System.currentTimeMillis() - this.lastDismissedAt < 350L);
   }

   @Override
   public void hide() {
      if (this.isVisible()) {
         this.lastDismissedAt = System.currentTimeMillis();
      }
      super.hide();
   }

   public final ItemInfoPopup.ActionButton findActionButton(ItemInfoPopup.ActionConfig config) {
      Array.ArrayIterator iter = this.actionButtonsTable.getChildren().iterator();

      while (iter.hasNext()) {
         Actor child;
         ItemInfoPopup.ActionButton button;
         if ((child = (Actor)iter.next()) instanceof ItemInfoPopup.ActionButton && (button = (ItemInfoPopup.ActionButton)child).actionConfig == config) {
            return button;
         }
      }

      return null;
   }

   @Override
   public final boolean isDismissOnOutsideClick() {
      return true;
   }

   @Override
   public final void onClose2() {
      if (this.itemSlot.getItem() != null) {
         this.populateItemDetails(this.itemSlot.getItem());
      }
   }

   @Override
   public final void drawBackground() {
      super.renderContext
         .drawTextureSized(TextureAsset.obtain("ui/140"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      Item item;
      if ((item = this.itemSlot.getItem()) != null && item.starGrade != Item.StarGrade.NONE) {

         for (int i = 0; i < item.starCount; i++) {
            RenderContext ctx = super.renderContext;
            TextureAsset starIcon = item.starGrade.starIcon;
            float starsX = this.getX() + 183.0F;
            ctx.drawTextureSized(
               starIcon, starsX + (float)(i * 43), this.getY() + 332.0F, 43.0F, 43.0F, Anchor.BOTTOM_LEFT
            );
         }
      }

      if (item != null && item.template != null) { // the item was Item.clear()ed when the list was resent
         super.renderContext.drawTexture(TextureAsset.obtain(item.template.emblemPath), this.getX() + 565.0F, this.getY() + 405.0F, Anchor.BOTTOM_LEFT);
      }
   }

   public final void showItemWithActions(ShapeButton source, Item item, ItemInfoPopup.ActionConfig doubleClickAction, ItemInfoPopup.ActionConfig... actions) {
      if (item != null) {
         if (this.sourceButton != null && this.sourceButton.equals(source) && !GameUtils.hasElapsed(this.lastClickTime, this.confirmDelayMs) && doubleClickAction != null) {
            doubleClickAction.onItemAction(item);
            this.pendingConfirmation = false;
         } else {
            this.lastClickTime = System.currentTimeMillis();
            this.pendingConfirmation = true;
            this.sourceButton = source;
            this.populateItemDetails(item);
            Array.ArrayIterator iter = this.actionButtonsTable.getChildren().iterator();

            while (iter.hasNext()) {
               Actor child;
               if ((child = (Actor)iter.next()) instanceof ItemInfoPopup.ActionButton) {
                  this.actionButtonPool.free((ItemInfoPopup.ActionButton)child);
               }
            }

            this.actionButtonsTable.clear();
            ItemInfoPopup.ActionConfig[] configs = actions;
            int count = actions.length;

            for (int i = 0; i < count; i++) {
               ItemInfoPopup.ActionConfig config = configs[i];
               this.actionButtonsTable.add(this.actionButtonPool.obtain().bind(config)).size(133.0F, 50.0F).space(10.0F);
            }

            TaskScheduler.schedule(new DelayedShowPopupRunnable(this), 200L, TimeUnit.MILLISECONDS);
         }
      }
   }

   public void populateItemDetails(Item item) {
      this.itemSlot.setItem(item);
      if (item.template != null) {
         this.itemNameLabel.setText("" + item.template.name);
         this.itemTypeLabel.setText(item.starGrade != null ? item.starGrade.displayName : "");
         StringBuilder sb;
         (sb = new StringBuilder()).append("[#8B4E23]");
         sb.append(item.template.description).append("\n");
         if (item.template.requiredClass != null) {
            sb.append("Dành cho: ").append(item.template.requiredClass.name).append("[#8B4E23]\n");
         }

         sb.append("Cấp độ yêu cầu: ").append(item.template.requiredLevel).append("\n");
         if (item.expireTime != -1L) {
            StringBuilder expireSb = sb.append("Hạn sử dụng: ");
            expireSb = expireSb.append(TimeFormatter.formatTimestamp(item.expireTime, "HH:mm dd/MM/yyyy"));
            expireSb.append("\n");
         }

         if (item.statLines != null) {
            StatLine[] statLines = item.statLines;
            int statCount = item.statLines.length;

            for (int i = 0; i < statCount; i++) {
               StatLine statLine = statLines[i];
               sb.append(statLine.formatText()).append("\n");
            }
         }

         Array.ArrayIterator iter = this.soulCharmTable.getChildren().iterator();

         while (iter.hasNext()) {
            Actor child;
            if ((child = (Actor)iter.next()) instanceof ItemEffectListRow) {
               this.ingredientRowPool.free((ItemEffectListRow)child);
            }
         }

         this.soulCharmTable.clear();
         if (item.soulCharms != null) {
            Item[] soulCharms = item.soulCharms;
            int charmCount = item.soulCharms.length;

            for (int j = 0; j < charmCount; j++) {
               Item charm;
               if ((charm = soulCharms[j]) != null) {
                  if (this.soulCharmTable.getChildren().isEmpty()) {
                     this.soulCharmTable.add(this.soulCharmSectionHeader).height(40.0F).growX().row();
                  }

                  this.soulCharmTable.add(this.ingredientRowPool.obtain().bindItem(charm)).growX().row();
               }
            }
         }

         iter = this.elementalStoneTable.getChildren().iterator();

         while (iter.hasNext()) {
            Actor stoneChild;
            if ((stoneChild = (Actor)iter.next()) instanceof ItemSingleEffectRow) {
               this.materialRowPool.free((ItemSingleEffectRow)stoneChild);
            }
         }

         this.elementalStoneTable.clear();
         if (item.elementStones != null) {
            ElementStone[] stones = item.elementStones;
            int stoneCount = item.elementStones.length;

            for (int k = 0; k < stoneCount; k++) {
               ElementStone stone;
               if ((stone = stones[k]) != null) {
                  if (this.elementalStoneTable.getChildren().isEmpty()) {
                     this.elementalStoneTable.add(this.elementalStoneSectionHeader).height(40.0F).growX().row();
                  }

                  this.elementalStoneTable.add(this.materialRowPool.obtain().bindComponent(stone)).growX().row();
               }
            }
         }

         this.descriptionLabel.setText(sb.toString());
      }
   }

   @Override
   public final void show() {
      if (this.sourceButton != null) {
         Actor source = this.sourceButton;
         Vector2 offset = new Vector2(this.sourceButton.getWidth() + 5.0F, 0.0F);
         Actor sourceActor = source;
         Vector2 stagePos = sourceActor.localToStageCoordinates(new Vector2(0.0F, 0.0F));
         Vector2 popupPos = new Vector2(stagePos.x, stagePos.y - this.getHeight() + sourceActor.getHeight());
         this.setPosition(popupPos.x + offset.x, popupPos.y + offset.y);
         if (this.getX() + this.getWidth() > (float)WorldConfig.VIEWPORT_WIDTH) {
            this.setX(this.getX() - this.getWidth() - this.sourceButton.getWidth() - 5.0F);
         }

         if (this.getY() < 0.0F) {
            this.setY(0.0F);
         }
      } else {
         this.setPosition(super.originX, super.originY, super.positionAlignment);
      }

      super.show();
   }

   public class ActionButton extends ShapeButton {
      public ItemInfoPopup.ActionConfig actionConfig;
      public final ItemInfoPopup popup = ItemInfoPopup.this;

      public ActionButton(RenderContext renderContext) {
         super(renderContext);
         this.addListener(new ActionButtonClickListener(this, ItemInfoPopup.this));
      }

      public final ItemInfoPopup.ActionButton bind(ItemInfoPopup.ActionConfig config) {
         this.actionConfig = config;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain("ui/102"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
            );
         if (this.actionConfig != null) {
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  this.actionConfig.label,
                  this.getX() + this.getWidth() / 2.05F,
                  this.getY() + this.getHeight() / 1.8F,
                  Anchor.CENTER,
                  20.0F,
                  Color.WHITE
               );
         }

         super.renderContext.resetBatchColor();
      }
   }

   public abstract static class ActionConfig {
      public String label;
      public Object callbackData;

      public ActionConfig(String label) {
         this.label = label;
      }

      public final ItemInfoPopup.ActionConfig withCallbackData(ConsignmentItem data) {
         this.callbackData = data;
         return this;
      }

      public abstract void onItemAction(Item item);
   }
}
