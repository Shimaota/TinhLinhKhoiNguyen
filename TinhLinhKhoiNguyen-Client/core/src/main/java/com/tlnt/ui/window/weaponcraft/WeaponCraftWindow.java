package com.tlnt.ui.window.weaponcraft;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.StatLine;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.Popup;
import com.tlnt.util.GameUtils;

public class WeaponCraftWindow extends Popup {
   public WeaponCraftWindow.RecipeDetailPanel detailPanel = new WeaponCraftWindow.RecipeDetailPanel(this.renderContext);
   public Item[] recipes;
   public int currentRecipeIndex;

   public WeaponCraftWindow(RenderContext ctx) {
      super(
         ctx,
         WindowType.CHOOSE_WEAPON_POPUP,
         0.0F,
         0.0F,
         12,
         (float)WorldConfig.VIEWPORT_WIDTH,
         (float)WorldConfig.VIEWPORT_HEIGHT
      );
      this.detailPanel.setSize(497.0F, 396.0F);
      float posX = (float)(WorldConfig.VIEWPORT_WIDTH / 2 + 203);
      float posY = (float)(WorldConfig.VIEWPORT_HEIGHT / 2 + 36);
      this.detailPanel.setPosition(posX, posY, 10);
      this.addActor(this.detailPanel);
      PrevArrowButton prevButton;
      (prevButton = new PrevArrowButton(this, super.renderContext)).setSize(100.0F, 100.0F);
      posX = (float)(WorldConfig.VIEWPORT_WIDTH / 2 - 350);
      prevButton.setPosition(
         posX, (float)(WorldConfig.VIEWPORT_HEIGHT / 2 + 115), 20
      );
      this.addActor(prevButton);
      prevButton.addListener(new PrevRecipeClickListener(this));
      NextArrowButton nextButton;
      (nextButton = new NextArrowButton(this, super.renderContext)).setSize(100.0F, 100.0F);
      int centerX = WorldConfig.VIEWPORT_WIDTH / 2;
      posX = (float)(centerX + 350);
      int centerY = WorldConfig.VIEWPORT_HEIGHT / 2;
      nextButton.setPosition(posX, (float)(centerY + 115), 12);
      this.addActor(nextButton);
      nextButton.addListener(new NextRecipeClickListener(this));
   }

   public void showPrevRecipe() {
      this.currentRecipeIndex -= 1;
      if (this.currentRecipeIndex < 0) {
         this.currentRecipeIndex = this.recipes.length - 1;
      }

      this.updateRecipeDisplay();
   }

   public void showNextRecipe() {
      this.currentRecipeIndex += 1;
      if (this.currentRecipeIndex > this.recipes.length - 1) {
         this.currentRecipeIndex = 0;
      }

      this.updateRecipeDisplay();
   }

   public void updateRecipeDisplay() {
      Item recipe = this.recipes[this.currentRecipeIndex];
      StringBuilder sb;
      (sb = new StringBuilder()).append(recipe.template.description);
      StatLine[] statLines;
      int statCount = (statLines = recipe.statLines).length;

      for (int i = 0; i < statCount; i++) {
         StatLine statLine = statLines[i];
         sb.append("\n").append(statLine.formatText());
      }

      this.detailPanel.descriptionLabel.setText(sb.toString());
   }

   public final void setRecipes(Item[] newRecipes) {
      this.recipes = newRecipes;
      this.currentRecipeIndex = 0;
      this.updateRecipeDisplay();
      this.show();
   }

   @Override
   public void drawChildren(Batch batch, float parentAlpha) {
      super.drawChildren(batch, parentAlpha);
   }

   @Override
   public final void drawBackground() {
      super.renderContext.drawLine(this.getX(), this.getY(), this.getWidth(), this.getHeight(), new Color(0.0F, 0.0F, 0.0F, 0.5F * this.getColor().a));
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/46"),
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F + 348.0F,
            414.0F,
            122.0F,
            Anchor.BOTTOM_CENTER
         );
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String title = "Chế Tác Vũ Khí";
      float textX = this.getX() + this.getWidth() / 2.0F;
      float textY = this.getY() + this.getHeight() / 2.0F + 400.0F;
      ctx.drawTextColorName(font, title, textX, textY, Anchor.BOTTOM_CENTER, 40.0F, "#8B4E23");
      super.renderContext
         .drawTextureFit(
            TextureAsset.obtain(this.recipes[this.currentRecipeIndex].template.previewImagePath),
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + 712.0F + GameUtils.oscillateInterpolated(Interpolation.sine, 2.0F, 0.0F, 30.0F),
            588.0F,
            300.0F,
            Anchor.CENTER
         );
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/208"),
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F - 18.0F,
            304.0F,
            342.0F,
            Anchor.TOP_CENTER
         );
   }

   public class RecipeDetailPanel extends BaseTable {

      public final WeaponCraftWindow weaponCraftWindow = WeaponCraftWindow.this;
      public NinePatchAsset background;
      public TextraLabel descriptionLabel;

      public RecipeDetailPanel(RenderContext ctx) {
         super(ctx);
         this.background = NinePatchAsset.obtain("ui/13", 20);
         Table descriptionTable;
         Table descTable = descriptionTable = new Table();
         descTable.add(this.descriptionLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 24, 2.5F)))
            .grow();
         this.descriptionLabel.setAlignment(10);
         this.descriptionLabel.setWrap(true);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(descriptionTable)).setSize(477.0F, 228.0F);
         scrollPane.setPosition(10.0F, 111.0F);
         this.addActor(scrollPane);
         CraftWeaponButton craftButton;
         (craftButton = new CraftWeaponButton(this, super.renderContext)).setSize(169.0F, 63.0F);
         craftButton.setPosition(164.0F, 10.0F);
         this.addActor(craftButton);
         craftButton.addListener(new CraftWeaponClickListener(this));
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextColorName(
               FontManager.DEFAULT_FONT,
               WeaponCraftWindow.this.recipes[WeaponCraftWindow.this.currentRecipeIndex].template.name,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 352.0F,
               Anchor.BOTTOM_CENTER,
               30.0F,
               "#FFF600"
            );
         super.draw(batch, parentAlpha);
      }
   }
}
