package com.tlnt.ui.hud.itemslot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.util.GameUtils;

public class ItemSlotButton extends ShapeButton {
   public long lastFrameId;
   public static final int[] NORMAL_ICON_FRAMES;
   public static final int[] SOUL_ICON_FRAMES;
   public static final int[] ELEMENT_ICON_FRAMES;
   public static final int[] SPECIAL_ICON_FRAMES;
   public ItemSlotButton.SlotStyle slotStyle;
   public Item.StarGrade itemType;
   public int[] currentAnimFrames;
   public int animFrameIndex;
   public float animFrameTimer;
   public final float frameInterval = 0.041666668F;
   public boolean showLabel;
   public String labelText;
   public Item item;
   public boolean showCount;
   public boolean showCheckedHighlight;
   public boolean showLockedIndicator;
   public float itemIconScale;
   public EnchantSlotEffect enchantEffect;

   public ItemSlotButton(RenderContext renderContext) {
      this(renderContext, ItemSlotButton.SlotStyle.NORMAL);
   }

   public ItemSlotButton(RenderContext renderContext, ItemSlotButton.SlotStyle slotStyle) {
      super(renderContext);
      this.showLabel = true;
      this.labelText = "";
      this.showCount = true;
      this.showCheckedHighlight = true;
      this.showLockedIndicator = true;
      this.itemIconScale = 1.0F;
      this.slotStyle = slotStyle;
      this.enchantEffect = new EnchantSlotEffect(this);
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      parentAlpha = Gdx.graphics.getDeltaTime();
      if (this.lastFrameId != Gdx.graphics.getFrameId()) {
         this.lastFrameId = Gdx.graphics.getFrameId();
         if (this.itemType != Item.StarGrade.NONE) {
            this.animFrameTimer += parentAlpha;
            if (this.animFrameTimer >= 0.041666668F) {
               this.animFrameTimer %= 0.041666668F;
               this.animFrameIndex += 1;
            }

            if (this.currentAnimFrames != null) {
               this.animFrameIndex = this.animFrameIndex % this.currentAnimFrames.length;
            }
         }
      }

      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      if (this.item != null && this.item.starGrade != Item.StarGrade.NONE) {
         if (this.currentAnimFrames != null) {
            super.renderContext
               .drawTexture(
                  TextureAsset.obtain("icon/" + this.currentAnimFrames[this.animFrameIndex]),
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  Anchor.CENTER
               );
         }
      } else if (this.isChecked() && this.showCheckedHighlight) {
         super.renderContext
            .drawNinePatch(
               this.slotStyle.checkedPatch,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               this.getWidth() + 12.0F,
               this.getHeight() + 12.0F,
               Anchor.CENTER
            );
      } else {
         super.renderContext
            .drawNinePatch(
               this.slotStyle.normalPatch,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               this.getWidth(),
               this.getHeight(),
               Anchor.CENTER
            );
      }

      if (this.item != null && this.item.template != null) {
         TextureAsset itemIcon = TextureAsset.obtain(this.item.template.iconPath);
         super.renderContext
            .drawTextureSized(
               itemIcon,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               itemIcon.getWidth() * this.itemIconScale,
               itemIcon.getHeight() * this.itemIconScale,
               Anchor.CENTER
            );
         if (this.showCount) {
            if (this.item.quantity > 1) {
               super.renderContext
                  .drawText(
                     FontManager.DEFAULT_FONT,
                     GameUtils.formatCompactNumber((long)this.item.quantity),
                     this.getX() + this.getWidth() - 7.0F,
                     this.getY() + 7.0F,
                     Anchor.BOTTOM_RIGHT,
                     20.0F,
                     Color.WHITE
                  );
            }
         }
      } else if (this.showLabel) {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.labelText,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               Anchor.CENTER,
               22.0F,
               Color.WHITE
            );
      }

