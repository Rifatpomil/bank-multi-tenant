package org.axonframework.samples.bank.ai.anomaly;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Z-score based anomaly detector using Welford's online algorithm.
 * Maintains per-account running statistics and flags transactions
 * that deviate significantly from the account's historical mean.
 *
 * Severity: |z| > 3σ = SEVERE, |z| > 2σ = MODERATE
 */
@Service
public class StatisticalAnomalyDetector implements AnomalyDetectionService {

    private static final double MODERATE_THRESHOLD = 2.0;
    private static final double SEVERE_THRESHOLD = 3.0;
    private static final int MIN_OBSERVATIONS = 5;

    private final ConcurrentHashMap<String, AccountStatistics> accountStats = new ConcurrentHashMap<>();

    @Override
    public AnomalyAlert analyze(String accountId, String tenantId, long amount) {
        String key = tenantId + ":" + accountId;
        AccountStatistics stats = accountStats.computeIfAbsent(key, k -> new AccountStatistics());

        // Always update statistics with the new observation
        stats.update(amount);

        // Need minimum observations before flagging anomalies
        if (stats.getCount() < MIN_OBSERVATIONS) {
            return null;
        }

        double z = stats.zScore(amount);
        double absZ = Math.abs(z);

        if (absZ >= SEVERE_THRESHOLD) {
            return new AnomalyAlert(accountId, tenantId, "SEVERE",
                z, stats.getMean(), stats.getStandardDeviation(), amount);
        } else if (absZ >= MODERATE_THRESHOLD) {
            return new AnomalyAlert(accountId, tenantId, "MODERATE",
                z, stats.getMean(), stats.getStandardDeviation(), amount);
        }

        return null; // within normal range
    }
}
