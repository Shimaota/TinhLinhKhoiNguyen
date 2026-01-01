package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.chat.ChatMessage;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.chat.widget.ChatMessageRow;

public final class SystemChatPanel extends GamePanel {
   public Pool<ChatMessageRow> messageRowPool = new SystemChatMessagePool(this);
   public NinePatchAsset backgroundPatch;
   public ScrollPane scrollPane;
   public Table messageTable;

   public SystemChatPanel(RenderContext renderContext) {
      super(renderContext);
      this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
      BaseTable scrollBackground = (BaseTable)new SystemChatScrollBackground(this, super.renderContext).pad(15.0F);
      this.add(scrollBackground).grow();
      this.messageTable = new Table().top().left();
      this.scrollPane = ScrollPaneFactory.createScrollPane(this.messageTable);
      scrollBackground.add(this.scrollPane).grow();
   }

   @Override
   public final void refresh() {
      if (GameState.chatManager.systemDirty) {
         GameState.chatManager.systemDirty = false;
         Array.ArrayIterator it = this.messageTable.getChildren().iterator();

         while (it.hasNext()) {
            Actor child;
            if ((child = (Actor)it.next()) instanceof ChatMessageRow) {
               this.messageRowPool.free((ChatMessageRow)child);
            }
         }

         this.messageTable.clear();

         for (ChatMessage message : GameState.chatManager.systemMessages) {
            this.messageTable.add(this.messageRowPool.obtain().setMessage(message)).minHeight(35.0F).growX().space(20.0F).row();
         }

         Gdx.app.postRunnable(new SystemChatScrollTask(this));
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
