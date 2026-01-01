package com.tlnt.ui.hud.itemslot;

import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.render.RenderContext;

public final class EnchantSlotEffect {
   public ItemSlotButton slotButton;
   public SlotSpineEffect enchantAnimEffect;
   public SlotSpineEffect slotFxEffect;

   public EnchantSlotEffect(ItemSlotButton slotButton) {
      this.slotButton = slotButton;
      this.enchantAnimEffect = new SlotSpineEffect("ui/enchan", 3.5F);
      this.slotFxEffect = new SlotSpineEffect("ui/slot_fx", 3.5F);
   }

   public final void render(RenderContext ctx) {
      float x = this.slotButton.getX();
      float y = this.slotButton.getY();
      float width = this.slotButton.getWidth();
      float height = this.slotButton.getHeight();
      this.enchantAnimEffect.renderAtPosition(ctx, x, y, width, height);
      this.slotFxEffect.renderAtPosition(ctx, x, y, width, height);
   }

   public final void playEnchantResult(boolean success) {
      String animationName;
      if (success) {
         animationName = "success";
      } else {
         animationName = "fail";
      }

      this.enchantAnimEffect.setAnimation(animationName, false);
      String soundName;
      if (success) {
         soundName = "enchan_success";
      } else {
         soundName = "enchan_fail";
      }

      AudioManager.playSound(soundName, null);
   }

   public final void playSweep() {
      String animationName = "sweep";
      this.slotFxEffect.setAnimation(animationName, false);
   }

}
