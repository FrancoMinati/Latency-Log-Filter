package latency;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class LatencyWindowAverager {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS");
    private final int windowSizeSeconds;
    private final ZoneId zoneId;

    public static class Result {
        public final Instant windowStart;
        public final double averageLatency;
        public final int count;

        public Result(Instant windowStart, double averageLatency, int count) {
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

    private static class Bucket {
        long startEpochMillis;
        List<Integer> latencies = new ArrayList<>();

        Bucket(long startEpochMillis) {
            this.startEpochMillis = startEpochMillis;
        }

        void add(int latency) {
            latencies.add(latency);
        }

        double getAverage() {
            return latencies.stream().mapToInt(i -> i).average().orElse(0.0);
        }

        int getCount() {
            return latencies.size();
        }

        Result toResult() {
            return new Result(Instant.ofEpochMilli(startEpochMillis), getAverage(), getCount());
        }
    }

    private final Map<Long, Bucket> buckets = new TreeMap<>();

    public LatencyWindowAverager(int windowSizeSeconds) {
        this.windowSizeSeconds = windowSizeSeconds;
        this.zoneId = ZoneId.of("America/Argentina/Buenos_Aires"); // o configurable
    }

    public void addLine(String line) {
        String[] parts = line.split(";");
        if (parts.length != 5) return;

        try {
            String recvTimeStr = parts[2];
            int latency = Integer.parseInt(parts[4]);
            //if (latency < 0) return;
            long epochMillis = LocalDateTime.parse(recvTimeStr, formatter)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli();

            long windowStart = (epochMillis / (windowSizeSeconds * 1000)) * (windowSizeSeconds * 1000);

            buckets.computeIfAbsent(windowStart, Bucket::new).add(latency);
        } catch (Exception e) {
            // log error o ignorar línea inválida
        }
    }

    public List<Result> getResults() {
        List<Result> results = new ArrayList<>();
        for (Bucket bucket : buckets.values()) {
            results.add(bucket.toResult());
        }
        return results;
    }

    public void parseLog()  {
        LatencyWindowAverager averager = new LatencyWindowAverager(60); // ventana de 15s


        try (BufferedReader reader = new BufferedReader(new FileReader("C:\\Users\\fm2871\\Documents\\DocumentFilter\\logs\\delay354.log"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                averager.addLine(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (LatencyWindowAverager.Result result : averager.getResults()) {
            System.out.println(result);
        }

    }

    public static class Stats {
        public final double average;
        public final double averageBelowP95;
        public final double stdDev;
        public final int peakCount;
        public final int maxLatency;
        public final int minLatency;
        public final int p50;
        public final int p95;
        public final int p99;
        public final int p999;
        public final int aboveP95Count;
        public final int totalDataSize;

        public Stats(double average, double averageBelowP95, double stdDev, int peakCount, int maxLatency, int minLatency, int p50, int p95, int p99, int p999, int aboveP95Count, int totalDataSize) {
            this.average = average;
            this.averageBelowP95 = averageBelowP95;
            this.stdDev = stdDev;
            this.peakCount = peakCount;
            this.maxLatency = maxLatency;
            this.minLatency = minLatency;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.p999 = p999;
            this.aboveP95Count = aboveP95Count;
            this.totalDataSize = totalDataSize;
        }
    }



    public List<Integer> getAllLatencies() {
        List<Integer> all = new ArrayList<>();
        for (Bucket b : buckets.values()) {
            all.addAll(b.latencies);
        }
        return all;
    }
    public Stats getStats() {
        List<Integer> values = getAllLatencies();
        if (values.isEmpty()) return new Stats(0, 0,0, 0,0, 0,0, 0, 0, 0, 0,0);

        Collections.sort(values);
        int n = values.size();

        double avg = values.stream().mapToInt(i -> i).average().orElse(0);
        double std = Math.sqrt(values.stream().mapToDouble(i -> Math.pow(i - avg, 2)).average().orElse(0));
        int peakCount = (int) values.stream().filter(i -> i > avg + std).count();
        int max = values.get(n - 1);
        int min = values.get(0);

        int p50= values.get(Math.min((int) Math.ceil(0.5 * n) - 1, n - 1));
        int p95 = values.get(Math.min((int) Math.ceil(0.95 * n) - 1, n - 1));
        int p99 = values.get(Math.min((int) Math.ceil(0.99 * n) - 1, n - 1));
        int p999 = values.get(Math.min((int) Math.ceil(0.999 * n) - 1, n - 1));
        int aboveP95 = (int) values.stream().filter(i -> i > p95).count();
        double averageBelowP95= getAverageBelowP95();
        return new Stats(avg, averageBelowP95,std, peakCount, max, min, p50, p95, p99, p999, aboveP95,n);
    }
    public double getAverageBelowP95() {
        List<Integer> values = getAllLatencies();
        if (values.isEmpty()) return 0.0;

        Collections.sort(values);
        int n = values.size();
        int p95Index = Math.min((int) Math.ceil(0.95 * n) - 1, n - 1);
        int p95Value = values.get(p95Index);

        List<Integer> filtered = values.stream()
                .filter(v -> v <= p95Value)
                .collect(Collectors.toList());

        return filtered.stream()
                .mapToInt(i -> i)
                .average()
                .orElse(0.0);
    }


}
