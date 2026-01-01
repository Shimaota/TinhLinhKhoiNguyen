package com.tlnt.model.combopack;

import com.badlogic.gdx.utils.Pool;

public final class ComboPackPool extends Pool<ComboPack> {

   @Override
   public ComboPack newObject() {
      return new com.tlnt.model.combopack.ComboPack();
   }
}
