package document_filter.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
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

}
