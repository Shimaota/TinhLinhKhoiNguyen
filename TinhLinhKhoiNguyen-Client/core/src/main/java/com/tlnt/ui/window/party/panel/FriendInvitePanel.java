package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.friendentry.FriendInviteEntry;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;

public final class FriendInvitePanel extends GamePanel {
   public Pool<FriendInvitePanel.InviteRow> rowPool = new FriendInviteRowPool(this);
   public Table contentTable;

   public FriendInvitePanel(RenderContext renderContext) {
      super(renderContext);
      this.pad(10.0F);
      ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.contentTable = new Table().top());
      this.add(scrollPane).grow();
   }

   @Override
   public final void onShow() {
      ServerPacketSender.getInstance().sendRequestSkills();
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator it = this.contentTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof FriendInvitePanel.InviteRow) {
            this.rowPool.free((FriendInvitePanel.InviteRow)child);
         }
      }

      this.contentTable.clear();

      for (FriendInviteEntry invite : GameState.friendInvites) {
         this.contentTable.add(this.rowPool.obtain().bindEntry(invite)).size(1189.0F, 104.0F).space(10.0F).row();
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
   }

   public class InviteRow extends BaseTable {
      public NinePatchAsset background;
      public FriendInviteAcceptButton acceptButton;
      public LearnSkillButton inviteFriendButton;
      public UpgradeSkillButton declineButton;
      public FriendInviteEntry inviteEntry;

      public InviteRow(RenderContext renderContext) {
         super(renderContext);
         this.background = NinePatchAsset.obtain("ui/99", 20);
         this.acceptButton = new FriendInviteAcceptButton(this, "Thông tin", super.renderContext);
         this.acceptButton.addListener(new FriendInviteAcceptClickListener(this));
         this.inviteFriendButton = new LearnSkillButton(this, "Đồng ý", super.renderContext);
         this.declineButton = new UpgradeSkillButton(this, "Từ chối", super.renderContext);
         this.acceptButton.setSize(133.0F, 50.0F);
         this.acceptButton.setPosition(770.0F, 28.0F);
         this.addActor(this.acceptButton);
         this.inviteFriendButton.setSize(133.0F, 50.0F);
         this.inviteFriendButton.setPosition(908.0F, 28.0F);
         this.addActor(this.inviteFriendButton);
         this.declineButton.setSize(133.0F, 50.0F);
         this.declineButton.setPosition(1046.0F, 28.0F);
         this.addActor(this.declineButton);
         this.inviteFriendButton.addListener(new InviteFriendClickListener(this));
         this.declineButton.addListener(new UpgradeSkillClickListener(this));
      }

      public final FriendInvitePanel.InviteRow bindEntry(FriendInviteEntry inviteEntry) {
         this.inviteEntry = inviteEntry;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextureSized(TextureAsset.obtain(this.inviteEntry.avatarIcon), this.getX() + 20.0F, this.getY() + 12.0F, 81.0F, 81.0F, Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, this.inviteEntry.name, this.getX() + 121.0F, this.getY() + 66.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT, "Level: " + this.inviteEntry.level, this.getX() + 121.0F, this.getY() + 35.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.YELLOW
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.inviteEntry.online ? "Online" : "Offline",
               this.getX() + 440.0F,
               this.getY() + 50.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               Color.WHITE
            );
         super.draw(batch, parentAlpha);
      }
   }
}
