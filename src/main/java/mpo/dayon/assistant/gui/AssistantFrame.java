package mpo.dayon.assistant.gui;

import mpo.dayon.common.babylon.Babylon;
import mpo.dayon.common.event.Listeners;
import mpo.dayon.common.gui.common.*;
import mpo.dayon.common.gui.statusbar.StatusBar;
import mpo.dayon.common.gui.toolbar.ToolBar;
import mpo.dayon.common.monitoring.counter.Counter;
import mpo.dayon.common.version.Version;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.awt.event.KeyEvent.VK_WINDOWS;

class AssistantFrame extends BaseFrame {

    private static final int OFFSET = 9;

    private static final int DEFAULT_FACTOR = 1;

    private final transient Listeners<AssistantFrameListener> listeners = new Listeners<>();

    private final JScrollPane assistantPanelWrapper;

    private final AssistantPanel assistantPanel;
    
    private final transient AssistantActions actions;

    private Timer sessionTimer;

    @Nullable
    private JComponent center;

    private final AtomicBoolean controlActivated = new AtomicBoolean(false);

    private final AtomicBoolean windowsKeyActivated = new AtomicBoolean(false);

    private float xFactor = DEFAULT_FACTOR;

    private float yFactor = DEFAULT_FACTOR;

    AssistantFrame(AssistantActions actions, Set<Counter<?>> counters) {
        super.setFrameType(FrameType.ASSISTANT);

        setTitle("Dayon! (" + Babylon.translate("assistant") + ") " + Version.get());
        
        this.actions = actions;
        this.actions.setSendWindowsKeyAction(createSendWindowsKeyAction());
        this.actions.setToggleControlModeAction(createToggleControlMode());

        setupToolBar(createToolBar());
        setupStatusBar(createStatusBar(counters));

        assistantPanel = new AssistantPanel();
        assistantPanel.setFocusable(false);
        assistantPanelWrapper = new JScrollPane(assistantPanel);

        addMouseListeners();

        // -------------------------------------------------------------------------------------------------------------
        // Not really needed for the time being - allows for seeing
        // the TAB with a regular KEY listener ...
        setFocusTraversalKeysEnabled(false);

        addKeyListeners();

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent ev) {
                if (controlActivated.get()) {
                    fireOnKeyReleased(-1, Character.MIN_VALUE);
                }
            }
        });

        onReady(); // the network has been before we've been registered as a listener ...
    }

    private void addMouseListeners() {
        assistantPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
                if (controlActivated.get()) {
                    fireOnMousePressed(ev.getX(), ev.getY(), ev.getButton());
                }
            }

            @Override
            public void mouseReleased(MouseEvent ev) {
                if (controlActivated.get()) {
                    fireOnMouseReleased(ev.getX(), ev.getY(), ev.getButton());
                }
            }
        });

        assistantPanel.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                if (controlActivated.get()) {
                    fireOnMouseMove(ev.getX(), ev.getY());
                }
            }

            @Override
            public void mouseMoved(MouseEvent ev) {
                if (controlActivated.get()) {
                    fireOnMouseMove(ev.getX(), ev.getY());
                }
            }
        });

        assistantPanel.addMouseWheelListener(ev -> {
            if (controlActivated.get()) {
                fireOnMouseWheeled(ev.getX(), ev.getY(), ev.getWheelRotation());
            }
        });
    }

    private void addKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ev) {
                if (controlActivated.get()) {
                    fireOnKeyPressed(ev.getKeyCode(), ev.getKeyChar());
                }
            }

            @Override
            public void keyReleased(KeyEvent ev) {
                if (controlActivated.get()) {
                    fireOnKeyReleased(ev.getKeyCode(), ev.getKeyChar());
                }
            }
        });
    }

    public void addListener(AssistantFrameListener listener) {
        listeners.add(listener);
    }

    private ToolBar createToolBar() {
        final ToolBar toolbar = new ToolBar();

        toolbar.addAction(actions.getStartAction());
        toolbar.addAction(actions.getStopAction());
        toolbar.addSeparator();
        toolbar.addAction(actions.getNetworkConfigurationAction());
        toolbar.addAction(actions.getCaptureEngineConfigurationAction());
        toolbar.addAction(actions.getCompressionEngineConfigurationAction());
        toolbar.addAction(actions.getLookAndFeelAction());
        toolbar.addToggleAction(actions.getToggleFitScreenAction());
        toolbar.addSeparator();
        toolbar.addAction(actions.getResetAction());
        toolbar.addToggleAction(actions.getToggleControlModeAction());
        toolbar.addAction(actions.getRemoteClipboardRequestAction());
        toolbar.addAction(actions.getRemoteClipboardSetAction());
        toolbar.addToggleAction(actions.getSendWindowsKeyAction());
        toolbar.addSeparator();
        toolbar.addAction(createShowInfoAction());
        toolbar.addAction(createShowHelpAction());
        toolbar.addGlue();
        toolbar.addAction(actions.getIpAddressAction());
        toolbar.addSeparator();
        toolbar.addAction(createExitAction());

        return toolbar;
    }

    private StatusBar createStatusBar(Set<Counter<?>> counters) {
        final StatusBar statusBar = new StatusBar();

        for (Counter<?> counter : counters) {
            statusBar.addSeparator();
            statusBar.addCounter(counter, counter.getWidth());
        }

        statusBar.addSeparator();
        statusBar.addRamInfo();
        statusBar.addSeparator();
        statusBar.addConnectionDuration();
        statusBar.add(Box.createHorizontalStrut(10));

        return statusBar;
    }

    private Action createToggleControlMode() {
        final Action remoteControl = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ev) {
                controlActivated.set(!controlActivated.get());
                actions.getSendWindowsKeyAction().setEnabled(controlActivated.get());
            }
        };

        remoteControl.putValue(Action.NAME, "toggleControlMode");
        remoteControl.putValue(Action.SHORT_DESCRIPTION, Babylon.translate("control.mode"));
        remoteControl.putValue(Action.SMALL_ICON, ImageUtilities.getOrCreateIcon(ImageNames.CONTROL));

        return remoteControl;
    }

    Action createSendWindowsKeyAction() {
        final Action sendWindowsKey = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                if (windowsKeyActivated.get()) {
                    fireOnKeyReleased(VK_WINDOWS, ' ');
                } else {
                    fireOnKeyPressed(VK_WINDOWS, ' ');
                }
                windowsKeyActivated.set(!windowsKeyActivated.get());
            }
        };

        sendWindowsKey.putValue(Action.NAME, "sendWindowsKey");
        sendWindowsKey.putValue(Action.SHORT_DESCRIPTION, Babylon.translate("send.windowsKey"));
        sendWindowsKey.putValue(Action.SMALL_ICON, ImageUtilities.getOrCreateIcon(ImageNames.WIN));

        return sendWindowsKey;
    }

    void onReady() {
        removeCenter();

        validate();
        repaint();

        actions.getStartAction().setEnabled(true);
        actions.getStopAction().setEnabled(false);

        actions.getNetworkConfigurationAction().setEnabled(true);
        actions.getIpAddressAction().setEnabled(true);
        actions.getCaptureEngineConfigurationAction().setEnabled(true);
        actions.getLookAndFeelAction().setEnabled(true);
        actions.getResetAction().setEnabled(false);

        disableControls();
        statusBar.setMessage(Babylon.translate("ready"));
    }

    void onHttpStarting(int port) {
        actions.getStartAction().setEnabled(false);
        actions.getStopAction().setEnabled(true);

        actions.getNetworkConfigurationAction().setEnabled(false);
        actions.getIpAddressAction().setEnabled(false);

        final ImageIcon waiting = ImageUtilities.getOrCreateIcon(ImageNames.WAITING);

        center = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                final int x = (getWidth() - waiting.getIconWidth()) / 2;
                final int y = (getHeight() - waiting.getIconHeight()) / 2;
                g.drawImage(waiting.getImage(), x, y, this);
            }
        };
        add(center, BorderLayout.CENTER);

        statusBar.setMessage(Babylon.translate("listening", port));
    }

    boolean onAccepted(Socket connection) {
        if (JOptionPane.showOptionDialog(this, Babylon.translate("connection.incoming.msg1", connection.getInetAddress().getHostAddress()),
                Babylon.translate("connection.incoming"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                ImageUtilities.getOrCreateIcon(ImageNames.USERS), OK_CANCEL_OPTIONS, OK_CANCEL_OPTIONS[1]) == 0) {
            return false;
        }

        removeCenter();

        statusBar.setMessage(Babylon.translate("connection.incoming.msg2", connection.getInetAddress().getHostAddress()));
        center = assistantPanelWrapper;
        add(center, BorderLayout.CENTER);

        actions.getResetAction().setEnabled(true);
        actions.getToggleControlModeAction().setEnabled(true);
        actions.getSendWindowsKeyAction().setEnabled(controlActivated.get());
        enableTransferControls();

        validate();
        repaint();

        return true;
    }

    void onClipboardRequested() {
        disableTransferControls();
    }

    void onClipboardSending() {
        disableTransferControls();
    }

    void onClipboardSent() {
        enableTransferControls();
    }

    void onClipboardReceived() {
        enableTransferControls();
    }

    void onSessionStarted() {
        long sessionStartTime = Instant.now().getEpochSecond();
        sessionTimer = new Timer(1000, e -> {
            final long seconds = Instant.now().getEpochSecond() - sessionStartTime;
            statusBar.setSessionDuration(String.format("%02d:%02d:%02d", seconds/3600, (seconds % 3600)/60, seconds % 60));
        });
        sessionTimer.start();
    }

    void onDisconnecting() {
        stopSessionTimer();
    }

    void onIOError(IOException error) {
        actions.getStartAction().setEnabled(false);
        actions.getStopAction().setEnabled(false);
        actions.getResetAction().setEnabled(false);
        disableControls();

        stopSessionTimer();
        removeCenter();
        validate();
        repaint();

        if (error.getMessage() != null) {
            JOptionPane.showMessageDialog(this, Babylon.translate("comm.error.msg1", Babylon.translate(error.getMessage())), Babylon.translate("comm.error"),
                    JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, Babylon.translate("comm.error.msg1", "!"), Babylon.translate("comm.error"),
                    JOptionPane.ERROR_MESSAGE);
        }

    }

    Dimension getUsableSize(int sourceWidth, int sourceHeight) {
        Dimension dimension = assistantPanelWrapper.getSize();
        dimension.setSize(dimension.getWidth() - assistantPanelWrapper.getVerticalScrollBar().getWidth() + OFFSET,
                dimension.getHeight() - assistantPanelWrapper.getHorizontalScrollBar().getHeight() + OFFSET);
        xFactor = (float) dimension.getWidth() / sourceWidth;
        yFactor = (float) dimension.getHeight() / sourceHeight;
        return dimension;
    }

    void resetFactors() {
        xFactor = DEFAULT_FACTOR;
        yFactor = DEFAULT_FACTOR;
    }

    private void disableControls() {
        controlActivated.set(false);
        windowsKeyActivated.set(false);
        actions.getToggleControlModeAction().setEnabled(false);
        actions.getSendWindowsKeyAction().setEnabled(false);
        disableTransferControls();
    }

    private void disableTransferControls() {
        actions.getRemoteClipboardSetAction().setEnabled(false);
        actions.getRemoteClipboardRequestAction().setEnabled(false);
    }

    private void enableTransferControls() {
        actions.getRemoteClipboardSetAction().setEnabled(true);
        actions.getRemoteClipboardRequestAction().setEnabled(true);
    }

    private void stopSessionTimer() {
        if (sessionTimer != null) {
            sessionTimer.stop();
        }
    }

    private void removeCenter() {
        if (center != null) {
            remove(center);
        }
    }

    void onCaptureUpdated(final BufferedImage captureImage) {
        assistantPanel.onCaptureUpdated(captureImage);
    }

    /**
     * Should not block as called from the network incoming message thread (!)
     */
    void onMouseLocationUpdated(int x, int y) {
        int xs = Math.round(x * xFactor);
        int ys = Math.round(y * yFactor);
        assistantPanel.onMouseLocationUpdated(xs, ys);
    }

    private void fireOnMouseMove(int x, int y) {
        int xs = scaleXPosition(x);
        int ys = scaleYPosition(y);
        for (final AssistantFrameListener xListener : listeners.getListeners()) {
            xListener.onMouseMove(xs, ys);
        }
    }

    private void fireOnMousePressed(int x, int y, int button) {
        int xs = scaleXPosition(x);
        int ys = scaleYPosition(y);
        for (final AssistantFrameListener xListener : listeners.getListeners()) {
            xListener.onMousePressed(xs, ys, button);
        }
    }

    private void fireOnMouseReleased(int x, int y, int button) {
        int xs = scaleXPosition(x);
        int ys = scaleYPosition(y);
        for (final AssistantFrameListener xListener : listeners.getListeners()) {
            xListener.onMouseReleased(xs, ys, button);
        }
    }

    private void fireOnMouseWheeled(int x, int y, int rotations) {
        int xs = scaleXPosition(x);
        int ys = scaleYPosition(y);
        for (final AssistantFrameListener xListener : listeners.getListeners()) {
            xListener.onMouseWheeled(xs, ys, rotations);
        }
    }

    private int scaleYPosition(int y) {
        return Math.round(y / yFactor);
    }

    private int scaleXPosition(int x) {
        return Math.round(x / xFactor);
    }

    private void fireOnKeyPressed(int keyCode, char keyChar) {
        for (final AssistantFrameListener xListener : listeners.getListeners()) {
            xListener.onKeyPressed(keyCode, keyChar);
        }
    }

    private void fireOnKeyReleased(int keyCode, char keyChar) {
        for (final AssistantFrameListener xListener : listeners.getListeners()) {
            xListener.onKeyReleased(keyCode, keyChar);
        }
    }

}
