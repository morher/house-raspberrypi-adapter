package net.morher.house.raspberrypi.blinds;

import java.time.Instant;

public interface BlindMotion {
    Instant motionStart();

    void stop();
}
