package net.morher.house.raspberrypi.cmdswitch;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import net.morher.house.api.entity.switches.SwitchEntity;
import net.morher.house.api.entity.switches.SwitchStateHandler;
import net.morher.house.raspberrypi.config.RaspberryPiConfig.CommandSwitchConfig;

@Slf4j
public class CommandSwitch {
  private final String[] onCommand;
  private final String[] offCommand;

  public CommandSwitch(SwitchEntity entity, CommandSwitchConfig config) {
    new SwitchStateHandler(entity, this::onState);
    this.onCommand = config.getOnCommand();
    this.offCommand = config.getOffCommand();
  }

  private void onState(boolean on) {
    String[] cmd = on ? onCommand : offCommand;
    try {
      Runtime.getRuntime().exec(cmd);
    } catch (IOException e) {
      log.error("Failed to run command '{}'", cmd, e);
    }
  }
}
