package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.window.GamePanel;

public final class EntityHpBarPanel extends GamePanel {
   public TextureAsset bgTexture;
   public TextureAsset frameTexture;
   public CompositeSprite leftHpBar;
   public CompositeSprite rightHpBar;
   public TextureAsset markerTexture;
   public long entityId;
   public int animPeriod;
   public final float[] leftBarRange;
   public final float[] rightBarRange;

   public EntityHpBarPanel(RenderContext renderContext) {
      super(renderContext);
      this.bgTexture = TextureAsset.obtain("ui/176");
      this.frameTexture = TextureAsset.obtain("ui/175");
      String upPath = "ui/178";
      String downPath = "ui/179";
      String checkedPath = "ui/180";
      this.leftHpBar = CompositeSprite.obtain(upPath, downPath, checkedPath, false);
      upPath = "ui/181";
      downPath = "ui/182";
      this.rightHpBar = CompositeSprite.obtain(upPath, downPath, "ui/183", false);
      this.markerTexture = TextureAsset.obtain("ui/177");
      this.leftBarRange = new float[2];
      this.rightBarRange = new float[2];
      this.setSize(504.0F, 33.0F);
      this.setPosition((float)WorldConfig.VIEWPORT_WIDTH / 2.0F - this.getWidth() / 2.0F, 164.0F);
      this.setVisible(false);
   }

   public final void showForEntity(long entityId, int animPeriod, float leftStart, float leftEnd, float rightStart, float rightEnd) {
      this.entityId = entityId;
      this.animPeriod = animPeriod;
      this.leftBarRange[0] = leftStart;
      this.leftBarRange[1] = leftEnd;
      this.rightBarRange[0] = rightStart;
      this.rightBarRange[1] = rightEnd;
      this.setVisible(true);
   }

   @Override
   public final void onUpdate(float delta) {
      super.onUpdate(delta);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      float x = this.getX();
      float y = this.getY();
      float width = this.getWidth();
      float height = this.getHeight();
      super.renderContext.drawTextureSized(this.bgTexture, x, y, width, height, Anchor.BOTTOM_LEFT);
      float barX = x + width * this.leftBarRange[0];
      float leftEnd = this.leftBarRange[1];
      super.renderContext.drawCompositeSprite(this.leftHpBar, barX, y, width * (leftEnd - this.leftBarRange[0]), height, Anchor.BOTTOM_LEFT);
      barX = x + width * this.rightBarRange[0];
      super.renderContext
         .drawCompositeSprite(
            this.rightHpBar,
            barX,
            y,
            width * (this.rightBarRange[1] - this.rightBarRange[0]),
            height,
            Anchor.BOTTOM_LEFT
         );
      super.renderContext.drawTextureSized(this.frameTexture, x, y, width, height, Anchor.BOTTOM_LEFT);
      long elapsedMs;
      int forwardFlag;
      if ((elapsedMs = System.currentTimeMillis() - this.entityId) / (long)this.animPeriod % 2L == 0L) {
         forwardFlag = 1;
      } else {
         forwardFlag = 0;
      }

      int forward = forwardFlag;
      height = (float)(elapsedMs % (long)this.animPeriod) / (float)this.animPeriod;
      height = forward != 0 ? height : 1.0F - height;
      super.renderContext.drawTexture(this.markerTexture, x + width * height, y, Anchor.BOTTOM_CENTER);
      super.draw(batch, parentAlpha);
   }

}
