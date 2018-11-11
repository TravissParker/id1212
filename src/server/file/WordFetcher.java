package server.file;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;

/**
 * Gets and delivers word to the game.
 * */
public class WordFetcher {
    //Fixme: set relative path to project root
    private final String path = "C:\\Users\\mikae\\OneDrive\\IdeaProjects\\HW1\\words.txt";
    private List<String> library = new ArrayList<>();


    public WordFetcher() {
       try {
           library = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

    public String supplyWord() {
        return drawWord();
   }

   private String drawWord() {
       int index = new Random().nextInt(library.size());
       return library.get(index);
   }
}
