package forum.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;

import forum.AppPaths;
import forum.db.ConnectionManager;
import forum.model.AvatarOption;
import forum.model.CategoryInfo;
import forum.model.CommentInfo;
import forum.model.ForumUser;
import forum.model.ThreadInfo;
import forum.service.ForumService;

/**
 * Primary Swing shell: menus, category list, and database sanity checks.
 */
public class MainWindow extends JFrame {

    private static final String CARD_HOME = "home";
    private static final String CARD_FORUM = "forum";
    private static final String CARD_PROFILE = "profile";
    private static final String CARD_POST_DETAIL = "postDetail";
    private static final String CARD_CREATE_POST = "createPost";
   
    private static final Color NAVY_PRIMARY = new Color(24, 24, 24);
    private static final Color NAVY_MID = new Color(45, 45, 45);
    private static final Color GOLD_PRIMARY = new Color(186, 12, 47);
    private static final Color GOLD_HOVER_TONE = new Color(146, 10, 37);
    private static final Color BG_APP = new Color(244, 244, 246);
    private static final Color BG_PANEL = new Color(255, 255, 255);
    private static final Color BG_TOP = NAVY_PRIMARY;
    private static final Color TEXT_TOP = Color.WHITE;
    private static final Color ACCENT = GOLD_PRIMARY;
    private static final Color ACCENT_ALT = NAVY_MID;
    private static final Color TEXT_HEADING = NAVY_PRIMARY;
    private static final Color TEXT_MUTED = new Color(76, 76, 82);
    private static final Color PLACEHOLDER_BG = new Color(249, 239, 242);
    private static final Color PLACEHOLDER_BORDER = new Color(186, 12, 47);
    private static final Color SPLIT_BORDER = new Color(198, 198, 204);
    private static final String HOME_BANNER_IMAGE_PATH = "assets/teacher-wide.jpg";
    private static final String AI_PENDING_PLACEHOLDER = "[AI] ...";
    private static final int AI_AUTO_REFRESH_DELAY_MS = 1200;
    private static final int AI_AUTO_REFRESH_MAX_TRIES = 8;


