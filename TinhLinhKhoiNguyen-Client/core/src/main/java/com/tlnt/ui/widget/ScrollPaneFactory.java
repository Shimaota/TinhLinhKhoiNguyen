package com.tlnt.ui.widget;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.tlnt.ui.window.Dialog;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.ranking.panel.RankScrollRefreshListener;

public final class ScrollPaneFactory {
   public static ScrollPane createScrollPane(Table table) {
      return createRefreshScrollPane(table, null);
   }

   public static RefreshScrollPane createRefreshScrollPane(Actor actor, RankScrollRefreshListener rankScrollRefreshListener) {
      RefreshScrollPane scrollPane;
      RefreshScrollPane pane = scrollPane = new RefreshScrollPane(actor, rankScrollRefreshListener);
      pane.setFadeScrollBars(false);
      if (rankScrollRefreshListener != null) {
         scrollPane.setForceScroll(false, true);
      }

      return scrollPane;
   }

   public static void refreshVisibleActors(Actor actor) {
      if (actor.isVisible()) {
         if (actor instanceof Dialog) {
            ((Dialog)actor).onClose2();
         } else if (actor instanceof GamePanel) {
            ((GamePanel)actor).refresh();
         }

         if (actor instanceof Group) {
            Array.ArrayIterator it = ((Group)actor).getChildren().iterator();

            while (it.hasNext()) {
               refreshVisibleActors((Actor)it.next());
            }
         }
      }
   }

   public interface RefreshListener {
      void onRefresh();
   }
}
