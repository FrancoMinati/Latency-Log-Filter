package latency;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class LatencyExcelExporter {

    public static void processDirectory(String folderPath, int windowSeconds, String outputExcelFile) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Ruta inválida: " + folderPath);
        }

        List<File> logFiles = new ArrayList<>();
        Map<String, File> nameToFile = new LinkedHashMap<>();
        Map<String, LatencyWindowAverager> averagerMap = new HashMap<>();
        File[] subdirs = folder.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                File delayLog = new File(subdir, "delay.log");
                if (delayLog.exists()) {
                    logFiles.add(delayLog);
                    nameToFile.put(subdir.getName() + "/delay.log", delayLog);
                }
            }
        }

        if (logFiles.isEmpty()) {
            System.out.println("No se encontraron archivos delay.log en subdirectorios de la carpeta.");
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet summarySheet = workbook.createSheet("Latency Summary");
            String[] headers = {
                    "Archivo", "Promedio (ms)", "Prom. ventanas (ms)", "Desvío estándar",
                    "Máxima", "Mínima", "Picos (>μ+σ)", "P95", "P99", "P99.9", "Cantidad > P95"
            };

            Row headerRow = summarySheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            double bestAvg = Double.MAX_VALUE;
            int bestRow = -1;

            Map<String, List<LatencyWindowAverager.Result>> allWindowResults = new LinkedHashMap<>();
            Set<Long> commonWindowStartEpochs = null;

            // Leer y procesar archivos
            for (Map.Entry<String, File> entry : nameToFile.entrySet()) {
                String name = entry.getKey();
                File file = entry.getValue();

                LatencyWindowAverager averager = new LatencyWindowAverager(windowSeconds);
                averagerMap.put(name, averager);
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        averager.addLine(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                List<LatencyWindowAverager.Result> results = averager.getResults();
                allWindowResults.put(name, results);

                Set<Long> windowEpochs = results.stream()
                        .map(r -> r.windowStart.toEpochMilli())
                        .collect(Collectors.toSet());

                if (commonWindowStartEpochs == null) {
                    commonWindowStartEpochs = new TreeSet<>(windowEpochs);
                } else {
                    commonWindowStartEpochs.retainAll(windowEpochs);
                }
            }

            if (commonWindowStartEpochs == null || commonWindowStartEpochs.isEmpty()) {
                System.out.println("❌ No hay ventanas comunes entre los archivos.");
                return;
            }

            long minCommonEpoch = Collections.min(commonWindowStartEpochs);
            long maxCommonEpoch = Collections.max(commonWindowStartEpochs);

            // Generar todas las ventanas entre min y max, en pasos de windowSeconds
            List<Long> allWindowEpochs = new ArrayList<>();
            for (long w = minCommonEpoch; w <= maxCommonEpoch; w += windowSeconds * 1000L) {
                allWindowEpochs.add(w);
            }

            for (Map.Entry<String, File> entry : nameToFile.entrySet()) {
                String name = entry.getKey();

                List<LatencyWindowAverager.Result> results = allWindowResults.get(name);
                Map<Long, LatencyWindowAverager.Result> resultMap = new HashMap<>();
                for (LatencyWindowAverager.Result r : results) {
                    resultMap.put(r.windowStart.toEpochMilli(), r);
                }

                // Completar ventanas faltantes con resultados vacíos
                List<LatencyWindowAverager.Result> filledResults = new ArrayList<>();
                for (Long windowStart : allWindowEpochs) {
                    if (resultMap.containsKey(windowStart)) {
                        filledResults.add(resultMap.get(windowStart));
                    } else {
                        filledResults.add(new LatencyWindowAverager.Result(
                                java.time.Instant.ofEpochMilli(windowStart),
                                0.0,
                                0));
                    }
                }

                // Calcular promedio ventana solo sobre ventanas con datos (count > 0)
                double windowAvg = filledResults.stream()
                        .filter(r -> r.count > 0)
                        .mapToDouble(r -> r.averageLatency)
                        .average()
                        .orElse(0.0);

                // Usar el averager para estadísticas totales
                LatencyWindowAverager.Stats stats = averagerMap.get(name).getStats();

                // Escribir fila resumen
                Row row = summarySheet.createRow(rowNum);
                row.createCell(0).setCellValue(name);
                row.createCell(1).setCellValue(stats.average);
                row.createCell(2).setCellValue(windowAvg);
                row.createCell(3).setCellValue(stats.stdDev);
                row.createCell(4).setCellValue(stats.maxLatency);
                row.createCell(5).setCellValue(stats.minLatency);
                row.createCell(6).setCellValue(stats.peakCount);
                row.createCell(7).setCellValue(stats.p95);
                row.createCell(8).setCellValue(stats.p99);
                row.createCell(9).setCellValue(stats.p999);
                row.createCell(10).setCellValue(stats.aboveP95Count);

                if (windowAvg < bestAvg) {
                    bestAvg = windowAvg;
                    bestRow = rowNum;
                }

                rowNum++;

                // Hoja individual con ventanas completas
                Sheet windowSheet = workbook.createSheet(name.replace("/", "_") + "_ventanas");
                Row header = windowSheet.createRow(0);
                header.createCell(0).setCellValue("Window Start");
                header.createCell(1).setCellValue("Promedio (ms)");
                header.createCell(2).setCellValue("Cantidad");

                int r = 1;
                for (LatencyWindowAverager.Result result : filledResults) {
                    Row rowW = windowSheet.createRow(r++);
                    rowW.createCell(0).setCellValue(result.windowStart.toString());
                    rowW.createCell(1).setCellValue(result.averageLatency);
                    rowW.createCell(2).setCellValue(result.count);
                }

                for (int i = 0; i < 3; i++) {
                    windowSheet.autoSizeColumn(i);
                }

                System.out.println("✔ Procesado: " + name);
            }

            for (int i = 0; i < headers.length; i++) {
                summarySheet.autoSizeColumn(i);
            }

            // Marcar la mejor fila en verde
            if (bestRow > 0) {
                CellStyle greenStyle = workbook.createCellStyle();
                greenStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                Row best = summarySheet.getRow(bestRow);
                for (int i = 0; i < headers.length; i++) {
                    best.getCell(i).setCellStyle(greenStyle);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputExcelFile)) {
                workbook.write(fos);
                System.out.println("✅ Exportado a Excel: " + outputExcelFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el Excel", e);
        }
    }
}
