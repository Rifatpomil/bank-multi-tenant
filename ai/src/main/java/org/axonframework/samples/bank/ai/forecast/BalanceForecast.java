package org.axonframework.samples.bank.ai.forecast;

import java.util.Collections;
import java.util.List;

/**
 * Forecast result containing predicted future balance data points.
 */
public class BalanceForecast {

    private final String accountId;
    private final List<ForecastPoint> predictions;
    private final String methodology;

    public BalanceForecast(String accountId, List<ForecastPoint> predictions, String methodology) {
        this.accountId = accountId;
        this.predictions = Collections.unmodifiableList(predictions);
        this.methodology = methodology;
    }

    public String getAccountId() { return accountId; }
    public List<ForecastPoint> getPredictions() { return predictions; }
    public String getMethodology() { return methodology; }
}
