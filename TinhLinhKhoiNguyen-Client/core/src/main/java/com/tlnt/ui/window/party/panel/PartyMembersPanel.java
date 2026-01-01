package com.tlnt.ui.window.party.panel;

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
import com.tlnt.model.party.PartyMember;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.world.entity.player.PlayerEntity;

public final class PartyMembersPanel extends GamePanel {
   public Pool<PartyMembersPanel.MemberRow> rowPool = new PartyMemberRowPool(this);
   public NinePatchAsset background;
   public TextraLabel titleLabel;
   public Table contentTable;
   public AutoApproveToggle autoApproveToggle;
   public DisbandPartyButton2 disbandButton;
   public LeavePartyButton leaveButton;

   public PartyMembersPanel(RenderContext renderContext) {
      super(renderContext);
      String patchPath = "ui/100";
      this.background = NinePatchAsset.obtain(patchPath, 50);
      String titleText = "Thành viên tổ đội";
      this.add(this.titleLabel = new TextraLabel(titleText, FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 30)))
         .padTop(-5.0F)
         .growX();
      this.titleLabel.setAlignment(2);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.contentTable = new Table().top().left())).setSize(687.0F, 485.0F);
      scrollPane.setPosition(27.0F, 76.0F);
      this.addActor(scrollPane);
      this.autoApproveToggle = new AutoApproveToggle(this, super.renderContext);
      this.autoApproveToggle.setSize(55.0F, 55.0F);
      this.autoApproveToggle.setPosition(27.0F, 10.0F);
      this.addActor(this.autoApproveToggle);
      this.autoApproveToggle.addListener(new RequestFriendsClickListener(this));
      this.disbandButton = new DisbandPartyButton2(this, "Giải tán", super.renderContext);
      this.disbandButton.addListener(new DisbandPartyClickListener(this));
      this.disbandButton.setSize(133.0F, 50.0F);
      this.disbandButton.setPosition(571.0F, 11.0F);
      this.addActor(this.disbandButton);
      this.leaveButton = new LeavePartyButton(this, "Rời nhóm", super.renderContext);
      this.leaveButton.addListener(new LeavePartyListener(this));
      this.leaveButton.setSize(133.0F, 50.0F);
      this.leaveButton.setPosition(571.0F, 11.0F);
      this.addActor(this.leaveButton);
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator it = this.contentTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof PartyMembersPanel.MemberRow) {
            this.rowPool.free((PartyMembersPanel.MemberRow)child);
         }
      }

      this.contentTable.clear();
      if (GameState.currentParty != null) {
         this.disbandButton
            .setVisible(
               ((GameState.currentParty.getMemberRole(PlayerEntity.getInstance().getId()) == PartyMember.Role.LEADER
                  ? 1
                  : 0) != 0)
            );
         this.leaveButton
            .setVisible(
               ((GameState.currentParty.getMemberRole(PlayerEntity.getInstance().getId()) != PartyMember.Role.LEADER
                  ? 1
                  : 0) != 0)
            );

         for (PartyMember member : GameState.currentParty.members) {
            this.contentTable.add(this.rowPool.obtain().bindMember(member)).height(104.0F).growX().space(10.0F).row();
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

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

   public class MemberRow extends ShapeButton {
      public NinePatchAsset background;
      public TextraLabel nameLabel;
      public TextraLabel levelLabel;
      public TextraLabel roleLabel;
      public PartyMember memberData;

      public MemberRow(RenderContext renderContext) {
         super(renderContext);
         this.background = NinePatchAsset.obtain("ui/99", 20);
         this.nameLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
         this.nameLabel.setAlignment(10);
         this.nameLabel.setSize(200.0F, 35.0F);
         this.nameLabel.setPosition(121.0F, 50.0F);
         this.addActor(this.nameLabel);
         this.levelLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
         this.levelLabel.setAlignment(10);
         this.levelLabel.setSize(200.0F, 35.0F);
         this.levelLabel.setPosition(121.0F, 19.0F);
         this.addActor(this.levelLabel);
         this.roleLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
         this.roleLabel.setAlignment(18);
         this.roleLabel.setSize(200.0F, 35.0F);
         this.roleLabel.setPosition(472.0F, 36.0F);
         this.addActor(this.roleLabel);
         this.addListener(new PartyMemberItemActionListener(this, PartyMembersPanel.this));
      }

      public final PartyMembersPanel.MemberRow bindMember(PartyMember memberData) {
         this.memberData = memberData;
         this.nameLabel.setText(this.memberData.name + " (" + this.memberData.charClass.name + ")");
         this.levelLabel.setText("[yellow]Cấp: " + this.memberData.level);
         this.roleLabel.setText(this.memberData.role.label);
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain(this.memberData.spirit.avatarIconPath), this.getX() + 20.0F, this.getY() + 12.0F, 81.0F, 81.0F, Anchor.BOTTOM_LEFT
            );
         super.renderContext.resetBatchColor();
         super.draw(batch, parentAlpha);
      }

      @Override
      public final void drawBackground() {
      }
   }
}
