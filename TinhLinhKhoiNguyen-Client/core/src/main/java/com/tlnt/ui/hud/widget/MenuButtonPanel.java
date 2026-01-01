package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;

public final class MenuButtonPanel extends GamePanel {
   public MenuButtonPanel.MenuIconButton[][] buttonGrid;

   public MenuButtonPanel(RenderContext renderContext) {
      super(renderContext);
      this.setSize(453.0F, 285.0F);
      if (Gdx.app.getType() != Application.ApplicationType.iOS) {
         this.setPosition(
            (float)(WorldConfig.VIEWPORT_WIDTH - 25),
            (float)(WorldConfig.VIEWPORT_HEIGHT - 88),
            18
         );
      } else {
         this.setPosition(
            (float)(WorldConfig.VIEWPORT_WIDTH - 127),
            (float)(WorldConfig.VIEWPORT_HEIGHT - 103),
            18
         );
      }
      MenuButtonPanel panel = this;
      MenuButtonPanel.MenuIconButton[][] grid = new MenuButtonPanel.MenuIconButton[3][];
      MenuButtonPanel.MenuIconButton[] row = new MenuButtonPanel.MenuIconButton[4];
      row[0] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/82");
      row[1] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/80");
      row[2] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/85");
      row[3] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/79");
      grid[0] = row;
      row = new MenuButtonPanel.MenuIconButton[4];
      row[0] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/84");
      row[1] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/86");
      row[2] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/83");
      row[3] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/76");
      grid[1] = row;
      row = new MenuButtonPanel.MenuIconButton[4];
      row[0] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/77");
      row[1] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/199");
      row[2] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/78");
      row[3] = new MenuButtonPanel.MenuIconButton(super.renderContext, "ui/81");
      grid[2] = row;
      this.buttonGrid = grid;

      for (int i = 0; i < panel.buttonGrid.length; i++) {
         for (int j = 0; j < panel.buttonGrid[i].length; j++) {
            panel.add(panel.buttonGrid[i][j]).grow().uniform();
         }

         panel.row();
      }

      MenuButtonPanel.MenuIconButton[] buttons = panel.buttonGrid[0];
      buttons[0].addListener(new OpenRechargeWindowListener()); // ui/82 -> Recharge (decompiled C3642)
      buttons[1].addListener(new OpenQuestPartyWindowListener(panel));
      panel.buttonGrid[0][2]
         .addNotificationBadge(new MenuButtonVisibilityChecker(panel))
         .addListener(new QuestWindowClickListener(panel));
      buttons = panel.buttonGrid[0];
      buttons[3].addNotificationBadge(new GiftcodeBadgeChecker(panel)).addListener(new WelfareWindowClickListener(panel));
      buttons = panel.buttonGrid[1];
      buttons[0].addNotificationBadge(new MenuButtonNotificationChecker(panel)).addListener(new TeamWindowClickListener(panel));
      panel.buttonGrid[1][1].addListener(new OpenTribeWindowClickListener(panel));
      panel.buttonGrid[1][2].addListener(new OpenMailWindowListener(panel));
      buttons = panel.buttonGrid[1];
      buttons[3].addNotificationBadge(new MailBadgeChecker(panel)).addListener(new MailPopupClickListener(panel));
      panel.buttonGrid[2][1].addListener(new OpenTalentWindowClickListener(panel));
      panel.buttonGrid[2][2].addListener(new HandbookPopupClickListener(panel));
      panel.buttonGrid[2][3].addListener(new OpenRankWindowListener(panel));
      panel.buttonGrid[2][0].addListener(new EventPopupClickListener(panel));
   }

   public final MenuButtonPanel.MenuIconButton getRankButton(int row, int col) {
      return this.buttonGrid[2][3];
   }

   public class MenuIconButton extends ShapeButton {
      public TextureAsset icon;

      public MenuIconButton(RenderContext renderContext, String iconPath) {
         super(renderContext);
         this.icon = TextureAsset.obtain(iconPath);
      }

      public final MenuButtonPanel.MenuIconButton addNotificationBadge(TargetMarker.BadgeCondition condition) {
         TargetMarker marker = new TargetMarker(super.renderContext, condition);
         this.add(marker).size(15.0F).expand().top().right().padTop(20.0F).padRight(20.0F);
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext.drawTexture(this.icon, this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
         super.renderContext.resetBatchColor();
         super.drawChildren(batch, parentAlpha);
      }
   }
}
