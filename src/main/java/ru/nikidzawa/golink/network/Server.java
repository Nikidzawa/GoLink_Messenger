package ru.nikidzawa.golink.network;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;


public class Server implements TCPConnectionLink {
    public List <TCPConnection> connections = new ArrayList<>();
    @Getter
    private int PORT;
    ServerSocket serverSocket;
    @SneakyThrows
    public Server (int port) {
        serverSocket = new ServerSocket(port);
        PORT = serverSocket.getLocalPort();
    }

    public void start () {
        while (true) {
            try {
                new TCPConnection(serverSocket.accept(), this);
            } catch (IOException ex) {
                System.out.println("Подтверждение завершения работы сервера на порту " + PORT + ex);
                break;
            }
        }
    }


    @Override
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        connections.add(tcpConnection);
        System.out.println(tcpConnection);
    }

    @Override
    public synchronized void onReceiveMessage(TCPConnection tcpConnection, String string) {
        sendMessage(tcpConnection, string);
        System.out.println(string);
    }

    @Override
    @SneakyThrows
    public synchronized void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(tcpConnection);
        if (connections.isEmpty()) {
            serverSocket.close();
            System.out.println("Сервер на порту " + PORT + " прекратил работу");
        }
        System.out.println("Client disconnect " + tcpConnection);
    }

    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception ex) {
        System.out.println("TCP ex " + ex);
    }
    public void sendMessage (TCPConnection tcpConnection, String message) {
        for (TCPConnection connection : connections) {
            if (connection != tcpConnection) {
                connection.sendMessage(message);
            }
        }
    }
}
