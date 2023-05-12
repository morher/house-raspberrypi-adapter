package net.morher.house.raspberrypi.blinds;

import java.time.Instant;

import lombok.Data;

@Data
public class MotionReport {
    private final BlindsMovement direction;
    private final Instant started;

}
