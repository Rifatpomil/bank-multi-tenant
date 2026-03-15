/*
 * Copyright (c) 2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.samples.bank.ai.fraud;

import org.axonframework.samples.bank.ai.TransactionContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rule-based fraud detection engine.
 * Evaluates configurable heuristic rules against each transaction:
 * <ul>
 *   <li>Velocity check: excessive transactions in a short window</li>
 *   <li>Amount threshold: unusually large single transactions</li>
 *   <li>Off-hours activity: transactions during suspicious hours</li>
 *   <li>Rapid drain: large percentage of balance withdrawn at once</li>
 * </ul>
 * Each rule can independently contribute to the overall risk score.
 */
@Service
public class RuleBasedFraudDetector implements FraudDetectionService {

    private static final long ONE_MINUTE_MILLIS = 60_000L;
    private static final int VELOCITY_THRESHOLD = 5;
    private static final long HIGH_AMOUNT_THRESHOLD = 10_000;
    private static final long CRITICAL_AMOUNT_THRESHOLD = 50_000;
    private static final double RAPID_DRAIN_RATIO = 0.80;
    private static final int OFF_HOURS_START = 1; // 1 AM
    private static final int OFF_HOURS_END = 5;   // 5 AM

    private final TransactionVelocityTracker velocityTracker;

    public RuleBasedFraudDetector() {
        this.velocityTracker = new TransactionVelocityTracker(ONE_MINUTE_MILLIS);
    }

    // Visible for testing
    RuleBasedFraudDetector(TransactionVelocityTracker velocityTracker) {
        this.velocityTracker = velocityTracker;
    }

    @Override
    public FraudAssessment assess(TransactionContext context) {
        List<String> reasons = new ArrayList<>();
        RiskLevel maxRisk = RiskLevel.LOW;

        // Rule 1: Velocity check
        int txCount = velocityTracker.recordAndCount(context.getAccountId());
        if (txCount > VELOCITY_THRESHOLD) {
            reasons.add("High transaction velocity: " + txCount + " transactions in the last minute");
            maxRisk = elevate(maxRisk, RiskLevel.HIGH);
        }

        // Rule 2: Amount thresholds
        if (context.getAmount() > CRITICAL_AMOUNT_THRESHOLD) {
            reasons.add("Critical amount threshold exceeded: " + context.getAmount() + " cents");
            maxRisk = elevate(maxRisk, RiskLevel.CRITICAL);
        } else if (context.getAmount() > HIGH_AMOUNT_THRESHOLD) {
            reasons.add("High amount threshold exceeded: " + context.getAmount() + " cents");
            maxRisk = elevate(maxRisk, RiskLevel.MEDIUM);
        }

        // Rule 3: Off-hours activity
        int hour = Instant.ofEpochMilli(context.getTimestampMillis())
            .atZone(ZoneId.systemDefault()).getHour();
        if (hour >= OFF_HOURS_START && hour < OFF_HOURS_END) {
            reasons.add("Transaction during off-hours (between " + OFF_HOURS_START
                + " AM and " + OFF_HOURS_END + " AM)");
            maxRisk = elevate(maxRisk, RiskLevel.MEDIUM);
        }

        // Rule 4: Rapid drain (only for withdrawals/transfers out)
        if (context.getCurrentBalance() > 0 && context.getAmount() > 0) {
            double ratio = (double) context.getAmount() / context.getCurrentBalance();
            if (ratio >= RAPID_DRAIN_RATIO) {
                reasons.add(String.format("Rapid drain detected: %.0f%% of balance in one transaction",
                    ratio * 100));
                maxRisk = elevate(maxRisk, RiskLevel.HIGH);
            }
        }

        if (reasons.isEmpty()) {
            return new FraudAssessment(RiskLevel.LOW, Collections.singletonList("No suspicious patterns detected"));
        }

        return new FraudAssessment(maxRisk, reasons);
    }

    private static RiskLevel elevate(RiskLevel current, RiskLevel candidate) {
        return candidate.ordinal() > current.ordinal() ? candidate : current;
    }
}