      if (this.itemType != Item.StarGrade.NONE && this.item != null && this.item.starCount != 0) {
         TextureAsset starTexture = null;
         if (this.itemType == Item.StarGrade.STAR_TIER_1) {
            starTexture = TextureAsset.obtain("icon/" + SOUL_ICON_FRAMES[this.item.starCount - 1]);
         } else if (this.itemType == Item.StarGrade.STAR_TIER_2) {
            starTexture = TextureAsset.obtain("icon/" + SPECIAL_ICON_FRAMES[this.item.starCount - 1]);
         }

         super.renderContext
            .drawTextureSized(
               starTexture,
               this.getX() + this.getWidth() - (float)starTexture.getWidth() / 2.0F,
               this.getY() + this.getHeight() - (float)starTexture.getHeight() / 2.0F,
               (float)starTexture.getWidth() * GameUtils.oscillate(1.0F, 0.8F, 1.0F),
               (float)starTexture.getHeight() * GameUtils.oscillate(1.0F, 0.8F, 1.0F),
               Anchor.CENTER
            );
      }

      if (this.showLockedIndicator && this.item != null && this.item.locked) {
         super.renderContext
            .drawTextureFit(
               TextureAsset.obtain("ui/194"), this.getX() + 5.0F, this.getY() + this.getHeight() - 5.0F, 16.0F, 22.0F, Anchor.TOP_LEFT
            );
      }

      super.renderContext.resetBatchColor();
      if (this.enchantEffect != null) {
         this.enchantEffect.render(super.renderContext);
      }
   }

   public final ItemSlotButton setItem(Item item) {
      this.item = item;
      this.itemType = this.item != null ? this.item.starGrade : Item.StarGrade.NONE;
      if (this.itemType == null) {
         this.itemType = Item.StarGrade.NONE;
      }

      switch (this.itemType) {
         case STAR_TIER_1:
            this.currentAnimFrames = NORMAL_ICON_FRAMES;
            break;
         case STAR_TIER_2:
            this.currentAnimFrames = ELEMENT_ICON_FRAMES;
      }

      return this;
   }

   public final ItemSlotButton setLabelText(String labelText) {
      this.labelText = labelText;
      return this;
   }

   public final ItemSlotButton setShowLabel(boolean showLabel) {
      this.showLabel = showLabel;
      return this;
   }

   public final ItemSlotButton setShowCheckedHighlight(boolean show) {
      this.showCheckedHighlight = false;
      return this;
   }

   public final Item getItem() {
      return this.item;
   }

   static {
      int[] frames = new int[16];
      frames[0] = 500;
      frames[1] = 501;
      frames[2] = 502;
      frames[3] = 503;
      frames[4] = 504;
      frames[5] = 505;
      frames[6] = 506;
      frames[7] = 507;
      frames[8] = 508;
      frames[9] = 509;
      frames[10] = 510;
      frames[11] = 511;
      frames[12] = 512;
      frames[13] = 513;
      frames[14] = 514;
      frames[15] = 515;
      NORMAL_ICON_FRAMES = frames;
      frames = new int[10];
      frames[0] = 490;
      frames[1] = 491;
      frames[2] = 492;
      frames[3] = 493;
      frames[4] = 494;
      frames[5] = 495;
      frames[6] = 496;
      frames[7] = 497;
      frames[8] = 498;
      frames[9] = 499;
      SOUL_ICON_FRAMES = frames;
      frames = new int[16];
      frames[0] = 526;
      frames[1] = 527;
      frames[2] = 528;
      frames[3] = 529;
      frames[4] = 530;
      frames[5] = 531;
      frames[6] = 532;
      frames[7] = 533;
      frames[8] = 534;
      frames[9] = 535;
      frames[10] = 536;
      frames[11] = 537;
      frames[12] = 538;
      frames[13] = 539;
      frames[14] = 540;
      frames[15] = 541;
      ELEMENT_ICON_FRAMES = frames;
      frames = new int[10];
      frames[0] = 516;
      frames[1] = 517;
      frames[2] = 518;
      frames[3] = 519;
      frames[4] = 520;
      frames[5] = 521;
      frames[6] = 522;
      frames[7] = 523;
      frames[8] = 524;
      frames[9] = 525;
      SPECIAL_ICON_FRAMES = frames;
   }

   public static enum SlotStyle {
      NORMAL(NinePatchAsset.obtain("ui/99", 20), NinePatchAsset.obtain("ui/103", 50)),
      SPECIAL(NinePatchAsset.obtain("ui/164", 20), NinePatchAsset.obtain("ui/215", 50));

      public NinePatchAsset normalPatch;
      public NinePatchAsset checkedPatch;

      SlotStyle(NinePatchAsset normalPatch, NinePatchAsset checkedPatch) {
         this.normalPatch = normalPatch;
         this.checkedPatch = checkedPatch;
      }

   }
}
