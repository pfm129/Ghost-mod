package dev.ghost.limits;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestLimiterTest {
    @Test void enforcesMinuteAndHourWindows() {
        RequestLimiter limiter = new RequestLimiter();
        assertTrue(limiter.allow(2, 30, 0));
        assertTrue(limiter.allow(2, 30, 1));
        assertFalse(limiter.allow(2, 3, 2));
        assertTrue(limiter.allow(2, 30, 60_001));
        assertTrue(limiter.allow(2, 30, 60_002));
        assertFalse(limiter.allow(2, 30, 60_003));

        RequestLimiter hourly = new RequestLimiter();
        assertTrue(hourly.allow(30, 3, 0));
        assertTrue(hourly.allow(30, 3, 1));
        assertTrue(hourly.allow(30, 3, 60_001));
        assertFalse(hourly.allow(30, 3, 60_002));
    }
}
