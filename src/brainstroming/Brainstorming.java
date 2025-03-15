package brainstroming;

import org.jline.reader.*;
import org.jline.terminal.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Brainstorming {
    private static final String FILE_PATH = "brainstorming.txt";
    private static List<String> lines = new ArrayList<>();
    private static Terminal terminal;
    private static LineReader reader;

    public static void main(String[] args) throws IOException {
        terminal = TerminalBuilder.terminal();
        reader = LineReaderBuilder.builder().terminal(terminal).build();

        loadFile();
        runEditor();
    }

    private static void loadFile() throws IOException {
        Path path = Paths.get(FILE_PATH);
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        lines = Files.readAllLines(path);
        if (lines.isEmpty()) lines.add("");
    }

    private static void runEditor() throws IOException {
        while (true) {
            displayFile();
            String command = reader.readLine("Befehl (edit <nr>, add, delete <nr>, save, exit): ");
            handleCommand(command);
        }
    }

    private static void displayFile() {
        System.out.println("\n--- Datei Inhalt ---");
        for (int i = 0; i < lines.size(); i++) {
            System.out.println((i + 1) + ": " + lines.get(i));
        }
        System.out.println("---------------------\n");
    }

    private static void handleCommand(String command) throws IOException {
        if (command.startsWith("edit ")) {
            editLine(command);
        } else if (command.equals("add")) {
            addLine();
        } else if (command.startsWith("delete ")) {
            deleteLine(command);
        } else if (command.equals("save")) {
            saveFile();
        } else if (command.equals("exit")) {
            System.out.println("Editor beendet.");
            System.exit(0);
        } else {
            System.out.println("Unbekannter Befehl!");
        }
    }

    private static void editLine(String command) {
        try {
            int lineNumber = Integer.parseInt(command.split(" ")[1]) - 1;
            if (lineNumber >= 0 && lineNumber < lines.size()) {
                String newText = reader.readLine("Neue Eingabe für Zeile " + (lineNumber + 1) + ": ");
                lines.set(lineNumber, newText);
            } else {
                System.out.println("Ungültige Zeilennummer!");
            }
        } catch (Exception e) {
            System.out.println("Fehlerhafte Eingabe!");
        }
    }

    private static void addLine() {
        String newText = reader.readLine("Neue Zeile: ");
        lines.add(newText);
    }

    private static void deleteLine(String command) {
        try {
            int lineNumber = Integer.parseInt(command.split(" ")[1]) - 1;
            if (lineNumber >= 0 && lineNumber < lines.size()) {
                lines.remove(lineNumber);
                System.out.println("Zeile " + (lineNumber + 1) + " gelöscht.");
            } else {
                System.out.println("Ungültige Zeilennummer!");
            }
        } catch (Exception e) {
            System.out.println("Fehlerhafte Eingabe!");
        }
    }

    private static void saveFile() throws IOException {
        Files.write(Paths.get(FILE_PATH), lines);
        System.out.println("Datei gespeichert!");
    }
}
