package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.github.tommyettinger.textra.TypingLabel;
import com.tlnt.core.GameState;
import com.tlnt.engine.WorldConfig;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.party.PartyMember;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.GamePanel;

public final class QuestPartyPanel extends GamePanel {
   public float openX;
   public ButtonGroup<QuestPartyPanel.TabButton> tabButtonGroup = new ButtonGroup<>();
   public QuestPartyPanel.QuestDetailPanel questPanel;
   public QuestPartyPanel.PartyMemberListPanel partyPanel;

   public QuestPartyPanel(RenderContext renderContext) {
      super(renderContext);
      this.setSize(331.0F, 274.0F);
      Application.ApplicationType appType = Gdx.app.getType();
      this.openX = appType == Application.ApplicationType.Desktop ? 10.0F : appType == Application.ApplicationType.iOS ? 125.0F : 45.0F;
      this.setPosition(this.openX, (float)(WorldConfig.VIEWPORT_HEIGHT - 249 - 274));
      Table tabRow = new Table().right();
      this.add(tabRow).height(51.0F).growX().space(2.0F).row();
      QuestPartyPanel.TabButton questTab = new QuestPartyPanel.TabButton("Nhiệm vụ", super.renderContext);
      QuestPartyPanel.TabButton partyTab = new QuestPartyPanel.TabButton("Tổ đội", super.renderContext);
      tabRow.add(questTab).size(134.0F, 51.0F).space(7.0F);
      tabRow.add(partyTab).size(134.0F, 51.0F).space(7.0F);
      QuestPartyContentPanel contentPanel = new QuestPartyContentPanel(this, super.renderContext);
      this.add(contentPanel).grow().space(2.0F);
      this.questPanel = new QuestPartyPanel.QuestDetailPanel(super.renderContext);
      this.partyPanel = new QuestPartyPanel.PartyMemberListPanel(super.renderContext);
      this.partyPanel.setVisible(false);
      Stack contentStack = new Stack();
      contentPanel.add(contentStack).grow();
      contentStack.add(this.questPanel);
      contentStack.add(this.partyPanel);
      questTab.addListener(new QuestTabClickListener(this));
      partyTab.addListener(new QuestPartyTabToggleListener(this));
   }

   public class PartyMemberListPanel extends GamePanel {
      public Pool<QuestPartyPanel.PartyMemberListPanel.PartyMemberRow> memberRowPool = new PartyMemberRowPool(this);
      public Table memberTable;
      public PartyActionButton createPartyButton;

