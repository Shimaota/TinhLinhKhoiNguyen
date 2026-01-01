package com.tlnt.ui.window.party.tab;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.hud.widget.TargetMarker;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.party.panel.FriendInvitePanel;
import com.tlnt.ui.window.party.panel.FriendListPanel;

public final class PartyFriendsTab extends GamePanel {
   public PartyFriendsTab.FriendTabButton friendListTabButton;
   public PartyFriendsTab.FriendTabButton friendInviteTabButton;
   public Group contentContainer;
   public FriendListPanel friendListPanel;
   public FriendInvitePanel friendInvitePanel;

   public PartyFriendsTab(RenderContext renderContext) {
      super(renderContext);
      Table tabBar;
      (tabBar = new FriendTabButtonBar(this, super.renderContext).pad(10.0F).left()).setSize(632.0F, 92.0F);
      tabBar.setPosition(308.0F, 558.0F);
      this.addActor(tabBar);
      ButtonGroup buttonGroup = new ButtonGroup();
      tabBar.add(this.friendListTabButton = new PartyFriendsTab.FriendTabButton("Danh sách bạn bè", super.renderContext))
         .size(301.0F, 72.0F)
         .space(10.0F);
      tabBar.add(this.friendInviteTabButton = new PartyFriendsTab.FriendTabButton("Lời mời", super.renderContext)).size(301.0F, 72.0F).space(10.0F);
      TargetMarker badgeMarker;
      (badgeMarker = new TargetMarker(super.renderContext, new FriendInvitePendingChecker(this))).setSize(15.0F, 15.0F);
      badgeMarker.setPosition(290.0F, 60.0F);
      this.friendInviteTabButton.addActor(badgeMarker);
      Button[] tabButtons = new Button[2];
      tabButtons[0] = this.friendListTabButton;
      tabButtons[1] = this.friendInviteTabButton;
      buttonGroup.add(tabButtons);
      FriendTabBackground tabBackground;
      (tabBackground = new FriendTabBackground(this, super.renderContext)).setSize(1247.0F, 548.0F);
      this.addActor(tabBackground);
      tabBackground.add(this.contentContainer = new Group()).grow();
      this.contentContainer.addActor(this.friendListPanel = new FriendListPanel(super.renderContext));
      this.friendListPanel.setFillParent(true);
      this.contentContainer.addActor(this.friendInvitePanel = new FriendInvitePanel(super.renderContext));
      this.friendInvitePanel.setFillParent(true);
      this.friendInvitePanel.setVisible(false);
      this.friendListTabButton.addListener(new FriendListTabClickListener(this));
      this.friendInviteTabButton.addListener(new FriendInviteTabClickListener(this));
   }

   public static class FriendTabButton extends TextButton {
      public CompositeSprite buttonSprite;

      public FriendTabButton(String text, RenderContext renderContext) {
         super(text, renderContext);
         this.buttonSprite = CompositeSprite.obtain(
            "ui/109",
            "ui/110",
            "ui/111",
            false
         );
      }

      @Override
      public final void drawBackground() {
         if (this.isChecked()) {
            super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         }
      }

      @Override
      public final void drawLabel() {
         super.renderContext
            .drawText(
               FontManager.PLAIN_FONT,
               super.text,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               Anchor.CENTER,
               30.0F,
               Color.BROWN
            );
      }

   }
}
