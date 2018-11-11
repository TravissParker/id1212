package client.view;

public class SynchronizedStdOut {

    synchronized void print(String output) {
        System.out.print(output);
    }

    synchronized void println(String output) {
        System.out.println(output);
    }
}
