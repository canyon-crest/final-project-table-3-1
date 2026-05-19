package forum;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import forum.db.CategoryRepository;
import forum.db.CommentRepository;
import forum.db.ConnectionManager;
import forum.db.ThreadRepository;
import forum.db.UserRepository;
import forum.service.ForumService;
import forum.service.ForumSession;
import forum.ui.MainWindow;

/**
 * Entry point for the Swing forum client.
 */
public final class ForumApp {

    private ForumApp() {
    }

    /**
     * Loads local DB settings, then opens the main window on the event dispatch thread.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        installLookAndFeel();

        File props = AppPaths.resolveForumPropertiesFile();
        if (props != null) {
            try {
                ConnectionManager.loadFromFile(props);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not read forum.properties:\n" + ex.getMessage(),
                        "CCA Forum",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else if (!ConnectionManager.loadFromEnvironment()) {
            JOptionPane.showMessageDialog(null,
                    "Missing forum.properties.\n"
                            + "Place forum.properties next to the app EXE/JAR (or working folder),\n"
                            + "or set FORUM_DB_URL, FORUM_DB_USER, FORUM_DB_PASSWORD.",
                    "CCA Forum",
                    JOptionPane.WARNING_MESSAGE);
        }

        SwingUtilities.invokeLater(() -> {
            ForumSession session = new ForumSession();
            ForumService service = new ForumService(
                    session,
                    new UserRepository(),
                    new CategoryRepository(),
                    new ThreadRepository(),
                    new CommentRepository());
            MainWindow window = new MainWindow(service);
            window.setVisible(true);
        });
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException ignored) {
            // Fall back to default L&F
        }
    }
}
