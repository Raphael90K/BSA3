package brainstroming;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Brainstorming {
    private static String FILE_PATH;
    private static List<String> lines = new ArrayList<>();

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
                Files.write(Paths.get(FILE_PATH), Arrays.asList(textBox.getText().split("\n")));
                screen.stopScreen();
                System.out.println("Datei gespeichert!");
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        panel.addComponent(saveButton);

        BasicWindow window = new BasicWindow("Texteditor");
        window.setComponent(panel);
        window.setHints(Arrays.asList(Window.Hint.FIXED_SIZE)); // Fenstergröße fixieren
        window.setSize(new TerminalSize(90, 25)); // Fenstergröße setzen

        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace());
        gui.addWindowAndWait(window);
    }
}
