package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
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
import com.tlnt.model.chat.ChatMessage;
import com.tlnt.model.chat.PrivateConversation;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.chat.widget.ChatInputBar;
import com.tlnt.world.entity.player.PlayerEntity;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PrivateChatPanel extends GamePanel implements ChatInputBar.SubmitListener {
   public static final Pattern PRIVATE_CHAT_PATTERN = Pattern.compile("^/([^:]+):\\s*(.+)$");
   public Pool<PrivateChatPanel.ContactRow> contactRowPool = new PrivateChatContactRowPool(this);
   public Pool<PrivateChatPanel.MessageBubble> messageRowPool = new PrivateChatMessageRowPool(this);
   public Table messageTable;
   public ChatInputBar chatInputBar;
   public Table contactListTable;
   public PrivateConversation selectedContact;

   public PrivateChatPanel(RenderContext renderContext) {
      super(renderContext);
      BaseTable conversationBackground = (BaseTable)new PrivateChatConversationBackground(this, super.renderContext).pad(15.0F).padTop(50.0F);
      this.add(conversationBackground).width(840.0F).growY().space(10.0F);
      BaseTable contactBackground = (BaseTable)new PrivateChatBackground(this, super.renderContext).pad(15.0F);
      this.add(contactBackground).grow().space(10.0F).row();
      Cell inputCell = this.add(
            this.chatInputBar = new ChatInputBar(super.renderContext, ChatMessage.ChatChannel.PRIVATE, "/tên người nhận: nội dung", this)
         )
         .height(83.0F)
         .growX();
      inputCell.colspan(2);
      this.messageTable = new Table().top();
      ScrollPane messageScrollPane = ScrollPaneFactory.createScrollPane(this.messageTable);
      conversationBackground.add(messageScrollPane).grow();
      this.contactListTable = new Table().top().left();
      ScrollPane contactScrollPane = ScrollPaneFactory.createScrollPane(this.contactListTable);
      contactBackground.add(contactScrollPane).grow();
   }

   public final void selectContact(PrivateConversation contact) {
      this.selectedContact = contact;
      this.refresh();
   }

   @Override
   public final void refresh() {
      if (GameState.chatManager.privateDirty) {
         GameState.chatManager.privateDirty = false;
         Array.ArrayIterator contactIt = this.contactListTable.getChildren().iterator();

         while (contactIt.hasNext()) {
            Actor child;
            if ((child = (Actor)contactIt.next()) instanceof PrivateChatPanel.ContactRow) {
               this.contactRowPool.free((PrivateChatPanel.ContactRow)child);
            }
         }

         this.contactListTable.clear();

         for (PrivateConversation conversation : GameState.chatManager.privateConversations) {
            this.contactListTable.add(this.contactRowPool.obtain().bindContact(conversation)).height(104.0F).growX().space(10.0F).row();
         }
      }

      if (this.selectedContact != null && this.selectedContact.hasUnread) {
         this.selectedContact.hasUnread = false;
         Array.ArrayIterator messageIt = this.messageTable.getChildren().iterator();

         while (messageIt.hasNext()) {
            Actor messageChild;
            if ((messageChild = (Actor)messageIt.next()) instanceof PrivateChatPanel.MessageBubble) {
               this.messageRowPool.free((PrivateChatPanel.MessageBubble)messageChild);
            }
         }

         this.messageTable.clear();

         for (ChatMessage message : this.selectedContact.messages) {
            this.messageTable.add(this.messageRowPool.obtain().bindMessage(message)).minHeight(52.0F).growX().space(10.0F).row();
         }
      }
   }

   @Override
   public final boolean onSubmit(String text) {
      PrivateChatPanel.ContactEntry parsedEntry;
      if (text == null) {
         parsedEntry = null;
      } else {
         Matcher matcher;
         if (!(matcher = PRIVATE_CHAT_PATTERN.matcher(text)).matches()) {
            parsedEntry = null;
         } else {
            String recipient = matcher.group(1).trim();
            String body = matcher.group(2).trim();
            parsedEntry = new PrivateChatPanel.ContactEntry(recipient, body);
         }
      }

      PrivateChatPanel.ContactEntry entry = parsedEntry;
      if (parsedEntry != null) {
         ServerPacketSender.getInstance().sendWhisper(entry.senderName, entry.recipientName);
         return true;
      } else if (this.selectedContact != null) {
         ServerPacketSender.getInstance().sendWhisper(this.selectedContact.partnerName, text);
         return true;
      } else {
         return false;
      }
   }

   public static class ContactEntry {
      public String senderName;
      public String recipientName;

      public ContactEntry(String senderName, String recipientName) {
         this.senderName = senderName;
         this.recipientName = recipientName;
      }
   }

   public class ContactRow extends ShapeButton {

      public final PrivateChatPanel privateChatPanel = PrivateChatPanel.this;
      public NinePatchAsset backgroundPatch;
      public TextureAsset avatarTexture;
      public TextureAsset unreadIndicatorTexture;
      public PrivateConversation contact;

      public ContactRow(RenderContext renderContext) {
         super(renderContext);
         this.backgroundPatch = NinePatchAsset.obtain("ui/99", 50);
         this.unreadIndicatorTexture = TextureAsset.obtain("ui/193");
         this.addListener(new PrivateChatContactClickListener(this, PrivateChatPanel.this));
         this.top().right().pad(5.0F);
         PrivateChatContactCloseButton closeButton = new PrivateChatContactCloseButton(this, super.renderContext, PrivateChatPanel.this);
         this.add(closeButton).size(30.0F, 30.0F);
         closeButton.addListener(new PrivateChatRemoveContactListener(this, PrivateChatPanel.this));
      }

      public final PrivateChatPanel.ContactRow bindContact(PrivateConversation contact) {
         this.contact = contact;
         if (this.contact != null) {
            this.avatarTexture = TextureAsset.obtain(this.contact.partnerClass.avatarIconPath);
         }

         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         } else if (this.contact == PrivateChatPanel.this.selectedContact) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (this.contact != null) {
            super.renderContext.drawTextureSized(this.avatarTexture, this.getX() + 11.0F, this.getY() + 11.0F, 81.0F, 81.0F, Anchor.BOTTOM_LEFT);
            super.renderContext
               .drawText(FontManager.DEFAULT_FONT, this.contact.partnerName, this.getX() + 117.0F, this.getY() + 65.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
            super.renderContext
               .drawTextColorName(
                  FontManager.DEFAULT_FONT,
                  "Level: " + this.contact.partnerLevel,
                  this.getX() + 117.0F,
                  this.getY() + 30.0F,
                  Anchor.BOTTOM_LEFT,
                  22.0F,
                  "#FFF600"
               );
            if (this.contact.hasUnread) {
               super.renderContext
                  .drawTextureSized(this.unreadIndicatorTexture, this.getX() + this.getWidth() - 5.0F, this.getY() + 5.0F, 28.0F, 23.0F, Anchor.BOTTOM_RIGHT);
            }
         }

         super.renderContext.resetBatchColor();
      }
   }

   public class MessageBubble extends BaseTable {
      public TextraLabel messageLabel;
      public BaseTable bubbleContainer = (BaseTable)new PrivateChatMessageBubbleBackground(this, this.renderContext).pad(10.0F).padTop(5.0F);
      public Cell bubbleCell = this.add(this.bubbleContainer).maxWidth(480.0F).width(new PrivateBubbleWidthValue(this)).minHeight(52.0F).expand();
      public ChatMessage chatMessage;

      public MessageBubble(RenderContext renderContext) {
         super(renderContext);
         this.messageLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 2.5F));
         this.messageLabel.setWrap(true);
         this.messageLabel.setAlignment(10);
         this.bubbleContainer.add(this.messageLabel).grow();
      }

      public final PrivateChatPanel.MessageBubble bindMessage(ChatMessage chatMessage) {
         this.chatMessage = chatMessage;
         if (this.chatMessage != null) {
            this.messageLabel.setText(this.chatMessage.text);
            int ownFlag;
            if (chatMessage.senderId == PlayerEntity.getInstance().getId()) {
               ownFlag = 1;
            } else {
               ownFlag = 0;
            }

            if (ownFlag != 0) {
               this.bubbleCell.right();
               this.invalidate();
            } else {
               this.bubbleCell.left();
               this.invalidate();
            }
         }

         return this;
      }
   }
}
