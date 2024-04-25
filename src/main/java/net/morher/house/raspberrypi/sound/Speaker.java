package net.morher.house.raspberrypi.sound;

import java.util.Map;
import net.morher.house.api.entity.sound.SoundCommand;
import net.morher.house.api.entity.sound.SoundCommand.SoundRequest;
import net.morher.house.api.entity.sound.SoundEntity;

public class Speaker {
  private final Map<String, Sound> sounds;

  public Speaker(SoundEntity entity, Map<String, Sound> sounds) {
    this.sounds = sounds;
    entity.getSoundRequestTopic().subscribe(this::onCommand);
    entity.getSoundRequestTopic().publish(new SoundCommand());
  }

  private void onCommand(SoundCommand command) {
    // TODO: Implement delay and scheduling
    for (SoundRequest request : command.getSounds()) {
      if (request.getSound() != null) {
        Sound sound = sounds.get(request.getSound());
        if (sound == null) {
          break;
        }
        sound.play();
      }
    }
  }
}
