package com.tlnt.ui.widget;

import com.tlnt.engine.pool.ObjectPool;

public final class LabelPool extends ObjectPool<PooledLabel> {

   @Override
   public PooledLabel newObject() {
      return new com.tlnt.ui.widget.PooledLabel();
   }
}
