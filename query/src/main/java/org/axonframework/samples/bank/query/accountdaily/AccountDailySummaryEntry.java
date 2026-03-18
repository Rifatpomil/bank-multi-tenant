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

package org.axonframework.samples.bank.query.accountdaily;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Read model: daily rollup of account activity.
 * One row per (tenant, account, date).
 */
@Entity
@Table(name = "account_daily_summary", indexes = {
    @Index(name = "idx_daily_tenant_account_date", columnList = "tenantId,axonBankAccountId,summaryDate", unique = true),
    @Index(name = "idx_daily_tenant_date", columnList = "tenantId,summaryDate")
})
public class AccountDailySummaryEntry {

    @Id
    @GeneratedValue
    private long id;
    private String tenantId;
    private String axonBankAccountId;
    private LocalDate summaryDate;
    private long totalDeposits;
    private long totalWithdrawals;
    private long closingBalance;

    @SuppressWarnings("unused")
    public AccountDailySummaryEntry() {
    }

    public AccountDailySummaryEntry(String tenantId, String axonBankAccountId, LocalDate summaryDate) {
        this.tenantId = tenantId;
        this.axonBankAccountId = axonBankAccountId;
        this.summaryDate = summaryDate;
        this.totalDeposits = 0;
        this.totalWithdrawals = 0;
        this.closingBalance = 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAxonBankAccountId() {
        return axonBankAccountId;
    }

    public void setAxonBankAccountId(String axonBankAccountId) {
        this.axonBankAccountId = axonBankAccountId;
    }

    public LocalDate getSummaryDate() {
        return summaryDate;
    }

    public void setSummaryDate(LocalDate summaryDate) {
        this.summaryDate = summaryDate;
    }

    public long getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(long totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public long getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(long totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public long getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(long closingBalance) {
        this.closingBalance = closingBalance;
    }
}
