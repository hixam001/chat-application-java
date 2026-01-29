package com.chat.app;

import java.io.IOException;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

public class MainLauncher {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            try {
                ChatServer.main(new String[]{});
            } catch (Exception e) {
                System.err.println("Server encountered an error during startup: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                ChatClient client = new ChatClient();
                client.setVisible(true);
            });
        }
    }
}