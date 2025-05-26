package domain;

import java.time.Instant;

public class WindowResult {
    public final Instant windowStart;
    public final double averageLatency;
    public final int count;

    public WindowResult(Instant windowStart, double averageLatency, int count) {
        this.windowStart = windowStart;
        this.averageLatency = averageLatency;
        this.count = count;
    }

    @Override
    public String toString() {
        return String.format("Window starting at %s: count=%d, avg=%.2f ms",
                windowStart, count,averageLatency);
    }
}