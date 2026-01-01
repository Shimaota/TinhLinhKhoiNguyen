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
import com.tlnt.model.friendentry.FriendEntry;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;

public final class FriendListPanel extends GamePanel {
   public Pool<FriendListPanel.FriendRow> rowPool = new FriendRowPool(this);
   public Table contentTable;

   public FriendListPanel(RenderContext renderContext) {
      super(renderContext);
      this.pad(10.0F);
      ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.contentTable = new Table().top());
      this.add(scrollPane).grow();
   }

   @Override
   public final void onShow() {
      ServerPacketSender.getInstance().sendRequestMap();
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator it = this.contentTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof FriendListPanel.FriendRow) {
            this.rowPool.free((FriendListPanel.FriendRow)child);
         }
      }

      this.contentTable.clear();

      for (FriendEntry friend : GameState.friends) {
         this.contentTable.add(this.rowPool.obtain().setFriendData(friend)).size(1189.0F, 104.0F).space(10.0F).row();
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
   }

   public class FriendRow extends ShapeButton {
      public NinePatchAsset background;
      public FriendEntry friendData;

      public FriendRow(RenderContext renderContext) {
         super(renderContext);
         this.background = NinePatchAsset.obtain("ui/99", 20);
         this.addListener(new FriendContextMenuListener(this, FriendListPanel.this));
      }

      public final FriendListPanel.FriendRow setFriendData(FriendEntry friendData) {
         this.friendData = friendData;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextureSized(TextureAsset.obtain(this.friendData.avatarIcon), this.getX() + 20.0F, this.getY() + 12.0F, 81.0F, 81.0F, Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, this.friendData.name, this.getX() + 121.0F, this.getY() + 66.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT, "Level: " + this.friendData.level, this.getX() + 121.0F, this.getY() + 35.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.YELLOW
            );
         String statusText;
         if (this.friendData.online) {
            statusText = "Online";
         } else {
            statusText = "Offline";
         }

         super.renderContext.drawText(FontManager.DEFAULT_FONT, statusText, this.getX() + 440.0F, this.getY() + 50.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
         super.renderContext.resetBatchColor();
         super.draw(batch, parentAlpha);
      }

      @Override
      public final void drawBackground() {
      }
   }
}
