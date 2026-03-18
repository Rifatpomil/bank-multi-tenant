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

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * JPA entity storing categorized transactions for querying via REST API.
 */
@Entity
public class CategorizedTransactionEntry {

    @Id
    @GeneratedValue
    private long id;
    private String tenantId;
    private String accountId;
    private long amount;
    private String transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionCategory category;

    private double confidence;
    private long timestampMillis;

    @SuppressWarnings("unused")
    public CategorizedTransactionEntry() {
    }

    public CategorizedTransactionEntry(String tenantId, String accountId, long amount,
                                        String transactionType, TransactionCategory category,
                                        double confidence, long timestampMillis) {
        this.tenantId = tenantId;
        this.accountId = accountId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.category = category;
        this.confidence = confidence;
        this.timestampMillis = timestampMillis;
    }

    public long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getAccountId() { return accountId; }
    public long getAmount() { return amount; }
    public String getTransactionType() { return transactionType; }
    public TransactionCategory getCategory() { return category; }
    public double getConfidence() { return confidence; }
    public long getTimestampMillis() { return timestampMillis; }
}
