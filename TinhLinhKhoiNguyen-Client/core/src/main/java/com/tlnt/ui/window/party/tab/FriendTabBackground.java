package com.tlnt.ui.window.party.tab;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class FriendTabBackground extends BaseTable {
   public com.tlnt.ui.window.party.tab.PartyFriendsTab friendsTab;
   public NinePatchAsset background;

   public FriendTabBackground(PartyFriendsTab friendsTab, RenderContext renderContext) {
      super(renderContext);
      this.friendsTab = friendsTab;
      this.background = NinePatchAsset.obtain("ui/100", 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
