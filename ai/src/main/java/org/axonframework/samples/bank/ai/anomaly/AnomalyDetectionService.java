package org.axonframework.samples.bank.ai.anomaly;

/**
 * Strategy interface for statistical anomaly detection on transactions.
 */
public interface AnomalyDetectionService {

    /**
     * Analyze a transaction amount against historical statistics.
     *
     * @param accountId the account being analyzed
     * @param tenantId  the tenant
     * @param amount    the transaction amount
     * @return the analysis result (null if no anomaly detected)
     */
    AnomalyAlert analyze(String accountId, String tenantId, long amount);
}
