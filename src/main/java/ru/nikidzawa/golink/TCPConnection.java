package ru.nikidzawa.golink;

import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPConnection {
    private final Socket socket;
    private final Thread thread;
    TCPConnectionLink link;

    private final BufferedReader in;
    private final BufferedWriter out;

    public TCPConnection (TCPConnectionLink link, String ipAddr, int port) throws IOException {
        this(new Socket(ipAddr, port), link);
    }

    public TCPConnection (Socket socket, TCPConnectionLink link) throws IOException {
        this.socket = socket;
        this.link = link;
        in = new BufferedReader(new InputStreamReader(socket. getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                link.onConnectionReady(TCPConnection.this);
                while (!thread.isInterrupted()) {
                    try {
                        link.onReceiveMessage(TCPConnection.this, in.readLine());
                    } catch (IOException e) {
                        link.onException(TCPConnection.this, e);
                    }
                }
                link.onDisconnect(TCPConnection.this);
            }
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
            link.onException(TCPConnection.this, e);
            disconnect();
        }
    }
    public synchronized void disconnect () {
        thread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            link.onException(TCPConnection.this, e);
        }
    }
    @Override
    public String toString() {
        return "TCPConnection: " + socket.getInetAddress() + ": " + socket.getPort();
    }
}
