package com.tlnt.ui.window.party.tab;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class FriendTabButtonBar extends BaseTable {
   public com.tlnt.ui.window.party.tab.PartyFriendsTab friendsTab;
   public CompositeSprite buttonBarSprite;

   public FriendTabButtonBar(PartyFriendsTab friendsTab, RenderContext renderContext) {
      super(renderContext);
      this.friendsTab = friendsTab;
      this.buttonBarSprite = CompositeSprite.obtain(
         "ui/104",
         "ui/105",
         "ui/106",
         false
      );
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawCompositeSprite(this.buttonBarSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
