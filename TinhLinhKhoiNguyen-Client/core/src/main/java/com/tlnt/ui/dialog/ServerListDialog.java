package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.model.server.ServerInfo;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.utils.SpineSkeletonActor;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.WindowType;

public final class ServerListDialog extends SlideInDialog {
   public Pool<ServerListDialog.ServerItem> serverButtonPool = new ServerItemPool(this);
   public Table serverListTable;

   public ServerListDialog(RenderContext renderContext) {
      super(renderContext, WindowType.SERVER_LIST_PANEL);
      ServerSelectTitlePanel titlePanel;
      (titlePanel = new ServerSelectTitlePanel(this, super.renderContext)).setSize(1009.0F, 379.0F);
      titlePanel.setPosition((float)WorldConfig.VIEWPORT_WIDTH / 2.1F, (float)WorldConfig.VIEWPORT_HEIGHT / 2.0F, 1);
      this.addActor(titlePanel);
      SpineSkeletonActor logoActor;
      (logoActor = new SpineSkeletonActor()).setRenderer(super.renderContext.getSkeletonRenderer());
      NamedPair skeletonPair = SkeletonLoader.obtainSkeleton("ui/logo", 100.0F, new SkeletonLoader.AnimationMixConfig());
      logoActor.setAnimationState((AnimationState)skeletonPair.first);
      logoActor.setSkeleton((Skeleton)skeletonPair.second);
      AnimationState animState = logoActor.getAnimationState();
      animState.setAnimation(0, "animation", true);
      logoActor.setPosition(titlePanel.getWidth() / 1.85F, titlePanel.getHeight() - 50.0F, 2);
      titlePanel.addActor(logoActor);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.serverListTable = new Table().left())).setSize(737.0F, 77.0F);
      scrollPane.setPosition(175.0F, 121.0F);
      titlePanel.addActor(scrollPane);
   }

   @Override
   public final void onClose2() {
      Array.ArrayIterator it = this.serverListTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof ServerListDialog.ServerItem) {
            this.serverButtonPool.free((ServerListDialog.ServerItem)child);
         }
      }

      this.serverListTable.clear();

      for (ServerInfo server : GameState.serverList) {
         this.serverListTable.add(this.serverButtonPool.obtain().setServerData(server)).size(239.0F, 77.0F).space(10.0F);
      }
   }

   public class ServerItem extends TextButton {
      public CompositeSprite buttonSprite;
      public ServerInfo serverData;

      public ServerItem(String text, RenderContext renderContext) {
         super(text, renderContext);
         String upPath = "ui/10";
         String downPath = "ui/11";
         this.buttonSprite = CompositeSprite.obtain(upPath, downPath, "ui/12", false);
         this.addListener(new SelectServerListener(this, ServerListDialog.this));
      }

      public final ServerListDialog.ServerItem setServerData(ServerInfo serverData) {
         this.serverData = serverData;
         this.setText(this.serverData.name);
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }

      @Override
      public final void drawLabel() {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               super.text,
               this.getX() + this.getWidth() / 2.05F,
               this.getY() + this.getHeight() / 1.8F,
               Anchor.CENTER,
               24.0F,
               Color.WHITE
            );
      }
   }
}
