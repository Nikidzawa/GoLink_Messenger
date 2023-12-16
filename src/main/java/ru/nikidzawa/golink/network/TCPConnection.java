package ru.nikidzawa.golink.network;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPConnection {
    private final Socket socket;
    private final Thread thread;
    private final TCPConnectionLink link;
    private final BufferedReader in;
    private final BufferedWriter out;
    @Getter
    private final String userId;

    @SneakyThrows
    public TCPConnection(Socket socket, TCPConnectionLink link, String userId) {
        this.socket = socket;
        this.link = link;
        this.userId = userId;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        sendMessage(userId);
        thread = new Thread(() -> {
            link.onConnectionReady(TCPConnection.this);
            while (!Thread.interrupted()) {
                try {
                    String message = in.readLine();
                    if (message == null) {break;}
                    link.onReceiveMessage(TCPConnection.this, message);
                } catch (IOException e) {
                    link.onException(TCPConnection.this, e);
                    link.onDisconnect(TCPConnection.this);
                    break;
                }
            }
            link.onDisconnect(TCPConnection.this);
        });
        thread.start();
    }

    public synchronized void sendMessage(String string) {
        try {
            if (string != null) {
                out.write(string + "\n");
                out.flush();
            }
        } catch (IOException e) {
            link.onException(this, e);
            disconnect();
        }
    }

    public synchronized void disconnect() {
        thread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            link.onException(this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection: " + socket.getInetAddress() + ":" + socket.getPort() + " (UserId: " + userId + ")";
    }
}
