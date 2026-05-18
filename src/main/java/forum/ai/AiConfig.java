package forum.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads AI settings from forum.properties.
 */
public final class AiConfig {

    private final boolean enabled;
    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutMs;
    private final int maxOutputTokens;
    private final int maxContextComments;
    private final int maxPromptChars;
    private final int maxContinuations;
    private final int retryCount;
    private final long cooldownMs;

    private AiConfig(boolean enabled, String provider, String baseUrl, String apiKey, String model,
            int timeoutMs, int maxOutputTokens, int maxContextComments, int maxPromptChars,
            int maxContinuations,
            int retryCount, long cooldownMs) {
        this.enabled = enabled;
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutMs = timeoutMs;
        this.maxOutputTokens = maxOutputTokens;
        this.maxContextComments = maxContextComments;
        this.maxPromptChars = maxPromptChars;
        this.maxContinuations = maxContinuations;
        this.retryCount = retryCount;
        this.cooldownMs = cooldownMs;
    }

    public static AiConfig loadFromFile(File file) {
        Properties p = new Properties();
        if (file != null && file.isFile()) {
            try (InputStream in = new FileInputStream(file)) {
                p.load(in);
            } catch (IOException ignored) {
                // Falls back to safe defaults.
            }
        }
        boolean enabled = parseBool(p.getProperty("ai.enabled"), false);
        String provider = normalize(p.getProperty("ai.provider", "kimi"));
        String baseUrl = normalize(p.getProperty("ai.base_url", "https://api.moonshot.cn/v1/chat/completions"));
        String apiKey = normalize(p.getProperty("ai.api_key", ""));
        String model = normalize(p.getProperty("ai.model", "moonshot-v1-8k"));
        int timeoutMs = parseInt(p.getProperty("ai.timeout_ms"), 15000, 1000, 120000);
        int maxOutputTokens = parseInt(p.getProperty("ai.max_output_tokens"), 220, 32, 2048);
        int maxContextComments = parseInt(p.getProperty("ai.max_context_comments"), 12, 1, 100);
        int maxPromptChars = parseInt(p.getProperty("ai.max_prompt_chars"), 1200, 80, 8000);
        int maxContinuations = parseInt(p.getProperty("ai.max_continuations"), 1, 0, 3);
        int retryCount = parseInt(p.getProperty("ai.retry_count"), 1, 0, 3);
        long cooldownMs = parseInt(p.getProperty("ai.cooldown_ms"), 2500, 0, 60000);
        return new AiConfig(enabled, provider, baseUrl, apiKey, model, timeoutMs, maxOutputTokens,
                maxContextComments, maxPromptChars, maxContinuations, retryCount, cooldownMs);
    }

    private static String normalize(String v) {
        return v == null ? "" : v.trim();
    }

    private static boolean parseBool(String raw, boolean fallback) {
        if (raw == null) {
            return fallback;
        }
        String t = raw.trim().toLowerCase();
        return "1".equals(t) || "true".equals(t) || "yes".equals(t) || "on".equals(t);
    }

    private static int parseInt(String raw, int fallback, int min, int max) {
        try {
            int n = Integer.parseInt(raw == null ? "" : raw.trim());
            if (n < min) {
                return min;
            }
            if (n > max) {
                return max;
            }
            return n;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getProvider() {
        return provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public int getMaxContextComments() {
        return maxContextComments;
    }

    public int getMaxPromptChars() {
        return maxPromptChars;
    }

    public int getMaxContinuations() {
        return maxContinuations;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }
}
