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
import org.axonframework.samples.bank.api.bankaccount.MoneySubtractedEvent;
import org.axonframework.samples.bank.query.bankaccount.BankAccountEntry;
import org.axonframework.samples.bank.query.bankaccount.BankAccountEventListener;
import org.axonframework.samples.bank.query.bankaccount.BankAccountRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies tenant isolation: events from tenant A must not appear in tenant B's read model.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@EntityScan("org.axonframework.samples.bank.query")
public class TenantIsolationTest {

    @Autowired
    private BankAccountRepository repository;

    private BankAccountEventListener listener;

    @Before
    public void setUp() {
        repository.deleteAll();
        listener = new BankAccountEventListener(repository, mock(SimpMessageSendingOperations.class));
    }

    @Test
    public void tenantIsolation_readModelScopedByTenant() {
        String accountA = "acc-tenant-a";
        String accountB = "acc-tenant-b";

        listener.on(new BankAccountCreatedEvent(accountA, 0), "tenant-a");
        listener.on(new MoneyDepositedEvent(accountA, 100), "tenant-a");

        listener.on(new BankAccountCreatedEvent(accountB, 0), "tenant-b");
        listener.on(new MoneyDepositedEvent(accountB, 200), "tenant-b");

        Iterable<BankAccountEntry> tenantAEntries = repository.findAllByTenantIdOrderByIdAsc("tenant-a");
        Iterable<BankAccountEntry> tenantBEntries = repository.findAllByTenantIdOrderByIdAsc("tenant-b");

        assertThat(tenantAEntries).hasSize(1);
        assertThat(tenantAEntries.iterator().next().getAxonBankAccountId()).isEqualTo(accountA);
        assertThat(tenantAEntries.iterator().next().getBalance()).isEqualTo(100);

        assertThat(tenantBEntries).hasSize(1);
        assertThat(tenantBEntries.iterator().next().getAxonBankAccountId()).isEqualTo(accountB);
        assertThat(tenantBEntries.iterator().next().getBalance()).isEqualTo(200);

        assertThat(repository.findOneByTenantIdAndAxonBankAccountId("tenant-a", accountB)).isNull();
        assertThat(repository.findOneByTenantIdAndAxonBankAccountId("tenant-b", accountA)).isNull();
    }
}
