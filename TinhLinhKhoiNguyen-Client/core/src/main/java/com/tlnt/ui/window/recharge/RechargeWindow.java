package com.tlnt.ui.window.recharge;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;

/**
 * "Recharge" window (op 72 — port of decompiled C0061 / C3757, {@code RECHARGE_POPUP}): package grid (5 per row), each package: name,
 * icon, "Receive now" + description, price button "X VNĐ". Clicking a package -> C→S 72/0 int id, the server replies 72/1 link -> open the browser.
 * Opened by clicking the sapphire slot in the currency bar ({@code CurrencyBarPanel}).
 */
public final class RechargeWindow extends TabbedWindow {

   /** One recharge package (decompiled C3998). */
   public static final class Pack {
      public final int id;
      public final String name;
      public final String icon;
      public final int price;
      public final String desc;

      public Pack(int id, String name, String icon, int price, String desc) {
         this.id = id;
         this.name = name;
         this.icon = icon;
         this.price = price;
         this.desc = desc;
      }
   }

   public RechargeWindow(RenderContext ctx) {
      super(ctx, WindowType.RECHARGE_POPUP);
      // decompiled C0061: desktop = web recharge tab (C3757), phone = app-store style tab (C3756: image ui/222,
      // no package name at the top). This build has no in-app purchases -> both send op 72 to get the recharge page link.
      boolean desktop = com.badlogic.gdx.Gdx.app == null
         || com.badlogic.gdx.Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Desktop;
      this.addTab("Nạp tiền", "icon/1", new PackListPanel(super.renderContext, !desktop));
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

   /** Package grid. */
   static final class PackListPanel extends GamePanel {
      private final Table table = new Table().top().left();
      private final Pool<PackButton> pool;

      PackListPanel(RenderContext ctx, boolean storeStyle) {
         super(ctx);
         final RenderContext rc = ctx;
         this.pool = new Pool<PackButton>() {
            @Override
            protected PackButton newObject() {
               return new PackButton(rc, storeStyle);
            }
         };
         this.add(ScrollPaneFactory.createScrollPane(this.table)).grow();
      }

      @Override
      public final void refresh() {
         Array.ArrayIterator<Actor> it = this.table.getChildren().iterator();

         while (it.hasNext()) {
            Actor child = it.next();
            if (child instanceof PackButton) {
               this.pool.free((PackButton)child);
            }
         }

         this.table.clear();

         for (Pack pack : GameState.rechargePacks) {
            this.table.add(this.pool.obtain().bind(pack)).size(235.0F, 317.0F).space(18.0F);
            if (this.table.getChildren().size % 5 == 0) {
               this.table.row();
            }
         }
      }
   }

   /** One package cell (decompiled C3757 inner class). */
   static final class PackButton extends ShapeButton {
      private final TextureAsset background = TextureAsset.obtain("ui/216");
      private final TextureAsset header = TextureAsset.obtain("ui/217");
      private final CompositeSprite priceButton = CompositeSprite.obtain("ui/10", "ui/11", "ui/12", false);
      private Pack pack;

      /** C3756 style (phone): image ui/222 instead of the package icon, no package name drawn at the top. */
      private final boolean storeStyle;

      PackButton(RenderContext ctx, boolean storeStyle) {
         super(ctx);
         this.storeStyle = storeStyle;
         this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
               if (PackButton.this.pack != null) {
                  ServerPacketSender.getInstance().sendRechargePack(PackButton.this.pack.id);
                  com.tlnt.core.GameMain.postFloatingMessage("Đang mở trang nạp " + PackButton.this.pack.name + "...");
               }
            }
         });
      }

      /** "20,000 VNĐ", computed once when the package is bound (no string formatting every frame). */
      private String priceText = "";

      PackButton bind(Pack pack) {
         this.pack = pack;
         this.priceText = String.format(java.util.Locale.US, "%,d", pack.price) + " VNĐ";
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.pack == null) {
            return;
         }

         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         float cx = this.getX() + this.getWidth() / 2.0F;
         super.renderContext.drawTextureSized(this.background, this.getX(), this.getY(), 235.0F, 300.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.drawTexture(this.header, cx, this.getY() + 261.0F, Anchor.BOTTOM_CENTER);
         if (!this.storeStyle) {
            super.renderContext.drawText(FontManager.DEFAULT_FONT, this.pack.name, cx, this.getY() + 288.0F, Anchor.BOTTOM_CENTER, 24.0F, Color.WHITE);
         }
         String icon = this.storeStyle ? "ui/222" : this.pack.icon;
         if (icon != null && !icon.isEmpty()) {
            super.renderContext.drawTexture(TextureAsset.obtain(icon), cx, this.getY() + 150.0F, Anchor.BOTTOM_CENTER);
         }

         super.renderContext.drawTextColorName(FontManager.PLAIN_FONT, "Nhận ngay", cx, this.getY() + 125.0F, Anchor.BOTTOM_CENTER, 22.0F, "#8B4E23");
         super.renderContext.drawTextColorName(FontManager.PLAIN_FONT, this.pack.desc, cx, this.getY() + 90.0F, Anchor.BOTTOM_CENTER, 22.0F, "#8B4E23");
         super.renderContext.drawCompositeSprite(this.priceButton, cx, this.getY() + 10.0F, 190.0F, 63.0F, Anchor.BOTTOM_CENTER);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.priceText,
               cx,
               this.getY() + 34.0F,
               Anchor.BOTTOM_CENTER,
               24.0F,
               Color.WHITE
            );
         super.renderContext.resetBatchColor();
      }
   }
}
