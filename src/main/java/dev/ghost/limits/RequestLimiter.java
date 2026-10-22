package dev.ghost.limits;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RequestLimiter {
    private final Deque<Long> timestamps = new ArrayDeque<>();

    public synchronized boolean allow(int perMinute, int perHour, long nowMillis) {
        while (!timestamps.isEmpty() && nowMillis - timestamps.peekFirst() >= 3_600_000L) timestamps.removeFirst();
        long minuteCount = timestamps.stream().filter(time -> nowMillis - time < 60_000L).count();
        if (timestamps.size() >= Math.max(1, perHour) || minuteCount >= Math.max(1, perMinute)) return false;
        timestamps.addLast(nowMillis);
        return true;
    }

    public synchronized void clear() { timestamps.clear(); }
}
