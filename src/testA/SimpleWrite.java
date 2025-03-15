package testA;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class SimpleWrite {
    public static void main(String[] args) {
        String path = "/zfs/brainstorming.txt";
        String text = "Hallo Welt";

        SimpleWrite.write(path, text, 5_000);

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

    public static void write_mem(char c) throws Exception {
        Path path = Paths.get("/dev/shm/status"); // Shared Memory Datei
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 1);
            buffer.put(0, (byte) c);
        }
    }

    public static char read_mem() throws Exception {
        Path path = Paths.get("/dev/shm/status"); // Shared Memory Datei
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 1);
            return (char) buffer.get(0);
        }
    }
}
