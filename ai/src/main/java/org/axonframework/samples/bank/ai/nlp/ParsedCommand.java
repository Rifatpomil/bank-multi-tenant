package org.axonframework.samples.bank.ai.nlp;

import java.util.Collections;
import java.util.Map;

/**
 * Result of parsing a natural language command.
 */
public class ParsedCommand {

    private final String commandType;
    private final Map<String, String> parameters;
    private final double confidence;
    private final String originalInput;
    private final String suggestedConfirmation;

    public ParsedCommand(String commandType, Map<String, String> parameters,
                          double confidence, String originalInput, String suggestedConfirmation) {
        this.commandType = commandType;
        this.parameters = Collections.unmodifiableMap(parameters);
        this.confidence = confidence;
        this.originalInput = originalInput;
        this.suggestedConfirmation = suggestedConfirmation;
    }

    public String getCommandType() { return commandType; }
    public Map<String, String> getParameters() { return parameters; }
    public double getConfidence() { return confidence; }
    public String getOriginalInput() { return originalInput; }
    public String getSuggestedConfirmation() { return suggestedConfirmation; }

    public boolean isRecognized() {
        return !"UNKNOWN".equals(commandType);
    }
}
