package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.WindowType;

public class NpcDialogPopupV2 extends Popup {
   public Pool<NpcDialogPopupV2.NpcChoiceButton> choicePool;
   public NinePatchAsset backgroundNinePatch;
   public TextraLabel npcNameLabel;
   public TextureAsset npcPortraitTexture;
   public TypingLabel dialogTextLabel;
   public int npcType;
   public int npcId;
   public Table choicesTable;

   public NpcDialogPopupV2(RenderContext renderContext) {
      super(renderContext, WindowType.MENU_NPC_POPUP_V2, ((float)WorldConfig.VIEWPORT_WIDTH / 2.0F), ((float)WorldConfig.VIEWPORT_HEIGHT / 2.0F), 1, (float)WorldConfig.VIEWPORT_WIDTH, 373.0F);
      this.choicePool = new NpcDialogChoicePool(this);
      String bgPath = "ui/73";
      this.backgroundNinePatch = NinePatchAsset.obtain(bgPath, 20);
      this.npcNameLabel = new TextraLabel("[#FFF600]", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 30));
      this.npcNameLabel.setAlignment(10);
      this.npcNameLabel.setPosition(435.0F, 370.0F);
      this.addActor(this.npcNameLabel);
      Table textTable = new Table().padBottom(20.0F);
      this.dialogTextLabel = new TypingLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 2.5F));
      this.dialogTextLabel.setAlignment(10);
      this.dialogTextLabel.setWrap(true);
      textTable.add(this.dialogTextLabel).grow();
      ScrollPane textScrollPane;
      (textScrollPane = ScrollPaneFactory.createScrollPane(textTable)).setSize(1453.0F, 170.0F);
      textScrollPane.setPosition(435.0F, 128.0F);
      this.addActor(textScrollPane);
      ScrollPane choicesScrollPane;
      (choicesScrollPane = ScrollPaneFactory.createScrollPane(this.choicesTable = new Table().right())).setSize(1453.0F, 78.0F);
      choicesScrollPane.setPosition(435.0F, 35.0F);
      this.addActor(choicesScrollPane);
   }

   public final NpcDialogPopupV2.NpcChoiceButton findChoiceButton(String text) {
      Array.ArrayIterator iter = this.choicesTable.getChildren().iterator();

      while (iter.hasNext()) {
         Actor child;
         if ((child = (Actor)iter.next()) instanceof NpcDialogPopupV2.NpcChoiceButton && ((NpcDialogPopupV2.NpcChoiceButton)child).getText().equals(text)) {
            return (NpcDialogPopupV2.NpcChoiceButton)child;
         }
      }

      return null;
   }

   @Override
   public final boolean isDismissOnOutsideClick() {
      return true;
   }

   public final void showNpcDialog(byte npcType, int npcId, String npcName, String portraitPath, String dialogText, String[] choices) {
      this.npcType = npcType;
      this.npcId = npcId;
      this.npcNameLabel.setText(npcName);
      this.npcPortraitTexture = TextureAsset.obtain(portraitPath);
      this.dialogTextLabel.setText("{FASTER}" + dialogText);
      this.dialogTextLabel.restart();
      Array.ArrayIterator iter = this.choicesTable.getChildren().iterator();

      while (iter.hasNext()) {
         Actor child;
         if ((child = (Actor)iter.next()) instanceof NpcDialogPopupV2.NpcChoiceButton) {
            this.choicePool.free((NpcDialogPopupV2.NpcChoiceButton)child);
         }
      }

      this.choicesTable.clear();

      for (int i = 0; i < choices.length; i++) {
         this.choicesTable.add(this.choicePool.obtain().setChoiceIndex(i).setText(choices[i])).size(315.0F, 65.0F).space(10.0F);
      }
   }

   @Override
   public final void drawBackground() {
      super.renderContext.drawNinePatch(this.backgroundNinePatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.drawTexture(this.npcPortraitTexture, this.getX(), this.getY(), Anchor.BOTTOM_LEFT);
   }

   @Override
   public final void show() {
      super.show();
      this.clearActions();
      this.addAction(Actions.sequence(Actions.moveTo(0.0F, 0.0F, 0.2F, Interpolation.fastSlow)));
   }

   @Override
   public final void hide() {
      this.clearActions();
      this.addAction(Actions.sequence(Actions.moveTo(0.0F, -this.getHeight(), 0.2F, Interpolation.fastSlow), Actions.hide()));
   }

   public class NpcChoiceButton extends TextButton {

      public final NpcDialogPopupV2 buttonSprite = NpcDialogPopupV2.this;
      public NinePatchAsset backgroundNinePatch;
      public int choiceIndex;

      public NpcChoiceButton(String text, RenderContext renderContext) {
         super(text, renderContext);
         this.backgroundNinePatch = NinePatchAsset.obtain("ui/137", 20);
         this.addListener(new NpcDialogChoiceClickListener(this, NpcDialogPopupV2.this));
      }

      public final NpcDialogPopupV2.NpcChoiceButton setChoiceIndex(int choiceIndex) {
         this.choiceIndex = choiceIndex;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawNinePatch(this.backgroundNinePatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
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
