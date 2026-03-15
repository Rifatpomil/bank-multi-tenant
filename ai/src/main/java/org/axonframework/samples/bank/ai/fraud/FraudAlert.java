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

import java.util.List;
import java.util.UUID;

/**
 * Immutable alert raised when a fraud assessment flags suspicious activity.
 * Pushed to clients via WebSocket in real time.
 */
public class FraudAlert {

    private final String alertId;
    private final String accountId;
    private final String tenantId;
    private final RiskLevel riskLevel;
    private final List<String> reasons;
    private final long timestampMillis;
    private final long transactionAmount;
    private final String transactionType;

    public FraudAlert(String accountId, String tenantId, RiskLevel riskLevel,
                      List<String> reasons, long transactionAmount, String transactionType) {
        this.alertId = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.riskLevel = riskLevel;
        this.reasons = reasons;
        this.timestampMillis = System.currentTimeMillis();
        this.transactionAmount = transactionAmount;
        this.transactionType = transactionType;
    }

    public String getAlertId() { return alertId; }
    public String getAccountId() { return accountId; }
    public String getTenantId() { return tenantId; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public List<String> getReasons() { return reasons; }
    public long getTimestampMillis() { return timestampMillis; }
    public long getTransactionAmount() { return transactionAmount; }
    public String getTransactionType() { return transactionType; }
}
