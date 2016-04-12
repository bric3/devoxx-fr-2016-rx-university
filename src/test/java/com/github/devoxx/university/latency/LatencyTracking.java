package com.github.devoxx.university.latency;

import java.io.PrintStream;
import java.util.concurrent.Callable;
import org.HdrHistogram.Histogram;
import org.LatencyUtils.LatencyStats;

public class LatencyTracking {
    private final LatencyStats latencyStats = new LatencyStats();
    private final Histogram intervalHistogram = latencyStats.getIntervalHistogram();
    private final Histogram accumulatedHistogram = new Histogram(intervalHistogram);

    public <T> T trackLatency(Callable<T> operation) throws Exception {
        long startTime = System.nanoTime();

        try {
            return operation.call();
        } catch (Exception ex) {
            throw ex;
        } finally {
            latencyStats.recordLatency(System.nanoTime() - startTime);
        }
    }

    public void printHistogram(PrintStream out) {
        latencyStats.getIntervalHistogramInto(intervalHistogram);
        accumulatedHistogram.add(intervalHistogram);
        accumulatedHistogram.outputPercentileDistribution(out, 1000000.0);
    }

}
