package mpo.dayon.common.error;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jetbrains.annotations.Nullable;

import mpo.dayon.common.babylon.Babylon;
import mpo.dayon.common.log.Log;

public abstract class KeyboardErrorHandler {
    @Nullable
    private static JFrame frame;

    private KeyboardErrorHandler() {
    }

    /**
     * Displays a self closing translated warning message
     */
    public static void warn(final String message) {

        if (frame != null) {
            final JLabel label = new JLabel();
            int timerDelay = 1000;
            ActionListener ac = new ActionListener() {
                int timeLeft = 4;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (timeLeft > 0) {
                        String sb = "<html>" + Babylon.translate("keyboard.error.msg1") + "<br/>" +
                                Babylon.translate("keyboard.error.msg2", message) + "<br/>" +
                                Babylon.translate("keyboard.error.msg3", message) + "</html>";
                        label.setText(sb);
                        --timeLeft;
                    } else {
                        ((Timer) e.getSource()).stop();
                        Window win = SwingUtilities.getWindowAncestor(label);
                        win.setVisible(false);
                    }
                }
            };

            Timer timer = new Timer(timerDelay, ac);
            timer.setInitialDelay(0);
            timer.start();

            JOptionPane.showMessageDialog(frame, label, Babylon.translate("keyboard.error"), JOptionPane.WARNING_MESSAGE);

        } else {
            Log.error("Unable to display error message " + message);
        }

    }

    public static void attachFrame(JFrame frame) {
        KeyboardErrorHandler.frame = frame;
    }
}
