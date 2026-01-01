package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.graphics.Color;
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
import com.tlnt.model.social.TribeMember;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.tribe.panel.info.TribeInfoPanel;

public final class TribeMemberRow extends GamePanel {
   public Pool<TribeMemberRow.MemberButton> memberButtonPool = new TribeMemberButtonPool(this);
   public Table memberTable;

   public TribeMemberRow(RenderContext renderContext) {
      super(renderContext);
      TribeInfoPanel infoPanel;
      (infoPanel = new TribeInfoPanel(super.renderContext)).setSize(616.0F, 610.0F);
      this.addActor(infoPanel);
      TribeMemberListPanel listPanel;
      (listPanel = new TribeMemberListPanel(this, super.renderContext)).setSize(610.0F, 610.0F);
      listPanel.setPosition(637.0F, 0.0F);
      this.addActor(listPanel);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.memberTable = new Table().top())).setSize(560.0F, 512.0F);
      scrollPane.setPosition(25.0F, 49.0F);
      listPanel.addActor(scrollPane);
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator it = this.memberTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof TribeMemberRow.MemberButton) {
            this.memberButtonPool.free((TribeMemberRow.MemberButton)child);
         }
      }

      this.memberTable.clear();
      if (GameState.tribeData.myTribe != null) {
         for (TribeMember member : GameState.tribeData.myTribe.members) {
            this.memberTable.add(this.memberButtonPool.obtain().setMemberData(member)).height(104.0F).growX().space(10.0F).row();
         }
      }
   }

   public class MemberButton extends ShapeButton {
      public NinePatchAsset backgroundPatch;
      public TribeMember memberData;

      public MemberButton(RenderContext renderContext) {
         super(renderContext);
         this.backgroundPatch = NinePatchAsset.obtain("ui/99", 20);
         this.addListener(new TribeMemberRowClickListener(this, TribeMemberRow.this));
      }

      public final TribeMemberRow.MemberButton setMemberData(TribeMember memberData) {
         this.memberData = memberData;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain(this.memberData.spirit.avatarIconPath),
               this.getX() + 10.0F,
               this.getY() + 10.0F,
               this.getHeight() - 20.0F,
               this.getHeight() - 20.0F,
               Anchor.BOTTOM_LEFT
            );
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, this.memberData.name, this.getX() + 117.0F, this.getY() + 60.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT, "Level: " + this.memberData.level, this.getX() + 117.0F, this.getY() + 29.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.YELLOW
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.memberData.role.label,
               this.getX() + this.getWidth() - 14.0F,
               this.getY() + 60.0F,
               Anchor.BOTTOM_RIGHT,
               22.0F,
               Color.WHITE
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Cống hiến: " + this.memberData.contribution,
               this.getX() + this.getWidth() - 14.0F,
               this.getY() + 29.0F,
               Anchor.BOTTOM_RIGHT,
               22.0F,
               Color.WHITE
            );
         super.renderContext.resetBatchColor();
      }
   }
}
