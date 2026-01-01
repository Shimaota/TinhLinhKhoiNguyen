package com.tlnt.ui.window.giftcode;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.Popup;

public class GiftcodeEventConfirmDialog extends Popup {
   public TextraLabel messageLabel;
   public byte eventType;
   public Runnable confirmAction;

   public GiftcodeEventConfirmDialog(RenderContext ctx) {
      super(ctx, WindowType.SMALL_CONFIRM_POPUP, ((float)WorldConfig.VIEWPORT_WIDTH / 2.0F), ((float)WorldConfig.VIEWPORT_HEIGHT / 4.0F), 1, 538.0F, 340.0F);
      Table messageTable;
      (messageTable = new Table())
         .add(this.messageLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F)))
         .grow()
         .padBottom(10.0F);
      this.messageLabel.setAlignment(2);
      this.messageLabel.setWrap(true);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(messageTable)).setSize(442.0F, 122.0F);
      scrollPane.setPosition(48.0F, 170.0F);
      this.addActor(scrollPane);
      AcceptButton acceptButton;
      (acceptButton = new AcceptButton(this, "Đồng ý", super.renderContext)).setSize(169.0F, 63.0F);
      acceptButton.setPosition(95.0F, 80.0F);
      this.addActor(acceptButton);
      acceptButton.addListener(new GiftcodeConfirmAcceptListener(this));
      RefuseButton refuseButton;
      (refuseButton = new RefuseButton(this, "Từ chối", super.renderContext)).setSize(169.0F, 63.0F);
      refuseButton.setPosition(274.0F, 80.0F);
      this.addActor(refuseButton);
      refuseButton.addListener(new GiftcodeConfirmCancelListener(this));
   }

   public final void showConfirm(byte type, String message) {
      this.confirmAction = null;
      this.eventType = type;
      this.messageLabel.setText("[#8B4E23]" + message);
   }

   public final void showConfirm(String message, Runnable confirmAction) {
      this.confirmAction = confirmAction;
      this.messageLabel.setText("[#8B4E23]" + message);
   }

   @Override
   public final void onClose() {
      this.confirmAction = null;
      super.onClose();
   }

   @Override
   public final boolean isDismissOnOutsideClick() {
      return true;
   }

   @Override
   public final void drawBackground() {
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/130"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
   }

}
