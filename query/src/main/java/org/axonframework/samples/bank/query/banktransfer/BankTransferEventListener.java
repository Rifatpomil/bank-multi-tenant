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

package org.axonframework.samples.bank.query.banktransfer;

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.samples.bank.api.banktransfer.BankTransferCompletedEvent;
import org.axonframework.samples.bank.api.banktransfer.BankTransferCreatedEvent;
import org.axonframework.samples.bank.api.banktransfer.BankTransferFailedEvent;
import org.axonframework.samples.bank.tenant.TenantConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BankTransferEventListener {

    private BankTransferRepository repository;

    @Autowired
    public BankTransferEventListener(BankTransferRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void on(BankTransferCreatedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        String tenant = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
        repository.save(new BankTransferEntry(tenant, event.getBankTransferId(),
            event.getSourceBankAccountId(), event.getDestinationBankAccountId(), event.getAmount()));
    }

    @EventHandler
    public void on(BankTransferFailedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        String tenant = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
        BankTransferEntry bankTransferEntry = repository.findOneByTenantIdAndAxonBankTransferId(tenant,
            event.getBankTransferId());
        if (bankTransferEntry != null) {
            bankTransferEntry.setStatus(BankTransferEntry.Status.FAILED);
            repository.save(bankTransferEntry);
        }
    }

    @EventHandler
    public void on(BankTransferCompletedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        String tenant = tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
        BankTransferEntry bankTransferEntry = repository.findOneByTenantIdAndAxonBankTransferId(tenant,
            event.getBankTransferId());
        if (bankTransferEntry != null) {
            bankTransferEntry.setStatus(BankTransferEntry.Status.COMPLETED);
            repository.save(bankTransferEntry);
        }
    }
}
