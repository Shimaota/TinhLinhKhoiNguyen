package com.tlnt.model.announcement;

public final class Announcement {
   public int id;
   public String text;
   public long startTime;
   public int durationMs;

   public Announcement(int id, String text, long startTime, int durationMs) {
      this.id = id;
      this.text = text;
      this.startTime = startTime;
      this.durationMs = durationMs;
   }
}
