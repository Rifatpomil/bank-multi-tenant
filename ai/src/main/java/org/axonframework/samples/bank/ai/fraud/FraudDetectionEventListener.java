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

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.samples.bank.ai.TransactionContext;
import org.axonframework.samples.bank.ai.TransactionType;
import org.axonframework.samples.bank.api.bankaccount.MoneyAddedEvent;
import org.axonframework.samples.bank.api.bankaccount.MoneySubtractedEvent;
import org.axonframework.samples.bank.api.banktransfer.BankTransferCreatedEvent;
import org.axonframework.samples.bank.query.bankaccount.BankAccountEntry;
import org.axonframework.samples.bank.query.bankaccount.BankAccountRepository;
import org.axonframework.samples.bank.tenant.TenantConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Listens to financial events and runs fraud detection in real time.
 * Suspicious transactions trigger alerts pushed to WebSocket clients.
 */
@Component
public class FraudDetectionEventListener {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionEventListener.class);

    private final FraudDetectionService fraudDetectionService;
    private final BankAccountRepository bankAccountRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    /** Recent alerts kept in memory for REST retrieval (bounded to last 100). */
    private final CopyOnWriteArrayList<FraudAlert> recentAlerts = new CopyOnWriteArrayList<>();
    private static final int MAX_RECENT_ALERTS = 100;

    public FraudDetectionEventListener(FraudDetectionService fraudDetectionService,
                                       BankAccountRepository bankAccountRepository,
                                       SimpMessageSendingOperations messagingTemplate) {
        this.fraudDetectionService = fraudDetectionService;
        this.bankAccountRepository = bankAccountRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @EventHandler
    public void on(MoneyAddedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        String tenant = resolveTenant(tenantId);
        long balance = getBalance(tenant, event.getBankAccountId());
        TransactionContext ctx = new TransactionContext(
            event.getBankAccountId(), tenant, event.getAmount(),
            TransactionType.DEPOSIT, System.currentTimeMillis(), balance, "");
        evaluate(ctx);
    }

    @EventHandler
    public void on(MoneySubtractedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        String tenant = resolveTenant(tenantId);
        long balance = getBalance(tenant, event.getBankAccountId());
        TransactionContext ctx = new TransactionContext(
            event.getBankAccountId(), tenant, event.getAmount(),
            TransactionType.WITHDRAWAL, System.currentTimeMillis(), balance, "");
        evaluate(ctx);
    }

    @EventHandler
    public void on(BankTransferCreatedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        String tenant = resolveTenant(tenantId);
        long balance = getBalance(tenant, event.getSourceBankAccountId());
        TransactionContext ctx = new TransactionContext(
            event.getSourceBankAccountId(), tenant, event.getAmount(),
            TransactionType.TRANSFER_OUT, System.currentTimeMillis(), balance, "");
        evaluate(ctx);
    }

    private void evaluate(TransactionContext ctx) {
        FraudAssessment assessment = fraudDetectionService.assess(ctx);
        if (assessment.isSuspicious()) {
            FraudAlert alert = new FraudAlert(
                ctx.getAccountId(), ctx.getTenantId(), assessment.getRiskLevel(),
                assessment.getReasons(), ctx.getAmount(), ctx.getType().name());

            addAlert(alert);
            log.warn("Fraud alert [{}]: account={}, risk={}, reasons={}",
                alert.getAlertId(), ctx.getAccountId(),
                assessment.getRiskLevel(), assessment.getReasons());

            messagingTemplate.convertAndSend(
                "/topic/fraud-alerts." + ctx.getTenantId(), alert);
        }
    }

    private void addAlert(FraudAlert alert) {
        recentAlerts.add(0, alert);
        while (recentAlerts.size() > MAX_RECENT_ALERTS) {
            recentAlerts.remove(recentAlerts.size() - 1);
        }
    }

    /**
     * Returns recent fraud alerts (for REST endpoint consumption).
     */
    public List<FraudAlert> getRecentAlerts() {
        return Collections.unmodifiableList(new ArrayList<>(recentAlerts));
    }

    private long getBalance(String tenantId, String accountId) {
        BankAccountEntry entry = bankAccountRepository.findOneByTenantIdAndAxonBankAccountId(tenantId, accountId);
        return entry != null ? entry.getBalance() : 0;
    }

    private static String resolveTenant(String tenantId) {
        return tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
    }
}
