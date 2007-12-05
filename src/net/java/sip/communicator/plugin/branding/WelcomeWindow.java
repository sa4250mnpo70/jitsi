package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class WelcomeWindow extends JDialog
{
    private WindowBackground mainPanel = new WindowBackground();

    private JLabel titleLabel = new JLabel("SIP Communicator");

    private JLabel versionLabel = new JLabel(" "
            + System.getProperty("sip-communicator.version"));

    private JTextArea logoArea = new JTextArea(Resources
            .getString("logoMessage"));

    private JEditorPane rightsArea = new JEditorPane();

    private JEditorPane licenseArea = new JEditorPane();

    private JPanel textPanel = new JPanel();

    private static final Color DARK_BLUE = new Color(23, 65, 125);

    private static final String FONT_NAME = "Verdana";

    private static final String FONT_SIZE = "12";

    private static final Font FONT = new Font(FONT_NAME, Font.PLAIN,
            new Integer(FONT_SIZE).intValue());

    private JPanel loadingPanel = new JPanel(new BorderLayout());

    private JLabel loadingLabel = new JLabel(Resources.getString("loading")
            + ": ");

    private JLabel bundleLabel = new JLabel();

    public WelcomeWindow()
    {
        this.setTitle("SIP Communicator");
        this.setModal(false);
        this.setUndecorated(true);

        this.mainPanel.setLayout(new BorderLayout());

        this.textPanel.setPreferredSize(new Dimension(470, 240));
        this.textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        this.textPanel
                .setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));
        this.textPanel.setOpaque(false);

        this.titleLabel.setFont(FONT.deriveFont(Font.BOLD, 28));
        this.titleLabel.setForeground(DARK_BLUE);
        this.titleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.versionLabel.setFont(FONT.deriveFont(Font.BOLD, 18));
        this.versionLabel.setForeground(Color.GRAY);
        this.versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.logoArea.setFont(FONT.deriveFont(Font.BOLD, 12));
        this.logoArea.setForeground(DARK_BLUE);
        this.logoArea.setOpaque(false);
        this.logoArea.setLineWrap(true);
        this.logoArea.setWrapStyleWord(true);
        this.logoArea.setEditable(false);
        this.logoArea.setPreferredSize(new Dimension(100, 20));
        this.logoArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.logoArea.setBorder(BorderFactory.createEmptyBorder(20, 180, 0, 0));

        this.rightsArea.setContentType("text/html");
        this.rightsArea.setText(Resources.getString("welcomeMessage",
            new String[]{   BrandingResources.getString("productName"),
                            BrandingResources.getString("productWebSite")}));

        this.rightsArea.setPreferredSize(new Dimension(50, 10));
        this.rightsArea
                .setBorder(BorderFactory.createEmptyBorder(0, 180, 0, 0));
        this.rightsArea.setOpaque(false);
        this.rightsArea.setEditable(false);
        this.rightsArea.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.licenseArea.setContentType("text/html");
        this.licenseArea.setText(Resources.getString("license", new String[]
        {
            "<a href=http://sip-communicator.org>"
                    + "http://sip-communicator.org</a>"
        }));

        this.licenseArea.setPreferredSize(new Dimension(50, 20));
        this.licenseArea.setBorder(BorderFactory
                .createEmptyBorder(0, 180, 0, 0));
        this.licenseArea.setOpaque(false);
        this.licenseArea.setEditable(false);
        this.licenseArea.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.bundleLabel.setFont(loadingLabel.getFont().deriveFont(Font.PLAIN));
        this.loadingPanel.setOpaque(false);
        this.loadingPanel.add(loadingLabel, BorderLayout.WEST);
        this.loadingPanel.add(bundleLabel, BorderLayout.CENTER);
        this.loadingPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.loadingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10,
                10));

        this.textPanel.add(titleLabel);
        this.textPanel.add(versionLabel);
        this.textPanel.add(logoArea);
        this.textPanel.add(rightsArea);
        this.textPanel.add(licenseArea);

        this.mainPanel.add(textPanel, BorderLayout.CENTER);
        this.mainPanel.add(loadingPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        this.setResizable(false);

        this.mainPanel.setPreferredSize(new Dimension(570, 330));

        this.setLocation(
            Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 527 / 2,
            Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 305 / 2);

        // Close the splash screen on simple click or Esc.
        this.getGlassPane().addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                WelcomeWindow.this.close();
            }
        });

        this.getGlassPane().setVisible(true);

        ActionMap amap = this.getRootPane().getActionMap();

        amap.put("close", new CloseAction());

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }

    protected void close()
    {
        this.dispose();
    }

    public void setBundle(String bundleName)
    {
        this.bundleLabel.setText(bundleName);

        this.loadingPanel.revalidate();
        this.loadingPanel.repaint();
    }

    /**
     * The action invoked when user presses Escape key.
     */
    private class CloseAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            WelcomeWindow.this.close();
        }
    }

}
