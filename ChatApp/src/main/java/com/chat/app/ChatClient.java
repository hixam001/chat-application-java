package com.chat.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ChatClient extends JFrame {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JTextArea chatArea;
    private JTextField messageField;
    private String loggedInUsername;

    private JPanel authPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;

    private JPanel chatPanel;

    public ChatClient() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.err.println("Could not set Look and Feel: " + e.getMessage());
        }

        setTitle("Chat Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setRows(15);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        messageField = new JTextField(30);
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessageAction();
            }
        });

        authPanel = new JPanel(new GridBagLayout());
        authPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        authPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        authPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        authPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        authPanel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 5, 5, 5);
        authPanel.add(buttonPanel, gbc);

        chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(messageField, BorderLayout.CENTER);
        JButton sendButton = new JButton("Send");
        inputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        add(authPanel);
        chatPanel.setVisible(false);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                if (!username.isEmpty() && !password.isEmpty()) {
                    sendMessage("LOGIN_REQUEST:" + username + ":" + password);
                } else {
                    JOptionPane.showMessageDialog(ChatClient.this, "Username and password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                if (!username.isEmpty() && !password.isEmpty()) {
                    sendMessage("REGISTER_REQUEST:" + username + ":" + password);
                } else {
                    JOptionPane.showMessageDialog(ChatClient.this, "Username and password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessageAction();
            }
        });

        SwingUtilities.updateComponentTreeUI(this);

        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            chatArea.append("Connected to chat server.\n");

            new Thread(this::readMessages).start();

        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Could not connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            dispose();
        }
    }

    private void sendMessageAction() {
        String message = messageField.getText();
        if (message != null && !message.trim().isEmpty()) {
            sendMessage(message);
            messageField.setText("");
        }
    }

    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void readMessages() {
        try {
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                if (serverMessage.startsWith("LOGIN_SUCCESS:")) {
                    loggedInUsername = serverMessage.substring("LOGIN_SUCCESS:".length());
                    SwingUtilities.invokeLater(() -> {
                        authPanel.setVisible(false);
                        remove(authPanel);
                        add(chatPanel);
                        chatPanel.setVisible(true);
                        setTitle("Chat Client - Logged in as: " + loggedInUsername);
                        chatArea.append("You are now logged in as " + loggedInUsername + ".\n");
                        revalidate();
                        repaint();
                        messageField.requestFocusInWindow();
                    });
                } else if (serverMessage.startsWith("LOGIN_FAILED:")) {
                    final String reason = serverMessage.substring("LOGIN_FAILED:".length());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Login failed: " + reason, "Authentication Error", JOptionPane.ERROR_MESSAGE);
                        passwordField.setText("");
                    });
                } else if (serverMessage.startsWith("REGISTER_SUCCESS:")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        usernameField.setText("");
                        passwordField.setText("");
                    });
                } else if (serverMessage.startsWith("REGISTER_FAILED:")) {
                    final String reason = serverMessage.substring("REGISTER_FAILED:".length());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Registration failed: " + reason, "Authentication Error", JOptionPane.ERROR_MESSAGE);
                        passwordField.setText("");
                    });
                } else {
                    final String finalServerMessage = serverMessage;
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(finalServerMessage + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    });
                }
            }
        } catch (IOException e) {
            System.err.println("Client readMessages error: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                chatArea.append("Disconnected from server.\n");
                JOptionPane.showMessageDialog(this, "Server connection lost: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            });
            e.printStackTrace();
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient().setVisible(true));
    }
}