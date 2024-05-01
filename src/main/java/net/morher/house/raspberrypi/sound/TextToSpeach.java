package net.morher.house.raspberrypi.sound;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TextToSpeach {

  public void say(String text, String voice) {
    String[] cmd = {"espeak", text};
    try {
      Runtime.getRuntime().exec(cmd);

    } catch (IOException e) {
      log.error("Failed to run command '{}'", cmd, e);
    }
  }
}
