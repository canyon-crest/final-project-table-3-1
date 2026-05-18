package forum.ai;

import java.util.List;

/**
 * Prompt payload sent to an AI provider.
 */
public class AiRequest {
    private final String threadTitle;
    private final String threadContent;
    private final String requesterUsername;
    private final String prompt;
    private final boolean summarizeMode;
    private final List<String> recentComments;
    private final int maxOutputTokens;

    public AiRequest(String threadTitle, String threadContent, String requesterUsername, String prompt, boolean summarizeMode,
            List<String> recentComments, int maxOutputTokens) {
        this.threadTitle = threadTitle == null ? "" : threadTitle;
        this.threadContent = threadContent == null ? "" : threadContent;
        this.requesterUsername = requesterUsername == null ? "" : requesterUsername;
        this.prompt = prompt == null ? "" : prompt;
        this.summarizeMode = summarizeMode;
        this.recentComments = recentComments;
        this.maxOutputTokens = maxOutputTokens;
    }

    public String getThreadTitle() {
        return threadTitle;
    }

    public String getThreadContent() {
        return threadContent;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public String getPrompt() {
        return prompt;
    }

    public boolean isSummarizeMode() {
        return summarizeMode;
    }

    public List<String> getRecentComments() {
        return recentComments;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }
}
