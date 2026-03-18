package org.axonframework.samples.bank.ai.forecast;

import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryEntry;
import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * EWMA-based (Exponentially Weighted Moving Average) balance forecaster.
 * Queries historical AccountDailySummary data and projects future balances
 * with confidence intervals based on historical variance.
 */
@Service
public class MovingAverageForecaster implements ForecastService {

    private static final double ALPHA = 0.3; // smoothing factor (higher = more weight to recent data)
    private static final int HISTORY_DAYS = 90;

    private final AccountDailySummaryRepository summaryRepository;

    public MovingAverageForecaster(AccountDailySummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    @Override
    public BalanceForecast forecast(String tenantId, String accountId, int daysAhead) {
        List<AccountDailySummaryEntry> history = summaryRepository
            .findByTenantIdAndAxonBankAccountIdOrderBySummaryDateDesc(
                tenantId, accountId, PageRequest.of(0, HISTORY_DAYS));

        if (history.isEmpty()) {
            return new BalanceForecast(accountId, Collections.emptyList(),
                "EWMA (no historical data available)");
        }

        // Reverse to chronological order
        List<AccountDailySummaryEntry> chronological = new ArrayList<>(history);
        Collections.reverse(chronological);

        // Calculate daily net changes
        List<Long> dailyNetChanges = new ArrayList<>();
        for (AccountDailySummaryEntry entry : chronological) {
            dailyNetChanges.add(entry.getTotalDeposits() - entry.getTotalWithdrawals());
        }

        // EWMA of daily net change
        double ewma = dailyNetChanges.get(0);
        for (int i = 1; i < dailyNetChanges.size(); i++) {
            ewma = ALPHA * dailyNetChanges.get(i) + (1 - ALPHA) * ewma;
        }

        // Calculate variance for confidence intervals
        double sumSquaredDiff = 0;
        for (long change : dailyNetChanges) {
            double diff = change - ewma;
            sumSquaredDiff += diff * diff;
        }
        double stdDev = Math.sqrt(sumSquaredDiff / dailyNetChanges.size());

        // Project forward from latest known balance
        long latestBalance = chronological.get(chronological.size() - 1).getClosingBalance();
        LocalDate startDate = chronological.get(chronological.size() - 1).getSummaryDate().plusDays(1);

        List<ForecastPoint> predictions = new ArrayList<>();
        long projectedBalance = latestBalance;

        for (int i = 0; i < daysAhead; i++) {
            projectedBalance += Math.round(ewma);
            long margin = Math.round(stdDev * Math.sqrt(i + 1)); // widening confidence interval
            predictions.add(new ForecastPoint(
                startDate.plusDays(i),
                projectedBalance,
                projectedBalance - margin,
                projectedBalance + margin));
        }

        return new BalanceForecast(accountId, predictions,
            "EWMA (alpha=" + ALPHA + ", based on " + history.size() + " days of history)");
    }
}
