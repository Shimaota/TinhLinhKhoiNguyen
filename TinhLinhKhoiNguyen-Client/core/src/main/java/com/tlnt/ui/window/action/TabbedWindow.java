package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.hud.widget.TargetMarker;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.WindowType;

public abstract class TabbedWindow extends Popup {
   public static final Color OVERLAY_COLOR = new Color(0.0F, 0.0F, 0.0F, 0.5F);
   public Group contentGroup;
   public ButtonGroup<TabbedWindow.TabButton> tabButtonGroup;
   public Table tabNavTable;
   public final Array<GamePanel> panels;
   public final Array<TabbedWindow.TabButton> tabButtons;
   public final Array<TabbedWindow.TabVisibilityProvider> tabVisibilityCallbacks;

   public TabbedWindow(RenderContext renderContext, WindowType windowType) {
      super(renderContext, windowType, ((float)WorldConfig.VIEWPORT_WIDTH / 2.1F), ((float)WorldConfig.VIEWPORT_HEIGHT / 2.0F), 1, 1525.0F, 919.0F);
      this.panels = new Array<>();
      this.tabButtons = new Array<>();
      this.tabVisibilityCallbacks = new Array<>();
      this.addActor(new TabbedWindow.CloseButton(super.renderContext));
      this.tabButtonGroup = new ButtonGroup<>();
      this.contentGroup = new Group();
      this.contentGroup.setSize(1247.0F, 610.0F);
      this.contentGroup.setPosition(180.0F, 163.0F);
      this.addActor(this.contentGroup);
      this.tabNavTable = new Table().top();
      this.tabNavTable.setSize(184.0F, 555.0F);
      this.tabNavTable.setPosition(-46.0F, 184.0F);
      this.addActor(this.tabNavTable);
   }

   public final <T extends GamePanel> T findPanelByClass(Class<T> panelClass) {
      for (Actor child : this.contentGroup.getChildren()) {
         if (panelClass.isInstance(child)) {
            return (T)panelClass.cast(child);
         }
      }

      return null;
   }

   public final <T extends GamePanel> TabbedWindow.TabButton findTabButtonByPanelClass(Class<T> panelClass) {
      for (int i = 0; i < this.panels.size; i++) {
         GamePanel panel = this.panels.get(i);
         if (panelClass.isInstance(panel)) {
            return this.tabButtons.get(i);
         }
      }

      return null;
   }

   @Override
   public void onOpen() {
      AudioManager.playSound("menu_open", null);
   }

   @Override
   public void onClose() {
      AudioManager.playSound("menu_close", null);
   }

   @Override
   public final void show() {
      this.clearActions();
      super.show();
      this.addAction(
         Actions.sequence(
            Actions.moveToAligned(super.originX, -this.getHeight(), super.positionAlignment),
            Actions.moveToAligned(super.originX, super.originY, super.positionAlignment, 0.3F, Interpolation.smoother)
         )
      );
   }

   @Override
   public final void hide() {
      this.clearActions();
      this.addAction(
         Actions.sequence(
            Actions.moveToAligned(super.originX, super.originY, super.positionAlignment),
            Actions.moveToAligned(super.originX, -this.getHeight(), super.positionAlignment, 0.3F, Interpolation.smoother),
            Actions.visible(false)
         )
      );
   }

   public final void addTab(String label, String iconPath, GamePanel panel) {
      this.addTabFull(label, iconPath, panel, null, null);
   }

   public final void addTabWithCallback(String label, String iconPath, GamePanel panel, TabbedWindow.TabVisibilityProvider visibilityProvider) {
      this.addTabFull(label, iconPath, panel, visibilityProvider, null);
   }

   public final void addTabFull(String label, String iconPath, GamePanel panel, TabbedWindow.TabVisibilityProvider visibilityProvider, TargetMarker.BadgeCondition markerConfig) {
      if (panel != null) {
         panel.setFillParent(true);
         this.contentGroup.addActor(panel);
         this.panels.add(panel);
         TabbedWindow.TabButton tabButton = new TabbedWindow.TabButton(super.renderContext, label, iconPath, markerConfig);
         this.tabButtonGroup.add(tabButton);
         tabButton.addListener(new TabVisibilityToggleListener(this, panel));
         if (panel.isVisible() != tabButton.isChecked()) {
            panel.setVisible(tabButton.isChecked());
         }

         this.tabButtons.add(tabButton);
         this.tabVisibilityCallbacks.add(visibilityProvider);
         this.refreshTabLayout();
      }
   }

   public void refreshTabLayout() {
      this.tabNavTable.clear();
      Array.ArrayIterator iter = this.tabButtons.iterator();

      while (iter.hasNext()) {
         TabbedWindow.TabButton tabButton;
         if ((tabButton = (TabbedWindow.TabButton)iter.next()).isVisible()) {
            this.tabNavTable.add(tabButton).size(184.0F, 80.0F).space(15.0F).row();
         }
      }
   }

   public final void selectTabByIndex(int index) {
      if (index >= 0) {
         if (index <= this.tabButtons.size - 1) {
            TabbedWindow.TabButton tabButton = this.tabButtons.get(index);
            tabButton.setChecked(true);

            for (Actor child : this.contentGroup.getChildren()) {
               child.setVisible(false);
            }

            this.contentGroup.getChildren().get(index).setVisible(true);
            return;
         }
      }
   }

