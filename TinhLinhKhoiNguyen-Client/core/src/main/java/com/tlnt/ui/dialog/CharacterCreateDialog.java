package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.DeformableSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.model.character.CharacterTemplate;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.utils.SpineSkeletonActor;
import com.tlnt.ui.dialog.widget.SpineCharacterPreview;
import com.tlnt.ui.dialog.widget.StyledTextField;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.WindowType;
import com.tlnt.util.GameUtils;

public final class CharacterCreateDialog extends SlideInDialog {
   public Pool<CharacterCreateDialog.ClassSelectButton> classButtonPool = new CharacterButtonPool(this);
   public TextureAsset backgroundTexture;
   public Table classListTable;
   public SpineCharacterPreview characterViewer;
   public Table statsContentTable;
   public CharacterTemplate selectedClass;
   public float animationTime;
   public float animationDuration;
   public CharacterCreateDialog.StatBar[] statBars;
   public StyledTextField nameInput;

   public CharacterCreateDialog(RenderContext renderContext) {
      super(renderContext, WindowType.CREATE_PLAYER_PANEL);
      this.backgroundTexture = TextureAsset.obtain("ui/BG2");
      this.animationDuration = 1.0F;
      CharacterCreateBackButton backButton;
      (backButton = new CharacterCreateBackButton(this, super.renderContext)).setSize(100.0F, 100.0F);
      backButton.setPosition(30.0F, 970.0F);
      this.addActor(backButton);
      backButton.addListener(new CreateCharacterClickListener2(this));
      this.classListTable = new Table().pad(33.0F);
      ScrollPane classScrollPane;
      ScrollPane scrollPane = classScrollPane = ScrollPaneFactory.createScrollPane(this.classListTable);
      scrollPane.setScrollingDisabled(true, false);
      CharacterCreateBackgroundPanel classListPanel;
      (classListPanel = new CharacterCreateBackgroundPanel(this, super.renderContext)).setSize(202.0F, 644.0F);
      classListPanel.setPosition(44.0F, 168.0F);
      classListPanel.add(classScrollPane).grow();
      this.addActor(classListPanel);
      this.characterViewer = new SpineCharacterPreview(super.renderContext);
      this.characterViewer.setPosition(700.0F, 287.0F);
      this.addActor(this.characterViewer);
      Table previewPanel;
      (previewPanel = new CharacterPreviewPanel(this, super.renderContext).padTop(75.0F)).setSize(738.0F, 604.0F);
      previewPanel.setPosition(1138.0F, 169.0F);
      this.addActor(previewPanel);
      ScrollPane statsScrollPane = ScrollPaneFactory.createScrollPane(this.statsContentTable = new Table().pad(5.0F));
      previewPanel.add(statsScrollPane).grow();
      CharacterStatsPanel statsPanel;
      (statsPanel = new CharacterStatsPanel(this, super.renderContext)).setSize(553.0F, 202.0F);
      statsPanel.setPosition(387.0F, 41.0F);
      this.addActor(statsPanel);
      this.statBars = new CharacterCreateDialog.StatBar[4];
      this.statBars[0] = new CharacterCreateDialog.StatBar(super.renderContext);
      this.statBars[0].setSize(370.0F, 23.0F);
      this.statBars[0].setPosition(165.0F, 162.0F);
      statsPanel.addActor(this.statBars[0]);
      this.statBars[1] = new CharacterCreateDialog.StatBar(super.renderContext);
      this.statBars[1].setSize(370.0F, 23.0F);
      this.statBars[1].setPosition(165.0F, 114.0F);
      statsPanel.addActor(this.statBars[1]);
      this.statBars[2] = new CharacterCreateDialog.StatBar(super.renderContext);
      this.statBars[2].setSize(370.0F, 23.0F);
      this.statBars[2].setPosition(165.0F, 66.0F);
      statsPanel.addActor(this.statBars[2]);
      this.statBars[3] = new CharacterCreateDialog.StatBar(super.renderContext);
      this.statBars[3].setSize(370.0F, 23.0F);
      this.statBars[3].setPosition(165.0F, 18.0F);
      statsPanel.addActor(this.statBars[3]);
      Table namePanel;
      (namePanel = new CharacterNameInputPanel(this, super.renderContext).pad(20.0F)).setSize(315.0F, 78.0F);
      namePanel.setPosition(501.0F, 684.0F);
      this.addActor(namePanel);
      namePanel.add(this.nameInput = new StyledTextField("", super.renderContext)).grow();
      this.nameInput.setMessageText("Tên nhân vật");
      CharacterCreateTextButton createButton;
      (createButton = new CharacterCreateTextButton(this, "Tạo nhân vật", super.renderContext)).setSize(169.0F, 63.0F);
      createButton.setPosition(821.0F, 691.0F);
      this.addActor(createButton);
      createButton.addListener(new CreateCharacterClickListener(this));
      SpineSkeletonActor logoActor;
      (logoActor = new SpineSkeletonActor()).setRenderer(super.renderContext.getSkeletonRenderer());
      NamedPair skeletonPair = SkeletonLoader.obtainSkeleton("ui/logo", 100.0F, new SkeletonLoader.AnimationMixConfig());
      logoActor.setAnimationState((AnimationState)skeletonPair.first);
      logoActor.setSkeleton((Skeleton)skeletonPair.second);
      AnimationState animState = logoActor.getAnimationState();
      String animationName = "animation";
      animState.setAnimation(0, animationName, true);
      logoActor.setPosition(960.0F, 820.0F);
      this.addActor(logoActor);
   }

   @Override
   public final void onClose() {
      if (this.getStage() != null) {
         this.getStage().setKeyboardFocus(null);
      }
   }

