package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Queue;
import com.github.tommyettinger.textra.TypingLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.window.UpdateListener;

public final class MessageTicker extends BaseTable implements UpdateListener {
   public NinePatchAsset backgroundPatch;
   public Queue<String> messageQueue;
   public TypingLabel tickerLabel;
   public String currentMessage;
   public float scrollSpeed;
   public float pauseTimer;
   public float pauseDuration;
   public byte animPhase;
   public float labelPaddingX;

   public MessageTicker(RenderContext renderContext) {
      super(renderContext);
      this.backgroundPatch = NinePatchAsset.obtain("ui/13", 20);
      this.messageQueue = new Queue<>();
      this.scrollSpeed = 250.0F;
      this.pauseDuration = 1.0F;
      this.labelPaddingX = 10.0F;
      this.setSize(660.0F, 60.0F);
      float posX = (float)WorldConfig.VIEWPORT_WIDTH / 2.0F;
      float posY = (float)(WorldConfig.VIEWPORT_HEIGHT - 185);
      this.setPosition(posX, posY, 2);
      this.setTouchable(Touchable.enabled);
      this.tickerLabel = new TypingLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
      this.tickerLabel.setAlignment(10);
      this.tickerLabel.setWrap(false);
      this.addActor(this.tickerLabel);
      this.addListener(new OpenChatWindowListener(this));
      this.setVisible(false);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.currentMessage != null) {
         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (super.renderContext.pushScissors(this.getX() + this.labelPaddingX, this.getY(), this.getWidth() - this.labelPaddingX * 2.0F, this.getHeight())) {
            super.draw(batch, parentAlpha);
            super.renderContext.popScissors();
         }
      }
   }

   public final void enqueueMessage(String message) {
      this.messageQueue.addLast(message);
      this.setVisible(true);
   }

   @Override
   public final void onUpdate(float delta) {
      if (this.currentMessage == null) {
         if (!this.messageQueue.isEmpty()) {
            this.displayMessage(this.messageQueue.removeFirst());
         }
      } else {
         switch (this.animPhase) {
            case 0:
               this.tickerLabel.moveBy(-this.scrollSpeed * delta, 0.0F);
               if (this.tickerLabel.getX() <= this.labelPaddingX) {
                  this.tickerLabel.setX(this.labelPaddingX);
                  this.pauseTimer = 0.0F;
                  this.animPhase = (byte)1;
                  return;
               }
               break;
            case 1:
               this.pauseTimer += delta;
               if (this.pauseTimer >= this.pauseDuration) {
                  this.animPhase = (byte)2;
                  return;
               }
               break;
            case 2:
               this.tickerLabel.moveBy(-this.scrollSpeed * delta, 0.0F);
               if (this.tickerLabel.getX() <= -this.tickerLabel.getPrefWidth()) {
                  this.currentMessage = null;
                  if (this.messageQueue.isEmpty()) {
                     this.setVisible(false);
                     return;
                  }

                  this.displayMessage(this.messageQueue.removeFirst());
               }
         }
      }
   }

   public void displayMessage(String currentMessage) {
      this.currentMessage = currentMessage;
      this.tickerLabel.setText(currentMessage);
      this.tickerLabel.pack();
      float posX = this.getWidth();
      float posY = this.getHeight() / 4.0F * 3.0F;
      this.tickerLabel.setPosition(posX, posY, 8);
      this.animPhase = (byte)0;
   }

}
