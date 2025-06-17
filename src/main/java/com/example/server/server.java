package com.example.server;

import com.example.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class server {

    private static final int PORT = 5050;
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Спроба запустити сервер на порту " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущено на порту " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new InitialConnectionHandler(clientSocket)).start();
            }
        } catch (BindException e) {
            System.err.println("Порт " + PORT + " вже зайнятий. Сервер не може бути запущений.");
        } catch (IOException e) {
            System.err.println("Помилка вводу/виводу при запуску сервера:");
            e.printStackTrace();
        }
    }

    private static class InitialConnectionHandler implements Runnable {
        private final Socket socket;

        public InitialConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String username = reader.readLine();

                if (username == null || username.isEmpty() || clients.containsKey(username)) {
                    socket.close();
                    return;
                }

                System.out.println("Користувач приєднався: " + username);

                ClientHandler handler = new ClientHandler(username, socket);
                clients.put(username, handler);

                broadcastUserList();

                new Thread(handler).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final String username;
        private final Socket socket;
        private final PrintWriter writer;
        private final BufferedReader reader;

        public ClientHandler(String username, Socket socket) throws IOException {
            this.username = username;
            this.socket = socket;
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.equals("__exit__")) {
                        break;
                    }

                    // Пропускаємо не-XML повідомлення
                    if (!line.trim().startsWith("<message>")) {
                        continue;
                    }

                    Message message = Message.fromXML(line);

                    if (message != null && message.receiver != null) {
                        ClientHandler receiverHandler = clients.get(message.receiver);
                        ClientHandler senderHandler = clients.get(message.sender);

                        if (receiverHandler != null) {
                            receiverHandler.send(message.toXML());
                        }
                        if (senderHandler != null && !message.sender.equals(message.receiver)) {
                            senderHandler.send(message.toXML());
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Втрата зв'язку з користувачем: " + username);
            } finally {
                disconnect();
            }
        }

        public void send(String xml) {
            writer.println(xml);
        }

        private void disconnect() {
            try {
                System.out.println("Користувач від'єднався: " + username);
                clients.remove(username);
                broadcastUserList();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void broadcastUserList() {
        String usersLine = "__users__:" + String.join(",", clients.keySet());
        for (ClientHandler handler : clients.values()) {
            handler.send(usersLine);
        }
    }
}