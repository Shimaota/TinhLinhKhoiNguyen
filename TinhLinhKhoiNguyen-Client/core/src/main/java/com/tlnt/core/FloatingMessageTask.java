package com.tlnt.core;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.tlnt.engine.WorldConfig;
import com.tlnt.screen.Scene2dScreen;
import com.tlnt.util.GameUtils;
import java.util.regex.Matcher;

public final class FloatingMessageTask implements Runnable {
   public java.lang.String message;

   public FloatingMessageTask(String message) {
      super();
      this.message = message;
   }

   @Override
   public final void run() {
      Screen screen;
      if ((screen = GameMain.instance.getScreen()) instanceof Scene2dScreen) {
         int x = WorldConfig.VIEWPORT_WIDTH / 2;
         int y = 200;
         int riseDistance = GameUtils.randomInt(50, 100);
         float duration = 2.0F;
         String text = this.message;
         Matcher matcher;
         if ((matcher = GameMain.FLOATING_TEXT_PATTERN.matcher(this.message)).matches()) {
            String xGroup = matcher.group(1);
            text = matcher.group(2);
            String riseGroup = matcher.group(3);
            String durationGroup = matcher.group(4);
            if (!"?".equals(xGroup)) {
               x = Integer.parseInt(xGroup);
            }

            if (text != null && !"?".equals(text)) {
               y = Integer.parseInt(text);
            }

            if (riseGroup != null) {
               if (!"?".equals(riseGroup)) {
                  riseDistance = Integer.parseInt(riseGroup);
               }
            }

            if (durationGroup != null && !"?".equals(durationGroup)) {
               duration = Float.parseFloat(durationGroup);
            }

            text = matcher.group(5).trim();
         }

         SequenceAction action = Actions.sequence(
            Actions.moveToAligned((float)x, (float)y, 1),
            Actions.parallel(
               Actions.moveBy(0.0F, (float)riseDistance, duration, Interpolation.fastSlow),
               Actions.sequence(Actions.delay(Math.max(duration - 0.7F, 0.5F)), Actions.fadeOut(0.7F))
            )
         );
         ((Scene2dScreen)screen).showFloatingText(text, action);
      }
   }

}
