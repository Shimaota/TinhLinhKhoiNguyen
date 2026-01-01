package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.utils.Pool;

public final class BuffIconPool extends Pool<BuffBar.BuffIcon> {
   public com.tlnt.ui.hud.widget.BuffBar buffBar;
   public BuffIconPool(BuffBar buffBar) {
      super();
      this.buffBar = buffBar;
   }

   @Override
   public BuffBar.BuffIcon newObject() {
      return (this.buffBar).new BuffIcon(this.buffBar.renderContext);
   }
}
