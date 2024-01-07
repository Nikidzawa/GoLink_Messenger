package ru.nikidzawa.golink.services.GoMessage;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPBroker  {
    private final Socket socket;
    private Thread thread;
    private final GoMessageListener listener;
    private final BufferedReader in;
    final BufferedWriter out;
    @Getter
    private final String userId;

    @SneakyThrows
    public TCPBroker (Socket socket, GoMessageListener listener, String userId, BufferedReader in) {
        this.in = in;
        this.socket = socket;
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.listener = listener;
        this.userId = userId;
        start();
    }

    @SneakyThrows
    public TCPBroker(Socket socket, GoMessageListener listener, String userId) {
        this.socket = socket;
        this.listener = listener;
        this.userId = userId;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        setUserId();
        start();
    }
    @SneakyThrows
    private void setUserId () {
        out.write(userId + "\n");
        out.flush();
    }
    private void start() {
        thread = new Thread(() -> {
            listener.onConnectionReady(TCPBroker.this);
            while (!Thread.interrupted()) {
                try {
                    String message = in.readLine();
                    if (message == null) {break;}
                    listener.onReceiveMessage(TCPBroker.this, message);
                } catch (Exception e) {
                    break;
                }
            }
            listener.onDisconnect(TCPBroker.this);
        });
        thread.start();
    }
    public synchronized void sendMessage (String string){
        try {
            if (string != null) {
                out.write(string + "\n");
                out.flush();
            }
        } catch (IOException ex) {
            disconnect();
            throw new RuntimeException(ex);
        }
    }
    public synchronized void CHECK_USER_STATUS (Long interlocutorId) {
        try {
            out.write("CHECK_USER_STATUS:" + interlocutorId + "\n");
            out.flush();
        } catch (IOException ex) {
            disconnect();
            throw new RuntimeException(ex);
        }
    }
    public synchronized void ADD_MESSAGE_ON_CASH (Long receiver, Long chatId, Long messageId, String text) {
        try {
            out.write("ADD_MESSAGE_ON_CASH:" + receiver + ":" + chatId + ":" + messageId + ":" + text + "\n");
            out.flush();
        } catch (IOException ex) {
            disconnect();
            throw new RuntimeException(ex);
        }
    }
    public synchronized void DELETE_MESSAGE (Long interlocutorId, Long chatId, Long messageId, int messagePosition) {
        try {
            out.write("DELETE_MESSAGE:" + interlocutorId + ":" + chatId + ":" + messageId + ":" + messagePosition + "\n");
            out.flush();
        } catch (IOException ex) {
            disconnect();
            throw new RuntimeException(ex);
        }
    }
    public synchronized void CREATE_NEW_CHAT_ROOM (Long interlocutorId, Long userId, Long chatId, String name) {
        try {
            out.write("CREATE_NEW_CHAT_ROOM:" + interlocutorId + ":" + userId + ":" + chatId + ":" + name + "\n");
            out.flush();
        } catch (IOException ex) {
            disconnect();
            throw new RuntimeException(ex);
        }
    }

    @SneakyThrows
    public synchronized void disconnect() {
        thread.interrupt();
        socket.close();
    }
}