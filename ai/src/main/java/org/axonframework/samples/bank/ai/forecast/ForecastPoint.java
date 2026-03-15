package org.axonframework.samples.bank.ai.forecast;

import java.time.LocalDate;

/**
 * A single point in a balance forecast timeline.
 */
public class ForecastPoint {

    private final LocalDate date;
    private final long predictedBalance;
    private final long lowerBound;
    private final long upperBound;

    public ForecastPoint(LocalDate date, long predictedBalance, long lowerBound, long upperBound) {
        this.date = date;
        this.predictedBalance = predictedBalance;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public LocalDate getDate() { return date; }
    public long getPredictedBalance() { return predictedBalance; }
    public long getLowerBound() { return lowerBound; }
    public long getUpperBound() { return upperBound; }
}
