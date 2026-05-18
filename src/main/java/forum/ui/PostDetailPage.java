package forum.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Dedicated page for viewing a post and its comments.
 * MainWindow wires events and data-loading behavior; this class owns layout/widgets.
 */
public class PostDetailPage extends JPanel {

    private final JLabel postTitleLabel = new JLabel("Post");
    private final JLabel postMetaLabel = new JLabel(" ");
    private final JLabel postAuthorAvatarLabel = new JLabel();
    private final JTextArea postBodyArea = new JTextArea(8, 42);
    private final JPanel commentsListPanel = new JPanel();
    private final JTextArea newCommentArea = new JTextArea(3, 36);
    private final JButton addCommentButton = new JButton("Add comment");
    private final JButton refreshCommentsButton = new JButton("Refresh comments");
    private final JButton backToPostsButton = new JButton("Back to posts");
    private final JLabel replyContextLabel = new JLabel(" ");
    private final JButton cancelReplyButton = new JButton("Cancel reply");
    private final JScrollPane pageScroll;
    private final JScrollPane composerScroll;

    public PostDetailPage(Color appBackground, Color panelBackground, Color headingText, Color mutedText, Color splitBorder) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        setBackground(appBackground);

        postTitleLabel.setFont(postTitleLabel.getFont().deriveFont(Font.BOLD, 22f));
        postTitleLabel.setForeground(headingText);
        postMetaLabel.setForeground(mutedText);

        postBodyArea.setEditable(false);
        postBodyArea.setLineWrap(true);
        postBodyArea.setWrapStyleWord(true);
        postBodyArea.setBorder(BorderFactory.createTitledBorder("Post"));
        postBodyArea.setBackground(panelBackground);
        postBodyArea.setRows(10);

        commentsListPanel.setLayout(new BoxLayout(commentsListPanel, BoxLayout.Y_AXIS));
        commentsListPanel.setBackground(appBackground);
        commentsListPanel.setBorder(BorderFactory.createTitledBorder("Comments"));
        commentsListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        newCommentArea.setLineWrap(true);
        newCommentArea.setWrapStyleWord(true);
        newCommentArea.setBorder(BorderFactory.createTitledBorder("Write a comment"));
        newCommentArea.setRows(1);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionRow.setBackground(appBackground);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.add(backToPostsButton);
        actionRow.add(refreshCommentsButton);
        actionRow.add(addCommentButton);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(appBackground);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(appBackground);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        postAuthorAvatarLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        postAuthorAvatarLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        header.add(postTitleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(postMetaLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(postAuthorAvatarLabel);

        JScrollPane postBodyScroll = new JScrollPane(postBodyArea);
        postBodyScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        postBodyScroll.setPreferredSize(new Dimension(760, 210));
        postBodyScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        content.add(header);
        content.add(Box.createVerticalStrut(10));
        content.add(postBodyScroll);
        content.add(Box.createVerticalStrut(10));
        content.add(commentsListPanel);

        pageScroll = new JScrollPane(content);
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        pageScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel composerPanel = new JPanel(new BorderLayout(0, 6));
        composerPanel.setBackground(appBackground);
        composerPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        replyContextLabel.setForeground(mutedText);
        replyContextLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        JPanel replyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        replyRow.setOpaque(false);
        replyRow.add(replyContextLabel);
        replyRow.add(cancelReplyButton);

        composerScroll = new JScrollPane(newCommentArea);
        composerScroll.setBorder(BorderFactory.createLineBorder(splitBorder));
        composerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        composerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        composerPanel.add(replyRow, BorderLayout.NORTH);
        composerPanel.add(composerScroll, BorderLayout.CENTER);
        composerPanel.add(actionRow, BorderLayout.SOUTH);

        JPanel leftSidebar = new JPanel();
        leftSidebar.setBackground(appBackground);
        leftSidebar.setPreferredSize(new Dimension(160, 0));
        leftSidebar.setMinimumSize(new Dimension(120, 0));

        JPanel mainColumn = new JPanel(new BorderLayout(0, 0));
        mainColumn.setBackground(appBackground);
        mainColumn.add(pageScroll, BorderLayout.CENTER);
        mainColumn.add(composerPanel, BorderLayout.SOUTH);

        add(leftSidebar, BorderLayout.WEST);
        add(mainColumn, BorderLayout.CENTER);
    }

    public JLabel getPostTitleLabel() {
        return postTitleLabel;
    }

    public JLabel getPostMetaLabel() {
        return postMetaLabel;
    }

    public JLabel getPostAuthorAvatarLabel() {
        return postAuthorAvatarLabel;
    }

    public JTextArea getPostBodyArea() {
        return postBodyArea;
    }

    public JPanel getCommentsListPanel() {
        return commentsListPanel;
    }

    public JTextArea getNewCommentArea() {
        return newCommentArea;
    }

    public JButton getAddCommentButton() {
        return addCommentButton;
    }

    public JButton getRefreshCommentsButton() {
        return refreshCommentsButton;
    }

    public JButton getBackToPostsButton() {
        return backToPostsButton;
    }

    public JLabel getReplyContextLabel() {
        return replyContextLabel;
    }

    public JButton getCancelReplyButton() {
        return cancelReplyButton;
    }

    public JScrollPane getPageScroll() {
        return pageScroll;
    }

    public JScrollPane getComposerScroll() {
        return composerScroll;
    }
}
