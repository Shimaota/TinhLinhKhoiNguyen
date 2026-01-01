package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.dialog.widget.StyledTextArea;
import com.tlnt.ui.dialog.widget.StyledTextField;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class MailComposePanel extends GamePanel {
   public Pool<ItemSlotButton> attachmentItemPool = new ComposeAttachmentPool(this);
   public TextraLabel recipientLabel;
   public StyledTextField recipientField;
   public TextraLabel subjectLabel;
   public StyledTextField subjectField;
   public TextraLabel bodyLabel;
   public StyledTextArea bodyField;
   public Table attachmentRow;
   public TextraLabel mailFeeLabel;

   public MailComposePanel(RenderContext renderContext) {
      super(renderContext);
      InventoryGridPanel inventoryPanel;
      (inventoryPanel = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_3)).setPosition(637.0F, 0.0F);
      this.addActor(inventoryPanel);
      String labelText = "[#8B4E23]Tên người nhận";
      this.recipientLabel = new TextraLabel(labelText, FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 22));
      this.recipientLabel.setAlignment(10);
      this.recipientLabel.setSize(157.0F, 25.0F);
      this.recipientLabel.setPosition(0.0F, 569.0F);
      this.addActor(this.recipientLabel);
      ComposeSubjectFieldBackground recipientBackground;
      (recipientBackground = new ComposeSubjectFieldBackground(this, super.renderContext)).setSize(616.0F, 58.0F);
      recipientBackground.setPosition(0.0F, 501.0F);
      this.addActor(recipientBackground);
      recipientBackground.add(this.recipientField = new StyledTextField("", super.renderContext)).grow().pad(10.0F);
      this.subjectLabel = new TextraLabel(
         "[#8B4E23]Chủ đề", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 22)
      );
      this.subjectLabel.setAlignment(10);
      this.subjectLabel.setSize(70.0F, 25.0F);
      this.subjectLabel.setPosition(0.0F, 461.0F);
      this.addActor(this.subjectLabel);
      ComposeBodyFieldBackground subjectBackground;
      (subjectBackground = new ComposeBodyFieldBackground(this, super.renderContext)).setSize(616.0F, 58.0F);
      subjectBackground.setPosition(0.0F, 393.0F);
      this.addActor(subjectBackground);
      subjectBackground.add(this.subjectField = new StyledTextField("", super.renderContext)).grow().pad(10.0F);
      this.bodyLabel = new TextraLabel("[#8B4E23]Nội dung", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 22));
      this.bodyLabel.setAlignment(10);
      this.bodyLabel.setSize(92.0F, 25.0F);
      this.bodyLabel.setPosition(0.0F, 355.0F);
      this.addActor(this.bodyLabel);
      ComposeInputBackground bodyBackground;
      (bodyBackground = new ComposeInputBackground(this, super.renderContext)).setSize(616.0F, 164.0F);
      bodyBackground.setPosition(0.0F, 181.0F);
      this.addActor(bodyBackground);
      bodyBackground.add(this.bodyField = new StyledTextArea("", super.renderContext)).grow().pad(10.0F);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.attachmentRow = new Table().left().pad(5.0F))).setSize(616.0F, 114.0F);
      scrollPane.setPosition(0.0F, 65.0F);
      this.addActor(scrollPane);
      MailSendButton sendButton;
      (sendButton = new MailSendButton(this, "Gửi thư", super.renderContext)).setSize(133.0F, 50.0F);
      sendButton.setPosition(483.0F, 0.0F);
      this.addActor(sendButton);
      sendButton.addListener(new SendMailClickListener(this));
      labelText = "[#8B4E23]Phí gửi thư: 100.000 vàng";
      this.mailFeeLabel = new TextraLabel(labelText, FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 22));
      this.mailFeeLabel.setAlignment(10);
      this.mailFeeLabel.setHeight(25.0F);
      this.mailFeeLabel.setPosition(0.0F, 21.0F);
      this.addActor(this.mailFeeLabel);
   }

   public void sendMail() {
      String recipient = this.recipientField.getText();
      String subject = this.subjectField.getText();
      String body = this.bodyField.getText();
      if (recipient != null && subject != null && body != null && !recipient.trim().isEmpty() && !subject.trim().isEmpty() && !body.trim().isEmpty()) {
         ServerPacketSender.getInstance().sendEquipmentSet(recipient, subject, body, GameState.mailbox.composeAttachments);
         this.recipientField.setText("");
         this.subjectField.setText("");
         this.bodyField.setText("");
         GameState.mailbox.composeAttachments.clear();
         this.refresh();
      }
   }

   @Override
   public final void refresh() {
      if (this.attachmentRow.getChildren().size != 6) {
         Array.ArrayIterator it = this.attachmentRow.getChildren().iterator();

         while (it.hasNext()) {
            Actor child;
            if ((child = (Actor)it.next()) instanceof ItemSlotButton) {
               this.attachmentItemPool.free((ItemSlotButton)child);
            }
         }

         this.attachmentRow.clear();

         for (int i = 0; i < 6; i++) {
            this.attachmentRow.add(this.attachmentItemPool.obtain()).size(104.0F).space(10.0F);
         }
      }

      for (int slotIndex = 0; slotIndex < this.attachmentRow.getChildren().size; slotIndex++) {
         if (slotIndex >= GameState.mailbox.composeAttachments.size) {
            ((ItemSlotButton)this.attachmentRow.getChild(slotIndex)).setItem(null);
         } else {
            Item item = GameState.mailbox.composeAttachments.get(slotIndex);
            ((ItemSlotButton)this.attachmentRow.getChild(slotIndex)).setItem(item);
         }
      }
   }

}
