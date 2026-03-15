package org.axonframework.samples.bank.ai.anomaly;

/**
 * Welford's online algorithm for computing running mean and standard deviation.
 * Thread-safe via synchronization. Designed for per-account statistics tracking.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Welford's_online_algorithm">Welford's Algorithm</a>
 */
public class AccountStatistics {

    private long count = 0;
    private double mean = 0.0;
    private double m2 = 0.0;

    /**
     * Update statistics with a new observation.
     */
    public synchronized void update(double value) {
        count++;
        double delta = value - mean;
        mean += delta / count;
        double delta2 = value - mean;
        m2 += delta * delta2;
    }

    public synchronized long getCount() { return count; }
    public synchronized double getMean() { return mean; }

    /**
     * Get the population standard deviation.
     * Returns 0.0 if fewer than 2 observations.
     */
    public synchronized double getStandardDeviation() {
        if (count < 2) return 0.0;
        return Math.sqrt(m2 / count);
    }

    /**
     * Calculate the z-score for a given value.
     * Returns 0.0 if standard deviation is zero (not enough data).
     */
    public synchronized double zScore(double value) {
        double sd = getStandardDeviation();
        if (sd == 0.0) return 0.0;
        return (value - mean) / sd;
    }
}
