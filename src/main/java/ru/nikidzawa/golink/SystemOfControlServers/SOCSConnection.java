package ru.nikidzawa.golink.SystemOfControlServers;

import lombok.SneakyThrows;
import ru.nikidzawa.golink.TCPConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SOCSConnection {

    private final static Socket socket;
    private static final BufferedReader in;
    private static final PrintWriter out;
    static {
        try {
            socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static String CREATE_SERVER () {
        out.println("CREATE_SERVER");
        return in.readLine();
    }
    @SneakyThrows
    public static void GET_SERVERS_PORTS () {
        out.println("GET_SERVERS_PORTS");
    }
}
