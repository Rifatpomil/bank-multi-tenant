package org.axonframework.samples.bank.ai.insights;

import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryEntry;
import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered financial insights engine using Spring AI (RAG approach).
 * Fetches recent daily summary data and uses it as context for an LLM
 * to answer complex, natural language financial questions.
 */
@Service
@Primary
public class SpringAiInsightsEngine implements FinancialInsightsService {

    private final AccountDailySummaryRepository summaryRepository;
    private final ChatClient chatClient;

    public SpringAiInsightsEngine(AccountDailySummaryRepository summaryRepository, ChatClient.Builder builder) {
        this.summaryRepository = summaryRepository;
        this.chatClient = builder
                .defaultSystem("""
                        Analyze the 30-day daily summary data for a bank account and answer the user's question.
                        
                        Provide a professional, data-driven response as JSON with:
                        - answer: A concise, human-readable analysis
                        - confidence: A double between 0 and 1
                        - suggestedFollowUps: 2-3 short strings for the user to ask next
                        """)
                .build();
    }

    @Override
    public InsightResponse answer(String tenantId, String accountId, String question) {
        // Step 1: Fetch account transaction data (Retrieval phase of RAG)
        List<AccountDailySummaryEntry> summaries = summaryRepository
                .findByTenantIdAndAxonBankAccountIdOrderBySummaryDateDesc(
                        tenantId, accountId, PageRequest.of(0, 30, Sort.unsorted()));

        if (summaries.isEmpty()) {
            return new InsightResponse(
                    "I couldn't find any transaction history for this account to analyze.",
                    Collections.emptyList(), 0.1,
                    List.of("Try making some transactions first.", "Check the account ID and tenant."));
        }

        // Step 2: Prepare data context for the LLM
        String dataContext = summaries.stream()
                .map(s -> String.format("Date: %s, Deposits: %d, Withdrawals: %d, Closing Balance: %d",
                        s.getSummaryDate(), s.getTotalDeposits(), s.getTotalWithdrawals(), s.getClosingBalance()))
                .collect(Collectors.joining("\n"));

        try {
            // Step 3: Use LLM to generate insights based on the retrieved data (Generation phase)
            return chatClient.prompt()
                    .user(u -> u.text("""
                            Account ID: {accountId}
                            Transaction Context:
                            {context}
                            
                            User Question: "{question}"
                            """)
                            .param("accountId", accountId)
                            .param("context", dataContext)
                            .param("question", question))
                    .call()
                    .entity(InsightResponseResponse.class)
                    .toInsightResponse();
        } catch (Exception e) {
            // Fallback if AI fails
            return new InsightResponse(
                    "I encountered an error while analyzing your account data. Please try again later.",
                    Collections.emptyList(), 0.0,
                    List.of("Try again later", "Check system logs"));
        }
    }

    /**
     * Internal DTO for structured LLM response.
     */
    private static record InsightResponseResponse(
            String answer,
            double confidence,
            List<String> suggestedFollowUps
    ) {
        public InsightResponse toInsightResponse() {
            return new InsightResponse(
                    answer,
                    Collections.emptyList(), // In a more complex version, we could include chart data
                    confidence,
                    suggestedFollowUps
            );
        }
    }
}
