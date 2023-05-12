package net.morher.house.raspberrypi.blinds;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.morher.house.raspberrypi.blinds.BlindsMovement.DOWN;
import static net.morher.house.raspberrypi.blinds.BlindsMovement.UP;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PositionCalculator {
    private static final int MIN_POSITION = 0;
    private static final int MAX_POSITION = 100;

    private final Duration downStepDuration;
    private final Duration upStepDuration;
    private final Supplier<Integer> initialPositionSupplier;
    private Integer lastStillPosition;
    private Motion motion;

    public PositionCalculator(Duration downDuration, Duration upDuration, Supplier<Integer> initialPositionSupplier) {
        this.downStepDuration = downDuration.dividedBy(MAX_POSITION);
        this.upStepDuration = upDuration.dividedBy(MAX_POSITION);
        this.initialPositionSupplier = initialPositionSupplier;
    }

    public synchronized void up(Instant time) {
        if (motion != null && !UP.equals(motion.getDirection())) {
            stop(time);
        }
        motion = new Motion(time, UP);
    }

    public synchronized void down(Instant time) {
        if (motion != null && !DOWN.equals(motion.getDirection())) {
            stop(time);
        }
        motion = new Motion(time, DOWN);
    }

    public synchronized void stop(Instant time) {
        lastStillPosition = calculatedLocation(time);
        motion = null;
        log.debug("New still position: {}", lastStillPosition);
    }

    public int calculatedLocation(Instant time) {
        if (lastStillPosition == null) {
            lastStillPosition = initialPositionSupplier.get();
        }
        if (!isInMotion()) {
            return lastStillPosition;
        }
        Duration duration = Duration.between(motion.started, time);
        log.debug("Calculate distance traveled after {}", duration);

        if (UP.equals(motion.getDirection())) {
            return min(MAX_POSITION, lastStillPosition + ((int) duration.dividedBy(upStepDuration)));
        } else {
            return max(MIN_POSITION, lastStillPosition - ((int) duration.dividedBy(downStepDuration)));
        }
    }

    public boolean isInMotion() {
        return motion != null;
    }

    public Optional<Distance> calculateDistanceTo(int newPosition, Instant time) {
        int currentPosition = calculatedLocation(time);
        int diff = newPosition - currentPosition;
        if (diff < 0) {
            return Optional.of(new Distance(diff, DOWN, upStepDuration.multipliedBy(-diff)));
        }
        if (diff > 0) {
            return Optional.of(new Distance(diff, UP, upStepDuration.multipliedBy(diff)));
        }
        return Optional.empty();
    }

    @Data
    private static class Motion {
        private final Instant started;
        private final BlindsMovement direction;
    }

    @Data
    public static class Distance {
        private final int diff;
        private final BlindsMovement direction;
        private final Duration duration;
    }
}
