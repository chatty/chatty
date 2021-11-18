
package chatty.gui.components.updating;

import chatty.Chatty;
import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.gui.components.settings.SettingsUtil;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.GitHub;
import chatty.util.GitHub.Asset;
import chatty.util.GitHub.Release;
import chatty.util.GitHub.Releases;
import chatty.util.MiscUtil;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class UpdateDialog extends JDialog {
    
    private static final Logger LOGGER = Logger.getLogger(UpdateDialog.class.getName());
    
    private static final String HTML_PREFIX = "<html><body style='text-align:center;'>";
    
    private static final String BETA_INFO = "<html><body style='width:350px;'>Betas are steps towards the next "
            + "which contain the latest features, but may not be quite polished yet.";
    
    private static final String VERSION = Chatty.VERSION;
    
    private final LinkLabel info;
    private final LinkLabel downloadsInfo;
    private final Settings settings;
    private final LinkLabel downloadButtonInfo;
    private final InstallListener installListener;
    
    private final Changes changes;
    
    private Releases releases;
    private Asset downloadAsset;
    private Path installDir;
    private Release runningRelease;
    private Release latestRelease;
    
    private final JCheckBox enableCheckBeta;
    private final JCheckBox enableUpdateJar;
    private final JButton closeButton;
    private final JButton downloadButton;
    
    public UpdateDialog(Window owner, LinkLabelListener linkLabelListener,
            Settings settings, InstallListener installListener) {
        super(owner);
        setModal(true);
        //setResizable(false);
        
        this.installListener = installListener;
        LinkLabelListener myLinkLabelListener = new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                if (type.equals("changelog")) {
                    if (releases != null && latestRelease != null) {
                        changes.showDialog(releases, latestRelease, runningRelease);
                    }
                } else {
                    linkLabelListener.linkClicked(type, ref);
                }
            }
        };
        
        this.settings = settings;
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        
        changes = new Changes(this);
        
        info = new LinkLabel(null, myLinkLabelListener);
        gbc = GuiUtil.makeGbc(0, 0, 1, 1, GridBagConstraints.CENTER);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        add(info, gbc);
        
        downloadButton = new JButton("Download & Install Latest Version");
        gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.CENTER);
        gbc.insets = new Insets(5, 5, 0, 5);
        downloadButton.addActionListener(e -> download(downloadAsset));
        add(downloadButton, gbc);
        
        downloadButtonInfo = new LinkLabel("", linkLabelListener);
        gbc = GuiUtil.makeGbc(0, 2, 1, 1, GridBagConstraints.CENTER);
        gbc.insets = new Insets(0, 5, 5, 5);
        add(downloadButtonInfo, gbc);
        
        downloadsInfo = new LinkLabel(null, linkLabelListener);
        gbc = GuiUtil.makeGbc(0, 3, 1, 1, GridBagConstraints.CENTER);
        add(downloadsInfo, gbc);
        
        enableCheckBeta = new JCheckBox("Show beta versions");
        enableCheckBeta.addActionListener(e -> {
            settings.setBoolean("checkNewBeta", enableCheckBeta.isSelected());
            updateDisplay();
        });
        gbc = GuiUtil.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST);
        add(enableCheckBeta, gbc);
        
        LinkLabel releaseNotes = new LinkLabel("[help-releases:top Release notes]", linkLabelListener);
        gbc = GuiUtil.makeGbc(0, 5, 1, 1, GridBagConstraints.WEST);
        add(releaseNotes, gbc);
        
        //gbc = GuiUtil.makeGbc(0, 5, 1, 1);
        //gbc.fill = GridBagConstraints.HORIZONTAL;
        //gbc.weightx = 1;
        //gbc.insets = new Insets(1, 12, 5, 5);
        //add(new JLabel(BETA_INFO), gbc);
        
        enableUpdateJar = new JCheckBox("Stay on current Standalone (updates JAR only)");
        enableUpdateJar.setToolTipText(SettingsUtil.addTooltipLinebreaks("Will download the JAR installer only, for example if you need to save data or need to stay on the old Standalone version."));
        enableUpdateJar.addActionListener(e -> {
            settings.setBoolean("updateJar", enableUpdateJar.isSelected());
            updateDisplay();
        });
        gbc = GuiUtil.makeGbc(0, 6, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(0, 5, 5, 5);
        add(enableUpdateJar, gbc);

        closeButton = new JButton(Language.getString("dialog.button.close"));
        gbc = GuiUtil.makeGbc(0, 7, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(closeButton, gbc);
        
        closeButton.addActionListener(e -> setVisible(false));
    }
    
    public void showDialog() {
        if (releases == null) {
            Version.request(settings, (v, r) -> {
                SwingUtilities.invokeLater(() -> {
                    setInfo(r);
                });
            });
        }
        updateDisplay();
        setVisible(true);
    }
    
    public void showDialog(Releases releases) {
        setInfo(releases);
        setVisible(true);
    }
    
    public void setInfo(Releases releases) {
        this.releases = releases;
        updateDisplay();
    }
    
    /**
     * Update the contents of the update dialog, based on current data.
     */
    private void updateDisplay() {
        downloadButton.setVisible(false);
        downloadButtonInfo.setText(null);
        downloadsInfo.setVisible(false);
        enableCheckBeta.setSelected(settings.getBoolean("checkNewBeta"));
        enableUpdateJar.setSelected(settings.getBoolean("updateJar"));
        if (releases != null) {
            if (!Stuff.isStandalone()) {
                enableUpdateJar.setVisible(false);
            }
            Release latest = releases.getLatest();
            boolean betaUpdateAvailable = false;
            
            // Check for beta and decide whether to show it
            if (releases.getLatestBeta() != null) {
                boolean betaActuallyNewer = Version.compareVersions(VERSION, releases.getLatestBeta().getVersion()) == 1;
                if (settings.getBoolean("checkNewBeta")) {
                    latest = releases.getLatestBeta();
                } else if (betaActuallyNewer) {
                    betaUpdateAvailable = true;
                }
            }
            
            // State variables
            latestRelease = latest;
            runningRelease = releases.getByVersion(VERSION);
            
            // Actually update dialog
            if (Version.compareVersions(VERSION, latest.getVersion()) == 1
                    || Debugging.isEnabled("update")) {
                setNewVersion(latest, runningRelease);
            } else {
                setVersion(latest, betaUpdateAvailable);
            }
        } else {
            setTitle("Loading..");
            info.setText("Loading version info..");
        }
        pack();
        setLocationRelativeTo(getParent());
        SwingUtilities.invokeLater(() -> {
            Dimension p = getPreferredSize();
            Dimension c = getSize();
            setMinimumSize(getPreferredSize());
            if (p.width > c.width || p.height > c.height) {
                pack();
                setLocationRelativeTo(getParent());
            }
        });
    }
    
    private void setVersion(Release release, boolean newerBeta) {
        setTitle("You are up-to-date!");
        
        String main = HTML_PREFIX + ("<div style='font-size:1.2em;padding-left:10px;padding-right:10px;'>"
                + "You&nbsp;are&nbsp;running&nbsp;the&nbsp;[help-releases:top latest&nbsp;version]"
                + "</div>");
        if (newerBeta) {
            main += "<div style='margin:5px'>There is a newer beta version though!</div>";
        }
        info.setText(String.format(main, VERSION, release.getVersion()));
    }
    
    /**
     * Update dialog contents for a new version being available.
     * 
     * @param latest
     * @param current May be null if not in received release history
     */
    private void setNewVersion(Release latest, Release current) {
        setTitle("Update available!");
        
        String main = "<div style='padding-left:10px;padding-right:10px;'>"
                + "<div style='font-size:1.2em;'>Installed: <span style='font-family:monospaced;'>%1$s</span> <small>%3$s</small></div>"
                + "<div style='padding:5px;'><span style='font-size:1.2em;'>↓</span> [changelog:b View Changes]</div>"
                + "<div style='font-size:1.2em;'>Latest: <span style='font-family:monospaced;'>%2$s</span> <small>%4$s</small></div>"
                + "</div>".replaceAll(" ", "&nbsp;");

        String currentAge = "(?)";
        if (current != null) {
            currentAge = String.format("(%d days)", current.daysOld());
        }
        String latestAge = String.format("(%d days)", latest.daysOld());
        
        String html = HTML_PREFIX+String.format(main,
                VERSION, latest.getVersion(), currentAge, latestAge);
        info.setText(html);
        
        boolean isStandalone = Stuff.isStandalone();
        installDir = null;
        
        Asset asset = null;
        if (MiscUtil.OS_WINDOWS && Stuff.installPossible()) {
            if (isStandalone && !settings.getBoolean("updateJar")) {
                asset = latest.getAsset("win_standalone_setup.exe");
            }
            if (asset == null) {
                isStandalone = false;
                asset = latest.getAsset("win_setup.exe");
            }
        }
        
        downloadAsset = asset;
        installDir = Stuff.getInstallDir(isStandalone);
        downloadButton.setVisible(true);
        if (asset == null || installDir == null) {
            String reason = "";
            if (!MiscUtil.OS_WINDOWS) {
                reason = "only supported on Windows";
            }
            else if (!Stuff.installPossible() && Stuff.getInitError() != null) {
                reason = Stuff.getInitError();
            }
            if (!StringUtil.isNullOrEmpty(reason)) {
                reason = ", "+reason;
            }
            downloadButton.setEnabled(false);
            downloadButtonInfo.setText("<html><body style='width:200px;text-align:center;'>"
                    + "<small>(Automatic download not available"+reason+")</small>");
        } else {
            downloadButton.setEnabled(true);
            downloadButtonInfo.setText("<html><body><div style='width:200px;text-align:center;'>"
                    + "<small>(Downloads, closes Chatty and runs the setup: "+asset.getName()+")</small></div>");
        }
        downloadsInfo.setVisible(true);
        downloadsInfo.setText("<html><body style='padding:0px 10px 0px 10px;'><p>Direct downloads (manual install):</p>"
                + makeDownloadLinks(latest));
    }
    
    private String makeDownloadLinks(Release release) {
        StringBuilder b = new StringBuilder();
        b.append("<ul style='margin-left:24'>");
        for (Asset asset : release.getAssets()) {
            b.append("<li>");
            b.append("[url:").append(asset.getUrl()).append(" ").append(asset.getName()).append("]");
            b.append("</li>");
        }
        b.append("</ul>");
        return b.toString();
    }
    
    private void download(Asset asset) {
        try {
            Path installerPath = Stuff.getTempFilePath(asset.getName());
            URL downloadUrl = new URL(asset.getUrl());
            if (FileDownloaderDialog.downloadFile(this, downloadUrl, installerPath, "Download update")) {
//                int result = JOptionPane.showConfirmDialog(this, "Running the installer will close Chatty, continue?", "Install Update", JOptionPane.YES_NO_OPTION);
//                if (result == JOptionPane.YES_OPTION) {
                    install(installerPath);
//                }
            }
        } catch (IOException ex) {
            LOGGER.warning("Error downloading asset: "+ex);
            // The downloader dialog should display the error
        }
    }
    
    private void install(Path installerPath) {
        try {
            RunUpdater.run(installerPath, installDir, Stuff.getJarPath(),
                Stuff.getChattyExe(), Stuff.getJavawExe(), Chatty.getArgs(),
                Debugging.isEnabled("update"));
            installListener.installing();
        } catch (IOException ex) {
            LOGGER.warning("Error running installer: "+ex);
            String text = "<html><body style='width:400px'>"+ex;
            JOptionPane.showMessageDialog(this, text, "Error installing update",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public interface InstallListener {
        public void installing();
    }
    
    public static final void main(String[] args) {
        Debugging.command("+update");
        Stuff.init(Paths.get("H:\\chatty_install\\Param Test.jar"));
        
        Settings settings = new Settings("", null);
        settings.addBoolean("checkNewBeta", true);
        settings.addBoolean("updateJar", false);
        settings.addLong("versionLastChecked", 0L);
        
        LinkLabelListener linkLabelListener = (type, ref) -> {
            System.out.println("Link clicked: " + type + ":" + ref);
        };
        
        Releases data = testReleases();
        //data = GitHub.getReleases();
        
        UpdateDialog d = new UpdateDialog(null, linkLabelListener, settings, () -> System.exit(0));
        d.setLocationRelativeTo(null);
        d.showDialog(data);
        System.exit(0);
    }
    
    private static Releases testReleases() {
        List<Release> releases = new ArrayList<>();
        releases.add(testRelease("0.9.1-beta2", true, DateTime.parseDatetime("2018-04-04T00:31:11Z")));
        releases.add(testRelease("0.9.1-beta1", true, DateTime.parseDatetime("2018-04-03T00:31:11Z")));
        releases.add(testRelease("0.9", false, DateTime.parseDatetime("2018-04-02T00:31:11Z")));
        releases.add(testRelease("0.9b3", true, DateTime.parseDatetime("2018-03-22T00:31:11Z")));
        releases.add(testRelease("0.9b2", true, DateTime.parseDatetime("2018-02-12T00:31:11Z")));
        releases.add(testRelease("0.9b1", true, DateTime.parseDatetime("2018-02-03T00:31:11Z")));
        releases.add(testRelease("0.8.7", false, DateTime.parseDatetime("2018-02-02T00:31:11Z")));
        releases.add(testRelease("0.8.6", false, DateTime.parseDatetime("2018-01-02T00:31:11Z")));
        return new Releases(releases);
    }
    
    private static Release testRelease(String version, boolean beta, long datetime) {
        String description = "Changes [compared to previous version](https://github.com/chatty/chatty/compare/v0.9...v0.9.1) (v0.9):\n"
                + "\n"
                + "### Main Features\n"
                + "- Added basic support for Rooms (join via Channels-menu and Favorites/History)\n"
                + "- Added initial support for translating the Chatty GUI to other languages,\n"
                + "  added some partial translations (thanks to volunteers translating)";
        
        List<Asset> assets = new ArrayList<>();
        assets.add(new Asset("Chatty_"+version+".zip", "http://example.com/"));
        assets.add(new Asset("Chatty_"+version+"_hotkey_32bit.zip", "http://example.com/"));
        assets.add(new Asset("Chatty_"+version+"_hotkey_64bit.zip", "http://example.com/"));
        assets.add(new Asset("Chatty_"+version+"_win_standalone.zip", "http://example.com/"));
        assets.add(new Asset("Chatty_"+version+"_win_setup.exe", "file:\\H:\\chatty_test\\Chatty_0.9.1_installer.exe"));
        assets.add(new Asset("Chatty_"+version+"_win_standalone_setup.exe", "http://example.com/"));
        return new GitHub.Release("v"+version, "Version "+version, description, beta, assets, datetime);
    }
    
}
