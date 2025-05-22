package latency;

import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class LatencyComparator {

    public static void compareFiles(String file1, String file2, int windowSizeSeconds) {
        LatencyWindowAverager avg1 = new LatencyWindowAverager(windowSizeSeconds);
        LatencyWindowAverager avg2 = new LatencyWindowAverager(windowSizeSeconds);

        try (BufferedReader r1 = new BufferedReader(new FileReader(file1))) {
            String line;
            while ((line = r1.readLine()) != null) {
                avg1.addLine(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader r2 = new BufferedReader(new FileReader(file2))) {
            String line;
            while ((line = r2.readLine()) != null) {
                avg2.addLine(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<Long, LatencyWindowAverager.Result> map1 = avg1.getResults().stream()
                .collect(Collectors.toMap(r -> r.windowStart.toEpochMilli(), r -> r));

        Map<Long, LatencyWindowAverager.Result> map2 = avg2.getResults().stream()
                .collect(Collectors.toMap(r -> r.windowStart.toEpochMilli(), r -> r));

        // Ventanas comunes
        Set<Long> commonWindows = new TreeSet<>(map1.keySet());
        commonWindows.retainAll(map2.keySet());

        String name1 = new File(file1).getName();
        String name2 = new File(file2).getName();

        System.out.printf("%-30s %-25s %-25s %-20s%n", "Window Start", name1 + " (ms)", name2 + " (ms)", "Diff (ms)");

        double totalLatency1 = 0.0;
        double totalLatency2 = 0.0;
        int commonCount = 0;

        for (Long window : commonWindows) {
            LatencyWindowAverager.Result r1 = map1.get(window);
            LatencyWindowAverager.Result r2 = map2.get(window);

            double v1 = r1.averageLatency;
            double v2 = r2.averageLatency;
            double diff = v1 - v2;

            ZonedDateTime localTime = r1.windowStart.atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
            String timestamp = localTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            System.out.printf("%-30s %-25.2f %-25.2f %-20.2f%n", timestamp, v1, v2, diff);

            totalLatency1 += v1;
            totalLatency2 += v2;
            commonCount++;
        }

        if (commonCount == 0) {
            System.out.println("\n⚠️ No hay ventanas comunes entre los dos archivos.");
            return;
        }

        double avg1d = totalLatency1 / commonCount;
        double avg2d = totalLatency2 / commonCount;

        System.out.println("\nResumen de latencia promedio en ventanas comunes:");
        System.out.printf("→ %s: %.2f ms%n", name1, avg1d);
        System.out.printf("→ %s: %.2f ms%n", name2, avg2d);

        if (avg1d < avg2d) {
            System.out.printf("\n✅ %s tuvo menor latencia promedio.\n", name1);
        } else if (avg2d < avg1d) {
            System.out.printf("\n✅ %s tuvo menor latencia promedio.\n", name2);
        } else {
            System.out.println("\n⚖️ Ambos archivos tienen la misma latencia promedio.");
        }
        LatencyWindowAverager.Stats stats1 = avg1.getStats();
        LatencyWindowAverager.Stats stats2 = avg2.getStats();

        System.out.println("\n📊 Estadísticas adicionales:");

        System.out.printf("→ %s:\n", name1);
        showMetrics(stats1);

        System.out.printf("\n→ %s:\n", name2);
        showMetrics(stats2);

    }

    private static void showMetrics(LatencyWindowAverager.Stats stats1) {
        System.out.printf("   Promedio: %.2f ms\n", stats1.average);
        System.out.printf("   Desviación estándar: %.2f ms\n", stats1.stdDev);
        System.out.printf("   Máxima latencia: %d ms\n", stats1.maxLatency);
        System.out.printf("   Mínima latencia: %d ms\n", stats1.minLatency);
        System.out.printf("   Picos (> μ+σ): %d\n", stats1.peakCount);
        System.out.printf("   Percentil 95: %d ms\n", stats1.p95);
        System.out.printf("   Percentil 99: %d ms\n", stats1.p99);
        System.out.printf("   Cantidad > P95: %d\n", stats1.aboveP95Count);
    }
}

