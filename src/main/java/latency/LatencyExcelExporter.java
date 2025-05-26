package latency;

import domain.Stats;
import domain.WindowResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LatencyExcelExporter {

    public static void processDirectory(String folderPath, int windowSeconds, String outputExcelFile) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Ruta inválida: " + folderPath);
        }

        Map<String, File> nameToFile = new LinkedHashMap<>();
        File[] subdirs = folder.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                File delayLog = new File(subdir, "delay.log");
                if (delayLog.exists()) {
                    nameToFile.put(subdir.getName() + "/delay.log", delayLog);
                }
            }
        }

        if (nameToFile.isEmpty()) {
            System.out.println("No se encontraron archivos delay.log en subdirectorios de la carpeta.");
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet summarySheet = workbook.createSheet("Latency Summary");
            String[] headers = {
                    "Archivo", "Promedio (ms)", "Prom. ventanas (ms)", "Desvío estándar",
                    "Máxima", "Mínima", "Picos (>μ+σ)", "P95", "P99", "P99.9", "Mediana" ,"Cantidad > P95","Tamaño muestra","Prom. Menor P95"
            };

            Row headerRow = summarySheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            double bestAvg = Double.MAX_VALUE;
            int bestRow = -1;

            for (Map.Entry<String, File> entry : nameToFile.entrySet()) {
                String name = entry.getKey();
                File file = entry.getValue();

                LatencyWindowAverager averager = new LatencyWindowAverager(windowSeconds);

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        averager.addLine(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                List<WindowResult> results = averager.getResults();
                Stats stats = averager.getStats();
                double windowAvg = getWindowWeightedAvg(results);

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
                row.createCell(10).setCellValue(stats.p50);
                row.createCell(11).setCellValue(stats.aboveP95Count);
                row.createCell(12).setCellValue(stats.totalDataSize);
                row.createCell(13).setCellValue(stats.averageBelowP95);

                if (stats.average < bestAvg) {
                    bestAvg = stats.average;
                    bestRow = rowNum;
                }

                rowNum++;

                Sheet windowSheet = workbook.createSheet(name.replace("/", "_") + "_ventanas");
                Row header = windowSheet.createRow(0);
                header.createCell(0).setCellValue("Window Start");
                header.createCell(1).setCellValue("Promedio (ms)");
                header.createCell(2).setCellValue("Cantidad");
                header.createCell(3).setCellValue("Ponderado (ms)");

                int r = 1;
                for (WindowResult result : results) {
                    Row rowW = windowSheet.createRow(r++);
                    rowW.createCell(0).setCellValue(result.windowStart.toString());
                    rowW.createCell(1).setCellValue(result.averageLatency);
                    rowW.createCell(2).setCellValue(result.count);
                    rowW.createCell(3).setCellValue(result.averageLatency*result.count*result.count); // genera un ponderado donde cada mensaje pesa n, siendo n la cantidad de mensajes totales durante ese segundo
                }

                for (int i = 0; i < 4; i++) {
                    windowSheet.autoSizeColumn(i);
                }

                System.out.println("✔ Procesado: " + name);
            }

            for (int i = 0; i < headers.length; i++) {
                summarySheet.autoSizeColumn(i);
            }

            if (bestRow > 0) {
                CellStyle greenStyle = workbook.createCellStyle();
                greenStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
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

    private static double getWindowWeightedAvg(List<WindowResult> results) {
        double weightedSum = results.stream()
                .mapToDouble(r -> r.averageLatency * r.count * r.count)
                .sum();

        double totalWeight = results.stream()
                .mapToDouble(r -> r.count * r.count)
                .sum();

        return totalWeight == 0.0 ? 0.0 : weightedSum / totalWeight;
    }
}
