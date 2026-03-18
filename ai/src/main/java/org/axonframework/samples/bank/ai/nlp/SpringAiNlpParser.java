package org.axonframework.samples.bank.ai.nlp;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Real AI-powered natural language command parser using Spring AI and LLMs.
 * Replaces the rigid regex-based PatternBasedNlpParser with a more flexible
 * LLM-driven approach that understands context and intent.
 */
@Service
@Primary
public class SpringAiNlpParser implements NlpCommandService {

    private final ChatClient chatClient;

    public SpringAiNlpParser(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        Parse the following natural language banking command into a structured format.
                        Supported commands:
                        - CREATE_BANK_ACCOUNT (params: overdraftLimit)
                        - DEPOSIT_MONEY (params: amount, accountId)
                        - WITHDRAW_MONEY (params: amount, accountId)
                        - CREATE_BANK_TRANSFER (params: amount, sourceAccountId, destinationAccountId)
                        - CHECK_BALANCE (params: accountId)
                        
                        Return the result as JSON with fields:
                        - commandType: The command identifier (e.g., "CREATE_BANK_TRANSFER")
                        - parameters: A map of parameter keys to string values
                        - confidence: A double between 0 and 1
                        - suggestedConfirmation: A user-friendly confirmation message
                        
                        If you don't understand, return commandType "UNKNOWN".
                        """)
                .build();
    }

    @Override
    public ParsedCommand parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return unknownCommand(input);
        }

        try {
            return chatClient.prompt()
                    .user(u -> u.text("Input: \"{input}\"")
                            .param("input", input))
                    .call()
                    .entity(ParsedCommandResponse.class)
                    .toParsedCommand(input);
        } catch (Exception e) {
            // Fallback to unknown if AI fails
            return unknownCommand(input);
        }
    }

    private ParsedCommand unknownCommand(String input) {
        return new ParsedCommand("UNKNOWN", Map.of(), 0.0, input,
                "I'm sorry, I couldn't understand that command. Please try something like 'transfer $50 from acc-1 to acc-2'.");
    }

    /**
     * Internal DTO for structured output from Spring AI.
     */
    private static record ParsedCommandResponse(
            String commandType,
            Map<String, String> parameters,
            double confidence,
            String suggestedConfirmation
    ) {
        public ParsedCommand toParsedCommand(String originalInput) {
            return new ParsedCommand(
                    commandType,
                    parameters != null ? parameters : new HashMap<>(),
                    confidence,
                    originalInput,
                    suggestedConfirmation
            );
        }
    }
}
