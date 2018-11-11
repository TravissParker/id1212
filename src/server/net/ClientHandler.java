package server.net;

import common.Command;
import server.model.DTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static common.Constants.*;

public class ClientHandler implements Runnable {
    private Server server = null;
    private Socket clientSocket;
    private BufferedReader fromClient;
    private PrintWriter toClient;
    private String player = "Anonymous"; // Fix player_int
    private boolean connected;
    private int stat = 0;

    ClientHandler(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
        connected = true;
    }

    public void run() {
        try {
            fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            boolean flush = true;
            toClient = new PrintWriter(clientSocket.getOutputStream(), flush);
        }
        catch (IOException e) {
            System.out.println(e);
        }

        while (connected) {
            try {
                //Fixme: I think we get fail loop here because we are waiting for a read but then socket is closed?
                String stream = fromClient.readLine();

                String[] parts = stream.split(LENGTH_DELIMITER);
                int lengthHeader = Integer.parseInt(parts[LENGTH_INDEX]);
                StringBuilder clientInput = new StringBuilder(parts[DATA_INDEX]);

                while (lengthHeader > clientInput.length())
                    clientInput.append(fromClient.readLine());

                String input = clientInput.toString().toUpperCase();
                Command cmd = Command.valueOf(parseCommand(input));

                switch (cmd) {
                    case START:
                        if (server.isGameRunning()) {
                            transmit(Command.RUNNING.toString());
                            transmit(getStateOutput());
                            break;
                        }

                        server.startGame();

                        server.broadcast(Command.START.toString() + DATA_DELIMITER + player);
                        server.broadcast(getStateOutput());
                        break;
                    case DISCONNECT:
                        disconnect();
                        server.broadcast(Command.DISCONNECT.toString() + DATA_DELIMITER + player);
                        break;
                    case GUESS:
                        if (!server.isGameRunning()) {
                            transmit(Command.NOT_RUNNING.toString());
                        }

                        DTO dto = server.guess(parseLiteral(input));

                        if (dto.gameWon()) {
                            server.incrementScore();
                        } else if (!dto.gameWon() & dto.getRemainingAttempts() == 0) {
                            server.decrementScore();
                        }

                        server.broadcast(Command.GUESS.toString() +
                                DATA_DELIMITER + player +
                                DATA_DELIMITER +
                                dto.getGuessedLetters().substring(dto.getGuessedLetters().length() - 1));
                        server.broadcast(getStateOutput());
                        break;
                    case USER:
                        String playerOld = player;
                        player = parseLiteral(input);

                        server.broadcast(Command.USER.toString() +
                                DATA_DELIMITER + playerOld +
                                DATA_DELIMITER + player);
                        break;
                    case SCORE:
                        transmit(Command.SCORE.toString() + DATA_DELIMITER + stat);
                        break;
                    case RULES:
                        transmit(Command.RULES.toString() + DATA_DELIMITER + server.getRules());
                        break;
                    default:
                        //Fixme: proper exception? Is that even a req?
                        System.out.println("default in clienthandler");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //Fixme: these functions are the same as in interpreter, can we extract anything?
    private String parseCommand(String input) {
        return input.split(" ")[0];
    }

    private String parseLiteral(String input) {
        return input.split(" ")[1];
    }

    private String getStateOutput() {
        DTO dto = server.getGameStateDTO();
        return Command.STATE.toString() +
                DATA_DELIMITER + dto.getGameState() +
                DATA_DELIMITER + dto.getRemainingAttempts() +
                DATA_DELIMITER + dto.gameWon() +
                DATA_DELIMITER + dto.getGuessedLetters() +
                DATA_DELIMITER + dto.getNoLetters();
    }

    /**
     * Used to transmit data to its client.
     * */
    void transmit(String update) {
        String lengthHeader = Integer.toString(update.length()) + LENGTH_DELIMITER ;
        toClient.println(lengthHeader + update);
    }

    private void disconnect() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connected = false;
        server.removeHandler(this);
    }

    void incrementScore() {
        stat++;
    }

    void decrementScore() {
        stat--;
    }
}
