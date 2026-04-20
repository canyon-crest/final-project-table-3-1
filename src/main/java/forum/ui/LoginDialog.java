package forum.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import forum.service.ForumService;

/**
 * Modal login form; updates {@link ForumService#getSession()} on success.
 */
public class LoginDialog extends JDialog {

    private final MainWindow mainWindow;
    private final ForumService forumService;
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    /**
     * @param owner        parent frame (used to refresh the status line after login)
     * @param forumService service used to verify credentials
     */
    public LoginDialog(MainWindow owner, ForumService forumService) {
        super(owner, "Log in", true);
        this.mainWindow = owner;
        this.forumService = forumService;
        buildUi();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Username"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> onOk());
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel();
        buttons.add(ok);
        buttons.add(cancel);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
    }

    private void onOk() {
        String user = usernameField.getText();
        char[] passChars = passwordField.getPassword();
        String pass = new String(passChars);
        java.util.Arrays.fill(passChars, '\0');
        try {
            if (forumService.attemptLogin(user, pass)) {
                mainWindow.refreshAfterLogin();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
