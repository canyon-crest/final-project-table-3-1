package forum.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JViewport;

import forum.model.AvatarOption;
import forum.model.ForumUser;

/**
 * Dedicated profile page UI (avatar preview, username, and avatar-part selectors).
 * MainWindow wires actions and data updates.
 */
public class ProfilePage extends JPanel implements Scrollable {

    private final JLabel avatarLabel = new JLabel();
    private final JLabel usernameLabel = new JLabel("Username: ");
    private final JTextField usernameField = new JTextField(20);
    private final JComboBox<AvatarOption> headpieceCombo = new JComboBox<>();
    private final JComboBox<AvatarOption> clothingCombo = new JComboBox<>();
    private final JComboBox<AvatarOption> accessoryCombo = new JComboBox<>();
    private final JButton updateProfileButton = new JButton("Update profile");
    private final JButton backToForumButton = new JButton("Back to forum");

    public ProfilePage(Color appBackground, Color mutedText, Color headingText, Color splitBorder) {
        setLayout(new GridBagLayout());
        setBackground(appBackground);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.setBackground(appBackground);

        JLabel title = new JLabel("Profile");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(headingText);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(splitBorder, 1),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        usernameLabel.setForeground(mutedText);
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(320, 30));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel avatarPartsLabel = new JLabel("Avatar parts");
        avatarPartsLabel.setForeground(mutedText);
        avatarPartsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headpieceCombo.setMaximumSize(new Dimension(320, 30));
        clothingCombo.setMaximumSize(new Dimension(320, 30));
        accessoryCombo.setMaximumSize(new Dimension(320, 30));
        headpieceCombo.setPreferredSize(new Dimension(320, 30));
        clothingCombo.setPreferredSize(new Dimension(320, 30));
        accessoryCombo.setPreferredSize(new Dimension(320, 30));
        headpieceCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        clothingCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        accessoryCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField.setPreferredSize(new Dimension(320, 30));
        updateProfileButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backToForumButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(title);
        content.add(Box.createVerticalStrut(16));
        content.add(avatarLabel);
        content.add(Box.createVerticalStrut(16));
        content.add(usernameLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(usernameField);
        content.add(Box.createVerticalStrut(22));
        content.add(avatarPartsLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(makeSectionLabel("Headpiece", mutedText));
        content.add(Box.createVerticalStrut(6));
        content.add(headpieceCombo);
        content.add(Box.createVerticalStrut(10));
        content.add(makeSectionLabel("Clothing", mutedText));
        content.add(Box.createVerticalStrut(6));
        content.add(clothingCombo);
        content.add(Box.createVerticalStrut(10));
        content.add(makeSectionLabel("Accessory", mutedText));
        content.add(Box.createVerticalStrut(6));
        content.add(accessoryCombo);
        content.add(Box.createVerticalStrut(16));
        content.add(updateProfileButton);
        content.add(Box.createVerticalStrut(10));
        content.add(backToForumButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(content, gbc);
    }

    private static JLabel makeSectionLabel(String text, Color mutedText) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(mutedText);
        return label;
    }

    public JLabel getAvatarLabel() {
        return avatarLabel;
    }

    public JLabel getUsernameLabel() {
        return usernameLabel;
    }

    public JTextField getUsernameField() {
        return usernameField;
    }

    public JComboBox<AvatarOption> getHeadpieceCombo() {
        return headpieceCombo;
    }

    public JComboBox<AvatarOption> getClothingCombo() {
        return clothingCombo;
    }

    public JComboBox<AvatarOption> getAccessoryCombo() {
        return accessoryCombo;
    }

    public JButton getUpdateProfileButton() {
        return updateProfileButton;
    }

    public JButton getBackToForumButton() {
        return backToForumButton;
    }

    public Icon buildAvatarIcon(ForumUser user, int size,
            List<AvatarOption> cachedHeadpieces, List<AvatarOption> cachedClothing, List<AvatarOption> cachedAccessories) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        String seed = user == null ? "" : user.getUsername();
        int baseHash = Math.abs(seed == null ? 0 : seed.hashCode());
        long headpiece = user == null || user.getAvatarHeadpieceId() == null ? 0L : user.getAvatarHeadpieceId().longValue();
        long clothing = user == null || user.getAvatarClothingId() == null ? 0L : user.getAvatarClothingId().longValue();
        long accessory = user == null || user.getAvatarAccessoryId() == null ? 0L : user.getAvatarAccessoryId().longValue();
        int hash = Math.abs(baseHash ^ (int) (headpiece * 31 + clothing * 131 + accessory * 271));
        Color bgColor = new Color(120 + (hash % 80), 100 + ((hash / 3) % 80), 120 + ((hash / 5) % 80));

        int head = (int) (size * 0.36);
        int headX = (size - head) / 2;
        int headY = (int) (size * 0.2);

        int bodyW = (int) (size * 0.58);
        int bodyH = (int) (size * 0.32);
        int bodyX = (size - bodyW) / 2;
        int bodyY = (int) (size * 0.62);

        int eyeY = headY + (head / 2) - (size / 60);

        String clothCode = getAvatarCode(cachedClothing, clothing);
        Color shirtColor;
        if (clothCode == null) {
            shirtColor = new Color(80 + (hash % 120), 40 + ((hash / 7) % 120), 40 + ((hash / 11) % 120));
        } else if ("polo_navy".equals(clothCode)) {
            shirtColor = new Color(30, 45, 90);
        } else if ("raven_tee".equals(clothCode)) {
            shirtColor = new Color(30, 30, 33);
        } else if ("hoodie_black".equals(clothCode)) {
            shirtColor = new Color(45, 45, 50);
        } else if ("sweater_cream".equals(clothCode)) {
            shirtColor = new Color(240, 228, 208);
        } else if ("dress_shirt".equals(clothCode)) {
            shirtColor = new Color(210, 225, 245);
        } else if ("letterman".equals(clothCode)) {
            shirtColor = new Color(155, 20, 35);
        } else if ("jacket_red".equals(clothCode)) {
            shirtColor = new Color(186, 12, 47);
        } else if ("tracksuit".equals(clothCode)) {
            shirtColor = new Color(28, 68, 110);
        } else if ("formal_gown".equals(clothCode)) {
            shirtColor = new Color(80, 50, 110);
        } else if ("team_jersey".equals(clothCode)) {
            shirtColor = new Color(38, 105, 55);
        } else {
            shirtColor = new Color(186, 12, 47);
        }

        g.setColor(bgColor);
        g.fillRoundRect(0, 0, size, size, size / 8, size / 8);

        g.setColor(shirtColor);
        g.fillRoundRect(bodyX, bodyY, bodyW, bodyH, size / 12, size / 12);
        if (clothCode != null) {
            if ("hoodie_black".equals(clothCode) || "sweater_cream".equals(clothCode)) {
                g.setColor(new Color(255, 255, 255, 90));
                int stripeW = Math.max(1, bodyW / 8);
                g.fillRect(bodyX + bodyW / 2 - stripeW / 2, bodyY, stripeW, bodyH);
            } else if ("dress_shirt".equals(clothCode) || "letterman".equals(clothCode) || "jacket_red".equals(clothCode)) {
                g.setColor(new Color(0, 0, 0, 55));
                g.drawLine(bodyX + bodyW / 2, bodyY, bodyX + bodyW / 2, bodyY + bodyH);
                g.drawLine(bodyX + bodyW / 2 - 2, bodyY, bodyX + bodyW / 2 - 2, bodyY + bodyH);
            }
        }

        g.setColor(new Color(245, 214, 178));
        g.fillOval(headX, headY, head, head);

        g.setColor(Color.BLACK);
        int eyeR = Math.max(2, size / 24);
        g.fillOval(headX + head / 3 - eyeR / 2, eyeY, eyeR, eyeR);
        g.fillOval(headX + (head * 2 / 3) - eyeR / 2, eyeY, eyeR, eyeR);

        String hpCode = getAvatarCode(cachedHeadpieces, headpiece);
        if (hpCode != null && !"none".equals(hpCode)) {
            if ("cap_red".equals(hpCode)) {
                g.setColor(new Color(186, 12, 47, 240));
                g.fillArc(headX, headY - head / 4, head, head / 2, 0, 180);
                g.fillRect(headX + head / 2, headY - head / 6, head / 2 + size / 15, size / 20);
            } else if ("beanie_gray".equals(hpCode)) {
                g.setColor(new Color(120, 120, 125, 240));
                g.fillArc(headX - size / 40, headY - head / 3, head + size / 20, head * 2 / 3, 0, 180);
            } else if ("visor_white".equals(hpCode)) {
                g.setColor(new Color(240, 240, 245, 240));
                g.fillRect(headX - size / 30, headY - size / 40, head + size / 15, size / 15);
                g.fillRect(headX + head / 2, headY - size / 40, head / 2 + size / 15, size / 12);
            } else if ("headband_raven".equals(hpCode)) {
                g.setColor(new Color(24, 24, 24, 240));
                g.fillRect(headX, headY, head, size / 12);
            } else if ("crown_gold".equals(hpCode)) {
                g.setColor(new Color(200, 170, 60, 250));
                for (int i = 0; i < 5; i++) {
                    int px = headX + (int) (size * 0.05) + i * (head - (int) (size * 0.1)) / 4;
                    g.fillPolygon(new int[] { px - size / 30, px, px + size / 30 },
                            new int[] { headY - size / 40, headY - size / 6, headY - size / 40 }, 3);
                }
                g.fillRect(headX + size / 40, headY - size / 20, head - size / 20, size / 20);
            } else if ("halo".equals(hpCode)) {
                g.setColor(new Color(255, 240, 150, 200));
                g.setStroke(new BasicStroke(Math.max(1f, size / 40f)));
                g.drawOval(headX - size / 20, headY - size / 6, head + size / 10, size / 8);
                g.setStroke(new BasicStroke(1f));
            }
        }

        String accCode = getAvatarCode(cachedAccessories, accessory);
        if (accCode != null && !"none".equals(accCode)) {
            g.setColor(new Color(10, 10, 10, 220));
            if ("glasses".equals(accCode)) {
                int y = eyeY - size / 40;
                int gw = size / 8;
                g.drawOval(headX + head / 3 - gw / 2, y, gw, gw);
                g.drawOval(headX + (head * 2 / 3) - gw / 2, y, gw, gw);
                g.drawLine(headX + head / 3 + gw / 2, y + gw / 2, headX + (head * 2 / 3) - gw / 2, y + gw / 2);
            } else if ("scarf_striped".equals(accCode)) {
                g.setColor(new Color(186, 12, 47, 230));
                g.fillRect(headX, headY + head - size / 20, head, size / 10);
                g.fillRect(headX + head / 2, headY + head - size / 20, size / 10, size / 4);
            } else if ("watch_silver".equals(accCode)) {
                g.setColor(new Color(180, 190, 200, 250));
                g.fillRect(bodyX - size / 30, bodyY + bodyH / 2, size / 12, size / 15);
            } else if ("bow_tie".equals(accCode)) {
                int bowY = headY + head - size / 30;
                int cx = headX + head / 2;
                g.setColor(new Color(40, 40, 90, 250));
                int bw = size / 10;
                int bh = size / 12;
                g.fillPolygon(new int[] { cx, cx - bw, cx - bw }, new int[] { bowY, bowY - bh / 2, bowY + bh / 2 }, 3);
                g.fillPolygon(new int[] { cx, cx + bw, cx + bw }, new int[] { bowY, bowY - bh / 2, bowY + bh / 2 }, 3);
            } else if ("star_pin".equals(accCode)) {
                g.setColor(new Color(240, 210, 80, 250));
                int px = bodyX + bodyW / 4;
                int py = bodyY + bodyH / 4;
                g.fillOval(px, py, size / 15, size / 15);
            } else if ("backpack".equals(accCode)) {
                g.setColor(new Color(55, 55, 60, 250));
                g.fillRoundRect(bodyX - size / 12, bodyY + size / 15, size / 8, bodyH * 2 / 3, size / 20, size / 20);
                g.fillRoundRect(bodyX + bodyW - size / 30, bodyY + size / 15, size / 8, bodyH * 2 / 3, size / 20, size / 20);
            } else if ("earbuds".equals(accCode)) {
                g.setColor(Color.WHITE);
                g.drawLine(headX, headY + head * 2 / 3, headX, bodyY + size / 10);
                g.drawLine(headX + head, headY + head * 2 / 3, headX + head, bodyY + size / 10);
                g.fillOval(headX - size / 30, headY + head / 2, size / 15, size / 10);
                g.fillOval(headX + head - size / 30, headY + head / 2, size / 15, size / 10);
            }
        }
        g.dispose();
        return new ImageIcon(image);
    }

