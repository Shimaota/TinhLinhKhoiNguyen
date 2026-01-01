package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.skill.LearnedSkill;
import com.tlnt.model.skill.SkillLevelData;
import com.tlnt.model.skill.TalentData;
import com.tlnt.model.skill.TalentPointCap;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.CharacterModelActor;
import com.tlnt.ui.window.charinfo.CharacterPortraitPanel;
import com.tlnt.world.entity.player.PlayerEntity;

public class TalentPanel extends GamePanel {
   public TalentPanel.SkillListPanel skillListPanel = new TalentPanel.SkillListPanel(this.renderContext);
   public TalentPanel.SkillDetailPanel skillDetailPanel;
   public boolean showingSkillSlot;
   public TalentData selectedPassiveSkill;
   public LearnedSkill selectedSkillSlot;

   public TalentPanel(RenderContext ctx) {
      super(ctx);
      this.skillListPanel.setSize(796.0F, 610.0F);
      this.addActor(this.skillListPanel);
      this.skillDetailPanel = new TalentPanel.SkillDetailPanel(super.renderContext);
      this.skillDetailPanel.setSize(433.0F, 610.0F);
      this.skillDetailPanel.setPosition(826.0F, 0.0F);
      this.addActor(this.skillDetailPanel);
   }

   public final TalentPanel.TalentTypeButton getPassiveSkillNode(int index) {
      if (5 > this.skillListPanel.talentTypeTable.getChildren().size - 1) {
         return null;
      } else {
         return (TalentPanel.TalentTypeButton)this.skillListPanel.talentTypeTable.getChild(5);
      }
   }

   public final TextButton getEnterPointsButton() {
      return this.skillDetailPanel.upgradeButton;
   }

   public void showSkillInfo(TalentData talent) {
      this.selectedPassiveSkill = talent;
      if (this.selectedPassiveSkill != null) {
         this.showingSkillSlot = false;
         this.skillDetailPanel.skillNameLabel.setText("[#8B4E23]" + this.selectedPassiveSkill.name);
         StringBuilder sb;
         (sb = new StringBuilder()).append("[#8B4E23]").append(this.selectedPassiveSkill.description);
         this.skillDetailPanel.skillDescLabel.setText(sb.toString());
         sb.setLength(0);
         TalentPointCap nextCap = TalentPointCap.findNextCap(
            PlayerEntity.getInstance().POOL2.talentPointsArray[this.selectedPassiveSkill.id - 1], this.selectedPassiveSkill.id
         );
         sb.append("[#8B4E23]");
         if (nextCap != null) {
            sb.append("Yêu cầu level: ").append(nextCap.requiredLevel).append("\n");
            sb.append("Cần " + this.selectedPassiveSkill.pointCost + " điểm tiềm năng để nâng cấp");
         } else {
            sb.append("Đã đạt cấp độ tối đa!");
         }

         this.skillDetailPanel.nextLevelLabel.setText(sb.toString());
         this.skillDetailPanel.upgradeButton.setVisible(true);
         this.skillDetailPanel.learnSkillButton.setVisible(false);
         this.skillDetailPanel.viewSkillSlotButton.setVisible(false);
      }
   }

   public final TalentPanel.SkillButton getActiveSkillNode(int index) {
      return (TalentPanel.SkillButton)this.skillListPanel.skillTable.getChild(1);
   }

   public final TextButton getAssignSlotButton() {
      return this.skillDetailPanel.viewSkillSlotButton;
   }

