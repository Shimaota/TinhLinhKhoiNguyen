package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.GamePanel;

public final class LuckyWheelPanel extends GamePanel {
   public Array<ItemSlotButton> rewardSlots = new Array<>();

   public LuckyWheelPanel(RenderContext renderContext) {
      super(renderContext);
      LuckyWheelPanel panel = this;
      // Following decompiled C3710: Lucky Coin counter, a (hidden) legend box + a "?" button toggling that box.
      CoinCountBox coinBox = new CoinCountBox();
      coinBox.setSize(202.0F, 53.0F);
      coinBox.setPosition(29.0F, 54.0F);
      this.addActor(coinBox);
      CoinHelpBox helpBox = new CoinHelpBox();
      helpBox.setSize(240.0F, 160.0F);
      helpBox.setPosition(29.0F, 164.0F);
      this.addActor(helpBox);
      helpBox.setVisible(false);
      HelpButton helpButton = new HelpButton(super.renderContext);
      helpButton.setSize(30.0F, 30.0F);
      helpButton.setPosition(241.0F, 121.0F);
      this.addActor(helpButton);
      helpButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
         @Override
         public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
            helpBox.setVisible(!helpBox.isVisible());
         }
      });
      Table slotTable;
      (slotTable = new Table()).setSize(580.0F, 342.0F);
      slotTable.setPosition(334.0F, 111.0F);
      this.addActor(slotTable);
      ItemSlotButton slot;
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(0.0F, 238.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(119.0F, 238.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(238.0F, 238.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(357.0F, 238.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(476.0F, 238.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(476.0F, 119.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(476.0F, 0.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(357.0F, 0.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(238.0F, 0.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(119.0F, 0.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(0.0F, 0.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      (slot = new ItemSlotButton(super.renderContext, ItemSlotButton.SlotStyle.SPECIAL)).setSize(104.0F, 104.0F);
      slot.setPosition(0.0F, 119.0F);
      slotTable.addActor(slot);
      this.rewardSlots.add(slot);
      ButtonGroup buttonGroup = new ButtonGroup();

      for (ItemSlotButton rewardSlot : this.rewardSlots) {
         buttonGroup.add(rewardSlot);
         rewardSlot.addListener(new LuckyWheelItemSelectListener(panel, rewardSlot));
      }

      SpinOnceButton spinOnceButton;
      (spinOnceButton = new SpinOnceButton(panel, "Quay 1 lần", panel.renderContext)).setSize(169.0F, 63.0F);
      spinOnceButton.setPosition(445.0F, 28.0F);
      panel.addActor(spinOnceButton);
      spinOnceButton.addListener(new SpinOnceClickListener(panel));
      SpinTenButton spinTenButton;
      (spinTenButton = new SpinTenButton(panel, "Quay 10 lần", panel.renderContext)).setSize(169.0F, 63.0F);
      spinTenButton.setPosition(634.0F, 28.0F);
      panel.addActor(spinTenButton);
      spinTenButton.addListener(new SpinTenClickListener(panel));
   }

   @Override
   public final void onShow() {
      ServerPacketSender.getInstance().sendRequestNotification();
   }

   @Override
   public final void refresh() {
      for (int i = 0;
         i < GameState.luckyWheelData.rewardItems.size && i <= this.rewardSlots.size - 1;
         i++
      ) {
         this.rewardSlots.get(i).setItem(GameState.luckyWheelData.rewardItems.get(i));
      }

      this.rewardSlots.get(GameState.luckyWheelData.winningSlotIndex).setChecked(true);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext
         .drawTextureSized(TextureAsset.obtain("ui/BG3"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.DEFAULT_FONT;
      String title = "Vòng quay may mắn";
      float titleX = this.getX() + this.getWidth() / 2.0F;
      float titleY = this.getY() + 576.0F;
      ctx.drawTextColorName(font, title, titleX, titleY, Anchor.BOTTOM_CENTER, 30.0F, "#FFF600");
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Cần 1 Đồng xu may mắn để quay",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + 517.0F,
            Anchor.BOTTOM_CENTER,
            24.0F,
            Color.WHITE
         );
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Có thể nhận được các vật phẩm sau",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + 479.0F,
            Anchor.BOTTOM_CENTER,
            24.0F,
            Color.WHITE
         );
      super.renderContext.drawText(
         FontManager.DEFAULT_FONT, "Đồng xu may mắn", this.getX() + 29.0F, this.getY() + 128.0F, Anchor.BOTTOM_LEFT, 24.0F, Color.WHITE
      );
      super.draw(batch, parentAlpha);
   }

   /** Lucky Coin counter: frame ui/164, icon/737, number of item 307 in the inventory. */
   private final class CoinCountBox extends com.badlogic.gdx.scenes.scene2d.Actor {
      @Override
      public void draw(Batch batch, float parentAlpha) {
         RenderContext ctx = LuckyWheelPanel.this.renderContext;
         ctx.drawNinePatch(com.tlnt.engine.render.NinePatchAsset.obtain("ui/164", 10), this.getX(), this.getY(), this.getWidth(),
            this.getHeight(), Anchor.BOTTOM_LEFT);
         ctx.drawTextureSized(TextureAsset.obtain("icon/737"), this.getX(), this.getY(), 56.0F, 53.0F, Anchor.BOTTOM_LEFT);
         ctx.drawText(FontManager.DEFAULT_FONT, Integer.toString(GameState.inventory.luckyCoinCount),
            this.getX() + this.getWidth() / 2.0F + 30.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER, 24.0F, Color.WHITE);
      }
   }

   /** Lucky Coin legend box (hidden, toggled by the "?" button). */
   private final class CoinHelpBox extends com.badlogic.gdx.scenes.scene2d.Actor {
      @Override
      public void draw(Batch batch, float parentAlpha) {
         RenderContext ctx = LuckyWheelPanel.this.renderContext;
         ctx.drawNinePatch(com.tlnt.engine.render.NinePatchAsset.obtain("ui/164", 10), this.getX(), this.getY(), this.getWidth(),
            this.getHeight(), Anchor.BOTTOM_LEFT);
         ctx.drawText(FontManager.DEFAULT_FONT, "Đồng xu may mắn", this.getX() + this.getWidth() / 2.0F, this.getY() + 130.0F,
            Anchor.BOTTOM_CENTER, 18.0F, Color.WHITE);
         ctx.drawText(FontManager.DEFAULT_FONT, "Có thể mua tại cửa hàng\ntạp hóa Selami hoặc tham\ngia các hoạt động hàng\nngày để nhận được",
            this.getX() + 10.0F, this.getY() + 18.0F, Anchor.BOTTOM_LEFT, 18.0F, Color.WHITE);
      }
   }

   /** "?" button ui/231. */
   private static final class HelpButton extends com.tlnt.ui.widget.ShapeButton {
      HelpButton(RenderContext ctx) {
         super(ctx);
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            this.renderContext.setBatchColor(Color.GRAY);
         }
         this.renderContext.drawTextureSized(TextureAsset.obtain("ui/231"), this.getX(), this.getY(), this.getWidth(), this.getHeight(),
            Anchor.BOTTOM_LEFT);
         this.renderContext.resetBatchColor();
      }
   }

}