      public PartyMemberListPanel(RenderContext renderContext) {
         super(renderContext);
         this.setFillParent(true);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.memberTable = new Table().top())).setSize(304.0F, 201.0F);
         scrollPane.setPosition(15.0F, 8.0F);
         this.addActor(scrollPane);
         this.createPartyButton = new PartyActionButton(this, "Tạo đội", super.renderContext);
         this.createPartyButton.setSize(133.0F, 50.0F);
         this.createPartyButton.setPosition(98.0F, 85.0F);
         this.addActor(this.createPartyButton);
         this.createPartyButton.addListener(new FindPartyClickListener(this));
      }

      @Override
      public final void refresh() {
         int noParty;
         if (GameState.currentParty == null) {
            noParty = 1;
         } else {
            noParty = 0;
         }

         this.createPartyButton.setVisible((noParty != 0));
         Array.ArrayIterator it = this.memberTable.getChildren().iterator();

         while (it.hasNext()) {
            Actor child;
            if ((child = (Actor)it.next()) instanceof QuestPartyPanel.PartyMemberListPanel.PartyMemberRow) {
               this.memberRowPool.free((QuestPartyPanel.PartyMemberListPanel.PartyMemberRow)child);
            }
         }

         this.memberTable.clear();
         if (GameState.currentParty != null) {
            for (PartyMember partyMember : GameState.currentParty.members) {
               this.memberTable.add(this.memberRowPool.obtain().bind(partyMember)).size(304.0F, 45.0F).space(10.0F).row();
            }
         }
      }

      public class PartyMemberRow extends BaseTable {
         public PartyMember member;

         public PartyMemberRow(RenderContext renderContext) {
            super(renderContext);
         }

         public final QuestPartyPanel.PartyMemberListPanel.PartyMemberRow bind(PartyMember member) {
            this.member = member;
            return this;
         }

         @Override
         public final void draw(Batch batch, float parentAlpha) {
            super.renderContext
               .drawTextColorName(
                  FontManager.DEFAULT_FONT,
                  this.member.name,
                  this.getX() + 62.0F,
                  this.getY() + 25.0F,
                  Anchor.BOTTOM_LEFT,
                  18.0F,
                  "#FFF600"
               );
            super.renderContext
               .drawTextureSized(TextureAsset.obtain(this.member.spirit.avatarIconPath), this.getX(), this.getY(), 45.0F, 45.0F, Anchor.BOTTOM_LEFT);
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  "lv: " + this.member.level + " (" + this.member.charClass.name + ")",
                  this.getX() + 62.0F,
                  this.getY() + 6.0F,
                  Anchor.BOTTOM_LEFT,
                  14.0F,
                  Color.WHITE
               );
            if (this.member.role == PartyMember.Role.LEADER) {
               super.renderContext
                  .drawTextColorName(
                     FontManager.DEFAULT_FONT,
                     "(C)",
                     this.getX() + 281.0F,
                     this.getY() + 25.0F,
                     Anchor.BOTTOM_LEFT,
                     18.0F,
                     "#FFF600"
                  );
            }
         }
      }
   }

   public class QuestDetailPanel extends GamePanel {
      public TextraLabel questTitleLabel;
      public TypingLabel questDescriptionLabel;

      public QuestDetailPanel(RenderContext renderContext) {
         super(renderContext);
         this.setFillParent(true);
         Table contentTable;
         ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(contentTable = new Table().top().pad(5.0F, 0.0F, 10.0F, 0.0F));
         this.add(scrollPane).grow().pad(10.0F);
         this.questTitleLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 2.0F));
         this.questTitleLabel.setWrap(true);
         this.questTitleLabel.setAlignment(10);
         contentTable.add(this.questTitleLabel).growX().space(10.0F).row();
         this.questDescriptionLabel = new TypingLabel(
            "", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 20, 2.0F)
         );
         this.questDescriptionLabel.setWrap(true);
         this.questDescriptionLabel.setAlignment(10);
         this.questDescriptionLabel.skipToTheEnd();
         contentTable.add(this.questDescriptionLabel).grow();
         this.questDescriptionLabel.setTypingListener(new QuestTypingEventListener(this));
      }

      @Override
      public final void refresh() {
         this.questTitleLabel.setText("[#FFF600]" + GameState.tutorialState.questTitle);
         StringBuilder sb = new StringBuilder();
         if (GameState.tutorialState.currentStageIndex <= GameState.tutorialState.stages.size - 1) {
            StringBuilder builder = sb.append("{TRIGGER=finding}♦")
               .append(GameState.tutorialState.stages.get(GameState.tutorialState.currentStageIndex).stageName)
               .append("{RESET}\n[green](")
               .append(GameState.tutorialState.currentProgress)
               .append("/")
               .append(GameState.tutorialState.stages.get(GameState.tutorialState.currentStageIndex).requiredCount);
            builder.append(")\n\n[#FFF600]Gợi ý:[reset]\n")
               .append(GameState.tutorialState.stages.get(GameState.tutorialState.currentStageIndex).stageDescription);
            this.questDescriptionLabel.setText(sb.toString());
         }
      }
   }

   public class TabButton extends TextButton {
      public CompositeSprite buttonSprite;
      public CompositeSprite uncheckedSprite;

      public TabButton(String text, RenderContext renderContext) {
         super(text, renderContext);
         String upPath = "ui/17";
         String downPath = "ui/18";
         String checkedPath = "ui/19";
         this.buttonSprite = CompositeSprite.obtain(upPath, downPath, checkedPath, false);
         upPath = "ui/14";
         downPath = "ui/15";
         this.uncheckedSprite = CompositeSprite.obtain(upPath, downPath, "ui/16", false);
         QuestPartyPanel.this.tabButtonGroup.add(this);
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawCompositeSprite(
               this.isChecked() ? this.buttonSprite : this.uncheckedSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
            );
         super.renderContext.resetBatchColor();
      }

      @Override
      public final void drawLabel() {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               super.text,
               this.getX() + this.getWidth() / 2.05F,
               this.getY() + this.getHeight() / 1.8F,
               Anchor.CENTER,
               20.0F,
               Color.WHITE
            );
      }
   }
}
