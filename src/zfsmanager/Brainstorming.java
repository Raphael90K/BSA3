package zfsmanager;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class Brainstorming {

    private static final String IDEAS_DIRECTORY = "ideas"; // Ordner, in dem die Ideen gespeichert werden
    private static final TransactionManager TM = new TransactionManager(IDEAS_DIRECTORY);

    public static void main(String[] args) {


        do {
            System.out.println("\nWählen Sie eine Option:");
            System.out.println("1: Alle Ideen anzeigen");
            System.out.println("2: Eine neue Idee hinzufügen");
            System.out.println("3: Eine Idee kommentieren");
            System.out.println("4: Eine Idee löschen");
            System.out.println("5: Beenden");

        } while (handleConsole());
    }

    private static boolean handleConsole() {
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // Um den "Enter" nach der Zahl zu konsumieren

        switch (choice) {
            case 1:
                displayIdeas(scanner);
                break;
            case 2:
                addIdea(scanner);
                break;
            case 3:
                commentOnIdea(scanner);
                break;
            case 4:
                deleteIdea(scanner);
                break;
            case 5:
                System.out.println("Programm wird beendet.");
                scanner.close();
                return false;
            default:
                System.out.println("Ungültige Auswahl, bitte wählen Sie eine der Optionen.");
        }
        return true;
    }

    // 1. Alle Ideen anzeigen
    public static void displayIdeas(Scanner scanner) {
        File folder = new File(IDEAS_DIRECTORY);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (listOfFiles != null && listOfFiles.length > 0) {
            System.out.println("\nListe der Ideen:");
            for (File file : listOfFiles) {
                String fileName = file.getName().replace(".txt", ""); // Entferne die Endung .txt
                System.out.println("- " + fileName);
            }
        } else {
            System.out.println("Es gibt keine Ideen.");
        }
    }


    // 2. Eine neue Idee hinzufügen
    public static void addIdea(Scanner scanner) {
        System.out.print("Geben Sie den Titel der neuen Idee ein: ");
        String title = scanner.nextLine().trim();

        // Verhindere das Erstellen von Dateien mit leerem Titel
        if (title.isEmpty()) {
            System.out.println("Titel darf nicht leer sein.");
            return;
        }

        Path filePath = Path.of(IDEAS_DIRECTORY, title + ".txt");

        TM.start(filePath);
        String text = handleAdd(scanner, filePath, title);
        TM.commit(text, false);
    }

    private static String handleAdd(Scanner scanner, Path filePath, String title) {
        File newIdeaFile = filePath.toFile().getAbsoluteFile();
        StringBuilder comment = new StringBuilder();
        try {
            if (newIdeaFile.createNewFile()) {
                System.out.println("Neue Idee '" + title + "' wurde hinzugefügt.");
                System.out.print("Geben Sie Kommentare zur Idee ein (gib 'exit' ein, um zu beenden): ");

                String line;
                while (!(line = scanner.nextLine()).equals("exit")) {
                    comment.append(line).append("\n");
                }
            } else {
                System.out.println("Die Datei für diese Idee existiert bereits.");
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Erstellen der Datei: " + e.getMessage());
            e.printStackTrace();
        }
        return comment.toString();
    }

    // 3. Eine Idee kommentieren (Kommentar anhängen)
    public static void commentOnIdea(Scanner scanner) {
        System.out.print("Geben Sie den Titel der Idee ein, die Sie kommentieren möchten: ");
        String title = scanner.nextLine().trim();

        Path filePath = Path.of(IDEAS_DIRECTORY, title + ".txt");
        TM.start(filePath);
        String text = handleComment(scanner, filePath, title);
        TM.commit(text, true);
    }

    private static String handleComment(Scanner scanner, Path filePath, String title) {
        File ideaFile = filePath.toFile().getAbsoluteFile();

        if (!ideaFile.exists()) {
            System.out.println("Diese Idee existiert nicht.");
            return "";
        }

        // Zeige den aktuellen Inhalt der Idee an
        System.out.println("\nAktuelle Kommentare zu '" + title + "':");
        try (BufferedReader reader = new BufferedReader(new FileReader(ideaFile))) {
            String line;
            boolean hasContent = false;
            while ((line = reader.readLine()) != null) {
                System.out.println("- " + line);
                hasContent = true;
            }
            if (!hasContent) {
                System.out.println("(Noch keine Kommentare vorhanden)");
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Datei: " + e.getMessage());
            return "";
        }

        // Füge Kommentar hinzu
        System.out.print("Geben Sie den Kommentar ein, der hinzugefügt werden soll: ");
        String comment = scanner.nextLine().trim();

        return comment;
    }

    // 4. Eine Idee löschen
    public static void deleteIdea(Scanner scanner) {
        System.out.print("Geben Sie den Titel der zu löschenden Idee ein: ");
        String title = scanner.nextLine().trim();

        Path filePath = Path.of(IDEAS_DIRECTORY, title + ".txt");

        TM.start(filePath);
        handleDelete(filePath, title);
        TM.commit("delete file", true);
    }

    private static void handleDelete(Path filePath, String title) {
        File ideaFile = filePath.toFile().getAbsoluteFile();

        if (ideaFile.exists() && ideaFile.delete()) {
            System.out.println("Die Idee '" + title + "' wurde gelöscht.");
        } else {
            System.out.println("Die Idee konnte nicht gefunden oder gelöscht werden.");
        }
    }
}
