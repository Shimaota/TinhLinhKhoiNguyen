package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.window.GamePanel;
import java.text.MessageFormat;

public final class CurrencyBarPanel extends GamePanel {
   public CurrencyBarPanel.CurrencySlot firstCurrencySlot;
   public CurrencyBarPanel.CurrencySlot secondCurrencySlot;
   public CurrencyBarPanel.CurrencySlot thirdCurrencySlot;

   public CurrencyBarPanel(RenderContext renderContext) {
      super(renderContext);
      this.setSize(981.0F, 78.0F);
      this.setPosition(313.0F, -41.0F);
      this.add(this.firstCurrencySlot = new CurrencyBarPanel.CurrencySlot("ui/147", super.renderContext))
         .size(315.0F, 78.0F)
         .space(18.0F);
      this.add(this.secondCurrencySlot = new CurrencyBarPanel.CurrencySlot("ui/148", super.renderContext)).size(315.0F, 78.0F).space(18.0F);
      this.add(this.thirdCurrencySlot = new CurrencyBarPanel.CurrencySlot("ui/146", super.renderContext))
         .size(315.0F, 78.0F)
         .space(18.0F);
      // sapphire slot: opens the "Recharge" window (op 72) if the server has recharge packages
      this.thirdCurrencySlot.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
      this.thirdCurrencySlot.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
         @Override
         public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
            if (GameState.rechargePacks.size == 0) {
               com.tlnt.core.GameMain.postFloatingMessage("Chưa có gói nạp (máy chủ chưa gửi danh sách).");
               return;
            }
            if (com.tlnt.core.GameMain.instance != null) {
               com.tlnt.core.GameMain.instance.getLoginScreen().debugScrollPane.showWindow(com.tlnt.ui.window.WindowType.RECHARGE_POPUP);
               com.tlnt.core.GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
            }
         }
      });
   }

   @Override
   public final void refresh() {
      this.firstCurrencySlot.setAmount(GameState.inventory.inventoryTimestamp1);
      this.secondCurrencySlot.setAmount(GameState.inventory.inventoryTimestamp2);
      this.thirdCurrencySlot.setAmount(GameState.inventory.inventoryTimestamp3);
   }

   public class CurrencySlot extends BaseTable {
      public TextureAsset backgroundTexture;
      public TextureAsset iconTexture;
      public String formattedValue;

      public CurrencySlot(String iconPath, RenderContext renderContext) {
         super(renderContext);
         this.backgroundTexture = TextureAsset.obtain("ui/89");
         this.iconTexture = TextureAsset.obtain(iconPath);
      }

      public final void setAmount(long amount) {
         String pattern = "{0}";
         Object[] formatArgs = new Object[1];
         formatArgs[0] = amount;
         this.formattedValue = MessageFormat.format(pattern, formatArgs);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.drawTexture(this.iconTexture, this.getX() + 40.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.formattedValue,
               this.getX() + this.getWidth() - 23.0F,
               this.getY() + 32.0F,
               Anchor.BOTTOM_RIGHT,
               22.0F,
               Color.WHITE
            );
      }
   }
}
