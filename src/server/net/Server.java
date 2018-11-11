package server.net;

import server.model.DTO;
import server.model.Game;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final Server server = new Server();
    private final List<ClientHandler> clients = new ArrayList<>();
    private final int PORT_NO = 8080;
    private Game game = new Game();
    private boolean gameRunning = false;

    public static void main(String[] args) {
        server.serve();
    }

    private Server() {}

    private void serve() {
        try {
            ServerSocket listeningSocket = new ServerSocket(PORT_NO);
            while (true) {
                Socket clientSocket = listeningSocket.accept();
                startHandler(clientSocket);
            }
        }
        catch (IOException e) {
            System.out.println("Server error in server");
        }
    }

    private void startHandler(Socket clientSocket) throws SocketException {
        ClientHandler handler = new ClientHandler(this, clientSocket);
        synchronized (clients) {
            clients.add(handler);
        }
        Thread handlerThread = new Thread(handler);
        handlerThread.start();
    }

    void broadcast(String update) {
        synchronized (clients) {
            clients.forEach((client) -> client.transmit(update));
        }
    }

    void removeHandler(ClientHandler handler) {
        synchronized (clients) {
            clients.remove(handler);
        }
    }

    DTO startGame() {
        synchronized (clients) {
            gameRunning = true;
            return game.start();
        }
    }

    DTO guess(String lit) {
        return game.makeGuess(lit);
    }

    void incrementScore() {
        synchronized (clients) {
            stopGame();
            clients.forEach((client) -> client.incrementScore());
        }
    }

    void decrementScore() {
        synchronized (clients) {
            stopGame();
            clients.forEach((client) -> client.decrementScore());
        }
    }

    void stopGame() {
        gameRunning = false;
    }

    boolean isGameRunning() {
        return gameRunning;
    }

    DTO getGameStateDTO() {
        return game.getDTO();
    }

    String getRules() {
        return game.getRules();
    }
}
