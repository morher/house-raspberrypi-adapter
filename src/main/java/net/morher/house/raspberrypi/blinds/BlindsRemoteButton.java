package net.morher.house.raspberrypi.blinds;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BlindsRemoteButton {
    private final String name;
    private final Consumer<Boolean> stateConsumer;
    @Getter
    final Duration clickDuration;
    @Getter
    private final Duration cooldownDuration;
    @Getter
    private Instant holdAt;

    public boolean isHolding() {
        return holdAt != null;
    }

    public void hold(Instant now) {
        this.holdAt = now;
        stateConsumer.accept(true);
        log.debug("Hold button {}", this);
    }

    public void release() {
        this.holdAt = null;
        stateConsumer.accept(false);
        log.debug("Release button {}", this);
    }

    @Override
    public String toString() {
        return name;
    }
}