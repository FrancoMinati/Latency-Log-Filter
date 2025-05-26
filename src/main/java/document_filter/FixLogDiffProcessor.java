package document_filter;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public class FixLogDiffProcessor {

    private static final Pattern logTimePattern = Pattern.compile("\\[(\\d{4}/\\d{2}/\\d{2}-\\d{2}:\\d{2}:\\d{2},\\d{3})\\]");
    private static final Pattern fixTimePattern = Pattern.compile("52=(\\d{8}-\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");

    private static final DateTimeFormatter fixFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS");
    private static final DateTimeFormatter logFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss,SSS");
    private static final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void parseLog() {

        String inputPath = "C:\\Users\\fm2871\\Documents\\DocumentFilter\\cvfixhub.log";
        String outputPath = "resultado_diferencias.csv";

        List<Long> diferencias = new ArrayList<>();
        int procesadas = 0;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputPath));
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath))) {

            writer.write("log_timestamp,fix_timestamp_adjusted,diff_ms");
            writer.newLine();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("|Last message for FIXT.1.1:") || line.contains("|fromAdmin|")) {
                    continue;
                }

                Matcher logMatcher = logTimePattern.matcher(line);
                Matcher fixMatcher = fixTimePattern.matcher(line);

                if (logMatcher.find() && fixMatcher.find()) {
                    String logTimeStr = logMatcher.group(1);
                    String fixTimeStr = fixMatcher.group(1);

                    try {
                        LocalDateTime logTime = LocalDateTime.parse(logTimeStr, logFormatter);
                        if (logTime.toLocalTime().isBefore(LocalTime.of(11, 0))) {
                            continue;
                        }

                        LocalDateTime fixTime = LocalDateTime.parse(fixTimeStr, fixFormatter).minusHours(3);
                        long diffMs = Duration.between(fixTime, logTime).toMillis();

                        diferencias.add(diffMs);
                        procesadas++;

                        writer.write(String.format("%s,%s,%d",
                                logTime.format(outputFormatter),
                                fixTime.format(outputFormatter),
                                diffMs));
                        writer.newLine();

                    } catch (Exception e) {
                        System.err.println("Error procesando línea: " + e.getMessage());
                    }
                }
            }

            System.out.printf("Procesamiento completo. Total líneas válidas (desde 11:00): %d%n", procesadas);

            analizarDiferencias(diferencias);

            System.out.println("Resultados guardados en: " + outputPath);

        } catch (IOException e) {
            System.err.println("Error leyendo o escribiendo archivos: " + e.getMessage());
        }
    }

    private static void analizarDiferencias(List<Long> diferencias) {
        if (diferencias.isEmpty()) {
            System.out.println("No hay diferencias para analizar.");
            return;
        }

        List<Long> ordenadas = new ArrayList<>(diferencias);
        Collections.sort(ordenadas);

        double promedio = diferencias.stream().mapToLong(Long::longValue).average().orElse(0);
        double sumaCuadrados = diferencias.stream()
                .mapToDouble(d -> Math.pow(d - promedio, 2))
                .sum();
        double desviacion = Math.sqrt(sumaCuadrados / diferencias.size());

        long picos = diferencias.stream()
                .filter(d -> Math.abs(d - promedio) > 2 * desviacion)
                .count();

        long min = ordenadas.get(0);
        long max = ordenadas.get(ordenadas.size() - 1);
        long mediana = ordenadas.get(ordenadas.size() / 2);
        long p95 = ordenadas.get((int) (ordenadas.size() * 0.95));
        long p99 = ordenadas.get((int) (ordenadas.size() * 0.99));
        long p999 = ordenadas.get((int) (ordenadas.size() * 0.999));

        System.out.println("=== Estadísticas de diferencias (ms) ===");
        System.out.printf("Total: %d%n", diferencias.size());
        System.out.printf("Promedio: %.2f ms%n", promedio);
        System.out.printf("Mediana: %d ms%n", mediana);
        System.out.printf("Mínimo: %d ms%n", min);
        System.out.printf("Máximo: %d ms%n", max);
        System.out.printf("Desviación estándar: %.2f ms%n", desviacion);
        System.out.printf("Percentil 95 (p95): %d ms%n", p95);
        System.out.printf("Percentil 99 (p99): %d ms%n", p99);
        System.out.printf("Percentil 99.9 (p99.9): %d ms%n", p999);
        System.out.printf("Cantidad de picos (> 2σ): %d (%.2f%%)%n",
                picos, 100.0 * picos / diferencias.size());
    }

}
