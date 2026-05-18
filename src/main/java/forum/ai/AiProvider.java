package forum.ai;

/**
 * Adapter for provider-specific chat APIs.
 */
public interface AiProvider {
    String generateReply(AiRequest request) throws Exception;
}
