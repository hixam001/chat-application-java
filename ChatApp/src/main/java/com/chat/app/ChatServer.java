package com.chat.app;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.SQLException;

public class ChatServer {

    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = Collections.synchronizedSet(new HashSet<>());
    private static ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws IOException {
        System.out.println("Chat server started on port " + PORT);

        try {
            DatabaseHelper.initialize();
        } catch (SQLException e) {
            System.err.println("Server database initialization error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        ServerSocket listener = new ServerSocket(PORT);

        try {
            while (true) {
                pool.execute(new ClientHandler(listener.accept()));
            }
        } finally {
            pool.shutdown();
            listener.close();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if (clientMessage.startsWith("LOGIN_REQUEST:")) {
                        String[] parts = clientMessage.substring("LOGIN_REQUEST:".length()).split(":");
                        if (parts.length == 2) {
                            String reqUsername = parts[0];
                            String reqPassword = parts[1];
                            try {
                                if (DatabaseHelper.validateUser(reqUsername, reqPassword)) {
                                    this.username = reqUsername;
                                    out.println("LOGIN_SUCCESS:" + this.username);
                                    System.out.println("User logged in: " + this.username);
                                    clientWriters.add(out);
                                    broadcastMessage(this.username + " has joined the chat.");
                                    break;
                                } else {
                                    out.println("LOGIN_FAILED:Invalid username or password.");
                                    System.out.println("Login failed for: " + reqUsername);
                                }
                            } catch (SQLException e) {
                                out.println("LOGIN_FAILED:Database error during login.");
                                System.err.println("Database error during login for " + reqUsername + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            out.println("LOGIN_FAILED:Invalid login request format.");
                        }
                    } else if (clientMessage.startsWith("REGISTER_REQUEST:")) {
                        String[] parts = clientMessage.substring("REGISTER_REQUEST:".length()).split(":");
                        if (parts.length == 2) {
                            String reqUsername = parts[0];
                            String reqPassword = parts[1];
                            try {
                                if (DatabaseHelper.registerUser(reqUsername, reqPassword)) {
                                    out.println("REGISTER_SUCCESS:");
                                    System.out.println("User registered: " + reqUsername);
                                } else {
                                    out.println("REGISTER_FAILED:Username already exists.");
                                    System.out.println("Registration failed: Username " + reqUsername + " already exists.");
                                }
                            } catch (SQLException e) {
                                out.println("REGISTER_FAILED:Database error during registration.");
                                System.err.println("Database error during registration for " + reqUsername + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            out.println("REGISTER_FAILED:Invalid registration request format.");
                        }
                    } else {
                        out.println("ERROR:Please log in or register first.");
                    }
                }

                if (this.username != null) {
                    while ((clientMessage = in.readLine()) != null) {
                        broadcastMessage(this.username + ": " + clientMessage);
                    }
                }

            } catch (IOException e) {
                if (this.username != null) {
                    System.err.println("Client handler error for " + this.username + ": " + e.getMessage());
                } else {
                    System.err.println("Client handler error for unauthenticated client " + socket.getInetAddress().getHostAddress() + ": " + e.getMessage());
                }
            } finally {
                if (this.username != null) {
                    clientWriters.remove(out);
                    broadcastMessage(this.username + " has left the chat.");
                    System.out.println(this.username + " disconnected.");
                } else {
                    System.out.println("Unauthenticated client " + socket.getInetAddress().getHostAddress() + " disconnected.");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void broadcastMessage(String message) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }
}