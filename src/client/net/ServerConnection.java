package client.net;

import common.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.StringJoiner;

import static common.Constants.*;

public class ServerConnection {
    private Socket socket;
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private volatile boolean connected;
    private final String DISCONNECT = "DISCONNECT";

    public void connect(String host, int port, Broadcast caster) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port));
        boolean autoFlush = true;
        toServer = new PrintWriter(socket.getOutputStream(), autoFlush);
        fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
        new Thread(new Listener(caster)).start();
    }

    public void disconnect() throws IOException {
        transmit(DISCONNECT);
        socket.close();
        socket = null;
        connected = false;
    }

    public void username(String name) {
        transmit(Command.USER.toString(), name);
    }

    public void startGame() {
        transmit(Command.START.toString());
    }

    public void guess(String lit) {
        transmit(Command.GUESS.toString(), lit);
    }

    public void getScore() {
        transmit(Command.SCORE.toString());
    }

    public void getRules() {
        transmit(Command.RULES.toString());
    }

    private class Listener implements Runnable {
        private final Broadcast caster;

        private Listener(Broadcast caster) {
            this.caster = caster;
        }

        @Override
        public void run() {
            try {
                while (connected) {
                    //Fixme: thisb is the endles loop of trans null, we need to mark as not connected
                    //Question: readline blocks?
                    String stream = fromServer.readLine();

                    String[] parts = stream.split(LENGTH_DELIMITER);
                    int lengthHeader = Integer.parseInt(parts[LENGTH_INDEX]);
                    StringBuilder serverOutput = new StringBuilder(parts[DATA_INDEX]);

                    while (lengthHeader > serverOutput.length())
                        serverOutput.append(fromServer.readLine());

                    caster.relay(splitStream(serverOutput.toString()));
                }
            }
            catch (Exception e) {
                if (connected)
                    e.printStackTrace();
            }
        }
    }
    /**
     * PROTOCOL:
    * splitData = [0]:TYPE,
     * GUESS: [1]=PLAYER
     * STATE: [1]:State of word, [2]:Remaining attempts, [3]:Outcome, [4]: Prev. guessed letters, [5]: No letters
     * */
    private String splitStream(String data) {
        String[] splitData = data.split(DATA_DELIMITER);

        String returnValue;
        switch (Command.valueOf(splitData[TYPE_INDEX])) {
            case GUESS:
                returnValue = splitData[1] + " guessed: " + splitData[2];
                break;
            case STATE:
                String state = splitData[1];
                String outlook;
                int attemptsLeft = Integer.parseInt(splitData[2]);
                boolean gameWon = Boolean.parseBoolean(splitData[3]);

                if (attemptsLeft < 1 & !gameWon)
                    outlook = "Game over, better luck next time..."+ NEW_LINE;
                else if (gameWon)
                    outlook = "Good job, you won!"+ NEW_LINE;
                else
                    outlook = attemptsLeft + " attempts to go."+ NEW_LINE;

                returnValue = splitData[5] + " letter word: " + state +
                        NEW_LINE + outlook +
                        NEW_LINE + "Previously guessed:" +
                        NEW_LINE + splitData[4];
                break;
            case USER:
                returnValue = splitData[1] + " changed name to " + splitData[2] + NEW_LINE;
                break;
            case START:
                returnValue = splitData[1] + " started a new game!" + NEW_LINE;
                break;
            case DISCONNECT:
                //Todo
                returnValue = splitData[1] + " left the game :("+ NEW_LINE;
                break;
            case SCORE:
                returnValue = "Your score is " + splitData[1];
                break;
            case RUNNING:
                returnValue = "A game is already running, use the GUESS command to play."+ NEW_LINE;
                break;
            case NOT_RUNNING:
                returnValue = "The game hasn't been started, use the START command to play."+ NEW_LINE;
                break;
            case RULES:
                returnValue = splitData[1];
                break;
            default:
                returnValue = "ERROR";
        }
        return returnValue;
    }

    private void transmit(String... data) {
        StringJoiner sj = new StringJoiner(WORD_DELIMITER);
        for (String d: data)
            sj.add(d);
        String packet = Integer.toString(sj.length()) + LENGTH_DELIMITER + sj.toString();
        toServer.println(packet);
    }
}