   public void submitCharacterCreation() {
      String name;
      if (!(name = this.nameInput.getText()).trim().isEmpty()) {
         ServerPacketSender.getInstance().sendEncryptedPacket(name, this.selectedClass.id);
      }
   }

   @Override
   public final void onUpdate(float delta) {
      delta = Math.min(1.0F, (this.animationTime += delta) / this.animationDuration);
      delta = Interpolation.fastSlow.apply(0.0F, 1.0F, delta);
      CharacterCreateDialog.StatBar[] bars = this.statBars;
      int count = this.statBars.length;

      for (int i = 0; i < count; i++) {
         CharacterCreateDialog.StatBar bar;
         (bar = bars[i]).setOffsetScale(delta);
      }
   }

   @Override
   public final void onClose2() {
      Array.ArrayIterator it = this.classListTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof CharacterCreateDialog.ClassSelectButton) {
            this.classButtonPool.free((CharacterCreateDialog.ClassSelectButton)child);
         }
      }

      this.classListTable.clear();
      Array templates = CharacterTemplate.getSelectable();

      for (int i = 0; i < templates.size; i++) {
         CharacterTemplate template = (CharacterTemplate)templates.get(i);
         CharacterCreateDialog.ClassSelectButton classButton = this.classButtonPool.obtain().setCharacterClass(template);
         this.classListTable.add(classButton).size(136.0F, 162.0F).space(46.0F).row();
         if (i == 0) {
            classButton.setChecked(true);
            this.selectCharacterClass(template);
         }
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

   public void selectCharacterClass(CharacterTemplate selectedClass) {
      if (selectedClass != null) {
         this.animationTime = 0.0F;
         this.selectedClass = selectedClass;
         SpineCharacterPreview viewer = this.characterViewer;
         String skeletonPath = this.selectedClass.skeletonPath;
         String[] skinNames = new String[1];
         skinNames[0] = "skin/lv1";
         viewer.loadSkeletonAndPlayIdle(skeletonPath, skinNames);
         if (this.selectedClass.infoTable == null) {
            this.selectedClass.infoTable = this.selectedClass.uiBuilder.buildLayout(super.renderContext, this.selectedClass.infoTableJson);
         }

         this.statsContentTable.clear();
         this.statsContentTable.add(this.selectedClass.infoTable).grow();

         for (int i = 0; i < selectedClass.statRatings.length; i++) {
            if (i < this.statBars.length) {
               this.statBars[i].setFillRatio(selectedClass.statRatings[i]);
            }
         }
      }
   }

   public class ClassSelectButton extends ShapeButton {

      public final CharacterCreateDialog dialog = CharacterCreateDialog.this;
      public TextureAsset frameTexture;
      public DeformableSprite portraitSprite;
      public TextureAsset overlayTexture;
      public TextureAsset uncheckIndicator;
      public TextureAsset checkIndicator;
      public CharacterTemplate characterClass;

      public ClassSelectButton(RenderContext renderContext) {
         super(renderContext);
         this.frameTexture = TextureAsset.obtain("ui/144");
         this.portraitSprite = new DeformableSprite(TextureAsset.obtain("ui/2").getRegion());
         this.overlayTexture = TextureAsset.obtain("ui/1");
         this.uncheckIndicator = TextureAsset.obtain("ui/4");
         this.checkIndicator = TextureAsset.obtain("ui/150");
         this.addListener(new SelectCharacterClickListener(this, CharacterCreateDialog.this));
      }

      public final CharacterCreateDialog.ClassSelectButton setCharacterClass(CharacterTemplate characterClass) {
         this.characterClass = characterClass;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawTextureSized(this.frameTexture, this.getX(), this.getY() + 27.0F, 136.0F, 135.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.drawRadialSprite(this.portraitSprite, this.getX() + 12.0F, this.getY() + 36.0F, 115.0F, 115.0F, Anchor.BOTTOM_LEFT, 360.0F);
         super.renderContext.drawTexture(this.overlayTexture, this.getX() + 16.0F, this.getY() + 40.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.drawTexture(this.uncheckIndicator, this.getX() + 3.0F, this.getY(), Anchor.BOTTOM_LEFT);
         super.renderContext.drawTexture(TextureAsset.obtain(this.characterClass.avatarIconPath), this.getX() + 16.0F, this.getY() + 40.0F, Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.PLAIN_FONT,
               "1",
               this.getX() + 68.0F,
               this.getY() + 25.0F,
               Anchor.CENTER,
               18.0F,
               Color.WHITE
            );
         if (this.isChecked()) {
            super.renderContext
               .drawTexture(
                  this.checkIndicator,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() + GameUtils.oscillate(1.0F, 5.0F, 10.0F),
                  Anchor.BOTTOM_CENTER
               );
         }

         super.renderContext.resetBatchColor();
      }
   }

   public class StatBar extends BaseTable {
      public CompositeSprite sprite;
      public float offsetScale;
      public float fillRatio;

      public StatBar(RenderContext renderContext) {
         super(renderContext);
         String upPath = "ui/156";
         String downPath = "ui/157";
         String checkedPath = "ui/158";
         this.sprite = CompositeSprite.obtain(upPath, downPath, checkedPath, false);
         this.fillRatio = 1.0F;
      }

      public final void setFillRatio(float fillRatio) {
         this.fillRatio = fillRatio;
      }

      public final void setOffsetScale(float offsetScale) {
         this.offsetScale = offsetScale;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawCompositeSprite(
               this.sprite, this.getX(), this.getY(), this.getWidth() * this.offsetScale * this.fillRatio, this.getHeight(), Anchor.BOTTOM_LEFT
            );
         super.draw(batch, parentAlpha);
      }
   }
}
