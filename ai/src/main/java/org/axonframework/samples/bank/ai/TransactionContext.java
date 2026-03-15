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

package org.axonframework.samples.bank.ai;

/**
 * Shared context for AI transaction analysis.
 * Captures all relevant details about a single financial event.
 */
public class TransactionContext {

    private final String accountId;
    private final String tenantId;
    private final long amount;
    private final TransactionType type;
    private final long timestampMillis;
    private final long currentBalance;
    private final String description;

    public TransactionContext(String accountId, String tenantId, long amount,
                              TransactionType type, long timestampMillis,
                              long currentBalance, String description) {
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.amount = amount;
        this.type = type;
        this.timestampMillis = timestampMillis;
        this.currentBalance = currentBalance;
        this.description = description;
    }

    public String getAccountId() { return accountId; }
    public String getTenantId() { return tenantId; }
    public long getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public long getTimestampMillis() { return timestampMillis; }
    public long getCurrentBalance() { return currentBalance; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "TransactionContext{accountId='" + accountId + "', amount=" + amount
            + ", type=" + type + ", balance=" + currentBalance + '}';
    }
}
