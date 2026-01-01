package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.world.entity.player.PlayerEntity;

public final class PickupButtonClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.PickupButton pickupButton;
   public PickupButtonClickListener(PickupButton pickupButton) {
      super();
      this.pickupButton = pickupButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      PlayerEntity.getInstance().interactionController.interact(true);
   }
}
