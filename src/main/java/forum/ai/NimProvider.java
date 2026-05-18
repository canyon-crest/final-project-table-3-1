package forum.ai;

/**
 * Placeholder adapter for NVIDIA NIM; kept so provider swapping is straightforward later.
 */
public class NimProvider implements AiProvider {
    @Override
    public String generateReply(AiRequest request) {
        throw new UnsupportedOperationException("NIM provider is not configured in this PoC build.");
    }
}
