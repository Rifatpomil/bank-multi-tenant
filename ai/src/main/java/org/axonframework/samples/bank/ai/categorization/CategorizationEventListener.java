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

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.samples.bank.ai.TransactionContext;
import org.axonframework.samples.bank.ai.TransactionType;
import org.axonframework.samples.bank.api.bankaccount.MoneyAddedEvent;
import org.axonframework.samples.bank.api.bankaccount.MoneySubtractedEvent;
import org.axonframework.samples.bank.api.banktransfer.BankTransferCreatedEvent;
import org.axonframework.samples.bank.tenant.TenantConstants;
import org.springframework.stereotype.Component;

/**
 * Listens to financial events and auto-categorizes each transaction,
 * persisting the result for later querying.
 */
@Component
public class CategorizationEventListener {

    private final CategorizationService categorizationService;
    private final CategorizedTransactionRepository repository;

    public CategorizationEventListener(CategorizationService categorizationService,
                                       CategorizedTransactionRepository repository) {
        this.categorizationService = categorizationService;
        this.repository = repository;
    }

    @EventHandler
    public void on(MoneyAddedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        categorizeAndSave(event.getBankAccountId(), resolveTenant(tenantId),
            event.getAmount(), TransactionType.DEPOSIT);
    }

    @EventHandler
    public void on(MoneySubtractedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        categorizeAndSave(event.getBankAccountId(), resolveTenant(tenantId),
            event.getAmount(), TransactionType.WITHDRAWAL);
    }

    @EventHandler
    public void on(BankTransferCreatedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        categorizeAndSave(event.getSourceBankAccountId(), resolveTenant(tenantId),
            event.getAmount(), TransactionType.TRANSFER_OUT);
    }

    private void categorizeAndSave(String accountId, String tenantId, long amount, TransactionType type) {
        TransactionContext ctx = new TransactionContext(
            accountId, tenantId, amount, type, System.currentTimeMillis(), 0, "");

        CategoryResult result = categorizationService.categorize(ctx);

        repository.save(new CategorizedTransactionEntry(
            tenantId, accountId, amount, type.name(),
            result.getCategory(), result.getConfidence(), System.currentTimeMillis()));
    }

    private static String resolveTenant(String tenantId) {
        return tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
    }
}
