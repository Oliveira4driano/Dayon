package mpo.dayon.common.gui.common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import mpo.dayon.common.babylon.Babylon;
import mpo.dayon.common.gui.statusbar.StatusBar;
import mpo.dayon.common.gui.toolbar.ToolBar;
import mpo.dayon.common.log.Log;
import mpo.dayon.common.utils.SystemUtilities;
import mpo.dayon.common.version.Version;

public abstract class BaseFrame extends JFrame {

	private ToolBar toolBar;

	protected StatusBar statusBar;

	protected BaseFrame() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setIconImage(ImageUtilities.getOrCreateIcon(ImageNames.APP).getImage());

		addWindowListener(new WindowAdapter() {
			@Override
            public void windowClosing(WindowEvent ev) {
				doExit();
			}
		});
	}

	private void doExit() {
		if (JOptionPane.showConfirmDialog(this, Babylon.translate("exit.confirm"), Babylon.translate("exit"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
			Log.info("Bye!");
			System.exit(0);
		}
	}

	protected void setupToolBar(ToolBar toolBar) {
		this.toolBar = toolBar;
		add(toolBar, BorderLayout.NORTH);
	}

	protected void setupStatusBar(StatusBar statusBar) {
		this.statusBar = statusBar;
		add(statusBar, BorderLayout.SOUTH);
	}

	protected Action createExitAction() {
		final Action exit = new AbstractAction() {

			@Override
            public void actionPerformed(ActionEvent ev) {
				doExit();
			}
		};

		exit.putValue(Action.NAME, "exit");
		exit.putValue(Action.SHORT_DESCRIPTION, Babylon.translate("exit.dayon"));
		exit.putValue(Action.SMALL_ICON, ImageUtilities.getOrCreateIcon(ImageNames.EXIT));

		return exit;
	}

	private static final String HTTP_HOME = "https://github.com/retgal/dayon";
	private static final String HTTP_SUPPORT = "https://retgal.github.io/Dayon/support" + Babylon.translate("language.suffix") + ".html";
	private static final String HTTP_FEEDBACK = "https://github.com/retgal/dayon/issues";

	protected Action createShowInfoAction() {
		final Action showSystemInfo = new AbstractAction() {

			@Override
            public void actionPerformed(ActionEvent ev) {
				final JTextArea props = new JTextArea(SystemUtilities.getSystemPropertiesEx());
				props.setEditable(false);

				final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
				props.setFont(font);

				final JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

				panel.setPreferredSize(new Dimension(500, 300));

				final JLabel info = new JLabel("<html>Dayon! : <a href=''>" + Babylon.translate("synopsys") + ".</a></html>");
				info.setAlignmentX(Component.LEFT_ALIGNMENT);
				info.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						browse(HTTP_HOME);
					}
				});

				final JLabel version = new JLabel("<html>" + Babylon.translate("version.installed") + " : <a href=''>" + Version.get() + "</a></html>");
				version.setAlignmentX(Component.LEFT_ALIGNMENT);
				version.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						browse(Version.RELEASE_LOCATION + Version.get());
					}
				});

				final JLabel latest = new JLabel("<html>" + Babylon.translate("version.latest") + " : <a href=''>" + Version.get().getLatestRelease() + "</a></html>");
				version.setAlignmentX(Component.LEFT_ALIGNMENT);
				latest.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						browse(Version.RELEASE_LOCATION + Version.get().getLatestRelease());
					}
				});

				final JLabel support = new JLabel("<html>" + Babylon.translate("support") + " : <a href=''>" + HTTP_SUPPORT + "</a></html>");
				support.setAlignmentX(Component.LEFT_ALIGNMENT);
				support.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						browse(HTTP_SUPPORT);
					}
				});

				final JLabel feedback = new JLabel("<html>" + Babylon.translate("feedback") + " : <a href=''>" + HTTP_FEEDBACK + "</a></html>");
				feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
				feedback.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						browse(HTTP_FEEDBACK);
					}
				});

				final JScrollPane spane = new JScrollPane(props);
				spane.setAlignmentX(Component.LEFT_ALIGNMENT);

				panel.add(Box.createVerticalStrut(10));
				panel.add(info);
				panel.add(Box.createVerticalStrut(5));
				panel.add(version);
				if (Version.get().getLatestRelease() != null) {
					panel.add(Box.createVerticalStrut(5));
					panel.add(latest);
				}
				panel.add(Box.createVerticalStrut(10));
				panel.add(spane);
				panel.add(Box.createVerticalStrut(10));
				panel.add(support);
				panel.add(Box.createVerticalStrut(5));
				panel.add(feedback);

				JOptionPane.showMessageDialog(BaseFrame.this, panel, Babylon.translate("system.info"), JOptionPane.INFORMATION_MESSAGE,
						ImageUtilities.getOrCreateIcon(ImageNames.APP_LARGE));

			}
		};

		showSystemInfo.putValue(Action.NAME, "showSystemInfo");
		showSystemInfo.putValue(Action.SHORT_DESCRIPTION, Babylon.translate("system.info.show"));
		showSystemInfo.putValue(Action.SMALL_ICON, ImageUtilities.getOrCreateIcon(ImageNames.INFO));

		return showSystemInfo;
	}

	protected Action createShowHelpAction() {
		final Action showHelp = new AbstractAction() {
			@Override
            public void actionPerformed(ActionEvent ev) {
                browse(SystemUtilities.getLocalIndexHtml());
			}
		};

		showHelp.putValue(Action.NAME, "showHelp");
		showHelp.putValue(Action.SHORT_DESCRIPTION, Babylon.translate("help"));
		showHelp.putValue(Action.SMALL_ICON, ImageUtilities.getOrCreateIcon(ImageNames.HELP));

		return showHelp;
	}
	
	private static void browse(String url) {
		try {
			browse(new URI(url));
		} catch (URISyntaxException ex) {
			Log.warn(ex);
		}
	}
	
	private static void browse(URI uri) {
		try {
			if (Desktop.isDesktopSupported()) {
				final Desktop desktop = Desktop.getDesktop();
				if (desktop.isSupported(Desktop.Action.BROWSE)) {
					desktop.browse(uri);
				}
			}
		} catch (Exception ex) {
			Log.warn(ex);
		}
	}

	public ToolBar getToolBar() {
		return toolBar;
	}
}
