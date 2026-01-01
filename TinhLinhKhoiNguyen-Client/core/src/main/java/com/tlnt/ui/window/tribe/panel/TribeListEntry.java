package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.social.Tribe;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.GameTextField;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.util.GameUtils;

public final class TribeListEntry extends GamePanel {
   public Pool<TribeListEntry.TribeInfoRow> tribeInfoRowPool = new TribeListEntryRowPool(this);
   public Table tribeListTable;
   public TribeListEntry.SearchTextField searchTextField;

   public TribeListEntry(RenderContext renderContext) {
      super(renderContext);
      TribeListPanel listPanel;
      (listPanel = new TribeListPanel(this, super.renderContext)).setSize(1247.0F, 550.0F);
      listPanel.setPosition(0.0F, 58.0F);
      this.addActor(listPanel);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.tribeListTable = new Table().top())).setSize(1197.0F, 491.0F);
      scrollPane.setPosition(25.0F, 10.0F);
      listPanel.addActor(scrollPane);
      Table searchBar;
      (searchBar = new TribeListEntryBackground(this, super.renderContext).pad(5.0F))
         .add(this.searchTextField = new TribeListEntry.SearchTextField("", super.renderContext))
         .grow()
         .space(5.0F);
      this.searchTextField.addListener(new TribeSearchKeyListener(this));
      searchBar.add(new TribeListDividerImage(this, super.renderContext)).size(35.0F, 35.0F);
      searchBar.setSize(1109.0F, 50.0F);
      this.addActor(searchBar);
      TribeListButton createButton;
      (createButton = new TribeListButton(this, "Tạo bộ lạc", super.renderContext)).setSize(133.0F, 50.0F);
      createButton.setPosition(1114.0F, 0.0F);
      this.addActor(createButton);
      createButton.addListener(new CreateTribeClickListener(this));
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator it = this.tribeListTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof TribeListEntry.TribeInfoRow) {
            this.tribeInfoRowPool.free((TribeListEntry.TribeInfoRow)child);
         }
      }

      this.tribeListTable.clear();

      for (Tribe tribe : GameState.tribeData.tribeList) {
         this.tribeListTable.add(this.tribeInfoRowPool.obtain().setTribeData(tribe)).height(104.0F).growX().space(10.0F).row();
      }
   }

   @Override
   public final void onShow() {
      ServerPacketSender.getInstance().sendMarketSearch(this.searchTextField.getText());
   }

   public class SearchTextField extends GameTextField {
      public SearchTextField(String text, RenderContext renderContext) {
         super(text, renderContext, FontFactory.scaleFont(FontManager.TEXT_FIELD_FONT, 22.0F));
      }

      @Override
      public final void drawBackground(float x, float y, float width, float height, boolean focused, boolean disabled, float parentAlpha) {
      }

      @Override
      public final void drawText(Batch batch, BitmapFont font, float x, float y) {
         super.drawText(batch, font, x, y - 5.0F);
      }
   }

   public class TribeInfoRow extends BaseTable {
      public NinePatchAsset rowBackground;
      public Tribe tribeData;

      public TribeInfoRow(RenderContext renderContext) {
         super(renderContext);
         this.rowBackground = NinePatchAsset.obtain("ui/99", 20);
         TribeListJoinButton joinButton;
         (joinButton = new TribeListJoinButton(this, "Xin vào", super.renderContext)).setSize(169.0F, 63.0F);
         joinButton.setPosition(1018.0F, 21.0F);
         this.addActor(joinButton);
         joinButton.addListener(new JoinTribeClickListener(this));
      }

      public final TribeListEntry.TribeInfoRow setTribeData(Tribe tribeData) {
         this.tribeData = tribeData;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.rowBackground, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (this.tribeData != null) {
            if (this.tribeData.emblem != null && this.tribeData.emblem.framePaths.length != 0) {
               super.renderContext
                  .drawTexture(
                     TextureAsset.obtain(
                        this.tribeData.emblem.framePaths[GameUtils.timeFrameIndex(this.tribeData.emblem.framePaths.length, this.tribeData.emblem.frameRate)]
                     ),
                     this.getX() + 75.0F,
                     this.getY() + this.getHeight() / 2.0F,
                     Anchor.CENTER
                  );
            }

            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT, "Bộ lạc: " + this.tribeData.name, this.getX() + 140.0F, this.getY() + 65.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  "Tộc trưởng: " + this.tribeData.leaderName,
                  this.getX() + 140.0F,
                  this.getY() + 25.0F,
                  Anchor.BOTTOM_LEFT,
                  22.0F,
                  Color.YELLOW
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  "Thành viên: " + this.tribeData.members.size + "/" + this.tribeData.maxMembers,
                  this.getX() + 515.0F,
                  this.getY() + 46.0F,
                  Anchor.BOTTOM_LEFT,
                  22.0F,
                  Color.WHITE
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT, "Cấp: " + this.tribeData.level, this.getX() + 814.0F, this.getY() + 46.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE
               );
         }

         super.draw(batch, parentAlpha);
      }
   }
}
