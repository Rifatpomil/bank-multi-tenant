package org.axonframework.samples.bank.ai.insights;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Structured response from the financial insights engine.
 */
public class InsightResponse {

    private final String answer;
    private final List<Map<String, Object>> dataPoints;
    private final double confidence;
    private final List<String> suggestedFollowUps;

    public InsightResponse(String answer, List<Map<String, Object>> dataPoints,
                            double confidence, List<String> suggestedFollowUps) {
        this.answer = answer;
        this.dataPoints = dataPoints != null ? Collections.unmodifiableList(dataPoints) : Collections.emptyList();
        this.confidence = confidence;
        this.suggestedFollowUps = suggestedFollowUps != null
            ? Collections.unmodifiableList(suggestedFollowUps) : Collections.emptyList();
    }

    public String getAnswer() { return answer; }
    public List<Map<String, Object>> getDataPoints() { return dataPoints; }
    public double getConfidence() { return confidence; }
    public List<String> getSuggestedFollowUps() { return suggestedFollowUps; }
}
