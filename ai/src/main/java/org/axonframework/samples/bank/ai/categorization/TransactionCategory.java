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

/**
 * Categorization of financial transactions.
 */
public enum TransactionCategory {
    PAYROLL("Payroll"),
    SUBSCRIPTION("Subscription"),
    UTILITIES("Utilities"),
    TRANSFER("Transfer"),
    CASH_WITHDRAWAL("Cash Withdrawal"),
    DEPOSIT("Deposit"),
    REFUND("Refund"),
    HIGH_VALUE("High Value"),
    MICRO_TRANSACTION("Micro Transaction"),
    UNKNOWN("Unknown");

    private final String displayName;

    TransactionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
