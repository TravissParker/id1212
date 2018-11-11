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
    private final String WELCOME_MESSAGE = "Welcome to the Hangman Game!\nAt any time you can type HELP to see a list of instructions, if you feel you need it.";
    private final String  commands = "" +
            "    CONNECT: connects you to the server.\n" +
            "    DISCONNECT: disconnects you from the server.\n" +
            "    USER [name]: change your screen name. \n" +
            "    START: start a new game.\n" +
            "    GUESS [letter|word]: make a guess, letter or whole word.\n" +
            "    RULES: shows the rules of the game.\n" +
            "    SCORE: shows your score.\n";
    //Fixme: these should be parsed from user input, hardcoded hear during development
    private final String host = "localhost";
    private final int port = 8080;

    /**
    * Starts up a new interpreter thread.
    * */
    public void start() {
        // Guards that nothing will happen if start is called on running interpreter.
        if (running)    return;

        System.out.println(WELCOME_MESSAGE);
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
                        ctrl.disconnect();
                        break;
                    case USER:
                        ctrl.username(parseLiteral(input));
                        break;
                    case GUESS:
                        ctrl.guess(parseLiteral(input));
                        break;
                    case START:
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
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
            catch (IllegalArgumentException e) {
                printer.println("That is not a known command, type HELP to see a list of instructions.");
            }
            catch (ArrayIndexOutOfBoundsException e) {
                printer.println("Arguments where missing, type HELP to see a list of instructions.");
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
