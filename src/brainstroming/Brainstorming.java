package brainstroming;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Brainstorming {
    private static String FILE_PATH;
    private static List<String> lines = new ArrayList<>();
    private static BufferedWriter writer;

    public static void main(String[] args) {
        FILE_PATH = args.length == 0 ? "brainstorming.txt" : args[0];
        try {
            loadFile();
            startEditor();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadFile() throws IOException {
        Path path = Paths.get(FILE_PATH);
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        lines = Files.readAllLines(path);
        if (lines.isEmpty()) lines.add("");

        // Open the file once for writing
        writer = new BufferedWriter(new FileWriter(FILE_PATH, false)); // false: overwrite file
    }

    private static void startEditor() throws IOException {
        Screen screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();

        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        TextBox textBox = new TextBox(new TerminalSize(70, 20)); // Anpassung der Editor-Größe
        textBox.setText(String.join("\n", lines));
        textBox.setVerticalFocusSwitching(true);
        panel.addComponent(textBox);

        Button saveButton = new Button("Speichern & Beenden", () -> {
            try {
                // Write the new content directly to the opened file
                writer.write(textBox.getText());
                writer.flush(); // Ensure everything is written to the file
                screen.stopScreen();
                System.out.println("Datei gespeichert!");
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        panel.addComponent(saveButton);

        BasicWindow window = new BasicWindow("Brainstorming App");
        window.setComponent(panel);
        window.setHints(Arrays.asList(Window.Hint.FIXED_SIZE)); // Fenstergröße fixieren
        window.setSize(new TerminalSize(90, 25)); // Fenstergröße setzen

        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace());
        gui.addWindowAndWait(window);
    }
}
