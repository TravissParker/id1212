package client.view;

import java.io.IOException;
import java.util.Scanner;
import client.controller.Controller;
import client.net.Broadcast;
import common.Command;

import static common.Constants.LENGTH_DELIMITER;
import static common.Constants.WORD_DELIMITER;

public class Interpreter implements Runnable {
    private static final String PROMPT = "> ";
    private final Scanner console = new Scanner(System.in);
    private Controller ctrl;
    private SynchronizedStdOut printer = new SynchronizedStdOut();
    private boolean running = false;
    //Fixme: these should be found in input
    private final String host = "localhost";
    private final int port = 8080;
    private final String  commands = "" +
            "    CONNECT: connects you to the server.\n" +
            "    DISCONNECT: disconnects you from the server.\n" +
            "    USER [name]: change your screen name. \n" +
            "    START: start a new game.\n" +
            "    GUESS [letter|word]: make a guess, letter or whole word.\n" +
            "    RULES: shows the rules of the game.\n" +
            "    SCORE: shows your score.\n";

    /**
    * Starts up a new interpreter thread.
    * */
    public void start() {
        // Guards that nothing will happen if start is called on running interpreter.
        if (running)    return;

        ctrl = new Controller();
        running = true;
        new Thread(this).start();
    }
    /**
     * The code that is run in the thread. The thread will spend its life here.
     * Listens to user input.
     * */
    @Override
    public void run() {
        while (running) {
            try {
                String input = readNextLine().toUpperCase();
                input = input.trim();
                input = input.replace(LENGTH_DELIMITER, "");
                Command cmd = Command.valueOf(parseCommand(input));
                switch(cmd) {
                    case CONNECT:
                        ctrl.connect(host, port, new Broadcaster());
                        break;
                    case DISCONNECT:
                        //Fixme: disconnect post, the server loops exceptions, other threads can work still.
                        ctrl.disconnect();
                        break;
                    case USER:
                        ctrl.username(parseLiteral(input));
                        break;
                    case GUESS:
                        ctrl.guess(parseLiteral(input));
                        break;
                    case START:
                        //Fixme: I don't think this is threaded proper.
                        ctrl.startGame();
                        break;
                    case RULES:
                        ctrl.getRules();
                        break;
                    case SCORE:
                        ctrl.getScore();
                        break;
                    case HELP:
                        System.out.println(commands);
                    default:
                        throw new IllegalArgumentException();
                }
            }
            catch (IllegalArgumentException e) {
                printer.println("That is not a known command, type RULES to see a list of instructions.");
            }
            catch (ArrayIndexOutOfBoundsException e) {
                printer.println("Arguments where missing, type RULES to see a list of instructions.");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String readNextLine() {
        System.out.print(PROMPT);
        return console.nextLine();
    }

    private String parseCommand(String input) {
        return input.split(WORD_DELIMITER)[0];
    }

    private String parseLiteral(String input) {
        return input.split(WORD_DELIMITER)[1];
    }

    private class Broadcaster implements Broadcast {
        @Override
        public void relay(String trans) {
            printer.println(trans);
            printer.print(PROMPT);
        }
    }
}
