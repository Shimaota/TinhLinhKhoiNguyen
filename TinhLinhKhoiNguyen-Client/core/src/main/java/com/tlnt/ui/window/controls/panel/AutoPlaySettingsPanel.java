package com.tlnt.ui.window.controls.panel;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.core.Settings;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.mob.MobTemplate;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.AmountStepper;
import com.tlnt.ui.window.charinfo.SectionHeaderLabel;
import com.tlnt.ui.window.charinfo.SettingsToggleButton;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.player.PlayerEntity;

public class AutoPlaySettingsPanel extends GamePanel {
   public Pool<AutoPlaySettingsPanel.TargetFilterCheckbox> targetFilterCheckboxPool = new TargetFilterCheckboxPool(this);
   public Table contentTable = new Table().top().padBottom(10.0F);
   public SectionHeaderLabel autoAttackSectionLabel;
   public SettingsToggleButton autoSkillSlot1Checkbox;
   public SettingsToggleButton autoSkillSlot2Checkbox;
   public SettingsToggleButton autoSkillSlot3Checkbox;
   public SettingsToggleButton autoSkillSlot4Checkbox;
   public SettingsToggleButton autoSkillSlot5Checkbox;
   public SettingsToggleButton autoSkillSlot6Checkbox;
   public SettingsToggleButton noAttackPlayersCheckbox;
   public SettingsToggleButton avoidEliteMobCheckbox;
   public SectionHeaderLabel targetFilterSectionLabel;
   public SectionHeaderLabel autoOperationSectionLabel;
   public SettingsToggleButton autoReviveCheckbox;
   public SettingsToggleButton autoPickupItemsCheckbox;
   public SettingsToggleButton autoUseStaminaItemCheckbox;
   public SettingsToggleButton autoUseHpItemCheckbox;
   public SettingsToggleButton autoUseMpItemCheckbox;
   public SettingsToggleButton autoHarvestAppleCheckbox;
   public AmountStepper redAppleHpThresholdStepper;
   public AmountStepper greenAppleMpThresholdStepper;
   public Table targetFilterTable;
   public SettingsToggleButton allTargetsCheckbox;
   public OfflineTrainingButton offlineTrainingButton;
   /** "Attack range" (decompiled C3764 f15163): Unlimited / 500 … 2500 px around the spot where auto-attack was enabled. */
   public com.tlnt.ui.widget.DropdownSelect<String> attackRangeSelect;
   static final String RANGE_UNLIMITED = "Không giới hạn";

