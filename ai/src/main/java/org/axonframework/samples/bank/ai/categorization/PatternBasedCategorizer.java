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

package org.axonframework.samples.bank.ai.categorization;

import org.axonframework.samples.bank.ai.TransactionContext;
import org.axonframework.samples.bank.ai.TransactionType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Heuristic-based transaction categorizer.
 * Uses amount ranges, transaction types, and time patterns to assign categories.
 * Designed to be supplemented or replaced by an ML/LLM-based implementation.
 */
@Service
public class PatternBasedCategorizer implements CategorizationService {

    private static final long MICRO_THRESHOLD = 100;       // <= 1.00 in cents
    private static final long HIGH_VALUE_THRESHOLD = 50_000; // >= 500.00 in cents
    private static final long PAYROLL_MIN = 100_000;        // >= 1000.00 in cents
    private static final long PAYROLL_MAX = 1_000_000;      // <= 10000.00 in cents
    private static final long SUBSCRIPTION_MIN = 500;       // >= 5.00
    private static final long SUBSCRIPTION_MAX = 5_000;     // <= 50.00

    @Override
    public CategoryResult categorize(TransactionContext context) {
        List<String> rules = new ArrayList<>();
        TransactionCategory category = TransactionCategory.UNKNOWN;
        double confidence = 0.5;

        // Rule: Micro-transactions
        if (context.getAmount() <= MICRO_THRESHOLD) {
            category = TransactionCategory.MICRO_TRANSACTION;
            rules.add("Amount <= " + MICRO_THRESHOLD + " cents");
            confidence = 0.8;
        }
        // Rule: High-value transactions
        else if (context.getAmount() >= HIGH_VALUE_THRESHOLD) {
            category = TransactionCategory.HIGH_VALUE;
            rules.add("Amount >= " + HIGH_VALUE_THRESHOLD + " cents");
            confidence = 0.85;
        }
        // Rule: Deposits in payroll range
        else if (context.getType() == TransactionType.DEPOSIT
                 && context.getAmount() >= PAYROLL_MIN
                 && context.getAmount() <= PAYROLL_MAX) {
            category = TransactionCategory.PAYROLL;
            rules.add("Deposit in payroll range (" + PAYROLL_MIN + "-" + PAYROLL_MAX + " cents)");
            confidence = 0.6;
        }
        // Rule: Small recurring withdrawals → subscription
        else if (context.getType() == TransactionType.WITHDRAWAL
                 && context.getAmount() >= SUBSCRIPTION_MIN
                 && context.getAmount() <= SUBSCRIPTION_MAX) {
            category = TransactionCategory.SUBSCRIPTION;
            rules.add("Withdrawal in subscription range (" + SUBSCRIPTION_MIN + "-" + SUBSCRIPTION_MAX + " cents)");
            confidence = 0.5;
        }
        // Rule: Transfers
        else if (context.getType() == TransactionType.TRANSFER_OUT
                 || context.getType() == TransactionType.TRANSFER_IN) {
            category = TransactionCategory.TRANSFER;
            rules.add("Transaction type is transfer");
            confidence = 0.95;
        }
        // Rule: Deposits
        else if (context.getType() == TransactionType.DEPOSIT) {
            category = TransactionCategory.DEPOSIT;
            rules.add("Standard deposit");
            confidence = 0.7;
        }
        // Rule: Refunds
        else if (context.getType() == TransactionType.REFUND) {
            category = TransactionCategory.REFUND;
            rules.add("Transaction type is refund");
            confidence = 0.9;
        }
        // Rule: Cash withdrawal
        else if (context.getType() == TransactionType.WITHDRAWAL) {
            category = TransactionCategory.CASH_WITHDRAWAL;
            rules.add("Standard withdrawal");
            confidence = 0.7;
        }

        if (rules.isEmpty()) {
            rules.add("No matching pattern");
        }

        return new CategoryResult(category, confidence, rules);
    }
}
