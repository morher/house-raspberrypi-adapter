package net.morher.house.raspberrypi.sound;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Sound {
  private final File soundFile;
  private Clip clip;

  public Sound(File soundFile) {
    this.soundFile = soundFile;
    this.clip = loadClip(this.soundFile);
  }

  public void play() {
    clip.stop();
    clip.setFramePosition(0);
    clip.start();
  }

  private Clip loadClip(File soundFile) {
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
      Clip clip = AudioSystem.getClip();
      clip.open(audioInputStream);
      return clip;
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not load sound from file '" + soundFile + "'", e);
    }
  }

  public void stop() {}
}
