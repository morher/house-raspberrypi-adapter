package net.morher.house.raspberrypi.sound;

import java.util.Map;
import net.morher.house.api.entity.sound.SoundCommand;
import net.morher.house.api.entity.sound.SoundCommand.SoundRequest;
import net.morher.house.api.entity.sound.SoundEntity;

public class Speaker {
  private final Map<String, Sound> sounds;
  private final TextToSpeach tts;

  public Speaker(SoundEntity entity, Map<String, Sound> sounds, TextToSpeach tts) {
    this.sounds = sounds;
    this.tts = tts;
    entity.getSoundRequestTopic().subscribe(this::onCommand);
    entity.getSoundRequestTopic().publish(new SoundCommand());
  }

  private void onCommand(SoundCommand command) {
    // TODO: Implement delay and scheduling
    for (SoundRequest request : command.getSounds()) {
      if (request.getSound() != null) {
        Sound sound = sounds.get(request.getSound());
        if (sound != null) {
          sound.play();
        }
      }
      if (request.getText() != null) {
        tts.say(request.getText(), request.getVoice());
      }
    }
  }
}
