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
import com.tlnt.model.party.PartyJoinRequest;
import com.tlnt.model.party.PartyMember;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.world.entity.player.PlayerEntity;

public final class PartyJoinRequestsPanel extends GamePanel {
   public Pool<PartyJoinRequestsPanel.JoinRequestRow> rowPool = new PartyJoinRequestRowPool(this);
   public NinePatchAsset background;
   public TextraLabel titleLabel;
   public Table contentTable;
   public AutoApproveToggleButton autoApproveToggle;
   public PartyRequestsDisbandButton disbandButton;

   public PartyJoinRequestsPanel(RenderContext renderContext) {
      super(renderContext);
      this.background = NinePatchAsset.obtain("ui/100", 50);
      this.add(
            this.titleLabel = new TextraLabel(
               "Yêu cầu", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 30)
            )
         )
         .padTop(-5.0F)
         .growX();
      this.titleLabel.setAlignment(2);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.contentTable = new Table().top().left())).setSize(687.0F, 485.0F);
      scrollPane.setPosition(27.0F, 76.0F);
      this.addActor(scrollPane);
      this.autoApproveToggle = new AutoApproveToggleButton(this, super.renderContext);
      this.autoApproveToggle.setSize(55.0F, 55.0F);
      this.autoApproveToggle.setPosition(27.0F, 10.0F);
      this.addActor(this.autoApproveToggle);
      this.autoApproveToggle.addListener(new AutoApproveJoinRequestsListener(this));
      this.disbandButton = new PartyRequestsDisbandButton(this, "Giải tán", super.renderContext);
      this.disbandButton.addListener(new ClosePartyJoinRequestsListener(this));
      this.disbandButton.setSize(133.0F, 50.0F);
      this.disbandButton.setPosition(571.0F, 11.0F);
      this.addActor(this.disbandButton);
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
         if ((child = (Actor)it.next()) instanceof PartyJoinRequestsPanel.JoinRequestRow) {
            this.rowPool.free((PartyJoinRequestsPanel.JoinRequestRow)child);
         }
      }

      this.contentTable.clear();
      if (GameState.currentParty != null) {
         for (PartyJoinRequest request : GameState.currentParty.joinRequests) {
            this.contentTable.add(this.rowPool.obtain().bindRequest(request)).height(104.0F).growX().space(10.0F).row();
         }

         this.autoApproveToggle
            .setVisible(
               ((GameState.currentParty.getMemberRole(PlayerEntity.getInstance().getId()) == PartyMember.Role.LEADER
                  ? 1
                  : 0) != 0)
            );
         this.autoApproveToggle.setChecked(GameState.currentParty.autoApprove);
      }
   }

   public class JoinRequestRow extends BaseTable {
      public NinePatchAsset rowBackground;
      public TextraLabel playerNameLabel;
      public TextraLabel playerLevelLabel;
      public PartyJoinRequestAcceptButton acceptButton;
      public PartyJoinRequest joinRequest;

      public JoinRequestRow(RenderContext renderContext) {
         super(renderContext);
         String patchPath = "ui/99";
         this.rowBackground = NinePatchAsset.obtain(patchPath, 20);
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
         this.acceptButton = new PartyJoinRequestAcceptButton(this, "Đồng ý", super.renderContext);
         this.acceptButton.setSize(133.0F, 50.0F);
         this.acceptButton.setPosition(544.0F, 27.0F);
         this.addActor(this.acceptButton);
         this.acceptButton.addListener(new AcceptPartyJoinRequestListener(this));
      }

      public final PartyJoinRequestsPanel.JoinRequestRow bindRequest(PartyJoinRequest joinRequest) {
         this.joinRequest = joinRequest;
         this.playerNameLabel.setText(this.joinRequest.name);
         this.playerLevelLabel.setText("Cấp: " + this.joinRequest.level);
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.rowBackground, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain(this.joinRequest.spirit.avatarIconPath), this.getX() + 20.0F, this.getY() + 12.0F, 81.0F, 81.0F, Anchor.BOTTOM_LEFT
            );
         super.draw(batch, parentAlpha);
      }
   }
}
