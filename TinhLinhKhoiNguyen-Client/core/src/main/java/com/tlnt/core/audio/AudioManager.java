package com.tlnt.core.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.tlnt.core.Settings;
import com.tlnt.core.TaskScheduler;
import com.tlnt.world.entity.player.PlayerEntity;
import java.util.concurrent.TimeUnit;

public final class AudioManager {
   public static float musicVolume = 0.5F;
   public static float soundVolume = 0.5F;
   public static AssetManager assetManager = new AssetManager();
   public static ObjectMap<String, String> audioPaths = new ObjectMap<>();
   public static ObjectMap<Object, ObjectMap<Sound, Long>> loopedSoundIds = new ObjectMap<>();
   public static ObjectSet<Music> playingMusic = new ObjectSet<>();

   public static void setMusicVolume(float volume) {
      Settings.saveMusicVolume(volume);
      musicVolume = volume / 100.0F;
      ObjectSet.ObjectSetIterator it = playingMusic.iterator();

      while (it.hasNext()) {
         ((Music)it.next()).setVolume(musicVolume);
      }
   }

   public static void setSoundVolume(float volume) {
      Settings.saveSoundVolume(volume);
      soundVolume = volume / 100.0F;

      for (Object owner : loopedSoundIds.keys()) {
         ObjectMap<Sound, Long> sounds;
         ObjectMap.Keys it = (sounds = loopedSoundIds.get(owner)).keys().iterator();

         while (it.hasNext()) {
            Sound sound;
            (sound = (Sound)it.next()).setVolume(sounds.get(sound, -1L), soundVolume);
         }
      }
   }

   public static int getMusicVolumePercent() {
      return (int)(musicVolume * 100.0F);
   }

   public static int getSoundVolumePercent() {
      return (int)(soundVolume * 100.0F);
   }

   public static void finishLoading() {
      assetManager.finishLoading();
   }

   public static void playMusic(String musicName) {
      if (!assetManager.isFinished()) {
         TaskScheduler.schedule(new PlayMusicTask(musicName), 1000L, TimeUnit.MILLISECONDS);
      } else {
         try {
            Music music;
            stopOtherMusic(music = assetManager.get(audioPaths.get(musicName)));
            playingMusic.add(music);
            music.setVolume(musicVolume);
            music.setLooping(true);
            music.play();
         } catch (Exception e) {
            stopOtherMusic(null);
            System.out.println(e.getMessage());
         }
      }
   }

   public static void stopOtherMusic(Music keep) {
      ObjectSet.ObjectSetIterator it = playingMusic.iterator();

      while (it.hasNext()) {
         Music music;
         if (!(music = (Music)it.next()).equals(keep)) {
            music.stop();
         }
      }
   }

   public static long playSound(String soundName, Vector2 position) {
      if (position != null && PlayerEntity.getInstance() != null && position.dst(PlayerEntity.getInstance().getCenterPosition()) > 10.0F) {
         return -1L;
      } else {
         try {
            String path = audioPaths.get(soundName);
            if (path == null) {
               return -1L;
            }
            if (!assetManager.isLoaded(path)) {
               assetManager.load(path, Sound.class);
               assetManager.finishLoadingAsset(path);
            }
            return assetManager.<Sound>get(path).play(soundVolume);
         } catch (Exception e) {
            System.out.println(e.getMessage());
            return -1L;
         }
      }
   }

   public static void loopSound(Object owner, String soundName, Vector2 position) {
      if (position == null || PlayerEntity.getInstance() == null || !(position.dst(PlayerEntity.getInstance().getCenterPosition()) > 10.0F)) {
         try {
            ObjectMap<Sound, Long> sounds;
            if ((sounds = loopedSoundIds.get(owner)) == null) {
               sounds = new ObjectMap<>();
               loopedSoundIds.put(owner, sounds);
            }

            Sound sound = assetManager.get(audioPaths.get(soundName));
            if (sounds.get(sound, -1L) != -1L) {
               stopLoopedSound(owner, soundName);
            }

            long loopId = sound.loop(soundVolume);
            sounds.put(sound, loopId);
         } catch (Exception e) {
            System.out.println(e.getMessage());
         }
      }
   }

   public static void stopLoopedSound(Object owner, String soundName) {
      try {
         ObjectMap<Sound, Long> sounds;
         if ((sounds = loopedSoundIds.get(owner)) != null) {
            Sound sound = assetManager.get(audioPaths.get(soundName));
            long loopId;
            if ((loopId = sounds.get(sound, -1L)) != -1L) {
               sound.stop(loopId);
               sounds.put(sound, -1L);
            }
         }
      } catch (Exception e) {
         System.out.println(e.getMessage());
      }
   }

   public static long getLoopedSoundIdForPlayer(PlayerEntity player, String soundName) {
      try {
         ObjectMap<Sound, Long> sounds;
         if ((sounds = loopedSoundIds.get(player)) == null) {
            return -1L;
         } else {
            Sound sound = assetManager.get(audioPaths.get(soundName));
            return sounds.get(sound, -1L);
         }
      } catch (Exception e) {
         return -1L;
      }
   }

   public static void stopAllLoopedSounds() {
      for (Object owner : loopedSoundIds.keys()) {
         ObjectMap<Sound, Long> sounds;
         ObjectMap.Keys it = (sounds = loopedSoundIds.get(owner)).keys().iterator();

         while (it.hasNext()) {
            Sound sound;
            (sound = (Sound)it.next()).stop(sounds.get(sound, -1L));
         }
      }
   }

   static {
      String[] soundLines;
      int soundLineCount = (soundLines = Gdx.files.internal("sounds/sounds.txt").readString().split("\n")).length;

      for (int i = 0; i < soundLineCount; i++) {
         String soundName;
         FileHandle soundFile;
         if (!(soundName = (soundName = soundLines[i]).trim()).isEmpty() && (soundFile = Gdx.files.internal("sounds/" + soundName)).exists()) {
            String soundExt = soundFile.extension();
            if (soundExt.equals("ogg")) {
               assetManager.load(soundFile.path(), Sound.class);
               audioPaths.put(soundFile.nameWithoutExtension(), soundFile.path());
            }
         }
      }

      String[] musicLines;
      int musicLineCount = (musicLines = Gdx.files.internal("sounds/musics.txt").readString().split("\n")).length;

      for (int j = 0; j < musicLineCount; j++) {
         String musicLine;
         String musicName;
         FileHandle musicFile;
         if (!(musicName = (musicLine = musicLines[j]).trim()).isEmpty() && (musicFile = Gdx.files.internal("sounds/" + musicName)).exists()) {
            String musicExt = musicFile.extension();
            if (musicExt.equals("ogg")) {
               assetManager.load(musicFile.path(), Music.class);
               audioPaths.put(musicFile.nameWithoutExtension(), musicFile.path());
            }
         }
      }
   }

}
