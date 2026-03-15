package org.axonframework.samples.bank.ai.anomaly;

import java.util.List;
import java.util.UUID;

/**
 * Alert raised when a transaction is flagged as statistically anomalous.
 */
public class AnomalyAlert {

    private final String alertId;
    private final String accountId;
    private final String tenantId;
    private final String severity;
    private final double zScore;
    private final double expectedMean;
    private final double standardDeviation;
    private final long actualAmount;
    private final long timestampMillis;

    public AnomalyAlert(String accountId, String tenantId, String severity,
                         double zScore, double expectedMean, double standardDeviation,
                         long actualAmount) {
        this.alertId = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.severity = severity;
        this.zScore = zScore;
        this.expectedMean = expectedMean;
        this.standardDeviation = standardDeviation;
        this.actualAmount = actualAmount;
        this.timestampMillis = System.currentTimeMillis();
    }

    public String getAlertId() { return alertId; }
    public String getAccountId() { return accountId; }
    public String getTenantId() { return tenantId; }
    public String getSeverity() { return severity; }
    public double getZScore() { return zScore; }
    public double getExpectedMean() { return expectedMean; }
    public double getStandardDeviation() { return standardDeviation; }
    public long getActualAmount() { return actualAmount; }
    public long getTimestampMillis() { return timestampMillis; }
}
