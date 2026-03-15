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

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.samples.bank.api.bankaccount.BankAccountCreatedEvent;
import org.axonframework.samples.bank.api.bankaccount.MoneyAddedEvent;
import org.axonframework.samples.bank.api.bankaccount.MoneySubtractedEvent;
import org.axonframework.samples.bank.tenant.TenantConstants;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Builds AccountDailySummary read model from bank account events.
 * Subscribes to a separate processing group for independent replay.
 */
@Component
@org.axonframework.config.ProcessingGroup("account-daily-summary")
public class AccountDailySummaryEventListener {

    public static final String PROCESSING_GROUP = "account-daily-summary";

    private final AccountDailySummaryRepository repository;

    public AccountDailySummaryEventListener(AccountDailySummaryRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void on(BankAccountCreatedEvent event,
            @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId,
            @Timestamp Instant timestamp) {
        String tenant = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
        LocalDate date = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();
        ensureEntry(tenant, event.getId(), date);
    }

    @EventHandler
    public void on(MoneyAddedEvent event,
            @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId,
            @Timestamp Instant timestamp) {
        String tenant = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
        LocalDate date = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();
        AccountDailySummaryEntry entry = getOrCreate(tenant, event.getBankAccountId(), date);
        entry.setTotalDeposits(entry.getTotalDeposits() + event.getAmount());
        entry.setClosingBalance(entry.getClosingBalance() + event.getAmount());
        repository.save(entry);
    }

    @EventHandler
    public void on(MoneySubtractedEvent event,
            @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId,
            @Timestamp Instant timestamp) {
        String tenant = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
        LocalDate date = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();
        AccountDailySummaryEntry entry = getOrCreate(tenant, event.getBankAccountId(), date);
        entry.setTotalWithdrawals(entry.getTotalWithdrawals() + event.getAmount());
        entry.setClosingBalance(entry.getClosingBalance() - event.getAmount());
        repository.save(entry);
    }

    private void ensureEntry(String tenantId, String accountId, LocalDate date) {
        if (repository.findOneByTenantIdAndAxonBankAccountIdAndSummaryDate(tenantId, accountId, date) == null) {
            repository.save(new AccountDailySummaryEntry(tenantId, accountId, date));
        }
    }

    private AccountDailySummaryEntry getOrCreate(String tenantId, String accountId, LocalDate date) {
        AccountDailySummaryEntry entry = repository.findOneByTenantIdAndAxonBankAccountIdAndSummaryDate(
                tenantId, accountId, date);
        if (entry == null) {
            entry = new AccountDailySummaryEntry(tenantId, accountId, date);
            repository.save(entry);
        }
        return entry;
    }
}
