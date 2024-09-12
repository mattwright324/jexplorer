package io.mattw.jexplorer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.Arrays;

public class Application {

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        logger.trace("Application.main({})", () -> Arrays.toString(args));

        try {
            FlatStyle.setupFromPrefName(FlatStyle.FLATLAF_MACOS_DARK.getPrefName());
            System.setProperty("flatlaf.menuBarEmbedded", "true");

            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);

            final var window = new MainWindow();
            window.setVisible(true);
        } catch (Exception e) {
            logger.error(e);
        }
    }

}
