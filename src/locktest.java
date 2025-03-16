import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class locktest {
    public static void main(String[] args) {
        File lockFile = new File("test.txt");

        try (FileOutputStream fos = new FileOutputStream(lockFile, true);
             FileChannel channel = fos.getChannel()) { // Versuche exklusive Sperre

            FileLock lock = channel.lock();
            if (lock == null) {
                System.out.println("Ein anderer Prozess hat bereits die Sperre.");
                return; // Beende das Programm
            }


            System.out.println("Datei gesperrt. Drücke Enter zum Freigeben...");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write("Test");
            writer.flush();
            System.in.read(); // Halte Sperre aktiv

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Sperre freigegeben.");
    }
}
