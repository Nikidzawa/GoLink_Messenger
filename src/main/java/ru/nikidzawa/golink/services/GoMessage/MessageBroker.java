package ru.nikidzawa.golink.services.GoMessage;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class MessageBroker implements GoMessageListener{
    public HashMap<String, TCPBroker> connections = new HashMap<>();
    @Getter
    private final int PORT = 8081;
    public ServerSocket serverSocket;

    @SneakyThrows
    public MessageBroker() {
        serverSocket = new ServerSocket(PORT);
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String userId = in.readLine();

                new TCPBroker(socket, this, userId);
            } catch (IOException ex) {
                System.out.println("Сервер на порту " + PORT + " прекратил свою работу");
                break;
            }
        }
    }

    @Override
    public void onConnectionReady(TCPBroker tcpBroker) {
        connections.put(tcpBroker.getUserId(), tcpBroker);
    }

    @Override
    public void onReceiveMessage(TCPBroker tcpBroker, String string) {
        String[] strings = string.split(":");
        String command = strings[0];
        String userId = strings[1];
        String value;
        try {
            value = strings[2];
        } catch (ArrayIndexOutOfBoundsException ex) {
            value = null;
        }
        switch (command) {
            case "UPDATE_CHAT_ROOMS" :
                try {
                    connections.get(userId).sendMessage("UPDATE_CHAT_ROOMS");
                } catch (NullPointerException ex) {
                    System.out.println("Пользователи не найдены");
                }
                break;

            case "UPDATE_MESSAGES" :
                connections.get(userId).sendMessage("UPDATE_MESSAGES");
                break;

            case "NOTIFICATION" :
                connections.get(userId).sendMessage("NOTIFICATION:" + value);
                break;
        }
    }

    @Override
    public void onDisconnect(TCPBroker tcpBroker) {

    }

    @Override
    public void onException(TCPBroker tcpBroker, Exception ex) {

    }
}