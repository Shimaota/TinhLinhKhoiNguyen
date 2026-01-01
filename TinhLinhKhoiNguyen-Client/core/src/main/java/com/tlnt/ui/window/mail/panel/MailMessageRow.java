package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.item.Item;
import com.tlnt.model.mail.MailMessage;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.SectionHeaderLabel;
import com.tlnt.ui.window.charinfo.SettingsToggleButton;
import com.tlnt.util.TimeFormatter;

public final class MailMessageRow extends GamePanel {
   public Pool<ItemSlotButton> attachmentItemPool = new ComposeAttachmentItemPool(this);
   public MailMessageRow.InboxListPanel sentMailListPanel;
   public MailMessageRow.SentMailListPanel inboxListPanel;
   public TextraLabel senderLabel;
   public TextraLabel subjectLabel;
   public TextraLabel bodyLabel;
   public Table attachmentTable;
   public SectionHeaderLabel attachmentSectionLabel;
   public ClaimMailButton claimMailButton;
   public DeleteMailButton deleteMailButton;
   public int selectedMailId;
   public int selectedMailType;
   public ObjectSet<Integer> selectedMailIds;

   public MailMessageRow(RenderContext renderContext) {
      super(renderContext);
      this.selectedMailId = -1;
      this.selectedMailType = -1;
      this.selectedMailIds = new ObjectSet<>();
      Table table;
      (table = new MailRowBackground(this, super.renderContext).pad(10.0F)).setSize(616.0F, 92.0F);
      table.setPosition(0.0F, 518.0F);
      this.addActor(table);
      ButtonGroup buttonGroup = new ButtonGroup();
      MailMessageRow.MailTabButton inboxTab = new MailMessageRow.MailTabButton("Danh sách thư", super.renderContext);
      MailMessageRow.MailTabButton sentTab = new MailMessageRow.MailTabButton("Thư đã gửi", super.renderContext);
      Button[] tabButtons = new Button[2];
      tabButtons[0] = inboxTab;
      tabButtons[1] = sentTab;
      buttonGroup.add(tabButtons);
      table.add(inboxTab).grow().space(10.0F);
      table.add(sentTab).grow().space(10.0F);
      inboxTab.addListener(new InboxTabClickListener(this));
      sentTab.addListener(new SentTabClickListener(this));
      Group listGroup;
      (listGroup = new Group()).setSize(616.0F, 506.0F);
      this.addActor(listGroup);
      listGroup.addActor(this.sentMailListPanel = new MailMessageRow.InboxListPanel(super.renderContext));
      listGroup.addActor(this.inboxListPanel = new MailMessageRow.SentMailListPanel(super.renderContext));
      (table = new MailDetailBackgroundTable(this, super.renderContext).top().pad(5.0F)).setSize(584.0F, 606.0F);
      table.setPosition(634.0F, 0.0F);
      this.addActor(table);
      table.add(this.senderLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 26))).height(47.0F).growX().row();
      this.senderLabel.setAlignment(10);
      table.add(this.subjectLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 24)))
         .height(47.0F)
         .growX()
         .row();
      this.subjectLabel.setAlignment(10);
      table.add(new MailSeparatorRow(this, super.renderContext)).height(1.0F).growX().space(5.0F).row();
      Table bodyTable;
      (bodyTable = new Table().pad(10.0F))
         .add(this.bodyLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 24, 2.0F)))
         .grow();
      ScrollPane bodyScrollPane = ScrollPaneFactory.createScrollPane(bodyTable);
      this.bodyLabel.setAlignment(10);
      this.bodyLabel.setWrap(true);
      table.add(bodyScrollPane).grow().space(5.0F).row();
      table.add(this.attachmentSectionLabel = new SectionHeaderLabel("Đính kèm", super.renderContext))
         .height(30.0F)
         .growX()
         .row();
      this.attachmentSectionLabel.setVisible(false);
      ScrollPane attachmentScrollPane = ScrollPaneFactory.createScrollPane(this.attachmentTable = new Table().left());
      table.add(attachmentScrollPane).height(110.0F).growX().row();
      Table buttonRow = new Table().right();
      table.add(buttonRow).height(50.0F).growX();
      this.claimMailButton = new ClaimMailButton(this, "Nhận thư", super.renderContext);
      buttonRow.add(this.claimMailButton).size(133.0F, 50.0F).space(5.0F);
      this.claimMailButton.setVisible(false);
      this.claimMailButton.addListener(new ReceiveMailClickListener(this));
      this.deleteMailButton = new DeleteMailButton(this, "Xóa thư", super.renderContext);
      buttonRow.add(this.deleteMailButton).size(133.0F, 50.0F).space(5.0F);
      this.deleteMailButton.setVisible(false);
      this.deleteMailButton.addListener(new DeleteMailClickListener(this));
   }

   public void showMailDetail(MailMessage mail) {
      if (mail == null) {
         this.selectedMailId = -1;
         this.selectedMailType = -1;
         this.senderLabel.setText("[#73411E]Người gửi: ");
         this.subjectLabel.setText("[#73411E]Chủ đề: ");
         this.bodyLabel.setText("[#73411E]");
         Array.ArrayIterator it = this.attachmentTable.getChildren().iterator();

         while (it.hasNext()) {
            Actor child;
            if ((child = (Actor)it.next()) instanceof ItemSlotButton) {
               this.attachmentItemPool.free((ItemSlotButton)child);
            }
         }

         this.attachmentTable.clear();
         this.attachmentSectionLabel.setVisible(false);
         this.claimMailButton.setVisible(false);
         this.deleteMailButton.setVisible(false);
      } else {
         this.selectedMailId = mail.id;
         this.selectedMailType = mail.boxType;
         this.senderLabel.setText("[#73411E]Người gửi: " + mail.senderName);
         this.subjectLabel.setText("[#73411E]Chủ đề: " + mail.subject);
         this.bodyLabel.setText("[#73411E]" + mail.content);
         Array.ArrayIterator attachIt = this.attachmentTable.getChildren().iterator();

         while (attachIt.hasNext()) {
            Actor attachChild;
            if ((attachChild = (Actor)attachIt.next()) instanceof ItemSlotButton) {
               this.attachmentItemPool.free((ItemSlotButton)attachChild);
            }
         }

         this.attachmentTable.clear();

         for (Item item : mail.attachments) {
            this.attachmentTable.add(this.attachmentItemPool.obtain().setItem(item)).size(104.0F).space(10.0F);
         }

         int hasAttachmentsFlag;
         if (!mail.attachments.isEmpty()) {
            hasAttachmentsFlag = 1;
         } else {
            hasAttachmentsFlag = 0;
         }

         int hasAttachments = hasAttachmentsFlag;
         this.attachmentSectionLabel.setVisible((hasAttachments != 0));
         this.claimMailButton.setVisible((hasAttachments != 0));
         this.deleteMailButton.setVisible(true);
      }
   }

   public class InboxListPanel extends GamePanel {
      public final MailMessageRow mailMessageRow = MailMessageRow.this;
      public Pool<MailMessageRow.MailItemRow> rowPool = new SentMailRowPool(this);
      public Table rowTable;
      public SettingsToggleButton selectAllCheckbox;

      public InboxListPanel(RenderContext renderContext) {
         super(renderContext);
         this.setFillParent(true);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.rowTable = new Table().top())).setSize(616.0F, 404.0F);
         scrollPane.setPosition(0.0F, 102.0F);
         this.addActor(scrollPane);
         this.selectAllCheckbox = new SettingsToggleButton("Chọn tất cả", super.renderContext);
         this.selectAllCheckbox.setSize(190.0F, 54.0F);
         this.addActor(this.selectAllCheckbox);
         this.selectAllCheckbox.addListener(new SentSelectAllCheckboxListener(this));
         SentMailActionButton receiveButton;
         (receiveButton = new SentMailActionButton(this, "Nhận thư", super.renderContext)).setSize(133.0F, 50.0F);
         receiveButton.setPosition(345.0F, 0.0F);
         this.addActor(receiveButton);
         receiveButton.addListener(new DeleteSentMailsClickListener(this));
         DeleteSentMailsButton deleteButton;
         (deleteButton = new DeleteSentMailsButton(this, "Xóa thư", super.renderContext)).setSize(133.0F, 50.0F);
         deleteButton.setPosition(483.0F, 0.0F);
         this.addActor(deleteButton);
         deleteButton.addListener(new DeleteSelectedMailsClickListener(this));
      }

      @Override
      public final void refresh() {
         Array.ArrayIterator it = this.rowTable.getChildren().iterator();

         while (it.hasNext()) {
            Actor child;
            if ((child = (Actor)it.next()) instanceof MailMessageRow.MailItemRow) {
               this.rowPool.free((MailMessageRow.MailItemRow)child);
            }
         }

         this.rowTable.clear();
         it = GameState.mailbox.inboxMails.iterator();

         while (it.hasNext()) {
            MailMessage mail;
            if ((mail = (MailMessage)it.next()).id == MailMessageRow.this.selectedMailId) {
               MailMessageRow.this.showMailDetail(mail);
            }

            MailMessageRow.MailItemRow row;
            (row = this.rowPool.obtain().setMailData(mail)).selectCheckbox.setChecked(MailMessageRow.this.selectedMailIds.contains(mail.id));
            this.rowTable.add(row).size(616.0F, 92.0F).space(12.0F).row();
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         RenderContext ctx = super.renderContext;
         BitmapFont font = FontManager.PLAIN_FONT;
         String noteText = "*Hòm thư có giới hạn 50 thư và thư chỉ tồn tại trong 7 ngày";
         float noteX = this.getX();
         float noteY = this.getY() + 69.0F;
         ctx.drawTextColorName(font, noteText, noteX, noteY, Anchor.BOTTOM_LEFT, 22.0F, "#8B4E23");
         super.draw(batch, parentAlpha);
      }
   }

   public class MailItemRow extends ShapeButton {
      public final MailMessageRow mailMessageRow = MailMessageRow.this;
      public CompositeSprite backgroundSprite;
      public SettingsToggleButton selectCheckbox;
      public MailMessage mailData;

      public MailItemRow(RenderContext renderContext) {
         super(renderContext);
         String startPath = "ui/104";
         String middlePath = "ui/105";
         this.backgroundSprite = CompositeSprite.obtain(
            startPath, middlePath, "ui/106", false
         );
         this.selectCheckbox = new SettingsToggleButton("", super.renderContext);
         this.selectCheckbox.setSize(54.0F, 54.0F);
         this.selectCheckbox.setPosition(15.0F, 19.0F);
         this.addActor(this.selectCheckbox);
         this.selectCheckbox.addListener(new MailCheckboxClickListener(this));
         this.addListener(new MailRowSelectClickListener(this, MailMessageRow.this));
      }

      public final MailMessageRow.MailItemRow setMailData(MailMessage mailData) {
         this.mailData = mailData;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         } else if (this.mailData.id == MailMessageRow.this.selectedMailId) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext.drawCompositeSprite(this.backgroundSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, this.mailData.subject, this.getX() + 93.0F, this.getY() + 58.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               TimeFormatter.formatTimestamp(this.mailData.sendTime, "HH:mm dd/MM/yy"),
               this.getX() + 93.0F,
               this.getY() + 23.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               Color.WHITE
            );
         String statusText;
         if (!this.mailData.read) {
            statusText = "(Chưa đọc)";
         } else if (this.mailData.attachments.isEmpty()) {
            statusText = "Đã đọc";
         } else if (this.mailData.attachmentClaimed) {
            statusText = "Đã nhận";
         } else {
            statusText = "Có đính kèm";
         }

         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, statusText, this.getX() + this.getWidth() - 10.0F, this.getY() + 23.0F, Anchor.BOTTOM_RIGHT, 22.0F, Color.WHITE);
         super.renderContext.resetBatchColor();
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.draw(batch, parentAlpha);
      }
   }

   public class MailTabButton extends TextButton {
      public CompositeSprite buttonSprite;
      public Color textColor;

      public MailTabButton(String text, RenderContext renderContext) {
         super(text, renderContext);
         String startPath = "ui/109";
         String middlePath = "ui/110";
         String endPath = "ui/111";
         this.buttonSprite = CompositeSprite.obtain(startPath, middlePath, endPath, false);
         this.textColor = new Color(0.54509807F, 0.30588236F, 0.13725491F, 1.0F);
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
               this.textColor
            );
      }
   }

   public class SentMailListPanel extends GamePanel {

      public final MailMessageRow mailMessageRow = MailMessageRow.this;
      public Pool<MailMessageRow.MailItemRow> rowPool = new InboxMailRowPool(this);
      public Table rowTable;
      public SettingsToggleButton selectAllCheckbox;

      public SentMailListPanel(RenderContext renderContext) {
         super(renderContext);
         this.setFillParent(true);
         this.setVisible(false);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.rowTable = new Table().top())).setSize(616.0F, 404.0F);
         scrollPane.setPosition(0.0F, 102.0F);
         this.addActor(scrollPane);
         this.selectAllCheckbox = new SettingsToggleButton("Chọn tất cả", super.renderContext);
         this.selectAllCheckbox.setSize(190.0F, 54.0F);
         this.addActor(this.selectAllCheckbox);
         this.selectAllCheckbox.addListener(new InboxSelectAllCheckboxListener(this));
         ReceiveSelectedMailsButton deleteButton;
         (deleteButton = new ReceiveSelectedMailsButton(this, "Xóa thư", super.renderContext)).setSize(133.0F, 50.0F);
         deleteButton.setPosition(483.0F, 0.0F);
         this.addActor(deleteButton);
         deleteButton.addListener(new ReceiveSelectedMailsClickListener(this));
      }

      @Override
      public final void refresh() {
         Array.ArrayIterator it = this.rowTable.getChildren().iterator();

         while (it.hasNext()) {
            Actor child;
            if ((child = (Actor)it.next()) instanceof MailMessageRow.MailItemRow) {
               this.rowPool.free((MailMessageRow.MailItemRow)child);
            }
         }

         this.rowTable.clear();
         it = GameState.mailbox.sentMails.iterator();

         while (it.hasNext()) {
            MailMessage mail;
            if ((mail = (MailMessage)it.next()).id == MailMessageRow.this.selectedMailId) {
               MailMessageRow.this.showMailDetail(mail);
            }

            MailMessageRow.MailItemRow row;
            (row = this.rowPool.obtain().setMailData(mail)).selectCheckbox.setChecked(MailMessageRow.this.selectedMailIds.contains(mail.id));
            this.rowTable.add(row).size(616.0F, 92.0F).space(12.0F).row();
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawTextColorName(
               FontManager.PLAIN_FONT,
               "*Hòm thư có giới hạn 50 thư và thư chỉ tồn tại trong 7 ngày",
               this.getX(),
               this.getY() + 69.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               "#8B4E23"
            );
         super.draw(batch, parentAlpha);
      }
   }
}
