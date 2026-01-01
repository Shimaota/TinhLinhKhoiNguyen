package com.tlnt.ui.window.countdown;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.TitledPopup;

public class CountdownPopup extends TitledPopup {
   public Pool<CountdownPopup.CountdownOptionButton> OVERLAY_COLOR = new CountdownOptionButtonPool(this);
   public Table optionsTable;

   public CountdownPopup(RenderContext ctx) {
      super(ctx, WindowType.VIP_POPUP, "Tiện ích");
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.optionsTable = new Table().top())).setSize(733.0F, 330.0F);
      scrollPane.setPosition(176.0F, 179.0F);
      this.addActor(scrollPane);
   }

   @Override
   public final void drawBackground() {
      super.drawBackground();
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      long expiryTime = GameState.appleEventState.expiryTime;
      long now = System.currentTimeMillis();
      long remainingMs;
      String remainingText;
      if ((remainingMs = expiryTime - now) <= 0L) {
         remainingText = "";
      } else {
         long rem;
         long years = (rem = remainingMs / 1000L) / 31536000L;
         long remAfterYears;
         long months = (remAfterYears = rem % 31536000L) / 2592000L;
         long days = (rem = remAfterYears % 2592000L) / 86400L;
         long remAfterDays;
         long hours = (remAfterDays = rem % 86400L) / 3600L;
         long minutes = (rem = remAfterDays % 3600L) / 60L;
         long secondsValue;
         long seconds = secondsValue = rem % 60L;
         StringBuilder sb = new StringBuilder();
         if (years > 0L) {
            sb.append(years).append(" năm ");
         }

         if (months > 0L) {
            sb.append(months).append(" tháng ");
         }

         if (days > 0L) {
            StringBuilder sbDays = sb.append(days);
            sbDays.append(" ngày ");
         }

         if (hours > 0L) {
            StringBuilder sbHours = sb.append(hours);
            sbHours.append(" giờ ");
         }

         if (minutes > 0L) {
            StringBuilder sbMinutes = sb.append(minutes);
            sbMinutes.append(" phút ");
         }

         StringBuilder sbSeconds = sb.append(seconds);
         sbSeconds.append(" giây");
         remainingText = sb.toString().trim();
      }

      ctx.drawTextColorName(
         font,
         "Còn lại: " + remainingText,
         this.getX() + this.getWidth() - 100.0F,
         this.getY() + this.getHeight() - 120.0F,
         Anchor.BOTTOM_RIGHT,
         22.0F,
         "#73411E"
      );
   }

   public final void setCountdownOptions(String... options) {
      Array.ArrayIterator childIter = this.optionsTable.getChildren().iterator();

      while (childIter.hasNext()) {
         Actor child;
         if ((child = (Actor)childIter.next()) instanceof CountdownPopup.CountdownOptionButton) {
            this.OVERLAY_COLOR.free((CountdownPopup.CountdownOptionButton)child);
         }
      }

      this.optionsTable.clear();
      if (options.length != 0) {

         for (int i = 0; i < options.length; i++) {
            this.optionsTable.add(this.OVERLAY_COLOR.obtain().setOption(i, options[i])).size(730.0F, 58.0F).space(10.0F).row();
         }

         GameMain.instance.getLoginScreen().debugScrollPane.showWindow(super.windowType);
      }
   }

   public class CountdownOptionButton extends TextButton {
      public CompositeSprite buttonSprite;
      public int optionIndex;

      public CountdownOptionButton(String text, RenderContext ctx) {
         super(text, ctx);
         String leftPath = "ui/131";
         String middlePath = "ui/132";
         this.buttonSprite = CompositeSprite.obtain(leftPath, middlePath, "ui/133", false);
         this.addListener(new CountdownOptionClickListener(this, CountdownPopup.this));
      }

      public final CountdownPopup.CountdownOptionButton setOption(int index, String text) {
         this.optionIndex = index;
         this.setText(text);
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }

      @Override
      public final void drawLabel() {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               super.text,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               Anchor.CENTER,
               22.0F,
               Color.WHITE
            );
      }
   }
}
