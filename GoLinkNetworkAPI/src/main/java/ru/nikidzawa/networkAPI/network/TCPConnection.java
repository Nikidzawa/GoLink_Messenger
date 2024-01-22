package ru.nikidzawa.networkAPI.network;

import lombok.Getter;
import lombok.SneakyThrows;
import ru.nikidzawa.networkAPI.store.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
    private void setUserId() {
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

    public synchronized void sendMessage(String protocol) {
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

    public synchronized void sendFile(String protocol, byte[] metadata) {
        try {
            out.writeInt(100);
            out.writeUTF(protocol);
            try {
                out.writeInt(metadata.length);
                out.write(metadata);
            } catch (NullPointerException ex) {
                metadata = new byte[0];
                out.writeInt(metadata.length);
                out.write(metadata);
            }
            out.flush();
        } catch (IOException ex) {
            disconnect();
            throw new RuntimeException(ex);
        }
    }

    public void CHECK_USER_STATUS(long receiverID) {
        sendMessage("CHANGE_USER_STATUS:" + receiverID);
    }

    public void CREATE_NEW_CHAT_ROOM(long receiverID, long participantPersonalChatId) {
        sendMessage(String.format("CREATE_NEW_CHAT_ROOM:%d:%d", receiverID, participantPersonalChatId));
    }
    public void WRITING_STATUS (long receiverID, long chatID) {
        sendMessage(String.format("WRITING_STATUS:%d:%d", receiverID, chatID));
    }

    public void DELETE(long receiverID, long chatID, long messageID) {
        sendMessage(String.format("DELETE:%d:%d:%d", receiverID, chatID, messageID));
    }

    public void POST(long receiverID, long interlocutorPersonalChatID, long chatID, long messageID, MessageType messageType, String message, byte[] metadata) {
        sendFile(String.format("POST:%d:%d:%d:%d:%s:%s", receiverID, interlocutorPersonalChatID, chatID, messageID, messageType, message), metadata);
    }

    public void EDIT(long receiverID, long chatID, long messageID, MessageType messageType, String message, byte[] metadata) {
        sendFile(String.format("EDIT:%d:%d:%d:%s:%s", receiverID, chatID, messageID, messageType, message), metadata);
    }

    @SneakyThrows
    public synchronized void disconnect() {
        thread.interrupt();
        socket.close();
    }
}