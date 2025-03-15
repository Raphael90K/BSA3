package test;

import java.io.FileWriter;
import java.io.IOException;

public class test {
    public static void main(String[] args) {
        String path = "/zfs/1.txt";
        String text = "Hallo Welt";

        test.write(path, text, 15_000);

    }

    public static void write(String path, String text, int ms){
        try (FileWriter writer = new FileWriter(path)) {
            Thread.sleep(ms);
            writer.write(text);
            System.out.println("Datei erfolgreich geschrieben: " + path);
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
