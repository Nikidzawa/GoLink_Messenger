package ru.nikidzawa.golink.network;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;

public class TCPConnection {
    private final Socket socket;
    private Thread thread;
    private final TCPConnectionListener listener;
    private final DataOutputStream ous;
    private final DataInputStream ois;
    @Getter
    private final String userId;
    @SneakyThrows
    public TCPConnection (Socket socket, TCPConnectionListener listener, String userId, DataInputStream ois) {
        this.ois = ois;
        this.socket = socket;
        ous = new DataOutputStream(socket.getOutputStream());
        this.listener = listener;
        this.userId = userId;
        start();
    }

    @SneakyThrows
    public TCPConnection(Socket socket, TCPConnectionListener listener, String userId) {
        this.socket = socket;
        this.listener = listener;
        this.userId = userId;
        ous = new DataOutputStream(socket.getOutputStream());
        ois = new DataInputStream(socket.getInputStream());
        setUserId();
        start();
    }
    @SneakyThrows
    private void setUserId () {
        ous.writeUTF(userId);
        ous.flush();
    }

    private void start () {
        thread = new Thread(() -> {
            listener.onConnectionReady(TCPConnection.this);
            while (!Thread.interrupted()) {
                try {
                    switch (ois.readInt()) {
                        case 30 -> listener.onReceiveMessage(TCPConnection.this, ois.readUTF());
                        case 60 -> {
                            int length = ois.readInt();
                            byte[] photoBytes = new byte[length];
                            ois.readFully(photoBytes);
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
                ous.writeInt(30);
                ous.writeUTF(string);
                ous.flush();
            }
        } catch (IOException e) {
            disconnect();
        }
    }

    @SneakyThrows
    public synchronized void sendPhoto (byte[] bytes) {
        ous.writeInt(60);
        ous.writeInt(bytes.length);
        ous.write(bytes);
        ous.flush();
    }

    @SneakyThrows
    public synchronized void disconnect() {
        thread.interrupt();
        socket.close();
    }
}