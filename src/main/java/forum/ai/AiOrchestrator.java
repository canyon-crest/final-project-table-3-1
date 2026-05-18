package forum.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adds cooldown/retry controls around provider calls.
 */
public class AiOrchestrator {

    public static final String POLICY_REFUSAL_MESSAGE = "Sorry, that violates our policy, so I can't help with that.";

    private final AiConfig config;
    private final AiProvider provider;
    private final Map<String, Long> lastCallAtByKey = new ConcurrentHashMap<>();

    public AiOrchestrator(AiConfig config, AiProvider provider) {
        this.config = config;
        this.provider = provider;
    }

    public String generate(String userCooldownKey, AiRequest request) throws Exception {
        enforceCooldown(userCooldownKey);
        String blockedReason = detectPolicyViolation(request == null ? null : request.getPrompt());
        if (blockedReason != null) {
            return POLICY_REFUSAL_MESSAGE;
        }
        Exception last = null;
        int attempts = 1 + Math.max(0, config.getRetryCount());
        for (int i = 0; i < attempts; i++) {
            try {
                String response = provider.generateReply(request);
                // Defensive output moderation: if the model slips, replace with refusal text.
                if (detectUnsafeOutput(response)) {
                    return POLICY_REFUSAL_MESSAGE;
                }
                return response;
            } catch (Exception ex) {
                last = ex;
                if (i + 1 < attempts) {
                    Thread.sleep(250L);
                }
            }
        }
        throw last == null ? new IllegalStateException("AI provider failed.") : last;
    }

    private void enforceCooldown(String key) {
        long cooldownMs = Math.max(0L, config.getCooldownMs());
        if (cooldownMs <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastCallAtByKey.get(key);
        if (last != null && now - last < cooldownMs) {
            throw new IllegalStateException("Please wait before using /ai again.");
        }
        lastCallAtByKey.put(key, now);
    }

    private static String detectPolicyViolation(String prompt) {
        if (prompt == null) {
            return null;
        }
        String p = prompt.trim().toLowerCase();
        if (p.isEmpty()) {
            return null;
        }
        String[] jailbreakPhrases = {
                "ignore all previous",
                "ignore previous instructions",
                "forget your instructions",
                "bypass safety",
                "bypass policy",
                "break the rules",
                "act without restrictions"
        };
        for (String phrase : jailbreakPhrases) {
            if (p.contains(phrase)) {
                return "jailbreak";
            }
        }
        String[] offensiveRequests = {
                "say something racist",
                "write a racist",
                "hate speech",
                "slur",
                "offensive joke about"
        };
        for (String phrase : offensiveRequests) {
            if (p.contains(phrase)) {
                return "offensive";
            }
        }
        String[] hateTopics = {
                "white supremacist", "white supremacy", "supremacist", "nazi", "neo-nazi", "kkk",
                "racist", "racism", "hate group", "ethnic cleansing", "genocide", "slur"
        };
        String[] detailRequestWords = {
                "story", "write", "describe", "exactly", "specific", "examples",
                "views", "beliefs", "arguments", "talking points"
        };
        if (containsAny(p, hateTopics) && containsAny(p, detailRequestWords)) {
            return "hate_topic_detail_request";
        }
        return null;
    }

    private static boolean detectUnsafeOutput(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }
        String out = response.toLowerCase();
        if (out.contains(POLICY_REFUSAL_MESSAGE.toLowerCase())) {
            return false;
        }
        String[] unsafeTerms = {
                "white supremacy", "white supremacist", "nazi", "neo-nazi", "kkk",
                "racial superiority", "hate group", "ethnic cleansing", "genocide", "slur"
        };
        return containsAny(out, unsafeTerms);
    }

    private static boolean containsAny(String text, String[] needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
