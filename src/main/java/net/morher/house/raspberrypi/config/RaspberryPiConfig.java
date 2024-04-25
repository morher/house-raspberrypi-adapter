package net.morher.house.raspberrypi.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import net.morher.house.api.config.DeviceName;

@Data
public class RaspberryPiConfig {

  private final List<BlindsRemoteConfig> blindsRemotes = new ArrayList<>();
  private final List<CommandSwitchConfig> commandSwitches = new ArrayList<>();
  private final List<SpeakerConfig> speakers = new ArrayList<>();
  private final Map<String, SoundConfig> sounds = new HashMap<>();

  @Data
  public static class BlindsRemoteConfig {
    private final BlindsRemoteButtonsConfig buttons = new BlindsRemoteButtonsConfig();
    private int channelCount;
    private int holdForMaxDurationMs;
    private List<BlindsRemoteChannelConfig> channels = new ArrayList<>();
  }

  @Data
  public static class BlindsRemoteButtonsConfig {
    private final BlindsRemoteButtonConfig up = new BlindsRemoteButtonConfig();
    private final BlindsRemoteButtonConfig down = new BlindsRemoteButtonConfig();
    private final BlindsRemoteButtonConfig stop = new BlindsRemoteButtonConfig();
    private final BlindsRemoteButtonConfig channelSelect = new BlindsRemoteButtonConfig();
    private final BlindsRemoteButtonConfig reset = new BlindsRemoteButtonConfig();
  }

  @Data
  public static class BlindsRemoteButtonConfig {
    private int pin;
    private long clickDuration = 800l;
    private long cooldownDuration = 50l;
  }

  @Data
  public static class BlindsRemoteChannelConfig {
    private int channel;
    private int upDurationMs;
    private int downDurationMs;
    private DeviceName device;
  }

  @Data
  public static class CommandSwitchConfig {
    private DeviceName device;
    private String[] onCommand;
    private String[] offCommand;
  }

  @Data
  public static class SpeakerConfig {
    private DeviceName device;
  }

  @Data
  public static class SoundConfig {
    private String file;
  }
}
