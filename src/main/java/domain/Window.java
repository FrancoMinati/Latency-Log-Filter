package domain;

import latency.LatencyWindowAverager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Window {
    private long startEpochMillis;
    private List<Integer> latencies = new ArrayList<>();

    public Window(long startEpochMillis) {
        this.startEpochMillis = startEpochMillis;
    }

    public void add(int latency) {
        latencies.add(latency);
    }

    double getAverage() {
        return latencies.stream().mapToInt(i -> i).average().orElse(0.0);
    }

    int getCount() {
        return latencies.size();
    }

    public WindowResult toResult() {
        return new WindowResult(Instant.ofEpochMilli(startEpochMillis), getAverage(), getCount());
    }

    public long getStartEpochMillis() {
        return startEpochMillis;
    }

    public void setStartEpochMillis(long startEpochMillis) {
        this.startEpochMillis = startEpochMillis;
    }

    public List<Integer> getLatencies() {
        return latencies;
    }

    public void setLatencies(List<Integer> latencies) {
        this.latencies = latencies;
    }
}
