package org.axonframework.samples.bank.ai.anomaly;

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.samples.bank.api.bankaccount.MoneyAddedEvent;
import org.axonframework.samples.bank.api.bankaccount.MoneySubtractedEvent;
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
 * Listens to money events, feeds the anomaly detection engine,
 * and pushes alerts to WebSocket clients when anomalies are detected.
 */
@Component
public class AnomalyDetectionEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionEventListener.class);

    private final AnomalyDetectionService anomalyDetectionService;
    private final SimpMessageSendingOperations messagingTemplate;

    private final CopyOnWriteArrayList<AnomalyAlert> recentAlerts = new CopyOnWriteArrayList<>();
    private static final int MAX_RECENT_ALERTS = 100;

    public AnomalyDetectionEventListener(AnomalyDetectionService anomalyDetectionService,
                                          SimpMessageSendingOperations messagingTemplate) {
        this.anomalyDetectionService = anomalyDetectionService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventHandler
    public void on(MoneyAddedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        checkAnomaly(event.getBankAccountId(), resolveTenant(tenantId), event.getAmount());
    }

    @EventHandler
    public void on(MoneySubtractedEvent event,
                   @MetaDataValue(value = TenantConstants.TENANT_ID_KEY, required = false) String tenantId) {
        checkAnomaly(event.getBankAccountId(), resolveTenant(tenantId), event.getAmount());
    }

    private void checkAnomaly(String accountId, String tenant, long amount) {
        AnomalyAlert alert = anomalyDetectionService.analyze(accountId, tenant, amount);
        if (alert != null) {
            addAlert(alert);
            log.warn("Anomaly detected [{}]: account={}, severity={}, zScore={}, amount={}",
                alert.getAlertId(), accountId, alert.getSeverity(), alert.getZScore(), amount);
            messagingTemplate.convertAndSend("/topic/anomaly-alerts." + tenant, alert);
        }
    }

    private void addAlert(AnomalyAlert alert) {
        recentAlerts.add(0, alert);
        while (recentAlerts.size() > MAX_RECENT_ALERTS) {
            recentAlerts.remove(recentAlerts.size() - 1);
        }
    }

    public List<AnomalyAlert> getRecentAlerts() {
        return Collections.unmodifiableList(new ArrayList<>(recentAlerts));
    }

    private static String resolveTenant(String tenantId) {
        return tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT;
    }
}
