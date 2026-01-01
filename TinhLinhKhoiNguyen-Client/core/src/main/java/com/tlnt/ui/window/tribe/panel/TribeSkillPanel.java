package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.social.TribeSkill;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.tribe.panel.info.TribeInfoPanel;

public final class TribeSkillPanel extends GamePanel {
   public TribeSkillPanel.SkillDetailPane skillContent;

   public TribeSkillPanel(RenderContext renderContext) {
      super(renderContext);
      TribeInfoPanel infoPanel;
      (infoPanel = new TribeInfoPanel(super.renderContext)).setSize(616.0F, 610.0F);
      this.addActor(infoPanel);
      this.skillContent = new TribeSkillPanel.SkillDetailPane(super.renderContext);
      this.skillContent.setSize(610.0F, 610.0F);
      this.skillContent.setPosition(637.0F, 0.0F);
      this.addActor(this.skillContent);
   }

   public class SkillDetailPane extends GamePanel {

      public final TribeSkillPanel tribeSkillPanel = TribeSkillPanel.this;
      public Pool<TribeSkillPanel.SkillIconButton> skillButtonPool = new TribeSkillButtonPool(this);
      public NinePatchAsset panelBackground;
      public Table skillIconTable;
      public TextraLabel skillNameLabel;
      public TextraLabel skillDescLabel;
      public TribeSkillTabButton removeSkillButton;
      public TribeSkillLearnButton learnSkillButton;
      public TribeSkill selectedSkill;

      public SkillDetailPane(RenderContext renderContext) {
         super(renderContext);
         String patchPath = "ui/100";
         this.panelBackground = NinePatchAsset.obtain(patchPath, 50);
         this.pad(19.0F).padTop(43.0F).padBottom(10.0F);
         ScrollPane iconScrollPane;
         (iconScrollPane = ScrollPaneFactory.createScrollPane(this.skillIconTable = new Table())).setSize(572.0F, 230.0F);
         this.add(iconScrollPane).size(572.0F, 230.0F).row();
         this.skillNameLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 30));
         this.skillNameLabel.setAlignment(10);
         this.skillNameLabel.setWrap(true);
         this.add(this.skillNameLabel).growX().minHeight(40.0F).row();
         Table descTable = new Table();
         ScrollPane descScrollPane = new ScrollPane(descTable);
         this.skillDescLabel = new TextraLabel(
            "", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 2.0F)
         );
         this.skillDescLabel.setAlignment(10);
         this.skillDescLabel.setWrap(true);
         descTable.add(this.skillDescLabel).grow();
         this.add(descScrollPane).grow().space(10.0F).row();
         Table buttonRow = new Table().right();
         this.add(buttonRow).size(394.0F, 63.0F).expandX().right();
         this.removeSkillButton = new TribeSkillTabButton(this, "Mở kỹ năng", super.renderContext);
         this.removeSkillButton.addListener(new TribeSkillActionClickListener(this));
         buttonRow.add(this.removeSkillButton).size(192.0F, 63.0F).spaceRight(10.0F);
         this.learnSkillButton = new TribeSkillLearnButton(this, "Đổi kỹ năng", super.renderContext);
         this.learnSkillButton.addListener(new LearnSkillClickListener(this));
         buttonRow.add(this.learnSkillButton).size(192.0F, 63.0F);
         this.updateSkillActions();
      }

      public final void setSelectedSkill(TribeSkill selectedSkill) {
         this.selectedSkill = selectedSkill;
         this.skillNameLabel.setText(this.selectedSkill != null ? this.selectedSkill.name : "");
         this.skillDescLabel.setText(this.selectedSkill != null ? this.selectedSkill.description : "");
         this.updateSkillActions();
      }

      private void updateSkillActions() {
         int tribeLevel = GameState.tribeData.myTribe != null ? GameState.tribeData.myTribe.level : 0;
         int skillSlots = tribeLevel < 1 ? 0 : Math.min(10, 1 + tribeLevel / 5);
         boolean canOpenSkill = GameState.tribeData.myTribe != null && GameState.tribeData.skills.size < skillSlots;
         this.removeSkillButton.setVisible(canOpenSkill);
         this.learnSkillButton.setVisible(this.selectedSkill != null);
      }

      @Override
      public final void refresh() {
         int selectedSkillId = this.selectedSkill != null ? this.selectedSkill.id : -1;
         TribeSkill selectedSkill = null;

         for (int i = this.skillIconTable.getChildren().size - 1; i >= 0; i--) {
            Actor child;
            if ((child = this.skillIconTable.getChild(i)) instanceof TribeSkillPanel.SkillIconButton) {
               ((TribeSkillPanel.SkillIconButton)child).setChecked(false);
               this.skillButtonPool.free((TribeSkillPanel.SkillIconButton)child);
            }
         }

         this.skillIconTable.clear();

         for (TribeSkill skill : GameState.tribeData.skills) {
            TribeSkillPanel.SkillIconButton button;
            button = this.skillButtonPool.obtain().setSkill(skill);
            if (selectedSkill == null && skill.id == selectedSkillId) {
               button.setChecked(true);
               selectedSkill = skill;
            }

            this.skillIconTable.add(button).size(104.0F, 104.0F).space(10.0F);
            if (this.skillIconTable.getChildren().size % 5 == 0) {
               this.skillIconTable.row();
            }
         }

         this.setSelectedSkill(selectedSkill);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.panelBackground, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Kỹ năng Bộ lạc",
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 576.0F,
               Anchor.BOTTOM_CENTER,
               30.0F,
               Color.WHITE
            );
         super.draw(batch, parentAlpha);
      }
   }

   public class SkillIconButton extends ShapeButton {

      public final TribeSkillPanel tribeSkillPanel = TribeSkillPanel.this;
      public NinePatchAsset normalBackground;
      public NinePatchAsset selectedBackground;
      public TribeSkill skillData;

      public SkillIconButton(RenderContext renderContext) {
         super(renderContext);
         String patchPath = "ui/99";
         this.normalBackground = NinePatchAsset.obtain(patchPath, 20);
         patchPath = "ui/103";
         this.selectedBackground = NinePatchAsset.obtain(patchPath, 50);
         this.addListener(new TribeSkillSelectClickListener(this, TribeSkillPanel.this));
      }

      public final TribeSkillPanel.SkillIconButton setSkill(TribeSkill skillData) {
         this.skillData = skillData;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         if (this.isChecked()) {
            super.renderContext
               .drawNinePatch(
                  this.selectedBackground,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth() + 12.0F,
                  this.getHeight() + 12.0F,
                  Anchor.CENTER
               );
         } else {
            super.renderContext
               .drawNinePatch(
                  this.normalBackground,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth(),
                  this.getHeight(),
                  Anchor.CENTER
               );
         }

         if (this.skillData != null) {
            super.renderContext
               .drawTextureSized(
                  TextureAsset.obtain(this.skillData.iconPath),
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth() - 5.0F,
                  this.getHeight() - 5.0F,
                  Anchor.CENTER
               );
         }

         super.renderContext.resetBatchColor();
      }
   }
}
