package ru.nikidzawa.golink.network;

public interface TCPConnectionLink {
    void onConnectionReady(TCPConnection tcpConnection);
    void onReceiveMessage(TCPConnection tcpConnection, String string);
    void onDisconnect(TCPConnection tcpConnection);
    void onException(TCPConnection tcpConnection, Exception ex);
}