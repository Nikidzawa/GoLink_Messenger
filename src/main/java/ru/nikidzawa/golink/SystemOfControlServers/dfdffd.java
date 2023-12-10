package ru.nikidzawa.golink.SystemOfControlServers;


import ru.nikidzawa.golink.Server;

public class dfdffd {
    public static void main(String[] args) {
        new Thread(() -> new Server(8082).start()).start();
    }
}
