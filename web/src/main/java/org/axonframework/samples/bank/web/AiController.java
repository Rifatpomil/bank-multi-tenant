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

package org.axonframework.samples.bank.web;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.callbacks.LoggingCallback;
import org.axonframework.messaging.MetaData;
import org.axonframework.samples.bank.ai.anomaly.AnomalyAlert;
import org.axonframework.samples.bank.ai.anomaly.AnomalyDetectionEventListener;
import org.axonframework.samples.bank.ai.categorization.CategorizedTransactionEntry;
import org.axonframework.samples.bank.ai.categorization.CategorizedTransactionRepository;
import org.axonframework.samples.bank.ai.forecast.BalanceForecast;
import org.axonframework.samples.bank.ai.forecast.ForecastService;
import org.axonframework.samples.bank.ai.fraud.FraudAlert;
import org.axonframework.samples.bank.ai.fraud.FraudDetectionEventListener;
import org.axonframework.samples.bank.ai.insights.FinancialInsightsService;
import org.axonframework.samples.bank.ai.insights.InsightResponse;
import org.axonframework.samples.bank.ai.nlp.NlpCommandService;
import org.axonframework.samples.bank.ai.nlp.ParsedCommand;
import org.axonframework.samples.bank.api.bankaccount.CreateBankAccountCommand;
import org.axonframework.samples.bank.api.bankaccount.DepositMoneyCommand;
import org.axonframework.samples.bank.api.bankaccount.WithdrawMoneyCommand;
import org.axonframework.samples.bank.api.banktransfer.CreateBankTransferCommand;
import org.axonframework.samples.bank.tenant.TenantConstants;
import org.axonframework.samples.bank.tenant.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.axonframework.commandhandling.GenericCommandMessage.asCommandMessage;

