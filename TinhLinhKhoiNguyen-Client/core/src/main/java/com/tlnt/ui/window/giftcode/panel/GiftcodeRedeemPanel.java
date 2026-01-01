package com.tlnt.ui.window.giftcode.panel;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.dialog.widget.TextInputRow;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;

public final class GiftcodeRedeemPanel extends GamePanel {
   public TextInputRow giftcodeInputField;
   public GiftcodeRedeemConfirmButton confirmButton;
   public TextraLabel resultLabel;

   public GiftcodeRedeemPanel(RenderContext ctx) {
      super(ctx);
      this.center();
      Table column = new Table();
      this.add(column).grow().uniform();
      String inputTitle = "[#8B4E23]Nhập mã quà tặng";
      TextraLabel titleLabel;
      column.add(titleLabel = new TextraLabel(inputTitle, FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 26)))
         .growX()
         .space(35.0F)
         .row();
      titleLabel.setAlignment(2);
      this.giftcodeInputField = new TextInputRow("Nhập mã quà tặng", super.renderContext);
      column.add(this.giftcodeInputField).size(410.0F, 70.0F).space(10.0F).row();
      this.confirmButton = new GiftcodeRedeemConfirmButton(this, "Xác nhận", super.renderContext);
      column.add(this.confirmButton).size(170.0F, 63.0F);
      this.confirmButton.addListener(new GiftcodeRedeemConfirmListener(this));
      column = new GiftcodeListPanel(this, super.renderContext).top();
      this.add(column).grow().uniform();
      String listTitle = "[yellow]Danh sách Giftcode";
      TextraLabel listTitleLabel = titleLabel = new TextraLabel(listTitle, FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 26));
      listTitleLabel.setAlignment(2);
      column.add(titleLabel).growX().space(25.0F).row();
      Table resultTable;
      (resultTable = new Table())
         .add(
            this.resultLabel = new TextraLabel(
               "", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 2.5F)
            )
         )
         .grow();
      this.resultLabel.setAlignment(2);
      ScrollPane resultScroll = ScrollPaneFactory.createScrollPane(resultTable);
      column.add(resultScroll).grow();
   }

   @Override
   public final void refresh() {
      this.resultLabel.setText(GameState.giftcodeData.giftcodeListText);
   }

   public void redeemGiftcode() {
      String code;
      if (!(code = this.giftcodeInputField.getText().trim()).isEmpty() && !isBlankJ8(code)) {
         ServerPacketSender.getInstance().sendDialogResponse(code);
         this.giftcodeInputField.getTextField().setText("");
      } else {
         this.giftcodeInputField.getTextField().setText("");
      }
   }

   // Java 8 equivalent of String.isBlank() (Java 11): true if empty or only Character.isWhitespace code points.
   private static boolean isBlankJ8(String s) {
      int i = 0;

      while (i < s.length()) {
         int codePoint = s.codePointAt(i);
         if (!Character.isWhitespace(codePoint)) {
            return false;
         }

         i += Character.charCount(codePoint);
      }

      return true;
   }

}