   public AutoPlaySettingsPanel(RenderContext ctx) {
      super(ctx);
      ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.contentTable);
      this.add(scrollPane).grow();
      this.autoAttackSectionLabel = new SectionHeaderLabel("Thiết lập Tự động đánh", super.renderContext);
      this.contentTable.add(this.autoAttackSectionLabel).height(34.0F).growX().space(30.0F).row();
      Table autoAttackTable = new Table();
      this.contentTable.add(autoAttackTable).growX().spaceBottom(70.0F).row();
      autoAttackTable.add(this.autoSkillSlot1Checkbox = new SettingsToggleButton("Tự động dùng kỹ năng ô 1", super.renderContext)).height(55.0F).growX().uniform().space(10.0F);
      autoAttackTable.add(this.autoSkillSlot2Checkbox = new SettingsToggleButton("Tự động dùng kỹ năng ô 2", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      autoAttackTable.add(this.autoSkillSlot3Checkbox = new SettingsToggleButton("Tự động dùng kỹ năng ô 3", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      autoAttackTable.row();
      autoAttackTable.add(this.autoSkillSlot4Checkbox = new SettingsToggleButton("Tự động dùng kỹ năng ô 4", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      autoAttackTable.add(this.autoSkillSlot5Checkbox = new SettingsToggleButton("Tự động dùng kỹ năng ô 5", super.renderContext)).height(55.0F).growX().uniform().space(10.0F);
      autoAttackTable.add(this.autoSkillSlot6Checkbox = new SettingsToggleButton("Tự động dùng kỹ năng ô 6", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      autoAttackTable.row();
      autoAttackTable.add(this.noAttackPlayersCheckbox = new SettingsToggleButton("Không đánh Người và Boss", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      autoAttackTable.add(this.avoidEliteMobCheckbox = new SettingsToggleButton("Né siêu quái", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      this.autoSkillSlot1Checkbox.addListener(new AutoSkillSlot1ClickListener(this));
      this.autoSkillSlot2Checkbox.addListener(new AutoSkillSlot2ClickListener(this));
      this.autoSkillSlot3Checkbox.addListener(new AutoSkillSlot3ToggleListener(this));
      this.autoSkillSlot4Checkbox.addListener(new AutoSkillSlot4ClickListener(this));
      this.autoSkillSlot5Checkbox.addListener(new AutoSkillSlot5ClickListener(this));
      this.autoSkillSlot6Checkbox.addListener(new AutoSkillSlot6ClickListener(this));
      this.noAttackPlayersCheckbox.addListener(new AutoAvoidPlayerAndBossClickListener(this));
      this.avoidEliteMobCheckbox.addListener(new AutoAvoidEliteMobClickListener(this));
      this.targetFilterSectionLabel = new SectionHeaderLabel("Thiết lập Tự động đánh", super.renderContext);
      this.contentTable.add(this.targetFilterSectionLabel).height(34.0F).growX().space(30.0F).row();
      Table rangeTable = new Table();
      com.github.tommyettinger.textra.TextraLabel rangeLabel = new com.github.tommyettinger.textra.TextraLabel(
         "[#8B4E23]Phạm vi đánh:", com.tlnt.engine.font.FontFactory.getCachedTextraFont(com.tlnt.core.audio.FontManager.PLAIN_FONT, 24));
      rangeLabel.setAlignment(10);
      rangeTable.add(rangeLabel).space(10.0F).padTop(-45.0F);
      this.attackRangeSelect = new com.tlnt.ui.widget.DropdownSelect<>(super.renderContext);
      this.attackRangeSelect.setItems(RANGE_UNLIMITED, "500", "1000", "1500", "2000", "2500");
      this.attackRangeSelect.setSelectionListener(item -> {
         Settings.autoAttackRange = RANGE_UNLIMITED.equals(item) ? 0 : Integer.parseInt(item);
         com.tlnt.net.task.ServerPacketSender.getInstance().sendGameSettings();
      });
      this.attackRangeSelect.setSize(200.0F, 63.0F);
      rangeTable.add(this.attackRangeSelect).width(200.0F).height(50.0F);
      this.contentTable.add(rangeTable).expandX().left().spaceBottom(30.0F).row();
      this.targetFilterTable = new Table();
      this.contentTable.add(this.targetFilterTable).growX().spaceBottom(70.0F).row();
      this.allTargetsCheckbox = new SettingsToggleButton("Tất cả", super.renderContext);
      this.allTargetsCheckbox.addListener(new AutoTargetAllClickListener(this));
      this.autoOperationSectionLabel = new SectionHeaderLabel("Thiết lập Thao tác tự động", super.renderContext);
      this.contentTable.add(this.autoOperationSectionLabel).height(34.0F).growX().space(30.0F).row();
      Table autoOperationTable = new Table();
      this.contentTable.add(autoOperationTable).growX().spaceBottom(70.0F).row();
      autoOperationTable.add(this.autoReviveCheckbox = new SettingsToggleButton("Tự động hồi sinh", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      autoOperationTable.add(this.autoPickupItemsCheckbox = new SettingsToggleButton("Tự nhặt vật phẩm", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      autoOperationTable.add(this.autoUseStaminaItemCheckbox = new SettingsToggleButton("Tự dùng vật phẩm hồi thể lực", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      autoOperationTable.row();
      autoOperationTable.add(this.autoUseHpItemCheckbox = new SettingsToggleButton("Tự dùng vật phẩm hồi HP", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      autoOperationTable.add(this.autoUseMpItemCheckbox = new SettingsToggleButton("Tự dùng vật phẩm hồi MP", super.renderContext)).height(55.0F).growX().uniform().space(10.0F);
      autoOperationTable.add(this.autoHarvestAppleCheckbox = new SettingsToggleButton("Tự thu hoạch táo", super.renderContext))
         .height(55.0F)
         .growX()
         .uniform()
         .space(10.0F);
      this.contentTable
         .add(this.redAppleHpThresholdStepper = new AmountStepper("Sử dụng Táo đỏ khi HP còn dưới", 0.0F, 100.0F, 5.0F, super.renderContext))
         .size(293.0F, 55.0F)
         .expandX()
         .left()
         .space(60.0F)
         .row();
      this.contentTable
         .add(this.greenAppleMpThresholdStepper = new AmountStepper("Sử dụng Táo xanh khi MP còn dưới", 0.0F, 100.0F, 5.0F, super.renderContext))
         .size(293.0F, 55.0F)
         .expandX()
         .left()
         .space(60.0F)
         .row();
      this.contentTable.add(this.offlineTrainingButton = new OfflineTrainingButton(super.renderContext)).size(209.0F, 63.0F);
      this.autoReviveCheckbox.addListener(new AutoReviveClickListener(this));
      this.autoPickupItemsCheckbox.addListener(new AutoPickupItemsClickListener(this));
      this.autoUseStaminaItemCheckbox.addListener(new AutoUseStaminaItemClickListener(this));
      this.autoUseHpItemCheckbox.addListener(new AutoUseHpPotionToggleListener(this));
      this.autoUseMpItemCheckbox.addListener(new AutoUseMpItemClickListener(this));
      this.autoHarvestAppleCheckbox.addListener(new AutoHarvestAppleToggleListener(this));
      this.redAppleHpThresholdStepper.setValueChangeListener(new RedAppleHpThresholdChangeListener(this));
      this.greenAppleMpThresholdStepper.setValueChangeListener(new GreenAppleMpThresholdChangeListener(this));
   }

   @Override
   public final void refresh() {
      ObjectSet<MobTemplate> nearbyTemplates = new ObjectSet<>();

      for (MobEntity mob : GameState.nearbyMobs) {
         nearbyTemplates.add(mob.POOL2.template);
      }

      Array.ArrayIterator childIter = this.targetFilterTable.getChildren().iterator();

      while (childIter.hasNext()) {
         Actor child;
         if ((child = (Actor)childIter.next()) instanceof AutoPlaySettingsPanel.TargetFilterCheckbox) {
            this.targetFilterCheckboxPool.free((AutoPlaySettingsPanel.TargetFilterCheckbox)child);
         }
      }

      this.targetFilterTable.clear();
      this.targetFilterTable.add(this.allTargetsCheckbox).height(55.0F).growX().uniform().space(10.0F);

      for (MobTemplate template : nearbyTemplates) {
         this.targetFilterTable.add(this.targetFilterCheckboxPool.obtain().setMobType(template)).height(55.0F).growX().uniform().space(10.0F);
         if (this.targetFilterTable.getChildren().size % 3 == 0) {
            this.targetFilterTable.row();
         }
      }

      SettingsToggleButton allTargets = this.allTargetsCheckbox;
      int allTargetsFlag;
      if (PlayerEntity.getInstance() != null && PlayerEntity.getInstance().autoTargetController.speed) {
         allTargetsFlag = 1;
      } else {
         allTargetsFlag = 0;
      }

      allTargets.setChecked((allTargetsFlag != 0));
      this.autoSkillSlot1Checkbox.setChecked(Settings.autoSkillSlotEnabled[0]);
      this.autoSkillSlot2Checkbox.setChecked(Settings.autoSkillSlotEnabled[1]);
      this.autoSkillSlot3Checkbox.setChecked(Settings.autoSkillSlotEnabled[2]);
      this.autoSkillSlot4Checkbox.setChecked(Settings.autoSkillSlotEnabled[3]);
      this.autoSkillSlot5Checkbox.setChecked(Settings.autoSkillSlotEnabled[4]);
      this.autoSkillSlot6Checkbox.setChecked(Settings.autoSkillSlotEnabled[5]);
      this.noAttackPlayersCheckbox.setChecked(Settings.autoIgnorePlayersAndBosses);
      this.avoidEliteMobCheckbox.setChecked(Settings.autoAvoidEliteMobs);
      this.autoReviveCheckbox.setChecked(Settings.autoRevive);
      this.autoPickupItemsCheckbox.setChecked(Settings.autoPickupItems);
      this.autoUseStaminaItemCheckbox.setChecked(Settings.autoUseStaminaItems);
      this.autoUseHpItemCheckbox.setChecked(Settings.autoUseHpItems);
      this.autoUseMpItemCheckbox.setChecked(Settings.autoUseMpItems);
      this.autoHarvestAppleCheckbox.setVisible(GameState.appleEventState.active);
      this.autoHarvestAppleCheckbox.setChecked(Settings.autoHarvestApples);
      this.offlineTrainingButton.setVisible(GameState.appleEventState.active); // Spirit Badge still active
      this.redAppleHpThresholdStepper.setValue((float)Settings.autoHpThresholdPercent);
      this.greenAppleMpThresholdStepper.setValue((float)Settings.autoMpThresholdPercent);
      this.attackRangeSelect.setSelected(Settings.autoAttackRange == 0 ? RANGE_UNLIMITED : String.valueOf(Settings.autoAttackRange));
   }

   public class TargetFilterCheckbox extends SettingsToggleButton {

      public final AutoPlaySettingsPanel buttonSprite = AutoPlaySettingsPanel.this;
      public MobTemplate checkIconTexture;

      public TargetFilterCheckbox(String text, RenderContext ctx) {
         super(text, ctx);
         this.addListener(new MobTypeAutoTargetToggleListener(this, AutoPlaySettingsPanel.this));
      }

      public final AutoPlaySettingsPanel.TargetFilterCheckbox setMobType(MobTemplate template) {
         this.checkIconTexture = template;
         if (this.checkIconTexture != null) {
            this.setText(this.checkIconTexture.name);
            this.setChecked(PlayerEntity.getInstance().autoTargetController.active.contains(this.checkIconTexture));
         }

         return this;
      }
   }
}
