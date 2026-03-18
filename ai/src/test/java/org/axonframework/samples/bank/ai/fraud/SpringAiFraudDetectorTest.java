package org.axonframework.samples.bank.ai.fraud;

import org.axonframework.samples.bank.ai.TransactionContext;
import org.axonframework.samples.bank.ai.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SpringAiFraudDetectorTest {

    private ChatClient chatClient;
    private ChatClient.Builder builder;
    private SpringAiFraudDetector detector;

    @BeforeEach
    public void setUp() {
        chatClient = mock(ChatClient.class);
        builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        detector = new SpringAiFraudDetector(builder);
    }

    @Test
    public void testAssess_LowRisk_ReturnsHeuristicResult() {
        long noonTimestamp = LocalDateTime.now().withHour(12).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        TransactionContext context = new TransactionContext(
            "acc-1", "tenant-1", 100, TransactionType.DEPOSIT, 
            noonTimestamp, 1000, "test");
        
        FraudAssessment result = detector.assess(context);
        
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        verify(chatClient, never()).prompt();
    }

    @Test
    public void testAssess_HighRisk_CallsAiForExplanation() {
        // Amount above threshold
        TransactionContext context = new TransactionContext(
            "acc-1", "tenant-1", 60000, TransactionType.WITHDRAWAL, 
            System.currentTimeMillis(), 100000, "large withdrawal");

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Highly unusual withdrawal detected.");

        FraudAssessment result = detector.assess(context);

        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(result.getReasons()).anyMatch(r -> r.contains("AI Insight: Highly unusual withdrawal detected."));
        verify(chatClient).prompt();
    }
}
