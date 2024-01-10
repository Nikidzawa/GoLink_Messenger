package ru.nikidzawa.golink.network;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;

public class TCPConnection {
    private final Socket socket;
    private Thread thread;
    private final TCPConnectionListener listener;
    private final DataOutputStream out;
    private final DataInputStream in;
    @Getter
    private final String userId;
    @SneakyThrows
    public TCPConnection (Socket socket, TCPConnectionListener listener, String userId, DataInputStream in) {
        this.in = in;
        this.socket = socket;
        out = new DataOutputStream(socket.getOutputStream());
        this.listener = listener;
        this.userId = userId;
        start();
    }

    @SneakyThrows
    public TCPConnection(Socket socket, TCPConnectionListener listener, String userId) {
        this.socket = socket;
        this.listener = listener;
        this.userId = userId;
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        setUserId();
        start();
    }
    @SneakyThrows
    private void setUserId () {
        out.writeUTF(userId);
        out.flush();
    }

    private void start () {
        thread = new Thread(() -> {
            listener.onConnectionReady(TCPConnection.this);
            while (!Thread.interrupted()) {
                try {
                    switch (in.readInt()) {
                        case 30 -> listener.onReceiveMessage(TCPConnection.this, in.readUTF());
                        case 60 -> {
                            int length = in.readInt();
                            byte[] photoBytes = new byte[length];
                            in.readFully(photoBytes);
                            listener.onReceiveImage(TCPConnection.this, photoBytes);
                        }
                    }
                } catch (IOException ex) {
                    break;
                }
            }
            listener.onDisconnect(TCPConnection.this);
        });
        thread.start();
    }

    public synchronized void sendMessage(String string) {
        try {
            if (string != null) {
                out.writeInt(30);
                out.writeUTF(string);
                out.flush();
            }
        } catch (IOException e) {
            disconnect();
        }
    }

    @SneakyThrows
    public synchronized void sendPhoto (byte[] bytes) {
        out.writeInt(60);
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }

    @SneakyThrows
    public synchronized void disconnect() {
        thread.interrupt();
        socket.close();
    }
}