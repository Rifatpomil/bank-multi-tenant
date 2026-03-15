package org.axonframework.samples.bank.ai.forecast;

/**
 * Strategy interface for balance forecasting.
 */
public interface ForecastService {

    /**
     * Forecast future account balances.
     *
     * @param tenantId  the tenant
     * @param accountId the account
     * @param daysAhead number of days to forecast
     * @return the forecast result with prediction points
     */
    BalanceForecast forecast(String tenantId, String accountId, int daysAhead);
}
