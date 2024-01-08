package ru.nikidzawa.golink.services.SystemOfControlServers;

import ru.nikidzawa.golink.network.Server;
import ru.nikidzawa.golink.services.GoMessage.MessageBroker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class SOCS {
    public HashMap<Integer, Server> servers = new HashMap<>();

    public static void main(String[] args) {
        new Thread(MessageBroker::new).start();
        new Thread(SOCS::new).start();
    }

    public SOCS() {
        System.out.println("СИСТЕМА КОНТРОЛЯ СЕРВЕРОВ БЫЛА УСПЕШНО ЗАПУЩЕНА" +
                "\n-----------------------------------------------");
        int PORT = 8080;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        String[] strings = in.readLine().split(":", 2);
        String command = strings[0];
        int CHAT_ID = Integer.parseInt(strings[1]);
        try {
            switch (command) {
                case "CREATE_SERVER" -> {
                    if (servers.containsKey(CHAT_ID)) {
                        int PORT = servers.get(CHAT_ID).getPORT();
                        out.println(PORT);
                        System.out.println("JOIN IN SERVER: " + PORT + " CHAT ID: " + CHAT_ID);
                    } else {
                        Server server = new Server(0, CHAT_ID);
                        int PORT = server.getPORT();
                        System.out.println("CREATE NEW SERVER ON PORT: " + PORT + " CHAT ID: " + CHAT_ID);
                        servers.put(CHAT_ID, server);
                        new Thread(server::start).start();
                        out.println(PORT);
                    }
                }
                case "RELEASE_PORT" -> {
                    servers.remove(CHAT_ID);
                    System.out.println("Чат " + CHAT_ID + " очищен из кэша");
                }
            }
            clientSocket.close();
        } catch (RuntimeException exception) {
            throw new RuntimeException(exception);
        }
    }
}