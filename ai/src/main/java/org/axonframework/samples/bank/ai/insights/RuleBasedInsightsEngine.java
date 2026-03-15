package org.axonframework.samples.bank.ai.insights;

import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryEntry;
import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Rule-based financial insights engine.
 * Pattern-matches natural language questions and queries the AccountDailySummary
 * read model to produce structured answers.
 *
 * Supported question patterns:
 * - "spending trend" / "trend" → analyzes daily totals for trend direction
 * - "total deposits" / "how much deposited" → sums deposit totals
 * - "total withdrawals" / "how much withdrawn" → sums withdrawal totals
 * - "balance" / "current balance" → returns latest closing balance
 * - "summary" / "overview" → gives a full account summary
 */
@Service
public class RuleBasedInsightsEngine implements FinancialInsightsService {

    private static final Pattern TREND_PATTERN = Pattern.compile(
        "(?i).*(spending.*trend|trend|spending.*pattern|how.*spending).*");
    private static final Pattern DEPOSIT_PATTERN = Pattern.compile(
        "(?i).*(total.*deposit|how.*much.*deposit|deposit.*total|income).*");
    private static final Pattern WITHDRAWAL_PATTERN = Pattern.compile(
        "(?i).*(total.*withdraw|how.*much.*withdraw|withdraw.*total|spent|expenditure).*");
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
        "(?i).*(balance|how.*much.*have|account.*value).*");
    private static final Pattern SUMMARY_PATTERN = Pattern.compile(
        "(?i).*(summary|overview|report|snapshot).*");

    private final AccountDailySummaryRepository summaryRepository;

    public RuleBasedInsightsEngine(AccountDailySummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    @Override
    public InsightResponse answer(String tenantId, String accountId, String question) {
        // Fetch recent daily summaries (last 30 days worth)
        List<AccountDailySummaryEntry> summaries = summaryRepository
            .findByTenantIdAndAxonBankAccountIdOrderBySummaryDateDesc(
                tenantId, accountId, new PageRequest(0, 30));

        if (summaries.isEmpty()) {
            return new InsightResponse(
                "No transaction history found for this account.",
                Collections.emptyList(), 0.3,
                Arrays.asList("Try making some transactions first.",
                    "Check the account ID and tenant."));
        }

        if (TREND_PATTERN.matcher(question).matches()) {
            return analyzeTrend(summaries);
        } else if (DEPOSIT_PATTERN.matcher(question).matches()) {
            return analyzeDeposits(summaries);
        } else if (WITHDRAWAL_PATTERN.matcher(question).matches()) {
            return analyzeWithdrawals(summaries);
        } else if (BALANCE_PATTERN.matcher(question).matches()) {
            return analyzeBalance(summaries);
        } else if (SUMMARY_PATTERN.matcher(question).matches()) {
            return generateSummary(summaries);
        }

        return generateSummary(summaries);
    }

    private InsightResponse analyzeTrend(List<AccountDailySummaryEntry> summaries) {
        long totalIn = 0, totalOut = 0;
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        for (AccountDailySummaryEntry s : summaries) {
            totalIn += s.getTotalDeposits();
            totalOut += s.getTotalWithdrawals();
            Map<String, Object> dp = new LinkedHashMap<>();
            dp.put("date", s.getSummaryDate().toString());
            dp.put("deposits", s.getTotalDeposits());
            dp.put("withdrawals", s.getTotalWithdrawals());
            dp.put("closingBalance", s.getClosingBalance());
            dataPoints.add(dp);
        }

        String trend;
        if (totalIn > totalOut) {
            trend = "Your account shows a positive trend — deposits (" + totalIn
                + " cents) exceed withdrawals (" + totalOut + " cents) over the last "
                + summaries.size() + " days.";
        } else if (totalOut > totalIn) {
            trend = "Your account shows a declining trend — withdrawals (" + totalOut
                + " cents) exceed deposits (" + totalIn + " cents) over the last "
                + summaries.size() + " days.";
        } else {
            trend = "Your account is stable — deposits and withdrawals are balanced over the last "
                + summaries.size() + " days.";
        }

        return new InsightResponse(trend, dataPoints, 0.8,
            Arrays.asList("What is my current balance?", "Show my deposit total."));
    }

    private InsightResponse analyzeDeposits(List<AccountDailySummaryEntry> summaries) {
        long total = 0;
        for (AccountDailySummaryEntry s : summaries) { total += s.getTotalDeposits(); }
        return new InsightResponse(
            "Total deposits over the last " + summaries.size() + " days: " + total + " cents.",
            Collections.emptyList(), 0.9,
            Arrays.asList("What is my spending trend?", "What is my total withdrawal?"));
    }

    private InsightResponse analyzeWithdrawals(List<AccountDailySummaryEntry> summaries) {
        long total = 0;
        for (AccountDailySummaryEntry s : summaries) { total += s.getTotalWithdrawals(); }
        return new InsightResponse(
            "Total withdrawals over the last " + summaries.size() + " days: " + total + " cents.",
            Collections.emptyList(), 0.9,
            Arrays.asList("What is my spending trend?", "What is my total deposit?"));
    }

    private InsightResponse analyzeBalance(List<AccountDailySummaryEntry> summaries) {
        AccountDailySummaryEntry latest = summaries.get(0);
        return new InsightResponse(
            "Latest closing balance: " + latest.getClosingBalance() + " cents (as of "
                + latest.getSummaryDate() + ").",
            Collections.emptyList(), 0.95,
            Arrays.asList("What is my spending trend?", "Give me a summary."));
    }

    private InsightResponse generateSummary(List<AccountDailySummaryEntry> summaries) {
        long totalIn = 0, totalOut = 0;
        for (AccountDailySummaryEntry s : summaries) {
            totalIn += s.getTotalDeposits();
            totalOut += s.getTotalWithdrawals();
        }
        AccountDailySummaryEntry latest = summaries.get(0);
        String answer = String.format(
            "Account summary (last %d days): Total deposits: %d cents, Total withdrawals: %d cents, "
                + "Latest closing balance: %d cents (as of %s).",
            summaries.size(), totalIn, totalOut, latest.getClosingBalance(), latest.getSummaryDate());

        return new InsightResponse(answer, Collections.emptyList(), 0.85,
            Arrays.asList("What is my spending trend?", "Show me a forecast."));
    }
}
