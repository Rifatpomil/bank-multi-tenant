package org.axonframework.samples.bank.ai.nlp;

/**
 * Strategy interface for natural language command parsing.
 */
public interface NlpCommandService {

    /**
     * Parse a natural language input into a structured command.
     *
     * @param input the natural language text
     * @return the parsed command with extracted parameters
     */
    ParsedCommand parse(String input);
}
