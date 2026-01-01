package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.announcement.Announcement;
import com.tlnt.model.announcement.AnnouncementList;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.util.GameUtils;

public final class AnnouncementBar extends BaseTable {
   public AnnouncementBar(RenderContext renderContext) {
      super(renderContext);
      this.setSize(420.0F, 60.0F);
      float posY = (float)(WorldConfig.VIEWPORT_HEIGHT - 180);
      this.setPosition(109.0F, posY, 10);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      AnnouncementList announcements = GameState.announcements;
      Gdx.graphics.getDeltaTime();
      AnnouncementList announcementList = announcements;

      for (int i = announcements.announcements.size - 1; i >= 0; i--) {
         Announcement announcement;
         if (GameUtils.hasElapsed((announcement = announcementList.announcements.get(i)).startTime, announcement.durationMs)) {
            announcementList.announcements.removeIndex(i);
         }
      }

      String text;
      if ((text = GameState.announcements.getCurrentAnnouncementText()) != null) {
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, text, this.getX(), this.getY() + this.getHeight() / 2.0F, Anchor.MIDDLE_LEFT, 24.0F, Color.WHITE);
      }
   }
}
