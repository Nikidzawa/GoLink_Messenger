package ru.nikidzawa.golink.network;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server implements ServerListener {
    private HashMap<Integer, TCPConnection> connections = new HashMap<>();
    @Getter
    private final int PORT = 8081;
    public ServerSocket serverSocket;

    @SneakyThrows
    public static void main(String[] args) {
        new Server();
    }

    @SneakyThrows
    public Server() {
        System.out.println("Сервер запущен");
        serverSocket = new ServerSocket(PORT);
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                DataInputStream in = new DataInputStream(socket.getInputStream());
                String userId = in.readUTF();

                new TCPConnection(socket, this, userId, in);
            } catch (IOException ex) {
                System.out.println("Сервер на порту " + PORT + " прекратил свою работу");
                break;
            }
        }
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {
        connections.put(Integer.parseInt(tcpConnection.getUserId()), tcpConnection);
    }

    @Override
    public void onReceiveMessage(TCPConnection tcpConnection, String string) {
        String[] strings = string.split(":", 3);
        String command = strings[0];
        int userId = Integer.parseInt(strings[1]);
        try {
            switch (command) {
                case "DELETE", "CREATE_NEW_CHAT_ROOM" ->
                        connections.get(userId).sendMessage(command + ":" + strings[2]);
                case "CHANGE_USER_STATUS" -> tcpConnection.sendMessage(command + ":" + connections.containsKey(userId));
            }
        } catch (NullPointerException ex) {
            System.out.println("Получатель не в сети");
        }
    }

    @Override
    public void onReceiveFile(TCPConnection tcpConnection, String protocol, byte[] content) {
        String[] strings = protocol.split(":", 3);
        String command = strings[0];
        int userId = Integer.parseInt(strings[1]);
        try {
            switch (command) {
                case "POST", "EDIT" -> connections.get(userId).sendFile(command + ":" + strings[2], content);
            }
        } catch (NullPointerException ex) {
            System.out.println("Получатель не в сети");
        }
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(Integer.parseInt(tcpConnection.getUserId()));
    }
}