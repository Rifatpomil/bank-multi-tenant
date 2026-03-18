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

package org.axonframework.samples.bank.query;

import org.axonframework.samples.bank.api.bankaccount.BankAccountCreatedEvent;
import org.axonframework.samples.bank.api.bankaccount.MoneyDepositedEvent;
import org.axonframework.samples.bank.api.bankaccount.MoneyWithdrawnEvent;
import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryEntry;
import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryEventListener;
import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that replay produces the same deterministic results:
 * applying the same events in the same order yields identical projection state.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@EntityScan("org.axonframework.samples.bank.query")
public class AccountDailySummaryReplayDeterminismTest {

    private static final String TENANT = "tenant-1";
    private static final String ACCOUNT_ID = "acc-1";
    private static final LocalDate DATE = LocalDate.of(2025, 2, 24);

    @Autowired
    private AccountDailySummaryRepository repository;

    private AccountDailySummaryEventListener listener;

    @Before
    public void setUp() {
        repository.deleteAll();
        listener = new AccountDailySummaryEventListener(repository);
    }

    @Test
    public void replayDeterminism_sameEventsProduceSameState() {
        Instant ts = DATE.atStartOfDay(ZoneId.systemDefault()).toInstant();

        applyEvents(ts);

        List<AccountDailySummaryEntry> firstPass = repository.findByTenantIdAndAxonBankAccountIdOrderBySummaryDateDesc(
                TENANT, ACCOUNT_ID, PageRequest.of(0, 10, Sort.unsorted()));

        repository.deleteAll();
        applyEvents(ts);

        List<AccountDailySummaryEntry> secondPass = repository.findByTenantIdAndAxonBankAccountIdOrderBySummaryDateDesc(
                TENANT, ACCOUNT_ID, PageRequest.of(0, 10, Sort.unsorted()));

        assertThat(firstPass).hasSize(1);
        assertThat(secondPass).hasSize(1);

        AccountDailySummaryEntry a = firstPass.get(0);
        AccountDailySummaryEntry b = secondPass.get(0);

        assertThat(a.getTotalDeposits()).isEqualTo(b.getTotalDeposits());
        assertThat(a.getTotalWithdrawals()).isEqualTo(b.getTotalWithdrawals());
        assertThat(a.getClosingBalance()).isEqualTo(b.getClosingBalance());
    }

    private void applyEvents(Instant ts) {
        listener.on(new BankAccountCreatedEvent(ACCOUNT_ID, 0), TENANT, ts);
        listener.on(new MoneyDepositedEvent(ACCOUNT_ID, 100), TENANT, ts);
        listener.on(new MoneyWithdrawnEvent(ACCOUNT_ID, 30), TENANT, ts);
    }
}
