package com.tlnt.screen.charselect;

import com.tlnt.screen.BaseScreen;
import com.tlnt.screen.charselect.scene.CharSelectSceneObject;

public final class CharacterSelectionScreen extends BaseScreen {
   public CharSelectSceneObject active;

   @Override
   public final void onCreate() {
      this.active = new CharSelectSceneObject(this);
   }

   @Override
   public final void onUpdate(float delta) {
   }

   @Override
   public final void onDispose() {
   }
}
