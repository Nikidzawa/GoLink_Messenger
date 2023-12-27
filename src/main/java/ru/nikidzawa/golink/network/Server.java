package ru.nikidzawa.golink.network;

import lombok.Getter;
import lombok.SneakyThrows;

import ru.nikidzawa.golink.services.SystemOfControlServers.SOCSConnection;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class Server implements TCPConnectionListener {
    public Map<String, TCPConnection> connections = new HashMap<>();
    @Getter
    private final int PORT;
    public ServerSocket serverSocket;

    @SneakyThrows
    public Server(int port) {
        serverSocket = new ServerSocket(port);
        PORT = serverSocket.getLocalPort();
    }

    public void start() {
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
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        connections.put(tcpConnection.getUserId(), tcpConnection);
    }

    @Override
    public synchronized void onReceiveMessage(TCPConnection tcpConnection, String string) {
        sendMessage(tcpConnection, string);
    }

    @Override
    @SneakyThrows
    public synchronized void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(tcpConnection.getUserId());
        if (connections.isEmpty()) {
            serverSocket.close();
            new SOCSConnection().RELEASE_PORT(PORT);
        }
    }

    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception ex) {
        System.out.println("TCP ex " + ex);
    }

    public void sendMessage(TCPConnection tcpConnection, String message) {
        System.out.println(connections.size());
        String userId = tcpConnection.getUserId();
//        String messageWithUserId = userId + ": " + message;

        for (Map.Entry<String, TCPConnection> entry : connections.entrySet()) {
            if (!entry.getKey().equals(userId)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    @Override
    public void onReceiveImage(TCPConnection tcpConnection, byte[] image) {
        System.out.println(connections.size());
        String userId = tcpConnection.getUserId();

        for (Map.Entry<String, TCPConnection> entry : connections.entrySet()) {
            if (!entry.getKey().equals(userId)) {
                entry.getValue().sendPhoto(image);
            }
        }
    }
}