    /**
     * Renders a bot-specific logo mark that is never sourced from user customization options.
     */
    public Icon buildBotAvatarIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int corner = Math.max(6, size / 7);
        g.setColor(new Color(20, 28, 46));
        g.fillRoundRect(0, 0, size, size, corner, corner);
        g.setColor(new Color(56, 78, 122));
        g.setStroke(new BasicStroke(Math.max(1.4f, size / 28f)));
        g.drawRoundRect(1, 1, size - 3, size - 3, corner, corner);

        int inset = Math.max(2, size / 10);
        int innerW = size - inset * 2;
        int innerH = innerW;
        g.setColor(new Color(31, 45, 74));
        g.fillRoundRect(inset, inset, innerW, innerH, Math.max(6, corner - 2), Math.max(6, corner - 2));

        // Subtle top bar to make the mark feel app/logo-like.
        g.setColor(new Color(90, 140, 220, 140));
        g.fillRoundRect(inset + size / 14, inset + size / 16, innerW - size / 7, Math.max(2, size / 16),
                size / 14, size / 14);

        int cx = size / 2;
        int cy = size / 2;
        int r = Math.max(8, (int) (size * 0.26));
        int[] xs = { cx, cx + r, cx + r, cx, cx - r, cx - r };
        int[] ys = { cy - r, cy - r / 2, cy + r / 2, cy + r, cy + r / 2, cy - r / 2 };
        g.setColor(new Color(47, 74, 122, 210));
        g.fillPolygon(xs, ys, 6);
        g.setColor(new Color(138, 188, 255, 210));
        g.setStroke(new BasicStroke(Math.max(1.2f, size / 36f)));
        g.drawPolygon(xs, ys, 6);

