package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.chat.ChatMessage;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.chat.widget.ChatInputBar;
import com.tlnt.ui.window.chat.widget.ChatMessageRow;

public final class WorldChatPanel extends GamePanel implements ChatInputBar.SubmitListener {
   public Pool<ChatMessageRow> messageRowPool = new WorldChatMessageRowPool(this);
   public ScrollPane scrollPane;
   public Table messageTable;
   public ChatInputBar chatInputBar;

   public WorldChatPanel(RenderContext renderContext) {
      super(renderContext);
      BaseTable background = (BaseTable)new WorldChatBackground(this, super.renderContext).pad(15.0F);
      this.add(background).grow().space(10.0F).row();
      this.add(this.chatInputBar = new ChatInputBar(super.renderContext, ChatMessage.ChatChannel.WORLD, "Nhập tin nhắn chat", this))
         .height(83.0F)
         .growX();
      this.messageTable = new Table().top().left();
      this.scrollPane = ScrollPaneFactory.createScrollPane(this.messageTable);
      background.add(this.scrollPane).grow();
   }

   @Override
   public final void refresh() {
      if (GameState.chatManager.worldDirty) {
         GameState.chatManager.worldDirty = false;
         Array.ArrayIterator it = this.messageTable.getChildren().iterator();

         while (it.hasNext()) {
            Actor child;
            if ((child = (Actor)it.next()) instanceof ChatMessageRow) {
               this.messageRowPool.free((ChatMessageRow)child);
            }
         }

         this.messageTable.clear();

         for (ChatMessage message : GameState.chatManager.worldMessages) {
            this.messageTable.add(this.messageRowPool.obtain().setMessage(message)).minHeight(35.0F).growX().space(20.0F).row();
         }

         Gdx.app.postRunnable(new WorldChatScrollTask(this));
      }
   }

   @Override
   public final boolean onSubmit(String message) {
      ServerPacketSender.getInstance().sendPrivateChat(message);
      return true;
   }

}
