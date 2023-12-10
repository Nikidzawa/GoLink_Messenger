package ru.nikidzawa.golink.SystemOfControlServers;

import ru.nikidzawa.golink.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;


public class SOCS {
    public static void main(String[] args) {
        new SOCS();
    }
    public HashMap<Integer, Server> servers = new HashMap<>();
    private int PORT = 8080;
    public SOCS() {
        new Thread(() -> new Server(8081).start()).start();
        new Thread(() -> new Server(8082).start()).start();
        System.out.println("SOCS IS ACTIVE\n--------------");
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

        switch (in.readLine()) {
            case "CREATE_SERVER" :
                Server server = new Server(0);
                int port = server.getPORT();
                System.out.println("CREATE NEW SERVER ON PORT: " + port);
                servers.put(port, server);
                out.println(port);
                server.start();
                break;
            case "GET_SERVERS_PORTS":
                for (Integer key : servers.keySet()) {
                    System.out.println(key);
                }
                break;
        }
        clientSocket.close();
    }

}
