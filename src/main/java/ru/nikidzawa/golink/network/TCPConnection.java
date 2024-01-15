package ru.nikidzawa.golink.network;

import lombok.Getter;
import lombok.SneakyThrows;
import ru.nikidzawa.golink.store.MessageType;

import java.io.*;
import java.net.Socket;

public class TCPConnection {
    private final Socket socket;
    private Thread thread;
    private final ServerListener listener;
    private final DataOutputStream out;
    private final DataInputStream in;
    @Getter
    private final String userId;

    @SneakyThrows
    public TCPConnection(Socket socket, ServerListener listener, String userId, DataInputStream in) {
        this.in = in;
        this.socket = socket;
        out = new DataOutputStream(socket.getOutputStream());
        this.listener = listener;
        this.userId = userId;
        start();
    }

    @SneakyThrows
    public TCPConnection(Socket socket, ServerListener listener, String userId) {
        this.socket = socket;
        this.listener = listener;
        this.userId = userId;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        setUserId();
        start();
    }

    @SneakyThrows
    private void setUserId () {
        out.writeUTF(userId);
        out.flush();
    }

    private void start() {
        thread = new Thread(() -> {
            listener.onConnectionReady(TCPConnection.this);
            while (!Thread.interrupted()) {
                try {
                switch (in.readInt()) {
                    case 50 -> listener.onReceiveMessage(TCPConnection.this, in.readUTF());
                    case 100 -> {
                        String protocol = in.readUTF();
                        int length = in.readInt();
                        byte[] file = new byte[length];
                        in.readFully(file);
                        listener.onReceiveFile(TCPConnection.this, protocol, file);
                    }
                }
                } catch (Exception e) {
                    break;
                }
            }
            listener.onDisconnect(TCPConnection.this);
        });
        thread.start();
    }

    public synchronized void sendMessage (String protocol){
        try {
            if (protocol != null) {
                out.writeInt(50);
                out.writeUTF(protocol);
                out.flush();
            }
        } catch (IOException ex) {
            disconnect();
            throw new RuntimeException(ex);
        }
    }
    public synchronized void sendFile (String protocol, byte[] metadata) {
        try {
            out.writeInt(100);
            out.writeUTF(protocol);
            out.writeInt(metadata.length);
            out.write(metadata);
            out.flush();
        } catch (IOException ex) {
            disconnect();
            throw new RuntimeException(ex);
        }
    }

    public void CHECK_USER_STATUS (Long receiverID) {
        sendMessage("CHANGE_USER_STATUS:" + receiverID);
    }

    public void CREATE_NEW_CHAT_ROOM (Long receiverID, Long participantPersonalChatId) {
        sendMessage("CREATE_NEW_CHAT_ROOM:" + receiverID + ":" + participantPersonalChatId);
    }

    public void POST(Long receiverID, Long chatID, Long messageID, String message) {
        sendMessage("POST:" + receiverID + ":" + chatID + ":" + messageID + ":" + message);
    }

    public void EDIT (Long receiverID, Long chatID, Long messageID, String message) {
        sendMessage("EDIT:" + receiverID + ":" + chatID + ":" + messageID + ":" + message);
    }

    public void DELETE (Long receiverID, Long chatID, Long messageID) {
        sendMessage("DELETE:" + receiverID + ":" + chatID + ":" + messageID);
    }

    public void FILE_POST (Long receiverID, Long chatID, Long messageID, MessageType messageType, String message, byte[] metadata) {
        sendFile("POST:" + receiverID + ":" + chatID + ":" + messageID + ":" + messageType.name() + ":" + message, metadata);
    }
    public void FILE_EDIT (Long receiverID, Long chatID, Long messageID, MessageType messageType, String message, byte[] metadata) {
        sendFile("EDIT:" + receiverID + ":" + chatID + ":" + messageID + ":" + messageType + ":" + message, metadata);
    }

    @SneakyThrows
    public synchronized void disconnect() {
        thread.interrupt();
        socket.close();
    }
}