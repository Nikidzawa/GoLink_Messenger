package ru.nikidzawa.networkAPI.network;

public interface ServerListener {
    void onConnectionReady(TCPConnection TCPConnection);

    void onReceiveMessage(TCPConnection TCPConnection, String protocol);

    void onReceiveFile(TCPConnection TCPConnection, String protocol, byte[] content);

    void onDisconnect(TCPConnection TCPConnection);
}