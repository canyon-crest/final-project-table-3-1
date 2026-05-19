package forum.service;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import forum.AppPaths;
import forum.ai.AiConfig;
import forum.ai.AiOrchestrator;
import forum.ai.AiProvider;
import forum.ai.AiRequest;
import forum.ai.KimiProvider;
import forum.ai.NimProvider;
import forum.db.CategoryRepository;
import forum.db.CommentRepository;
import forum.db.ThreadRepository;
import forum.db.UserRepository;
import forum.model.AvatarOption;
import forum.model.CategoryInfo;
import forum.model.CommentInfo;
import forum.model.ForumUser;
import forum.model.ThreadInfo;

/**
 * Coordinates repositories and session state (composition: has-a users + categories + session).
 */
public class ForumService {

    private final ForumSession session;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ThreadRepository threads;
    private final CommentRepository comments;
    private final AiConfig aiConfig;
    private final AiOrchestrator aiOrchestrator;
    private final ExecutorService aiExecutor;
    private Long aiBotUserId;
    public static final String AI_BOT_USERNAME = "kimi_bot";
    private static final String AI_PENDING_MESSAGE = "[AI] ...";
    private static final int XP_CREATE_POST = 20;
    private static final int XP_CREATE_COMMENT = 8;
    private static final int XP_RECEIVE_LIKE = 2;
    private String lastUserMessage;

