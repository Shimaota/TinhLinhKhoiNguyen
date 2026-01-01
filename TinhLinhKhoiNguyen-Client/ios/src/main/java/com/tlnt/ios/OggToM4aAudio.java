package com.tlnt.ios;

import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.backends.iosrobovm.IOSAudio;
import com.badlogic.gdx.files.FileHandle;

/**
 * iOS cannot read .ogg (ObjectAL / AudioToolbox reports 'typ?'), while AudioManager only loads .ogg files.
 * ios:convertIosAudio converts every sounds/*.ogg to .m4a (AAC) at build time -> here x.m4a is loaded instead of x.ogg.
 * The .ogg files are still packaged because AudioManager checks exists() on the .ogg name.
 */
class OggToM4aAudio implements IOSAudio {
    private final IOSAudio audio;

    OggToM4aAudio(IOSAudio audio) {
        this.audio = audio;
    }

    private static FileHandle redirect(FileHandle file) {
        if (!"ogg".equalsIgnoreCase(file.extension())) return file;
        FileHandle m4a = file.sibling(file.nameWithoutExtension() + ".m4a");
        return m4a.exists() ? m4a : file;
    }

    @Override
    public Sound newSound(FileHandle fileHandle) {
        return audio.newSound(redirect(fileHandle));
    }

    @Override
    public Music newMusic(FileHandle file) {
        return audio.newMusic(redirect(file));
    }

    @Override
    public AudioDevice newAudioDevice(int samplingRate, boolean isMono) {
        return audio.newAudioDevice(samplingRate, isMono);
    }

    @Override
    public AudioRecorder newAudioRecorder(int samplingRate, boolean isMono) {
        return audio.newAudioRecorder(samplingRate, isMono);
    }

    @Override
    public boolean switchOutputDevice(String deviceIdentifier) {
        return audio.switchOutputDevice(deviceIdentifier);
    }

    @Override
    public String[] getAvailableOutputDevices() {
        return audio.getAvailableOutputDevices();
    }

    @Override
    public void didBecomeActive() {
        audio.didBecomeActive();
    }

    @Override
    public void willEnterForeground() {
        audio.willEnterForeground();
    }

    @Override
    public void willResignActive() {
        audio.willResignActive();
    }

    @Override
    public void willTerminate() {
        audio.willTerminate();
    }
}
