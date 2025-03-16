package brainstroming;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BrainstromingApp {
    private static final String DIRECTORY_PATH = "/zfs";
    private static String filePath;
    private static List<String> lines = new ArrayList<>();
    private static BufferedWriter writer;

    public static void main(String[] args) {
        showFileSelectionMenu();
    }

    private static void showFileSelectionMenu() {
        try (Screen screen = new DefaultTerminalFactory().createScreen()) {
            screen.startScreen();

            Panel panel = new Panel();
            panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

            Panel mainPanel = new Panel(new GridLayout(1)); // Erstellt ein zentriertes Layout
            mainPanel.addComponent(new EmptySpace(new TerminalSize(1, 1))); // Abstand für Zentrierung
            mainPanel.addComponent(panel.withBorder(Borders.doubleLine("Wählen"))); // Fügt einen Rahmen hinzu
            mainPanel.addComponent(new EmptySpace(new TerminalSize(1, 1))); // Weitere Abstand für Zentrierung

            List<String> files = getFiles();
            files.add("[Neue Datei erstellen]");
            files.add("[Datei löschen]");

            ComboBox<String> fileSelector = new ComboBox<>(files);
            panel.addComponent(fileSelector);

            Button openButton = new Button("wählen", () -> {
                String selected = fileSelector.getSelectedItem();
                if ("[Neue Datei erstellen]".equals(selected)) {
                    createNewFile(screen);
                } else if ("[Datei löschen]".equals(selected)) {
                    deleteFile(screen);
                } else {
                    filePath = DIRECTORY_PATH + "/" + selected;
                    try {
                        startEditor();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            panel.addComponent(openButton);

            Button exitButton = new Button("Beenden", () -> {
                System.exit(0);
            });
            panel.addComponent(exitButton);

            BasicWindow window = new BasicWindow("Brainstorming App");
            window.setComponent(mainPanel);
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace());
            gui.addWindowAndWait(window);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getFiles() {
        File folder = new File(DIRECTORY_PATH);
        String[] files = folder.list((dir, name) -> name.endsWith(".txt"));
        return files == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(files));
    }

    private static void createNewFile(Screen screen) {
        TextBox fileNameInput = new TextBox();
        Button createButton = new Button("Erstellen", () -> {
            String newFileName = fileNameInput.getText().trim();
            if (!newFileName.isEmpty()) {
                filePath = DIRECTORY_PATH + "/" + newFileName + ".txt";
                try {
                    Files.createFile(Paths.get(filePath));
                    startEditor();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        showSimpleWindow(screen, "Neue Datei", fileNameInput, createButton);
    }

    private static void deleteFile(Screen screen) {
        List<String> files = getFiles();
        ComboBox<String> fileSelector = new ComboBox<>(files);
        Button deleteButton = new Button("Löschen", () -> {
            String selected = fileSelector.getSelectedItem();
            if (selected != null) {
                try {
                    Files.delete(Paths.get(DIRECTORY_PATH + "/" + selected));
                    showFileSelectionMenu();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        showSimpleWindow(screen, "Datei löschen", fileSelector, deleteButton);
    }

    private static void showSimpleWindow(Screen screen, String title, Component input, Button actionButton) {
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(input);
        panel.addComponent(actionButton);

        BasicWindow window = new BasicWindow(title);
        window.setComponent(panel);
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace());
        gui.addWindowAndWait(window);
    }

    private static void startEditor() throws IOException {
        lines = Files.readAllLines(Paths.get(filePath));
        System.out.println(Paths.get(filePath).toFile().getAbsolutePath());
        writer = new BufferedWriter(new FileWriter(filePath, false)); // false: overwrite file
        if (lines.isEmpty()) lines.add("");

        Screen screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();

        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        TextBox textBox = new TextBox(new TerminalSize(70, 18));
        textBox.setText(String.join("\n", lines));
        textBox.setVerticalFocusSwitching(true);
        panel.addComponent(textBox);

        Button saveButton = new Button("Speichern & Zurück", () -> {
            try {
                // Write the new content directly to the opened file
                writer.write(textBox.getText());
                writer.flush(); // Ensure everything is written to the file
                screen.stopScreen();
                showFileSelectionMenu();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        panel.addComponent(saveButton);

        BasicWindow window = new BasicWindow(filePath);
        window.setComponent(panel);
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace());
        gui.addWindowAndWait(window);
    }
}
