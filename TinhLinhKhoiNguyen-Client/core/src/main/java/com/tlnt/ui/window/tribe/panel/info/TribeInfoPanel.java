package com.tlnt.ui.window.tribe.panel.info;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.DeformableSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.util.GameUtils;
import com.tlnt.util.TimeFormatter;
import com.tlnt.world.entity.player.PlayerEntity;

public final class TribeInfoPanel extends GamePanel {
   public NinePatchAsset panelBackground;
   public TextureAsset badgeBorderTexture;
   public DeformableSprite xpArcSprite;
   public TextureAsset badgeInnerTexture;
   public TextureAsset levelBadgeTexture;
   public DisbandTribeButton disbandButton;
   public SetMottoButton setMottoButton;
   public LeaveTribeButton leaveTribeButton;
   public TextraLabel infoLabel;

   public TribeInfoPanel(RenderContext renderContext) {
      super(renderContext);
      this.panelBackground = NinePatchAsset.obtain("ui/100", 50);
      this.badgeBorderTexture = TextureAsset.obtain("ui/144");
      this.xpArcSprite = new DeformableSprite(TextureAsset.obtain("ui/2").getRegion());
      this.badgeInnerTexture = TextureAsset.obtain("ui/1");
      this.levelBadgeTexture = TextureAsset.obtain("ui/4");
      DonateGoldButton donateGoldButton;
      (donateGoldButton = new DonateGoldButton(this, "Góp vàng", super.renderContext)).setSize(139.0F, 49.0F);
      donateGoldButton.setPosition(14.0F, 15.0F);
      this.addActor(donateGoldButton);
      donateGoldButton.addListener(new DonateGoldButtonListener(this));
      DonateSpiritStoneButton donateStoneButton;
      (donateStoneButton = new DonateSpiritStoneButton(this, "Góp L.Thạch", super.renderContext)).setSize(139.0F, 49.0F);
      donateStoneButton.setPosition(163.0F, 15.0F);
      this.addActor(donateStoneButton);
      donateStoneButton.addListener(new DonateSpiritStoneButtonListener(this));
      this.setMottoButton = new SetMottoButton(this, "Khẩu hiệu", super.renderContext);
      this.setMottoButton.setSize(139.0F, 49.0F);
      this.setMottoButton.setPosition(312.0F, 15.0F);
      this.addActor(this.setMottoButton);
      this.setMottoButton.addListener(new SetMottoButtonListener(this));
      this.disbandButton = new DisbandTribeButton(this, "Giải tán", super.renderContext);
      this.disbandButton.setSize(139.0F, 49.0F);
      this.disbandButton.setPosition(461.0F, 15.0F);
      this.addActor(this.disbandButton);
      this.disbandButton.addListener(new DisbandTribeButtonListener(this));
      this.leaveTribeButton = new LeaveTribeButton(this, "Rời đi", super.renderContext);
      this.leaveTribeButton.setSize(139.0F, 49.0F);
      this.leaveTribeButton.setPosition(461.0F, 15.0F);
      this.addActor(this.leaveTribeButton);
      this.leaveTribeButton.addListener(new LeaveTribeButtonListener(this));
      Table infoTable = new Table().top().left().padBottom(15.0F);
      infoTable
         .add(
            this.infoLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 3.0F))
         )
         .growX()
         .row();
      this.infoLabel.setAlignment(10);
      this.infoLabel.setWrap(true);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(infoTable)).setSize(564.0F, 323.0F);
      scrollPane.setPosition(26.0F, 85.0F);
      this.addActor(scrollPane);
   }

   @Override
   public final void refresh() {
      if (GameState.tribeData.myTribe != null) {
         this.leaveTribeButton.setVisible(GameState.tribeData.myTribe.isLeader(PlayerEntity.getInstance().getId()));
         LeaveTribeButton leaveButton = this.leaveTribeButton;
         int visibleFlag;
         if (!GameState.tribeData.myTribe.isLeader(PlayerEntity.getInstance().getId())) {
            visibleFlag = 1;
         } else {
            visibleFlag = 0;
         }

         leaveButton.setVisible((visibleFlag != 0));
         StringBuilder info;
         StringBuilder builder = info = new StringBuilder();
         StringBuilder emblemLine = builder.append("Biểu tượng bang: ")
            .append(GameState.tribeData.myTribe.emblem.name)
            .append(" (");
         String expireText;
         if (GameState.tribeData.myTribe.emblemExpireTime == -1L) {
            expireText = "vĩnh viễn";
         } else {
            expireText = TimeFormatter.formatTimeRemaining(GameState.tribeData.myTribe.emblemExpireTime);
         }

         emblemLine.append(expireText).append(")\n");
         StringBuilder leaderLine = info.append("Tọt trưởng: " + GameState.tribeData.myTribe.leaderName);
         leaderLine.append("\n");
         info.append("Khẩu hiệu: " + GameState.tribeData.myTribe.slogan);
         this.infoLabel.setText(info.toString());
      } else {
         this.infoLabel.setText("");
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.panelBackground, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Thông tin Bộ lạc",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + 577.0F,
            Anchor.BOTTOM_CENTER,
            30.0F,
            Color.WHITE
         );
      if (GameState.tribeData.myTribe != null) {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Tên bộ lạc: " + GameState.tribeData.myTribe.name,
               this.getX() + 188.0F,
               this.getY() + 530.0F,
               Anchor.BOTTOM_LEFT,
               24.0F,
               Color.YELLOW
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Cấp độ: " + GameState.tribeData.myTribe.level + " (" + GameState.tribeData.myTribe.exp + "/" + GameState.tribeData.myTribe.expMax + ")",
               this.getX() + 188.0F,
               this.getY() + 490.0F,
               Anchor.BOTTOM_LEFT,
               24.0F,
               Color.YELLOW
            );
      }

      super.renderContext.drawTexture(this.badgeBorderTexture, this.getX() + 26.0F, this.getY() + 453.0F, Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawRadialSprite(
            this.xpArcSprite,
            this.getX() + 38.0F,
            this.getY() + 462.0F,
            115.0F,
            115.0F,
            Anchor.BOTTOM_LEFT,
            Math.max(1.0F, 360.0F * Math.min((float)GameState.tribeData.myTribe.exp / (float)GameState.tribeData.myTribe.expMax, 1.0F))
         );
      super.renderContext.drawTexture(this.badgeInnerTexture, this.getX() + 42.0F, this.getY() + 466.0F, Anchor.BOTTOM_LEFT);
      if (GameState.tribeData.myTribe.emblem != null && GameState.tribeData.myTribe.emblem.framePaths.length != 0) {
         super.renderContext
            .drawTexture(
               TextureAsset.obtain(
                  GameState.tribeData.myTribe.emblem.framePaths[GameUtils.timeFrameIndex(
                     GameState.tribeData.myTribe.emblem.framePaths.length, GameState.tribeData.myTribe.emblem.frameRate
                  )]
               ),
               this.getX() + 42.0F + (float)this.badgeInnerTexture.getWidth() / 2.0F,
               this.getY() + 466.0F + (float)this.badgeInnerTexture.getHeight() / 2.0F,
               Anchor.CENTER
            );
      }

      super.renderContext.drawTexture(this.levelBadgeTexture, this.getX() + 28.0F, this.getY() + 426.0F, Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            GameState.tribeData.myTribe.level + "",
            this.getX() + 25.0F + (float)this.levelBadgeTexture.getWidth() / 2.0F,
            this.getY() + 453.0F,
            Anchor.CENTER,
            24.0F,
            Color.YELLOW
         );
      super.draw(batch, parentAlpha);
   }

}
