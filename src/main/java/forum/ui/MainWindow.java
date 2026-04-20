package forum.ui;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import forum.db.ConnectionManager;
import forum.model.CategoryInfo;
import forum.model.ThreadInfo;
import forum.service.ForumService;

/**
 * Primary Swing shell: menus, category list, and database sanity checks.
 */
public class MainWindow extends JFrame {

    private final ForumService forumService;
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final DefaultListModel<CategoryInfo> categoryModel = new DefaultListModel<>();
    private final JList<CategoryInfo> categoryList = new JList<>(categoryModel);
    private final DefaultListModel<ThreadInfo> postModel = new DefaultListModel<>();
    private final JList<ThreadInfo> postList = new JList<>(postModel);

    /**
     * @param forumService application facade (session + repositories)
     */
    public MainWindow(ForumService forumService) {
        super("CCA Forum");
        this.forumService = forumService;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 420);
        setLocationRelativeTo(null);

        buildMenuBar();
        buildContent();
        updateStatusLabel();
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        file.add(exit);

        JMenu account = new JMenu("Account");
        JMenuItem login = new JMenuItem("Log in…");
        login.addActionListener(e -> new LoginDialog(this, forumService).setVisible(true));
        JMenuItem register = new JMenuItem("Register…");
        register.addActionListener(e -> new RegisterDialog(this, forumService).setVisible(true));
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> {
            forumService.getSession().logout();
            updateStatusLabel();
        });
        account.add(login);
        account.add(register);
        account.add(logout);

        JMenu db = new JMenu("Database");
        JMenuItem test = new JMenuItem("Test connection");
        test.addActionListener(e -> testConnection());
        JMenuItem refresh = new JMenuItem("Reload categories");
        refresh.addActionListener(e -> reloadCategories());
        db.add(test);
        db.add(refresh);

        bar.add(file);
        bar.add(account);
        bar.add(db);
        setJMenuBar(bar);
    }

    private void buildContent() {
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        postList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                reloadPostsForSelectedCategory();
            }
        });

        JPanel south = new JPanel();
        JButton reload = new JButton("Reload categories");
        reload.addActionListener(e -> reloadCategories());
        JButton makePost = new JButton("Make post");
        makePost.addActionListener(e -> makePost());
        JButton openPost = new JButton("Open post");
        openPost.addActionListener(e -> openSelectedPost());
        south.add(reload);
        south.add(makePost);
        south.add(openPost);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(categoryList),
                new JScrollPane(postList));
        split.setResizeWeight(0.35);

        add(split, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.NORTH);
        add(south, BorderLayout.SOUTH);
    }

    private void updateStatusLabel() {
        if (forumService.getSession().isLoggedIn()) {
            statusLabel.setText("Logged in as " + forumService.getSession().getCurrentUser().getUsername());
        } else {
            statusLabel.setText("Not logged in — use Account menu");
        }
    }

    /**
     * Called after a successful login so the header updates immediately.
     */
    public void refreshAfterLogin() {
        updateStatusLabel();
        reloadCategories();
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
        if (selected == null || !ConnectionManager.isConfigured()) {
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
