package net.morher.house.raspberrypi.blinds;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.morher.house.api.entity.cover.CoverCommand;
import net.morher.house.api.entity.cover.CoverEntity;
import net.morher.house.api.entity.cover.CoverState;
import net.morher.house.api.schedule.DelayedTrigger;
import net.morher.house.api.schedule.HouseScheduler;
import net.morher.house.api.state.StateObserver;
import net.morher.house.api.utils.ResourceManager;
import net.morher.house.raspberrypi.blinds.BlindsRemote.BlindsChannel;
import net.morher.house.raspberrypi.blinds.PositionCalculator.Distance;
import net.morher.house.raspberrypi.config.RaspberryPiConfig.BlindsRemoteChannelConfig;

@Slf4j
public class BlindsChannelOperator {
    private final HouseScheduler scheduler;
    private final DelayedTrigger trigger;
    private final BlindsChannel channel;
    private final CoverEntity entity;
    private final ResourceManager resources = new ResourceManager();

    // State:
    private boolean requestedStop;
    private Integer requestedPosition;
    private PositionCalculator position;
    private Distance plan;
    private MotionReport lastMotionReport;
    private boolean reportPosition;

    public BlindsChannelOperator(HouseScheduler scheduler, BlindsChannel channel, CoverEntity entity, BlindsRemoteChannelConfig config) {
        this.scheduler = scheduler;
        this.channel = channel;
        this.entity = entity;

        channel.addListener(this::onMotionReport);

        StateObserver<Integer> positionObserver = entity.position().state().observer(0);
        resources.add(positionObserver);
        resources.add(entity.command().subscribe(this::onCoverCommand));
        resources.add(entity.position().command().subscribe(this::onPositionCommand));

        trigger = scheduler.delayedTrigger("Handle blinds channel state", this::update);
        position = new PositionCalculator(
                Duration.ofMillis(config.getDownDurationMs()),
                Duration.ofMillis(config.getUpDurationMs()),
                positionObserver);
    }

    private void onCoverCommand(CoverCommand command) {
        log.debug("Cover command received for {}: {}", entity.getId(), command);
        switch (command) {
        case OPEN:
            this.requestedStop = false;
            this.requestedPosition = 100;
            break;

        case CLOSE:
            this.requestedStop = false;
            this.requestedPosition = 0;
            break;

        case STOP:
            this.requestedStop = true;
            this.requestedPosition = null;
            break;
        }
        trigger.runNow();
    }

    private void onPositionCommand(Integer position) {
        log.debug("Cover position command received for {}: {}", entity.getId(), position);
        this.requestedPosition = position;
        trigger.runNow();
    }

    private void onMotionReport(MotionReport report) {
        Instant started = report.getStarted();
        BlindsMovement direction = report.getDirection();
        if (direction == null) {
            lastMotionReport = null;
            position.stop(started);

        } else if (BlindsMovement.UP.equals(direction)) {
            lastMotionReport = report;
            position.up(started);

        } else if (BlindsMovement.DOWN.equals(direction)) {
            lastMotionReport = report;
            position.down(started);
        }
        trigger.runNow();
    }

    private void update() {
        if (requestedStop) {
            stop();
            return;
        }

        if (isNewPositionOrdered()) {
            Optional<Distance> distance = position.calculateDistanceTo(requestedPosition, scheduler.now());
            if (currentMovementMustBeStopped(distance)) {
                stop();

                return;
            }
            plan = distance.orElse(null);
            requestedPosition = null;
            if (plan != null) {
                BlindsMovement direction = plan.getDirection();
                if (BlindsMovement.UP.equals(direction)) {
                    entity.state().publish(CoverState.OPENING);
                    channel.requestUp();
                } else if (BlindsMovement.DOWN.equals(direction)) {
                    entity.state().publish(CoverState.CLOSING);
                    channel.requestDown();
                }
            }
        }

        if (plan != null && lastMotionReport != null) {
            Instant stopTime = lastMotionReport.getStarted().plus(plan.getDuration());

            if (scheduler.now().isBefore(stopTime)) {
                trigger.runAt(stopTime);

            } else {
                stop();
                return;
            }
        }

        if (!position.isInMotion() && reportPosition) {
            reportPosition = false;
            entity.position().state().publish(position.calculatedLocation(scheduler.now()));
            int currentPosition = position.calculatedLocation(scheduler.now());
            if (currentPosition >= 100) {
                entity.state().publish(CoverState.OPEN);

            } else if (currentPosition <= 0) {
                entity.state().publish(CoverState.CLOSED);

            } else {
                entity.state().publish(CoverState.STOPPED);
            }
        }
    }

    private void stop() {
        reportPosition = true;
        requestedStop = false;
        plan = null;
        lastMotionReport = null;
        channel.requestStop();
    }

    private boolean currentMovementMustBeStopped(Optional<Distance> distance) {
        if (plan == null) {
            return false;
        }
        if (distance.isPresent()) {
            return !Objects.equals(plan.getDirection(), distance.get().getDirection());
        }
        return true;
    }

    private boolean isNewPositionOrdered() {
        return requestedPosition != null;
    }

    @Data
    private class Plan {
        private Distance distance;
        private CompletableFuture<Instant> motionBegin;
    }

    @Data
    private static class Motion {
        private final int startPosition;
        private final Instant started;

        private final int targetPosition;
        private final int direction;
        private final Instant until;
    }

    @Data
    private static class MotionPlan {
        private final BlindsMovement direction;
        private final Duration duration;
        private final Instant until;
    }
}
