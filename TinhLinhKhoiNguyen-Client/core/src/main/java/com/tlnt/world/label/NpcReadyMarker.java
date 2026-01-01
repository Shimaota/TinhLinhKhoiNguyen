package com.tlnt.world.label;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

public final class NpcReadyMarker extends FloatingOverlay {
   public TextureAsset playerEntity;
   public NpcEntity npcEntity;

   public NpcReadyMarker(NpcEntity npcEntity) {
      super(new Vector2(0.0F, 100.0F));
      this.playerEntity = TextureAsset.obtain("ui/197");
      this.npcEntity = npcEntity;
   }

   @Override
   public final void refresh() {
      int visibleFlag;
      if (this.npcEntity.POOL2.template.id == GameState.tutorialState.targetObjectId
         && GameMain.instance.getLoginScreen().editorViewport.getId() == GameState.tutorialState.targetMapId) {
         visibleFlag = 1;
      } else {
         visibleFlag = 0;
      }

      this.setVisible((visibleFlag != 0));
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      float scale = GameUtils.oscillateInterpolated(Interpolation.sine, 0.5F, 0.95F, 1.0F);
      ctx.drawTextureSized(
         this.playerEntity,
         x,
         y + GameUtils.oscillateInterpolated(Interpolation.sine, 0.5F, 0.0F, 10.0F),
         (float)this.playerEntity.getWidth() * scale,
         (float)this.playerEntity.getHeight() * scale,
         Anchor.CENTER
      );
   }

   @Override
   public final void release() {
      super.release();
      this.npcEntity = null;
   }

}
