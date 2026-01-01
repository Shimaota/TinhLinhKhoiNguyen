package com.tlnt.world.entity.combattext;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;

public final class FloatingCombatTextPool extends Pool<FloatingCombatText> {

   @Override
   public FloatingCombatText newObject() {
      return new com.tlnt.world.entity.combattext.FloatingCombatText(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final FloatingCombatText obtain() {
      FloatingCombatText text;
      if (!(text = (FloatingCombatText)super.obtain()).getEntity().isActive()) {
         text.setActive(true);
      }

      return text;
   }
}
