package forum.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import forum.model.CategoryInfo;

/**
 * Dedicated compose page for creating a new thread.
 * Keeps all "create post" UI widgets together so MainWindow only coordinates actions.
 */
public class CreatePostPage extends JPanel {

    private final JComboBox<CategoryInfo> categoryCombo = new JComboBox<>();
    private final JTextField titleField = new JTextField(42);
    private final JTextArea bodyArea = new JTextArea(14, 42);
    private final JButton submitButton = new JButton("Create post");
    private final JButton backButton = new JButton("Back to posts");

    public CreatePostPage(Color background, Color mutedText, Color headingText, Color splitBorder) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 24, 24, 24));
        setBackground(background);

        JLabel title = new JLabel("Create a post");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(headingText);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Pick a category, then write your title and post body.");
        subtitle.setForeground(mutedText);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(background);
        form.setAlignmentX(LEFT_ALIGNMENT);

        JLabel categoryLabel = new JLabel("Category");
        categoryLabel.setForeground(mutedText);
        categoryLabel.setAlignmentX(LEFT_ALIGNMENT);
        categoryCombo.setMaximumSize(new Dimension(420, 32));
        categoryCombo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("Title");
        titleLabel.setForeground(mutedText);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleField.setMaximumSize(new Dimension(760, 32));
        titleField.setAlignmentX(LEFT_ALIGNMENT);

        JLabel bodyLabel = new JLabel("Content");
        bodyLabel.setForeground(mutedText);
        bodyLabel.setAlignmentX(LEFT_ALIGNMENT);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.setBorder(BorderFactory.createTitledBorder("Write your post"));
        JScrollPane bodyScroll = new JScrollPane(bodyArea);
        bodyScroll.setAlignmentX(LEFT_ALIGNMENT);
        bodyScroll.setPreferredSize(new Dimension(760, 280));
        bodyScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));
        bodyScroll.setBorder(BorderFactory.createLineBorder(splitBorder));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(background);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.add(backButton);
        actions.add(submitButton);

        form.add(categoryLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(categoryCombo);
        form.add(Box.createVerticalStrut(12));
        form.add(titleLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(titleField);
        form.add(Box.createVerticalStrut(12));
        form.add(bodyLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(bodyScroll);
        form.add(Box.createVerticalStrut(12));
        form.add(actions);

        add(title);
        add(Box.createVerticalStrut(6));
        add(subtitle);
        add(Box.createVerticalStrut(16));
        add(form);
        add(Box.createVerticalGlue());
    }

    public JComboBox<CategoryInfo> getCategoryCombo() {
        return categoryCombo;
    }

    public JTextField getTitleField() {
        return titleField;
    }

    public JTextArea getBodyArea() {
        return bodyArea;
    }

    public JButton getSubmitButton() {
        return submitButton;
    }

    public JButton getBackButton() {
        return backButton;
    }

    public void setCategories(DefaultComboBoxModel<CategoryInfo> model) {
        categoryCombo.setModel(model);
    }

    public void resetDraft() {
        titleField.setText("");
        bodyArea.setText("");
        bodyArea.setCaretPosition(0);
    }
}
