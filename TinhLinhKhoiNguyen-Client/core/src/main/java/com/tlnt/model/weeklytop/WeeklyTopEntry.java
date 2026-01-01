package com.tlnt.model.weeklytop;

/** One row in the weekly event ranking. */
public final class WeeklyTopEntry {
   public int rank;
   public int playerId;
   public String playerName;
   public String score;

   public WeeklyTopEntry(int rank, int playerId, String playerName, String score) {
      this.rank = rank;
      this.playerId = playerId;
      this.playerName = playerName;
      this.score = score;
   }
}
