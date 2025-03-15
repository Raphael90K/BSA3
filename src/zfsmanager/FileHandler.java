package zfsmanager;

import java.io.*;
import java.nio.file.*;

public class FileHandler {
    private String directory;

    public FileHandler(String directory) {
        this.directory = directory;
    }

    // Methode zum Einlesen einer Datei und Rückgabe des Inhalts als String
    public String readFromFile(String filePath) {
        Path path = Paths.get(this.directory, filePath);
        StringBuilder content = new StringBuilder();
        try {
            // Lesen der Datei mit BufferedReader
            BufferedReader reader = Files.newBufferedReader(path);
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Datei: " + e.getMessage());
        }
        return content.toString();
    }

    // Methode zum Schreiben eines Strings in eine Datei
    public void writeToFile(String filePath, String content) {
        Path path = Paths.get(this.directory, filePath);
        try {
            // Schreiben der Datei mit BufferedWriter
            BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        }
    }

}