    private final ForumService forumService;
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.LEFT);
    private final DefaultListModel<CategoryInfo> categoryModel = new DefaultListModel<>();
    private final JList<CategoryInfo> categoryList = new JList<>(categoryModel);
    private final DefaultListModel<ThreadInfo> postModel = new DefaultListModel<>();
    private final JList<ThreadInfo> postList = new JList<>(postModel);
    private final JButton loginButton = new JButton("Log in");
    private final JButton signupButton = new JButton("Sign up");
    private final JButton homeButton = new JButton("Home");
    private final JButton profileButton = new JButton("Profile");
    private final JButton logoutButton = new JButton("Log out");
    private final JPanel accountButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    private final JLabel accountAvatarLabel = new JLabel();
    private final CardLayout centerCards = new CardLayout();
    private final JPanel centerPanel = new JPanel(centerCards);
    private final ProfilePage profilePage = new ProfilePage(BG_APP, TEXT_MUTED, TEXT_HEADING, SPLIT_BORDER);
    private final JLabel profileAvatarLabel = profilePage.getAvatarLabel();
    private final JLabel profileUsernameLabel = profilePage.getUsernameLabel();
    private final JTextField profileUsernameField = profilePage.getUsernameField();
    private final PostDetailPage postDetailPage = new PostDetailPage(BG_APP, BG_PANEL, TEXT_HEADING, TEXT_MUTED, SPLIT_BORDER);
    private final JLabel detailPostTitleLabel = postDetailPage.getPostTitleLabel();
    private final JLabel detailPostMetaLabel = postDetailPage.getPostMetaLabel();
    private final JLabel detailPostAuthorAvatarLabel = postDetailPage.getPostAuthorAvatarLabel();
    private final JLabel detailPostVotesLabel = postDetailPage.getPostVotesLabel();
    private final JTextArea detailPostBodyArea = postDetailPage.getPostBodyArea();
    private final JPanel detailCommentsListPanel = postDetailPage.getCommentsListPanel();
    private final JTextArea detailNewCommentArea = postDetailPage.getNewCommentArea();
    private final JScrollPane detailPageScroll = postDetailPage.getPageScroll();
    private final JButton detailAddCommentButton = postDetailPage.getAddCommentButton();
    private final JButton detailRefreshCommentsButton = postDetailPage.getRefreshCommentsButton();
    private final JButton detailPostLikeButton = postDetailPage.getPostLikeButton();
    private final JButton detailPostDislikeButton = postDetailPage.getPostDislikeButton();
    private final CreatePostPage createPostPage = new CreatePostPage(BG_APP, TEXT_MUTED, TEXT_HEADING, SPLIT_BORDER);
    private ThreadInfo selectedPostForDetail;
    private boolean resetDetailScrollToTopPending;
    private final JLabel detailReplyContextLabel = postDetailPage.getReplyContextLabel();
    private final JButton detailCancelReplyButton = postDetailPage.getCancelReplyButton();
    private Long replyTargetCommentId;
    private final DateTimeFormatter uiDateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());
    private final JComboBox<AvatarOption> profileHeadpieceCombo = profilePage.getHeadpieceCombo();
    private final JComboBox<AvatarOption> profileClothingCombo = profilePage.getClothingCombo();
    private final JComboBox<AvatarOption> profileAccessoryCombo = profilePage.getAccessoryCombo();
    private List<AvatarOption> cachedHeadpieces = new ArrayList<>();
    private List<AvatarOption> cachedClothing = new ArrayList<>();
    private List<AvatarOption> cachedAccessories = new ArrayList<>();
    private boolean avatarOptionsLoaded;
    private boolean avatarWarmupInProgress;
    private boolean updatingCombos;
    /** True only after combo boxes match {@link #profileCombosOwnerUserId}. */
    private boolean profileCombosReady;
    private long profileCombosOwnerUserId = -1L;
    private ForumUser pendingProfileComboUser;
    private long postsReloadSequence;
    private int aiPendingAutoRefreshRemaining;

    /**
     * @param forumService application facade (session + repositories)
     */
    public MainWindow(ForumService forumService) {
        super("CCA Forum");
        this.forumService = forumService;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 560);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_APP);

        buildTopBar();
        buildContent();
        wireProfilePageActions();
        wireCreatePostPageActions();
        wirePostDetailPageActions();
        updateUiForSession();
        startCatalogWarmupOnStartup();
    }

    private void buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        topBar.setBackground(BG_TOP);

        JLabel title = new JLabel("CCA Forum");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(TEXT_TOP);
        topBar.add(title, BorderLayout.WEST);
        accountButtonPanel.setOpaque(false);

        loginButton.addActionListener(e -> new LoginDialog(this, forumService).setVisible(true));
        signupButton.addActionListener(e -> new RegisterDialog(this, forumService).setVisible(true));
        homeButton.addActionListener(e -> centerCards.show(centerPanel, CARD_FORUM));
        profileButton.addActionListener(e -> showProfile());
        logoutButton.addActionListener(e -> {
            forumService.getSession().logout();
            resetProfilePageForUserSwitch();
            avatarOptionsLoaded = false;
            pendingProfileComboUser = null;
            updateUiForSession();
            categoryModel.clear();
            postModel.clear();
        });

        topBar.add(accountButtonPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);
        stylePrimaryButton(loginButton);
        styleSecondaryButton(signupButton);
        styleSecondaryButton(homeButton);
        styleSecondaryButton(profileButton);
        styleSecondaryButton(logoutButton);
    }

    private void buildContent() {
        JPanel homePanel = buildHomePanel();
        JPanel forumPanel = buildForumPanel();
        centerPanel.add(scrollWrap(homePanel), CARD_HOME);
        centerPanel.add(forumPanel, CARD_FORUM);
        centerPanel.add(scrollWrap(profilePage), CARD_PROFILE);
        centerPanel.add(postDetailPage, CARD_POST_DETAIL);
        centerPanel.add(scrollWrap(createPostPage), CARD_CREATE_POST);
        centerPanel.setBackground(BG_APP);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(BG_APP);
        add(centerPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    /** Wraps a panel in a borderless scroll pane so content is reachable at any window size. */
    private static JScrollPane scrollWrap(JPanel panel) {
        JScrollPane sp = new JScrollPane(panel);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JPanel buildHomePanel() {
        JPanel home = new JPanel();
        home.setLayout(new BoxLayout(home, BoxLayout.Y_AXIS));
        home.setBorder(BorderFactory.createEmptyBorder(40, 20, 30, 20));
        home.setBackground(BG_APP);

        JLabel welcome = new JLabel("Welcome to CCA Forum");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 26f));
        welcome.setForeground(TEXT_HEADING);
        welcome.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Log in or sign up to view categories and posts.");
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        JButton testConnection = new JButton("Test connection");
        testConnection.addActionListener(e -> testConnection());
        testConnection.setAlignmentX(CENTER_ALIGNMENT);

        JPanel stretchedBanner = buildStretchedHomeBanner();
        styleSecondaryButton(testConnection);

        home.add(welcome);
        home.add(Box.createVerticalStrut(10));
        home.add(subtitle);
        home.add(Box.createVerticalStrut(16));
        home.add(testConnection);
        home.add(Box.createVerticalStrut(28));
        home.add(stretchedBanner);
        home.add(Box.createVerticalGlue());
        return home;
    }

    private JPanel buildStretchedHomeBanner() {
        BufferedImage bannerImage = loadHomeBannerImage();
        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bannerImage != null) {
                    g.drawImage(bannerImage, 0, 0, getWidth(), getHeight(), null);
                }
            }
        };
        banner.setPreferredSize(new Dimension(760, 220));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        banner.setMinimumSize(new Dimension(460, 220));
        banner.setAlignmentX(CENTER_ALIGNMENT);

        if (bannerImage == null) {
            banner.setBackground(PLACEHOLDER_BG);
            banner.setBorder(BorderFactory.createDashedBorder(PLACEHOLDER_BORDER));
            banner.setLayout(new BorderLayout());
            JLabel missing = new JLabel("Missing banner image: " + HOME_BANNER_IMAGE_PATH, SwingConstants.CENTER);
            missing.setForeground(NAVY_MID);
            banner.add(missing, BorderLayout.CENTER);
            return banner;
        }

        banner.setBorder(BorderFactory.createLineBorder(PLACEHOLDER_BORDER, 1));
        return banner;
    }

    private BufferedImage loadHomeBannerImage() {
        try (InputStream in = MainWindow.class.getResourceAsStream("/" + HOME_BANNER_IMAGE_PATH)) {
            if (in != null) {
                BufferedImage bundled = ImageIO.read(in);
                if (bundled != null) {
                    return bundled;
                }
            }
        } catch (IOException ignored) {
            // Fall through to external file lookup.
        }
        File bannerFile = AppPaths.resolveAssetFile(HOME_BANNER_IMAGE_PATH);
        if (bannerFile == null) {
            return null;
        }
        try {
            return ImageIO.read(bannerFile);
        } catch (IOException ex) {
            return null;
        }
    }

    private void wireProfilePageActions() {
        profileHeadpieceCombo.addActionListener(e -> refreshProfileAvatarPreview());
        profileClothingCombo.addActionListener(e -> refreshProfileAvatarPreview());
        profileAccessoryCombo.addActionListener(e -> refreshProfileAvatarPreview());
        profilePage.getUpdateProfileButton().addActionListener(e -> updateProfileFromForm());
        profilePage.getBackToForumButton().addActionListener(e -> centerCards.show(centerPanel, CARD_FORUM));
        stylePrimaryButton(profilePage.getUpdateProfileButton());
        styleSecondaryButton(profilePage.getBackToForumButton());
    }

    private void wireCreatePostPageActions() {
        createPostPage.getBackButton().addActionListener(e -> centerCards.show(centerPanel, CARD_FORUM));
        createPostPage.getSubmitButton().addActionListener(e -> submitCreatePostFromPage());
        styleSecondaryButton(createPostPage.getBackButton());
        stylePrimaryButton(createPostPage.getSubmitButton());
    }

    private void wirePostDetailPageActions() {
        postDetailPage.getBackToPostsButton().addActionListener(e -> centerCards.show(centerPanel, CARD_FORUM));
        detailAddCommentButton.addActionListener(e -> submitCommentFromDetailPage());
        detailRefreshCommentsButton.addActionListener(e -> reloadCommentsForSelectedPost());
        detailCancelReplyButton.addActionListener(e -> setReplyTarget(null, null));
        detailCancelReplyButton.setMargin(new Insets(2, 8, 2, 8));
        detailCancelReplyButton.setFocusable(false);
        detailPostLikeButton.addActionListener(e -> reactToSelectedPost(true));
        detailPostDislikeButton.addActionListener(e -> reactToSelectedPost(false));
        styleSecondaryButton(postDetailPage.getBackToPostsButton());
        stylePrimaryButton(detailAddCommentButton);
        styleSecondaryButton(detailRefreshCommentsButton);
        styleSecondaryButton(detailCancelReplyButton);
        stylePrimaryButton(detailPostLikeButton);
        styleSecondaryButton(detailPostDislikeButton);
        configureGrowingCommentInput(detailNewCommentArea, postDetailPage.getComposerScroll(), 1, 6);
        setReplyTarget(null, null);
    }

    private void configureGrowingCommentInput(JTextArea input, JScrollPane inputScroll, int minRows, int maxRows) {
        Runnable resize = () -> {
            FontMetrics fm = input.getFontMetrics(input.getFont());
            int lineHeight = fm.getHeight();
            Insets insets = input.getInsets();
            int minRowsSafe = Math.max(1, minRows);
            int maxRowsSafe = Math.max(minRowsSafe, maxRows);

            int viewportWidth = inputScroll.getViewport().getExtentSize().width;
            int availableWidth = viewportWidth - insets.left - insets.right - 6;
            if (availableWidth <= 0) {
                availableWidth = Math.max(120, input.getColumns() * fm.charWidth('m'));
            }

            int visualRows = estimateWrappedLineCount(input.getText(), fm, availableWidth);
            int targetRows = Math.max(minRowsSafe, Math.min(maxRowsSafe, visualRows));
            int minHeight = minRowsSafe * lineHeight + insets.top + insets.bottom + 6;
            int targetHeight = targetRows * lineHeight + insets.top + insets.bottom + 6;

            Dimension size = new Dimension(Integer.MAX_VALUE, targetHeight);
            inputScroll.setPreferredSize(size);
            inputScroll.setMinimumSize(new Dimension(220, minHeight));
            inputScroll.setMaximumSize(size);
            inputScroll.getParent().revalidate();
        };

        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                resize.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resize.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resize.run();
            }
        });
        inputScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resize.run();
            }
        });
        resize.run();
    }

    private static int estimateWrappedLineCount(String text, FontMetrics fm, int availableWidth) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        String[] paragraphs = text.split("\n", -1);
        int lines = 0;
        int safeWidth = Math.max(1, availableWidth);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines += 1;
                continue;
            }
            int paragraphWidth = fm.stringWidth(paragraph);
            lines += Math.max(1, (int) Math.ceil((double) paragraphWidth / safeWidth));
        }
        return Math.max(1, lines);
    }

    private JPanel buildForumPanel() {
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        postList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        postList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ThreadInfo thread) {
                    String createdText = formatTimestamp(thread.getDateCreated());
                    String votesText = " - Likes: " + thread.getLikeCount() + " Dislikes: " + thread.getDislikeCount();
                    if (createdText.isEmpty()) {
                        label.setText(thread.getTitle() + " - by " + thread.getAuthorUsername() + votesText);
                    } else {
                        label.setText("<html><b>" + escapeHtml(thread.getTitle()) + "</b><br/>by "
                                + escapeHtml(thread.getAuthorUsername()) + " - " + escapeHtml(createdText)
                                + " - Likes: " + thread.getLikeCount()
                                + " Dislikes: " + thread.getDislikeCount() + "</html>");
                    }
                }
                return label;
            }
        });
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                reloadPostsForSelectedCategory();
            }
        });
        postList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && postList.locationToIndex(e.getPoint()) >= 0) {
                    openSelectedPost();
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        panel.setBackground(BG_APP);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(categoryList),
                new JScrollPane(postList));
        split.setResizeWeight(0.35);
        split.setBorder(BorderFactory.createLineBorder(SPLIT_BORDER));
        split.setBackground(BG_PANEL);
        categoryList.setBackground(BG_PANEL);
        postList.setBackground(BG_PANEL);
        categoryList.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        postList.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        south.setBackground(BG_APP);
        JButton reload = new JButton("Reload categories");
        reload.addActionListener(e -> reloadCategories());
        JButton makePost = new JButton("Make post");
        makePost.addActionListener(e -> makePost());
        JButton viewPost = new JButton("Open selected post");
        viewPost.addActionListener(e -> openSelectedPost());
        styleSecondaryButton(reload);
        stylePrimaryButton(makePost);
        styleSecondaryButton(viewPost);
        south.add(reload);
        south.add(makePost);
        south.add(viewPost);

        panel.add(split, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void stylePrimaryButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_HOVER_TONE, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }

    private void styleSecondaryButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setBackground(ACCENT_ALT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(NAVY_PRIMARY, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }

    /**
     * Called after a successful login so the header updates immediately.
     */
    public void refreshAfterLogin() {
        resetProfilePageForUserSwitch();
        pendingProfileComboUser = null;
        updateUiForSession();
        reloadCategories();
    }

    private void updateUiForSession() {
        accountButtonPanel.removeAll();
        if (forumService.getSession().isLoggedIn()) {
            accountButtonPanel.add(homeButton);
            accountButtonPanel.add(profileButton);
            accountButtonPanel.add(logoutButton);
            accountButtonPanel.add(accountAvatarLabel);
            refreshHeaderForCurrentUser();
            startProfileWarmupIfNeeded();
            centerCards.show(centerPanel, CARD_FORUM);
        } else {
            accountAvatarLabel.setIcon(null);
            accountButtonPanel.add(loginButton);
            accountButtonPanel.add(signupButton);
            statusLabel.setText("Not logged in");
            centerCards.show(centerPanel, CARD_HOME);
        }
        accountButtonPanel.revalidate();
        accountButtonPanel.repaint();
    }

    /**
     * Loads avatar option tables at startup.
     * This helps the Profile page open faster later.
     */
    private void startCatalogWarmupOnStartup() {
        if (avatarOptionsLoaded || avatarWarmupInProgress) {
            return;
        }
        avatarWarmupInProgress = true;
        SwingWorker<List<List<AvatarOption>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<List<AvatarOption>> doInBackground() throws Exception {
                List<List<AvatarOption>> result = new ArrayList<>();
                result.add(forumService.loadHeadpieces());
                result.add(forumService.loadClothing());
                result.add(forumService.loadAccessories());
                return result;
            }

            @Override
            protected void done() {
                avatarWarmupInProgress = false;
                if (avatarOptionsLoaded) {
                    return;
                }
                try {
                    List<List<AvatarOption>> loaded = get();
                    cachedHeadpieces = loaded.get(0);
                    cachedClothing = loaded.get(1);
                    cachedAccessories = loaded.get(2);
                    avatarOptionsLoaded = true;
                    if (forumService.getSession().isLoggedIn()) {
                        refreshHeaderForCurrentUser();
                    }
                } catch (Exception ignored) {
                    // If loading fails now, try again when Profile opens.
                }
            }
        };
        worker.execute();
    }

    /**
     * Loads profile data in the background after login so first profile open feels instant.
     * This does not change behavior; it only moves first-load work earlier.
     */
    private void startProfileWarmupIfNeeded() {
        if (!forumService.getSession().isLoggedIn() || avatarOptionsLoaded || avatarWarmupInProgress) {
            return;
        }
        avatarWarmupInProgress = true;
        ForumUser currentUser = forumService.getSession().getCurrentUser();
        // SwingWorker runs this in the background so the UI does not freeze.
        SwingWorker<List<List<AvatarOption>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<List<AvatarOption>> doInBackground() throws Exception {
                List<List<AvatarOption>> result = new ArrayList<>();
                result.add(forumService.loadHeadpieces());
                result.add(forumService.loadClothing());
                result.add(forumService.loadAccessories());
                if (currentUser != null) {
                    buildAvatarIcon(currentUser, 36);
                    buildAvatarIcon(currentUser, 120);
                }
                return result;
            }

            @Override
            protected void done() {
                avatarWarmupInProgress = false;
                if (!forumService.getSession().isLoggedIn() || avatarOptionsLoaded) {
                    return;
                }
                try {
                    List<List<AvatarOption>> loaded = get();
                    cachedHeadpieces = loaded.get(0);
                    cachedClothing = loaded.get(1);
                    cachedAccessories = loaded.get(2);
                    avatarOptionsLoaded = true;
                    refreshHeaderForCurrentUser();
                    finishPendingProfileComboLoad();
                } catch (Exception ignored) {
                    // If this fails, reloadAvatarCombos will try again later.
                }
            }
        };
        worker.execute();
    }

    private void showProfile() {
        if (!forumService.getSession().isLoggedIn()) {
            return;
        }
        ForumUser currentUser = forumService.getSession().getCurrentUser();
        resetProfileCombosOnly();
        profileAvatarLabel.setIcon(buildAvatarIcon(currentUser, 120));
        refreshProfileView();
        centerCards.show(centerPanel, CARD_PROFILE);
    }

    private void refreshProfileView() {
        if (!forumService.getSession().isLoggedIn()) {
            return;
        }
        ForumUser u = forumService.getSession().getCurrentUser();
        String username = u.getUsername();
        profileUsernameLabel.setText("Username: " + username + "  \u2022  Level " + u.getLevel() + "  \u2022  XP " + u.getXpTotal());
        profileUsernameField.setText(username);
        reloadAvatarCombos(u);
    }

    /**
     * Saves username + avatar parts together, runs the DB work in the background so the UI stays
     * responsive, then jumps back to the forum view.
     */
    private void updateProfileFromForm() {
        if (!forumService.getSession().isLoggedIn()) {
            return;
        }
        ForumUser current = forumService.getSession().getCurrentUser();
        String proposedUsername = profileUsernameField.getText();
        AvatarOption headpiece = (AvatarOption) profileHeadpieceCombo.getSelectedItem();
        AvatarOption clothing = (AvatarOption) profileClothingCombo.getSelectedItem();
        AvatarOption accessory = (AvatarOption) profileAccessoryCombo.getSelectedItem();
        // Store null for "None" options so the avatar drawing skips that slot.
        Long headpieceId = isNoneOption(headpiece) ? null : (headpiece == null ? null : headpiece.getId());
        Long clothingId = isNoneOption(clothing) ? null : (clothing == null ? null : clothing.getId());
        Long accessoryId = isNoneOption(accessory) ? null : (accessory == null ? null : accessory.getId());
        boolean usernameChanged = proposedUsername != null
                && !proposedUsername.trim().equals(current.getUsername());

        setProfileBusy(true);
        // Run DB updates in the background so the GUI does not freeze.
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                if (usernameChanged && !forumService.updateCurrentUsername(proposedUsername)) {
                    return "Could not update username (name may be taken or invalid).";
                }
                if (!forumService.updateCurrentAvatarSelection(headpieceId, clothingId, accessoryId)) {
                    return "Could not save avatar.";
                }
                return null;
            }

            @Override
            protected void done() {
                setProfileBusy(false);
                try {
                    String err = get();
                    if (err != null) {
                        JOptionPane.showMessageDialog(MainWindow.this, err, "Profile",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    // Update the top-bar avatar right after save.
                    accountAvatarLabel.setIcon(profileAvatarLabel.getIcon());
                    refreshHeaderForCurrentUser();
                    centerCards.show(centerPanel, CARD_FORUM);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(),
                            "Database error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void setProfileBusy(boolean busy) {
        profileUsernameField.setEnabled(!busy);
        profileHeadpieceCombo.setEnabled(!busy);
        profileClothingCombo.setEnabled(!busy);
        profileAccessoryCombo.setEnabled(!busy);
        setCursor(busy
                ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR)
                : java.awt.Cursor.getDefaultCursor());
    }

    private void reloadAvatarCombos(ForumUser u) {
        profileCombosReady = false;
        profileCombosOwnerUserId = -1L;
        if (avatarOptionsLoaded) {
            applyAvatarCombos(u);
            return;
        }
        if (avatarWarmupInProgress) {
            pendingProfileComboUser = u;
            profileHeadpieceCombo.setEnabled(false);
            profileClothingCombo.setEnabled(false);
            profileAccessoryCombo.setEnabled(false);
            return;
        }
        // Data not ready yet, so load it in the background.
        profileHeadpieceCombo.setEnabled(false);
        profileClothingCombo.setEnabled(false);
        profileAccessoryCombo.setEnabled(false);
        SwingWorker<List<List<AvatarOption>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<List<AvatarOption>> doInBackground() throws Exception {
                List<List<AvatarOption>> result = new ArrayList<>();
                result.add(forumService.loadHeadpieces());
                result.add(forumService.loadClothing());
                result.add(forumService.loadAccessories());
                return result;
            }

            @Override
            protected void done() {
                profileHeadpieceCombo.setEnabled(true);
                profileClothingCombo.setEnabled(true);
                profileAccessoryCombo.setEnabled(true);
                try {
                    List<List<AvatarOption>> loaded = get();
                    cachedHeadpieces = loaded.get(0);
                    cachedClothing = loaded.get(1);
                    cachedAccessories = loaded.get(2);
                    avatarOptionsLoaded = true;
                    if (forumService.getSession().isLoggedIn()
                            && forumService.getSession().getCurrentUser().getId() == u.getId()) {
                        applyAvatarCombos(u);
                    }
                } catch (Exception ex) {
                    avatarOptionsLoaded = false;
                    if (forumService.getSession().isLoggedIn()
                            && forumService.getSession().getCurrentUser().getId() == u.getId()) {
                        profileAvatarLabel.setIcon(buildAvatarIcon(u, 120));
                        JOptionPane.showMessageDialog(MainWindow.this,
                                "Avatar options could not be loaded. "
                                        + "Ensure avatar tables exist (see db_schema.sql).",
                                "Avatar catalog", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        };
        worker.execute();
    }

    private void applyAvatarCombos(ForumUser u) {
        if (!forumService.getSession().isLoggedIn()
                || forumService.getSession().getCurrentUser().getId() != u.getId()) {
            return;
        }
        List<AvatarOption> allowedHeadpieces = filterUnlockedOptions(cachedHeadpieces, u.getLevel(), u.getAvatarHeadpieceId());
        List<AvatarOption> allowedClothing = filterUnlockedOptions(cachedClothing, u.getLevel(), u.getAvatarClothingId());
        List<AvatarOption> allowedAccessories = filterUnlockedOptions(cachedAccessories, u.getLevel(), u.getAvatarAccessoryId());
        updatingCombos = true;
        try {
            profileHeadpieceCombo.setModel(new DefaultComboBoxModel<>(allowedHeadpieces.toArray(new AvatarOption[0])));
            profileClothingCombo.setModel(new DefaultComboBoxModel<>(allowedClothing.toArray(new AvatarOption[0])));
            profileAccessoryCombo.setModel(new DefaultComboBoxModel<>(allowedAccessories.toArray(new AvatarOption[0])));
            selectAvatarOption(profileHeadpieceCombo, u.getAvatarHeadpieceId());
            selectAvatarOption(profileClothingCombo, u.getAvatarClothingId());
            selectAvatarOption(profileAccessoryCombo, u.getAvatarAccessoryId());
        } finally {
            updatingCombos = false;
        }
        profileCombosOwnerUserId = u.getId();
        profileCombosReady = true;
        profileHeadpieceCombo.setEnabled(true);
        profileClothingCombo.setEnabled(true);
        profileAccessoryCombo.setEnabled(true);
        refreshProfileAvatarPreview();
    }

    /**
     * Clears profile avatar widgets when the logged-in user changes.
     */
    private void resetProfilePageForUserSwitch() {
        pendingProfileComboUser = null;
        profileCombosReady = false;
        profileCombosOwnerUserId = -1L;
        resetProfileCombosOnly();
        profileAvatarLabel.setIcon(null);
        profileUsernameLabel.setText(" ");
        profileUsernameField.setText("");
    }

    private void resetProfileCombosOnly() {
        profileCombosReady = false;
        profileCombosOwnerUserId = -1L;
        updatingCombos = true;
        try {
            profileHeadpieceCombo.setModel(new DefaultComboBoxModel<>());
            profileClothingCombo.setModel(new DefaultComboBoxModel<>());
            profileAccessoryCombo.setModel(new DefaultComboBoxModel<>());
        } finally {
            updatingCombos = false;
        }
    }

    private void finishPendingProfileComboLoad() {
        if (pendingProfileComboUser == null || !forumService.getSession().isLoggedIn()) {
            pendingProfileComboUser = null;
            return;
        }
        ForumUser pending = pendingProfileComboUser;
        pendingProfileComboUser = null;
        if (forumService.getSession().getCurrentUser().getId() == pending.getId()) {
            applyAvatarCombos(pending);
        }
    }

    /**
     * Updates the large profile preview from the current combo selection (before or after save).
     */
    private void refreshProfileAvatarPreview() {
        if (updatingCombos || !profileCombosReady || !forumService.getSession().isLoggedIn()) {
            return;
        }
        ForumUser base = forumService.getSession().getCurrentUser();
        if (base.getId() != profileCombosOwnerUserId) {
            return;
        }
        AvatarOption head = (AvatarOption) profileHeadpieceCombo.getSelectedItem();
        AvatarOption clothes = (AvatarOption) profileClothingCombo.getSelectedItem();
        AvatarOption acc = (AvatarOption) profileAccessoryCombo.getSelectedItem();
        Long headId = isNoneOption(head) ? null : (head == null ? null : head.getId());
        Long clothingId = isNoneOption(clothes) ? null : (clothes == null ? null : clothes.getId());
        Long accessoryId = isNoneOption(acc) ? null : (acc == null ? null : acc.getId());
        ForumUser preview = new ForumUser(base.getId(), base.getUsername(), base.getPasswordHash(),
                headId, clothingId, accessoryId);
        profileAvatarLabel.setIcon(buildAvatarIcon(preview, 120));
    }

    private void selectAvatarOption(JComboBox<AvatarOption> combo, Long id) {
        if (id == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            AvatarOption opt = combo.getItemAt(i);
            if (opt != null && opt.getId() == id.longValue()) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private Icon buildAvatarIcon(ForumUser user, int size) {
        if (user != null && forumService.isAiBotUsername(user.getUsername())) {
            return profilePage.buildBotAvatarIcon(size);
        }
        return profilePage.buildAvatarIcon(user, size, cachedHeadpieces, cachedClothing, cachedAccessories);
    }

    private void refreshHeaderForCurrentUser() {
        if (!forumService.getSession().isLoggedIn()) {
            statusLabel.setText("Not logged in");
            accountAvatarLabel.setIcon(null);
            return;
        }
        ForumUser u = forumService.getSession().getCurrentUser();
        statusLabel.setText("Logged in as " + u.getUsername() + " (Level " + u.getLevel() + ", XP " + u.getXpTotal() + ")");
        accountAvatarLabel.setIcon(buildAvatarIcon(u, 36));
        accountButtonPanel.revalidate();
        accountButtonPanel.repaint();
    }

    /**
     * Verifies JDBC settings by opening and closing a connection.
     */
    public void testConnection() {
        if (!ConnectionManager.isConfigured()) {
            JOptionPane.showMessageDialog(this,
                    "Configure forum.properties first (db.url, db.user, db.password).",
                    "Not configured",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (Connection c = ConnectionManager.getConnection()) {
            if (c != null && !c.isClosed()) {
                JOptionPane.showMessageDialog(this, "Connection OK.", "Database",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Connection failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Refreshes the category list from TiDB using an in-memory sort in {@link ForumService}.
     */
    public void reloadCategories() {
        if (!forumService.getSession().isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Log in first to load categories.", "Not logged in",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!ConnectionManager.isConfigured()) {
            JOptionPane.showMessageDialog(this, "Missing or invalid forum.properties.", "Database",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long previouslySelectedId = categoryList.getSelectedValue() == null
                ? null : categoryList.getSelectedValue().getId();
        categoryModel.clear();
        postModel.clear();
        statusLabel.setText("Loading categories...");

        SwingWorker<List<CategoryInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CategoryInfo> doInBackground() throws Exception {
                return forumService.loadCategoriesSorted();
            }

            @Override
            protected void done() {
                try {
                    List<CategoryInfo> sorted = get();
                    for (CategoryInfo cat : sorted) {
                        categoryModel.addElement(cat);
                    }
                    populateCreatePostCategoryDropdown();
                    if (!sorted.isEmpty()) {
                        int selectionIndex = 0;
                        if (previouslySelectedId != null) {
                            for (int i = 0; i < sorted.size(); i++) {
                                if (sorted.get(i).getId() == previouslySelectedId.longValue()) {
                                    selectionIndex = i;
                                    break;
                                }
                            }
                        }
                        // setSelectedIndex fires the listSelectionListener which calls reloadPostsForSelectedCategory.
                        categoryList.setSelectedIndex(selectionIndex);
                    } else {
                        statusLabel.setText("No categories found.");
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(), "Query failed",
                            JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Failed to load categories.");
                }
            }
        };
        worker.execute();
    }

    private void makePost() {
        if (!forumService.getSession().isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Log in first to make a post.", "Not logged in",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        populateCreatePostCategoryDropdown();
        CategoryInfo selected = categoryList.getSelectedValue();
        if (selected != null) {
            createPostPage.getCategoryCombo().setSelectedItem(selected);
        }
        createPostPage.resetDraft();
        centerCards.show(centerPanel, CARD_CREATE_POST);
    }

    private void populateCreatePostCategoryDropdown() {
        DefaultComboBoxModel<CategoryInfo> model = new DefaultComboBoxModel<>();
        for (int i = 0; i < categoryModel.size(); i++) {
            model.addElement(categoryModel.get(i));
        }
        createPostPage.setCategories(model);
    }

    private void submitCreatePostFromPage() {
        if (!forumService.getSession().isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Log in first to make a post.", "Not logged in",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        CategoryInfo selectedCategory = (CategoryInfo) createPostPage.getCategoryCombo().getSelectedItem();
        if (selectedCategory == null) {
            JOptionPane.showMessageDialog(this, "Select a category first.", "No category selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String title = createPostPage.getTitleField().getText();
        String content = createPostPage.getBodyArea().getText();
        long categoryId = selectedCategory.getId();
        String categoryName = selectedCategory.getName();

        createPostPage.getSubmitButton().setEnabled(false);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return forumService.createPost(categoryId, title, content);
            }

            @Override
            protected void done() {
                createPostPage.getSubmitButton().setEnabled(true);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        // Keep forum list in sync with where the new post was submitted.
                        selectCategoryInList(categoryId);
                        reloadPostsForSelectedCategory();
                        centerCards.show(centerPanel, CARD_FORUM);
                        JOptionPane.showMessageDialog(MainWindow.this,
                                "Post created in " + categoryName + ".", "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MainWindow.this,
                                "Could not create post. Check title/content and try again.",
                                "Post failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(), "Database error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void selectCategoryInList(long categoryId) {
        for (int i = 0; i < categoryModel.size(); i++) {
            if (categoryModel.get(i).getId() == categoryId) {
                categoryList.setSelectedIndex(i);
                return;
            }
        }
    }

    private void reloadPostsForSelectedCategory() {
        postModel.clear();
        CategoryInfo selected = categoryList.getSelectedValue();
        if (!forumService.getSession().isLoggedIn() || selected == null || !ConnectionManager.isConfigured()) {
            return;
        }
        long categoryId = selected.getId();
        long requestSequence = ++postsReloadSequence;
        statusLabel.setText("Loading posts...");

        SwingWorker<List<ThreadInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ThreadInfo> doInBackground() throws Exception {
                return forumService.loadPostsForCategory(categoryId);
            }

            @Override
            protected void done() {
                try {
                    if (requestSequence != postsReloadSequence) {
                        return;
                    }
                    List<ThreadInfo> posts = get();
                    for (ThreadInfo post : posts) {
                        postModel.addElement(post);
                    }
                    if (forumService.getSession().isLoggedIn()) {
                        statusLabel.setText("Logged in as " + forumService.getSession().getCurrentUser().getUsername());
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(), "Query failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void openSelectedPost() {
        if (!forumService.getSession().isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Log in first to view posts.", "Not logged in",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        ThreadInfo selected = postList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a post first.", "No post selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        showPostDetail(selected);
    }

    private void showPostDetail(ThreadInfo thread) {
        selectedPostForDetail = thread;
        aiPendingAutoRefreshRemaining = 0;
        detailPostTitleLabel.setText(thread.getTitle());
        String createdText = formatTimestamp(thread.getDateCreated());
        if (createdText.isEmpty()) {
            detailPostMetaLabel.setText("by " + thread.getAuthorUsername());
        } else {
            detailPostMetaLabel.setText("by " + thread.getAuthorUsername() + " - " + createdText);
        }
        ForumUser postAuthorAvatarUser = new ForumUser(
                0L,
                thread.getAuthorUsername(),
                "",
                thread.getAuthorAvatarHeadpieceId(),
                thread.getAuthorAvatarClothingId(),
                thread.getAuthorAvatarAccessoryId());
        detailPostAuthorAvatarLabel.setIcon(buildAvatarIcon(postAuthorAvatarUser, 36));
        detailPostBodyArea.setText(thread.getContent());
        detailPostVotesLabel.setText("Likes: " + thread.getLikeCount() + "  Dislikes: " + thread.getDislikeCount());
        detailPostBodyArea.setCaretPosition(0);
        postDetailPage.refreshLayoutSizes();
        detailNewCommentArea.setText("");
        setReplyTarget(null, null);
        detailCommentsListPanel.removeAll();
        JLabel loading = new JLabel("Loading comments...");
        loading.setForeground(TEXT_MUTED);
        loading.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        loading.setAlignmentX(LEFT_ALIGNMENT);
        detailCommentsListPanel.add(loading);
        detailCommentsListPanel.revalidate();
        detailCommentsListPanel.repaint();
        resetDetailScrollToTopPending = true;
        if (detailPageScroll != null) {
            SwingUtilities.invokeLater(() -> detailPageScroll.getVerticalScrollBar().setValue(0));
        }
        centerCards.show(centerPanel, CARD_POST_DETAIL);
        reloadCommentsForSelectedPost();
    }

    private void reloadCommentsForSelectedPost() {
        if (selectedPostForDetail == null) {
            aiPendingAutoRefreshRemaining = 0;
            detailCommentsListPanel.removeAll();
            detailCommentsListPanel.add(new JLabel("No post selected."));
            detailCommentsListPanel.revalidate();
            detailCommentsListPanel.repaint();
            return;
        }
        long threadId = selectedPostForDetail.getId();
        detailRefreshCommentsButton.setEnabled(false);

        SwingWorker<List<CommentInfo>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CommentInfo> doInBackground() throws Exception {
                return forumService.loadCommentsForThread(threadId);
            }

            @Override
            protected void done() {
                detailRefreshCommentsButton.setEnabled(true);
                try {
                    List<CommentInfo> loaded = get();
                    rebuildCommentsList(loaded);
                    if (resetDetailScrollToTopPending && detailPageScroll != null) {
                        SwingUtilities.invokeLater(() -> detailPageScroll.getVerticalScrollBar().setValue(0));
                    }
                    resetDetailScrollToTopPending = false;
                    handlePendingAiRefresh(loaded);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(), "Query failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void handlePendingAiRefresh(List<CommentInfo> loaded) {
        if (!containsPendingAiPlaceholder(loaded)) {
            aiPendingAutoRefreshRemaining = 0;
            return;
        }
        if (aiPendingAutoRefreshRemaining <= 0) {
            aiPendingAutoRefreshRemaining = AI_AUTO_REFRESH_MAX_TRIES;
        }
        if (aiPendingAutoRefreshRemaining-- <= 0) {
            return;
        }
        Timer timer = new Timer(AI_AUTO_REFRESH_DELAY_MS, e -> {
            if (selectedPostForDetail != null) {
                reloadCommentsForSelectedPost();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static boolean containsPendingAiPlaceholder(List<CommentInfo> loaded) {
        if (loaded == null || loaded.isEmpty()) {
            return false;
        }
        for (CommentInfo c : loaded) {
            String body = c.getContent();
            if (body != null && AI_PENDING_PLACEHOLDER.equals(body.trim())) {
                return true;
            }
        }
        return false;
    }

    private void submitCommentFromDetailPage() {
        if (selectedPostForDetail == null) {
            return;
        }
        String text = detailNewCommentArea.getText();
        if (text == null || text.isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter comment text.", "Empty comment",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        long threadId = selectedPostForDetail.getId();
        String trimmed = text.trim();
        Long parentCommentId = replyTargetCommentId;
        detailAddCommentButton.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return forumService.addComment(threadId, parentCommentId, trimmed);
            }

            @Override
            protected void done() {
                detailAddCommentButton.setEnabled(true);
                try {
                    if (Boolean.TRUE.equals(get())) {
                        detailNewCommentArea.setText("");
                        setReplyTarget(null, null);
                        reloadCommentsForSelectedPost();
                    } else {
                        String detail = forumService.consumeLastUserMessage();
                        String message = (detail == null || detail.isBlank())
                                ? "Could not add comment."
                                : detail;
                        JOptionPane.showMessageDialog(MainWindow.this, message, "Comment failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(), "Database error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void rebuildCommentsList(List<CommentInfo> list) {
        detailCommentsListPanel.removeAll();
        if (list.isEmpty()) {
            JLabel empty = new JLabel("No comments yet.");
            empty.setForeground(TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            empty.setAlignmentX(LEFT_ALIGNMENT);
            detailCommentsListPanel.add(empty);
            postDetailPage.refreshCommentsPanelHeight();
            return;
        }
        Map<Long, CommentInfo> byId = new HashMap<>();
        Map<Long, List<CommentInfo>> childrenByParent = new HashMap<>();
        List<CommentInfo> roots = new ArrayList<>();
        for (CommentInfo c : list) {
            byId.put(c.getId(), c);
        }
        for (CommentInfo c : list) {
            Long parentId = c.getParentCommentId();
            if (parentId == null || !byId.containsKey(parentId)) {
                roots.add(c);
            } else {
                childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(c);
            }
        }
        Set<Long> rendered = new HashSet<>();
        for (CommentInfo root : roots) {
            addCommentBranch(root, 0, childrenByParent, rendered);
        }
        // Safety check: if any comment was missed, still render it.
        for (CommentInfo c : list) {
            if (!rendered.contains(c.getId())) {
                addCommentBranch(c, 0, childrenByParent, rendered);
            }
        }
        postDetailPage.refreshCommentsPanelHeight();
    }

    private void addCommentBranch(CommentInfo comment, int depth, Map<Long, List<CommentInfo>> childrenByParent,
            Set<Long> rendered) {
        if (!rendered.add(comment.getId())) {
            return;
        }
        int indent = Math.min(220, Math.max(0, depth) * 20);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(LEFT_ALIGNMENT);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, indent, 0, 0));
        wrapper.add(buildCommentCard(comment), BorderLayout.CENTER);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height));
        detailCommentsListPanel.add(wrapper);
        detailCommentsListPanel.add(Box.createVerticalStrut(6));

        List<CommentInfo> children = childrenByParent.get(comment.getId());
        if (children == null) {
            return;
        }
        for (CommentInfo child : children) {
            addCommentBranch(child, depth + 1, childrenByParent, rendered);
        }
    }

    private JPanel buildCommentCard(CommentInfo c) {
        JPanel card = new JPanel(new BorderLayout(10, 4));
        card.setBackground(BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SPLIT_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        card.setAlignmentX(LEFT_ALIGNMENT);

        String when = formatTimestamp(c.getDateCreated());
        JLabel meta = new JLabel(when.isEmpty()
                ? c.getAuthorUsername()
                : c.getAuthorUsername() + " - " + when);
        meta.setForeground(TEXT_MUTED);

        JButton replyButton = new JButton("Reply");
        replyButton.setMargin(new Insets(2, 8, 2, 8));
        replyButton.setFocusable(false);
        styleSecondaryButton(replyButton);
        replyButton.addActionListener(e -> {
            setReplyTarget(c.getId(), c.getAuthorUsername());
            detailNewCommentArea.requestFocusInWindow();
        });
        JButton likeButton = new JButton("Like (" + c.getLikeCount() + ")");
        likeButton.setMargin(new Insets(2, 8, 2, 8));
        likeButton.setFocusable(false);
        stylePrimaryButton(likeButton);
        likeButton.addActionListener(e -> reactToComment(c.getId(), true));
        JButton dislikeButton = new JButton("Dislike (" + c.getDislikeCount() + ")");
        dislikeButton.setMargin(new Insets(2, 8, 2, 8));
        dislikeButton.setFocusable(false);
        styleSecondaryButton(dislikeButton);
        dislikeButton.addActionListener(e -> reactToComment(c.getId(), false));

        JPanel metaRow = new JPanel(new BorderLayout(6, 0));
        metaRow.setOpaque(false);
        metaRow.add(meta, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(likeButton);
        actions.add(dislikeButton);
        actions.add(replyButton);
        metaRow.add(actions, BorderLayout.EAST);

        // Use a non-editable JTextArea for the body so long text wraps naturally.
        JTextArea body = new JTextArea(c.getContent() == null ? "" : c.getContent().trim());
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setBorder(null);
        // Keep short comments to one-line height; wrap naturally for longer comments.
        body.setRows(1);

        ForumUser authorAvatarUser = new ForumUser(
                0L,
                c.getAuthorUsername(),
                "",
                c.getAuthorAvatarHeadpieceId(),
                c.getAuthorAvatarClothingId(),
                c.getAuthorAvatarAccessoryId());
        JLabel avatar = new JLabel(buildAvatarIcon(authorAvatarUser, 34));
        avatar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));

        JPanel bodyRow = new JPanel(new BorderLayout(6, 0));
        bodyRow.setOpaque(false);
        bodyRow.add(avatar, BorderLayout.WEST);
        bodyRow.add(body, BorderLayout.CENTER);

        card.add(metaRow, BorderLayout.NORTH);
        card.add(bodyRow, BorderLayout.CENTER);
        // Keep full-width cards while locking height to their natural content height.
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private void setReplyTarget(Long commentId, String authorUsername) {
        replyTargetCommentId = commentId;
        boolean active = commentId != null;
        if (active) {
            detailReplyContextLabel.setText("Replying to @" + (authorUsername == null ? "user" : authorUsername));
        } else {
            detailReplyContextLabel.setText(" ");
        }
        detailCancelReplyButton.setVisible(active);
        detailReplyContextLabel.setVisible(active);
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        return uiDateTimeFormatter.format(ts.toInstant());
    }

    /** Returns true when the user has "None" selected, which should be stored as null. */
    private static boolean isNoneOption(AvatarOption opt) {
        return opt != null && "None".equalsIgnoreCase(opt.getDisplayName());
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static List<AvatarOption> filterUnlockedOptions(List<AvatarOption> source, int level, Long selectedId) {
        List<AvatarOption> out = new ArrayList<>();
        long selected = selectedId == null ? -1L : selectedId.longValue();
        for (AvatarOption opt : source) {
            if (opt.getUnlockLevel() <= level || opt.getId() == selected) {
                out.add(opt);
            }
        }
        return out;
    }

    private void reactToSelectedPost(boolean like) {
        if (!forumService.getSession().isLoggedIn() || selectedPostForDetail == null) {
            return;
        }
        detailPostLikeButton.setEnabled(false);
        detailPostDislikeButton.setEnabled(false);
        long threadId = selectedPostForDetail.getId();
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return forumService.reactToThread(threadId, like);
            }

            @Override
            protected void done() {
                detailPostLikeButton.setEnabled(true);
                detailPostDislikeButton.setEnabled(true);
                try {
                    get();
                    reloadPostsForSelectedCategory();
                    CategoryInfo selectedCategory = categoryList.getSelectedValue();
                    if (selectedPostForDetail != null && selectedCategory != null) {
                        forumService.loadPostsForCategory(selectedCategory.getId()).stream()
                                .filter(t -> t.getId() == selectedPostForDetail.getId())
                                .findFirst()
                                .ifPresent(t -> {
                                    selectedPostForDetail = t;
                                    detailPostVotesLabel.setText(
                                            "Likes: " + t.getLikeCount() + "  Dislikes: " + t.getDislikeCount());
                                });
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(), "Vote failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void reactToComment(long commentId, boolean like) {
        if (!forumService.getSession().isLoggedIn()) {
            return;
        }
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return forumService.reactToComment(commentId, like);
            }

            @Override
            protected void done() {
                try {
                    get();
                    reloadCommentsForSelectedPost();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    JOptionPane.showMessageDialog(MainWindow.this, cause.getMessage(), "Vote failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
