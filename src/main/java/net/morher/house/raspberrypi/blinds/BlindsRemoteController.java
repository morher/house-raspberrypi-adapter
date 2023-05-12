package net.morher.house.raspberrypi.blinds;

import java.time.Duration;
import java.util.List;

import com.diozero.api.DigitalOutputDevice;

import lombok.RequiredArgsConstructor;
import net.morher.house.api.devicetypes.CoverDevice;
import net.morher.house.api.entity.Device;
import net.morher.house.api.entity.DeviceInfo;
import net.morher.house.api.entity.DeviceManager;
import net.morher.house.api.entity.cover.CoverEntity;
import net.morher.house.api.entity.cover.CoverOptions;
import net.morher.house.api.schedule.HouseScheduler;
import net.morher.house.raspberrypi.config.RaspberryPiConfig.BlindsRemoteButtonConfig;
import net.morher.house.raspberrypi.config.RaspberryPiConfig.BlindsRemoteButtonsConfig;
import net.morher.house.raspberrypi.config.RaspberryPiConfig.BlindsRemoteChannelConfig;
import net.morher.house.raspberrypi.config.RaspberryPiConfig.BlindsRemoteConfig;

@RequiredArgsConstructor
public class BlindsRemoteController {
    private final HouseScheduler scheduler = HouseScheduler.get();
    private final DeviceManager deviceManager;

    public void configure(List<BlindsRemoteConfig> blindsRemotes) {
        blindsRemotes.forEach(this::configure);
    }

    private void configure(BlindsRemoteConfig config) {

        BlindsRemoteButtonsConfig buttons = config.getButtons();

        BlindsRemote remote = new BlindsRemote(
                scheduler,
                button("Reset", buttons.getReset()),
                button("Channel select", buttons.getChannelSelect()),
                button("Stop", buttons.getStop()),
                button("Up", buttons.getUp()),
                button("Down", buttons.getDown()),
                config.getChannelCount());

        config.getChannels().forEach(channel -> configure(channel, remote));
    }

    private BlindsRemoteButton button(String name, BlindsRemoteButtonConfig config) {
        DigitalOutputDevice pin = new DigitalOutputDevice(config.getPin());

        return new BlindsRemoteButton(
                name,
                pin::setOn,
                Duration.ofMillis(config.getClickDuration()),
                Duration.ofMillis(config.getCooldownDuration()));
    }

    private void configure(BlindsRemoteChannelConfig config, BlindsRemote remote) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setManufacturer("Raspberry PI");

        Device device = deviceManager.device(config.getDevice().toDeviceId());
        device.setDeviceInfo(deviceInfo);

        CoverOptions options = new CoverOptions();
        options.setPosition(true);
        CoverEntity coverEntity = device.entity(CoverDevice.COVER, options);

        new BlindsChannelOperator(scheduler, remote.channel(config.getChannel()), coverEntity, config);
    }
}
