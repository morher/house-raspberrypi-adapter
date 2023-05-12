package net.morher.house.raspberrypi.blinds;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import org.junit.Test;

public class PositionCalculatorTest {

    @Test
    public void testInitialPosition() {
        @SuppressWarnings("unchecked")
        Supplier<Integer> initialPositionSupplier = mock(Supplier.class);

        // Constructor should not call supplier
        PositionCalculator calc = new PositionCalculator(
                Duration.ofMillis(100),
                Duration.ofMillis(100),
                initialPositionSupplier);
        verify(initialPositionSupplier, never()).get();

        // First motion should not call supplier
        calc.down(Instant.now());
        verify(initialPositionSupplier, never()).get();

        // calculatedLocation should call supplier...
        doReturn(50).when(initialPositionSupplier).get();
        calc.calculatedLocation(Instant.now());
        verify(initialPositionSupplier, times(1)).get();

        // ...but only the first time
        calc.calculatedLocation(Instant.now());
        verify(initialPositionSupplier, times(1)).get();
    }

    @Test
    public void testCalculateLocation() {
        PositionCalculator calc = new PositionCalculator(
                Duration.ofMillis(10000),
                Duration.ofMillis(10000),
                () -> 50);

        Instant now = Instant.now();

        assertThat("Starting at 50",
                calc.calculatedLocation(now), is(equalTo(50)));

        calc.down(now);
        assertThat("After 500ms of moving down at a rate of 1pr 100ms, position should be 45",
                calc.calculatedLocation(now.plusMillis(505)), is(equalTo(45)));

        calc.stop(now.plusMillis(800));
        assertThat("Stopped after 800ms, the position should be 42, even a long time after",
                calc.calculatedLocation(now.plusMillis(3000)), is(equalTo(42)));
    }

    @Test
    public void testUpGoesSlowerThanDown() {
        PositionCalculator calc = new PositionCalculator(
                Duration.ofMillis(10000),
                Duration.ofMillis(20000),
                () -> 0);

        Instant now = Instant.now();
        calc.down(now);
        calc.stop(now.plusMillis(10000));
        calc.up(now.plusMillis(12000));
        calc.stop(now.plusMillis(22000));

        assertThat(
                calc.calculatedLocation(now.plusMillis(25000)),
                is(equalTo(50)));
    }
}