   @Override
   public final void drawBackground() {
      super.renderContext.drawLine(0.0F, 0.0F, (float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT, OVERLAY_COLOR);
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/72"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
   }

   @Override
   public final void onClose2() {
      for (int i = 0; i < this.tabVisibilityCallbacks.size; i++) {
         if (this.tabVisibilityCallbacks.get(i) != null) {
            boolean visible = this.tabVisibilityCallbacks.get(i).isTabVisible();
            TabbedWindow window = this;
            if (i >= 0) {
               if (i <= this.tabButtons.size - 1 && this.tabButtons.get(i).isVisible() != visible) {
                  if (visible) {
                     this.tabButtons.get(i).setVisible(true);
                     int checkedHidden = 0;

                     for (int j = 0; j < window.tabButtons.size; j++) {
                        if (window.tabButtons.get(j).isChecked() && !window.tabButtons.get(j).isVisible()) {
                           checkedHidden = 1;
                           break;
                        }
                     }

                     if (checkedHidden != 0) {

                        for (int k = 0; k < window.tabButtons.size; k++) {
                           if (window.tabButtons.get(k).isVisible()) {
                              window.selectTabByIndex(k);
                              break;
                           }
                        }
                     }
                  } else {
                     TabbedWindow.TabButton hiddenTab = this.tabButtons.get(i);
                     hiddenTab.setVisible(false);
                     if (this.tabButtons.get(i).isChecked()) {
                        this.contentGroup.getChild(i).setVisible(false);

                        for (int l = 0; l < window.tabButtons.size; l++) {
                           if (window.tabButtons.get(l).isVisible()) {
                              window.selectTabByIndex(l);
                              break;
                           }
                        }
                     }
                  }

                  window.refreshTabLayout();
               }
            }
         }
      }

      this.onUpdate();
   }

   public void onUpdate() {
   }

   public final int getTabCount() {
      return this.contentGroup.getChildren().size;
   }

   public final void updateTabLabel(int index, String label, String iconPath) {
      if (index >= 0 && index <= this.tabButtons.size - 1) {
         this.tabButtons.get(index).setTabLabel(label).setTabIcon(iconPath);
      }
   }

   public final void clearAllTabs() {
      this.panels.clear();
      this.contentGroup.clearChildren();
      Array.ArrayIterator iter = this.tabButtons.iterator();

      while (iter.hasNext()) {
         ((TabbedWindow.TabButton)iter.next()).remove();
      }

      this.tabButtonGroup.clear();
      this.tabButtons.clear();
      this.tabVisibilityCallbacks.clear();
      this.tabNavTable.clear();
   }

   public final Array<GamePanel> getPanels() {
      return this.panels;
   }

   public class CloseButton extends ShapeButton {

      public final TabbedWindow tabbedWindow = TabbedWindow.this;

      public CloseButton(RenderContext renderContext) {
         super(renderContext);
         this.setSize(66.0F, 67.0F);
         this.setPosition(1448.0F, 840.0F);
         this.addListener(new TabbedWindowCloseClickListener(this, TabbedWindow.this));
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawTextureSized(TextureAsset.obtain("ui/65"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }
   }

   public static class TabButton extends ShapeButton {
      public static final Color TITLE_COLOR = new Color(0.54509807F, 0.30588236F, 0.13725491F, 1.0F);
      public TextureAsset activeTabTexture;
      public TextureAsset inactiveTabTexture;
      public String tabLabel;
      public TextureAsset tabIconTexture;

      public TabButton(RenderContext renderContext, String label, String iconPath, TargetMarker.BadgeCondition markerConfig) {
         super(renderContext);
         this.activeTabTexture = TextureAsset.obtain("ui/75");
         this.inactiveTabTexture = TextureAsset.obtain("ui/74");
         this.tabLabel = label;
         this.tabIconTexture = TextureAsset.obtain(iconPath);
         TargetMarker marker = new TargetMarker(super.renderContext, markerConfig);
         this.add(marker).size(15.0F).expand().top().right();
      }

      public final TabbedWindow.TabButton setTabLabel(String label) {
         this.tabLabel = label;
         return this;
      }

      public final TabbedWindow.TabButton setTabIcon(String iconPath) {
         this.tabIconTexture = TextureAsset.obtain(iconPath);
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         if (this.isChecked()) {
            super.renderContext.drawTextureSized(this.activeTabTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
            super.renderContext
               .drawText(
                  FontManager.PLAIN_FONT,
                  this.tabLabel,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  Anchor.CENTER,
                  30.0F,
                  TITLE_COLOR
               );
         } else {
            super.renderContext
               .drawTextureSized(
                  this.inactiveTabTexture, this.getX() + this.getWidth(), this.getY(), this.getWidth() / 2.0F, this.getHeight(), Anchor.BOTTOM_RIGHT
               );
            super.renderContext.drawTexture(this.tabIconTexture, this.getX() + 140.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
         }

         super.renderContext.resetBatchColor();
         super.drawChildren(batch, parentAlpha);
      }

   }

   public interface TabVisibilityProvider {
      boolean isTabVisible();
   }
}
