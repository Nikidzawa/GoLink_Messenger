package ru.nikidzawa.golink.network;

public interface TCPConnectionListener {
    void onConnectionReady(TCPConnection tcpConnection);
    void onReceiveMessage(TCPConnection tcpConnection, String string);
    void onReceiveImage(TCPConnection tcpConnection, byte[] image);
    void onDisconnect(TCPConnection tcpConnection);
}