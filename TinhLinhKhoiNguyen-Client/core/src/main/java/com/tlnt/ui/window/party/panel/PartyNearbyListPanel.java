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
import com.tlnt.model.party.Party;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;

public final class PartyNearbyListPanel extends GamePanel {
   public Pool<PartyNearbyListPanel.NearbyPartyRow> rowPool = new NearbyPartyRowPool(this);
   public NinePatchAsset background;
   public TextraLabel titleLabel;
   public Table contentTable;

   public PartyNearbyListPanel(RenderContext renderContext) {
      super(renderContext);
      String patchPath = "ui/100";
      this.background = NinePatchAsset.obtain(patchPath, 50);
      this.add(
            this.titleLabel = new TextraLabel(
               "Tổ đội lân cận",
               FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 30)
            )
         )
         .padTop(-5.0F)
         .growX();
      this.titleLabel.setAlignment(2);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.contentTable = new Table().top().left())).setSize(687.0F, 485.0F);
      scrollPane.setPosition(27.0F, 76.0F);
      this.addActor(scrollPane);
      NearbyPartyRefreshButton refreshButton;
      (refreshButton = new NearbyPartyRefreshButton(this, "Làm mới", super.renderContext))
         .addListener(new RefreshNearbyPartiesClickListener(this));
      refreshButton.setSize(133.0F, 50.0F);
      refreshButton.setPosition(38.0F, 11.0F);
      this.addActor(refreshButton);
      CreatePartyButton createButton;
      (createButton = new CreatePartyButton(this, "Tạo tổ đội", super.renderContext)).addListener(new CreatePartyListener(this));
      createButton.setSize(133.0F, 50.0F);
      createButton.setPosition(571.0F, 11.0F);
      this.addActor(createButton);
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
         if ((child = (Actor)it.next()) instanceof PartyNearbyListPanel.NearbyPartyRow) {
            this.rowPool.free((PartyNearbyListPanel.NearbyPartyRow)child);
         }
      }

      this.contentTable.clear();

      for (Party party : GameState.nearbyParties) {
         this.contentTable.add(this.rowPool.obtain().bindParty(party)).height(104.0F).growX().space(10.0F).row();
      }
   }

   @Override
   public final void onShow() {
      ServerPacketSender.getInstance().sendRequestParty();
   }

   public class NearbyPartyRow extends BaseTable {
      public NinePatchAsset background;
      public TextraLabel partyNameLabel;
      public TextraLabel partyInfoLabel;
      public JoinPartyButton joinButton;
      public Party partyData;

      public NearbyPartyRow(RenderContext renderContext) {
         super(renderContext);
         String patchPath = "ui/99";
         this.background = NinePatchAsset.obtain(patchPath, 20);
         this.partyNameLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
         this.partyNameLabel.setAlignment(10);
         this.partyNameLabel.setSize(200.0F, 35.0F);
         this.partyNameLabel.setPosition(121.0F, 50.0F);
         this.addActor(this.partyNameLabel);
         this.partyInfoLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
         this.partyInfoLabel.setAlignment(10);
         this.partyInfoLabel.setSize(200.0F, 35.0F);
         this.partyInfoLabel.setPosition(121.0F, 19.0F);
         this.addActor(this.partyInfoLabel);
         this.joinButton = new JoinPartyButton(this, "Xin vào", super.renderContext);
         this.joinButton.addListener(new JoinPartyClickListener(this));
         this.joinButton.setSize(133.0F, 50.0F);
         this.joinButton.setPosition(544.0F, 27.0F);
         this.addActor(this.joinButton);
      }

      public final PartyNearbyListPanel.NearbyPartyRow bindParty(Party partyData) {
         this.partyData = partyData;
         this.partyNameLabel.setText(partyData.name);
         this.joinButton.setDisabled(partyData.joinDisabled);
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain(this.partyData.leaderSpirit.avatarIconPath), this.getX() + 20.0F, this.getY() + 12.0F, 81.0F, 81.0F, Anchor.BOTTOM_LEFT
            );
         super.draw(batch, parentAlpha);
      }
   }
}
