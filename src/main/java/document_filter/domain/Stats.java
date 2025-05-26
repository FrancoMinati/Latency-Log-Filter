package document_filter.domain;

public class Stats {
    public final double average;
    public final double averageBelowP95;
    public final double stdDev;
    public final int maxLatency;
    public final int minLatency;
    public final int p50;
    public final int p95;
    public final int p99;
    public final int p999;
    public final int aboveP95Count;
    public final int totalDataSize;

    public Stats(double average, double averageBelowP95, double stdDev, int maxLatency, int minLatency, int p50, int p95, int p99, int p999, int aboveP95Count, int totalDataSize) {
        this.average = average;
        this.averageBelowP95 = averageBelowP95;
        this.stdDev = stdDev;
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