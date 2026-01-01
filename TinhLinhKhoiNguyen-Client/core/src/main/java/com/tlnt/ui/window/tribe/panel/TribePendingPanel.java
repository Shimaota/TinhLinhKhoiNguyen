package com.tlnt.ui.window.tribe.panel;

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
import com.tlnt.model.social.TribeApplicant;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.tribe.panel.info.TribeInfoPanel;

public final class TribePendingPanel extends GamePanel {
   public Pool<TribePendingPanel.PendingMemberRow> pendingRowPool = new TribePendingRowPool(this);
   public Table pendingList;

   public TribePendingPanel(RenderContext renderContext) {
      super(renderContext);
      TribeInfoPanel infoPanel;
      (infoPanel = new TribeInfoPanel(super.renderContext)).setSize(616.0F, 610.0F);
      this.addActor(infoPanel);
      TribeStoragePanel storagePanel;
      (storagePanel = new TribeStoragePanel(this, super.renderContext)).setSize(610.0F, 610.0F);
      storagePanel.setPosition(637.0F, 0.0F);
      this.addActor(storagePanel);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.pendingList = new Table().top())).setSize(560.0F, 512.0F);
      scrollPane.setPosition(25.0F, 49.0F);
      storagePanel.addActor(scrollPane);
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator it = this.pendingList.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof TribePendingPanel.PendingMemberRow) {
            this.pendingRowPool.free((TribePendingPanel.PendingMemberRow)child);
         }
      }

      this.pendingList.clear();
      if (GameState.tribeData.myTribe != null) {
         for (TribeApplicant applicant : GameState.tribeData.myTribe.applicants) {
            this.pendingList.add(this.pendingRowPool.obtain().setPendingMember(applicant)).height(104.0F).growX().space(10.0F).row();
         }
      }
   }

   public class PendingMemberRow extends BaseTable {
      public NinePatchAsset rowBackground;
      public TribeApplicant pendingMemberData;

      public PendingMemberRow(RenderContext renderContext) {
         super(renderContext);
         this.rowBackground = NinePatchAsset.obtain("ui/99", 20);
         TribePendingAcceptButton acceptButton;
         (acceptButton = new TribePendingAcceptButton(this, "Đồng ý", super.renderContext)).setSize(133.0F, 50.0F);
         acceptButton.setPosition(285.0F, 5.0F);
         this.addActor(acceptButton);
         acceptButton.addListener(new AcceptApplicantClickListener(this));
         TribePendingDeclineButton declineButton;
         (declineButton = new TribePendingDeclineButton(this, "Từ chối", super.renderContext)).setSize(133.0F, 50.0F);
         declineButton.setPosition(423.0F, 5.0F);
         this.addActor(declineButton);
         declineButton.addListener(new TribePendingDeclineClickListener(this));
      }

      public final TribePendingPanel.PendingMemberRow setPendingMember(TribeApplicant pendingMemberData) {
         this.pendingMemberData = pendingMemberData;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.rowBackground, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (this.pendingMemberData != null) {
            super.renderContext
               .drawTextureSized(
                  TextureAsset.obtain(this.pendingMemberData.spirit.avatarIconPath),
                  this.getX() + 10.0F,
                  this.getY() + 10.0F,
                  this.getHeight() - 20.0F,
                  this.getHeight() - 20.0F,
                  Anchor.BOTTOM_LEFT
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT, this.pendingMemberData.name, this.getX() + 117.0F, this.getY() + 60.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  "Level: " + this.pendingMemberData.level,
                  this.getX() + 117.0F,
                  this.getY() + 29.0F,
                  Anchor.BOTTOM_LEFT,
                  22.0F,
                  Color.YELLOW
               );
         }

         super.draw(batch, parentAlpha);
      }
   }
}
