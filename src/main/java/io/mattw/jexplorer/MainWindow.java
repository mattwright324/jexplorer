package io.mattw.jexplorer;

import com.formdev.flatlaf.FlatLaf;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTPClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Objects;

@Log4j2
public class MainWindow extends JFrame {

    public MainWindow() throws Exception {
        log.trace("MainWindow()");

        var icon = ImageIO.read(Objects.requireNonNull(getClass().getResource("/icon.png")));

        setIconImage(icon);
        setTitle("JExplorer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1000, 600);

        var centerX = (Toolkit.getDefaultToolkit().getScreenSize().width - getWidth()) / 2;
        var centerY = (Toolkit.getDefaultToolkit().getScreenSize().height - getHeight()) / 2;
        setLocation(centerX, centerY);

        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#0094FF"));

        try {
            FTPClient client = new FTPClient();
            client.setConnectTimeout(300);
            client.connect("cygwin.mirror.rafal.ca");

            if (client.login("ftp", "email@example.com")) {
                System.out.println("Login successful");
            } else {
                System.out.println("Login failed");
            }
        } catch (Exception e) {e.printStackTrace();}
    }

}
