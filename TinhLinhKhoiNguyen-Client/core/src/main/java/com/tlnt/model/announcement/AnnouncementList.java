package com.tlnt.model.announcement;

import com.badlogic.gdx.utils.Array;
import com.tlnt.util.TimeFormatter;

public final class AnnouncementList {
   public Array<Announcement> announcements = new Array<>();

   public final void addOrUpdateAnnouncement(int id, String text, long startTime, int durationMs) {
      Announcement existing = null;
      Array.ArrayIterator it = this.announcements.iterator();

      while (it.hasNext()) {
         Announcement announcement;
         if ((announcement = (Announcement)it.next()).id == id) {
            existing = announcement;
            break;
         }
      }

      if (existing == null) {
         existing = new Announcement(id, text, startTime, durationMs);
         this.announcements.insert(0, existing);
      } else {
         existing.startTime = startTime;
         existing.durationMs = durationMs;
      }
   }

   public final void removeAnnouncement(int id) {
      Array.ArrayIterator it = this.announcements.iterator();

      while (it.hasNext()) {
         Announcement announcement;
         if ((announcement = (Announcement)it.next()).id == id) {
            this.announcements.removeValue(announcement, true);
            return;
         }
      }
   }

   public final String getCurrentAnnouncementText() {
      Announcement current;
      return this.announcements.isEmpty()
         ? null
         : (current = this.announcements.first()).text + " " + TimeFormatter.formatTimeRemaining(current.startTime + (long)current.durationMs);
   }
}
