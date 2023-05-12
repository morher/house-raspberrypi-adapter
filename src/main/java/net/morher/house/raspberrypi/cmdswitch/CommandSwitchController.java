package net.morher.house.raspberrypi.cmdswitch;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.morher.house.api.devicetypes.GeneralDevice;
import net.morher.house.api.entity.Device;
import net.morher.house.api.entity.DeviceInfo;
import net.morher.house.api.entity.DeviceManager;
import net.morher.house.api.entity.switches.SwitchEntity;
import net.morher.house.api.entity.switches.SwitchOptions;
import net.morher.house.raspberrypi.config.RaspberryPiConfig.CommandSwitchConfig;

@RequiredArgsConstructor
public class CommandSwitchController {
  private final DeviceManager deviceManager;

  public void configure(List<CommandSwitchConfig> cmdswConfigs) {
    for (CommandSwitchConfig commandSwitchConfig : cmdswConfigs) {
      configure(commandSwitchConfig);
    }
  }

  public void configure(CommandSwitchConfig config) {
    DeviceInfo deviceInfo = new DeviceInfo();
    deviceInfo.setManufacturer("Raspberry PI");

    Device device = deviceManager.device(config.getDevice().toDeviceId());
    device.setDeviceInfo(deviceInfo);

    SwitchEntity entity = device.entity(GeneralDevice.ENABLE, new SwitchOptions());
    new CommandSwitch(entity, config);
  }
}