        // Stylized Y monogram.
        g.setColor(new Color(232, 244, 255));
        g.setStroke(new BasicStroke(Math.max(2f, size / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int yTop = cy - r / 2;
        int yMid = cy;
        int yBottom = cy + r / 2;
        g.drawLine(cx, yMid, cx, yBottom);
        g.drawLine(cx, yMid, cx - r / 2, yTop);
        g.drawLine(cx, yMid, cx + r / 2, yTop);

        // Small AI "circuit" accent (separate from user avatars).
        int nodeR = Math.max(2, size / 18);
        int n1x = cx + r / 2;
        int n1y = cy + r / 3;
        int n2x = n1x + size / 10;
        int n2y = n1y + size / 12;
        int n3x = n1x - size / 14;
        int n3y = n1y + size / 10;
        g.setColor(new Color(120, 215, 255));
        g.setStroke(new BasicStroke(Math.max(1f, size / 45f)));
        g.drawLine(n1x, n1y, n2x, n2y);
        g.drawLine(n1x, n1y, n3x, n3y);
        g.fillOval(n1x - nodeR, n1y - nodeR, nodeR * 2, nodeR * 2);
        g.fillOval(n2x - nodeR, n2y - nodeR, nodeR * 2, nodeR * 2);
        g.fillOval(n3x - nodeR, n3y - nodeR, nodeR * 2, nodeR * 2);

        g.dispose();
        return new ImageIcon(image);
    }

    private static String getAvatarCode(List<AvatarOption> list, long id) {
        if (id <= 0L) {
            return null;
        }
        for (AvatarOption opt : list) {
            if (opt.getId() == id) {
                return opt.getCode();
            }
        }
        return null;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(16, orientation == SwingConstants.VERTICAL ? visibleRect.height - 32 : visibleRect.width - 32);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport viewport) {
            return getPreferredSize().height <= viewport.getHeight();
        }
        return false;
    }
}