   public void showSkillSlotInfo(LearnedSkill learnedSkill) {
      this.selectedSkillSlot = learnedSkill;
      if (this.selectedSkillSlot != null) {
         this.showingSkillSlot = true;
         StringBuilder sb;
         StringBuilder sbRef = sb = new StringBuilder();
         sbRef.append("[#8B4E23]");
         sb.append(this.selectedSkillSlot.skill.name)
            .append(" (")
            .append(this.selectedSkillSlot.levelData == null ? "Chưa học" : "Cấp" + this.selectedSkillSlot.levelData.level)
            .append(")");
         this.skillDetailPanel.skillNameLabel.setText(sb.toString());
         sb.setLength(0);
         sbRef = sb.append("[#8B4E23]").append(this.selectedSkillSlot.skill.description);
         sbRef.append("\n");
         sb.append("- - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
         if (this.selectedSkillSlot.levelData != null) {
            sbRef = sb.append(this.selectedSkillSlot.levelData.effectDescription);
            sbRef.append("\n");
            if (this.selectedSkillSlot.levelData.nextLevel != null) {
               sb.append("- - - - - - - - - - - - - - - - - - - - - - - - - - - -\n[#C79D80]Cấp kế tiếp:[#C79D80]\n").append(this.selectedSkillSlot.levelData.nextLevel.effectDescription);
            }
         } else {
            sb.append("- - - - - - - - - - - - - - - - - - - - - - - - - - - -\n[#C79D80]Cấp kế tiếp:[#C79D80]\n").append(this.selectedSkillSlot.skill.levels.first().effectDescription);
         }

         this.skillDetailPanel.skillDescLabel.setText(sb.toString());
         sb.setLength(0);
         sb.append("[#8B4E23]");
         this.skillDetailPanel.upgradeButton.setVisible(false);
         this.skillDetailPanel.learnSkillButton.setVisible(true);
         this.skillDetailPanel.viewSkillSlotButton.setVisible(true);
         if (this.selectedSkillSlot.levelData != null) {
            SkillLevelData nextLevel = this.selectedSkillSlot.levelData.nextLevel;
            if (this.selectedSkillSlot.levelData.nextLevel != null) {
               sbRef = sb.append("Yêu cầu level: ").append(formatNumber(nextLevel.requiredLevel));
               sbRef.append("\n");
               sbRef = sb.append("Cần ").append(formatNumber(nextLevel.upgradeCost));
               sbRef.append(" điểm tiềm năng để nâng cấp");
               appendCost(sb, nextLevel);
            } else {
               sb.append("Kỹ năng đã đạt cấp độ tối đa");
               this.skillDetailPanel.learnSkillButton.setVisible(false);
            }
         } else {
            sb.append("Yêu cầu level: ")
               .append(formatNumber(this.selectedSkillSlot.skill.levels.first().requiredLevel))
               .append("\n");
            sb.append("Cần ")
               .append(formatNumber(this.selectedSkillSlot.skill.levels.first().upgradeCost))
               .append(" điểm tiềm năng để nâng cấp");
            appendCost(sb, this.selectedSkillSlot.skill.levels.first());
            this.skillDetailPanel.viewSkillSlotButton.setVisible(false);
         }

         this.skillDetailPanel.nextLevelLabel.setText(sb.toString());
      }
   }

   /** "#,###" like the decompiled code (C3849): 1,234,567. */
   private static String formatNumber(long value) {
      return String.format(java.util.Locale.US, "%,d", value);
   }

   /** The line "Tiêu hao: X Vàng và Y Kinh nghiệm" (decompiled C3680 m16855). */
   private static void appendCost(StringBuilder sb, SkillLevelData level) {
      sb.append("\nTiêu hao: ").append(formatNumber(level.goldCost)).append(" Vàng và ")
         .append(formatNumber(level.expCost)).append(" Kinh nghiệm");
   }

   public class SkillButton extends ShapeButton {
      public NinePatchAsset normalPatch;
      public NinePatchAsset selectedPatch;
      public LearnedSkill skill;

      public SkillButton(RenderContext ctx) {
         super(ctx);
         this.normalPatch = NinePatchAsset.obtain("ui/99", 20);
         String selectedPath = "ui/103";
         this.selectedPatch = NinePatchAsset.obtain(selectedPath, 50);
      }

      public final TalentPanel.SkillButton setSkill(LearnedSkill learnedSkill) {
         this.skill = learnedSkill;
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
                  this.selectedPatch,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth() + 0.0F,
                  this.getHeight() + 0.0F,
                  Anchor.CENTER
               );
         } else {
            super.renderContext
               .drawNinePatch(
                  this.normalPatch,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth(),
                  this.getHeight(),
                  Anchor.CENTER
               );
         }

         if (this.skill.levelData == null) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         int iconIndex;
         if (this.skill.skill.id == 1) {
            if (PlayerEntity.getInstance().POOL2.weaponType == -1) {
               iconIndex = 1;
            } else {
               iconIndex = 0;
            }
         } else if (PlayerEntity.getInstance().POOL2.weaponType == -1) {
            iconIndex = 0;
         } else {
            iconIndex = PlayerEntity.getInstance().POOL2.weaponType;
         }

         if (iconIndex != -1) {
            super.renderContext
               .drawTextureSized(
                  TextureAsset.obtain(this.skill.skill.iconPaths[iconIndex]),
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth() - 8.0F,
                  this.getHeight() - 8.0F,
                  Anchor.CENTER
               );
         }

         if (this.skill.levelData != null) {
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  this.skill.levelData.level + "",
                  this.getX() + this.getWidth() - 10.0F,
                  this.getY() + 10.0F,
                  Anchor.BOTTOM_RIGHT,
                  20.0F,
                  Color.WHITE
               );
         }

