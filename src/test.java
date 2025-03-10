import java.io.FileWriter;
import java.io.IOException;

public class test {
    public static void main(String[] args) {
        String dateiName = "/zfs/1.txt";
        String inhalt = "Hallo Welt";

        try (FileWriter writer = new FileWriter(dateiName)) {
            writer.write(inhalt);
            System.out.println("Datei erfolgreich geschrieben: " + dateiName);
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        }
    }
}
