package forum.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;

import forum.db.ConnectionManager;
import forum.model.CategoryInfo;
import forum.model.ThreadInfo;
import forum.service.ForumService;

/**
 * Primary Swing shell: menus, category list, and database sanity checks.
 */
public class MainWindow extends JFrame {

    private static final String CARD_HOME = "home";
    private static final String CARD_FORUM = "forum";
    /** Canyon Crest Academy theme (Ravens colors): red + black with neutral background. */
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

    private final ForumService forumService;
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.LEFT);
    private final DefaultListModel<CategoryInfo> categoryModel = new DefaultListModel<>();
    private final JList<CategoryInfo> categoryList = new JList<>(categoryModel);
    private final DefaultListModel<ThreadInfo> postModel = new DefaultListModel<>();
    private final JList<ThreadInfo> postList = new JList<>(postModel);
    private final JButton loginButton = new JButton("Log in");
    private final JButton signupButton = new JButton("Sign up");
    private final JButton profileButton = new JButton("Profile");
    private final JButton logoutButton = new JButton("Log out");
    private final JPanel accountButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
    private final CardLayout centerCards = new CardLayout();
    private final JPanel centerPanel = new JPanel(centerCards);

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
        updateUiForSession();
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
        profileButton.addActionListener(e -> showProfile());
        logoutButton.addActionListener(e -> {
            forumService.getSession().logout();
            updateUiForSession();
            categoryModel.clear();
            postModel.clear();
        });

        topBar.add(accountButtonPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);
        stylePrimaryButton(loginButton);
        styleSecondaryButton(signupButton);
        styleSecondaryButton(profileButton);
        styleSecondaryButton(logoutButton);
    }

    private void buildContent() {
        JPanel homePanel = buildHomePanel();
        JPanel forumPanel = buildForumPanel();
        centerPanel.add(homePanel, CARD_HOME);
        centerPanel.add(forumPanel, CARD_FORUM);
        centerPanel.setBackground(BG_APP);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(BG_APP);
        add(centerPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
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

        JLabel imagePlaceholder = new JLabel("Homepage image placeholder (add your picture later)", SwingConstants.CENTER);
        imagePlaceholder.setOpaque(true);
        imagePlaceholder.setBackground(PLACEHOLDER_BG);
        imagePlaceholder.setForeground(NAVY_MID);
        imagePlaceholder.setBorder(BorderFactory.createDashedBorder(PLACEHOLDER_BORDER));
        imagePlaceholder.setPreferredSize(new Dimension(460, 220));
        imagePlaceholder.setMaximumSize(new Dimension(460, 220));
        imagePlaceholder.setMinimumSize(new Dimension(460, 220));
        imagePlaceholder.setAlignmentX(CENTER_ALIGNMENT);

        home.add(welcome);
        home.add(Box.createVerticalStrut(10));
        home.add(subtitle);
        home.add(Box.createVerticalStrut(28));
        home.add(imagePlaceholder);
        home.add(Box.createVerticalGlue());
        return home;
    }

    private JPanel buildForumPanel() {
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        postList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                reloadPostsForSelectedCategory();
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
        JButton openPost = new JButton("Open post");
        openPost.addActionListener(e -> openSelectedPost());
        JButton testConnection = new JButton("Test connection");
        testConnection.addActionListener(e -> testConnection());
        styleSecondaryButton(reload);
        stylePrimaryButton(makePost);
        styleSecondaryButton(openPost);
        styleSecondaryButton(testConnection);
        south.add(reload);
        south.add(makePost);
        south.add(openPost);
        south.add(testConnection);

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
        updateUiForSession();
        reloadCategories();
    }

    private void updateUiForSession() {
        accountButtonPanel.removeAll();
        if (forumService.getSession().isLoggedIn()) {
            accountButtonPanel.add(profileButton);
            accountButtonPanel.add(logoutButton);
            statusLabel.setText("Logged in as " + forumService.getSession().getCurrentUser().getUsername());
            centerCards.show(centerPanel, CARD_FORUM);
        } else {
            accountButtonPanel.add(loginButton);
            accountButtonPanel.add(signupButton);
            statusLabel.setText("Not logged in");
            centerCards.show(centerPanel, CARD_HOME);
        }
        accountButtonPanel.revalidate();
        accountButtonPanel.repaint();
    }

    private void showProfile() {
        if (!forumService.getSession().isLoggedIn()) {
            return;
        }
        String username = forumService.getSession().getCurrentUser().getUsername();
        JOptionPane.showMessageDialog(this, "Username: " + username, "Profile", JOptionPane.INFORMATION_MESSAGE);
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
        Long previouslySelectedId = categoryList.getSelectedValue() == null ? null : categoryList.getSelectedValue().getId();
        categoryModel.clear();
        postModel.clear();
        try {
            List<CategoryInfo> sorted = forumService.loadCategoriesSorted();
            for (int i = 0; i < sorted.size(); i++) {
                categoryModel.add(i, sorted.get(i));
            }
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
                categoryList.setSelectedIndex(selectionIndex);
                reloadPostsForSelectedCategory();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Query failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void makePost() {
        if (!forumService.getSession().isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Log in first to make a post.", "Not logged in",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        CategoryInfo selected = categoryList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a category first.", "No category selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String title = JOptionPane.showInputDialog(this, "Post title (max 150 chars):", "Make post",
                JOptionPane.PLAIN_MESSAGE);
        if (title == null) {
            return;
        }

        JTextArea area = new JTextArea(8, 32);
        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(area), "Post content",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            boolean ok = forumService.createPost(selected.getId(), title, area.getText());
            if (ok) {
                reloadPostsForSelectedCategory();
                JOptionPane.showMessageDialog(this, "Post created in " + selected.getName() + ".", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Could not create post. Check title/content and try again.",
                        "Post failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reloadPostsForSelectedCategory() {
        postModel.clear();
        CategoryInfo selected = categoryList.getSelectedValue();
        if (!forumService.getSession().isLoggedIn() || selected == null || !ConnectionManager.isConfigured()) {
            return;
        }
        try {
            List<ThreadInfo> posts = forumService.loadPostsForCategory(selected.getId());
            for (int i = 0; i < posts.size(); i++) {
                postModel.add(i, posts.get(i));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Query failed",
                    JOptionPane.ERROR_MESSAGE);
        }
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
        JTextArea area = new JTextArea(selected.getContent(), 14, 42);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(
                this,
                new JScrollPane(area),
                selected.getTitle() + " - by " + selected.getAuthorUsername(),
                JOptionPane.INFORMATION_MESSAGE);
    }
}
