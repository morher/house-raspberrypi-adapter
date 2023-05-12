package net.morher.house.raspberrypi.blinds;

import static net.morher.house.raspberrypi.blinds.BlindsMovement.DOWN;
import static net.morher.house.raspberrypi.blinds.BlindsMovement.UP;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.morher.house.api.schedule.DelayedTrigger;
import net.morher.house.api.schedule.HouseScheduler;
import net.morher.house.api.schedule.Reschedule;

@RequiredArgsConstructor
public class BlindsRemote {
    private final HouseScheduler scheduler;
    private final DelayedTrigger trigger;

    private int selectedChannel = 1;
    private BlindsMovement currentDirection;
    private BlindsMovement requestedDirection;

    private final BlindsRemoteButton reset;
    private final BlindsRemoteButton channelSelect;
    private final BlindsRemoteButton stop;
    private final BlindsRemoteButton up;
    private final BlindsRemoteButton down;

    private Instant cooldownUntil;

    public BlindsRemote(
            HouseScheduler scheduler,
            BlindsRemoteButton reset,
            BlindsRemoteButton channelSelect,
            BlindsRemoteButton stop,
            BlindsRemoteButton up,
            BlindsRemoteButton down,
            int availableChannels) {

        this.scheduler = scheduler;
        this.trigger = scheduler.delayedTrigger("Handle remote state", this::handleState);

        this.reset = reset;
        this.channelSelect = channelSelect;
        this.stop = stop;
        this.up = up;
        this.down = down;
    }

    private final Map<Integer, BlindsChannel> channels = new HashMap<>();

    public synchronized BlindsChannel channel(int channelNumber) {
        BlindsChannel channel = channels.get(channelNumber);
        if (channel == null) {
            channel = new BlindsChannel(channelNumber);
            channels.put(channelNumber, channel);
        }
        return channel;
    }

    private synchronized void handleState() throws Reschedule {
        Instant now = Instant.now();
        handleCooldown(now);
        handleClickRelease(reset);
        handleClickRelease(channelSelect);
        handleClickRelease(stop);

        // Check if current channel has MovementRequest
        BlindsChannel channel = channels.get(selectedChannel);

        if (channel != null && !Objects.equals(currentDirection, channel.getRequest())) {
            requestedDirection = channel.getRequest();
            if (!UP.equals(requestedDirection) && up.isHolding()) {
                release(up);
            }
            if (!DOWN.equals(requestedDirection) && down.isHolding()) {
                release(down);
            }
            if (currentDirection != null) {
                currentDirection = null;
                channel.sendMotionReport(new MotionReport(null, scheduler.now()));
                click(stop);
            }
            if (DOWN.equals(requestedDirection) && !down.isHolding()) {
                channel.sendMotionReport(new MotionReport(DOWN, scheduler.now()));
                currentDirection = DOWN;
                down.hold(now);
            }
            if (UP.equals(requestedDirection) && !up.isHolding()) {
                channel.sendMotionReport(new MotionReport(UP, scheduler.now()));
                currentDirection = UP;
                up.hold(now);
            }
        }

        // TODO: Next channel...
    }

    private void click(BlindsRemoteButton button) throws Reschedule {
        button.hold(scheduler.now());
        throw Reschedule.at(scheduler.now().plus(button.clickDuration));
    }

    private void release(BlindsRemoteButton button) throws Reschedule {
        button.release();
        cooldown(button.getCooldownDuration());
    }

    private void handleCooldown(Instant now) throws Reschedule {
        if (cooldownUntil != null) {
            if (now.isBefore(cooldownUntil)) {
                throw Reschedule.at(cooldownUntil);
            }
            cooldownUntil = null;
        }
    }

    private void handleClickRelease(BlindsRemoteButton button) throws Reschedule {
        if (button.isHolding()) {
            Instant holdUntil = button.getHoldAt().plus(button.getClickDuration());
            if (scheduler.now().isBefore(holdUntil)) {
                throw Reschedule.at(holdUntil);
            }
            button.release();
            if (button.getCooldownDuration() != null) {
                cooldown(button.getCooldownDuration());
            }
        }
    }

    private void cooldown(Duration duration) throws Reschedule {
        if (duration != null) {
            cooldownUntil = scheduler.now().plus(duration);
            throw Reschedule.at(cooldownUntil);
        }
    }

    @RequiredArgsConstructor
    public class BlindsChannel {
        private final List<Consumer<MotionReport>> reportListeners = new ArrayList<>();
        @Getter
        private final int channelNumber;
        @Getter
        @Setter
        private BlindsMovement currentDirection;
        @Getter
        private BlindsMovement request;

        public void addListener(Consumer<MotionReport> listener) {
            reportListeners.add(listener);
        }

        void sendMotionReport(MotionReport report) {
            reportListeners.forEach(l -> l.accept(report));
        }

        public void requestUp() {
            registerRequest(BlindsMovement.UP);
        }

        public void requestDown() {
            registerRequest(BlindsMovement.DOWN);
        }

        public void requestStop() {
            registerRequest(null);
        }

        private synchronized void registerRequest(BlindsMovement direction) {
            this.request = direction;
            trigger.runNow();
        }
    }

}
