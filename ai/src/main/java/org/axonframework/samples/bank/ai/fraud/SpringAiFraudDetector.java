package org.axonframework.samples.bank.ai.fraud;

import org.axonframework.samples.bank.ai.TransactionContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * AI-enhanced fraud detection service using Spring AI.
 * Goes beyond simple rules to provide qualitative risk assessments
 * and natural language explanations of suspicious activity.
 */
@Service
@Primary
public class SpringAiFraudDetector implements FraudDetectionService {

    private final ChatClient chatClient;
    private final RuleBasedFraudDetector heuristicEngine;

    public SpringAiFraudDetector(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        this.heuristicEngine = new RuleBasedFraudDetector();
    }

    @Override
    public FraudAssessment assess(TransactionContext context) {
        // Step 1: Get raw risk level from the fast heuristic engine
        FraudAssessment heuristicResult = heuristicEngine.assess(context);

        // Step 2: If risk is above LOW, use AI to explain why it's a threat
        if (heuristicResult.getRiskLevel() != RiskLevel.LOW) {
            try {
                String hour = Instant.ofEpochMilli(context.getTimestampMillis())
                    .atZone(ZoneId.systemDefault()).toLocalTime().toString();
                
                String prompt = String.format("""
                        Analyze this suspicious banking transaction and explain the risk in a single professional sentence.
                        
                        Account: %s
                        Amount: %d cents
                        Type: %s
                        Time: %s
                        Current Balance: %d cents
                        Heuristic Reasons: %s
                        
                        Provide a brief, human-readable risk explanation for the security team.
                        """, 
                        context.getAccountId(), 
                        context.getAmount(), 
                        context.getType(), 
                        hour, 
                        context.getCurrentBalance(),
                        String.join(", ", heuristicResult.getReasons()));

                String explanation = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

                List<String> combinedReasons = new ArrayList<>(heuristicResult.getReasons());
                combinedReasons.add("AI Insight: " + explanation);
                
                return new FraudAssessment(heuristicResult.getRiskLevel(), combinedReasons);
            } catch (Exception e) {
                // Fallback to pure heuristic if AI is unavailable
                return heuristicResult;
            }
        }

        return heuristicResult;
    }
}
