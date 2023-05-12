package net.morher.house.raspberrypi.blinds;

public interface BlindsControlSession {
    void up(boolean pressed);

    void down(boolean pressed);

    void stop(boolean pressed);

    void close();
}
