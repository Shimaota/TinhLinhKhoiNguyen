package com.tlnt.ui.window.party.panel;

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
import com.tlnt.model.party.PartyMember;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.world.entity.player.PlayerEntity;

public final class PartyNearbyPlayersPanel extends GamePanel {
   public Pool<PartyNearbyPlayersPanel.NearbyPlayerRow> rowPool = new NearbyPlayerRowPool(this);
   public NinePatchAsset background;
   public TextraLabel titleLabel;
   public Table contentTable;
   public AutoApprovePartyMembersToggle autoApproveToggle;

   public PartyNearbyPlayersPanel(RenderContext renderContext) {
      super(renderContext);
      String patchPath = "ui/100";
      this.background = NinePatchAsset.obtain(patchPath, 50);
      this.add(
            this.titleLabel = new TextraLabel(
               "Người chơi lân cận", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 30)
            )
         )
         .padTop(-5.0F)
         .growX();
      this.titleLabel.setAlignment(2);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.contentTable = new Table().top().left())).setSize(687.0F, 485.0F);
      scrollPane.setPosition(27.0F, 76.0F);
      this.addActor(scrollPane);
      this.autoApproveToggle = new AutoApprovePartyMembersToggle(this, super.renderContext);
      this.autoApproveToggle.setSize(55.0F, 55.0F);
      this.autoApproveToggle.setPosition(27.0F, 10.0F);
      this.addActor(this.autoApproveToggle);
      this.autoApproveToggle.addListener(new RequestFriendsClickListener2(this));
      DisbandPartyButton disbandButton;
      (disbandButton = new DisbandPartyButton(this, "Giải tán", super.renderContext))
         .addListener(new CloseNearbyPlayersListener(this));
      disbandButton.setSize(133.0F, 50.0F);
      disbandButton.setPosition(571.0F, 11.0F);
      this.addActor(disbandButton);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator it = this.contentTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof PartyNearbyPlayersPanel.NearbyPlayerRow) {
            this.rowPool.free((PartyNearbyPlayersPanel.NearbyPlayerRow)child);
         }
      }

      this.contentTable.clear();
      it = GameState.nearbyPlayers.iterator();

      while (it.hasNext()) {
         PlayerEntity player;
         if (!(player = (PlayerEntity)it.next()).isLocalPlayer() && !player.isMountClass()) {
            int classFlag;
            label44: {
               if (player.POOL2 != null) {
                  if (player.POOL2.classId == 9) {
                     classFlag = 1;
                     break label44;
                  }
               }

               classFlag = 0;
            }

            if (classFlag == 0) {
               label38: {
                  if (player.POOL2 != null) {
                     if (player.POOL2.classId == 10) {
                        classFlag = 1;
                        break label38;
                     }
                  }

                  classFlag = 0;
               }

               if (classFlag == 0) {
                  this.contentTable.add(this.rowPool.obtain().setPlayer(player)).height(104.0F).growX().space(10.0F).row();
               }
            }
         }
      }

      AutoApprovePartyMembersToggle toggle = this.autoApproveToggle;
      int leaderFlag;
      if (GameState.currentParty.getMemberRole(PlayerEntity.getInstance().getId()) == PartyMember.Role.LEADER) {
         leaderFlag = 1;
      } else {
         leaderFlag = 0;
      }

      toggle.setVisible((leaderFlag != 0));
      this.autoApproveToggle.setChecked(GameState.currentParty.autoApprove);
   }

   public class NearbyPlayerRow extends BaseTable {
      public NinePatchAsset background;
      public TextraLabel playerNameLabel;
      public TextraLabel playerLevelLabel;
      public InviteNearbyPlayerButton inviteButton;
      public PlayerEntity playerEntity;

      public NearbyPlayerRow(RenderContext renderContext) {
         super(renderContext);
         String patchPath = "ui/99";
         this.background = NinePatchAsset.obtain(patchPath, 20);
         this.playerNameLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
         this.playerNameLabel.setAlignment(10);
         this.playerNameLabel.setSize(200.0F, 35.0F);
         this.playerNameLabel.setPosition(121.0F, 50.0F);
         this.addActor(this.playerNameLabel);
         this.playerLevelLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
         this.playerLevelLabel.setAlignment(10);
         this.playerLevelLabel.setSize(200.0F, 35.0F);
         this.playerLevelLabel.setPosition(121.0F, 19.0F);
         this.addActor(this.playerLevelLabel);
         this.inviteButton = new InviteNearbyPlayerButton(this, "Mời tổ đội", super.renderContext);
         this.inviteButton.setSize(133.0F, 50.0F);
         this.inviteButton.setPosition(544.0F, 27.0F);
         this.addActor(this.inviteButton);
         this.inviteButton.addListener(new InvitePlayerClickListener(this));
      }

      public final PartyNearbyPlayersPanel.NearbyPlayerRow setPlayer(PlayerEntity playerEntity) {
         this.playerEntity = playerEntity;
         this.playerNameLabel.setText(this.playerEntity.getCharacterName());
         this.playerLevelLabel.setText("Cấp: " + this.playerEntity.POOL2.level);
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain(this.playerEntity.POOL2.avatarTexture), this.getX() + 20.0F, this.getY() + 12.0F, 81.0F, 81.0F, Anchor.BOTTOM_LEFT
            );
         super.draw(batch, parentAlpha);
      }
   }
}
