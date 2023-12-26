package ru.nikidzawa.golink.services.GoMessage;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPBroker  {
    private final Socket socket;
    private final Thread thread;
    private final GoMessageListener listener;
    private final BufferedReader in;
    private final BufferedWriter out;
    @Getter
    private final String userId;

    @SneakyThrows
    public TCPBroker(Socket socket, GoMessageListener listener, String userId) {
        this.socket = socket;
        this.listener = listener;
        this.userId = userId;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        sendMessage(userId);
        thread = new Thread(() -> {
            listener.onConnectionReady(TCPBroker.this);
            while (!Thread.interrupted()) {
                try {
                    String message = in.readLine();
                    if (message == null) {break;}
                    listener.onReceiveMessage(TCPBroker.this, message);
                } catch (Exception e) {
                    listener.onDisconnect(TCPBroker.this);
                    break;
                }
            }
        });
        thread.start();
    }

    public synchronized void sendMessage(String string){
        try {
            if (string != null) {
                out.write(string + "\n");
                out.flush();
            }
        } catch (IOException e) {
            listener.onException(this, e);
            try {
                socket.close();
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }

    public synchronized void disconnect() {
        thread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            listener.onException(this, e);
        }
    }

}