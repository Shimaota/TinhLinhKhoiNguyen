package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.utils.Pool;

public final class TribeMemberButtonPool extends Pool<TribeMemberRow.MemberButton> {
   public com.tlnt.ui.window.tribe.panel.TribeMemberRow ownerRow;
   public TribeMemberButtonPool(TribeMemberRow ownerRow) {
      super();
      this.ownerRow = ownerRow;
   }

   @Override
   public TribeMemberRow.MemberButton newObject() {
      return (this.ownerRow).new MemberButton(this.ownerRow.renderContext);
   }
}
