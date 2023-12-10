package ru.nikidzawa.golink.SystemOfControlServers;

import ru.nikidzawa.golink.Server;

public class sdfsdf {
    public static void main(String[] args) {
        new Thread(() -> new Server(8081).start()).start();
    }
}
