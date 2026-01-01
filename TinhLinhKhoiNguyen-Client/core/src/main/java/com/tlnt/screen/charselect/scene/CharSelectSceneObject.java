package com.tlnt.screen.charselect.scene;

import com.badlogic.gdx.Gdx;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.charselect.CharacterSelectionScreen;
import com.tlnt.util.FileStorage;

public final class CharSelectSceneObject extends GameObject {

   public CharSelectSceneObject(CharacterSelectionScreen screen) {
      super(screen);
   }

   @Override
   public final void render(RenderContext ctx) {
      if (Gdx.files.internal("images/char_1.png").exists() || FileStorage.getExternalFile("char_1.png").exists()) {
         ctx.drawTexture(TextureAsset.obtain("char_1"), 0.0F, 0.0F, Anchor.CENTER);
      }
   }

   @Override
   public final void dispose() {
   }

}