/**
 * REST controller exposing all AI-powered features.
 * All endpoints are prefixed with /api/ai and tenant-aware via X-Tenant-ID
 * header.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final FraudDetectionEventListener fraudDetectionEventListener;
    private final AnomalyDetectionEventListener anomalyDetectionEventListener;
    private final CategorizedTransactionRepository categorizedTransactionRepository;
    private final FinancialInsightsService financialInsightsService;
    private final ForecastService forecastService;
    private final NlpCommandService nlpCommandService;
    private final CommandBus commandBus;

    public AiController(FraudDetectionEventListener fraudDetectionEventListener,
            AnomalyDetectionEventListener anomalyDetectionEventListener,
            CategorizedTransactionRepository categorizedTransactionRepository,
            FinancialInsightsService financialInsightsService,
            ForecastService forecastService,
            NlpCommandService nlpCommandService,
            CommandBus commandBus) {
        this.fraudDetectionEventListener = fraudDetectionEventListener;
        this.anomalyDetectionEventListener = anomalyDetectionEventListener;
        this.categorizedTransactionRepository = categorizedTransactionRepository;
        this.financialInsightsService = financialInsightsService;
        this.forecastService = forecastService;
        this.nlpCommandService = nlpCommandService;
        this.commandBus = commandBus;
    }

    // ===== Fraud Detection =====

    @GetMapping("/fraud/alerts")
    public List<FraudAlert> getFraudAlerts() {
        return fraudDetectionEventListener.getRecentAlerts();
    }

    // ===== Anomaly Detection =====

    @GetMapping("/anomaly/alerts")
    public List<AnomalyAlert> getAnomalyAlerts() {
        return anomalyDetectionEventListener.getRecentAlerts();
    }

    // ===== Transaction Categorization =====

    @GetMapping("/categorization/{accountId}")
    public List<CategorizedTransactionEntry> getCategorizedTransactions(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String tenantId = resolveTenant();
        return categorizedTransactionRepository
                .findByTenantIdAndAccountIdOrderByTimestampMillisDesc(tenantId, accountId, new PageRequest(page, size));
    }

    // ===== Financial Insights =====

    @PostMapping("/insights")
    public InsightResponse getInsights(@RequestBody Map<String, String> request) {
        String tenantId = resolveTenant();
        String accountId = request.get("accountId");
        String question = request.get("question");
        return financialInsightsService.answer(tenantId, accountId, question);
    }

    // ===== Balance Forecasting =====

    @GetMapping("/forecast/{accountId}")
    public BalanceForecast getBalanceForecast(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "7") int daysAhead) {
        String tenantId = resolveTenant();
        return forecastService.forecast(tenantId, accountId, daysAhead);
    }

    // ===== NLP Command Interface =====

    @PostMapping("/nlp/parse")
    public ParsedCommand parseCommand(@RequestBody Map<String, String> request) {
        String input = request.get("command");
        return nlpCommandService.parse(input);
    }

    @PostMapping("/nlp/execute")
    public ResponseEntity<Map<String, Object>> executeNlpCommand(@RequestBody Map<String, String> request) {
        String input = request.get("command");
        ParsedCommand parsed = nlpCommandService.parse(input);

        Map<String, Object> response = new HashMap<>();
        response.put("parsedCommand", parsed);

        if (!parsed.isRecognized()) {
            response.put("executed", false);
            response.put("message", parsed.getSuggestedConfirmation());
            return ResponseEntity.badRequest().body(response);
        }

        String tenantId = resolveTenant();
        MetaData meta = MetaData.with(TenantConstants.TENANT_ID_KEY, tenantId);

        try {
            switch (parsed.getCommandType()) {
                case "CREATE_BANK_ACCOUNT": {
                    String id = UUID.randomUUID().toString();
                    long overdraft = Long.parseLong(parsed.getParameters().getOrDefault("overdraftLimit", "0"));
                    commandBus.dispatch(asCommandMessage(new CreateBankAccountCommand(id, overdraft)).andMetaData(meta),
                            LoggingCallback.INSTANCE);
                    response.put("executed", true);
                    response.put("message", "Account created with ID: " + id);
                    response.put("accountId", id);
                    break;
                }
                case "DEPOSIT_MONEY": {
                    String accountId = parsed.getParameters().get("accountId");
                    long amount = Long.parseLong(parsed.getParameters().get("amount"));
                    commandBus.dispatch(asCommandMessage(new DepositMoneyCommand(accountId, amount)).andMetaData(meta),
                            LoggingCallback.INSTANCE);
                    response.put("executed", true);
                    response.put("message", "Deposited " + amount + " to " + accountId);
                    break;
                }
                case "WITHDRAW_MONEY": {
                    String accountId = parsed.getParameters().get("accountId");
                    long amount = Long.parseLong(parsed.getParameters().get("amount"));
                    commandBus.dispatch(asCommandMessage(new WithdrawMoneyCommand(accountId, amount)).andMetaData(meta),
                            LoggingCallback.INSTANCE);
                    response.put("executed", true);
                    response.put("message", "Withdrew " + amount + " from " + accountId);
                    break;
                }
                case "CREATE_BANK_TRANSFER": {
                    String transferId = UUID.randomUUID().toString();
                    String source = parsed.getParameters().get("sourceAccountId");
                    String dest = parsed.getParameters().get("destinationAccountId");
                    long amount = Long.parseLong(parsed.getParameters().get("amount"));
                    commandBus.dispatch(
                            asCommandMessage(new CreateBankTransferCommand(transferId, source, dest, amount))
                                    .andMetaData(meta),
                            LoggingCallback.INSTANCE);
                    response.put("executed", true);
                    response.put("message", "Transfer of " + amount + " from " + source + " to " + dest + " initiated");
                    response.put("transferId", transferId);
                    break;
                }
                case "CHECK_BALANCE": {
                    response.put("executed", false);
                    response.put("message", "Balance queries are read-only. Use GET /api/bank-accounts endpoint.");
                    break;
                }
                default:
                    response.put("executed", false);
                    response.put("message", "Command type not yet supported for execution: " + parsed.getCommandType());
            }
        } catch (Exception e) {
            response.put("executed", false);
            response.put("message", "Error executing command: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    private static String resolveTenant() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
    }
}
