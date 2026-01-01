package com.tlnt.core.audio;

public final class PlayMusicTask implements Runnable {
   public java.lang.String musicName;
   public PlayMusicTask(String musicName) {
      super();
      this.musicName = musicName;
   }

   @Override
   public final void run() {
      AudioManager.playMusic(this.musicName);
   }
}
