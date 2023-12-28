package ru.nikidzawa.golink.services.GoMessage;

public interface GoMessageListener {
    void onConnectionReady (TCPBroker tcpBroker);
    void onReceiveMessage (TCPBroker tcpBroker, String string);
    void onDisconnect (TCPBroker tcpBroker);
}