package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.WindowType;

public class NpcSelectionPopup extends Popup {
   public final Pool<NpcSelectionPopup.OptionButton> optionPool;
   public NpcSelectionPopup.OptionsPanel optionsPanel;
   public NpcSelectionPopup.BackgroundPanel backgroundPanel;

   public NpcSelectionPopup(RenderContext renderContext) {
      super(renderContext, WindowType.MENU_NPC_POPUP, ((float)WorldConfig.VIEWPORT_WIDTH / 2.0F), ((float)WorldConfig.VIEWPORT_HEIGHT / 2.0F), 1, 1034.0F, 796.0F);
      this.optionPool = new SelectionOptionPool(this);
      this.addActor(this.optionsPanel = new NpcSelectionPopup.OptionsPanel(super.renderContext));
      this.addActor(this.backgroundPanel = new NpcSelectionPopup.BackgroundPanel(super.renderContext));
   }

   @Override
   public final boolean isDismissOnOutsideClick() {
      return true;
   }

   @Override
   public final void drawBackground() {
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/44"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
   }

   public final void showWithOptions(byte npcType, int npcId, String npcName, String portraitPath, String[] options) {
      this.optionsPanel.populateOptions(npcType, npcId, options);
   }

   public class BackgroundPanel extends BaseTable {
      public NinePatchAsset backgroundNinePatch;

      public BackgroundPanel(RenderContext renderContext) {
         super(renderContext);
         String bgPath = "ui/100";
         this.backgroundNinePatch = NinePatchAsset.obtain(bgPath, 50);
         this.setSize(816.0F, 169.0F);
         this.setPosition(142.0F, 109.0F);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.backgroundNinePatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      }
   }

   public class OptionButton extends ShapeButton {

      public final NpcSelectionPopup parentPopup = NpcSelectionPopup.this;
      public CompositeSprite buttonSprite;
      public byte npcType;
      public int npcId;
      public byte optionIndex;
      public TextraLabel optionLabel;

      public OptionButton(RenderContext renderContext) {
         super(renderContext);
         String leftPath = "ui/124";
         this.buttonSprite = CompositeSprite.obtain(
            leftPath, "ui/125", "ui/126", false
         );
         this.padTop(7.0F);
         this.add(this.optionLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 24))).grow();
         this.optionLabel.setAlignment(10);
         this.addListener(new NpcSelectionOptionClickListener(this, NpcSelectionPopup.this));
      }

      public final NpcSelectionPopup.OptionButton configure(byte npcType, int npcId, byte optionIndex, String text) {
         this.npcType = npcType;
         this.npcId = npcId;
         this.optionIndex = optionIndex;
         this.optionLabel.setText(text);
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
         this.drawChildren(batch, parentAlpha);
      }
   }

   public class OptionsPanel extends BaseTable {
      public Table optionsTable;

      public OptionsPanel(RenderContext renderContext) {
         super(renderContext);
         this.setSize(420.0F, 393.0F);
         this.setPosition(538.0F, 292.0F);
         ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.optionsTable = new Table());
         this.optionsTable.top();
         this.add(scrollPane).grow();
      }

      public void populateOptions(byte npcType, int npcId, String[] options) {
         Array.ArrayIterator iter = this.optionsTable.getChildren().iterator();

         while (iter.hasNext()) {
            Actor child;
            if ((child = (Actor)iter.next()) instanceof NpcSelectionPopup.OptionButton) {
               NpcSelectionPopup.this.optionPool.free((NpcSelectionPopup.OptionButton)child);
            }
         }

         this.optionsTable.clear();

         for (int i = 0; i < options.length; i++) {
            this.optionsTable
               .add(NpcSelectionPopup.this.optionPool.obtain().configure(npcType, npcId, (byte)i, options[i]))
               .size(420.0F, 58.0F)
               .space(9.0F)
               .row();
         }
      }
   }
}
