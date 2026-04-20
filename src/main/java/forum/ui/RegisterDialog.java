package forum.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import forum.model.ForumUser;
import forum.service.ForumService;

/**
 * Collects a new username and password and calls {@link ForumService#register(String, String)}.
 */
public class RegisterDialog extends JDialog {

    private final ForumService forumService;
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);

    /**
     * @param owner        parent frame
     * @param forumService persistence layer
     */
    public RegisterDialog(JFrame owner, ForumService forumService) {
        super(owner, "Create account", true);
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
        form.add(new JLabel("Confirm"));
        form.add(confirmField);

        JButton ok = new JButton("Register");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> onRegister());
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel();
        buttons.add(ok);
        buttons.add(cancel);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
    }

    private void onRegister() {
        String user = usernameField.getText().trim();
        char[] p1 = passwordField.getPassword();
        char[] p2 = confirmField.getPassword();
        String pass = new String(p1);
        String confirm = new String(p2);
        java.util.Arrays.fill(p1, '\0');
        java.util.Arrays.fill(p2, '\0');

        if (!ForumUser.isValidUsername(user)) {
            JOptionPane.showMessageDialog(this, "Username cannot be blank and must be 50 chars or less.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be blank.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (forumService.register(user, pass)) {
                JOptionPane.showMessageDialog(this, "Account created. You can log in now.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Could not register (name may be taken or rules failed).",
                        "Register failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
