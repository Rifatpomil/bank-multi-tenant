package org.axonframework.samples.bank.ai.nlp;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex + keyword-based natural language command parser.
 * Recognizes common banking commands and extracts parameters.
 *
 * Supported patterns:
 * - "transfer $50 from X to Y" / "send 50 from X to Y"
 * - "deposit $100 to X" / "add 100 to X"
 * - "withdraw $75 from X" / "take 75 from X"
 * - "create account" / "open account" / "new account with overdraft $1000"
 * - "check balance of X" / "balance X"
 */
@Service
public class PatternBasedNlpParser implements NlpCommandService {

    // Transfer: "transfer $50 from acc-1 to acc-2"
    private static final Pattern TRANSFER_PATTERN = Pattern.compile(
        "(?i)(?:transfer|send|move)\\s+\\$?(\\d+)\\s+from\\s+(\\S+)\\s+to\\s+(\\S+)");

    // Deposit: "deposit $100 to acc-1"
    private static final Pattern DEPOSIT_PATTERN = Pattern.compile(
        "(?i)(?:deposit|add|put)\\s+\\$?(\\d+)\\s+(?:to|into|in)\\s+(\\S+)");

    // Withdraw: "withdraw $75 from acc-1"
    private static final Pattern WITHDRAW_PATTERN = Pattern.compile(
        "(?i)(?:withdraw|take|remove)\\s+\\$?(\\d+)\\s+from\\s+(\\S+)");

    // Create account: "create account with overdraft $1000"
    private static final Pattern CREATE_ACCOUNT_PATTERN = Pattern.compile(
        "(?i)(?:create|open|new)\\s+account(?:\\s+(?:with\\s+)?overdraft\\s+\\$?(\\d+))?");

    // Balance check: "check balance of acc-1" or "balance acc-1"
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
        "(?i)(?:check\\s+)?balance\\s+(?:of\\s+)?(\\S+)");

    @Override
    public ParsedCommand parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return unknownCommand(input);
        }

        String trimmed = input.trim();

        // Try transfer
        Matcher m = TRANSFER_PATTERN.matcher(trimmed);
        if (m.find()) {
            Map<String, String> params = new HashMap<>();
            params.put("amount", m.group(1));
            params.put("sourceAccountId", m.group(2));
            params.put("destinationAccountId", m.group(3));
            String confirmation = String.format(
                "Transfer $%s from %s to %s. Confirm?", m.group(1), m.group(2), m.group(3));
            return new ParsedCommand("CREATE_BANK_TRANSFER", params, 0.9, input, confirmation);
        }

        // Try deposit
        m = DEPOSIT_PATTERN.matcher(trimmed);
        if (m.find()) {
            Map<String, String> params = new HashMap<>();
            params.put("amount", m.group(1));
            params.put("accountId", m.group(2));
            String confirmation = String.format("Deposit $%s to %s. Confirm?", m.group(1), m.group(2));
            return new ParsedCommand("DEPOSIT_MONEY", params, 0.9, input, confirmation);
        }

        // Try withdraw
        m = WITHDRAW_PATTERN.matcher(trimmed);
        if (m.find()) {
            Map<String, String> params = new HashMap<>();
            params.put("amount", m.group(1));
            params.put("accountId", m.group(2));
            String confirmation = String.format("Withdraw $%s from %s. Confirm?", m.group(1), m.group(2));
            return new ParsedCommand("WITHDRAW_MONEY", params, 0.9, input, confirmation);
        }

        // Try create account
        m = CREATE_ACCOUNT_PATTERN.matcher(trimmed);
        if (m.find()) {
            Map<String, String> params = new HashMap<>();
            params.put("overdraftLimit", m.group(1) != null ? m.group(1) : "0");
            String confirmation = String.format(
                "Create a new bank account with overdraft limit $%s. Confirm?",
                params.get("overdraftLimit"));
            return new ParsedCommand("CREATE_BANK_ACCOUNT", params, 0.85, input, confirmation);
        }

        // Try balance check
        m = BALANCE_PATTERN.matcher(trimmed);
        if (m.find()) {
            Map<String, String> params = new HashMap<>();
            params.put("accountId", m.group(1));
            String confirmation = String.format("Check balance of account %s.", m.group(1));
            return new ParsedCommand("CHECK_BALANCE", params, 0.9, input, confirmation);
        }

        return unknownCommand(input);
    }

    private ParsedCommand unknownCommand(String input) {
        return new ParsedCommand("UNKNOWN", Collections.emptyMap(), 0.0, input,
            "Could not understand the command. Try: 'transfer $50 from acc-1 to acc-2', "
                + "'deposit $100 to acc-1', or 'withdraw $75 from acc-1'.");
    }
}
