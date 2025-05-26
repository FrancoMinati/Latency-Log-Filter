package document_filter.latency;

import document_filter.domain.Stats;
import document_filter.domain.Window;
import document_filter.domain.WindowResult;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LatencyWindowAverager {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS");
    private final int windowSizeSeconds;
    private final ZoneId zoneId;
    private final Map<Long, Window> windows = new TreeMap<>();

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
            //if (document_filter.latency < 0) return;
            long epochMillis = LocalDateTime.parse(recvTimeStr, formatter)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli();

            long windowStart = (epochMillis / (windowSizeSeconds * 1000L)) * (windowSizeSeconds * 1000L);

            windows.computeIfAbsent(windowStart, Window::new).add(latency);
        } catch (Exception e) {
            // log error o ignorar línea inválida
        }
    }

    public List<WindowResult> getResults() {
        List<WindowResult> results = new ArrayList<>();
        for (Window window : windows.values()) {
            results.add(window.toResult());
        }
        return results;
    }

    public List<Integer> getAllLatencies() {
        List<Integer> all = new ArrayList<>();
        for (Window b : windows.values()) {
            all.addAll(b.getLatencies());
        }
        return all;
    }
    public Stats getStats() {
        List<Integer> values = getAllLatencies();
        if (values.isEmpty()) return new Stats(0, 0,0,0, 0, 0,0, 0, 0, 0, 0,0);

        Collections.sort(values);
        int n = values.size();

        double avg = values.stream().mapToInt(i -> i).average().orElse(0);
        double std = Math.sqrt(values.stream().mapToDouble(i -> Math.pow(i - avg, 2)).average().orElse(0));
        int max = values.get(n - 1);
        int min = values.getFirst();

        int p50= values.get(Math.min((int) Math.ceil(0.5 * n) - 1, n - 1));
        int p95 = values.get(Math.min((int) Math.ceil(0.95 * n) - 1, n - 1));
        int p99 = values.get(Math.min((int) Math.ceil(0.99 * n) - 1, n - 1));
        int p999 = values.get(Math.min((int) Math.ceil(0.999 * n) - 1, n - 1));
        int aboveP95 = (int) values.stream().filter(i -> i > p95).count();
        double averageUpperP95= getAverageBelowP95(values,p95);
        double averageBelowP95= getAverageBelowP95(values,p95);
        return new Stats(avg, averageBelowP95,averageUpperP95,std, max, min, p50, p95, p99, p999, aboveP95,n);
    }

    private static double getAverageBelowP95(List<Integer> values,int p95) {
        List<Integer> filtered = values.stream()
                .filter(v -> v <= p95)
                .toList();
        return filtered.stream()
                .mapToInt(i -> i)
                .average()
                .orElse(0.0);
    }
    private static double getAverageUpperP95(List<Integer> values,int p95) {
        List<Integer> filtered = values.stream()
                .filter(v -> v >= p95)
                .toList();
        return filtered.stream()
                .mapToInt(i -> i)
                .average()
                .orElse(0.0);
    }

    public static double getWindowWeightedAvg(List<WindowResult> results) {
        double weightedSum = results.stream()
                .mapToDouble(r -> r.averageLatency * r.count * r.count)
                .sum();

        double totalWeight = results.stream()
                .mapToDouble(r -> r.count * r.count)
                .sum();

        return totalWeight == 0.0 ? 0.0 : weightedSum / totalWeight;
    }


}
