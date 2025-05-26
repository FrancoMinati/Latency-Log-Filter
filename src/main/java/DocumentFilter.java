import latency.LatencyExcelExporter;

public class DocumentFilter {

    public static void main(String[] args) {
        System.out.println("Procesando archivos de latencia...");

        String inputFolder = "C:\\Users\\fm2871\\Documents\\DocumentFilter\\logs";
        String outputExcel = "C:\\Users\\fm2871\\Documents\\DocumentFilter\\latency-summary.xlsx";
        int windowSeconds = 1; // o el valor que desees

        LatencyExcelExporter.processDirectory(inputFolder, windowSeconds, outputExcel);
    }
}
