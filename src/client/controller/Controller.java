package client.controller;

import client.net.Broadcast;
import client.net.ServerConnection;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class Controller {
    private final ServerConnection serverConnection = new ServerConnection();

    public void connect(String host, int port, Broadcast caster) {
        CompletableFuture.runAsync(() ->{
            try {
                serverConnection.connect(host, port, caster);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).thenRun(() -> caster.relay("Connected to " + host + " on port " + port));
    }

    public void disconnect() throws IOException {
        //Question: no comp future?
        serverConnection.disconnect();
    }

    public void username(String name) {
        CompletableFuture.runAsync(() -> serverConnection.username(name));
    }

    public void startGame() {
        CompletableFuture.runAsync(() -> serverConnection.startGame());
    }

    public void guess(String lit) {
        CompletableFuture.runAsync(() -> serverConnection.guess(lit));
    }
    public void getScore() {
        CompletableFuture.runAsync(() -> serverConnection.getScore());
    }

    public void getRules() {
        CompletableFuture.runAsync(() -> serverConnection.getRules());
    }
}
