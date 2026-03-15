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

package org.axonframework.samples.bank.query.bankaccount;

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.samples.bank.api.bankaccount.BankAccountCreatedEvent;
import org.axonframework.samples.bank.api.bankaccount.MoneyAddedEvent;
import org.axonframework.samples.bank.api.bankaccount.MoneySubtractedEvent;
import org.axonframework.samples.bank.tenant.TenantConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
public class BankAccountEventListener {

    private BankAccountRepository repository;
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    public BankAccountEventListener(BankAccountRepository repository, SimpMessageSendingOperations messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    @EventHandler
    public void on(BankAccountCreatedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        String tenant = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
        repository.save(new BankAccountEntry(tenant, event.getId(), 0, event.getOverdraftLimit()));
        broadcastUpdates(tenant);
    }

    @EventHandler
    public void on(MoneyAddedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        String tenant = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
        BankAccountEntry bankAccountEntry = repository.findOneByTenantIdAndAxonBankAccountId(tenant, event.getBankAccountId());
        if (bankAccountEntry != null) {
            bankAccountEntry.setBalance(bankAccountEntry.getBalance() + event.getAmount());
            repository.save(bankAccountEntry);
        }
        broadcastUpdates(tenant);
    }

    @EventHandler
    public void on(MoneySubtractedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        String tenant = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
        BankAccountEntry bankAccountEntry = repository.findOneByTenantIdAndAxonBankAccountId(tenant, event.getBankAccountId());
        if (bankAccountEntry != null) {
            bankAccountEntry.setBalance(bankAccountEntry.getBalance() - event.getAmount());
            repository.save(bankAccountEntry);
        }
        broadcastUpdates(tenant);
    }

    private void broadcastUpdates(String tenantId) {
        Iterable<BankAccountEntry> bankAccountEntries = repository.findAllByTenantIdOrderByIdAsc(tenantId);
        messagingTemplate.convertAndSend("/topic/bank-accounts.updates." + tenantId, bankAccountEntries);
        if (TenantConstants.DEFAULT_TENANT.equals(tenantId)) {
            messagingTemplate.convertAndSend("/topic/bank-accounts.updates", bankAccountEntries);
        }
    }
}
