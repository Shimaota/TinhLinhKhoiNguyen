package com.tlnt.ui.window.skillslot;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.skill.LearnedSkill;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.Popup;
import com.tlnt.world.entity.player.PlayerEntity;

public class SkillSlotPopup extends Popup {
   public TextureAsset backgroundTexture;
   public LearnedSkill selectedSkill;
   public Table slotsTable;

   public SkillSlotPopup(RenderContext ctx) {
      super(ctx, WindowType.SHORTCUT_SKILL, (float)WorldConfig.VIEWPORT_WIDTH / 2.0F, (float)WorldConfig.VIEWPORT_HEIGHT / 2.0F, 1, 790.0F, 234.0F);
      this.backgroundTexture = TextureAsset.obtain("ui/90");
      SkillSlotPopup self = this;
      this.slotsTable = new Table().left();
      this.slotsTable.setSize(580.0F, 104.0F);
      this.slotsTable.setPosition(45.0F, 55.0F);
      this.addActor(this.slotsTable);
      int slot = 0;

      while (true) {
         if (slot >= 6) {
            return;
         }

         self.slotsTable.add(self.new SkillSlotButton(slot, self.renderContext)).size(104.0F).space(15.0F);
         slot++;
      }
   }

   public final SkillSlotPopup.SkillSlotButton getSkillSlotButton(int slot) {
      return (SkillSlotPopup.SkillSlotButton)this.slotsTable.getChild(0);
   }

   @Override
   public final void onClose2() {
      for (int i = 0; i < GameState.skillBarSkills.size; i++) {
         LearnedSkill skill = GameState.skillBarSkills.get(i);
         if (i > this.slotsTable.getChildren().size - 1) {
            break;
         }

         if (this.slotsTable.getChild(i) instanceof SkillSlotPopup.SkillSlotButton) {
            ((SkillSlotPopup.SkillSlotButton)this.slotsTable.getChild(i)).setSkill(skill);
         }
      }
   }

   public final void openWithSkill(LearnedSkill skill) {
      this.selectedSkill = skill;
      GameMain.instance.getLoginScreen().debugScrollPane.showWindow(super.windowType);
   }

   @Override
   public final void drawBackground() {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            "Gán phím tắt kỹ năng",
            this.getX() + 224.0F,
            this.getY() + 179.0F,
            Anchor.BOTTOM_LEFT,
            22.0F,
            "#8B4E23"
         );
   }

   @Override
   public final boolean isDismissOnOutsideClick() {
      return true;
   }

   public class SkillSlotButton extends ShapeButton {

      public final SkillSlotPopup outerPopup = SkillSlotPopup.this;
      public TextureAsset slotBackgroundTexture;
      public int slotIndex;
      public LearnedSkill assignedSkill;

      public SkillSlotButton(int slot, RenderContext ctx) {
         super(ctx);
         this.slotBackgroundTexture = TextureAsset.obtain("ui/99");
         this.slotIndex = slot;
         this.addListener(new SkillSlotClickListener(this, SkillSlotPopup.this));
      }

      public final SkillSlotPopup.SkillSlotButton setSkill(LearnedSkill skill) {
         this.assignedSkill = skill;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawTextureSized(this.slotBackgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (this.assignedSkill != null) {
            int iconIndex;
            if (this.assignedSkill.skill.id == 1) {
               if (PlayerEntity.getInstance().POOL2.weaponType == -1) {
                  iconIndex = 1;
               } else {
                  iconIndex = 0;
               }
            } else {
               if (PlayerEntity.getInstance().POOL2.weaponType == -1) {
                  iconIndex = 0;
               } else {
                  iconIndex = PlayerEntity.getInstance().POOL2.weaponType;
               }
            }

            if (iconIndex != -1) {
               super.renderContext
                  .drawTextureSized(
                     TextureAsset.obtain(this.assignedSkill.skill.iconPaths[iconIndex]),
                     this.getX() + this.getWidth() / 2.0F,
                     this.getY() + this.getHeight() / 2.0F,
                     this.getWidth() - 10.0F,
                     this.getHeight() - 10.0F,
                     Anchor.CENTER
                  );
            }
         } else {
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  "Ô kỹ\nnăng số\n" + (this.slotIndex + 1),
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  Anchor.CENTER,
                  22.0F,
                  Color.WHITE
               );
         }

         super.renderContext.resetBatchColor();
      }
   }
}
