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

                new TCPBroker(socket, this, userId, in);
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
        String[] strings = string.split(":", 3);
        String command = strings[0];
        String userId = strings[1];
        try {
            switch (command) {
                case "ADD_MESSAGE_ON_CASH" -> connections.get(userId).sendMessage(command + ":" + strings[2]);
                case "DELETE_MESSAGE" -> connections.get(userId).sendMessage(command + ":" + strings[2]);
                case "CHECK_USER_STATUS" -> tcpBroker.sendMessage(command + ":" + connections.containsKey(userId));
                case "CREATE_NEW_CHAT_ROOM" -> connections.get(userId).sendMessage(command + ":" + strings[2]);
            }
        } catch (NullPointerException ex) {
            System.out.println("Получатель не в сети");
        }
    }

    @Override
    public void onDisconnect(TCPBroker tcpBroker) {
        connections.remove(tcpBroker.getUserId());
    }
}