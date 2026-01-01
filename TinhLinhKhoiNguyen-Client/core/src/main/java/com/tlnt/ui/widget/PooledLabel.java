package com.tlnt.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.pool.ObjectPool;

public final class PooledLabel extends TextraLabel implements ObjectPool.Poolable {
   public static final ObjectPool<PooledLabel> POOL = new LabelPool();

   public PooledLabel() {
      super("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 24));
   }

   @Override
   public final void setText(String text) {
      super.setText(text);
      this.pack();
   }

   @Override
   public final boolean remove() {
      POOL.free(this);
      return super.remove();
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
      batch.setColor(Color.WHITE);
   }

   @Override
   public final void reset() {
      this.setColor(Color.WHITE);
   }
}
