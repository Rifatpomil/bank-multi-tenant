package org.axonframework.samples.bank.ai.insights;

/**
 * Strategy interface for AI-powered financial insights.
 * Accepts natural language questions and returns structured answers
 * by querying the read model projections.
 */
public interface FinancialInsightsService {

    /**
     * Answer a natural language financial question.
     *
     * @param tenantId  the tenant context
     * @param accountId the account to query about
     * @param question  the natural language question
     * @return a structured insight response
     */
    InsightResponse answer(String tenantId, String accountId, String question);
}