         super.renderContext.resetBatchColor();
      }
   }

   public class SkillDetailPanel extends GamePanel {

      public final TalentPanel talentPanel = TalentPanel.this;
      public TextraLabel skillNameLabel;
      public TextraLabel skillDescLabel;
      public TextraLabel nextLevelLabel;
      public TalentUpgradeButton upgradeButton;
      public AssignSkillSlotButton viewSkillSlotButton;
      public EquipSkillTypeButton learnSkillButton;

      public SkillDetailPanel(RenderContext ctx) {
         super(ctx);
         Table descTable;
         (descTable = new Table().padBottom(10.0F))
            .add(this.skillNameLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F)))
            .growX()
            .row();
         this.skillNameLabel.setWrap(true).setAlignment(10);
         descTable.add(this.skillDescLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 24, 2.0F))).grow();
         TextraLabel descLabel = this.skillDescLabel.setWrap(true);
         descLabel.setAlignment(10);
         ScrollPane descScroll;
         (descScroll = ScrollPaneFactory.createScrollPane(descTable)).setSize(433.0F, 400.0F);
         descScroll.setPosition(0.0F, 212.0F);
         this.addActor(descScroll);
         Table nextLevelTable;
         (nextLevelTable = new Table())
            .add(
               this.nextLevelLabel = new TextraLabel(
                  "", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 24, 2.0F)
               )
            )
            .grow();
         this.nextLevelLabel.setWrap(true).setAlignment(10);
         ScrollPane nextLevelScroll;
         (nextLevelScroll = ScrollPaneFactory.createScrollPane(nextLevelTable)).setSize(433.0F, 76.0F);
         nextLevelScroll.setPosition(0.0F, 83.0F);
         this.addActor(nextLevelScroll);
         this.upgradeButton = new TalentUpgradeButton(this, "Nâng cấp", super.renderContext);
         this.upgradeButton.setSize(169.0F, 63.0F);
         this.upgradeButton.setPosition(138.0F, 0.0F);
         this.addActor(this.upgradeButton);
         this.upgradeButton.addListener(new EnterTalentPointsClickListener(this));
         this.viewSkillSlotButton = new AssignSkillSlotButton(this, "Phím tắt", super.renderContext);
         this.viewSkillSlotButton.setSize(169.0F, 63.0F);
         this.viewSkillSlotButton.setPosition(34.0F, 0.0F);
         this.addActor(this.viewSkillSlotButton);
         this.viewSkillSlotButton.addListener(new OpenSkillSlotPopupListener(this));
         this.learnSkillButton = new EquipSkillTypeButton(this, "Nâng cấp", super.renderContext);
         this.learnSkillButton.setSize(169.0F, 63.0F);
         this.learnSkillButton.setPosition(213.0F, 0.0F);
         this.addActor(this.learnSkillButton);
         this.learnSkillButton.addListener(new EquipSkillTypeClickListener(this));
         this.upgradeButton.setVisible(false);
         this.learnSkillButton.setVisible(false);
         this.viewSkillSlotButton.setVisible(false);
      }
   }

   public class SkillListPanel extends GamePanel {

      public final TalentPanel talentPanel = TalentPanel.this;
      public Pool<TalentPanel.TalentTypeButton> talentTypeButtonPool = new TalentTypeButtonPool(this);
      public Pool<TalentPanel.SkillButton> skillButtonPool = new TalentSkillButtonPool(this);
      public CharacterPortraitPanel characterPortrait;
      public TextraLabel talentPointsLabel;
      public CharacterModelActor characterModel;
      public Array<TalentPanel.TalentTypeButton> talentTypeButtons = new Array<>();
      public Array<TalentPanel.SkillButton> skillButtons = new Array<>();
      public Table talentTypeTable;
      public Table skillTable;

      public SkillListPanel(RenderContext ctx) {
         super(ctx);
         this.characterPortrait = new CharacterPortraitPanel(super.renderContext);
         this.characterPortrait.setSize(279.0F, 128.0F);
         this.characterPortrait.setPosition(90.0F, 466.0F);
         this.addActor(this.characterPortrait);
         this.talentTypeTable = new Table().top().left();
         this.talentTypeTable.setSize(332.0F, 218.0F);
         this.talentTypeTable.setPosition(439.0F, 337.0F);
         this.addActor(this.talentTypeTable);
         ScrollPane skillScroll;
         (skillScroll = ScrollPaneFactory.createScrollPane(this.skillTable = new Table().top().left())).setSize(332.0F, 218.0F);
         skillScroll.setPosition(439.0F, 64.0F);
         this.addActor(skillScroll);
         this.talentPointsLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
         this.talentPointsLabel.setAlignment(10);
         this.talentPointsLabel.setSize(332.0F, 35.0F);
         this.talentPointsLabel.setPosition(439.0F, 15.0F);
         this.addActor(this.talentPointsLabel);
         this.characterModel = new CharacterModelActor(super.renderContext);
         this.characterModel.setPosition(240.0F, 120.0F);
         this.addActor(this.characterModel);
      }

      @Override
      public final void refresh() {
         this.characterPortrait.setPlayerData(PlayerEntity.getInstance().POOL2);
         this.talentPointsLabel.setText("Điểm tiềm năng hiện có: " + PlayerEntity.getInstance().POOL2.availableTalentPoints);
         this.characterModel.loadSkeleton(PlayerEntity.getInstance().POOL2.skeletonKey, PlayerEntity.getInstance().POOL2.equipmentSkinKeys);
         Array.ArrayIterator childIter = this.talentTypeTable.getChildren().iterator();

         while (childIter.hasNext()) {
            Actor child;
            if ((child = (Actor)childIter.next()) instanceof TalentPanel.TalentTypeButton) {
               this.talentTypeButtonPool.free((TalentPanel.TalentTypeButton)child);
            }
         }

         this.talentTypeTable.clear();
         childIter = this.skillTable.getChildren().iterator();

         while (childIter.hasNext()) {
            Actor skillChild;
            if ((skillChild = (Actor)childIter.next()) instanceof TalentPanel.SkillButton) {
               this.skillButtonPool.free((TalentPanel.SkillButton)skillChild);
            }
         }

         this.skillTable.clear();

         for (TalentData talent : TalentData.getAll()) {
            if (TalentPanel.this.selectedPassiveSkill == null) {
               TalentPanel.this.selectedPassiveSkill = talent;
            }

            TalentPanel.TalentTypeButton talentButton = this.talentTypeButtonPool.obtain().setTalentType(talent);
            if (talent.equals(TalentPanel.this.selectedPassiveSkill)) {
               talentButton.setChecked(true);
               if (!TalentPanel.this.showingSkillSlot) {
                  TalentPanel.this.showSkillInfo(talent);
               }
            }

            this.talentTypeTable.add(talentButton).size(104.0F).space(10.0F);
            if (this.talentTypeTable.getChildren().size % 3 == 0) {
               this.talentTypeTable.row();
            }
         }

         for (LearnedSkill learnedSkill : GameState.learnedSkills) {
            if (TalentPanel.this.selectedSkillSlot == null) {
               TalentPanel.this.selectedSkillSlot = learnedSkill;
            }

            TalentPanel.SkillButton skillButton = this.skillButtonPool.obtain().setSkill(learnedSkill);
            if (learnedSkill.equals(TalentPanel.this.selectedSkillSlot)) {
               skillButton.setChecked(true);
               if (TalentPanel.this.showingSkillSlot) {
                  TalentPanel.this.showSkillSlotInfo(learnedSkill);
               }
            }

            this.skillTable.add(skillButton).size(104.0F).space(10.0F);
            if (this.skillTable.getChildren().size % 3 == 0) {
               this.skillTable.row();
            }
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawNinePatch(
               NinePatchAsset.obtain("ui/100", 50),
               this.getX(),
               this.getY(),
               this.getWidth(),
               this.getHeight(),
               Anchor.BOTTOM_LEFT
            );
         super.renderContext
            .drawTexture(TextureAsset.obtain("ui/101"), this.getX() + 65.0F, this.getY() + 104.0F, Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, "Tiềm năng", this.getX() + 551.0F, this.getY() + 576.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Kỹ năng",
               this.getX() + 562.0F,
               this.getY() + 302.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               Color.WHITE
            );
         super.draw(batch, parentAlpha);
      }
   }

   public static class TalentTypeButton extends ShapeButton {
      public NinePatchAsset normalPatch;
      public NinePatchAsset selectedPatch;
      public TalentData talentType;

      public TalentTypeButton(RenderContext ctx) {
         super(ctx);
         String patchPath = "ui/99";
         this.normalPatch = NinePatchAsset.obtain(patchPath, 20);
         patchPath = "ui/103";
         this.selectedPatch = NinePatchAsset.obtain(patchPath, 50);
      }

      public final TalentPanel.TalentTypeButton setTalentType(TalentData talent) {
         this.talentType = talent;
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
                  this.selectedPatch,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth() + 0.0F,
                  this.getHeight() + 0.0F,
                  Anchor.CENTER
               );
         } else {
            super.renderContext
               .drawNinePatch(
                  this.normalPatch,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth(),
                  this.getHeight(),
                  Anchor.CENTER
               );
         }

         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain(this.talentType.iconPath),
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               this.getWidth() - 8.0F,
               this.getHeight() - 8.0F,
               Anchor.CENTER
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               PlayerEntity.getInstance().POOL2.talentPointsArray[this.talentType.id - 1] + "",
               this.getX() + this.getWidth() - 10.0F,
               this.getY() + 10.0F,
               Anchor.BOTTOM_RIGHT,
               20.0F,
               Color.WHITE
            );
         super.renderContext.resetBatchColor();
      }

   }
}
