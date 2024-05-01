package net.morher.house.raspberrypi.sound;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.morher.house.api.devicetypes.AudioVideoDevice;
import net.morher.house.api.entity.Device;
import net.morher.house.api.entity.DeviceManager;
import net.morher.house.raspberrypi.config.RaspberryPiConfig.SoundConfig;
import net.morher.house.raspberrypi.config.RaspberryPiConfig.SpeakerConfig;

@RequiredArgsConstructor
public class SoundController {
  private final DeviceManager deviceManager;
  private final Map<String, Sound> sounds = new HashMap<>();
  private final TextToSpeach tts = new TextToSpeach();

  public void configure(List<SpeakerConfig> list, Map<String, SoundConfig> map) {
    map.forEach(this::configureSound);
    list.forEach(this::configureSpeaker);
  }

  private void configureSound(String name, SoundConfig config) {
    Sound sound = new Sound(new File(config.getFile()));
    sounds.put(name, sound);
  }

  private void configureSpeaker(SpeakerConfig config) {
    Device device = deviceManager.device(config.getDevice().toDeviceId());

    new Speaker(device.entity(AudioVideoDevice.SOUND_COMMAND), sounds, tts);
  }
}