    /**
     * @param session    shared login state
     * @param users      user persistence
     * @param categories category persistence
     * @param threads thread persistence
     * @param comments   comment persistence
     */
    public ForumService(ForumSession session, UserRepository users, CategoryRepository categories,
            ThreadRepository threads, CommentRepository comments) {
        this.session = session;
        this.users = users;
        this.categories = categories;
        this.threads = threads;
        this.comments = comments;
        File configFile = AppPaths.resolveForumPropertiesFile();
        this.aiConfig = AiConfig.loadFromFile(configFile);
        AiProvider provider = "nim".equalsIgnoreCase(aiConfig.getProvider())
                ? new NimProvider()
                : new KimiProvider(aiConfig);
        this.aiOrchestrator = new AiOrchestrator(aiConfig, provider);
        this.aiExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "forum-ai-worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * @return the live session (for UI labels, logout, etc.)
     */
    public ForumSession getSession() {
        return session;
    }

    /**
     * Returns and clears the last user-facing message from service operations.
     *
     * @return last message shown to users, then clears it from memory
     */
    public String consumeLastUserMessage() {
        String msg = lastUserMessage;
        lastUserMessage = null;
        return msg;
    }

    /**
     * Helper used by UI/avatar rendering to detect the special AI bot account.
     *
     * @param username username to test
     * @return true when username matches {@link #AI_BOT_USERNAME}
     */
    public boolean isAiBotUsername(String username) {
        return username != null && AI_BOT_USERNAME.equalsIgnoreCase(username.trim());
    }

    /**
     * Attempts login and updates {@link ForumSession} on success.
     *
     * @param username plaintext username
     * @param password plaintext password
     * @return true if credentials matched
     * @throws SQLException on database errors
     */
    public boolean attemptLogin(String username, String password) throws SQLException {
        if (username == null || password == null || username.isBlank()) {
            return false;
        }
        Optional<ForumUser> row = users.findByUsername(username.trim());
        if (row.isEmpty()) {
            return false;
        }
        ForumUser u = row.get();
        if (!users.passwordMatches(password, u.getPasswordHash())) {
            return false;
        }
        session.setCurrentUser(u);
        return true;
    }

    /**
     * Creates an account if validation passes and the name is free.
     *
     * @param username plaintext username
     * @param password plaintext password
     * @return {@code true} if a row was inserted
     * @throws SQLException on database errors
     */
    public boolean register(String username, String password) throws SQLException {
        if (!ForumUser.isValidUsername(username) || password == null || password.isEmpty()) {
            return false;
        }
        String normalized = username.trim();
        if (AI_BOT_USERNAME.equalsIgnoreCase(normalized)) {
            return false;
        }
        if (users.findByUsername(normalized).isPresent()) {
            return false;
        }
        long id = users.insertUser(normalized, password);
        return id > 0;
    }

    /**
     * Updates the logged-in user's username and refreshes session data.
     *
     * @param newUsername proposed new name
     * @return true if update was applied
     * @throws SQLException on database errors
     */
    public boolean updateCurrentUsername(String newUsername) throws SQLException {
        if (!session.isLoggedIn() || !ForumUser.isValidUsername(newUsername)) {
            return false;
        }
        String normalized = newUsername.trim();
        if (AI_BOT_USERNAME.equalsIgnoreCase(normalized)) {
            return false;
        }
        ForumUser current = session.getCurrentUser();
        if (current.getUsername().equals(normalized)) {
            return true;
        }
        if (users.findByUsername(normalized).isPresent()) {
            return false;
        }
        boolean updated = users.updateUsername(current.getId(), normalized);
        if (updated) {
            session.setCurrentUser(new ForumUser(current.getId(), normalized, current.getPasswordHash(),
                    current.getAvatarHeadpieceId(), current.getAvatarClothingId(), current.getAvatarAccessoryId(),
                    current.getXpTotal(), current.getLevel()));
        }
        return updated;
    }

    /**
     * Loads active headpiece choices for the profile editor.
     *
     * @return list of headpiece options
     * @throws SQLException on database errors
     */
    public List<AvatarOption> loadHeadpieces() throws SQLException {
        return users.findAllHeadpieces();
    }

    /**
     * Loads active clothing choices for the profile editor.
     *
     * @return list of clothing options
     * @throws SQLException on database errors
     */
    public List<AvatarOption> loadClothing() throws SQLException {
        return users.findAllClothing();
    }

    /**
     * Loads active accessory choices for the profile editor.
     *
     * @return list of accessory options
     * @throws SQLException on database errors
     */
    public List<AvatarOption> loadAccessories() throws SQLException {
        return users.findAllAccessories();
    }

    /**
     * Updates the logged-in user's avatar part selections.
     *
     * @param headpieceId selected headpiece id, or null
     * @param clothingId selected clothing id, or null
     * @param accessoryId selected accessory id, or null
     * @return true if selection saved successfully
     * @throws SQLException on database errors
     */
    public boolean updateCurrentAvatarSelection(Long headpieceId, Long clothingId, Long accessoryId) throws SQLException {
        if (!session.isLoggedIn()) {
            return false;
        }
        ForumUser current = session.getCurrentUser();
        boolean ok = users.updateAvatarSelection(current.getId(), headpieceId, clothingId, accessoryId);
        if (ok) {
            session.setCurrentUser(new ForumUser(
                    current.getId(),
                    current.getUsername(),
                    current.getPasswordHash(),
                    headpieceId,
                    clothingId,
                    accessoryId,
                    current.getXpTotal(),
                    current.getLevel()));
        }
        return ok;
    }

    /**
     * Loads categories and applies an in-memory ordering pass (selection sort by {@code sortOrder}).
     *
     * @return a new list sorted for display
     * @throws SQLException on database errors
     */
    public List<CategoryInfo> loadCategoriesSorted() throws SQLException {
        List<CategoryInfo> list = new ArrayList<>(categories.findAll());
        selectionSortBySortOrder(list);
        return list;
    }

    /**
     * Loads posts for the chosen category.
     *
     * @param categoryId category to view
     * @return posts in newest-first order
     * @throws SQLException on database errors
     */
    public List<ThreadInfo> loadPostsForCategory(long categoryId) throws SQLException {
        return threads.findByCategoryId(categoryId);
    }

    /**
     * Creates a new thread in the selected category.
     *
     * @param categoryId target category
     * @param title post title
     * @param content post body
     * @return true if inserted
     * @throws SQLException on database errors
     */
    public boolean createPost(long categoryId, String title, String content) throws SQLException {
        if (!session.isLoggedIn()) {
            return false;
        }
        if (title == null || title.isBlank() || title.length() > 150) {
            return false;
        }
        if (content == null || content.isBlank()) {
            return false;
        }
        long authorId = session.getCurrentUser().getId();
        long id = threads.insertThread(categoryId, authorId, title.trim(), content.trim());
        if (id > 0) {
            grantXpToUser(authorId, XP_CREATE_POST);
            return true;
        }
        return false;
    }

    /**
     * Loads comments for a thread, oldest first.
     *
     * @param threadId thread id
     * @return comments with author names
     * @throws SQLException on database errors
     */
    public List<CommentInfo> loadCommentsForThread(long threadId) throws SQLException {
        return comments.findByThreadId(threadId);
    }

    /**
     * Adds a top-level comment on a thread.
     *
     * @param threadId target thread
     * @param content  comment body
     * @return true if inserted
     * @throws SQLException on database errors
     */
    public boolean addComment(long threadId, String content) throws SQLException {
        return addComment(threadId, null, content);
    }

    /**
     * Adds a comment on a thread; if parentCommentId is set, this is a reply.
     *
     * @param threadId target thread
     * @param parentCommentId comment being replied to, or {@code null} for top-level
     * @param content comment body
     * @return true if inserted
     * @throws SQLException on database errors
     */
    public boolean addComment(long threadId, Long parentCommentId, String content) throws SQLException {
        lastUserMessage = null;
        if (!session.isLoggedIn()) {
            lastUserMessage = "Log in first to add a comment.";
            return false;
        }
        if (content == null || content.isBlank()) {
            lastUserMessage = "Enter comment text.";
            return false;
        }
        String trimmed = content.trim();
        if (isAiCommand(trimmed)) {
            return handleAiCommand(threadId, parentCommentId, trimmed);
        }
        long authorId = session.getCurrentUser().getId();
        long id = comments.insertComment(threadId, authorId, parentCommentId, trimmed);
        if (id > 0) {
            grantXpToUser(authorId, XP_CREATE_COMMENT);
            return true;
        }
        return false;
    }

    /**
     * Applies a like/dislike toggle on a thread for the logged-in user.
     *
     * @param threadId target thread
     * @param like true for like, false for dislike
     * @return true if the request was processed
     * @throws SQLException on database errors
     */
    public boolean reactToThread(long threadId, boolean like) throws SQLException {
        if (!session.isLoggedIn()) {
            return false;
        }
        long voterId = session.getCurrentUser().getId();
        int reactionState = threads.setReaction(threadId, voterId, like ? 1 : -1);
        if (reactionState == 1) {
            long authorId = threads.findAuthorId(threadId);
            if (authorId > 0L && authorId != voterId) {
                grantXpToUser(authorId, XP_RECEIVE_LIKE);
            }
        }
        return true;
    }

    /**
     * Applies a like/dislike toggle on a comment for the logged-in user.
     *
     * @param commentId target comment
     * @param like true for like, false for dislike
     * @return true if the request was processed
     * @throws SQLException on database errors
     */
    public boolean reactToComment(long commentId, boolean like) throws SQLException {
        if (!session.isLoggedIn()) {
            return false;
        }
        long voterId = session.getCurrentUser().getId();
        int reactionState = comments.setReaction(commentId, voterId, like ? 1 : -1);
        if (reactionState == 1) {
            long authorId = comments.findAuthorId(commentId);
            if (authorId > 0L && authorId != voterId) {
                grantXpToUser(authorId, XP_RECEIVE_LIKE);
            }
        }
        return true;
    }

    private boolean handleAiCommand(long threadId, Long parentCommentId, String rawCommand) throws SQLException {
        String userPrompt = rawCommand.length() <= 3 ? "" : rawCommand.substring(3);
        if (userPrompt.startsWith(" ")) {
            userPrompt = userPrompt.substring(1);
        }
        if (userPrompt.isEmpty()) {
            lastUserMessage = "Usage: /ai <prompt>";
            return false;
        }
        if (userPrompt.length() > aiConfig.getMaxPromptChars()) {
            lastUserMessage = "Prompt is too long. Max " + aiConfig.getMaxPromptChars() + " characters.";
            return false;
        }
        long authorId = session.getCurrentUser().getId();
        long userCommandCommentId = comments.insertComment(threadId, authorId, parentCommentId, rawCommand.trim());
        if (userCommandCommentId <= 0L) {
            lastUserMessage = "Could not add comment.";
            return false;
        }
        grantXpToUser(authorId, XP_CREATE_COMMENT);
        long botId = ensureAiBotUserId();
        if (botId <= 0L) {
            return true;
        }
        long pendingAiCommentId = comments.insertComment(threadId, botId, userCommandCommentId, AI_PENDING_MESSAGE);
        if (pendingAiCommentId <= 0L) {
            return true;
        }

        boolean summarize = isSummarizeCommand(userPrompt);
        Optional<ThreadInfo> thread = threads.findById(threadId);
        String threadTitle = thread.map(ThreadInfo::getTitle).orElse("Thread #" + threadId);
        String threadContent = thread.map(ThreadInfo::getContent).orElse("");
        if (threadContent.length() > 900) {
            threadContent = threadContent.substring(0, 900) + "...";
        }
        List<String> recent;
        if (summarize) {
            List<CommentInfo> allComments = comments.findByThreadId(threadId);
            recent = extractRecentCommentContext(allComments, aiConfig.getMaxContextComments());
        } else {
            // For normal /ai prompts, do not include old comments.
            recent = List.of();
        }
        int adaptiveOutputTokens = computeAdaptiveOutputTokenBudget(userPrompt, summarize, recent.size());
        AiRequest request = new AiRequest(
                threadTitle,
                threadContent,
                session.getCurrentUser().getUsername(),
                userPrompt,
                summarize,
                recent,
                adaptiveOutputTokens);
        String cooldownKey = session.getCurrentUser().getId() + ":" + threadId;
        aiExecutor.execute(() -> completeAiPlaceholderComment(pendingAiCommentId, cooldownKey, request));
        return true;
    }

    private boolean isAiCommand(String text) {
        return text != null && text.trim().toLowerCase().startsWith("/ai ");
    }

    private boolean isSummarizeCommand(String prompt) {
        String p = prompt == null ? "" : prompt.trim().toLowerCase();
        return "summarize".equals(p)
                || "summarize this thread".equals(p)
                || p.startsWith("summarize ");
    }

    private List<String> extractRecentCommentContext(List<CommentInfo> commentsList, int maxContextComments) {
        List<String> out = new ArrayList<>();
        if (commentsList == null || commentsList.isEmpty()) {
            return out;
        }
        int max = Math.max(1, maxContextComments);
        int start = Math.max(0, commentsList.size() - max);
        for (int i = start; i < commentsList.size(); i++) {
            CommentInfo c = commentsList.get(i);
            String body = c.getContent() == null ? "" : c.getContent().trim();
            if (body.isEmpty()) {
                continue;
            }
            if (isAiBotUsername(c.getAuthorUsername())) {
                continue;
            }
            if (body.startsWith("/ai ") || body.startsWith("[AI]")) {
                continue;
            }
            if (body.length() > 220) {
                body = body.substring(0, 220) + "...";
            }
            out.add(c.getAuthorUsername() + ": " + body);
        }
        return out;
    }

    private int computeAdaptiveOutputTokenBudget(String userPrompt, boolean summarizeMode, int contextCount) {
        int configured = Math.max(32, aiConfig.getMaxOutputTokens());
        int budget = configured;
        String lower = userPrompt == null ? "" : userPrompt.toLowerCase();

        // If the prompt asks for a long answer, lower the token budget to avoid cutoffs.
        if (lower.contains("story") || lower.contains("detailed") || lower.contains("exactly")
                || lower.contains("example") || lower.contains("step by step")) {
            budget -= 40;
        }
        if (summarizeMode) {
            budget = Math.min(budget, 180);
        }
        int promptLength = userPrompt == null ? 0 : userPrompt.length();
        if (promptLength > 500) {
            budget -= 24;
        } else if (promptLength > 250) {
            budget -= 12;
        }
        if (contextCount > 8) {
            budget -= 12;
        }
        return Math.max(80, Math.min(configured, budget));
    }

    private void completeAiPlaceholderComment(long pendingAiCommentId, String cooldownKey, AiRequest request) {
        String finalText;
        if (!aiConfig.isEnabled()) {
            finalText = "[AI] AI is disabled. Set ai.enabled=true in forum.properties.";
        } else {
            try {
                String reply = aiOrchestrator.generate(cooldownKey, request);
                if (reply == null || reply.isBlank()) {
                    finalText = "[AI] Sorry, I couldn't generate a response right now.";
                } else {
                    finalText = "[AI] " + reply.trim();
                }
            } catch (Exception ex) {
                finalText = "[AI] Sorry, the AI request failed. Please try again.";
            }
        }
        try {
            comments.updateCommentContent(pendingAiCommentId, finalText);
        } catch (SQLException ignored) {
            // If this fails, keep the temporary AI comment text.
        }
    }

    private long ensureAiBotUserId() throws SQLException {
        if (aiBotUserId != null && aiBotUserId.longValue() > 0L) {
            return aiBotUserId.longValue();
        }
        Optional<ForumUser> existing = users.findByUsername(AI_BOT_USERNAME);
        if (existing.isPresent()) {
            aiBotUserId = Long.valueOf(existing.get().getId());
            return aiBotUserId.longValue();
        }
        String generatedPassword = "bot-" + UUID.randomUUID() + "-" + UUID.randomUUID();
        long created = users.insertUser(AI_BOT_USERNAME, generatedPassword);
        if (created > 0L) {
            aiBotUserId = Long.valueOf(created);
            return created;
        }
        return -1L;
    }

    private void grantXpToUser(long userId, int delta) {
        if (delta <= 0 || userId <= 0L) {
            return;
        }
        try {
            users.addXp(userId, delta);
            refreshSessionUserIfNeeded(userId);
        } catch (SQLException ignored) {
            // XP is a bonus system; do not fail the main action when XP update fails.
        }
    }

    private void refreshSessionUserIfNeeded(long userId) throws SQLException {
        if (!session.isLoggedIn() || session.getCurrentUser().getId() != userId) {
            return;
        }
        users.findById(userId).ifPresent(session::setCurrentUser);
    }

    /**
     * In-place selection sort.
     * This is a classic algorithm: find the smallest item in the unsorted part
     * and swap it to the front.
     *
     * @param list categories to reorder
     */
    void selectionSortBySortOrder(List<CategoryInfo> list) {
        for (int i = 0; i < list.size(); i++) {
            // Assume the minimum is the first element of the unsorted part
            int min = i;
            
            // Iterate through the rest of the list to find the actual minimum
            for (int j = i + 1; j < list.size(); j++) {
                if (list.get(j).getSortOrder() < list.get(min).getSortOrder()) {
                    min = j;
                }
            }
            
            // Swap if a smaller element was found
            if (min != i) {
                CategoryInfo tmp = list.get(i);
                list.set(i, list.get(min));
                list.set(min, tmp);
            }
        }
    }
}
