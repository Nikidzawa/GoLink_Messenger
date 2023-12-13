package ru.nikidzawa.golink.services.SystemOfControlServers;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Component
public class SOCSConnection {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    @SneakyThrows
    public SOCSConnection () {
            socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @SneakyThrows
    public String CREATE_SERVER () {
        out.println("CREATE_SERVER");
        return in.readLine();
    }
    @SneakyThrows
    public void GET_SERVERS_PORTS () {
        out.println("GET_SERVERS_PORTS");
    }
}
